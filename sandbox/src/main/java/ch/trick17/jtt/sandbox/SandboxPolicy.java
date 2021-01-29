package ch.trick17.jtt.sandbox;

import java.net.URL;
import java.security.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class SandboxPolicy extends Policy {

    private final Set<URL> unrestrictedCode;

    public SandboxPolicy(Collection<URL> unrestrictedCode) {
        this.unrestrictedCode = new HashSet<>(unrestrictedCode);
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        return domain.getCodeSource() != null ||
                unrestrictedCode.contains(domain.getCodeSource().getLocation());
    }
}
