package com.ultraopt;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class Draw {
    private static final int[][] INSETS = new int[32][];

    private Draw() {}

    private static int[] insets(int r) {
        int[] a = INSETS[r];
        if (a != null) return a;
        a = new int[r];
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double dx = Math.sqrt(Math.max(0, r * r - dy * dy));
            a[i] = Math.max(0, r - (int) Math.ceil(dx));
        }
        INSETS[r] = a;
        return a;
    }

    /** Скруглённый прямоугольник на основе fill. */
    public static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r > 31) r = 31;
        if (r <= 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        int[] ins = insets(r);
        g.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int in = ins[i];
            g.fill(x + in, y + i, x + w - in, y + i + 1, color);
            g.fill(x + in, y + h - 1 - i, x + w - in, y + h - i, color);
        }
    }

    /** Мягкое свечение вокруг прямоугольника. */
    public static void glow(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int rgb, int layers, int baseAlpha) {
        for (int i = layers; i >= 1; i--) {
            int a = baseAlpha * (layers - i + 1) / (layers + 1) / 2;
            rect(g, x - i, y - i, w + i * 2, h + i * 2, r + i, alpha(rgb, Math.max(2, a)));
        }
    }

    /** Стеклянная панель: фон, блик сверху, опционально акцентная линия снизу. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, int opacityPct) {
        int r = Theme.radius();
        if (Theme.glowOn()) glow(g, x, y, w, h, r, Theme.accent(), 3, 0x50);
        rect(g, x, y, w, h, r, Theme.panelBg(opacityPct));
        if (Theme.glass()) {
            int rr = Math.min(r, Math.min(w, h) / 2);
            g.fill(x + rr, y, x + w - rr, y + 1, 0x38FFFFFF);
        }
    }

    public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int color) {
        g.text(f, s, x, y, color, Theme.shadow());
    }

    public static void textCenter(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int color) {
        text(g, f, s, cx - f.width(s) / 2, y, color);
    }

    public static void textRight(GuiGraphicsExtractor g, Font f, String s, int rx, int y, int color) {
        text(g, f, s, rx - f.width(s), y, color);
    }

    /** Текст с плавным градиентом от c1 к c2. */
    public static void textGradient(GuiGraphicsExtractor g, Font f, String s, int x, int y, int c1, int c2) {
        int n = s.length();
        int cx = x;
        for (int i = 0; i < n; i++) {
            String ch = s.substring(i, i + 1);
            int col = mix(c1, c2, n <= 1 ? 0f : i / (float) (n - 1));
            g.text(f, ch, cx, y, col, Theme.shadow());
            cx += f.width(ch);
        }
    }

    public static String fit(Font f, String s, int maxW) {
        if (maxW <= 0) return "";
        if (f.width(s) <= maxW) return s;
        while (s.length() > 1 && f.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    public static void gradH(GuiGraphicsExtractor g, int x, int y, int w, int h, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        int step = w > 160 ? 3 : (w > 80 ? 2 : 1);
        for (int i = 0; i < w; i += step) {
            int col = mix(c1, c2, w <= 1 ? 0f : i / (float) (w - 1));
            g.fill(x + i, y, x + Math.min(w, i + step), y + h, col);
        }
    }

    public static void gradV(GuiGraphicsExtractor g, int x, int y, int w, int h, int c1, int c2) {
        if (w <= 0 || h <= 0) return;
        int step = h > 160 ? 3 : (h > 80 ? 2 : 1);
        for (int i = 0; i < h; i += step) {
            int col = mix(c1, c2, h <= 1 ? 0f : i / (float) (h - 1));
            g.fill(x, y + i, x + w, y + Math.min(h, i + step), col);
        }
    }

    /** Заполненный круг. */
    public static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** Кольцо. */
    public static void ring(GuiGraphicsExtractor g, int cx, int cy, int r, int thick, int color) {
        int ri = Math.max(0, r - thick);
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            if (Math.abs(dy) >= ri) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
            } else {
                int inner = (int) Math.round(Math.sqrt(Math.max(0, ri * ri - dy * dy)));
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
            }
        }
    }

    /** Линия толщиной t (шагами по пикселям). */
    public static void line(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int t, int color) {
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            g.fill(x0, y0, x0 + t, y0 + t, color);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int x = x0 + dx * i / steps, y = y0 + dy * i / steps;
            g.fill(x, y, x + t, y + t, color);
        }
    }

    /** Затемнение/подсветка по краям экрана ступенями. */
    public static void edges(GuiGraphicsExtractor g, int sw, int sh, int thick, int rgb, int maxAlpha, int steps) {
        if (maxAlpha <= 0 || steps <= 0) return;
        int stepPx = Math.max(1, thick / steps);
        for (int i = 0; i < steps; i++) {
            float k = 1f - i / (float) steps;
            int a = (int) (maxAlpha * k * k);
            if (a <= 0) continue;
            int c = alpha(rgb, a);
            int o = i * stepPx;
            g.fill(o, o, sw - o, o + stepPx, c);
            g.fill(o, sh - o - stepPx, sw - o, sh - o, c);
            g.fill(o, o + stepPx, o + stepPx, sh - o - stepPx, c);
            g.fill(sw - o - stepPx, o + stepPx, sw - o, sh - o - stepPx, c);
        }
    }

    private static int itemState = 0;

    /** Иконка предмета (если метод есть в этой версии), иначе false. */
    public static boolean item(GuiGraphicsExtractor g, ItemStack st, int x, int y) {
        if (itemState < 0 || st == null || st.isEmpty()) return false;
        if (itemState == 0) {
            itemState = itemProbe(g) ? 1 : -1;
            if (itemState < 0) return false;
        }
        Refl.call(g, "item", st, x, y);
        return true;
    }

    private static boolean itemProbe(GuiGraphicsExtractor g) {
        for (java.lang.reflect.Method m : g.getClass().getMethods()) {
            if (m.getName().equals("item") && m.getParameterCount() == 3) return true;
        }
        return false;
    }

    public static int alpha(int rgb, int a) {
        return (Anim.clamp(a, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mulA(int argb, float k) {
        int a = (int) (((argb >>> 24) & 0xFF) * Anim.clamp(k, 0f, 1f));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = Anim.clamp(t, 0f, 1f);
        int a = (int) (((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = (int) (((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int gr = (int) (((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = (int) ((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float r, g, b;
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
