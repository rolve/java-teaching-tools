package ch.trick17.jtt.memcompile;

import javax.tools.SimpleJavaFileObject;
import java.io.*;
import java.net.URI;

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
}
