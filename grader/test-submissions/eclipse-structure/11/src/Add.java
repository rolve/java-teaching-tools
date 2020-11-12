public class Add {
    
    public static int add(int i, int j) {
        while (true) {
            try {
                i++;
                j--;
            } catch (ThreadDeath e) {
                // happily ignore
            }
        }
    }
}
