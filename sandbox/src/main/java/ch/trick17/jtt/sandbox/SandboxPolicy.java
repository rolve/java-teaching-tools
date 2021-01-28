package ch.trick17.jtt.sandbox;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class SandboxPolicy extends Policy {

    private final Set<URL> restrictedCode;

    public SandboxPolicy(Collection<URL> restrictedCode) {
        this.restrictedCode = new HashSet<>(restrictedCode);
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        return domain.getCodeSource() == null ||
                !restrictedCode.contains(domain.getCodeSource().getLocation());
    }
}
