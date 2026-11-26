package com.ultraopt;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

/** Глобальная тема: палитры из assets/ultraopt/themes/*.json и общие параметры оформления. */
final class Theme {
    public static final class Pal {
        public String name;
        public int a = 0xFF7A1A, b = 0xFF3D81, bg = 0x14161C;
    }

    public static final List<Pal> PALS = new ArrayList<>();
    private static final String[] IDS = {"sunset", "ocean", "neon", "mono", "sakura", "forest", "aurora", "amethyst", "ember", "ice"};

    public static final String[] SWATCH_NAMES = {"Тема", "Оранжевый", "Голубой", "Зелёный", "Розовый", "Фиолетовый",
            "Красный", "Жёлтый", "Белый", "Чёрный", "Радуга"};
    private static final int[] SWATCH = {0, 0xFF7A1A, 0x3FB8FF, 0x3DDC84, 0xFF5FA2, 0xA66BFF, 0xFF4D4D, 0xFFD23F, 0xFFFFFF, 0x0A0A0C, 0};

    private static final Pattern STR = Pattern.compile("\"(name|accent|accent2|bg)\"\\s*:\\s*\"([^\"]*)\"");

    static ThemeModule tm;

    private Theme() {}

    private static void pal(String name, int a, int b, int bg) {
        Pal p = new Pal();
        p.name = name;
        p.a = a;
        p.b = b;
        p.bg = bg;
        PALS.add(p);
    }

    public static void load() {
        PALS.clear();
        pal("Закат", 0xFF7A3D, 0xFF3D81, 0x16121A);
        pal("Океан", 0x3FB8FF, 0x3D6BFF, 0x0F1620);
        pal("Неон", 0x39FF9E, 0x00C8FF, 0x0B1410);
        pal("Моно", 0xFFFFFF, 0x9AA0AA, 0x121212);
        pal("Сакура", 0xFF8FC0, 0xC77DFF, 0x1A1218);
        pal("Лес", 0x5BD66F, 0xB8E36A, 0x101712);
        pal("Аврора", 0x5CF2C8, 0xA66BFF, 0x0E1420);
        pal("Аметист", 0xA66BFF, 0xFF5FD2, 0x15111F);
        pal("Уголь", 0xFF5A2A, 0xFFC23D, 0x1A1210);
        pal("Лёд", 0xB8E6FF, 0x7FB4FF, 0x10151C);
    }

    public static String[] palNames() {
        String[] n = new String[PALS.size() + 2];
        for (int i = 0; i < PALS.size(); i++) n[i] = PALS.get(i).name;
        n[PALS.size()] = "Свой оттенок";
        n[PALS.size() + 1] = "Радуга";
        return n;
    }

    private static float rainbowT() {
        return (System.currentTimeMillis() % 9000L) / 9000f;
    }

    private static Pal pal() {
        if (tm == null) return PALS.isEmpty() ? new Pal() : PALS.get(0);
        int p = tm.palette.i;
        return p < PALS.size() ? PALS.get(p) : null;
    }

    public static int accent() {
        if (tm == null) return 0xFFFF7A1A;
        Pal p = pal();
        if (p != null) return 0xFF000000 | p.a;
        if (tm.palette.i == PALS.size()) return Draw.hsv(tm.hue.i / 360f, 0.72f, 1f);
        return Draw.hsv(rainbowT(), 0.65f, 1f);
    }

    public static int accent2() {
        if (tm == null) return 0xFFFF3D81;
        Pal p = pal();
        if (p != null) return 0xFF000000 | p.b;
        if (tm.palette.i == PALS.size()) return Draw.hsv(tm.hue.i / 360f + 0.12f, 0.65f, 1f);
        return Draw.hsv(rainbowT() + 0.18f, 0.65f, 1f);
    }

    public static int bgRgb() {
        Pal p = pal();
        return p != null ? p.bg : 0x14161C;
    }

    public static int panelBg(int opacityPct) {
        int base = tm == null ? 70 : tm.bgAlpha.i;
        int a = Anim.clamp(base * opacityPct / 100 * 255 / 100, 0, 255);
        return (a << 24) | bgRgb();
    }

    public static int text() { return 0xFFFFFFFF; }

    public static int sub() { return 0xFF9AA0AA; }

    public static int radius() { return tm == null ? 8 : tm.radius.i; }

    public static boolean shadow() { return tm == null || tm.shadow.b; }

    public static boolean glass() { return tm == null || tm.glass.b; }

    public static boolean glowOn() { return tm != null && tm.glow.b; }

    public static boolean gradient() { return tm == null || tm.gradient.b; }

    public static boolean anims() { return tm == null || tm.anims.b; }

    public static float speed() { return tm == null ? 8f : tm.speed.i * 1.5f; }

    /** Цвет по настройке-свотчу: 0 = цвет темы, последний = радуга. */
    public static int swatch(int i) {
        if (i <= 0) return accent();
        if (i >= SWATCH.length - 1) return Draw.hsv(rainbowT(), 0.65f, 1f);
        return 0xFF000000 | SWATCH[i];
    }

    public static int swatch2(int i) {
        if (i <= 0) return accent2();
        if (i >= SWATCH.length - 1) return Draw.hsv(rainbowT() + 0.18f, 0.65f, 1f);
        return Draw.mix(0xFF000000 | SWATCH[i], 0xFFFFFFFF, 0.3f);
    }

    public static int swatchRgb(int i) {
        return i <= 0 || i >= SWATCH.length - 1 ? 0xFFFFFF : SWATCH[i];
    }
}

/** Общая тема всего визуала: палитра, скругления, свечение, анимации. */
final class ThemeModule extends VModule {
    public final Setting palette, hue, bgAlpha, radius, gradient, glow, glass, shadow, anims, speed;

    public ThemeModule() {
        super("theme", "Тема и стиль", "Общие цвета, скругления и анимации для всех модулей", C_STYLE, true);
        palette = choice("palette", "Палитра", "Готовая тема, свой оттенок или радуга", 0, Theme.palNames());
        hue = num("hue", "Свой оттенок", "Работает на палитре «Свой оттенок»", 200, 0, 355, 5, "°");
        bgAlpha = num("bg_alpha", "Плотность фона", "Прозрачность всех панелей", 70, 20, 95, 5, "%");
        radius = num("radius", "Скругление углов", "Радиус панелей", 8, 0, 14, 2, " px");
        gradient = bool("gradient", "Градиентный акцент", "Переход между двумя цветами темы", true);
        glow = bool("glow", "Свечение панелей", "Мягкий ореол вокруг панелей", false);
        glass = bool("glass", "Стеклянный блик", "Светлая линия сверху панелей", true);
        shadow = bool("shadow", "Тень текста", "Текст читается лучше", true);
        anims = bool("anims", "Анимации", "Плавное движение всех элементов", true);
        speed = num("speed", "Скорость анимаций", "Выше = быстрее", 6, 1, 12, 1, "");
        Theme.tm = this;
    }

    @Override
    public boolean always() {
        return true;
    }
}

/** Базовый класс визуального модуля: свои настройки, включение, отрисовка. */
abstract class VModule {
    public static final int C_HUD = 0, C_FX = 1, C_STYLE = 2;

    public final String id, name, desc;
    public final int cat;
    public final boolean defOn;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<>();

    protected int px, py;

    protected VModule(String id, String name, String desc, int cat, boolean on) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.cat = cat;
        this.defOn = on;
        this.enabled = on;
    }

    /** Нельзя выключить (например, тема). */
    public boolean always() { return false; }

    public void render(Ctx c) {}

    public void tick(Minecraft mc) {}

    public void onJoin() {}

    public void resetAll() {
        for (Setting s : settings) s.reset();
    }

    // ---------- конструкторы настроек ----------
    protected Setting bool(String key, String name, String desc, boolean def) {
        Setting s = new Setting(Setting.Type.BOOL, cat, id + "." + key, name, desc, def ? 1 : 0, 0, 1, 1, "", null);
        settings.add(s);
        return s;
    }

    protected Setting num(String key, String name, String desc, int def, int min, int max, int step, String suffix) {
        Setting s = new Setting(Setting.Type.INT, cat, id + "." + key, name, desc, def, min, max, step, suffix, null);
        settings.add(s);
        return s;
    }

    protected Setting choice(String key, String name, String desc, int def, String... opts) {
        Setting s = new Setting(Setting.Type.CHOICE, cat, id + "." + key, name, desc, def, 0, opts.length - 1, 1, "", opts);
        settings.add(s);
        return s;
    }

    protected Setting color(String key, String name, int def) {
        Setting s = new Setting(Setting.Type.COLOR, cat, id + "." + key, name, "", def, 0,
                Theme.SWATCH_NAMES.length - 1, 1, "", Theme.SWATCH_NAMES);
        settings.add(s);
        return s;
    }

    protected Setting corner(String key, int def) {
        return choice(key, "Угол экрана", "Где расположен виджет", def,
                "Слева сверху", "Справа сверху", "Слева снизу", "Справа снизу");
    }

    protected Setting opacity(int def) {
        return num("opacity", "Прозрачность фона", "Насколько плотная подложка", def, 10, 100, 5, "%");
    }

    // ---------- помощники ----------
    protected int col(Setting s) { return Theme.swatch(s.i); }

    protected int col2(Setting s) { return Theme.swatch2(s.i); }

    /** Считает позицию виджета w×h в углу и кладёт в px, py. */
    protected void place(Ctx c, Setting corner, Setting ox, Setting oy, int w, int h) {
        int a = corner.i;
        px = (a == 1 || a == 3) ? c.sw - w - ox.i : ox.i;
        py = a >= 2 ? c.sh - h - oy.i : oy.i;
    }
}

/** Готовые наборы модулей: assets/ultraopt/profiles/*.json. */
final class Profiles {
    public static final class Profile {
        public String name;
        public final Set<String> modules = new HashSet<>();
    }

    public static final List<Profile> LIST = new ArrayList<>();
    private static final String[] IDS = {"minimal", "full", "streamer", "pvp"};
    private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern MODS = Pattern.compile("\"modules\"\\s*:\\s*\\[([^\\]]*)\\]");
    private static final Pattern ITEM = Pattern.compile("\"([^\"]+)\"");

    private Profiles() {}

    private static void prof(String name, String... ids) {
        Profile p = new Profile();
        p.name = name;
        for (String s : ids) p.modules.add(s);
        LIST.add(p);
    }

    public static void load() {
        LIST.clear();
        prof("Минимал", "island", "crosshair", "hitmarker", "intro");
        prof("Полный", "island", "watermark", "infopanel", "keys", "bars", "armor", "potions", "compass", "fpsgraph",
                "clock", "target", "toasts", "crosshair", "hitmarker", "vignette", "lowhp", "dmgflash", "frame", "intro");
        prof("Стример", "island", "watermark", "keys", "bars", "compass", "fpsgraph", "clock", "toasts", "crosshair",
                "vignette", "frame", "intro");
        prof("PvP", "island", "keys", "bars", "armor", "potions", "target", "crosshair", "hitmarker", "dmgflash",
                "lowhp", "fpsgraph", "speedlines");
    }

    public static void apply(Profile p) {
        for (VModule m : ModuleManager.ALL) {
            if (m.always()) continue;
            m.enabled = p.modules.contains(m.id);
        }
        Notify.push("Профиль", p.name, 0x3DDC84, 2200);
    }
}
