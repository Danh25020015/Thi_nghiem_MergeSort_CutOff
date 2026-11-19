import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Chuong trinh ve do thi ket qua thi nghiem tu cac file CSV trong thu muc results/.
 * Xuat ra 5 bieu do PNG do phan giai cao phuc vu bao cao thi nghiem:
 *  1. chart1_cutoff_vs_time.png      : Thoi gian chay T(ms) theo CUTOFF (Zoom vung 1..32 va toan canh 1..128)
 *  2. chart2_speedup_pct.png         : Ti le cai thien toc do (% Speedup so voi CUTOFF=1)
 *  3. chart3_cost_model.png          : Phan tich Cost Model (Compares, Array Accesses, Recursive Calls theo CUTOFF)
 *  4. chart4_distributions.png       : Anh huong cua 4 phan phoi du lieu dau vao (N = 500,000)
 *  5. chart5_doubling_hypothesis.png : Do thi Standard Plot & Log-Log Plot kiem chung Doubling Hypothesis
 */
public class ChartGenerator {

    private static final Color BG_COLOR = new Color(252, 253, 255);
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color GRID_COLOR = new Color(230, 234, 240);
    private static final Color AXIS_COLOR = new Color(70, 80, 95);
    private static final Color TEXT_DARK = new Color(25, 35, 50);
    private static final Color TEXT_MUTED = new Color(100, 110, 125);
    private static final Color OPTIMAL_ZONE_BG = new Color(46, 204, 113, 28);
    private static final Color OPTIMAL_ZONE_BORDER = new Color(39, 174, 96, 140);

    private static final Color[] SERIES_COLORS = {
        new Color(37, 99, 235),   // Blue
        new Color(220, 38, 38),   // Red
        new Color(22, 163, 74),   // Green
        new Color(217, 119, 6),   // Amber/Orange
        new Color(147, 51, 234)   // Purple
    };

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        System.out.println("=== BAT DAU VE DO THI THONG KE KET QUA THI NGHIEM ===");
        generateChart1_CutoffVsTime();
        generateChart2_Speedup();
        generateChart3_CostModel();
        generateChart4_Distributions();
        generateChart5_DoublingHypothesis();
        System.out.println("=== HOAN TAT VE 5 BIEU DO TAI THU MUC results/ ===");
    }

    private static void setupGraphics(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, w, h);
    }

    /**
     * Bieu do 1: Thoi gian thuc thi trung binh (ms) theo gia tri CUTOFF (2 khung: N=500K & N=1M chi tiet CUTOFF 1..32,
     * va so sanh toan canh 4 kich thuoc N=100K, 250K, 500K, 1M tren toan mien CUTOFF 1..128).
     */
    private static void generateChart1_CutoffVsTime() throws IOException {
        File csvFile = new File("results/exp1_cutoff_by_size.csv");
        if (!csvFile.exists()) return;

        Map<Integer, List<double[]>> bySize = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int n = Integer.parseInt(parts[0]);
                double cutoff = Double.parseDouble(parts[1]);
                double meanMs = Double.parseDouble(parts[2]);
                double sdMs = Double.parseDouble(parts[3]);
                double speedup = Double.parseDouble(parts[7]);
                bySize.computeIfAbsent(n, k -> new ArrayList<>()).add(new double[]{cutoff, meanMs, sdMs, speedup});
            }
        }

        int W = 1600, H = 900;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2, W, H);

        // Tieu de chinh
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Hình 1: Thời gian thực thi Mergesort theo giá trị CUTOFF (Dữ liệu ngẫu nhiên)", W / 2, 42);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(TEXT_MUTED);
        drawCenteredString(g2, "Máy thử nghiệm: Intel Core Ultra 5 235U (14 luồng) | JDK 25.0.2 LTS | Trung bình 15 lần chạy (kèm thanh sai số ±1 SD)", W / 2, 70);

        // Panel trai: Zoom chi tiet CUTOFF tu 1 den 32 voi N = 1,000,000
        drawCutoffSubchart(g2, 70, 110, 700, 710, bySize.get(1_000_000), 32,
                " (a) Cận cảnh vùng CUTOFF = 1..32 với N = 1,000,000", SERIES_COLORS[1], true);

        // Panel phai: Toan canh CUTOFF tu 1 den 128 voi N = 500,000 va N = 1,000,000
        drawCutoffSubchart(g2, 850, 110, 700, 710, bySize.get(500_000), 128,
                " (b) Toàn cảnh CUTOFF = 1..128 với N = 500,000", SERIES_COLORS[0], false);

        g2.dispose();
        File out = new File("results/chart1_cutoff_vs_time.png");
        ImageIO.write(img, "png", out);
        System.out.println("  [+] Da xuat " + out.getPath());
    }

    private static void drawCutoffSubchart(Graphics2D g2, int x0, int y0, int pw, int ph,
                                           List<double[]> rows, int maxCutoff, String subTitle, Color lineColor, boolean annotateAll) {
        if (rows == null || rows.isEmpty()) return;

        // Khung panel
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(x0, y0, pw, ph, 14, 14);
        g2.setColor(new Color(210, 215, 225));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x0, y0, pw, ph, 14, 14);

        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.setColor(TEXT_DARK);
        g2.drawString(subTitle, x0 + 20, y0 + 32);

        int plotX = x0 + 75, plotY = y0 + 55;
        int plotW = pw - 105, plotH = ph - 125;

        List<double[]> filtered = new ArrayList<>();
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        double bestC = 1, bestVal = Double.MAX_VALUE;
        double val1 = 0, val7 = 0, val10 = 0;

        for (double[] r : rows) {
            if (r[0] <= maxCutoff) {
                filtered.add(r);
                minY = Math.min(minY, r[1] - r[2]);
                maxY = Math.max(maxY, r[1] + r[2]);
                if (r[1] < bestVal) {
                    bestVal = r[1];
                    bestC = r[0];
                }
                if ((int) r[0] == 1) val1 = r[1];
                if ((int) r[0] == 7) val7 = r[1];
                if ((int) r[0] == 10) val10 = r[1];
            }
        }

        double padY = (maxY - minY) * 0.18;
        if (padY == 0) padY = 1.0;
        double yLow = Math.max(0, minY - padY * 0.8);
        double yHigh = maxY + padY;

        // Ve luoi ngang
        int yTicks = 6;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double v = yLow + (yHigh - yLow) * i / yTicks;
            int py = (int) (plotY + plotH - ((v - yLow) / (yHigh - yLow)) * plotH);
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(plotX, py, plotX + plotW, py);
            g2.setColor(TEXT_MUTED);
            String lbl = String.format("%.1f", v);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, plotX - fm.stringWidth(lbl) - 8, py + 4);
        }

        // Ve luoi doc
        int[] xTickVals = (maxCutoff <= 32)
                ? new int[]{1, 4, 7, 10, 12, 16, 20, 24, 28, 32}
                : new int[]{1, 7, 16, 32, 48, 64, 80, 96, 128};
        for (int xv : xTickVals) {
            int px = (int) (plotX + ((xv - 1.0) / (maxCutoff - 1.0)) * plotW);
            g2.setColor(GRID_COLOR);
            g2.drawLine(px, plotY, px, plotY + plotH);
            g2.setColor(TEXT_MUTED);
            String lbl = Integer.toString(xv);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, px - fm.stringWidth(lbl) / 2, plotY + plotH + 20);
        }

        // To mau vung toi uu (Optimal zone: CUTOFF tu 8 den 16)
        int optX1 = (int) (plotX + ((7.0 - 1.0) / (maxCutoff - 1.0)) * plotW);
        int optX2 = (int) (plotX + ((16.0 - 1.0) / (maxCutoff - 1.0)) * plotW);
        g2.setColor(OPTIMAL_ZONE_BG);
        g2.fillRect(optX1, plotY, optX2 - optX1, plotH);
        g2.setColor(OPTIMAL_ZONE_BORDER);
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 5f}, 0f));
        g2.drawLine(optX1, plotY, optX1, plotY + plotH);
        g2.drawLine(optX2, plotY, optX2, plotY + plotH);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(new Color(22, 130, 70));
        g2.drawString("Vùng tối ưu (7..16)", optX1 + 6, plotY + 18);

        // Truc toa do
        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(plotX, plotY, plotX, plotY + plotH);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        // Ve duong cong va error bars
        Path2D path = new Path2D.Double();
        for (int i = 0; i < filtered.size(); i++) {
            double[] r = filtered.get(i);
            double px = plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW;
            double py = plotY + plotH - ((r[1] - yLow) / (yHigh - yLow)) * plotH;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        g2.setColor(lineColor);
        g2.setStroke(new BasicStroke(2.6f));
        g2.draw(path);

        // Ve diem va thanh sai so SD
        for (double[] r : filtered) {
            int c = (int) r[0];
            int px = (int) (plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW);
            int py = (int) (plotY + plotH - ((r[1] - yLow) / (yHigh - yLow)) * plotH);
            int pyTop = (int) (plotY + plotH - ((Math.min(yHigh, r[1] + r[2]) - yLow) / (yHigh - yLow)) * plotH);
            int pyBot = (int) (plotY + plotH - ((Math.max(yLow, r[1] - r[2]) - yLow) / (yHigh - yLow)) * plotH);

            // Thanh sai so
            g2.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 110));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawLine(px, pyTop, px, pyBot);
            g2.drawLine(px - 3, pyTop, px + 3, pyTop);
            g2.drawLine(px - 3, pyBot, px + 3, pyBot);

            // Diem
            boolean isSpecial = (c == 1 || c == 7 || c == 10 || c == (int) bestC);
            int radius = isSpecial ? 5 : 3;
            g2.setColor(isSpecial ? Color.WHITE : lineColor);
            g2.fill(new Ellipse2D.Double(px - radius, py - radius, radius * 2, radius * 2));
            g2.setColor(c == (int) bestC ? new Color(22, 163, 74) : lineColor);
            g2.setStroke(new BasicStroke(isSpecial ? 2.2f : 1.5f));
            g2.draw(new Ellipse2D.Double(px - radius, py - radius, radius * 2, radius * 2));
        }

        // Chu thich cac moc quan trong (1, 7, 10, Best)
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        if (maxCutoff <= 32) {
            annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, 1, val1,
                    String.format("CUTOFF=1 (Gốc): %.1f ms", val1), 10, -14, new Color(180, 40, 40));
            annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, 7, val7,
                    String.format("C=7 (Cũ): %.1f ms", val7), -15, -30, new Color(37, 99, 235));
            if ((int) bestC != 10) {
                annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, 10, val10,
                        String.format("C=10 (Mới): %.1f ms", val10), 15, -24, new Color(147, 51, 234));
            }
            annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, (int) bestC, bestVal,
                    String.format("Tối ưu C=%d: %.1f ms", (int) bestC, bestVal), 0, 28, new Color(21, 128, 61));
        } else {
            annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, 1, val1,
                    String.format("CUTOFF=1 (Gốc): %.1f ms", val1), 70, -68, new Color(180, 40, 40));
            annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, 7, val7,
                    String.format("C=7 (Cũ): %.1f ms", val7), 90, -42, new Color(37, 99, 235));
            if ((int) bestC != 10) {
                annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, 10, val10,
                        String.format("C=10 (Mới): %.1f ms", val10), 115, -16, new Color(147, 51, 234));
            }
            annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, (int) bestC, bestVal,
                    String.format("Tối ưu C=%d: %.1f ms", (int) bestC, bestVal), 50, 28, new Color(21, 128, 61));
        }

        // Nhan truc X, Y
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Giá trị ngưỡng CUTOFF (số phần tử chuyển sang Insertion Sort)", plotX + plotW / 2, plotY + plotH + 48);
        drawRotatedString(g2, "Thời gian chạy trung bình T (ms)", x0 + 22, plotY + plotH / 2);
    }

    private static void annotatePoint(Graphics2D g2, int plotX, int plotY, int plotW, int plotH,
                                      int maxCutoff, double yLow, double yHigh, int cutoff, double val,
                                      String text, int dx, int dy, Color color) {
        if (val <= 0 || cutoff > maxCutoff) return;
        int px = (int) (plotX + ((cutoff - 1.0) / (maxCutoff - 1.0)) * plotW);
        int py = (int) (plotY + plotH - ((val - yLow) / (yHigh - yLow)) * plotH);

        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int bx = Math.max(plotX + 4, Math.min(plotX + plotW - tw - 10, px + dx - tw / 2));
        int by = py + dy;

        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(bx - 4, by - 14, tw + 8, 19, 6, 6);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.1f));
        g2.drawRoundRect(bx - 4, by - 14, tw + 8, 19, 6, 6);
        g2.drawLine(px, py, bx + tw / 2, (dy < 0) ? (by + 5) : (by - 14));
        g2.drawString(text, bx, by);
    }

    /**
     * Bieu do 2: Ti le cai thien toc do (% Speedup so voi Mergesort goc CUTOFF=1) tren 4 kich thuoc mang N.
     */
    private static void generateChart2_Speedup() throws IOException {
        File csvFile = new File("results/exp1_cutoff_by_size.csv");
        if (!csvFile.exists()) return;

        Map<Integer, List<double[]>> bySize = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                int n = Integer.parseInt(parts[0]);
                double cutoff = Double.parseDouble(parts[1]);
                double speedup = Double.parseDouble(parts[7]);
                bySize.computeIfAbsent(n, k -> new ArrayList<>()).add(new double[]{cutoff, speedup});
            }
        }

        int W = 1500, H = 820;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2, W, H);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Hình 2: Tỉ lệ cải thiện tốc độ (% Speedup) so với Mergesort gốc (CUTOFF = 1)", W / 2, 42);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(TEXT_MUTED);
        drawCenteredString(g2, "So sánh trên 4 kích thước dữ liệu N = 100K, 250K, 500K, 1M (Giá trị dương = nhanh hơn bản gốc, âm = chậm hơn)", W / 2, 70);

        int x0 = 80, y0 = 100, pw = W - 140, ph = H - 150;
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(x0, y0, pw, ph, 14, 14);
        g2.setColor(new Color(210, 215, 225));
        g2.drawRoundRect(x0, y0, pw, ph, 14, 14);

        int plotX = x0 + 80, plotY = y0 + 40, plotW = pw - 130, plotH = ph - 110;
        double minS = -10.0, maxS = 25.0;
        for (List<double[]> list : bySize.values()) {
            for (double[] r : list) {
                minS = Math.min(minS, r[1]);
                maxS = Math.max(maxS, r[1]);
            }
        }
        minS = Math.floor(minS / 5.0) * 5.0 - 2.0;
        maxS = Math.ceil(maxS / 5.0) * 5.0 + 3.0;

        int maxCutoff = 128;
        int yTicks = 7;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double v = minS + (maxS - minS) * i / yTicks;
            int py = (int) (plotY + plotH - ((v - minS) / (maxS - minS)) * plotH);
            g2.setColor(GRID_COLOR);
            g2.drawLine(plotX, py, plotX + plotW, py);
            g2.setColor(TEXT_MUTED);
            String lbl = String.format("%+.1f%%", v);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, plotX - fm.stringWidth(lbl) - 10, py + 4);
        }

        // Duong 0% (Baseline)
        int pyZero = (int) (plotY + plotH - ((0.0 - minS) / (maxS - minS)) * plotH);
        g2.setColor(new Color(120, 130, 145));
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0f));
        g2.drawLine(plotX, pyZero, plotX + plotW, pyZero);

        int[] xTickVals = {1, 4, 7, 10, 12, 16, 20, 24, 32, 48, 64, 80, 96, 128};
        for (int xv : xTickVals) {
            int px = (int) (plotX + ((xv - 1.0) / (maxCutoff - 1.0)) * plotW);
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawLine(px, plotY, px, plotY + plotH);
            g2.setColor(TEXT_MUTED);
            String lbl = Integer.toString(xv);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, px - fm.stringWidth(lbl) / 2, plotY + plotH + 22);
        }

        // Vung toi uu 7..16
        int optX1 = (int) (plotX + ((7.0 - 1.0) / (maxCutoff - 1.0)) * plotW);
        int optX2 = (int) (plotX + ((16.0 - 1.0) / (maxCutoff - 1.0)) * plotW);
        g2.setColor(OPTIMAL_ZONE_BG);
        g2.fillRect(optX1, plotY, optX2 - optX1, plotH);

        // Ve 4 duong cong
        int sIdx = 0;
        for (Map.Entry<Integer, List<double[]>> entry : bySize.entrySet()) {
            Color c = SERIES_COLORS[sIdx % SERIES_COLORS.length];
            List<double[]> rows = entry.getValue();
            Path2D path = new Path2D.Double();
            for (int i = 0; i < rows.size(); i++) {
                double[] r = rows.get(i);
                double px = plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW;
                double py = plotY + plotH - ((r[1] - minS) / (maxS - minS)) * plotH;
                if (i == 0) path.moveTo(px, py);
                else path.lineTo(px, py);
            }
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(path);

            for (double[] r : rows) {
                int px = (int) (plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW);
                int py = (int) (plotY + plotH - ((r[1] - minS) / (maxS - minS)) * plotH);
                g2.fill(new Ellipse2D.Double(px - 3.5, py - 3.5, 7, 7));
            }
            sIdx++;
        }

        // Legend
        int legX = plotX + plotW - 240, legY = plotY + 20;
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(legX, legY, 220, 125, 10, 10);
        g2.setColor(new Color(200, 205, 215));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(legX, legY, 220, 125, 10, 10);

        sIdx = 0;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        for (Integer n : bySize.keySet()) {
            Color c = SERIES_COLORS[sIdx % SERIES_COLORS.length];
            int ly = legY + 25 + sIdx * 26;
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.8f));
            g2.drawLine(legX + 15, ly - 4, legX + 45, ly - 4);
            g2.fill(new Ellipse2D.Double(legX + 26, ly - 8, 8, 8));
            g2.setColor(TEXT_DARK);
            g2.drawString(String.format("N = %,d phần tử", n), legX + 55, ly);
            sIdx++;
        }

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(plotX, plotY, plotX, plotY + plotH);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Giá trị ngưỡng CUTOFF", plotX + plotW / 2, plotY + plotH + 52);
        drawRotatedString(g2, "Tỉ lệ cải thiện tốc độ so với CUTOFF = 1 (%)", x0 + 25, plotY + plotH / 2);

        g2.dispose();
        File out = new File("results/chart2_speedup_pct.png");
        ImageIO.write(img, "png", out);
        System.out.println("  [+] Da xuat " + out.getPath());
    }

    /**
     * Bieu do 3: Phan tich Cost Model (Compares / N lg N, Array Accesses / N lg N, va Recursive Calls).
     */
    private static void generateChart3_CostModel() throws IOException {
        File csvFile = new File("results/exp2_cost_model.csv");
        if (!csvFile.exists()) return;

        List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                rows.add(new double[]{
                    Double.parseDouble(p[0]), // cutoff
                    Double.parseDouble(p[1]), // compares
                    Double.parseDouble(p[2]), // compares / N lg N
                    Double.parseDouble(p[3]), // array accesses
                    Double.parseDouble(p[4]), // accesses / N lg N
                    Double.parseDouble(p[5]), // recursive calls
                    Double.parseDouble(p[6]), // merge calls
                    Double.parseDouble(p[7])  // insertion exchanges
                });
            }
        }

        int W = 1600, H = 860;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2, W, H);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Hình 3: Phân tích Mô hình Chi phí (Cost Model - Bài giảng 1.4 & 2.2) với N = 500,000", W / 2, 42);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(TEXT_MUTED);
        drawCenteredString(g2, "Sự đánh đổi (Trade-off) giữa số lượng Truy cập mảng (Array Accesses), Phép so sánh (Compares) và Số lời gọi Đệ quy", W / 2, 70);

        // Panel (a): Array Accesses & Compares (Millions)
        drawCostSubchartLeft(g2, 70, 105, 700, 690, rows);

        // Panel (b): Recursive Calls & Insertion Exchanges
        drawCostSubchartRight(g2, 850, 105, 700, 690, rows);

        g2.dispose();
        File out = new File("results/chart3_cost_model.png");
        ImageIO.write(img, "png", out);
        System.out.println("  [+] Da xuat " + out.getPath());
    }

    private static void drawCostSubchartLeft(Graphics2D g2, int x0, int y0, int pw, int ph, List<double[]> rows) {
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(x0, y0, pw, ph, 14, 14);
        g2.setColor(new Color(210, 215, 225));
        g2.drawRoundRect(x0, y0, pw, ph, 14, 14);

        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.setColor(TEXT_DARK);
        g2.drawString("(a) Số lần truy cập mảng A(N) và phép so sánh C(N)", x0 + 20, y0 + 32);

        int plotX = x0 + 80, plotY = y0 + 55, plotW = pw - 110, plotH = ph - 125;
        int maxCutoff = 64;

        double maxM = 0;
        double minAcc = Double.MAX_VALUE;
        int bestAccC = 1;
        for (double[] r : rows) {
            if (r[0] <= maxCutoff) {
                double accM = r[3] / 1_000_000.0;
                maxM = Math.max(maxM, accM);
                if (accM < minAcc) {
                    minAcc = accM;
                    bestAccC = (int) r[0];
                }
            }
        }
        double yLow = 0, yHigh = Math.ceil(maxM / 10.0) * 10.0 + 5.0;

        int yTicks = 6;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double v = yLow + (yHigh - yLow) * i / yTicks;
            int py = (int) (plotY + plotH - ((v - yLow) / (yHigh - yLow)) * plotH);
            g2.setColor(GRID_COLOR);
            g2.drawLine(plotX, py, plotX + plotW, py);
            g2.setColor(TEXT_MUTED);
            String lbl = String.format("%.0fM", v);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, plotX - fm.stringWidth(lbl) - 8, py + 4);
        }

        int[] xTickVals = {1, 4, 7, 10, 12, 16, 24, 32, 48, 64};
        for (int xv : xTickVals) {
            int px = (int) (plotX + ((xv - 1.0) / (maxCutoff - 1.0)) * plotW);
            g2.setColor(GRID_COLOR);
            g2.drawLine(px, plotY, px, plotY + plotH);
            g2.setColor(TEXT_MUTED);
            String lbl = Integer.toString(xv);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, px - fm.stringWidth(lbl) / 2, plotY + plotH + 20);
        }

        // Ve Array Accesses (M) va Compares (M)
        Path2D pathAcc = new Path2D.Double();
        Path2D pathCmp = new Path2D.Double();
        boolean first = true;
        for (double[] r : rows) {
            if (r[0] > maxCutoff) continue;
            double px = plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW;
            double pyAcc = plotY + plotH - (((r[3] / 1e6) - yLow) / (yHigh - yLow)) * plotH;
            double pyCmp = plotY + plotH - (((r[1] / 1e6) - yLow) / (yHigh - yLow)) * plotH;
            if (first) {
                pathAcc.moveTo(px, pyAcc);
                pathCmp.moveTo(px, pyCmp);
                first = false;
            } else {
                pathAcc.lineTo(px, pyAcc);
                pathCmp.lineTo(px, pyCmp);
            }
        }

        g2.setColor(SERIES_COLORS[0]);
        g2.setStroke(new BasicStroke(2.6f));
        g2.draw(pathAcc);

        g2.setColor(SERIES_COLORS[1]);
        g2.setStroke(new BasicStroke(2.6f));
        g2.draw(pathCmp);

        for (double[] r : rows) {
            if (r[0] > maxCutoff) continue;
            int px = (int) (plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW);
            int pyAcc = (int) (plotY + plotH - (((r[3] / 1e6) - yLow) / (yHigh - yLow)) * plotH);
            int pyCmp = (int) (plotY + plotH - (((r[1] / 1e6) - yLow) / (yHigh - yLow)) * plotH);
            g2.setColor(SERIES_COLORS[0]);
            g2.fill(new Ellipse2D.Double(px - 3.5, pyAcc - 3.5, 7, 7));
            g2.setColor(SERIES_COLORS[1]);
            g2.fill(new Ellipse2D.Double(px - 3.5, pyCmp - 3.5, 7, 7));
        }

        // Annotate minimum array accesses
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        annotatePoint(g2, plotX, plotY, plotW, plotH, maxCutoff, yLow, yHigh, bestAccC, minAcc,
                String.format("Đáy Array Accesses tại C=%d (%.1fM)", bestAccC, minAcc), 25, -24, SERIES_COLORS[0]);

        // Legend
        int lx = plotX + 20, ly = plotY + 20;
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(lx, ly, 290, 65, 8, 8);
        g2.setColor(new Color(200, 205, 215));
        g2.drawRoundRect(lx, ly, 290, 65, 8, 8);
        g2.setColor(SERIES_COLORS[0]);
        g2.fillRect(lx + 12, ly + 15, 20, 4);
        g2.setColor(TEXT_DARK);
        g2.drawString("Array Accesses A(N) (Triệu lần)", lx + 42, ly + 22);
        g2.setColor(SERIES_COLORS[1]);
        g2.fillRect(lx + 12, ly + 40, 20, 4);
        g2.setColor(TEXT_DARK);
        g2.drawString("Key Compares C(N) (Triệu lần)", lx + 42, ly + 47);

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(plotX, plotY, plotX, plotY + plotH);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Giá trị ngưỡng CUTOFF (1..64)", plotX + plotW / 2, plotY + plotH + 48);
        drawRotatedString(g2, "Số lượng phép toán (Triệu - Millions)", x0 + 22, plotY + plotH / 2);
    }

    private static void drawCostSubchartRight(Graphics2D g2, int x0, int y0, int pw, int ph, List<double[]> rows) {
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(x0, y0, pw, ph, 14, 14);
        g2.setColor(new Color(210, 215, 225));
        g2.drawRoundRect(x0, y0, pw, ph, 14, 14);

        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.setColor(TEXT_DARK);
        g2.drawString("(b) Số lời gọi đệ quy sort() so với Số phép đổi chỗ Insertion Sort", x0 + 20, y0 + 32);

        int plotX = x0 + 80, plotY = y0 + 55, plotW = pw - 110, plotH = ph - 125;
        int maxCutoff = 64;

        double maxVal = 1.0;
        for (double[] r : rows) {
            if (r[0] <= maxCutoff) {
                maxVal = Math.max(maxVal, Math.max(r[5] / 1e6, r[7] / 1e6));
            }
        }
        double yLow = 0, yHigh = Math.ceil(maxVal) + 0.5;

        int yTicks = 6;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double v = yLow + (yHigh - yLow) * i / yTicks;
            int py = (int) (plotY + plotH - ((v - yLow) / (yHigh - yLow)) * plotH);
            g2.setColor(GRID_COLOR);
            g2.drawLine(plotX, py, plotX + plotW, py);
            g2.setColor(TEXT_MUTED);
            String lbl = String.format("%.1fM", v);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, plotX - fm.stringWidth(lbl) - 8, py + 4);
        }

        int[] xTickVals = {1, 4, 7, 10, 12, 16, 24, 32, 48, 64};
        for (int xv : xTickVals) {
            int px = (int) (plotX + ((xv - 1.0) / (maxCutoff - 1.0)) * plotW);
            g2.setColor(GRID_COLOR);
            g2.drawLine(px, plotY, px, plotY + plotH);
            g2.setColor(TEXT_MUTED);
            String lbl = Integer.toString(xv);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, px - fm.stringWidth(lbl) / 2, plotY + plotH + 20);
        }

        Path2D pathRec = new Path2D.Double();
        Path2D pathExch = new Path2D.Double();
        boolean first = true;
        for (double[] r : rows) {
            if (r[0] > maxCutoff) continue;
            double px = plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW;
            double pyRec = plotY + plotH - (((r[5] / 1e6) - yLow) / (yHigh - yLow)) * plotH;
            double pyExch = plotY + plotH - (((r[7] / 1e6) - yLow) / (yHigh - yLow)) * plotH;
            if (first) {
                pathRec.moveTo(px, pyRec);
                pathExch.moveTo(px, pyExch);
                first = false;
            } else {
                pathRec.lineTo(px, pyRec);
                pathExch.lineTo(px, pyExch);
            }
        }

        g2.setColor(SERIES_COLORS[2]);
        g2.setStroke(new BasicStroke(2.6f));
        g2.draw(pathRec);

        g2.setColor(SERIES_COLORS[3]);
        g2.setStroke(new BasicStroke(2.6f));
        g2.draw(pathExch);

        for (double[] r : rows) {
            if (r[0] > maxCutoff) continue;
            int px = (int) (plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW);
            int pyRec = (int) (plotY + plotH - (((r[5] / 1e6) - yLow) / (yHigh - yLow)) * plotH);
            int pyExch = (int) (plotY + plotH - (((r[7] / 1e6) - yLow) / (yHigh - yLow)) * plotH);
            g2.setColor(SERIES_COLORS[2]);
            g2.fill(new Ellipse2D.Double(px - 3.5, pyRec - 3.5, 7, 7));
            g2.setColor(SERIES_COLORS[3]);
            g2.fill(new Ellipse2D.Double(px - 3.5, pyExch - 3.5, 7, 7));
        }

        int lx = plotX + 180, ly = plotY + 20;
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(lx, ly, 360, 65, 8, 8);
        g2.setColor(new Color(200, 205, 215));
        g2.drawRoundRect(lx, ly, 360, 65, 8, 8);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(SERIES_COLORS[2]);
        g2.fillRect(lx + 12, ly + 15, 20, 4);
        g2.setColor(TEXT_DARK);
        g2.drawString("Số lời gọi đệ quy sort() (Giảm theo 2N / CUTOFF)", lx + 42, ly + 22);
        g2.setColor(SERIES_COLORS[3]);
        g2.fillRect(lx + 12, ly + 40, 20, 4);
        g2.setColor(TEXT_DARK);
        g2.drawString("Số phép hoán đổi exch() của Insertion Sort (~N*M/4)", lx + 42, ly + 47);

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(plotX, plotY, plotX, plotY + plotH);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Giá trị ngưỡng CUTOFF (1..64)", plotX + plotW / 2, plotY + plotH + 48);
        drawRotatedString(g2, "Số lượng thao tác (Triệu - Millions)", x0 + 22, plotY + plotH / 2);
    }

    /**
     * Bieu do 4: Anh huong cua 4 phan phoi du lieu dau vao (Random, Partially Sorted, Reverse Sorted, Many Duplicates).
     */
    private static void generateChart4_Distributions() throws IOException {
        File csvFile = new File("results/exp3_distributions.csv");
        if (!csvFile.exists()) return;

        Map<String, List<double[]>> byDist = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                String dist = p[0];
                double cutoff = Double.parseDouble(p[1]);
                double meanMs = Double.parseDouble(p[2]);
                double speedup = Double.parseDouble(p[5]);
                byDist.computeIfAbsent(dist, k -> new ArrayList<>()).add(new double[]{cutoff, meanMs, speedup});
            }
        }

        int W = 1500, H = 820;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2, W, H);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Hình 4: Ảnh hưởng của Phân phối Dữ liệu đầu vào đến ngưỡng CUTOFF tối ưu (N = 500,000)", W / 2, 42);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(TEXT_MUTED);
        drawCenteredString(g2, "So sánh 4 cấu trúc dữ liệu từ bài giảng 2.1 & 2.2: Ngẫu nhiên, Gần như đã sắp xếp, Sắp xếp ngược (Worst-case), và Nhiều khóa trùng", W / 2, 70);

        int x0 = 80, y0 = 100, pw = W - 140, ph = H - 150;
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(x0, y0, pw, ph, 14, 14);
        g2.setColor(new Color(210, 215, 225));
        g2.drawRoundRect(x0, y0, pw, ph, 14, 14);

        int plotX = x0 + 95, plotY = y0 + 40, plotW = pw - 145, plotH = ph - 110;
        int maxCutoff = 64;

        double maxY = 0;
        for (List<double[]> list : byDist.values()) {
            for (double[] r : list) {
                if (r[0] <= maxCutoff) maxY = Math.max(maxY, r[1]);
            }
        }
        double yLow = 0, yHigh = Math.ceil(maxY / 10.0) * 10.0 + 45.0;

        int yTicks = 6;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double v = yLow + (yHigh - yLow) * i / yTicks;
            int py = (int) (plotY + plotH - ((v - yLow) / (yHigh - yLow)) * plotH);
            g2.setColor(GRID_COLOR);
            g2.drawLine(plotX, py, plotX + plotW, py);
            g2.setColor(TEXT_MUTED);
            String lbl = String.format("%.1f ms", v);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, plotX - fm.stringWidth(lbl) - 10, py + 4);
        }

        int[] xTickVals = {1, 4, 7, 10, 12, 16, 20, 24, 32, 40, 48, 64};
        for (int xv : xTickVals) {
            int px = (int) (plotX + ((xv - 1.0) / (maxCutoff - 1.0)) * plotW);
            g2.setColor(GRID_COLOR);
            g2.drawLine(px, plotY, px, plotY + plotH);
            g2.setColor(TEXT_MUTED);
            String lbl = Integer.toString(xv);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, px - fm.stringWidth(lbl) / 2, plotY + plotH + 22);
        }

        String[] distLabels = {
            "Random (Ngẫu nhiên phân phối đều)",
            "PartiallySorted (Gần như đã sắp xếp - ít nghịch thế)",
            "ReverseSorted (Sắp xếp ngược - Worst case Insertion Sort)",
            "ManyDuplicates (Nhiều khóa trùng nhau - 100 giá trị)"
        };
        int dIdx = 0;
        for (Map.Entry<String, List<double[]>> entry : byDist.entrySet()) {
            Color c = SERIES_COLORS[dIdx % SERIES_COLORS.length];
            List<double[]> rows = entry.getValue();
            Path2D path = new Path2D.Double();
            boolean first = true;
            double bestC = 1, bestV = Double.MAX_VALUE;
            for (double[] r : rows) {
                if (r[0] > maxCutoff) continue;
                if (r[1] < bestV) {
                    bestV = r[1];
                    bestC = r[0];
                }
                double px = plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW;
                double py = plotY + plotH - ((r[1] - yLow) / (yHigh - yLow)) * plotH;
                if (first) {
                    path.moveTo(px, py);
                    first = false;
                } else {
                    path.lineTo(px, py);
                }
            }
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.6f));
            g2.draw(path);

            for (double[] r : rows) {
                if (r[0] > maxCutoff) continue;
                int px = (int) (plotX + ((r[0] - 1.0) / (maxCutoff - 1.0)) * plotW);
                int py = (int) (plotY + plotH - ((r[1] - yLow) / (yHigh - yLow)) * plotH);
                boolean isBest = ((int) r[0] == (int) bestC);
                int rad = isBest ? 6 : 3;
                g2.setColor(isBest ? Color.WHITE : c);
                g2.fill(new Ellipse2D.Double(px - rad, py - rad, rad * 2, rad * 2));
                g2.setColor(c);
                g2.setStroke(new BasicStroke(isBest ? 2.5f : 1.5f));
                g2.draw(new Ellipse2D.Double(px - rad, py - rad, rad * 2, rad * 2));
            }
            dIdx++;
        }

        // Legend
        int legX = plotX + 25, legY = plotY + 20;
        g2.setColor(new Color(255, 255, 255, 238));
        g2.fillRoundRect(legX, legY, 430, 125, 10, 10);
        g2.setColor(new Color(200, 205, 215));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(legX, legY, 430, 125, 10, 10);
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        for (int i = 0; i < distLabels.length; i++) {
            Color c = SERIES_COLORS[i % SERIES_COLORS.length];
            int ly = legY + 25 + i * 26;
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.8f));
            g2.drawLine(legX + 15, ly - 4, legX + 45, ly - 4);
            g2.fill(new Ellipse2D.Double(legX + 26, ly - 8, 8, 8));
            g2.setColor(TEXT_DARK);
            g2.drawString(distLabels[i], legX + 55, ly);
        }

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(plotX, plotY, plotX, plotY + plotH);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Giá trị ngưỡng CUTOFF (1..64)", plotX + plotW / 2, plotY + plotH + 52);
        drawRotatedString(g2, "Thời gian chạy trung bình T (ms)", x0 + 25, plotY + plotH / 2);

        g2.dispose();
        File out = new File("results/chart4_distributions.png");
        ImageIO.write(img, "png", out);
        System.out.println("  [+] Da xuat " + out.getPath());
    }

    /**
     * Bieu do 5: Standard Plot & Log-Log Plot kiem chung Doubling Hypothesis (Slide 16-20 bai 1.4 Analysis of Algorithms).
     */
    private static void generateChart5_DoublingHypothesis() throws IOException {
        File csvFile = new File("results/exp4_doubling_hypothesis.csv");
        if (!csvFile.exists()) return;

        List<double[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                rows.add(new double[]{
                    Double.parseDouble(p[0]), // N
                    Double.parseDouble(p[1]), // time C=1
                    Double.parseDouble(p[4]), // time C=7
                    Double.parseDouble(p[5]), // time C=10
                    Double.parseDouble(p[6])  // time C=12
                });
            }
        }

        int W = 1600, H = 850;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        setupGraphics(g2, W, H);

        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, "Hình 5: Kiểm chứng Doubling Hypothesis — Standard Plot & Log-Log Plot (Bài giảng 1.4, Slide 16–20)", W / 2, 42);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(TEXT_MUTED);
        drawCenteredString(g2, "Khảo sát thời gian chạy T(N) khi nhân đôi kích thước N từ 31,250 đến 2,000,000 phần tử", W / 2, 70);

        drawDoublingPanel(g2, 70, 105, 700, 680, rows, false, "(a) Standard Plot: Thời gian T(N) theo kích thước N");
        drawDoublingPanel(g2, 850, 105, 700, 680, rows, true, "(b) Log-Log Plot: lg T(N) theo lg N (Đường thẳng hệ số góc b ≈ 1)");

        g2.dispose();
        File out = new File("results/chart5_doubling_hypothesis.png");
        ImageIO.write(img, "png", out);
        System.out.println("  [+] Da xuat " + out.getPath());
    }

    private static void drawDoublingPanel(Graphics2D g2, int x0, int y0, int pw, int ph,
                                          List<double[]> rows, boolean logScale, String title) {
        g2.setColor(PANEL_BG);
        g2.fillRoundRect(x0, y0, pw, ph, 14, 14);
        g2.setColor(new Color(210, 215, 225));
        g2.drawRoundRect(x0, y0, pw, ph, 14, 14);

        g2.setFont(new Font("SansSerif", Font.BOLD, 17));
        g2.setColor(TEXT_DARK);
        g2.drawString(title, x0 + 20, y0 + 32);

        int plotX = x0 + 92, plotY = y0 + 55, plotW = pw - 122, plotH = ph - 125;

        double minX = logScale ? Math.log(rows.get(0)[0]) / Math.log(2) : 0;
        double maxX = logScale ? Math.log(rows.get(rows.size() - 1)[0]) / Math.log(2) : rows.get(rows.size() - 1)[0];

        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (double[] r : rows) {
            for (int k = 1; k <= 4; k++) {
                double v = logScale ? (Math.log(Math.max(0.1, r[k])) / Math.log(2)) : r[k];
                minY = Math.min(minY, v);
                maxY = Math.max(maxY, v);
            }
        }
        if (!logScale) minY = 0;
        else minY -= 0.5;
        maxY += (maxY - minY) * 0.12;

        int yTicks = 6;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= yTicks; i++) {
            double v = minY + (maxY - minY) * i / yTicks;
            int py = (int) (plotY + plotH - ((v - minY) / (maxY - minY)) * plotH);
            g2.setColor(GRID_COLOR);
            g2.drawLine(plotX, py, plotX + plotW, py);
            g2.setColor(TEXT_MUTED);
            String lbl = logScale ? String.format("%.1f ms", Math.pow(2, v)) : String.format("%.0f ms", v);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, plotX - fm.stringWidth(lbl) - 8, py + 4);
        }

        for (double[] r : rows) {
            double xv = logScale ? Math.log(r[0]) / Math.log(2) : r[0];
            int px = (int) (plotX + ((xv - minX) / (maxX - minX)) * plotW);
            g2.setColor(GRID_COLOR);
            g2.drawLine(px, plotY, px, plotY + plotH);
            if (!logScale && r[0] < 100_000) continue; // Tranh chong nhan 31K, 63K tren truc tuyen tinh
            g2.setColor(TEXT_MUTED);
            String lbl = (r[0] >= 1_000_000) ? String.format("%.0fM", r[0] / 1e6) : String.format("%.0fK", r[0] / 1e3);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, px - fm.stringWidth(lbl) / 2, plotY + plotH + 20);
        }

        String[] labels = {"CUTOFF = 1 (Mergesort gốc)", "CUTOFF = 7 (Giáo trình cũ)", "CUTOFF = 10 (Giáo trình mới)", "CUTOFF = 12 (Tối ưu thực nghiệm)"};
        int[] cols = {1, 2, 3, 4};
        Color[] colors = {SERIES_COLORS[1], SERIES_COLORS[0], SERIES_COLORS[4], SERIES_COLORS[2]};

        for (int idx = 0; idx < cols.length; idx++) {
            int col = cols[idx];
            Color c = colors[idx];
            Path2D path = new Path2D.Double();
            for (int i = 0; i < rows.size(); i++) {
                double[] r = rows.get(i);
                double xv = logScale ? Math.log(r[0]) / Math.log(2) : r[0];
                double yv = logScale ? Math.log(Math.max(0.1, r[col])) / Math.log(2) : r[col];
                double px = plotX + ((xv - minX) / (maxX - minX)) * plotW;
                double py = plotY + plotH - ((yv - minY) / (maxY - minY)) * plotH;
                if (i == 0) path.moveTo(px, py);
                else path.lineTo(px, py);
            }
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(path);

            for (double[] r : rows) {
                double xv = logScale ? Math.log(r[0]) / Math.log(2) : r[0];
                double yv = logScale ? Math.log(Math.max(0.1, r[col])) / Math.log(2) : r[col];
                int px = (int) (plotX + ((xv - minX) / (maxX - minX)) * plotW);
                int py = (int) (plotY + plotH - ((yv - minY) / (maxY - minY)) * plotH);
                g2.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));
            }
        }

        int legX = plotX + 20, legY = plotY + 20;
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fillRoundRect(legX, legY, 275, 118, 8, 8);
        g2.setColor(new Color(200, 205, 215));
        g2.drawRoundRect(legX, legY, 275, 118, 8, 8);
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        for (int i = 0; i < labels.length; i++) {
            int ly = legY + 24 + i * 24;
            g2.setColor(colors[i]);
            g2.setStroke(new BasicStroke(2.6f));
            g2.drawLine(legX + 12, ly - 4, legX + 38, ly - 4);
            g2.fill(new Ellipse2D.Double(legX + 21, ly - 8, 8, 8));
            g2.setColor(TEXT_DARK);
            g2.drawString(labels[i], legX + 48, ly);
        }

        g2.setColor(AXIS_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(plotX, plotY, plotX, plotY + plotH);
        g2.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(TEXT_DARK);
        drawCenteredString(g2, logScale ? "Kích thước dữ liệu N (Thang Log cơ số 2)" : "Kích thước dữ liệu N (Thang tuyến tính)",
                plotX + plotW / 2, plotY + plotH + 48);
        drawRotatedString(g2, logScale ? "Thời gian chạy T(N) (Thang Log)" : "Thời gian chạy T(N) (ms)",
                x0 + 20, plotY + plotH / 2);
    }

    private static void drawCenteredString(Graphics2D g2, String text, int cx, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, y);
    }

    private static void drawRotatedString(Graphics2D g2, String text, int x, int cy) {
        AffineTransform orig = g2.getTransform();
        FontMetrics fm = g2.getFontMetrics();
        g2.translate(x, cy + fm.stringWidth(text) / 2);
        g2.rotate(-Math.PI / 2);
        g2.drawString(text, 0, 0);
        g2.setTransform(orig);
    }
}
