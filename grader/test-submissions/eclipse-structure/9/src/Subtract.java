public class Subtract {

    private static int count1 = 0;
    private static int count3 = 0;
    private static int count5 = 0;
    
    public static int subtract1(int i, int j) {
        // this one is nondeterministic, but this is detected only if executed at least 5x
        return i - j + (++count1 / 5);
    }

    public static int subtract2(int i, int j) {
        // this one produces timeout
        while (true);
    }

    public static int subtract3(int i, int j) {
        // nondeterministic again
        return i - j + (++count3 / 5);
    }

    public static int subtract4(int i, int j) {
        // timeout again
        while (true);
    }

    public static int subtract5(int i, int j) {
        // one more, for good measure
        return i - j + (++count5 / 5);
    }

    public static int subtract6(int i, int j) {
        // here too
        while (true);
    }
}
