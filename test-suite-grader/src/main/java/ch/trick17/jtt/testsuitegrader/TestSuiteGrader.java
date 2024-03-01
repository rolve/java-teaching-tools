package ch.trick17.jtt.testsuitegrader;

import ch.trick17.jtt.memcompile.ClassPath;
import ch.trick17.jtt.memcompile.InMemClassFile;
import ch.trick17.jtt.memcompile.InMemSource;
import ch.trick17.jtt.testrunner.TestMethod;
import ch.trick17.jtt.testrunner.TestResult;
import ch.trick17.jtt.testrunner.TestRunException;
import ch.trick17.jtt.testrunner.TestRunner;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import org.junit.jupiter.api.Test;
import org.pitest.bytecode.analysis.ClassTree;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.build.intercept.equivalent.EquivalentReturnMutationFilter;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static ch.trick17.jtt.memcompile.InMemCompilation.compile;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.Map.Entry.comparingByKey;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;

public class TestSuiteGrader implements Closeable {

    private final TestRunner testRunner;

    public TestSuiteGrader() {
        this(new TestRunner());
    }

    public TestSuiteGrader(TestRunner testRunner) {
        this.testRunner = testRunner;
    }

    public Task prepareTask(List<List<InMemSource>> refImplementations,
                            List<InMemSource> refTestSuite) throws IOException {
        return prepareTask(refImplementations, refTestSuite, emptyList());
    }

    public Task prepareTask(List<List<InMemSource>> refImplementations,
                            List<InMemSource> refTestSuite,
                            List<Path> dependencies) throws IOException {
        var classPath = ClassPath.fromFiles(dependencies);
        var compiledImplementations = new ArrayList<List<InMemClassFile>>();
        for (int i = 0; i < refImplementations.size(); i++) {
            var implResult = compile(JAVAC, refImplementations.get(i), classPath, System.out);
            if (implResult.errors()) {
                throw new IllegalArgumentException("Could not compile reference implementation " + (i + 1));
            } else if (implResult.output().isEmpty()) {
                throw new IllegalArgumentException("Empty reference implementation " + (i + 1));
            }
            compiledImplementations.add(implResult.output());
        }

        classPath = classPath.withMemory(compiledImplementations.get(0)).withCurrent();
        var refTestSuiteResult = compile(JAVAC, refTestSuite, classPath, System.out);
        if (refTestSuiteResult.errors()) {
            throw new IllegalArgumentException("Could not compile reference test suite against sample implementation");
        } else if (refTestSuiteResult.output().isEmpty()) {
            throw new IllegalArgumentException("Empty reference test suite");
        }
        var compiledSuite = refTestSuiteResult.output();

        var tests = new ArrayList<TestMethod>();
        var killedMutants = new HashMap<Mutant, List<TestMethod>>();
        var testClassNames = compiledSuite.stream()
                .map(f -> f.getClassName())
                .toList();
        for (int i = 0; i < compiledImplementations.size(); i++) {
            var refImpl = compiledImplementations.get(i);
            var refResults = testRunner.run(
                    new TestRunner.Task(testClassNames,
                            ClassPath.fromMemory(refImpl).withMemory(compiledSuite),
                            ClassPath.fromFiles(dependencies).withCurrent())).testResults();
            if (i == 0) {
                refResults.forEach(r -> tests.add(r.method()));
            }
            for (var result : refResults) {
                if (!result.passed()) {
                    throw new IllegalArgumentException("Reference implementation " + (i + 1)
                                                       + " failed test " + result.method(), result.exceptions().get(0));
                }
            }

            var mutants = generateMutants(refImpl, i);
            System.out.println("Generated " + mutants.size() + " mutants" +
                               " for reference implementation " + (i + 1));
            var killed = killMutants(mutants, compiledSuite, dependencies);
            killedMutants.putAll(killed);
        }
        System.out.println(killedMutants.size() + " mutants were killed by test suite\n");

        var weights = computeWeights(tests, killedMutants);

        var mutations = new ArrayList<Mutation>();
        for (var e : killedMutants.entrySet()) {
            var mutant = e.getKey();
            var killers = e.getValue();
            mutations.add(new Mutation(
                    mutant.refImplementationIndex(),
                    mutant.mutatedClassIndex(),
                    mutant.details().getId(),
                    weights.get(mutant),
                    killers));
        }

        var descriptions = testDescriptions(refTestSuite);
        // order descriptions according to the order of the tests in the reference suite,
        // assumed to be roughly from weak to strong
        var ordered = descriptions.entrySet().stream()
                .sorted(comparingByKey(comparingInt(m -> tests.indexOf(m))))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
        return new Task(compiledImplementations, mutations, ordered);
    }

    private List<Mutant> generateMutants(List<InMemClassFile> refImpl, int refImplIndex) {
        var mutater = createMutator(refImpl);
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
                var mutant = new Mutant(refImplIndex, i, mutation, classes);
                mutants.add(mutant);
            }
        }
        return mutants;
    }

    private Map<Mutant, List<TestMethod>> killMutants(List<Mutant> mutants,
                                                      List<InMemClassFile> testSuite,
                                                      List<Path> dependencies) throws IOException {
        var testClassNames = testSuite.stream().map(f -> f.getClassName()).toList();
        var killed = new HashMap<Mutant, List<TestMethod>>();
        for (int j = 0; j < mutants.size(); j++) {
            var mutant = mutants.get(j);
            try {
                var mutantResults = testRunner.run(new TestRunner.Task(
                        testClassNames,
                        ClassPath.fromMemory(mutant.classes()).withMemory(testSuite),
                        ClassPath.fromFiles(dependencies).withCurrent())).testResults();
                var failedTests = mutantResults.stream()
                        .filter(r -> !r.passed())
                        .map(TestResult::method)
                        .toList();
                if (failedTests.isEmpty()) {
                    System.err.println("  Warning: Mutant " + (j + 1) +
                                       " survived reference test suite" +
                                       " (" + mutant.getDescription() + ")");
                } else {
                    killed.put(mutant, failedTests);
                }
            } catch (TestRunException e) {
                System.err.println("  Warning: Mutant " + (j + 1) +
                                   " could not be tested against reference test suite" +
                                   " (" + mutant.getDescription() + ")");
                e.printStackTrace();
            }
        }
        return killed;
    }

    private Map<Mutant, Double> computeWeights(List<TestMethod> tests,
                                               Map<Mutant, List<TestMethod>> mutants) {
        // Some mutants may be very easy to kill, requiring only weak tests,
        // while others require much stronger, specific tests. A test suite
        // should only get a high score if it also kills the harder mutants,
        // even when there are much fewer of them. Thus, we group mutants into
        // "difficulty" groups and use these groups as the basis for scoring the
        // test suite.
        // We assume that the tests in the reference suite are roughly ordered
        // from weak to strong; the difficulty group of a mutant is then the
        // *first* (weakest) test that kills it.
        var grouped = mutants.entrySet().stream()
                .collect(groupingBy(e -> e.getValue().get(0), mapping(Map.Entry::getKey, toList())));
        boolean first = true;
        for (var test : tests) {
            var kills = grouped.getOrDefault(test, emptyList()).size();
            var percentage = 100 * kills / mutants.size();
            System.out.println(kills + " (" + percentage + "%) " +
                               (!first ? " more" : "") +
                               " mutants killed by " + test);
            first = false;
        }

        // Since a test suite does not necessarily kill all mutants in a group,
        // we assign each individual mutant a weight that is inversely
        // proportional to the number of mutants in its group (and to the number
        // of groups). The score of a test suite is then the sum of the weights
        // of the mutants it kills.
        var weights = new HashMap<Mutant, Double>();
        for (var group : grouped.values()) {
            var kills = group.size();
            var weight = 1.0 / (kills * grouped.size());
            for (var mutant : group) {
                weights.put(mutant, weight);
            }
        }
        return weights;
    }

    private Map<TestMethod, String> testDescriptions(List<InMemSource> testSuite) {
        var result = new HashMap<TestMethod, String>();
        for (var src : testSuite) {
            for (var comment : src.getParsed().getAllComments()) {
                if (comment instanceof JavadocComment javadoc &&
                    javadoc.getCommentedNode().isPresent() &&
                    javadoc.getCommentedNode().get() instanceof MethodDeclaration method &&
                    method.isAnnotationPresent(Test.class)) {

                    var cls = (ClassOrInterfaceDeclaration) method.getParentNode()
                            .orElseThrow(AssertionError::new);
                    var className = cls.getFullyQualifiedName();
                    if (className.isPresent()) { // false for local classes...
                        var methodName = method.getName().asString();
                        var description = javadoc.parse().toText()
                                .replaceAll("(\r?\n)+$", "")
                                .replaceAll("(\r?\n)+", " ");
                        result.put(new TestMethod(className.get(), methodName), description);
                    }
                }
            }
        }
        return result;
    }

    public Result grade(Task task, Submission submission) throws IOException {
        return grade(task, submission, emptyList());
    }

    public Result grade(Task task, Submission submission, List<Path> dependencies) throws IOException {
        var classPath = ClassPath.fromMemory(task.refImplementations().get(0))
                .withFiles(dependencies)
                .withCurrent();
        if (submission.testSuite.isEmpty()) {
            return new Result(true, false, emptyList(), emptyList(), emptyList(), 0.0);
        }
        var compileResult = compile(ECLIPSE, submission.testSuite, classPath, System.out);
        if (compileResult.errors() && compileResult.output().isEmpty()) {
            return new Result(false, true, emptyList(), emptyList(), emptyList(), 0.0);
        }
        var testSuite = compileResult.output();
        var testClassNames = testSuite.stream().map(f -> f.getClassName()).toList();

        var refResults = new ArrayList<RefImplementationResult>();
        List<TestMethod> allTests = null;
        var incorrectTests = new HashSet<TestMethod>();
        for (var impl : task.refImplementations()) {
            var sandboxed = ClassPath.fromMemory(impl).withMemory(testSuite);
            var support = ClassPath.fromFiles(dependencies).withCurrent();
            var testRun = new TestRunner.Task(testClassNames, sandboxed, support, 1,
                    Duration.ofSeconds(2), Duration.ofSeconds(5), null, emptyList());
            var testResults = testRunner.run(testRun).testResults();
            var failedTests = testResults.stream()
                    .filter(r -> !r.passed())
                    .map(r -> r.method())
                    .toList();
            refResults.add(new RefImplementationResult(failedTests));
            allTests = testResults.stream()
                    .map(TestResult::method)
                    .toList();
            incorrectTests.addAll(failedTests);
        }

        var mutantResults = new ArrayList<MutantResult>();
        for (var mutation : task.mutations()) {
            var refImpl = task.refImplementationFor(mutation);
            var mutater = createMutator(refImpl);
            var mutated = mutater.getMutation(mutation.identifier()).getBytes();

            var classIndex = mutation.mutatedClassIndex();
            var className = refImpl.get(classIndex).getClassName();

            var classes = new ArrayList<>(refImpl);
            classes.set(classIndex, new InMemClassFile(className, mutated));

            var sandboxed = ClassPath.fromMemory(classes).withMemory(testSuite);
            var support = ClassPath.fromFiles(dependencies).withCurrent();
            var testRun = new TestRunner.Task(testClassNames, sandboxed, support, 1,
                    Duration.ofSeconds(2), Duration.ofSeconds(5), null, emptyList());
            var testResults = testRunner.run(testRun).testResults();
            var failedTests = testResults.stream()
                    .filter(r -> !r.passed())
                    .map(TestResult::method)
                    .filter(not(incorrectTests::contains))
                    .toList();
            mutantResults.add(new MutantResult(mutation, failedTests));
        }

        var mutantScore = mutantResults.stream()
                .filter(r -> !r.passed())
                .mapToDouble(r -> r.mutation().weight())
                .sum();

        return new Result(false, false, refResults, mutantResults, allTests, mutantScore);
    }

    private GregorMutater createMutator(List<InMemClassFile> refImpl) {
        ClassByteArraySource source = className -> {
            for (var file : refImpl) {
                // Pitest is inconsistent with slashes and dots, so check both
                if (file.getBinaryName().equals(className.replace('.', '/'))) {
                    return Optional.of(file.getContent());
                }
            }
            return Optional.empty();
        };
        return new GregorMutater(source, m -> true, Mutator.all());
    }

    @Override
    public void close() {
        testRunner.close();
    }

    public record Task(
            List<List<InMemClassFile>> refImplementations,
            List<Mutation> mutations,
            LinkedHashMap<TestMethod, String> refTestDescriptions) {

        public List<InMemClassFile> refImplementationFor(Mutation mutation) {
            return refImplementations.get(mutation.refImplementationIndex());
        }
    }

    public record Submission(List<InMemSource> testSuite) {
        public Submission {
            if (testSuite.isEmpty()) {
                throw new IllegalArgumentException("empty test suite");
            }
        }

        /**
         * Loads a submission containing all Java files in the given directory, matching the given
         * package filter. The filter is a prefix of the package name, e.g. "ch.trick17" to only
         * include classes in that package and its subpackages. If the filter is null, all classes
         * are included.
         */
        public static Submission loadFrom(Path testDir, String packageFilter) throws IOException {
            var root = packageFilter == null
                    ? testDir
                    : testDir.resolve(packageFilter.replace('.', '/'));
            try (var walk = Files.walk(root)) {
                var javaFiles = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".java"))
                        .toList();
                var testSuite = new ArrayList<InMemSource>();
                for (var file : javaFiles) {
                    testSuite.add(InMemSource.fromFile(file, testDir));
                }
                return new Submission(testSuite);
            }
        }
    }

    public record Result(
            boolean emptyTestSuite,
            boolean compilationFailed,
            List<RefImplementationResult> refImplementationResults,
            List<MutantResult> mutantResults,
            List<TestMethod> allTests,
            double mutantScore) {

        public Result {
            if (mutantScore < 0.0 || mutantScore > 1.0) {
                throw new IllegalArgumentException("invalid mutant score: " + mutantScore);
            }
        }

        public Set<TestMethod> incorrectTests() {
            return refImplementationResults.stream()
                    .flatMap(r -> r.failedTests().stream())
                    .collect(toSet());
        }
    }
}
