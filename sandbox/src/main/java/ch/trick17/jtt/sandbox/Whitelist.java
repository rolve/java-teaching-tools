package ch.trick17.jtt.sandbox;

import java.util.List;

public interface Whitelist {

    String DEFAULT_WHITELIST_DEF = """
                java.lang.Object.*
                
                java.lang.Byte.*
                java.lang.Short.*
                java.lang.Integer.*
                java.lang.Long.*
                java.lang.Float.*
                java.lang.Double.*
                java.lang.Boolean.*
                java.lang.Character.*
                
                java.lang.Number.*
                java.lang.CharSequence.*
                java.lang.String.*
                java.lang.StringBuilder.*
                java.lang.Math.*
                java.lang.Iterable.*
                java.lang.Comparable.*
                java.lang.Runnable.*
                java.lang.Enum.*
                
                java.lang.Throwable.*
                java.lang.Error.*
                java.lang.Exception.*
                java.lang.RuntimeException.*
                java.lang.ArithmeticException.*
                java.lang.ArrayIndexOutOfBoundsException.*
                java.lang.AssertionError.*
                java.lang.IllegalArgumentException.*
                java.lang.IllegalStateException.*
                java.lang.IndexOutOfBoundsException.*
                java.lang.InterruptedException.*
                java.lang.NullPointerException.*
                java.lang.NumberFormatException.*
                java.lang.StringIndexOutOfBoundsException.*
                java.lang.UnsupportedOperationException.*
                
                java.lang.System.arraycopy
                java.lang.System.currentTimeMillis
                java.lang.System.identityHashCode
                java.lang.System.lineSeparator
                java.lang.System.nanoTime
                
                java.lang.Class.toGenericString
                java.lang.Class.isInstance
                java.lang.Class.isAssignableFrom
                java.lang.Class.isInterface
                java.lang.Class.isArray
                java.lang.Class.isPrimitive
                java.lang.Class.isAnnotation
                java.lang.Class.isSynthetic
                java.lang.Class.getName
                java.lang.Class.getSuperclass
                java.lang.Class.getPackageName
                java.lang.Class.getInterfaces
                java.lang.Class.getComponentType
                java.lang.Class.getModifiers
                java.lang.Class.getDeclaringClass
                java.lang.Class.getEnclosingClass
                java.lang.Class.getSimpleName
                java.lang.Class.getTypeName
                java.lang.Class.getCanonicalName
                java.lang.Class.isAnonymousClass
                java.lang.Class.isLocalClass
                java.lang.Class.isMemberClass
                java.lang.Class.desiredAssertionStatus
                java.lang.Class.isEnum
                java.lang.Class.isRecord
                java.lang.Class.cast
                java.lang.Class.asSubclass
                java.lang.Class.descriptorString
                java.lang.Class.componentType
                java.lang.Class.arrayType
                java.lang.Class.isHidden
                java.lang.Class.getPermittedSubclasses
                java.lang.Class.isSealed
                
                java.util.Objects.*
                java.util.Random.*
                java.util.Arrays.*
                java.util.Collections.*
                java.util.Comparator.*
                java.util.StringTokenizer.*
                
                java.util.Collection.*
                java.util.Iterator.*
                java.util.List.*
                java.util.ListIterator.*
                java.util.ArrayList.*
                java.util.LinkedList.*
                java.util.Set.*
                java.util.HashSet.*
                java.util.LinkedHashSet.*
                java.util.TreeSet.*
                java.util.Map.*
                java.util.Map$Entry.*
                java.util.HashMap.*
                java.util.LinkedHashMap.*
                java.util.TreeMap.*
                java.util.Queue.*
                java.util.Deque.*
                java.util.ArrayDeque.*
                java.util.Stack.*
                java.util.Vector.*
                java.util.EnumSet.*
                java.util.EnumMap.*
                java.util.NoSuchElementException.*
                
                java.util.Optional.*
                java.util.OptionalInt.*
                java.util.OptionalLong.*
                java.util.OptionalDouble.*
                
                java.util.Scanner.<init>(java.lang.Readable)
                java.util.Scanner.<init>(java.io.InputStream)
                java.util.Scanner.<init>(java.io.InputStream,java.lang.String)
                java.util.Scanner.<init>(java.io.InputStream,java.nio.charset.Charset)
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
                
                java.util.concurrent.Callable.*
                java.util.concurrent.ConcurrentMap.*
                java.util.concurrent.ConcurrentHashMap.*
                
                java.util.concurrent.atomic.AtomicBoolean.*
                java.util.concurrent.atomic.AtomicInteger.*
                java.util.concurrent.atomic.AtomicIntegerArray.*
                java.util.concurrent.atomic.AtomicLong.*
                java.util.concurrent.atomic.AtomicLongArray.*
                java.util.concurrent.atomic.AtomicReference.*
                java.util.concurrent.atomic.AtomicReferenceArray.*
                java.util.concurrent.atomic.DoubleAccumulator.*
                java.util.concurrent.atomic.DoubleAdder.*
                java.util.concurrent.atomic.LongAccumulator.*
                java.util.concurrent.atomic.LongAdder.*
                
                java.util.function.BiConsumer.*
                java.util.function.BiFunction.*
                java.util.function.BinaryOperator.*
                java.util.function.BiPredicate.*
                java.util.function.BooleanSupplier.*
                java.util.function.Consumer.*
                java.util.function.DoubleBinaryOperator.*
                java.util.function.DoubleConsumer.*
                java.util.function.DoubleFunction.*
                java.util.function.DoublePredicate.*
                java.util.function.DoubleSupplier.*
                java.util.function.DoubleToIntFunction.*
                java.util.function.DoubleToLongFunction.*
                java.util.function.DoubleUnaryOperator.*
                java.util.function.Function.*
                java.util.function.IntBinaryOperator.*
                java.util.function.IntConsumer.*
                java.util.function.IntFunction.*
                java.util.function.IntPredicate.*
                java.util.function.IntSupplier.*
                java.util.function.IntToDoubleFunction.*
                java.util.function.IntToLongFunction.*
                java.util.function.IntUnaryOperator.*
                java.util.function.LongBinaryOperator.*
                java.util.function.LongConsumer.*
                java.util.function.LongFunction.*
                java.util.function.LongPredicate.*
                java.util.function.LongSupplier.*
                java.util.function.LongToDoubleFunction.*
                java.util.function.LongToIntFunction.*
                java.util.function.LongUnaryOperator.*
                java.util.function.ObjDoubleConsumer.*
                java.util.function.ObjIntConsumer.*
                java.util.function.ObjLongConsumer.*
                java.util.function.Predicate.*
                java.util.function.Supplier.*
                java.util.function.ToDoubleBiFunction.*
                java.util.function.ToDoubleFunction.*
                java.util.function.ToIntBiFunction.*
                java.util.function.ToIntFunction.*
                java.util.function.ToLongBiFunction.*
                java.util.function.ToLongFunction.*
                java.util.function.UnaryOperator.*
                
                java.util.stream.Stream.*
                java.util.stream.IntStream.*
                java.util.stream.LongStream.*
                java.util.stream.DoubleStream.*
                java.util.stream.Collectors.*
                
                java.util.regex.Matcher.*
                java.util.regex.MatchResult.*
                java.util.regex.Pattern.*
                java.util.regex.PatternSyntaxException.*
                
                java.text.Format.*
                java.text.Format$Field.*
                java.text.FieldPosition.*
                java.text.DateFormat.*
                java.text.DateFormat$Field.*
                java.text.DateFormatSymbols.*
                java.text.SimpleDateFormat.*
                java.text.MessageFormat.*
                java.text.MessageFormat$Field.*
                java.text.NumberFormat.*
                java.text.NumberFormat$Field.*
                java.text.ChoiceFormat.*
                java.text.DecimalFormat.*
                java.text.DecimalFormatSymbols.*
                
                java.io.IOException.*
                java.io.UncheckedIOException.*
                java.io.Closeable.*
                java.io.InputStream.*
                java.io.OutputStream.*
                java.io.ByteArrayInputStream.*
                java.io.ByteArrayOutputStream.*
                java.io.PrintStream.append
                java.io.PrintStream.charset
                java.io.PrintStream.checkError
                java.io.PrintStream.close
                java.io.PrintStream.flush
                java.io.PrintStream.format
                java.io.PrintStream.print
                java.io.PrintStream.printf
                java.io.PrintStream.println
                java.io.PrintStream.write
                java.io.PrintStream.writeBytes
                java.io.Reader.*
                java.io.Writer.*
                java.io.InputStreamReader.*
                java.io.OutputStreamWriter.*
                java.io.BufferedReader.*
                java.io.BufferedWriter.*
                
                java.nio.Buffer.*
                java.nio.BufferOverflowException.*
                java.nio.BufferUnderflowException.*
                java.nio.ByteBuffer.*
                java.nio.ByteOrder.*
                java.nio.CharBuffer.*
                java.nio.DoubleBuffer.*
                java.nio.FloatBuffer.*
                java.nio.IntBuffer.*
                java.nio.InvalidMarkException.*
                java.nio.LongBuffer.*
                java.nio.ReadOnlyBufferException.*
                java.nio.ShortBuffer.*
                
                java.nio.charset.CharacterCodingException.*
                java.nio.charset.Charset.*
                java.nio.charset.CharsetDecoder.*
                java.nio.charset.CharsetEncoder.*
                java.nio.charset.CoderMalfunctionError.*
                java.nio.charset.CoderResult.*
                java.nio.charset.CodingErrorAction.*
                java.nio.charset.IllegalCharsetNameException.*
                java.nio.charset.MalformedInputException.*
                java.nio.charset.StandardCharsets.*
                java.nio.charset.UnmappableCharacterException.*
                java.nio.charset.UnsupportedCharsetException.*
                
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
                
                java.time.Clock.*
                java.time.DateTimeException.*
                java.time.DayOfWeek.*
                java.time.Duration.*
                java.time.Instant.*
                java.time.InstantSource.*
                java.time.LocalDate.*
                java.time.LocalDateTime.*
                java.time.LocalTime.*
                java.time.Month.*
                java.time.MonthDay.*
                java.time.OffsetDateTime.*
                java.time.OffsetTime.*
                java.time.Period.*
                java.time.Year.*
                java.time.YearMonth.*
                java.time.ZonedDateTime.*
                java.time.ZoneId.*
                java.time.ZoneOffset.*
                
                java.time.chrono.Chronology.*
                java.time.chrono.Era.*
                java.time.chrono.IsoChronology.*
                java.time.chrono.IsoEra.*
                
                java.time.format.DateTimeFormatter.*
                java.time.format.DateTimeFormatterBuilder.*
                java.time.format.DateTimeParseException.*
                java.time.format.DateTimeParseException.*
                java.time.format.DecimalStyle.*
                java.time.format.FormatStyle.*
                java.time.format.ResolverStyle.*
                java.time.format.SignStyle.*
                java.time.format.TextStyle.*
                
                java.time.temporal.ChronoField.*
                java.time.temporal.ChronoUnit.*
                java.time.temporal.IsoFields.*
                java.time.temporal.JulianFields.*
                java.time.temporal.Temporal.*
                java.time.temporal.TemporalAccessor.*
                java.time.temporal.TemporalAdjuster.*
                java.time.temporal.TemporalAdjusters.*
                java.time.temporal.TemporalAmount.*
                java.time.temporal.TemporalField.*
                java.time.temporal.TemporalQueries.*
                java.time.temporal.TemporalQuery.*
                java.time.temporal.TemporalUnit.*
                java.time.temporal.UnsupportedTemporalTypeException.*
                java.time.temporal.ValueRange.*
                java.time.temporal.WeekFields.*
                
                java.time.zone.ZoneOffsetTransition.*
                java.time.zone.ZoneOffsetTransitionRule.*
                java.time.zone.ZoneOffsetTransitionRule$TimeDefinition.*
                java.time.zone.ZoneRules.*
                java.time.zone.ZoneRulesException.*
                java.time.zone.ZoneRulesProvider.*
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
