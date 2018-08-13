import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import ch.trick17.javaprocesses.JavaProcessBuilder;
import ch.trick17.javaprocesses.util.LineCopier;
import ch.trick17.javaprocesses.util.LineWriterAdapter;

public class Grader {

    private static final Pattern lines = Pattern.compile("\r?\n");

    @SuppressWarnings("serial")
	private static final List<Task> TASKS = new ArrayList<Task>() {{
        add(new Task("Aufgabe1", AbstandGradingTest.class, 99999 * 3/5));
        add(new Task("Aufgabe2", KombinationenGradingTest.class, 99999 * 3/5));
        add(new Task("Aufgabe3", WarenanalyseGradingTest.class, 99999 * 3/5));
        add(new Task("Aufgabe4", "a", CityBuilderGradingTest.class, 99999 * 3/5));
        add(new Task("Aufgabe4", "b", CityDeadEndsGradingTest.class, 99999 * 3/5));
        add(new Task("Aufgabe4", "c", CityReachableSquaresGradingTest.class, 99999 * 3/5));
    }};

    public static void main(String[] args) throws IOException {
        Path root = Paths.get(args[0]);
        new Grader(root).run();
    }

    private Path root;
    private Map<Task, Results> results;

    public Grader(Path root) {
        this.root = root;
        results = TASKS.stream().collect(toMap(t -> t, t -> new Results()));
    }

    private void run() throws IOException {
        List<Path> solutions = Files.list(root)
                .filter(Files::isDirectory)
                //.filter(s -> s.getFileName().toString().startsWith("aaa"))
                .sorted()
                .collect(toList());
        
        for (int i = 0; i < solutions.size(); i++) {
        	Path solution = solutions.get(i);
            System.out.println("Grading " + solution.getFileName() + " " + (i+1) + "/" + solutions.size());
			grade(solution);
			System.out.println();
        }

        for (Entry<Task, Results> entry : results.entrySet()) {
            entry.getValue().writeTo(Paths.get(entry.getKey().resultFileName()));
        }
    }

    private void grade(Path solution) throws IOException {
        for (Task task : TASKS) {
            gradeTask(solution, task);
        }
    }

    private void gradeTask(Path solution, Task task) throws IOException {
        Path projectPath = solution.resolve("results-clean").resolve(task.projectName);
        String student = solution.getFileName().toString();

        results.get(task).addStudent(student);
        boolean compiled = compileProject(projectPath);
        if (compiled) {
            results.get(task).addCriterion(student, "compiles");

            runTests(task, projectPath, student);
        }
    }

    private boolean compileProject(Path projectPath) throws IOException {
        String classpath = Paths.get("lib", "junit.jar").toAbsolutePath() + File.pathSeparator +
                Paths.get("lib","hamcrest.jar").toAbsolutePath() + File.pathSeparator +
                Paths.get("inspector.jar").toAbsolutePath();
        
        List<String> builderArgs = new ArrayList<>(Arrays.asList("javac", "-d", "bin", "-encoding", "UTF8", "-cp", classpath));
        builderArgs.addAll(Files.list(projectPath.resolve("src"))
        		.map(Path::toString)
        		.filter(f -> f.endsWith(".java"))
        		.collect(toList()));
        
        Process javac = new ProcessBuilder(builderArgs)
                .redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT)
                .directory(projectPath.toFile()).start();
        int returnCode = robustWaitFor(javac);
        return returnCode == 0;
    }

    private void runTests(Task task, Path projectPath, String student) throws IOException {
        String classes = Files.list(projectPath.resolve("src"))
                .map(p -> p.getFileName().toString())
                .filter(s -> s.endsWith(".java"))
                .map(s -> s.substring(0, s.length() - 5))
                .collect(joining(","));
        String agentArg = "-javaagent:inspector.jar=" + task.instrThreshold + "," + classes;

        JavaProcessBuilder jUnitBuilder = new JavaProcessBuilder(TestRunner.class, task.testClass.getName());
        jUnitBuilder.classpath(projectPath.resolve("bin") + File.pathSeparator + jUnitBuilder.classpath())
                .vmArgs("-Dfile.encoding=UTF8", agentArg);

        Process jUnit = jUnitBuilder.build()
                .redirectError(Redirect.INHERIT)
                .start();

        StringWriter jUnitOutput = new StringWriter();
        new LineCopier(jUnit.getInputStream(), new LineWriterAdapter(jUnitOutput)).run();

        lines.splitAsStream(jUnitOutput.toString()).forEach(line -> results.get(task).addCriterion(student, line));
    }

    private int robustWaitFor(Process javac) {
        int returnCode;
        while (true) {
            try {
                returnCode = javac.waitFor();
                break;
            } catch(InterruptedException e) {}
        }
        return returnCode;
    }
}
