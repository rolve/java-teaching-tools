import static java.io.File.pathSeparator;
import static java.io.File.separatorChar;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.ToolProvider.getSystemJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;

public class Grader {

    private static final Pattern lines = Pattern.compile("\r?\n");

    @SuppressWarnings("serial")
    private static final List<Task> TASKS = new ArrayList<Task>() {{
        add(new Task("Aufgabe 1", Summe.class, SummeGradingTest.class, 99999, 
                Set.of("Pair.java", "SummeGradingTest.java", "HardTimeout.java")));
        add(new Task("Aufgabe 2", Matrix.class, MatrixGradingTest.class, 99999, 
                Set.of("Position.java", "MatrixGradingTest.java", "HardTimeout.java")));
        add(new Task("Aufgabe 3", BestFit.class, BestFitGradingTest.class, 99999, 
                Set.of("BestFitGradingTest.java", "HardTimeout.java")));
        add(new Task("Aufgabe 4", "a", Inspektor.class, InspektorAGradingTest.class, 99999, 
                Set.of("InspektorAGradingTest.java", "HardTimeout.java")));
        add(new Task("Aufgabe 4", "b", Inspektor.class, InspektorBGradingTest.class, 99999, 
                Set.of("InspektorBGradingTest.java", "HardTimeout.java")));
        add(new Task("Aufgabe 5", "a", Rezept.class, RezeptAGradingTest.class, 99999, 
                Set.of("RezeptAGradingTest.java", "HardTimeout.java")));
        add(new Task("Aufgabe 5", "b", Rezept.class, RezeptBGradingTest.class, 99999, 
                Set.of("RezeptBGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 1", ZeroSum.class, ZeroSumGradingTest.class, 99999, 
//                Set.of("ZeroSumGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 2", Blocks.class, BlocksGradingTest.class, 99999, 
//                Set.of("BlocksGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 3", Nesting.class, NestingGradingTest.class, 99999, 
//                Set.of("NestingGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 4", "a", Service.class, ServiceAGradingTest.class, 99999, 
//                Set.of("ServiceAGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 4", "b", Service.class, ServiceBGradingTest.class, 99999, 
//                Set.of("ServiceBGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 5", "a", LinkedRingBuffer.class, LinkedRingBufferAGradingTest.class, 99999, 
//                Set.of("LinkedRingBufferAGradingTest.java", "HardTimeout.java")));
//        add(new Task("Aufgabe 5", "b", LinkedRingBuffer.class, LinkedRingBufferBGradingTest.class, 99999, 
//                Set.of("LinkedRingBufferBGradingTest.java", "HardTimeout.java")));
//        add(new Task("u04", StringAddition.class, StringAdditionGradingTest.class, 99999 * 3/5,
//                Set.of("StringAdditionGradingTest.java", "ByteCodeParseGradingTest.java", "HardTimeout.java")));
//        add(new Task("u05", Vermietung.class, VermietungGradingTest.class, 99999 * 3/5,
//                Set.of("VermietungGradingTest.java", "HardTimeout.java")));
//        add(new Task("u06", Tal.class, TalGradingTest.class, 99999 * 3/5,
//        		Set.of("TalGradingTest.java", "HardTimeout.java")));
//        add(new Task("u07", Verzahnung.class, VerzahnungGradingTest.class, 99999 * 3/5,
//        		Set.of("VerzahnungGradingTest.java", "HardTimeout.java")));
//        add(new Task("u08", LinkedIntList.class, LinkedIntListGradingTest.class, 99999 * 3 / 5,
//              Set.of("LinkedIntListGradingTest.java", "HardTimeout.java")));
//        add(new Task("u09", KlassenGradingTest.class, KlassenGradingTest.class, 99999 * 3 / 5,
//              Set.of("KlassenGradingTest.java", "HardTimeout.java")));
//        add(new Task("u10", Rechner.class, RechnerGradingTest.class, 99999 * 3 / 5,
//              Set.of("RechnerGradingTest.java", "HardTimeout.java")));
//        add(new Task("u11", Bienen.class, BienenGradingTest.class, 99999 * 3 / 5,
//              Set.of("BienenGradingTest.java", "HardTimeout.java")));
//        add(new Task("u12", Palindrome.class, PalindromeGradingTest.class, 99999 * 3 / 5,
//              Set.of("PalindromeGradingTest.java", "HardTimeout.java")));
    }};

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args[0]);
        new Grader(root).run();
    }

    private Path root;
    private Map<Task, Results> results;

    public Grader(Path root) {
        this.root = root;
        results = TASKS.stream().collect(toUnmodifiableMap(t -> t, t -> new Results()));
    }

    private void run() throws IOException {
        List<Path> solutions = Files.list(root)
                .filter(Files::isDirectory)
                //.filter(s -> Set.of("solution").contains(s.getFileName().toString()))
                .sorted()
                .collect(toList());

        long startTime = System.currentTimeMillis();
        AtomicInteger i = new AtomicInteger(0);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");
        solutions.stream().parallel().forEach(solution -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(baos);

            out.println("Grading " + solution.getFileName());
            grade(solution, out);
            if (Math.random() > 0.75) {
                writeResultsToFile();
            }

            out.println("Graded " + i.incrementAndGet() + "/" + solutions.size() +
                    " Total Time: " + ((System.currentTimeMillis() - startTime) / 1000) + " s");
            out.println();
            System.out.println(baos);
        });

        writeResultsToFile();
        System.out.println(solutions.size() + " solutions processed");
    }

	private synchronized void writeResultsToFile() {
        for (Entry<Task, Results> entry : results.entrySet()) {
            try {
                entry.getValue().writeTo(Paths.get(entry.getKey().resultFileName()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void grade(Path solution, PrintStream out) {
        for (Task task : TASKS) {
            gradeTask(solution, task, out);
        }
    }

    private void gradeTask(Path solution, Task task, PrintStream out) {
        Path projectPath = solution.resolve(task.projectName);
        String student = solution.getFileName().toString();

        results.get(task).addStudent(student);
        boolean compiled = compileProject(projectPath, task, out);
        if (compiled) {
            results.get(task).addCriterion(student, "compiles");

            runTests(task, projectPath, student, out);
        }
    }

    private boolean compileProject(Path projectPath, Task task, PrintStream out) {
        Path srcPath = projectPath.resolve("src").toAbsolutePath();
        Set<Path> sources;
        try {
            // remove any pre-compiled class files from bin/
            Path binPath = projectPath.resolve("bin");
            Files.createDirectories(binPath);
            Files.list(binPath)
                .map(Path::toFile)
                .filter(File::isFile)
                .forEach(f -> f.delete());

            // Copy GradingTests class into student's src/
            // Create src directory in case it doesn't exist (yes, it happened)
            createDirectories(srcPath);
            for (String f : task.filesToCopy) {
                Path filePath = Paths.get("tests", f).toAbsolutePath();
                Files.copy(filePath, srcPath.resolve(f), REPLACE_EXISTING);
            }

            sources = new HashSet<>(Files.walk(srcPath)
                    .filter(f -> f.toString().endsWith(".java"))
                    .collect(toSet()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String classpath = Paths.get("lib", "junit.jar").toAbsolutePath() + pathSeparator +
                Paths.get("lib","hamcrest.jar").toAbsolutePath() + pathSeparator +
                Paths.get("lib","asm-7.0.jar").toAbsolutePath() + pathSeparator +
                Paths.get("inspector.jar").toAbsolutePath();

        var javac = getSystemJavaCompiler();

        while (true) {
            var collector = new DiagnosticCollector<>();
            var manager = javac.getStandardFileManager(collector, null, UTF_8);

            var options = asList(
                    "-cp", classpath,
                    "-d", projectPath.resolve("bin").toString());
            javac.getTask(null, manager, collector, options, null,
                    manager.getJavaFileObjectsFromPaths(sources)).call();

            var errors = collector.getDiagnostics().stream()
                    .filter(d -> d.getKind() == ERROR).collect(toList());
            if (errors.isEmpty()) {
                return true;
            }

            var faultyFiles = errors.stream()
                    .map(d -> (JavaFileObject) d.getSource())
                    .map(f -> srcPath.relativize(Paths.get(f.getName())).toString())
                    .collect(toSet());
			if (faultyFiles.remove(task.classUnderTest.getName().replace('.', separatorChar) + ".java")) {
				// never remove class under test from compile arguments
				out.println("Class under test has errors.");
			}
			if (faultyFiles.removeAll(task.filesToCopy)) {
				// copy-in files should *never* have errors
			    out.println("WARNING: One of " + task.filesToCopy + " had a compile error.\n");
			}

			errors.forEach(out::println);

			if (faultyFiles.isEmpty()) {
				// no files left to remove. unable to compile.
				return false;
			} else {
    			var faulty = faultyFiles.stream().findFirst().get();
    			out.printf("%s appears to be faulty. Ignoring it for compilation.\n", faulty);
    			sources.remove(srcPath.resolve(faulty));
			}
        }
    }

    private void runTests(Task task, Path projectPath, String student, PrintStream out) {
        try {
            List<String> classes = Files.list(projectPath.resolve("src"))
                    .map(p -> p.getFileName().toString())
                    .filter(s -> s.endsWith(".java"))
                    .map(s -> s.substring(0, s.length() - 5))
                    .collect(toList());
            String agentArg = "-javaagent:inspector.jar=" + task.instrThreshold + "," +
                    classes.stream().collect(joining(","));
    
            List<String> junitArgs = new ArrayList<>(classes);
            junitArgs.add(0, task.testClass.getName());
            JavaProcessBuilder jUnitBuilder = new JavaProcessBuilder(TestRunner.class, junitArgs);
            jUnitBuilder.classpath(projectPath.resolve("bin") + File.pathSeparator + jUnitBuilder.classpath())
                    .vmArgs("-Dfile.encoding=UTF8", agentArg, "-XX:-OmitStackTraceInFastThrow");
    
            Process jUnit = jUnitBuilder.build().start();
    
            StringWriter jUnitOutput = new StringWriter();
            Thread outCopier = new Thread(new LineCopier(jUnit.getInputStream(),
                    new LineWriterAdapter(jUnitOutput)));
            Thread errCopier = new Thread(new LineCopier(jUnit.getErrorStream(),
                    new LineWriterAdapter(out)));
            outCopier.start();
            errCopier.start();
    
            while (true) {
                try {
                    outCopier.join();
                    errCopier.join();
                    break;
                } catch (InterruptedException e) {}
            }
            lines.splitAsStream(jUnitOutput.toString())
            		.filter(line -> !line.isEmpty())
    				.forEach(line -> results.get(task).addCriterion(student, line));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
