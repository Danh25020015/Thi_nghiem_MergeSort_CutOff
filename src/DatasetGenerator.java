import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * Chuong trinh xay dung va doc bo du lieu thi nghiem (Test Datasets).
 *
 * Tuan thu nguyen tac Scientific Method trong bai giang 1.4 Analysis of Algorithms (Slide 8):
 * "Experiments must be reproducible" - su dung fixed seed (SEED = 20261119L) de bo du lieu
 * co the tai lap chinh xac 100% tren moi lan chay.
 *
 * Cac kieu phan phoi du lieu duoc xay dung dua tren bai giang:
 * 1. Random (Ngau nhien phan phoi deu) - Slide 4, 31 (2.1 Elementary Sorts) & Slide 11 (2.2 Mergesort)
 * 2. Partially Sorted (Gan nhu da sap xep, so nghich the <= c*N) - Slide 34, 35 (2.1 Elementary Sorts)
 * 3. Reverse Sorted (Sap xep nguoc - Worst case cua Insertion Sort) - Slide 32, 33 (2.1 Elementary Sorts) & Slide 12 (2.2 Mergesort)
 * 4. Many Duplicates (Nhieu khoa trung nhau) - Slide 36 (2.2 Mergesort) & Slide 31 (2.3 Quicksort)
 */
public class DatasetGenerator {

    public static final long BASE_SEED = 20261119L;
    public static final String DATASET_DIR = "datasets";

    public static void main(String[] args) throws IOException {
        generateAllDatasets();
    }

    public static void generateAllDatasets() throws IOException {
        File dir = new File(DATASET_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        System.out.println("=== BAT DAU SINH BO DU LIEU THI NGHIEM (SEED = " + BASE_SEED + ") ===");

        // 1. Cac kich thuoc mang ngau nhien N = 100K, 250K, 500K, 1M
        int[] randomSizes = {100_000, 250_000, 500_000, 1_000_000};
        String[] randomNames = {"random_100K.txt", "random_250K.txt", "random_500K.txt", "random_1M.txt"};

        for (int i = 0; i < randomSizes.length; i++) {
            int n = randomSizes[i];
            File file = new File(dir, randomNames[i]);
            Integer[] data = generateRandomArray(n, BASE_SEED + n);
            saveDataset(file, data);
            System.out.printf("  [+] Da tao %-28s | N = %,9d phan tu%n", file.getPath(), n);
        }

        // 2. Cac phan phoi dac biet voi N = 500,000 de khao sat anh huong cua cau truc du lieu dau vao
        int nDist = 500_000;

        File fPartial = new File(dir, "partially_sorted_500K.txt");
        saveDataset(fPartial, generatePartiallySortedArray(nDist, BASE_SEED + 101));
        System.out.printf("  [+] Da tao %-28s | N = %,9d phan tu (Gan nhu da sap xep)%n", fPartial.getPath(), nDist);

        File fReverse = new File(dir, "reverse_sorted_500K.txt");
        saveDataset(fReverse, generateReverseSortedArray(nDist));
        System.out.printf("  [+] Da tao %-28s | N = %,9d phan tu (Sap xep nguoc)%n", fReverse.getPath(), nDist);

        File fDup = new File(dir, "duplicates_500K.txt");
        saveDataset(fDup, generateDuplicatesArray(nDist, 100, BASE_SEED + 202));
        System.out.printf("  [+] Da tao %-28s | N = %,9d phan tu (100 gia tri khoa phân biet)%n", fDup.getPath(), nDist);

        System.out.println("=== HOAN TAT SINH BO DU LIEU THI NGHIEM ===\n");
    }

    /**
     * Sinh mang ngau nhien phan phoi deu trong [0, 10*N).
     */
    public static Integer[] generateRandomArray(int n, long seed) {
        Random rand = new Random(seed);
        Integer[] a = new Integer[n];
        int bound = Math.max(n * 10, 1_000_000);
        for (int i = 0; i < n; i++) {
            a[i] = rand.nextInt(bound);
        }
        return a;
    }

    /**
     * Sinh mang gan nhu da sap xep (Partially sorted array - Slide 34 bai 2.1 Elementary Sorts):
     * Mang ban dau tang dan, sau do thuc hien hoan doi ngau nhien cac cap phan tu lan can
     * sao cho so nghich the (inversions) <= c * N (o day c ~ 0.1).
     */
    public static Integer[] generatePartiallySortedArray(int n, long seed) {
        Random rand = new Random(seed);
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        int swaps = n / 10; // ~0.1 N phep hoan doi cuc bo
        for (int k = 0; k < swaps; k++) {
            int idx = rand.nextInt(n - 1);
            Integer tmp = a[idx];
            a[idx] = a[idx + 1];
            a[idx + 1] = tmp;
        }
        return a;
    }

    /**
     * Sinh mang sap xep nguoc thu tu giam dan (Worst-case cua Insertion Sort - Slide 32 bai 2.1).
     */
    public static Integer[] generateReverseSortedArray(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = n - i;
        }
        return a;
    }

    /**
     * Sinh mang co nhieu khoa trung nhau (Duplicate keys - Slide 36 bai 2.2 & Slide 31 bai 2.3).
     */
    public static Integer[] generateDuplicatesArray(int n, int uniqueKeys, long seed) {
        Random rand = new Random(seed);
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = rand.nextInt(uniqueKeys);
        }
        return a;
    }

    /**
     * Ghi mang du lieu ra file text (dong dau tien la N, cac dong tiep theo la gia tri).
     * Dinh dang giong voi cac file 1Kints.txt, 250000.txt trong bai giang 1.4 Analysis of Algorithms.
     */
    public static void saveDataset(File file, Integer[] a) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file), 65536)) {
            bw.write(Integer.toString(a.length));
            bw.newLine();
            for (int i = 0; i < a.length; i++) {
                bw.write(Integer.toString(a[i]));
                bw.newLine();
            }
        }
    }

    /**
     * Doc file du lieu thi nghiem tu o dia len mang Integer[].
     */
    public static Integer[] loadDataset(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file), 65536)) {
            String firstLine = br.readLine();
            if (firstLine == null) return new Integer[0];
            int n = Integer.parseInt(firstLine.trim());
            Integer[] a = new Integer[n];
            for (int i = 0; i < n; i++) {
                String line = br.readLine();
                a[i] = Integer.parseInt(line.trim());
            }
            return a;
        }
    }
}
