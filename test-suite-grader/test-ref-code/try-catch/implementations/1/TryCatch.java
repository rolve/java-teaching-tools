package poker;

public class TryCatch {

    public static void foo() {
        // this code leads to an exception being thrown by Pitest if no fallback
        // ClassByteArraySource is defined
        int i = 1;
        int j = 1 / i;
        try {
            String s = "";
        } catch (Exception e) {
        }
    }
}
