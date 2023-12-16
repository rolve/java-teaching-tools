package ch.trick17.jtt.sandbox;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.Set;

import static ch.trick17.jtt.sandbox.JavaSandbox.SANDBOX;
import static java.util.Set.copyOf;

class SandboxPolicy extends Policy {

    private final ThreadLocal<Set<Path>> unrestricted = new InheritableThreadLocal<>();

    public void activate(Collection<Path> unrestrictedCode) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SANDBOX);
        }
        unrestricted.set(copyOf(unrestrictedCode));
    }

    public void deactivate() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(SANDBOX);
        }
        unrestricted.set(null);
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission permission) {
        // very simple policy: all or nothing, based on code source
        if (domain.getCodeSource() == null || unrestricted.get() == null) {
            return true;
        }
        try {
            var location = Path.of(domain.getCodeSource().getLocation().toURI());
            return unrestricted.get().contains(location);
        } catch (URISyntaxException e) {
            return true;
        }
    }
}
