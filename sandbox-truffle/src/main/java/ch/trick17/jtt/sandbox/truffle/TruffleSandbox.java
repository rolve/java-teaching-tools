package ch.trick17.jtt.sandbox.truffle;

import ch.trick17.jtt.sandbox.Sandbox;
import ch.trick17.jtt.sandbox.SandboxResult;
import org.graalvm.polyglot.Context;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.io.File.pathSeparator;
import static java.util.stream.Stream.concat;

public class TruffleSandbox extends Sandbox {

    @Override
    public <T> SandboxResult<T> run(List<URL> restrictedCode, List<URL> unrestrictedCode,
                                    String className, String methodName,
                                    List<Class<?>> paramTypes, List<?> args, Class<T> resultType) {
        var classpath = concat(restrictedCode.stream(), unrestrictedCode.stream())
                .map(this::toPath)
                .collect(Collectors.joining(pathSeparator));

        var context = Context.newBuilder()
                .allowNativeAccess(true)
                .allowCreateThread(true)
                .allowAllAccess(!permRestrictions)
                // TODO: I/O
                .option("java.Classpath", classpath)
                .build();

        try {
            var result = context.getBindings("java").getMember(className)
                    .invokeMember(methodName, args.toArray());
            return SandboxResult.normal(result.as(resultType));
        } catch (Throwable t) {
            return SandboxResult.exception(t);
        }
    }

    private String toPath(URL url) {
        try {
            return Path.of(url.toURI()).toAbsolutePath().toString();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }
}
