package ch.trick17.jtt.sandbox;

import ch.trick17.jtt.sandbox.SimpleWhitelist.WhitelistEntry.NamedEntry;
import ch.trick17.jtt.sandbox.SimpleWhitelist.WhitelistEntry.SignatureEntry;
import ch.trick17.jtt.sandbox.SimpleWhitelist.WhitelistEntry.WildcardEntry;

import java.util.List;

import static java.lang.String.join;
import static java.util.Arrays.asList;

public class SimpleWhitelist implements Whitelist {

    private final List<? extends WhitelistEntry> entries;

    public SimpleWhitelist(String whitelistDef) {
        entries = whitelistDef.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> {
                    var paramsIndex = line.indexOf('(');
                    var classNameMember = paramsIndex == -1 ? line : line.substring(0, paramsIndex);
                    var lastDotIndex = classNameMember.lastIndexOf('.');
                    var cls = classNameMember.substring(0, lastDotIndex);
                    var member = classNameMember.substring(lastDotIndex + 1);
                    if (member.equals("*")) {
                        return new WildcardEntry(cls);
                    } else if (paramsIndex == -1) {
                        return new NamedEntry(cls, member);
                    } else {
                        var paramTypes = line
                                .substring(paramsIndex + 1, line.length() - 1)
                                .split(",");
                        return new SignatureEntry(cls, member, List.of(paramTypes));
                    }
                })
                .toList();
    }

    public boolean methodPermitted(String className, String methodName, List<String> paramTypes) {
        return entries.stream().anyMatch(e -> e.matchesMethod(className, methodName, paramTypes));
    }

    public boolean constructorPermitted(String className, List<String> paramTypes) {
        return entries.stream().anyMatch(e -> e.matchesConstructor(className, paramTypes));
    }

    sealed interface WhitelistEntry {
        boolean matchesMethod(String className, String methodName, List<String> paramTypes);

        boolean matchesConstructor(String className, List<String> paramTypes);

        record WildcardEntry(String className) implements WhitelistEntry {
            public boolean matchesMethod(String className, String methodName, List<String> paramTypes) {
                return this.className.equals(className);
            }

            public boolean matchesConstructor(String className, List<String> paramTypes) {
                return this.className.equals(className);
            }
        }

        record NamedEntry(String className,
                          String memberName) implements WhitelistEntry {
            public boolean matchesMethod(String className, String methodName, List<String> paramTypes) {
                return this.className.equals(className) && memberName.equals(methodName);
            }

            public boolean matchesConstructor(String className, List<String> paramTypes) {
                return this.className.equals(className) && memberName.equals("<init>");
            }
        }

        record SignatureEntry(String className,
                              String memberName,
                              List<String> paramTypes) implements WhitelistEntry {
            public boolean matchesMethod(String className, String methodName, List<String> paramTypes) {
                return this.className.equals(className)
                       && this.memberName.equals(methodName)
                       && this.paramTypes.equals(paramTypes);
            }

            public boolean matchesConstructor(String className, List<String> paramTypes) {
                return this.className.equals(className)
                       && this.memberName.equals("<init>")
                       && this.paramTypes.equals(paramTypes);
            }
        }
    }
}
