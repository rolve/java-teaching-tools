package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.testrunner.TestResults.MethodResult;
import ch.trick17.jtt.testrunner.TestRunConfig;
import ch.trick17.jtt.testrunner.TestRunner;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.build.intercept.equivalent.EquivalentReturnMutationFilter;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static ch.trick17.jtt.memcompile.ClassPath.empty;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static ch.trick17.jtt.memcompile.InMemCompilation.compile;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparingInt;
import static java.util.Map.Entry.comparingByKey;
import static java.util.Map.Entry.comparingByValue;
import static java.util.Map.entry;
import static java.util.stream.Collectors.*;

public class TestSuiteGrader {

    public void grade(Submission submission, Task task) throws IOException {
        var refImpls = new ArrayList<List<InMemClassFile>>();
        for (int i = 0; i < task.refImplementations().size(); i++) {
            var implResult = compile(JAVAC, task.refImplementations().get(i),
                    empty(), System.out);
            if (implResult.errors()) {
                throw new IllegalArgumentException("Could not compile reference implementation " + (i + 1));
            } else if (implResult.output().isEmpty()) {
                throw new IllegalArgumentException("Empty reference implementation " + (i + 1));
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

        var mutants = new HashMap<Set<CauseOfDeath>, List<Mutant>>();
        try (var testRunner = new TestRunner()) {
            for (int i = 0; i < refImpls.size(); i++) {
                var refImpl = refImpls.get(i);
                var refResults = testRunner.run(
                        new TestRunConfig(task.testClassName(),
                                ClassPath.fromMemory(refImpl),
                                ClassPath.fromMemory(refTestSuite).withCurrent()));
                for (var result : refResults.methodResults()) {
                    if (!result.passed()) {
                        throw new IllegalArgumentException("Reference implementation " + (i + 1)
                                                           + " failed test " + result.method(), result.exceptions().get(0));
                    }
                }

                var allMutants = generateMutants(refImpl);
                System.out.println("Generated " + allMutants.size() + " mutants for reference implementation " + (i + 1));
                for (int j = 0; j < allMutants.size(); j++) {
                    var mutant = allMutants.get(j);
                    var mutantResults = testRunner.run(new TestRunConfig(
                            task.testClassName(),
                            ClassPath.fromMemory(mutant.classes()),
                            ClassPath.fromMemory(refTestSuite).withCurrent()));
                    var causes = mutantResults.methodResults().stream()
                            .filter(r -> !r.passed())
                            .map(r -> causeOfDeath(r))
                            .collect(toSet());
                    if (causes.isEmpty()) {
                        System.out.println("  Warning: Mutant " + (j + 1) +
                                           " survived reference test suite" +
                                           " (" + mutant.getDescription() + ")");
                    } else {
                        mutants.computeIfAbsent(causes, k -> new ArrayList<>()).add(mutant);
                    }
                }
            }
            System.out.println();
        }

        var totalMutants = mutants.values().stream().mapToInt(List::size).sum();
        System.out.println(totalMutants + " mutants killed by reference test suite\n");

        System.out.println(mutants.size() + " combinations of causes killed at least one mutant:");
        mutants.entrySet().stream()
                .sorted(comparingByKey(comparingInt(Set::size)))
                .forEach(e -> {
                    var count = e.getValue().size();
                    var causes = e.getKey().stream()
                            .map(Object::toString)
                            .sorted()
                            .collect(joining(", "));
                    System.out.println("  " + count + " mutants killed by: " + causes);
                });
        System.out.println();

        boolean first = true;
        while (!mutants.isEmpty()) {
            var killsPerCause = mutants.entrySet().stream()
                    .flatMap(e -> e.getKey().stream().map(t -> entry(t, e.getValue())))
                    .collect(groupingBy(Entry::getKey, summingInt(e -> e.getValue().size())));
            var causeWithMinKills = killsPerCause.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .min(comparingByValue()).orElseThrow()
                    .getKey();
            var kills = killsPerCause.get(causeWithMinKills);
            System.out.println(kills + " (" + (100 * kills / totalMutants) + "%)"
                               + (!first ? " more" : "")
                               + " mutants killed by " + causeWithMinKills);
            mutants.entrySet().removeIf(e -> e.getKey().contains(causeWithMinKills));
            first = false;
        }
    }

    private List<Mutant> generateMutants(List<InMemClassFile> refImpl) {
        var mutater = new GregorMutater(asSource(refImpl), m -> true, Mutator.all());
        var filter = new EquivalentReturnMutationFilter().createInterceptor(null);

        var mutants = new ArrayList<Mutant>();
        for (int i = 0; i < refImpl.size(); i++) {
            var cls = refImpl.get(i);
            var mutations = mutater.findMutations(ClassName.fromString(cls.getClassName()));

            filter.begin(ClassTree.fromBytes(cls.getContent()));
            var filtered = filter.intercept(mutations, mutater);
            filter.end();

            for (var mutation : filtered) {
                var classes = new ArrayList<>(refImpl);
                var mutated = mutater.getMutation(mutation.getId()).getBytes();
                classes.set(i, new InMemClassFile(cls.getClassName(), mutated));
                mutants.add(new Mutant(classes, i, mutation));
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

    private static CauseOfDeath causeOfDeath(MethodResult result) {
        if (result.timeout()) {
            return new Timeout(result.method());
        } else if (result.outOfMemory()) {
            return new OutOfMemory(result.method());
        } else if (!result.illegalOps().isEmpty()) {
            return new IllegalOp(result.method(), result.illegalOps().get(0));
        } else {
            var e = result.exceptions().get(0);
            var line = stream(e.getStackTrace())
                    .filter(s -> s.getMethodName().equals(result.method()))
                    .findFirst().orElseThrow()
                    .getLineNumber();
            return new Exception(result.method(), line, e.getClass());
        }
    }

    sealed interface CauseOfDeath {
        String method();
    }

    public record Timeout(String method) implements CauseOfDeath {
        public String toString() {
            return method + " (timeout)";
        }
    }

    public record OutOfMemory(String method) implements CauseOfDeath {
        public String toString() {
            return method + " (out of memory)";
        }
    }

    public record IllegalOp(String method, String op) implements CauseOfDeath {
        public String toString() {
            return method + " (illegal op: " + op + ")";
        }
    }

    public record Exception(String method, int line,
                            Class<?> exceptionClass) implements CauseOfDeath {
        public String toString() {
            return method + ":" + line + " (" + exceptionClass.getSimpleName() + ")";
        }
    }
}
