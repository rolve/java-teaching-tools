public class Subtract {

    public static int subtract1(int i, int j) {
        // this one is nondeterministic, but this is detected only if executed at least 5x
        return i - j + (increment("counter-1") / 5);
    }

    public static int subtract2(int i, int j) {
        // this one produces timeout
        while (true);
    }

    public static int subtract3(int i, int j) {
        // nondeterministic again
        return i - j + (increment("counter-3") / 5);
    }

    public static int subtract4(int i, int j) {
        // timeout again
        while (true);
    }

    public static int subtract5(int i, int j) {
        // one more, for good measure
        return i - j + (increment("counter-5") / 5);
    }

    public static int subtract6(int i, int j) {
        // here too
        while (true);
    }

    private static int increment(String name) {
        // use system properties, which persist even when this class
        // is reloaded for every test run
        int count = Integer.getInteger(name, 0);
        count++;
        System.setProperty(name, Integer.toString(count));
        return count;
    }
}
