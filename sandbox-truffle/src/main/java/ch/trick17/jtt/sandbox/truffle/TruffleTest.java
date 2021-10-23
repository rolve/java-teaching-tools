package ch.trick17.jtt.sandbox.truffle;

import org.graalvm.polyglot.Context;

import java.util.Random;
import java.util.stream.IntStream;

public class TruffleTest {
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            var start = System.nanoTime();

            var context = Context.newBuilder()
                    .allowNativeAccess(true)
                    .allowCreateThread(true)
                    .option("java.Classpath", System.getProperty("java.class.path"))
                    .build();
            context.getBindings("java").getMember(Guest.class.getName())
                    .invokeMember("main", (Object) null);

            var time = (System.nanoTime() - start) / 1_000_000_000.0;
            System.out.printf("%.2f s (host)\n", time);
        }
    }

    public static class Guest {

        public static void main(String[] args) {
            var start = System.nanoTime();

            var random = new Random();
            IntStream.generate(() -> random.nextInt(1_000_000))
                    .filter(Guest::isPrime)
                    .limit(1000)
                    .forEach(System.out::println);

            var time = (System.nanoTime() - start) / 1_000_000_000.0;
            System.out.printf("\n%.2f s (guest)\n", time);
        }

        public static boolean isPrime(int number) {
            int sqrt = (int) Math.sqrt(number) + 1;
            for (int i = 2; i < sqrt; i++) {
                if (number % i == 0) {
                    return false;
                }
            }
            return true;
        }
    }
}
