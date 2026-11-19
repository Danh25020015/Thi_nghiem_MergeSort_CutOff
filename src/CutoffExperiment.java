import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/**
 * Chuong trinh chay thi nghiem tim gia tri CUTOFF tot nhat cho thuat toan Mergesort.
 *
 * Thiet ke thi nghiem dua tren:
 * - Bai giang 1.4 Analysis of Algorithms:
 *   + Scientific Method (Slide 8): Observe, Hypothesize, Predict, Verify, Validate.
 *   + Stopwatch (Slide 13): Do thoi gian thuc thi tu dong.
 *   + Data Analysis & Doubling Hypothesis (Slide 15-20): Khao sat T(N), ti so T(2N)/T(N) va lg(ratio).
 *   + Experimental Algorithmics (Slide 21): Phan tach yeu to doc lap he thong (Algorithm, Input data)
 *     va phu thuoc he thong (Hardware CPU/Cache, Software JVM/JIT/GC).
 *   + Cost Model (Slide 24-36): Dem tan suat cac phep toan co ban (compares, array accesses).
 * - Bai giang 2.1 Elementary Sorts & 2.2 Mergesort:
 *   + Cai dat Insertion Sort (Slide 28 bai 2.1) va Mergesort kem CUTOFF (Slide 7, 9, 19 bai 2.2).
 */
public class CutoffExperiment {

    // Danh sach cac gia tri CUTOFF can khao sat:
    // Quet chi tiet tung gia tri tu 1 den 20 (bao gom moc 1 = khong cutoff, moc 7 = giao trinh cu, moc 10 = giao trinh moi)
    // va mo rong toi 128 de quan sat ro vung suy giam hieu nang khi CUTOFF qua lon.
    public static final int[] CUTOFF_VALUES = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        22, 24, 28, 32, 40, 48, 64, 80, 96, 128
    };

    public static final String RESULTS_DIR = "results";

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);

        File resDir = new File(RESULTS_DIR);
        if (!resDir.exists()) {
            resDir.mkdirs();
        }

        // Dam bao bo du lieu thi nghiem da duoc sinh ra trong thu muc datasets/
        File dsDir = new File(DatasetGenerator.DATASET_DIR);
        if (!dsDir.exists() || !new File(dsDir, "random_500K.txt").exists()) {
            DatasetGenerator.generateAllDatasets();
        }

        System.out.println("=========================================================================");
        System.out.println("   THI NGHIEM TIM GIA TRI CUTOFF TOI UU CHO MERGESORT (ALGORITHMS 4TH)   ");
        System.out.println("=========================================================================");
        printSystemInfo();

        // Buoc 0: Kiem tra tinh dung dan cua thuat toan tren tat ca cac moc CUTOFF
        verifyCorrectness();

        // Buoc 1: Lam nong JVM (JIT Warm-up) de tranh sai so bien dich dong o nhung lan chay dau
        warmupJVM();

        // Buoc 2: Thi nghiem 1 - Khao sat thoi gian chay theo CUTOFF tren cac kich thuoc N (100K, 250K, 500K, 1M)
        runExperiment1_CutoffBySize();

        // Buoc 3: Thi nghiem 2 - Phan tich Cost Model (So phep so sanh, truy cap mang, loi goi de quy)
        runExperiment2_CostModel();

        // Buoc 4: Thi nghiem 3 - Anh huong cua phan phoi du lieu (Random, Partially Sorted, Reverse Sorted, Duplicates)
        runExperiment3_Distributions();

        // Buoc 5: Thi nghiem 4 - Kiem chung Doubling Hypothesis (Slide 19-20 bai 1.4 Analysis of Algorithms)
        runExperiment4_DoublingHypothesis();

        System.out.println("\n=== TOAN BO THI NGHIEM DA HOAN TAT. KET QUA LUU TAI THU MUC results/ ===");
    }

    private static void printSystemInfo() {
        System.out.println("Cau hinh moi truong thuc nghiem:");
        System.out.println("  - OS          : " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")");
        System.out.println("  - Java VM     : " + System.getProperty("java.vm.name") + " (build " + System.getProperty("java.vm.version") + ")");
        System.out.println("  - JDK Version : " + System.getProperty("java.version"));
        System.out.println("  - Logical CPUs: " + Runtime.getRuntime().availableProcessors());
        System.out.printf ("  - Max Heap    : %.2f MB%n", Runtime.getRuntime().maxMemory() / (1024.0 * 1024.0));
        System.out.println("-------------------------------------------------------------------------");
    }

    private static void verifyCorrectness() {
        System.out.print("[Kiem tra tinh dung dan] Dang test Merge.sort voi moi CUTOFF... ");
        Integer[] base = DatasetGenerator.generateRandomArray(10_000, 999L);
        for (int cutoff : CUTOFF_VALUES) {
            Integer[] copy = Arrays.copyOf(base, base.length);
            Merge.sort(copy, cutoff);
            if (!Merge.isSorted(copy)) {
                throw new AssertionError("Loi: Mang chua duoc sap xep dung voi CUTOFF = " + cutoff);
            }
        }
        System.out.println("PASSED (100% chinh xac)!");
    }

    private static void warmupJVM() {
        System.out.print("[JVM Warm-up] Dang lam nong HotSpot JIT Compiler... ");
        Integer[] base = DatasetGenerator.generateRandomArray(100_000, 12345L);
        int[] warmupCutoffs = {1, 4, 7, 10, 12, 16, 24, 32, 64};
        for (int round = 0; round < 6; round++) {
            for (int c : warmupCutoffs) {
                Integer[] copy = Arrays.copyOf(base, base.length);
                Merge.sort(copy, c);
            }
        }
        System.gc();
        System.out.println("DONE!");
    }

    /**
     * Thi nghiem 1: Do thoi gian chay (ms) cho tung CUTOFF tren 4 bo du lieu ngau nhien
     * co kich thuoc N = 100,000; 250,000; 500,000; 1,000,000.
     * Su dung phuong phap chay xen ke (interleaved rounds) qua T = 15 lan lap de triet tieu
     * nhieu he thong va tinh Trung binh (Mean), Do lech chuan (SD), Trung vi (Median), Min, Max.
     */
    private static void runExperiment1_CutoffBySize() throws IOException {
        System.out.println("\n>>> THI NGHIEM 1: KHAO SAT CUTOFF TREN CAC KICH THUOC MANG N (RANDOM DATA)");
        String[] files = {"random_100K.txt", "random_250K.txt", "random_500K.txt", "random_1M.txt"};
        int[] sizes = {100_000, 250_000, 500_000, 1_000_000};
        int trials = 15;
        int warmupRounds = 2;

        int numCutoffs = CUTOFF_VALUES.length;
        // Luu ket qua: meanTimes[sizeIdx][cutoffIdx], sdTimes[sizeIdx][cutoffIdx], medianTimes[sizeIdx][cutoffIdx]
        double[][] meanTimes = new double[sizes.length][numCutoffs];
        double[][] sdTimes = new double[sizes.length][numCutoffs];
        double[][] medianTimes = new double[sizes.length][numCutoffs];
        double[][] minTimes = new double[sizes.length][numCutoffs];
        double[][] maxTimes = new double[sizes.length][numCutoffs];

        try (BufferedWriter csv = new BufferedWriter(new FileWriter(new File(RESULTS_DIR, "exp1_cutoff_by_size.csv")))) {
            csv.write("N,cutoff,mean_ms,sd_ms,median_ms,min_ms,max_ms,speedup_pct");
            csv.newLine();

            for (int s = 0; s < sizes.length; s++) {
                int n = sizes[s];
                File f = new File(DatasetGenerator.DATASET_DIR, files[s]);
                Integer[] baseData = DatasetGenerator.loadDataset(f);
                Integer[] workArray = new Integer[n];

                double[][] rawTimes = new double[numCutoffs][trials];

                System.out.printf("  * Dang do bo du lieu %-16s (N = %,9d, %d trials/cutoff)...%n", files[s], n, trials);

                // Chay xen ke cac vong (interleaved rounds): moi vong quet qua tat ca cac CUTOFF
                // Dao chieu quet chan/le giua cac vong de loai bo hoan toan xu huong troi thoi gian (drift)
                for (int r = -warmupRounds; r < trials; r++) {
                    for (int idx = 0; idx < numCutoffs; idx++) {
                        int cIdx = (r % 2 == 0) ? idx : (numCutoffs - 1 - idx);
                        int cutoff = CUTOFF_VALUES[cIdx];

                        System.arraycopy(baseData, 0, workArray, 0, n);
                        Stopwatch timer = new Stopwatch();
                        Merge.sort(workArray, cutoff);
                        double elapsedMs = timer.elapsedTimeMillis();

                        if (r >= 0) {
                            rawTimes[cIdx][r] = elapsedMs;
                        }
                    }
                }

                // Tinh thong ke cho tung CUTOFF
                double baselineMean = 0.0;
                int bestCutoff = 1;
                double bestMean = Double.MAX_VALUE;

                for (int cIdx = 0; cIdx < numCutoffs; cIdx++) {
                    double[] samples = rawTimes[cIdx];
                    Arrays.sort(samples);

                    // Tinh Trimmed Mean (loai bo 1 gia tri nho nhat va 1 gia tri lon nhat de chong nhieu GC bat thuong)
                    double sum = 0.0;
                    int count = samples.length - 2;
                    for (int i = 1; i < samples.length - 1; i++) {
                        sum += samples[i];
                    }
                    double mean = sum / count;

                    double sqDiff = 0.0;
                    for (int i = 1; i < samples.length - 1; i++) {
                        double d = samples[i] - mean;
                        sqDiff += d * d;
                    }
                    double sd = Math.sqrt(sqDiff / Math.max(1, count - 1));
                    double median = samples[samples.length / 2];
                    double min = samples[0];
                    double max = samples[samples.length - 1];

                    meanTimes[s][cIdx] = mean;
                    sdTimes[s][cIdx] = sd;
                    medianTimes[s][cIdx] = median;
                    minTimes[s][cIdx] = min;
                    maxTimes[s][cIdx] = max;

                    if (cIdx == 0) {
                        baselineMean = mean; // CUTOFF = 1 (Mergesort goc)
                    }
                    if (mean < bestMean) {
                        bestMean = mean;
                        bestCutoff = CUTOFF_VALUES[cIdx];
                    }

                    double speedupPct = ((baselineMean - mean) / baselineMean) * 100.0;
                    csv.write(String.format("%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f",
                            n, CUTOFF_VALUES[cIdx], mean, sd, median, min, max, speedupPct));
                    csv.newLine();
                }

                // In tom tat ket qua cho kich thuoc N hien tai
                int idx7 = findCutoffIndex(7);
                int idx10 = findCutoffIndex(10);
                int idxBest = findCutoffIndex(bestCutoff);
                System.out.printf("    -> Baseline (CUTOFF=1) : %.2f ms (+-%.2f ms)%n", meanTimes[s][0], sdTimes[s][0]);
                System.out.printf("    -> CUTOFF = 7  (Cu)    : %.2f ms (+-%.2f ms) | Nhanh hon %.2f%%%n",
                        meanTimes[s][idx7], sdTimes[s][idx7], ((baselineMean - meanTimes[s][idx7]) / baselineMean) * 100.0);
                System.out.printf("    -> CUTOFF = 10 (Moi)   : %.2f ms (+-%.2f ms) | Nhanh hon %.2f%%%n",
                        meanTimes[s][idx10], sdTimes[s][idx10], ((baselineMean - meanTimes[s][idx10]) / baselineMean) * 100.0);
                System.out.printf("    -> TOT NHAT (CUTOFF=%-2d): %.2f ms (+-%.2f ms) | Nhanh hon %.2f%%%n",
                        bestCutoff, bestMean, sdTimes[s][idxBest], ((baselineMean - bestMean) / baselineMean) * 100.0);
            }
        }
    }

    /**
     * Thi nghiem 2: Phan tich Cost Model (Slide 14 bai 2.2 Mergesort & Slide 30 bai 1.4 Analysis of Algorithms).
     * Dem chinh xac so phep so sanh (compares), so truy cap mang (array accesses),
     * so lan goi de quy sort() va merge() tren bo du lieu random_500K.txt (N = 500,000).
     */
    private static void runExperiment2_CostModel() throws IOException {
        System.out.println("\n>>> THI NGHIEM 2: PHAN TICH MO HINH CHI PHI (COST MODEL - N = 500,000)");
        File f = new File(DatasetGenerator.DATASET_DIR, "random_500K.txt");
        Integer[] baseData = DatasetGenerator.loadDataset(f);
        int n = baseData.length;
        double nLgN = n * (Math.log(n) / Math.log(2.0));

        try (BufferedWriter csv = new BufferedWriter(new FileWriter(new File(RESULTS_DIR, "exp2_cost_model.csv")))) {
            csv.write("cutoff,compares,compares_per_nlgn,array_accesses,accesses_per_nlgn,recursive_calls,merge_calls,insertion_exchanges");
            csv.newLine();

            System.out.printf("  %-8s | %-14s | %-16s | %-15s | %-12s | %-14s%n",
                    "CUTOFF", "Compares", "Array Accesses", "Recursive Calls", "Merge Calls", "Ins. Exchanges");
            System.out.println("  -----------------------------------------------------------------------------------------------");

            for (int cutoff : CUTOFF_VALUES) {
                Integer[] copy = Arrays.copyOf(baseData, n);
                Merge.CostMetrics m = Merge.analyzeCostModel(copy, cutoff);

                csv.write(String.format("%d,%d,%.4f,%d,%.4f,%d,%d,%d",
                        cutoff,
                        m.compares, m.compares / nLgN,
                        m.arrayAccesses, m.arrayAccesses / nLgN,
                        m.recursiveCalls, m.mergeCalls, m.insertionExchanges));
                csv.newLine();

                if (cutoff == 1 || cutoff == 4 || cutoff == 7 || cutoff == 10 || cutoff == 12 || cutoff == 16 || cutoff == 32 || cutoff == 64 || cutoff == 128) {
                    System.out.printf("  %-8d | %,14d | %,16d | %,15d | %,12d | %,14d%n",
                            cutoff, m.compares, m.arrayAccesses, m.recursiveCalls, m.mergeCalls, m.insertionExchanges);
                }
            }
        }
    }

    /**
     * Thi nghiem 3: Khao sat anh huong cua phan phoi du lieu dau vao (N = 500,000)
     * gom: Random, Partially Sorted, Reverse Sorted, Many Duplicates.
     */
    private static void runExperiment3_Distributions() throws IOException {
        System.out.println("\n>>> THI NGHIEM 3: ANH HUONG CUA PHAN PHOI DU LIEU DAU VAO (N = 500,000)");
        String[] distNames = {"Random", "PartiallySorted", "ReverseSorted", "ManyDuplicates"};
        String[] distFiles = {"random_500K.txt", "partially_sorted_500K.txt", "reverse_sorted_500K.txt", "duplicates_500K.txt"};
        int trials = 12;
        int warmupRounds = 2;
        int numCutoffs = CUTOFF_VALUES.length;

        try (BufferedWriter csv = new BufferedWriter(new FileWriter(new File(RESULTS_DIR, "exp3_distributions.csv")))) {
            csv.write("distribution,cutoff,mean_ms,sd_ms,median_ms,speedup_pct,compares,array_accesses");
            csv.newLine();

            for (int d = 0; d < distNames.length; d++) {
                File f = new File(DatasetGenerator.DATASET_DIR, distFiles[d]);
                Integer[] baseData = DatasetGenerator.loadDataset(f);
                int n = baseData.length;
                Integer[] workArray = new Integer[n];

                double[][] rawTimes = new double[numCutoffs][trials];

                for (int r = -warmupRounds; r < trials; r++) {
                    for (int idx = 0; idx < numCutoffs; idx++) {
                        int cIdx = (r % 2 == 0) ? idx : (numCutoffs - 1 - idx);
                        int cutoff = CUTOFF_VALUES[cIdx];

                        System.arraycopy(baseData, 0, workArray, 0, n);
                        Stopwatch timer = new Stopwatch();
                        Merge.sort(workArray, cutoff);
                        double elapsedMs = timer.elapsedTimeMillis();

                        if (r >= 0) {
                            rawTimes[cIdx][r] = elapsedMs;
                        }
                    }
                }

                double baselineMean = 0.0;
                int bestCutoff = 1;
                double bestMean = Double.MAX_VALUE;

                for (int cIdx = 0; cIdx < numCutoffs; cIdx++) {
                    int cutoff = CUTOFF_VALUES[cIdx];
                    double[] samples = rawTimes[cIdx];
                    Arrays.sort(samples);

                    double sum = 0.0;
                    int count = samples.length - 2;
                    for (int i = 1; i < samples.length - 1; i++) {
                        sum += samples[i];
                    }
                    double mean = sum / count;

                    double sqDiff = 0.0;
                    for (int i = 1; i < samples.length - 1; i++) {
                        double diff = samples[i] - mean;
                        sqDiff += diff * diff;
                    }
                    double sd = Math.sqrt(sqDiff / Math.max(1, count - 1));
                    double median = samples[samples.length / 2];

                    if (cIdx == 0) baselineMean = mean;
                    if (mean < bestMean) {
                        bestMean = mean;
                        bestCutoff = cutoff;
                    }

                    System.arraycopy(baseData, 0, workArray, 0, n);
                    Merge.CostMetrics cm = Merge.analyzeCostModel(workArray, cutoff);

                    double speedupPct = ((baselineMean - mean) / baselineMean) * 100.0;
                    csv.write(String.format("%s,%d,%.4f,%.4f,%.4f,%.2f,%d,%d",
                            distNames[d], cutoff,MeanOrZero(mean), sd, median, speedupPct, cm.compares, cm.arrayAccesses));
                    csv.newLine();
                }

                System.out.printf("  * Phan phoi %-16s | Baseline (CUTOFF=1): %6.2f ms | Best CUTOFF = %-2d (%6.2f ms, nhanh hon %.2f%%)%n",
                        distNames[d], baselineMean, bestCutoff, bestMean, ((baselineMean - bestMean) / baselineMean) * 100.0);
            }
        }
    }

    /**
     * Thi nghiem 4: Kiem chung Doubling Hypothesis (Slide 19-20 bai 1.4 Analysis of Algorithms).
     * Nhan doi kich thuoc dau vao N tu 31,250 den 2,000,000 va do ti so T(2N)/T(N) cung lg(ratio)
     * giua Mergesort goc (CUTOFF=1), CUTOFF=7, CUTOFF=10 va CUTOFF=12.
     */
    private static void runExperiment4_DoublingHypothesis() throws IOException {
        System.out.println("\n>>> THI NGHIEM 4: KIEM CHUNG DOUBLING HYPOTHESIS (SLIDE 19 - BAI GIANG 1.4)");
        int[] sizes = {31_250, 62_500, 125_000, 250_000, 500_000, 1_000_000, 2_000_000};
        int[] compareCutoffs = {1, 7, 10, 12};
        int trials = 10;

        double[][] times = new double[sizes.length][compareCutoffs.length];

        try (BufferedWriter csv = new BufferedWriter(new FileWriter(new File(RESULTS_DIR, "exp4_doubling_hypothesis.csv")))) {
            csv.write("N,time_c1_ms,ratio_c1,lg_ratio_c1,time_c7_ms,time_c10_ms,time_c12_ms,ratio_c12,lg_ratio_c12");
            csv.newLine();

            for (int s = 0; s < sizes.length; s++) {
                int n = sizes[s];
                Integer[] base = DatasetGenerator.generateRandomArray(n, DatasetGenerator.BASE_SEED + n * 7L);
                Integer[] work = new Integer[n];

                for (int r = -1; r < trials; r++) {
                    for (int c = 0; c < compareCutoffs.length; c++) {
                        System.arraycopy(base, 0, work, 0, n);
                        Stopwatch sw = new Stopwatch();
                        Merge.sort(work, compareCutoffs[c]);
                        double ms = sw.elapsedTimeMillis();
                        if (r >= 0) {
                            times[s][c] += ms;
                        }
                    }
                }
                for (int c = 0; c < compareCutoffs.length; c++) {
                    times[s][c] /= trials;
                }

                double ratio1 = (s == 0) ? 0.0 : (times[s][0] / times[s - 1][0]);
                double lgRatio1 = (s == 0) ? 0.0 : (Math.log(ratio1) / Math.log(2.0));
                double ratio12 = (s == 0) ? 0.0 : (times[s][3] / times[s - 1][3]);
                double lgRatio12 = (s == 0) ? 0.0 : (Math.log(ratio12) / Math.log(2.0));

                csv.write(String.format("%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f",
                        n, times[s][0], ratio1, lgRatio1, times[s][1], times[s][2], times[s][3], ratio12, lgRatio12));
                csv.newLine();

                System.out.printf("  N = %,10d | C=1: %7.2f ms (ratio=%4.2f, lg=%4.2f) | C=7: %7.2f ms | C=10: %7.2f ms | C=12: %7.2f ms (ratio=%4.2f, lg=%4.2f)%n",
                        n, times[s][0], ratio1, lgRatio1, times[s][1], times[s][2], times[s][3], ratio12, lgRatio12);
            }
        }
    }

    private static double MeanOrZero(double val) {
        return Double.isNaN(val) ? 0.0 : val;
    }

    private static int findCutoffIndex(int cutoff) {
        for (int i = 0; i < CUTOFF_VALUES.length; i++) {
            if (CUTOFF_VALUES[i] == cutoff) return i;
        }
        return 0;
    }
}
