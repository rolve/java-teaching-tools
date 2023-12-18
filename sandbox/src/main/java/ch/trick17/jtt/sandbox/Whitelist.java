package ch.trick17.jtt.sandbox;

import java.util.List;

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
                
                java.util.Scanner.<init>(java.lang.Readable)
                java.util.Scanner.<init>(java.io.InputStream)
                java.util.Scanner.<init>(java.io.InputStream, java.lang.String)
                java.util.Scanner.<init>(java.io.InputStream, java.nio.charset.Charset)
                java.util.Scanner.<init>(java.lang.String)
                java.util.Scanner.close
                java.util.Scanner.ioException
                java.util.Scanner.delimiter
                java.util.Scanner.useDelimiter
                java.util.Scanner.locale
                java.util.Scanner.useLocale
                java.util.Scanner.radix
                java.util.Scanner.useRadix
                java.util.Scanner.match
                java.util.Scanner.toString
                java.util.Scanner.hasNext
                java.util.Scanner.next
                java.util.Scanner.hasNext
                java.util.Scanner.hasNextLine
                java.util.Scanner.nextLine
                java.util.Scanner.findInLine
                java.util.Scanner.findWithinHorizon
                java.util.Scanner.skip
                java.util.Scanner.hasNextBoolean
                java.util.Scanner.nextBoolean
                java.util.Scanner.hasNextByte
                java.util.Scanner.nextByte
                java.util.Scanner.hasNextShort
                java.util.Scanner.nextShort
                java.util.Scanner.hasNextInt
                java.util.Scanner.nextInt
                java.util.Scanner.hasNextLong
                java.util.Scanner.nextLong
                java.util.Scanner.hasNextFloat
                java.util.Scanner.nextFloat
                java.util.Scanner.hasNextDouble
                java.util.Scanner.nextDouble
                java.util.Scanner.reset
                java.util.Scanner.tokens
                java.util.Scanner.findAll
                
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
                
                java.nio.file.Path.of
                java.nio.file.Path.isAbsolute
                java.nio.file.Path.getRoot
                java.nio.file.Path.getFileName
                java.nio.file.Path.getParent
                java.nio.file.Path.getNameCount
                java.nio.file.Path.getName
                java.nio.file.Path.subpath
                java.nio.file.Path.startsWith
                java.nio.file.Path.endsWith
                java.nio.file.Path.normalize
                java.nio.file.Path.resolve
                java.nio.file.Path.resolveSibling
                java.nio.file.Path.relativize
                java.nio.file.Path.iterator
                java.nio.file.Path.compareTo
                java.nio.file.Path.equals
                java.nio.file.Path.hashCode
                java.nio.file.Path.toString
                """;

    static Whitelist getDefault() {
        return parse(DEFAULT_WHITELIST_DEF);
    }

    static Whitelist parse(String whitelistDef) {
        return new SimpleWhitelist(whitelistDef);
    }

    boolean methodPermitted(String className, String methodName, List<String> paramTypes);
    boolean constructorPermitted(String className, List<String> paramTypes);
}
