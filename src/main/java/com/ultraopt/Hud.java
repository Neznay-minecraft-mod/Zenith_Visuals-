package com.ultraopt;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Маленькая плашка с названием в углу. */
final class WatermarkModule extends VModule {
    private final Setting style, gradText, corner, ox, oy, opacity, colorS, dot;

    public WatermarkModule() {
        super("watermark", "Ватермарк", "Плашка с названием сборки", C_HUD, true);
        style = choice("style", "Содержимое", "Что написано на плашке", 1, "Название", "Название и ник", "Название и FPS");
        gradText = bool("grad_text", "Градиентное название", "Буквы плавно меняют цвет", true);
        dot = bool("dot", "Светящаяся точка", "Цветной маркер слева", true);
        colorS = color("color", "Цвет", 0);
        corner = corner("corner", 0);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 6, 0, 120, 2, " px");
        opacity = opacity(70);
    }

    @Override
    public void render(Ctx c) {
        String title = "ZENITH";
        String tail = switch (style.i) {
            case 1 -> "  ·  " + Stats.nick;
            case 2 -> "  ·  " + Stats.fpsS + " FPS";
            default -> "";
        };
        int w = 12 + (dot.b ? 10 : 0) + c.f.width(title) + c.f.width(tail) + 6, h = 18;
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);
        int x = px + 8, a = col(colorS), b = col2(colorS);
        if (dot.b) {
            Draw.glow(c.g, x - 1, py + 7, 4, 4, 2, a, 2, 0x60);
            Draw.disc(c.g, x + 1, py + 9, 2, a);
            x += 10;
        }
        if (gradText.b) Draw.textGradient(c.g, c.f, title, x, py + 5, a, b);
        else Draw.text(c.g, c.f, title, x, py + 5, a);
        Draw.text(c.g, c.f, tail, x + c.f.width(title), py + 5, Theme.sub());
    }
}

/** Панель статистики со строками подпись/значение. */
final class InfoPanelModule extends VModule {
    private static final String[] L = new String[16];
    private static final String[] V = new String[16];

    private final Setting set, labels, bar, valAcc, density, corner, ox, oy, opacity, colorS;

    public InfoPanelModule() {
        super("infopanel", "Панель статистики", "FPS, пинг, координаты, память", C_HUD, true);
        set = choice("set", "Набор строк", "Сколько информации показывать", 1, "Минимум", "Стандарт", "Полный");
        labels = bool("labels", "Подписи слева", "Серые названия строк", true);
        bar = bool("bar", "Акцентная полоса", "Цветная линия слева", true);
        valAcc = bool("val_acc", "Значения цветом акцента", "Иначе белые", false);
        density = num("density", "Высота строки", "Плотность текста", 11, 9, 15, 1, " px");
        colorS = color("color", "Цвет", 0);
        corner = corner("corner", 0);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 30, 0, 120, 2, " px");
        opacity = opacity(70);
    }

    @Override
    public void render(Ctx c) {
        int n = 0;
        L[n] = "FPS"; V[n++] = Stats.fpsS;
        L[n] = "Пинг"; V[n++] = Stats.pingS + " мс";
        if (set.i >= 1) {
            L[n] = "XYZ"; V[n++] = Stats.coords;
            L[n] = "Курс"; V[n++] = Stats.dir;
            L[n] = "Время"; V[n++] = Stats.clock;
            L[n] = "ОЗУ"; V[n++] = Stats.mem;
        }
        if (set.i >= 2) {
            L[n] = "Скорость"; V[n++] = Stats.speedS;
            L[n] = "CPS"; V[n++] = Stats.cpsS;
            L[n] = "Предмет"; V[n++] = Draw.fit(c.f, Stats.held, 150);
            if (Cfg.OPT_STATS.b) {
                L[n] = "Скрыто"; V[n++] = Stats.entS + ", част. " + Stats.partS;
                L[n] = "Тики"; V[n++] = Stats.tickS;
            }
        }
        int lw = 0, vw = 0;
        for (int i = 0; i < n; i++) {
            lw = Math.max(lw, c.f.width(L[i]));
            vw = Math.max(vw, c.f.width(V[i]));
        }
        int pad = 6, lh = density.i;
        int w = pad * 2 + (bar.b ? 4 : 0) + (labels.b ? lw + 10 : 0) + vw;
        int h = pad * 2 + n * lh - (lh - 9);
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);
        int acc = col(colorS), acc2 = col2(colorS);
        if (bar.b) {
            if (Theme.gradient()) Draw.gradV(c.g, px + 3, py + 5, 2, h - 10, acc, acc2);
            else c.g.fill(px + 3, py + 5, px + 5, py + h - 5, acc);
        }
        int tx = px + pad + (bar.b ? 4 : 0);
        for (int i = 0; i < n; i++) {
            int ty = py + pad + i * lh;
            int vx = tx;
            if (labels.b) {
                Draw.text(c.g, c.f, L[i], tx, ty, Theme.sub());
                vx = tx + lw + 10;
            }
            Draw.text(c.g, c.f, V[i], vx, ty, valAcc.b ? acc : 0xFFFFFFFF);
        }
    }
}

/** Клавиши WASD, мышь и пробел с плавной подсветкой нажатий. */
final class KeyStrokesModule extends VModule {
    private final Setting size, gap, mouse, space, cpsS, smooth, corner, ox, oy, opacity, colorS;
    private final float[] a = new float[7];

    public KeyStrokesModule() {
        super("keys", "Клавиши", "Нажатые клавиши с плавной подсветкой", C_HUD, true);
        size = num("size", "Размер клавиши", "Сторона квадрата", 20, 14, 32, 2, " px");
        gap = num("gap", "Промежуток", "Между клавишами", 2, 0, 6, 1, " px");
        mouse = bool("mouse", "Кнопки мыши", "ЛКМ и ПКМ", true);
        space = bool("space", "Пробел", "Широкая клавиша", true);
        cpsS = bool("cps", "CPS на кнопках", "Клики в секунду на ЛКМ", false);
        smooth = num("smooth", "Плавность нажатия", "Выше = быстрее отклик", 14, 4, 30, 2, "");
        colorS = color("color", "Цвет нажатия", 0);
        corner = corner("corner", 3);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 6, 0, 120, 2, " px");
        opacity = opacity(60);
    }

    @Override
    public void render(Ctx c) {
        int b = size.i, g = gap.i;
        int rows = 2 + (mouse.b ? 1 : 0) + (space.b ? 1 : 0);
        int w = b * 3 + g * 2, h = b * rows + g * (rows - 1);
        place(c, corner, ox, oy, w, h);
        var o = c.mc.options;
        boolean[] d = {o.keyUp.isDown(), o.keyLeft.isDown(), o.keyDown.isDown(), o.keyRight.isDown(),
                o.keyAttack.isDown(), o.keyUse.isDown(), o.keyJump.isDown()};
        for (int i = 0; i < 7; i++) a[i] = Anim.approach(a[i], d[i] ? 1f : 0f, c.dt, smooth.i);

        int acc = col(colorS);
        key(c, px + b + g, py, b, b, "W", a[0], acc);
        int y1 = py + b + g;
        key(c, px, y1, b, b, "A", a[1], acc);
        key(c, px + b + g, y1, b, b, "S", a[2], acc);
        key(c, px + 2 * (b + g), y1, b, b, "D", a[3], acc);
        int y = y1 + b + g;
        if (mouse.b) {
            int half = (w - g) / 2;
            key(c, px, y, half, b, cpsS.b ? "ЛКМ " + Stats.cpsS : "ЛКМ", a[4], acc);
            key(c, px + half + g, y, w - half - g, b, "ПКМ", a[5], acc);
            y += b + g;
        }
        if (space.b) key(c, px, y, w, Math.max(12, b * 3 / 4), "—", a[6], acc);
    }

    private void key(Ctx c, int x, int y, int w, int h, String s, float t, int acc) {
        GuiGraphicsExtractor g = c.g;
        Font f = c.f;
        int r = Math.min(Theme.radius(), 5);
        Draw.rect(g, x, y, w, h, r, Theme.panelBg(opacity.i));
        if (t > 0.01f) Draw.rect(g, x, y, w, h, r, Draw.mulA(acc, 0.85f * t));
        int tc = Draw.mix(0xFFFFFFFF, 0xFF101114, t);
        g.text(f, s, x + (w - f.width(s)) / 2, y + (h - 8) / 2, tc, false);
    }
}

/** Современные полоски здоровья, голода и брони с "шлейфом" урона. */
final class StatusBarsModule extends VModule {
    private final Setting width, height, labels, nums, levelColor, food, armor, trail, corner, ox, oy, opacity, colorS, hideVanilla;
    private float hpA, lagA, foodA, armA;

    public StatusBarsModule() {
        super("bars", "Полоски состояния", "Здоровье, голод и броня в современном виде", C_HUD, true);
        width = num("width", "Ширина", "Длина полосок", 110, 60, 220, 10, " px");
        height = num("height", "Толщина", "Высота полосок", 7, 4, 14, 1, " px");
        labels = bool("labels", "Подписи", "Названия слева", true);
        nums = bool("nums", "Числа", "Значения справа", true);
        levelColor = bool("level_color", "Цвет по уровню HP", "Зелёный → красный", true);
        trail = bool("trail", "Шлейф урона", "Светлая полоса при потере здоровья", true);
        food = bool("food", "Голод", "Полоска сытости", true);
        armor = bool("armor", "Броня", "Полоска защиты", true);
        colorS = color("color", "Цвет акцента", 0);
        hideVanilla = bool("hide_vanilla", "Скрыть ванильные полоски", "Прячет сердца, голод и броню Minecraft", false);
        corner = corner("corner", 2);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 44, 0, 160, 2, " px");
        opacity = opacity(60);
    }

    public boolean hidesVanilla() {
        return enabled && hideVanilla.b;
    }

    @Override
    public void render(Ctx c) {
        int rows = 1 + (food.b ? 1 : 0) + (armor.b ? 1 : 0);
        int bh = height.i, gp = 4, pad = 6;
        int lw = labels.b ? 34 : 0, nw = nums.b ? 28 : 0;
        int w = pad * 2 + lw + width.i + nw;
        int h = pad * 2 + rows * bh + (rows - 1) * gp;
        place(c, corner, ox, oy, w, h);

        float sp = 10f;
        float hp = Anim.clamp(Stats.hp / Stats.hpMax, 0f, 1f);
        hpA = Anim.approach(hpA, hp, c.dt, sp);
        lagA = hp >= lagA ? hp : Anim.approach(lagA, hp, c.dt, 2.2f);
        foodA = Anim.approach(foodA, Anim.clamp(Stats.food / 20f, 0f, 1f), c.dt, sp);
        armA = Anim.approach(armA, Anim.clamp(Stats.armor / 20f, 0f, 1f), c.dt, sp);

        Draw.panel(c.g, px, py, w, h, opacity.i);
        int acc = col(colorS), acc2 = col2(colorS);
        int x = px + pad + lw, y = py + pad;
        int hpc1 = levelColor.b ? Draw.mix(0xFFFF4D4D, 0xFF3DDC84, hpA) : acc;
        int hpc2 = levelColor.b ? Draw.mix(0xFFFF8A6D, 0xFF8DF0B0, hpA) : acc2;
        row(c, x, y, "HP", hpA, trail.b ? lagA : 0f, hpc1, hpc2, Math.round(Stats.hp) + (Stats.absorb > 0 ? "+" + Math.round(Stats.absorb) : ""), lw, nw);
        y += bh + gp;
        if (food.b) {
            row(c, x, y, "Еда", foodA, 0f, 0xFFE8A33D, 0xFFFFD27A, Integer.toString(Stats.food), lw, nw);
            y += bh + gp;
        }
        if (armor.b) row(c, x, y, "Броня", armA, 0f, 0xFF7FA8D6, 0xFFBFD9F2, Integer.toString(Stats.armor), lw, nw);
    }

    private void row(Ctx c, int x, int y, String label, float v, float lag, int c1, int c2, String num, int lw, int nw) {
        int bh = height.i, w = width.i;
        if (labels.b) Draw.text(c.g, c.f, label, x - lw, y + (bh - 8) / 2, Theme.sub());
        int r = Math.min(bh / 2, 4);
        Draw.rect(c.g, x, y, w, bh, r, 0x40FFFFFF);
        if (lag > v + 0.005f) Draw.rect(c.g, x, y, Math.max(1, (int) (w * lag)), bh, r, 0x80FFFFFF);
        int fw = (int) (w * v);
        if (fw > 0) {
            Draw.rect(c.g, x, y, Math.max(2, fw), bh, r, c1);
            if (Theme.gradient() && fw > 4) Draw.gradH(c.g, x + r, y + 1, Math.max(1, fw - 2 * r), Math.max(1, bh / 3), Draw.mulA(c2, 0.7f), Draw.mulA(c1, 0.0f));
        }
        if (nums.b) Draw.text(c.g, c.f, num, x + w + 6, y + (bh - 8) / 2, 0xFFFFFFFF);
    }
}

/** Настоящее и игровое время с иконкой солнца/луны. */
final class ClockModule extends VModule {
    private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("hh:mm a");

    private final Setting real, game, h24, icon, corner, ox, oy, opacity, colorS;

    public ClockModule() {
        super("clock", "Часы", "Реальное и игровое время", C_HUD, true);
        real = bool("real", "Реальное время", "Системные часы", true);
        game = bool("game", "Игровое время", "Время суток в мире", true);
        h24 = bool("h24", "24-часовой формат", "Иначе AM/PM", true);
        icon = bool("icon", "Солнце и луна", "Значок времени суток", true);
        colorS = color("color", "Цвет акцента", 0);
        corner = corner("corner", 1);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 6, 0, 160, 2, " px");
        opacity = opacity(70);
    }

    @Override
    public void render(Ctx c) {
        long t;
        Object dt = Refl.call(c.mc.level, "getDayTime");
        t = dt instanceof Number nn ? nn.longValue() : c.mc.level.getGameTime();
        long dayT = ((t % 24000L) + 24000L) % 24000L;
        int hh = (int) (((dayT / 1000L) + 6) % 24), mm = (int) ((dayT % 1000L) * 60 / 1000L);
        String gs = String.format("%02d:%02d", hh, mm);
        String rs = LocalTime.now().format(h24.b ? H24 : H12);

        String line = (real.b ? rs : "") + (real.b && game.b ? "  ·  " : "") + (game.b ? gs : "");
        if (line.isEmpty()) return;
        int w = 12 + (icon.b ? 14 : 0) + c.f.width(line) + 4, h = 18;
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);
        int x = px + 8;
        if (icon.b) {
            boolean day = hh >= 6 && hh < 18;
            int cy = py + 9;
            if (day) {
                Draw.glow(c.g, x - 2, cy - 2, 5, 5, 2, 0xFFD23F, 2, 0x70);
                Draw.disc(c.g, x + 1, cy, 3, 0xFFFFD23F);
            } else {
                Draw.disc(c.g, x + 1, cy, 3, 0xFFDDE6FF);
                Draw.disc(c.g, x + 3, cy - 1, 3, Theme.panelBg(100) | 0xFF000000);
            }
            x += 14;
        }
        Draw.text(c.g, c.f, line, x, py + 5, col(colorS));
    }
}

/** Спидометр: полоска и число. */
final class SpeedometerModule extends VModule {
    private final Setting width, maxS, unit, grad, label, corner, ox, oy, opacity, colorS;
    private float v;

    public SpeedometerModule() {
        super("speedo", "Спидометр", "Скорость полоской", C_HUD, false);
        width = num("width", "Ширина", "Длина полоски", 110, 70, 220, 10, " px");
        maxS = num("max", "Максимум шкалы", "Скорость, при которой полоска полная", 20, 8, 60, 2, " бл/с");
        unit = choice("unit", "Единицы", "Как показывать число", 0, "бл/с", "км/ч");
        grad = bool("grad", "Цвет по скорости", "Зелёный → красный", true);
        label = bool("label", "Подпись", "Слово «Скорость»", true);
        colorS = color("color", "Цвет акцента", 0);
        corner = corner("corner", 3);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 98, 0, 200, 2, " px");
        opacity = opacity(60);
    }

    @Override
    public void render(Ctx c) {
        v = Anim.approach(v, (float) Stats.speed, c.dt, 8f);
        int pad = 6, w = pad * 2 + width.i, h = pad * 2 + 8 + 6;
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);
        float f = Anim.clamp(v / maxS.i, 0f, 1f);
        String num = String.format(Locale.ROOT, "%.1f %s", unit.i == 0 ? v : v * 3.6f, unit.i == 0 ? "бл/с" : "км/ч");
        if (label.b) Draw.text(c.g, c.f, "Скорость", px + pad, py + pad, Theme.sub());
        Draw.textRight(c.g, c.f, num, px + w - pad, py + pad, 0xFFFFFFFF);
        int by = py + pad + 11;
        c.g.fill(px + pad, by, px + pad + width.i, by + 3, 0x40FFFFFF);
        int fw = Math.max(1, (int) (width.i * f));
        if (grad.b) Draw.gradH(c.g, px + pad, by, fw, 3, 0xFF3DDC84, Draw.mix(0xFF3DDC84, 0xFFFF4D4D, f));
        else Draw.gradH(c.g, px + pad, by, fw, 3, col(colorS), col2(colorS));
    }
}

/** График кадров по времени. */
final class FpsGraphModule extends VModule {
    private final Setting width, height, mode, numbers, avgLine, corner, ox, oy, opacity, colorS;
    private final int[] hist = new int[120];
    private int pos, filled;
    private long lastSample;

    public FpsGraphModule() {
        super("fpsgraph", "График FPS", "История кадровой частоты", C_HUD, true);
        width = num("width", "Ширина", "Количество столбиков ×2 px", 120, 60, 240, 10, " px");
        height = num("height", "Высота", "Высота графика", 30, 20, 70, 5, " px");
        mode = choice("mode", "Цвет столбиков", "Как красить график", 0, "По FPS", "Градиент акцента");
        numbers = bool("numbers", "Числа", "Текущий, минимум и максимум", true);
        avgLine = bool("avg", "Линия среднего", "Горизонтальная отметка", true);
        colorS = color("color", "Цвет акцента", 0);
        corner = corner("corner", 2);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 6, 0, 160, 2, " px");
        opacity = opacity(60);
    }

    @Override
    public void render(Ctx c) {
        if (c.now - lastSample >= 100) {
            lastSample = c.now;
            hist[pos] = Stats.fps;
            pos = (pos + 1) % hist.length;
            filled = Math.min(hist.length, filled + 1);
        }
        int bars = Math.min(width.i / 2, hist.length);
        int pad = 5, gw = bars * 2, gh = height.i;
        int top = numbers.b ? 12 : 0;
        int w = pad * 2 + gw, h = pad * 2 + gh + top;
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);

        int mx = 60, mn = Integer.MAX_VALUE, mxv = 0;
        long sum = 0;
        int cnt = Math.min(bars, filled);
        for (int i = 0; i < cnt; i++) {
            int v = hist[(pos - 1 - i + hist.length * 2) % hist.length];
            mx = Math.max(mx, v);
            mn = Math.min(mn, v);
            mxv = Math.max(mxv, v);
            sum += v;
        }
        int acc = col(colorS), acc2 = col2(colorS);
        int gx = px + pad, gy = py + pad + top;
        if (numbers.b) {
            String s = Stats.fpsS + " FPS";
            Draw.text(c.g, c.f, s, gx, py + pad, acc);
            if (cnt > 0) Draw.textRight(c.g, c.f, mn + " – " + mxv, gx + gw, py + pad, Theme.sub());
        }
        for (int i = 0; i < cnt; i++) {
            int v = hist[(pos - 1 - i + hist.length * 2) % hist.length];
            int bh = Math.max(1, v * gh / mx);
            int col = mode.i == 0 ? (v >= 60 ? 0xFF3DDC84 : (v >= 30 ? 0xFFFFD23F : 0xFFFF4D4D))
                    : Draw.mix(acc, acc2, i / (float) Math.max(1, bars));
            int x = gx + gw - (i + 1) * 2;
            c.g.fill(x, gy + gh - bh, x + 1, gy + gh, col);
        }
        if (avgLine.b && cnt > 0) {
            int ay = gy + gh - (int) (sum / cnt) * gh / mx;
            c.g.fill(gx, ay, gx + gw, ay + 1, 0x70FFFFFF);
        }
    }
}
