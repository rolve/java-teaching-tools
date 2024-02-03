package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.testrunner.TestRunConfig;
import ch.trick17.jtt.testrunner.TestRunner;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static ch.trick17.jtt.memcompile.ClassPath.empty;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static ch.trick17.jtt.memcompile.InMemCompilation.compile;
import static java.lang.String.join;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;
import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

public class TestSuiteGrader {

    public void grade(Submission submission, Task task) throws IOException {
        var refImpls = new ArrayList<List<InMemClassFile>>();
        for (int i = 0; i < task.refImplementations().size(); i++) {
            var implResult = compile(JAVAC, task.refImplementations().get(i),
                    empty(), System.out);
            if (implResult.errors()) {
                throw new IllegalArgumentException("Could not compile reference implementation " + i);
            } else if (implResult.output().isEmpty()) {
                throw new IllegalArgumentException("Empty reference implementation " + i);
            }
            refImpls.add(implResult.output());
        }

        var refTestSuiteResult = compile(JAVAC, List.of(task.refTestSuite()),
                ClassPath.fromMemory(refImpls.get(0)).withCurrent(), System.out);
        if (refTestSuiteResult.errors()) {
            throw new IllegalArgumentException("Could not compile reference test suite against sample implementation");
        } else if (refTestSuiteResult.output().isEmpty()) {
            throw new IllegalArgumentException("Empty reference test suite");
        }
        var refTestSuite = refTestSuiteResult.output();

        var mutants = new HashMap<List<String>, List<List<InMemClassFile>>>();
        try (var testRunner = new TestRunner()) {
            for (int i = 0; i < refImpls.size(); i++) {
                var refImpl = refImpls.get(i);
                var refResults = testRunner.run(
                        new TestRunConfig(task.testClassName(),
                        ClassPath.fromMemory(refImpl),
                        ClassPath.fromMemory(refTestSuite).withCurrent()));
                for (var result : refResults.methodResults()) {
                    if (!result.passed()) {
                        throw new IllegalArgumentException("Reference implementation " + i
                                + " failed test " + result.method() + ": " + result.failMsgs().get(0));
                    }
                }

                var allMutants = generateMutants(refImpl);
                System.out.println("Generated " + allMutants.size() + " mutants for reference implementation " + i);
                for (int j = 0; j < allMutants.size(); j++) {
                    var mutantResults = testRunner.run(new TestRunConfig(
                            task.testClassName(),
                            ClassPath.fromMemory(allMutants.get(j)),
                            ClassPath.fromMemory(refTestSuite).withCurrent()));
                    var failed = mutantResults.methodResults().stream()
                            .filter(r -> !r.passed())
                            .map(r -> r.method())
                            .sorted()
                            .toList();
                    if (failed.isEmpty()) {
                        System.out.println("  Warning: Mutant " + j + " survived reference test suite");
                    } else {
                        mutants.computeIfAbsent(failed, k -> new ArrayList<>()).add(allMutants.get(j));
                        System.out.println("  Mutant " + j + " killed by tests: " + join(", ", failed));
                    }
                }
            }
            System.out.println();
        }

        var totalMutants = mutants.values().stream().mapToInt(List::size).sum();
        System.out.println(totalMutants + " mutants killed by reference test suite in total\n");

        System.out.println(mutants.size() + " combinations of tests killed at least one mutant:");
        mutants.entrySet().stream()
                .sorted(comparingByKey(comparingInt(List::size)))
                .forEach(e -> System.out.println("  " + e.getValue().size()
                                                 + " mutants killed by tests: "
                                                 + join(", ", e.getKey())));
        System.out.println();

        boolean first = true;
        while (!mutants.isEmpty()) {
            var killsPerTest = mutants.entrySet().stream()
                    .flatMap(e -> e.getKey().stream().map(t -> entry(t, e.getValue())))
                    .collect(groupingBy(Entry::getKey, summingInt(e -> e.getValue().size())));
            var testWithMaxKills = killsPerTest.entrySet().stream()
                    .max(comparingByValue()).orElseThrow()
                    .getKey();
            var kills = killsPerTest.get(testWithMaxKills);
            System.out.println(kills + " (" + (100 * kills / totalMutants) + "%)"
                               + (!first ? " more" : "")
                               + " mutants killed by " + testWithMaxKills);
            mutants.entrySet().removeIf(e -> e.getKey().contains(testWithMaxKills));
            first = false;
        }
    }

    private List<List<InMemClassFile>> generateMutants(List<InMemClassFile> refImpl) {
        var mutater = new GregorMutater(asSource(refImpl), m -> true, Mutator.all());
        var mutants = new ArrayList<List<InMemClassFile>>();
        for (int i = 0; i < refImpl.size(); i++) {
            var cls = refImpl.get(i);
            var mutations = mutater.findMutations(ClassName.fromString(cls.getClassName()));
            for (int j = 0; j < mutations.size(); j++) {
                var mutated = mutater.getMutation(mutations.get(j).getId()).getBytes();
                var mutant = new ArrayList<>(refImpl);
                mutant.set(i, new InMemClassFile(cls.getClassName(), mutated));
                mutants.add(mutant);
            }
        }
        return mutants;
    }

    private ClassByteArraySource asSource(List<InMemClassFile> impl) {
        return className -> {
            for (var file : impl) {
                if (file.getBinaryName().equals(className)) {
                    return Optional.of(file.getContent());
                }
            }
            return Optional.empty();
        };
    }
}
