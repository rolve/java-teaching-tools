import ch.trick17.jtt.memcompile.Compiler;
import ch.trick17.jtt.memcompile.*;
import ch.trick17.jtt.sandbox.Sandbox;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static ch.trick17.jtt.memcompile.ClassPath.empty;
import static ch.trick17.jtt.sandbox.SandboxResult.Kind.NORMAL;
import static ch.trick17.jtt.sandbox.SandboxResult.Kind.TIMEOUT;

public class PitestTest {
    public static void main(String[] args) throws Exception {
        var source = InMemSource.fromString("""
                public class Hello {
                    public static void main(String[] args) {
                        var greeting = "";
                        for (int i = 0; i < 10; i++) {
                            greeting += "Hello, " + (char) ('A' + i) + "!\\n";
                        }
                        System.out.println(greeting);
                    }
                }
                """);
        var compileResult = InMemCompilation.compile(Compiler.JAVAC, List.of(source),
                ClassPath.empty(), System.out);
        var cls = compileResult.output().get(0);

        var byteSource = new ClassByteArraySource() {
            public Optional<byte[]> getBytes(String className) {
                return Optional.ofNullable(className.equals("Hello")
                        ? cls.getContent()
                        : null);
            }
        };
        var mutater = new GregorMutater(byteSource, m -> true, Mutator.all());
        var mutations = mutater.findMutations(ClassName.fromString("Hello"));

        for (int i = 0; i < mutations.size(); i++) {
            var mutation = mutations.get(i);
            var mutant = mutater.getMutation(mutation.getId()).getBytes();
            var classPath = ClassPath.fromMemory(List.of(new InMemClassFile("Hello", mutant)));
            var sandbox = new Sandbox.Builder(classPath, empty())
                    .timeout(Duration.ofSeconds(1))
                    .build();

            System.out.println("============ Mutation " + i
                               + " (" + mutation.getDescription()
                               + " on line " + mutation.getLineNumber()
                               + ") ============");
            var result = sandbox.run("Hello", "main", List.of(String[].class),
                    List.of((Object) new String[0]), Void.class);

            if (result.kind() == TIMEOUT) {
                System.out.println("Timeout!");
            } else if (result.kind() != NORMAL) {
                result.exception().printStackTrace(System.out);
            }
        }
    }
}
