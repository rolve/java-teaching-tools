package ch.trick17.jtt.sandbox;

public interface Whitelist {

    String DEFAULT_WHITELIST_DEF = """
                java.lang.Byte.*
                java.lang.Short.*
                java.lang.Integer.*
                java.lang.Long.*
                java.lang.Float.*
                java.lang.Double.*
                java.lang.Boolean.*
                java.lang.Character.*
                
                java.lang.String.*
                java.lang.Math.*
                java.lang.Iterable.*
                java.lang.Comparable.*
                
                java.lang.Throwable.*
                java.lang.Error.*
                java.lang.Exception.*
                java.lang.RuntimeException.*
                java.lang.IllegalArgumentException.*
                java.lang.IllegalStateException.*
                
                java.lang.System.currentTimeMillis
                java.lang.System.nanoTime
                
                java.util.Random.*
                java.util.Objects.*
                
                java.util.Collection.*
                java.util.Iterator.*
                java.util.List.*
                java.util.ArrayList.*
                java.util.LinkedList.*
                java.util.Set.*
                java.util.HashSet.*
                java.util.LinkedHashSet.*
                java.util.TreeSet.*
                java.util.Map.*
                java.util.HashMap.*
                java.util.LinkedHashMap.*
                java.util.TreeMap.*
                
                java.util.Arrays.*
                java.util.Collections.*
                
                java.util.Optional.*
                java.util.OptionalInt.*
                java.util.OptionalLong.*
                java.util.OptionalDouble.*
                
                java.util.stream.Stream.*
                java.util.stream.IntStream.*
                java.util.stream.LongStream.*
                java.util.stream.DoubleStream.*
                java.util.stream.Collectors.*
                
                java.util.regex.Pattern.*
                java.util.regex.Matcher.*
                
                java.io.IOException.*
                java.io.InputStream.*
                java.io.OutputStream.*
                java.io.BufferedReader.*
                java.io.BufferedWriter.*
                java.io.PrintStream.print
                java.io.PrintStream.printf
                java.io.PrintStream.println
                """;

    static Whitelist getDefault() {
        return parse(DEFAULT_WHITELIST_DEF);
    }

    static Whitelist parse(String whitelistDef) {
        return new SimpleWhitelist(whitelistDef);
    }

    boolean methodPermitted(String className, String methodName);
    boolean constructorPermitted(String className);
}
