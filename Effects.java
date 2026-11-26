package com.ultraopt;



/** Свой прицел: точка, кольцо, крест, динамический разлёт и индикатор перезарядки. */
final class CrosshairPlusModule extends VModule {
    private final Setting style, size, thick, gap, outline, dynamic, cooldown, hideVanilla, bright, colorS;
    private float spread;

    public CrosshairPlusModule() {
        super("crosshair", "Прицел", "Свой прицел с динамикой и перезарядкой", C_FX, true);
        style = choice("style", "Стиль", "Форма прицела", 2, "Точка", "Кольцо", "Крест", "Кольцо и точка");
        size = num("size", "Размер", "Длина линий или радиус", 5, 2, 14, 1, " px");
        thick = num("thick", "Толщина", "Линии прицела", 1, 1, 3, 1, " px");
        gap = num("gap", "Просвет в центре", "Расстояние до центра", 3, 0, 8, 1, " px");
        colorS = color("color", "Цвет", 8);
        bright = num("bright", "Яркость", "Непрозрачность прицела", 90, 30, 100, 5, "%");
        outline = bool("outline", "Тёмная обводка", "Лучше виден на светлом фоне", true);
        dynamic = bool("dynamic", "Разлёт при движении", "Прицел раскрывается на бегу", true);
        cooldown = bool("cooldown", "Индикатор перезарядки", "Полоска под прицелом", true);
        hideVanilla = bool("hide_vanilla", "Скрыть ванильный прицел", "Иначе будут два прицела", true);
    }

    public boolean hidesVanilla() {
        return enabled && hideVanilla.b;
    }

    @Override
    public void render(Ctx c) {
        int cx = c.sw / 2, cy = c.sh / 2;
        float target = dynamic.b ? (float) Math.min(8.0, Stats.speed * 0.7) : 0f;
        spread = Anim.approach(spread, target, c.dt, 10f);
        int g = gap.i + Math.round(spread), s = size.i, t = thick.i;
        int base = col(colorS);
        int main = Draw.mulA(base, bright.i / 100f);
        int shade = Draw.mulA(0xFF000000, 0.55f * bright.i / 100f);
        int half = t / 2;

        if (style.i == 1 || style.i == 3) {
            int r = s + g / 2 + 2;
            if (outline.b) Draw.ring(c.g, cx, cy, r + 1, t + 2, shade);
            Draw.ring(c.g, cx, cy, r, t, main);
        }
        if (style.i == 0 || style.i == 3) {
            if (outline.b) Draw.disc(c.g, cx, cy, t + 1, shade);
            Draw.disc(c.g, cx, cy, Math.max(1, t), main);
        }
        if (style.i == 2) {
            arm(c, cx - half - g - s, cy - half, s, t, outline.b, shade, main);
            arm(c, cx + half + 1 + g, cy - half, s, t, outline.b, shade, main);
            armV(c, cx - half, cy - half - g - s, s, t, outline.b, shade, main);
            armV(c, cx - half, cy + half + 1 + g, s, t, outline.b, shade, main);
        }
        if (cooldown.b) {
            float k = Refl.f(Refl.call(c.mc.player, "getAttackStrengthScale", 0.0f), 1f);
            if (k < 0.999f) {
                int w = 22, x = cx - w / 2, y = cy + s + g + 8;
                c.g.fill(x, y, x + w, y + 2, 0x50000000);
                Draw.gradH(c.g, x, y, Math.max(1, (int) (w * k)), 2, base, Theme.swatch2(colorS.i));
            }
        }
    }

    private void arm(Ctx c, int x, int y, int len, int t, boolean ol, int shade, int main) {
        if (ol) c.g.fill(x - 1, y - 1, x + len + 1, y + t + 1, shade);
        c.g.fill(x, y, x + len, y + t, main);
    }

    private void armV(Ctx c, int x, int y, int len, int t, boolean ol, int shade, int main) {
        if (ol) c.g.fill(x - 1, y - 1, x + t + 1, y + len + 1, shade);
        c.g.fill(x, y, x + t, y + len, main);
    }
}

/** Маркер попадания в центре экрана. */
final class HitMarkerModule extends VModule {
    private final Setting style, size, gap, thick, dur, colorS;

    public HitMarkerModule() {
        super("hitmarker", "Маркер попадания", "Вспышка в центре при ударе по цели", C_FX, true);
        style = choice("style", "Форма", "Вид маркера", 0, "Косой крест", "Плюс", "Четыре чёрточки");
        size = num("size", "Размер", "Длина линий", 6, 3, 14, 1, " px");
        gap = num("gap", "Просвет в центре", "Расстояние до центра", 3, 1, 10, 1, " px");
        thick = num("thick", "Толщина", "Линии маркера", 1, 1, 3, 1, " px");
        dur = num("dur", "Длительность", "Сколько виден", 300, 100, 800, 50, " мс");
        colorS = color("color", "Цвет", 8);
    }

    @Override
    public void render(Ctx c) {
        long age = c.now - Stats.hitAt;
        if (Stats.hitAt == 0 || age > dur.i) return;
        float k = 1f - age / (float) dur.i;
        int cx = c.sw / 2, cy = c.sh / 2, g = gap.i + Math.round((1f - k) * 3), s = size.i, t = thick.i;
        int col = Draw.mulA(col(colorS), Anim.easeOut(k));
        switch (style.i) {
            case 0 -> {
                Draw.line(c.g, cx - g, cy - g, cx - g - s, cy - g - s, t, col);
                Draw.line(c.g, cx + g, cy - g, cx + g + s, cy - g - s, t, col);
                Draw.line(c.g, cx - g, cy + g, cx - g - s, cy + g + s, t, col);
                Draw.line(c.g, cx + g, cy + g, cx + g + s, cy + g + s, t, col);
            }
            case 1 -> {
                c.g.fill(cx - g - s, cy, cx - g, cy + t, col);
                c.g.fill(cx + g, cy, cx + g + s, cy + t, col);
                c.g.fill(cx, cy - g - s, cx + t, cy - g, col);
                c.g.fill(cx, cy + g, cx + t, cy + g + s, col);
            }
            default -> {
                int d = g + s / 2;
                c.g.fill(cx - d - 2, cy - d - 2, cx - d + 2, cy - d - 2 + t, col);
                c.g.fill(cx + d - 2, cy - d - 2, cx + d + 2, cy - d - 2 + t, col);
                c.g.fill(cx - d - 2, cy + d + 2 - t, cx - d + 2, cy + d + 2, col);
                c.g.fill(cx + d - 2, cy + d + 2 - t, cx + d + 2, cy + d + 2, col);
            }
        }
    }
}

/** Мягкое затемнение или подсветка по краям экрана. */
final class VignetteModule extends VModule {
    private final Setting strength, size, pulse, colorS;

    public VignetteModule() {
        super("vignette", "Виньетка", "Мягкие края экрана", C_FX, false);
        strength = num("strength", "Сила", "Насколько тёмные края", 40, 5, 100, 5, "%");
        size = num("size", "Размер", "Насколько глубоко заходит", 40, 10, 90, 5, "%");
        pulse = bool("pulse", "Дыхание", "Края медленно пульсируют", false);
        colorS = color("color", "Цвет", 9);
    }

    @Override
    public void render(Ctx c) {
        int thick = Math.min(c.sw, c.sh) * size.i / 200;
        float k = pulse.b ? 0.75f + 0.25f * Anim.pulse(c.now, 0.35f) : 1f;
        Draw.edges(c.g, c.sw, c.sh, thick, col(colorS), (int) (strength.i * 2.2f * k), 16);
    }
}

/** Пульсирующая красная рамка при низком здоровье. */
final class LowHpModule extends VModule {
    private final Setting hearts, strength, pulseS, size, colorS;
    private float k;

    public LowHpModule() {
        super("lowhp", "Низкое здоровье", "Красная пульсация по краям", C_FX, true);
        hearts = num("hearts", "Порог", "Сердец, при которых начинается", 4, 1, 10, 1, " ♥");
        strength = num("strength", "Сила", "Яркость рамки", 60, 10, 100, 5, "%");
        pulseS = num("pulse", "Скорость пульса", "Ударов в секунду ×0.4", 4, 1, 8, 1, "");
        size = num("size", "Размер", "Насколько глубоко", 35, 10, 80, 5, "%");
        colorS = color("color", "Цвет", 6);
    }

    @Override
    public void render(Ctx c) {
        float limit = hearts.i * 2f;
        float want = Stats.hp <= limit ? 1f - Stats.hp / limit * 0.6f : 0f;
        k = Anim.approach(k, want, c.dt, 5f);
        if (k < 0.02f) return;
        float p = 0.55f + 0.45f * Anim.pulse(c.now, pulseS.i * 0.4f);
        int thick = Math.min(c.sw, c.sh) * size.i / 200;
        int rgb = col(colorS);
        Draw.edges(c.g, c.sw, c.sh, thick, rgb, (int) (strength.i * 2.4f * k * p), 16);
    }
}

/** Короткая вспышка при получении урона. */
final class DamageFlashModule extends VModule {
    private final Setting strength, dur, scale, full, colorS;

    public DamageFlashModule() {
        super("dmgflash", "Вспышка урона", "Мягкая вспышка по краям при уроне", C_FX, true);
        strength = num("strength", "Сила", "Яркость вспышки", 50, 10, 100, 5, "%");
        dur = num("dur", "Длительность", "Как долго гаснет", 500, 200, 1500, 100, " мс");
        scale = bool("scale", "Зависит от урона", "Сильный удар = ярче", true);
        full = bool("full", "Лёгкий оттенок на весь экран", "Помимо краёв", false);
        colorS = color("color", "Цвет", 6);
    }

    @Override
    public void render(Ctx c) {
        if (Stats.hurtAt == 0) return;
        long age = c.now - Stats.hurtAt;
        if (age > dur.i) return;
        float k = 1f - age / (float) dur.i;
        k *= k;
        float dm = scale.b ? Anim.clamp(Stats.hurtAmt / 8f, 0.35f, 1f) : 1f;
        int rgb = col(colorS);
        int a = (int) (strength.i * 2.4f * k * dm);
        Draw.edges(c.g, c.sw, c.sh, Math.min(c.sw, c.sh) / 3, rgb, a, 14);
        if (full.b) c.g.fill(0, 0, c.sw, c.sh, Draw.alpha(rgb, a / 6));
    }
}

/** Линии скорости по краям экрана при быстром движении. */
final class SpeedLinesModule extends VModule {
    private final Setting threshold, density, length, opacityS, colorS;
    private float k;

    public SpeedLinesModule() {
        super("speedlines", "Линии скорости", "Штрихи по краям при быстром движении", C_FX, false);
        threshold = num("threshold", "Порог скорости", "С какой скорости появляются", 9, 4, 30, 1, " бл/с");
        density = num("density", "Количество", "Сколько штрихов", 14, 4, 28, 2, "");
        length = num("length", "Длина", "Длина штриха", 26, 8, 70, 2, " px");
        opacityS = num("opacity", "Яркость", "Прозрачность штрихов", 40, 10, 100, 5, "%");
        colorS = color("color", "Цвет", 8);
    }

    @Override
    public void render(Ctx c) {
        float over = (float) (Stats.speed - threshold.i) / Math.max(1f, threshold.i);
        k = Anim.approach(k, Anim.clamp(over, 0f, 1f), c.dt, 6f);
        if (k < 0.03f) return;
        long seed = c.now / 90;
        int cx = c.sw / 2, cy = c.sh / 2;
        double rad = Math.hypot(cx, cy);
        int col = Draw.mulA(col(colorS), opacityS.i / 100f * k);
        for (int i = 0; i < density.i; i++) {
            long h = (seed * 31 + i) * 0x9E3779B97F4A7C15L;
            h ^= h >>> 29;
            double ang = ((h & 0xFFFF) / 65535.0) * Math.PI * 2;
            double r0 = rad * (0.62 + ((h >>> 16 & 0xFF) / 255.0) * 0.3);
            double len = length.i * (0.6 + ((h >>> 24 & 0xFF) / 255.0) * 0.8) * (0.5 + k);
            int x0 = (int) (cx + Math.cos(ang) * r0), y0 = (int) (cy + Math.sin(ang) * r0 * 0.75);
            int x1 = (int) (cx + Math.cos(ang) * (r0 + len)), y1 = (int) (cy + Math.sin(ang) * (r0 + len) * 0.75);
            if (x0 < 0 || x0 > c.sw || y0 < 0 || y0 > c.sh) continue;
            Draw.line(c.g, x0, y0, x1, y1, 1, col);
        }
    }
}

/** Тонкая светящаяся рамка по краю экрана. */
final class ScreenFrameModule extends VModule {
    private final Setting thick, glowS, animated, pulse, opacityS, colorS;

    public ScreenFrameModule() {
        super("frame", "Рамка экрана", "Светящаяся линия по краям", C_FX, false);
        thick = num("thick", "Толщина", "Линия рамки", 2, 1, 6, 1, " px");
        glowS = num("glow", "Свечение", "Ширина мягкого ореола", 24, 0, 80, 4, " px");
        animated = bool("animated", "Переливание", "Цвет плавно меняется между двумя цветами темы", true);
        pulse = bool("pulse", "Пульсация", "Свечение дышит", true);
        opacityS = num("opacity", "Яркость", "Прозрачность", 80, 10, 100, 5, "%");
        colorS = color("color", "Цвет", 0);
    }

    @Override
    public void render(Ctx c) {
        float t = animated.b ? Anim.pulse(c.now, 0.2f) : 0f;
        int base = Draw.mix(col(colorS), col2(colorS), t);
        float p = pulse.b ? 0.65f + 0.35f * Anim.pulse(c.now, 0.5f) : 1f;
        int rgb = base & 0xFFFFFF;
        int a = (int) (255 * opacityS.i / 100f);
        if (glowS.i > 0) Draw.edges(c.g, c.sw, c.sh, glowS.i, rgb, (int) (a * 0.45f * p), 12);
        int t0 = thick.i, col = Draw.alpha(rgb, a);
        c.g.fill(0, 0, c.sw, t0, col);
        c.g.fill(0, c.sh - t0, c.sw, c.sh, col);
        c.g.fill(0, t0, t0, c.sh - t0, col);
        c.g.fill(c.sw - t0, t0, c.sw, c.sh - t0, col);
    }
}

/** Цветной фильтр на весь экран. */
final class TintModule extends VModule {
    private final Setting strength, grad, colorS;

    public TintModule() {
        super("tint", "Цветовой фильтр", "Лёгкий оттенок всего экрана", C_FX, false);
        colorS = color("color", "Цвет", 0);
        strength = num("strength", "Сила", "Насколько заметен оттенок", 10, 2, 40, 2, "%");
        grad = bool("grad", "Градиент сверху вниз", "Между двумя цветами", true);
    }

    @Override
    public void render(Ctx c) {
        int a = (int) (strength.i * 2.55f);
        int c1 = Draw.alpha(col(colorS), a), c2 = Draw.alpha(grad.b ? col2(colorS) : col(colorS), a);
        if (!grad.b) c.g.fill(0, 0, c.sw, c.sh, c1);
        else Draw.gradV(c.g, 0, 0, c.sw, c.sh, c1, c2);
    }
}

/** Кинематографичные чёрные полосы сверху и снизу. */
final class CinematicBarsModule extends VModule {
    private final Setting height, speed, colorS;
    private float cur;
    private long lastRender;

    public CinematicBarsModule() {
        super("cinema", "Кино-полосы", "Чёрные полосы сверху и снизу", C_FX, false);
        height = num("height", "Высота", "Часть экрана", 12, 4, 25, 1, "%");
        speed = num("speed", "Скорость появления", "Выше = быстрее", 5, 1, 12, 1, "");
        colorS = color("color", "Цвет", 9);
    }

    @Override
    public void render(Ctx c) {
        if (c.now - lastRender > 250) cur = 0f;
        lastRender = c.now;
        cur = Anim.approach(cur, c.sh * height.i / 100f, c.dt, speed.i * 1.2f);
        int h = Math.round(cur);
        if (h <= 0) return;
        int col = colorS.i == 9 ? 0xFF000000 : col(colorS);
        c.g.fill(0, 0, c.sw, h, col);
        c.g.fill(0, c.sh - h, c.sw, c.sh, col);
    }
}

/** Красивое появление при входе в мир. */
final class WorldIntroModule extends VModule {
    private final Setting dur, style, title, colorS;
    private long t0;

    public WorldIntroModule() {
        super("intro", "Вход в мир", "Плавное появление при заходе", C_FX, true);
        dur = num("dur", "Длительность", "Сколько длится появление", 1500, 500, 4000, 100, " мс");
        style = choice("style", "Стиль", "Как открывается мир", 0, "Затухание", "Шторки");
        title = bool("title", "Название и сервер", "Надпись по центру", true);
        colorS = color("color", "Цвет заливки", 9);
    }

    @Override
    public void onJoin() {
        t0 = System.currentTimeMillis();
    }

    @Override
    public void render(Ctx c) {
        long age = c.now - t0;
        if (t0 == 0 || age > dur.i) return;
        float k = age / (float) dur.i;
        float out = 1f - Anim.easeOut(k);
        int base = colorS.i == 9 ? 0xFF000000 : col(colorS);
        if (style.i == 0) {
            c.g.fill(0, 0, c.sw, c.sh, Draw.mulA(base, out));
        } else {
            int h = (int) (c.sh / 2 * out);
            c.g.fill(0, 0, c.sw, h, base);
            c.g.fill(0, c.sh - h, c.sw, c.sh, base);
        }
        if (title.b) {
            float ta = Anim.clamp(1f - Math.abs(k - 0.35f) * 2.2f, 0f, 1f);
            if (ta > 0.02f) {
                String s = "ZENITH VISUALS";
                int x = (c.sw - c.f.width(s)) / 2, y = c.sh / 2 - 8;
                Draw.textGradient(c.g, c.f, s, x, y, Draw.mulA(Theme.accent(), ta), Draw.mulA(Theme.accent2(), ta));
                Draw.textCenter(c.g, c.f, Stats.server, c.sw / 2, y + 13, Draw.mulA(Theme.sub(), ta));
            }
        }
    }
}
