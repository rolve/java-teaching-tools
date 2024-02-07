import java.util.Random;

public class RandomSort {

    public static boolean isSorted(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] > array[i + 1]) {
                return false;
            }
        }
        return true;
    }

    public static int randomSort(int[] array) {
        Random random = new Random();

        // stupid special cases to generate more mutants, maybe more similar
        // to what students might write
        if (isSorted(array)) {
            return 0;
        } else if (array.length == 2) {
            int t = array[0];
            array[0] = array[1];
            array[1] = t;
            return 1;
        }

        // but still need the general case
        int swaps = 0;
        while (!isSorted(array)) {
            int i = random.nextInt(array.length);
            int j = random.nextInt(array.length);
            int lo = Math.min(i, j);
            int hi = Math.max(i, j);
            if (array[lo] > array[hi]) {
                int t = array[lo];
                array[lo] = array[hi];
                array[hi] = t;
                swaps++;
            }
        }
        return swaps;
    }
}
