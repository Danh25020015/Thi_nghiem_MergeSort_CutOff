public class Merge {
    private static int CUTOFF = 7;

    public static void setCutoff(int cutoff) {
        CUTOFF = cutoff;
    }

    public static int getCutoff() {
        return CUTOFF;
    }

    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
        assert isSorted(a, lo, mid);    
        assert isSorted(a, mid + 1, hi);

        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }

        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)              a[k] = aux[j++];
            else if (j > hi)               a[k] = aux[i++];
            else if (less(aux[j], aux[i])) a[k] = aux[j++];
            else                           a[k] = aux[i++];
        }

        assert isSorted(a, lo, hi);
    }

    private static void sort(Comparable[] a, Comparable[] aux, int lo, int hi) {
        if (hi <= lo + CUTOFF - 1) {
            Insertion.sort(a, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sort(a, aux, lo, mid);
        sort(a, aux, mid + 1, hi);
        merge(a, aux, lo, mid, hi);
    }

    public static void sort(Comparable[] a) {
        Comparable[] aux = new Comparable[a.length];
        sort(a, aux, 0, a.length - 1);
    }

    public static void sort(Comparable[] a, int cutoff) {
        CUTOFF = cutoff;
        sort(a);
    }

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    private static void exch(Comparable[] a, int i, int j) {
        Comparable swap = a[i];
        a[i] = a[j];
        a[j] = swap;
    }

    public static boolean isSorted(Comparable[] a) {
        return isSorted(a, 0, a.length - 1);
    }

    public static boolean isSorted(Comparable[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            if (less(a[i], a[i - 1])) return false;
        }
        return true;
    }

    // PHAN MO RONG PHUC VU PHAN TICH LY THUYET (COST MODEL - BAI GIANG 1.4 & 2.2)
    // Dem chinh xac so phep so sanh (compares), truy cap mang (array accesses)
    // va so lời goi de quy sort() / merge() tren mot luong doc lap (khong anh huong Stopwatch)
    // Tham khao AI

    public static class CostMetrics {
        public long compares = 0;
        public long arrayAccesses = 0;
        public long recursiveCalls = 0;
        public long mergeCalls = 0;
        public long insertionExchanges = 0;
    }

    public static CostMetrics analyzeCostModel(Comparable[] a, int cutoff) {
        CostMetrics m = new CostMetrics();
        Comparable[] aux = new Comparable[a.length];
        sortWithCost(a, aux, 0, a.length - 1, cutoff, m);
        return m;
    }

    private static void sortWithCost(Comparable[] a, Comparable[] aux, int lo, int hi, int cutoff, CostMetrics m) {
        m.recursiveCalls++;
        if (cutoff <= 1) {
            if (hi <= lo) return;
        } else {
            if (hi <= lo + cutoff - 1) {
                insertionSortWithCost(a, lo, hi, m);
                return;
            }
        }

        int mid = lo + (hi - lo) / 2;
        sortWithCost(a, aux, lo, mid, cutoff, m);
        sortWithCost(a, aux, mid + 1, hi, cutoff, m);
        mergeWithCost(a, aux, lo, mid, hi, m);
    }

    private static void insertionSortWithCost(Comparable[] a, int lo, int hi, CostMetrics m) {
        for (int i = lo; i <= hi; i++) {
            for (int j = i; j > lo; j--) {
                m.compares++;
                m.arrayAccesses += 2; // doc a[j] va a[j-1]
                if (less(a[j], a[j - 1])) {
                    exch(a, j, j - 1);
                    m.insertionExchanges++;
                    m.arrayAccesses += 4; // 2 lan doc + 2 lan ghi trong exch
                } else {
                    break;
                }
            }
        }
    }

    private static void mergeWithCost(Comparable[] a, Comparable[] aux, int lo, int mid, int hi, CostMetrics m) {
        m.mergeCalls++;
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
            m.arrayAccesses += 2; // 1 doc tu a[k], 1 ghi vao aux[k]
        }

        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a[k] = aux[j++];
                m.arrayAccesses += 2;
            } else if (j > hi) {
                a[k] = aux[i++];
                m.arrayAccesses += 2;
            } else {
                m.compares++;
                m.arrayAccesses += 2; // doc aux[j] va aux[i] de so sanh
                if (less(aux[j], aux[i])) {
                    a[k] = aux[j++];
                    m.arrayAccesses += 2;
                } else {
                    a[k] = aux[i++];
                    m.arrayAccesses += 2;
                }
            }
        }
    }
}
