import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RandomSortTest {

    @Test
    public void testIsSorted() {
        assertTrue(RandomSort.isSorted(new int[]{1, 2}));
        assertTrue(RandomSort.isSorted(new int[]{1, 2, 3}));
        assertTrue(RandomSort.isSorted(new int[]{-1, 1, 3, 13}));
        assertTrue(RandomSort.isSorted(new int[]{0, 3, 13, 101, 102, 1000}));

        assertFalse(RandomSort.isSorted(new int[]{2, 1}));
        assertFalse(RandomSort.isSorted(new int[]{3, 1, 2}));
        assertFalse(RandomSort.isSorted(new int[]{1, 3, 2}));
        assertFalse(RandomSort.isSorted(new int[]{-1, 1, 13, 3}));
        assertFalse(RandomSort.isSorted(new int[]{1, -1, 3, 13}));
        assertFalse(RandomSort.isSorted(new int[]{-1, 3, 1, 13}));
        assertFalse(RandomSort.isSorted(new int[]{13, 3, 1, -1}));
        assertFalse(RandomSort.isSorted(new int[]{10000, 0, 3, 13, 101, 102, 1000}));
    }

    @Test
    public void testIsSortedSpecial() {
        assertTrue(RandomSort.isSorted(new int[]{}));
        assertTrue(RandomSort.isSorted(new int[]{1}));
        assertTrue(RandomSort.isSorted(new int[]{-13}));
    }

    @Test
    public void testIsSortedDuplicates() {
        assertTrue(RandomSort.isSorted(new int[]{1, 1}));
        assertTrue(RandomSort.isSorted(new int[]{1, 1, 2, 2}));
        assertTrue(RandomSort.isSorted(new int[]{3, 3, 3, 3, 3}));

        assertFalse(RandomSort.isSorted(new int[]{2, 1, 1}));
        assertFalse(RandomSort.isSorted(new int[]{1, 2, 1}));
        assertFalse(RandomSort.isSorted(new int[]{0, 1, 1, 1, 2, 2, 1}));
    }

    @Test
    public void testRandomSortSingleSwap() {
        int[] array = {1, 2, 3, 4, 6, 5};
        int[] sorted = {1, 2, 3, 4, 5, 6};
        int swaps = RandomSort.randomSort(array);
        assertArrayEquals(sorted, array);
        assertEquals(1, swaps);
    }

    @Test
    public void testRandomSortOneOrThreeSwaps() {
        // teste mehrmals, da zufällig
        int oneSwap = 0;
        int threeSwaps = 0;
        for (int i = 0; i < 100; i++) {
            // 1 oder 3 swaps möglich:
            int[] array = {1, 2, 3, 6, 5, 4};
            int[] copy = Arrays.copyOf(array, array.length);
            int swaps = RandomSort.randomSort(array);
            Arrays.sort(copy);
            assertArrayEquals(copy, array);
            if (swaps == 1) {
                oneSwap++;
            } else if (swaps == 3) {
                threeSwaps++;
            } else {
                fail("1 oder 3 Swaps erwartet, waren " + swaps);
            }
        }
        if (oneSwap == 0) {
            fail("Nie in 1 Swap erfolgt. Sehr wahrscheinlich verbuggt...");
        }
        if (threeSwaps == 0) {
            fail("Nie in 3 Swaps erfolgt. Sehr wahrscheinlich verbuggt...");
        }
    }

    @Test
    public void testRandomSortTwoOrFourSwaps() {
        // teste mehrmals, da zufällig
        int twoSwaps = 0;
        int fourSwaps = 0;
        for (int i = 0; i < 100; i++) {
            // 2 oder 4 swaps möglich:
            int[] array = {1, 3, 2, 6, 5, 4};
            int[] copy = Arrays.copyOf(array, array.length);
            int swaps = RandomSort.randomSort(array);
            Arrays.sort(copy);
            assertArrayEquals(copy, array);
            if (swaps == 2) {
                twoSwaps++;
            } else if (swaps == 4) {
                fourSwaps++;
            } else {
                fail("2 oder 4 Swaps erwartet, waren " + swaps);
            }
        }
        if (twoSwaps == 0) {
            fail("Nie in 2 Swaps erfolgt. Sehr wahrscheinlich verbuggt...");
        }
        if (fourSwaps == 0) {
            fail("Nie in 4 Swaps erfolgt. Sehr wahrscheinlich verbuggt...");
        }
    }

    @Test
    public void testRandomSortManySwaps() {
        // teste mehrmals, da zufällig
        for (int i = 0; i < 100; i++) {
            // verschiedene swaps möglich, aber alle gerade und mindestens 4:
            int[] array = {6, 3, 7, 2, 5, 1, 4};
            int[] copy = Arrays.copyOf(array, array.length);
            int swaps = RandomSort.randomSort(array);
            Arrays.sort(copy);
            assertArrayEquals(copy, array);
            assertTrue(swaps >= 4 && swaps % 2 == 0,
                    "Gerade Anzahl Swaps >= 4 erwartet, waren " + swaps);
        }
    }

    @Test
    public void testRandomSortNoSwaps() {
        // schon sortiert, also 0 swaps nötig
        int[] array = {-1, 0, 1, 2, 7, 13, 14};
        int[] copy = Arrays.copyOf(array, array.length);
        int swaps = RandomSort.randomSort(array);
        assertArrayEquals(copy, array);
        assertEquals(0, swaps);
    }

    @Test
    public void testRandomSortSingleSwapDuplicates() {
        int[] array = {2, 2, 2, 4, 5, 4};
        int[] sorted = {2, 2, 2, 4, 4, 5};
        int swaps = RandomSort.randomSort(array);
        assertArrayEquals(sorted, array);
        assertEquals(1, swaps);
    }
}
