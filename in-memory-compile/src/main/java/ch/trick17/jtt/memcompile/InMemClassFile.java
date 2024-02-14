package ch.trick17.jtt.memcompile;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

import static javax.tools.JavaFileObject.Kind.CLASS;

public class InMemClassFile extends SimpleJavaFileObject {

    private final String className;
    private byte[] content;

    public InMemClassFile(String className) {
        super(URI.create("mem:///" + className.replace('.', '/') + ".class"), CLASS);
        this.className = className;
    }

    public InMemClassFile(String className, byte[] content) {
        this(className);
        this.content = content;
    }

    public String getClassName() {
        return className;
    }

    public String getBinaryName() {
        return className.replace('.', '/');
    }

    public String getSimpleName() {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    public String getPackageName() {
        var lastDot = className.lastIndexOf('.');
        return lastDot == -1 ? "" : className.substring(0, lastDot);
    }

    @Override
    public InputStream openInputStream() {
        if (content == null) {
            throw new IllegalStateException("no content");
        }
        return new ByteArrayInputStream(content);
    }

    @Override
    public OutputStream openOutputStream() {
        if (content != null) {
            throw new IllegalStateException("content already set");
        }
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                content = toByteArray();
            }
        };
    }

    public byte[] getContent() {
        if (content == null) {
            throw new IllegalStateException("no content");
        }
        return content;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof InMemClassFile other
               && className.equals(other.className)
               && Arrays.equals(content, other.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, Arrays.hashCode(content));
    }
}
