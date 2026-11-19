/**
 * Lop Stopwatch do thoi gian chay cua chuong trinh.
 * Dựng theo nội dung bài giảng 1.4 Analysis of Algorithms (Slide 13).
 * Sử dụng System.nanoTime() để đạt độ chính xác cao hơn khi đo các khoảng thời gian ngắn.
 */
public class Stopwatch {
    private final long start;

    /**
     * Khởi tạo bộ bấm giờ mới và bắt đầu tính giờ.
     */
    public Stopwatch() {
        start = System.nanoTime();
    }

    /**
     * Trả về thời gian đã trôi qua kể từ lúc khởi tạo (tính bằng giây - đúng theo API bài giảng).
     */
    public double elapsedTime() {
        long now = System.nanoTime();
        return (now - start) / 1_000_000_000.0;
    }

    /**
     * Trả về thời gian đã trôi qua tính bằng mili-giây (ms) để thuận tiện lập bảng thống kê chi tiết.
     */
    public double elapsedTimeMillis() {
        long now = System.nanoTime();
        return (now - start) / 1_000_000.0;
    }
}
