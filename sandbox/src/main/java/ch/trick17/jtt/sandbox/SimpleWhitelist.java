package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.sandbox.SimpleWhitelist.WhitelistEntry.NamedEntry;
import ch.trick17.jtt.sandbox.SimpleWhitelist.WhitelistEntry.WildcardEntry;

import java.util.List;

import static java.lang.String.join;
import static java.util.Arrays.asList;

public class SimpleWhitelist implements Whitelist {

    private final List<? extends WhitelistEntry> entries;

    public SimpleWhitelist(String whitelistDef) {
        entries = whitelistDef.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    var parts = asList(line.split("\\."));
                    var className = join(".", parts.subList(0, parts.size() - 1));
                    var memberName = parts.get(parts.size() - 1);
                    if (parts.size() > 1 && memberName.equals("*")) {
                        return new WildcardEntry(className);
                    } else {
                        return new NamedEntry(className, memberName);
                    }
                })
                .toList();
    }

    public boolean methodPermitted(String className, String methodName) {
        return entries.stream().anyMatch(e -> e.matchesMethod(className, methodName));
    }

    public boolean constructorPermitted(String className) {
        return entries.stream().anyMatch(e -> e.matchesConstructor(className));
    }

    sealed interface WhitelistEntry {
        boolean matchesMethod(String className, String methodName);
        boolean matchesConstructor(String className);

        record WildcardEntry(String className) implements WhitelistEntry {
            public boolean matchesMethod(String className, String methodName) {
                return this.className.equals(className);
            }
            public boolean matchesConstructor(String className) {
                return this.className.equals(className);
            }
        }

        record NamedEntry(String className,
                          String memberName) implements WhitelistEntry {
            public boolean matchesMethod(String className, String methodName) {
                return this.className.equals(className) && memberName.equals(methodName);
            }
            public boolean matchesConstructor(String className) {
                return this.className.equals(className) && memberName.equals("<init>");
            }
        }
    }
}
