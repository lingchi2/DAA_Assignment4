package city;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;
import javax.imageio.ImageIO;

public final class PlotWriter {
    public record Series(String name, double[] xs, double[] ys) {
    }

    private static final Color[] COLORS = {
        new Color(0x1f77b4), new Color(0xd62728), new Color(0x2ca02c),
        new Color(0xff7f0e), new Color(0x9467bd), new Color(0x8c564b)
    };

    private PlotWriter() {
    }

    public static void draw(String path, String title, String xLabel, String yLabel,
                            List<Series> series, boolean logX, boolean logY,
                            double yFloor, String caption) throws IOException {
        System.setProperty("java.awt.headless", "true");
        int w = 1100;
        int h = 700;
        int left = 100;
        int right = 280;
        int top = 70;
        int bottom = 130;
        int pw = w - left - right;
        int ph = h - top - bottom;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        double xMin = Double.MAX_VALUE;
        double xMax = -Double.MAX_VALUE;
        double yMin = Double.MAX_VALUE;
        double yMax = -Double.MAX_VALUE;
        TreeSet<Double> xTicks = new TreeSet<>();
        for (Series s : series) {
            for (int i = 0; i < s.xs.length; i++) {
                xMin = Math.min(xMin, s.xs[i]);
                xMax = Math.max(xMax, s.xs[i]);
                xTicks.add(s.xs[i]);
                if (!logY || s.ys[i] > 0) {
                    yMin = Math.min(yMin, s.ys[i]);
                }
                yMax = Math.max(yMax, s.ys[i]);
            }
        }
        double yLo;
        double yHi;
        double[] yTicks;
        if (logY) {
            int lo = (int) Math.floor(Math.log10(yMin));
            int hi = (int) Math.ceil(Math.log10(yMax));
            if (hi == lo) {
                hi++;
            }
            yLo = Math.pow(10, lo);
            yHi = Math.pow(10, hi);
            yTicks = new double[hi - lo + 1];
            for (int i = 0; i < yTicks.length; i++) {
                yTicks[i] = Math.pow(10, lo + i);
            }
        } else {
            yLo = yFloor;
            double step = niceStep((yMax - yLo) / 6.0);
            yHi = Math.ceil(yMax / step) * step;
            int cnt = (int) Math.round((yHi - yLo) / step) + 1;
            yTicks = new double[cnt];
            for (int i = 0; i < cnt; i++) {
                yTicks[i] = yLo + i * step;
            }
        }

        double xLo = xMin;
        double xHi = xMax;
        if (xHi == xLo) {
            xHi = xLo + 1;
        }

        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics fm = g.getFontMetrics();

        for (double t : yTicks) {
            int y = mapY(t, yLo, yHi, logY, top, ph);
            g.setColor(new Color(0xdddddd));
            g.drawLine(left, y, left + pw, y);
            g.setColor(Color.BLACK);
            String lab = fmt(t);
            g.drawString(lab, left - 8 - fm.stringWidth(lab), y + 5);
        }
        for (double t : xTicks) {
            int x = mapX(t, xLo, xHi, logX, left, pw);
            g.setColor(new Color(0xeeeeee));
            g.drawLine(x, top, x, top + ph);
            g.setColor(Color.BLACK);
            g.drawLine(x, top + ph, x, top + ph + 5);
            String lab = fmt(t);
            g.drawString(lab, x - fm.stringWidth(lab) / 2, top + ph + 20);
        }
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(left, top, pw, ph);

        int idx = 0;
        for (Series s : series) {
            Color c = COLORS[idx % COLORS.length];
            g.setColor(c);
            g.setStroke(new BasicStroke(2.2f));
            int px = -1;
            int py = -1;
            for (int i = 0; i < s.xs.length; i++) {
                if (logY && s.ys[i] <= 0) {
                    continue;
                }
                int x = mapX(s.xs[i], xLo, xHi, logX, left, pw);
                int y = mapY(s.ys[i], yLo, yHi, logY, top, ph);
                if (px >= 0) {
                    g.drawLine(px, py, x, y);
                }
                px = x;
                py = y;
            }
            for (int i = 0; i < s.xs.length; i++) {
                if (logY && s.ys[i] <= 0) {
                    continue;
                }
                int x = mapX(s.xs[i], xLo, xHi, logX, left, pw);
                int y = mapY(s.ys[i], yLo, yHi, logY, top, ph);
                drawMarker(g, idx, x, y);
            }
            idx++;
        }

        int ly = top + 10;
        idx = 0;
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        for (Series s : series) {
            Color c = COLORS[idx % COLORS.length];
            g.setColor(c);
            g.setStroke(new BasicStroke(2.2f));
            int lx = left + pw + 20;
            g.drawLine(lx, ly, lx + 28, ly);
            drawMarker(g, idx, lx + 14, ly);
            g.setColor(Color.BLACK);
            g.drawString(s.name, lx + 36, ly + 5);
            ly += 26;
            idx++;
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics tf = g.getFontMetrics();
        g.drawString(title, left + (pw - tf.stringWidth(title)) / 2, 38);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics lf = g.getFontMetrics();
        g.drawString(xLabel, left + (pw - lf.stringWidth(xLabel)) / 2, top + ph + 52);
        AffineTransform old = g.getTransform();
        g.rotate(-Math.PI / 2);
        g.drawString(yLabel, -(top + (ph + lf.stringWidth(yLabel)) / 2), 28);
        g.setTransform(old);

        g.setFont(new Font("SansSerif", Font.ITALIC, 14));
        g.drawString(caption, left, h - 28);

        g.dispose();
        File f = new File(path);
        f.getParentFile().mkdirs();
        ImageIO.write(img, "png", f);
    }

    private static void drawMarker(Graphics2D g, int idx, int x, int y) {
        int r = 5;
        switch (idx % 4) {
            case 0 -> g.fillOval(x - r, y - r, 2 * r, 2 * r);
            case 1 -> g.fillRect(x - r, y - r, 2 * r, 2 * r);
            case 2 -> g.fillPolygon(new int[]{x - r - 1, x + r + 1, x}, new int[]{y + r, y + r, y - r - 1}, 3);
            default -> g.fillPolygon(new int[]{x - r - 1, x, x + r + 1, x}, new int[]{y, y - r - 1, y, y + r + 1}, 4);
        }
    }

    private static int mapX(double v, double lo, double hi, boolean log, int left, int pw) {
        double t = log ? (Math.log10(v) - Math.log10(lo)) / (Math.log10(hi) - Math.log10(lo)) : (v - lo) / (hi - lo);
        return (int) Math.round(left + 20 + t * (pw - 40));
    }

    private static int mapY(double v, double lo, double hi, boolean log, int top, int ph) {
        double t;
        if (log) {
            double lv = v > 0 ? Math.log10(v) : Math.log10(lo);
            t = (lv - Math.log10(lo)) / (Math.log10(hi) - Math.log10(lo));
        } else {
            t = (v - lo) / (hi - lo);
        }
        return (int) Math.round(top + ph - t * ph);
    }

    private static double niceStep(double raw) {
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double r = raw / mag;
        double n = r <= 1 ? 1 : r <= 2 ? 2 : r <= 5 ? 5 : 10;
        return n * mag;
    }

    private static String fmt(double v) {
        if (v == 0) {
            return "0";
        }
        double a = Math.abs(v);
        if (a >= 100000 || a < 0.01) {
            return String.format("%.0e", v).replace("e+0", "e").replace("e+", "e").replace("e-0", "e-");
        }
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.format("%.2g", v);
    }
}
