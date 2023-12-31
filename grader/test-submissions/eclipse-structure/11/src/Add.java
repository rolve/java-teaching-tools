public class Add {
    
    public static int add(int i, int j) {
        while (true) {
            try {
                i++;
                j--;
                if (Math.random() < Double.MIN_VALUE) {
                    throw new InterruptedException();
                }
            } catch (InterruptedException e) {
                // happily ignore
            }
        }
    }
}
