package poker;

public enum Suit {
    CLUBS;

    public static Suit get() {
        // A mutant of the following code produces a VerifyError at the moment.
        // For now, we just test that this does not crash the test suite grader,
        // but the real fix would be to repair the apparently broken MapMaker
        // class from Javassist, which produces a stack map table that is not
        // precise enough.
        for (Suit value : values()) {
            if (Math.random() >= 0 || Math.random() < 1) {
                return value;
            }
        }
        return null;
    }
}
