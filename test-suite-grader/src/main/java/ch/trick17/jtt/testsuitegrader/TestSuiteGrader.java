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
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.mutationtest.build.intercept.equivalent.EquivalentReturnMutationFilter;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.config.Mutator;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

import static ch.trick17.jtt.memcompile.Compiler.ECLIPSE;
import static ch.trick17.jtt.memcompile.Compiler.JAVAC;
import static ch.trick17.jtt.memcompile.InMemCompilation.compile;
import static ch.trick17.jtt.sandbox.Whitelist.DEFAULT_WHITELIST_DEF;
import static ch.trick17.jtt.testsuitegrader.TestSuiteWhitelists.*;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.*;
import static org.slf4j.LoggerFactory.getLogger;

public class TestSuiteGrader implements Closeable {

    // When grading untrusted test suites, we sandbox not only the
    // implementation code, but the test code as well, hence the need for a
    // more permissive whitelist than the default one.
    public static final String WHITELIST = DEFAULT_WHITELIST_DEF +
                                           JUNIT5_DEF +
                                           MOCKITO_DEF +
                                           SAFE_REFLECTION_DEF;

    private static final int PREPARE_REPETITIONS = 1;
    private static final int GRADE_REPETITIONS = 1;
    private static final Duration REP_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(5);

    private static final Logger logger = getLogger(TestSuiteGrader.class);

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
        var refTestSuiteString = refTestSuite.stream()
                .map(s -> s.getPath().replaceAll(".*/", ""))
                .collect(joining(", "));
        logger.info("Preparing task for reference test suite: {}", refTestSuiteString);
        var classPath = ClassPath.fromFiles(dependencies);
        var compiledImplementations = new ArrayList<List<InMemClassFile>>();
        for (int i = 0; i < refImplementations.size(); i++) {
            var implResult = compile(JAVAC, refImplementations.get(i), classPath);
            if (!implResult.errors().isEmpty()) {
                throw new IllegalArgumentException("Could not compile reference implementation " + (i + 1));
            } else if (implResult.output().isEmpty()) {
                throw new IllegalArgumentException("Empty reference implementation " + (i + 1));
            }
            compiledImplementations.add(implResult.output());
        }

        classPath = classPath.withMemory(compiledImplementations.getFirst()).withCurrent();
        var refTestSuiteResult = compile(JAVAC, refTestSuite, classPath);
        if (!refTestSuiteResult.errors().isEmpty()) {
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
            var sandboxed = ClassPath.fromMemory(refImpl).withMemory(compiledSuite);
            var support = ClassPath.fromFiles(dependencies).withCurrent();
            var testRun = new TestRunner.Task(testClassNames, sandboxed, support,
                    PREPARE_REPETITIONS, REP_TIMEOUT, TEST_TIMEOUT, WHITELIST);
            var refResults = testRunner.run(testRun).testResults();
            if (i == 0) {
                refResults.forEach(r -> tests.add(r.method()));
            }
            for (var result : refResults) {
                if (!result.passed()) {
                    throw new IllegalArgumentException("Reference implementation " + (i + 1) +
                                                       " failed test " + result.method() + ": " +
                                                       result.exceptions().getFirst());
                }
            }

            var mutants = generateMutants(refImpl, i);
            logger.info("Generated {} mutants for reference implementation {}",
                    mutants.size(), i + 1);
            var killed = killMutants(mutants, compiledSuite, dependencies);
            killedMutants.putAll(killed);
        }
        logger.info("{} mutants were killed by test suite", killedMutants.size());

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
        var ordered = descriptions.stream()
                .sorted(comparingInt(d -> tests.indexOf(d.method)))
                .toList();

        logger.info("Finished preparing task for reference test suite: {}", refTestSuiteString);
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
        for (var mutant : mutants) {
            try {
                var sandboxed = ClassPath.fromMemory(mutant.classes()).withMemory(testSuite);
                var support = ClassPath.fromFiles(dependencies).withCurrent();
                var testRun = new TestRunner.Task(testClassNames, sandboxed, support,
                        PREPARE_REPETITIONS, REP_TIMEOUT, TEST_TIMEOUT, WHITELIST);
                var mutantResults = testRunner.run(testRun).testResults();

                var nonDeterministic = mutantResults.stream()
                        .filter(r -> r.nonDeterm())
                        .map(TestResult::method)
                        .findAny().isPresent();
                var verifyError = mutantResults.stream()
                        .map(TestResult::exceptions)
                        .flatMap(List::stream)
                        .anyMatch(e -> e.className().equals("java.lang.VerifyError"));
                var failedTests = mutantResults.stream()
                        .filter(r -> !r.passed())
                        .map(TestResult::method)
                        .toList();
                if (nonDeterministic) {
                    logger.warn("Mutant produced non-deterministic test results: {}",
                            mutant.getDescription());
                } else if (verifyError) {
                    logger.warn("Mutant produced a VerifyError: {}",
                            mutant.getDescription());
                } else if (failedTests.isEmpty()) {
                    logger.warn("Mutant survived reference test suite: {}",
                            mutant.getDescription());
                } else {
                    killed.put(mutant, failedTests);
                }
            } catch (TestRunException e) {
                logger.warn("Mutant could not be tested against reference test suite: {}",
                        mutant.getDescription(), e);
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
                .collect(groupingBy(e -> e.getValue().getFirst(), mapping(Map.Entry::getKey, toList())));
        boolean first = true;
        for (var test : tests) {
            var kills = grouped.getOrDefault(test, emptyList()).size();
            var percentage = 100 * kills / mutants.size();
            logger.info("{} ({}%){} mutants killed by {}", kills,
                    percentage, !first ? " more" : "", test);
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

    private List<TestDescription> testDescriptions(List<InMemSource> testSuite) {
        var result = new ArrayList<TestDescription>();
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
                        result.add(new TestDescription(
                                new TestMethod(className.get(), methodName), description));
                    }
                }
            }
        }
        return result;
    }

    public Result grade(Task task, List<InMemSource> testSuite) throws IOException {
        return grade(task, testSuite, emptyList());
    }

    public Result grade(Task task, List<InMemSource> testSuite, List<Path> dependencies) throws IOException {
        var classPath = ClassPath.fromMemory(task.refImplementations().getFirst())
                .withFiles(dependencies)
                .withCurrent();
        if (testSuite.isEmpty()) {
            return new Result(true, false, emptyList(), emptyList(), emptyList(), 0.0);
        }
        var compileResult = compile(ECLIPSE, testSuite, classPath);
        if (!compileResult.errors().isEmpty() && compileResult.output().isEmpty()) {
            return new Result(false, true, emptyList(), emptyList(), emptyList(), 0.0);
        }
        var compiledSuite = compileResult.output();
        var testClassNames = compiledSuite.stream().map(f -> f.getClassName()).toList();

        var refResults = new ArrayList<TestResult>();
        List<TestMethod> allTests = null;
        var incorrectTests = new HashSet<TestMethod>();
        for (var impl : task.refImplementations()) {
            var sandboxed = ClassPath.fromMemory(impl).withMemory(compiledSuite);
            var support = ClassPath.fromFiles(dependencies).withCurrent();
            var testRun = new TestRunner.Task(testClassNames, sandboxed, support, GRADE_REPETITIONS,
                    REP_TIMEOUT, TEST_TIMEOUT, WHITELIST);
            var testResults = testRunner.run(testRun).testResults();
            refResults.addAll(testResults);

            allTests = testResults.stream()
                    .map(TestResult::method)
                    .toList();
            testResults.stream()
                    .filter(r -> !r.passed())
                    .forEach(r -> incorrectTests.add(r.method()));
        }

        var mutantResults = new ArrayList<MutantResult>();
        for (int i = 0; i < task.mutations().size(); i++) {
            var mutation = task.mutations().get(i);
            var refImpl = task.refImplementationFor(mutation);
            var mutater = createMutator(refImpl);
            var mutated = mutater.getMutation(mutation.identifier()).getBytes();

            var classIndex = mutation.mutatedClassIndex();
            var className = refImpl.get(classIndex).getClassName();

            var classes = new ArrayList<>(refImpl);
            classes.set(classIndex, new InMemClassFile(className, mutated));

            var sandboxed = ClassPath.fromMemory(classes).withMemory(compiledSuite);
            var support = ClassPath.fromFiles(dependencies).withCurrent();
            var testRun = new TestRunner.Task(testClassNames, sandboxed, support,
                    GRADE_REPETITIONS, REP_TIMEOUT, TEST_TIMEOUT, WHITELIST);
            var testResults = testRunner.run(testRun).testResults();
            // TODO: Do we need to collect more info (timeouts, etc.) here as well?
            var failedTests = testResults.stream()
                    .filter(r -> !r.passed())
                    .map(TestResult::method)
                    .filter(not(incorrectTests::contains))
                    .toList();
            mutantResults.add(new MutantResult(mutation, failedTests));
        }

        var killedMutantResults = mutantResults.stream()
                .filter(r -> !r.passed())
                .toList();
        double mutantScore;
        if (killedMutantResults.size() == mutantResults.size()) {
            // special case to ensure mutant score is 100% and not something
            // like 99.99999999999999%, which could be rounded down to 99%
            mutantScore = 1.0;
        } else {
            mutantScore = killedMutantResults.stream()
                    .mapToDouble(r -> r.mutation().weight())
                    .sum();
        }

        return new Result(false, false, refResults, mutantResults, allTests, mutantScore);
    }

    private GregorMutater createMutator(List<InMemClassFile> refImpl) {
        var fallbackSource = ClassloaderByteArraySource.fromContext();
        ClassByteArraySource source = className -> {
            for (var file : refImpl) {
                // Pitest is inconsistent with slashes and dots, so check both
                if (file.getBinaryName().equals(className.replace('.', '/'))) {
                    return Optional.of(file.getContent());
                }
            }
            return fallbackSource.getBytes(className);
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
            List<TestDescription> refTestDescriptions) {

        public List<InMemClassFile> refImplementationFor(Mutation mutation) {
            return refImplementations.get(mutation.refImplementationIndex());
        }
    }

    public record TestDescription(TestMethod method, String description) {}

    public record Result(
            boolean emptyTestSuite,
            boolean compilationFailed,
            List<TestResult> refImplementationResults,
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
                    .filter(r -> !r.passed())
                    .map(TestResult::method)
                    .collect(toSet());
        }

        public Result with(List<TestResult> refImplementationResults) {
            return new Result(emptyTestSuite, compilationFailed, refImplementationResults,
                              mutantResults, allTests, mutantScore);
        }
    }
}
