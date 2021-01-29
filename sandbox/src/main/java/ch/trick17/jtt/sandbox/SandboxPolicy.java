package ch.trick17.jtt.sandbox;

import java.net.URL;
import java.security.*;
import java.util.Collection;
import java.util.Set;

import static java.util.Set.copyOf;

class SandboxPolicy extends Policy {

    // could be anything at the moment, as the policy is all-or-nothing anyway
    private static final Permission SANDBOX = new RuntimePermission("sandbox");

    private final ThreadLocal<Set<URL>> unrestricted = new InheritableThreadLocal<>();

    public void activate(Collection<URL> unrestrictedCode) {
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
        return domain.getCodeSource() == null
                || unrestricted.get() == null
                || unrestricted.get().contains(domain.getCodeSource().getLocation());
    }
}
