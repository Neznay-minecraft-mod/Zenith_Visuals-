package com.ultraopt;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

final class Stats {
    public static int version = 0;
    public static int fps;
    public static int ping = -1;
    public static float hp, hpMax = 20f, absorb;
    public static int food = 20, armor;
    public static double speed;
    public static int cps;
    public static String fpsS = "0", pingS = "—", coords = "", dir = "", speedS = "", clock = "",
            mem = "", held = "", server = "", cpsS = "0", nick = "", entS = "0", partS = "0", tickS = "0";

    // события (обновляются каждый тик)
    public static long hurtAt, hitAt, attackAt;
    public static float hurtAmt;
    public static Entity target;
    public static boolean moving;

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String[] DIRS = {"Ю", "ЮЗ", "З", "СЗ", "С", "СВ", "В", "ЮВ"};
    private static final long[] CLICKS = new long[64];

    private static int frames, ci, entFrame, partSec, tickSec;
    private static long lastFpsT, lastRefresh, lastSecond, lastSpeedT;
    private static boolean wasDown;
    private static float lastHp = -1;
    private static double lx, lz;

    private Stats() {}

    /** Раз в клиентский тик: здоровье, цель, атаки. */
    public static void tick(Minecraft mc) {
        if (mc.player == null) {
            lastHp = -1;
            target = null;
            return;
        }
        long now = System.currentTimeMillis();
        float h = mc.player.getHealth();
        if (lastHp >= 0 && h < lastHp - 0.01f) {
            hurtAt = now;
            hurtAmt = lastHp - h;
        }
        lastHp = h;
        hp = h;

        Object t = Refl.field(mc, "crosshairPickEntity");
        target = t instanceof Entity e ? e : null;

        boolean down = mc.options.keyAttack.isDown();
        if (down && !wasDown) {
            attackAt = now;
            if (target != null) hitAt = now;
        }
        wasDown = down;
        moving = speed > 0.5;
    }

    public static void frame(Minecraft mc, long now) {
        frames++;
        if (now - lastFpsT >= 500) {
            if (lastFpsT != 0) fps = (int) (frames * 1000L / (now - lastFpsT));
            frames = 0;
            lastFpsT = now;
        }

        entFrame = CullLogic.entCulled;
        CullLogic.entCulled = 0;

        boolean down = mc.options.keyAttack.isDown();
        if (down && !wasClick) CLICKS[ci++ & 63] = now;
        wasClick = down;

        if (now - lastSecond >= 1000) {
            lastSecond = now;
            partSec = CullLogic.partCulled;
            tickSec = CullLogic.tickSkipped;
            CullLogic.partCulled = 0;
            CullLogic.tickSkipped = 0;
            ping = readPing(mc);
        }

        if (now - lastRefresh >= Cfg.HUD_INTERVAL.i) {
            lastRefresh = now;
            refresh(mc, now);
        }
    }

    private static boolean wasClick;

    private static void refresh(Minecraft mc, long now) {
        if (mc.player == null) return;
        var p = mc.player;

        fpsS = Integer.toString(fps);
        pingS = ping >= 0 ? Integer.toString(ping) : "—";
        coords = String.format(Locale.ROOT, "%.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ());

        float yaw = ((p.getYRot() % 360f) + 360f) % 360f;
        dir = DIRS[(int) Math.floor(yaw / 45f + 0.5f) % 8];

        double dt = (now - lastSpeedT) / 1000.0;
        if (lastSpeedT != 0 && dt > 0) {
            speed = Math.hypot(p.getX() - lx, p.getZ() - lz) / dt;
            speedS = String.format(Locale.ROOT, "%.1f бл/с", speed);
        }
        lx = p.getX();
        lz = p.getZ();
        lastSpeedT = now;

        clock = LocalTime.now().format(CLOCK);

        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) >> 20;
        mem = used + " / " + (rt.maxMemory() >> 20) + " МБ";

        int c = 0;
        for (long t : CLICKS) if (t != 0 && now - t <= 1000) c++;
        cps = c;
        cpsS = Integer.toString(c);

        ItemStack st = p.getMainHandItem();
        if (st.isEmpty()) {
            held = "—";
        } else {
            String n = st.getHoverName().getString();
            if (st.isDamageableItem()) n += "  " + (st.getMaxDamage() - st.getDamageValue()) + "/" + st.getMaxDamage();
            else if (st.getCount() > 1) n += "  x" + st.getCount();
            held = n;
        }

        nick = p.getName().getString();
        hp = p.getHealth();
        hpMax = Math.max(1f, Refl.f(Refl.call(p, "getMaxHealth"), 20f));
        absorb = Refl.f(Refl.call(p, "getAbsorptionAmount"), 0f);
        armor = Refl.i(Refl.call(p, "getArmorValue"), 0);
        Object fd = Refl.call(p, "getFoodData");
        food = Refl.i(Refl.call(fd, "getFoodLevel"), 20);
        server = readServer(mc);

        entS = entFrame + " сущ.";
        partS = partSec + "/с";
        tickS = tickSec + "/с";

        version++;
    }

    private static int readPing(Minecraft mc) {
        try {
            Object conn = mc.getConnection();
            if (conn == null || mc.player == null) return -1;
            Object info = conn.getClass().getMethod("getPlayerInfo", java.util.UUID.class).invoke(conn, mc.player.getUUID());
            if (info == null) return -1;
            return (Integer) info.getClass().getMethod("getLatency").invoke(info);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String readServer(Minecraft mc) {
        try {
            Object sd = mc.getCurrentServer();
            if (sd == null) return "Одиночная игра";
            return (String) sd.getClass().getField("ip").get(sd);
        } catch (Throwable t) {
            return "—";
        }
    }
}

final class ModuleManager {
    public static final List<VModule> ALL = new ArrayList<>();
    public static final String[] CATS = {"Интерфейс", "Эффекты", "Стиль", "Оптимизация"};

    /** F12: все визуалы скрыты. Состояние модулей при этом не меняется, поэтому возврат точный. */
    public static boolean hidden = false;
    private static long hiddenAt = 0;

    private static final Ctx CTX = new Ctx();
    private static Object lastLevel;
    private static long lastT;

    private ModuleManager() {}

    private static void add(VModule m) {
        ALL.add(m);
    }

    public static void init() {
        Theme.load();
        Profiles.load();

        add(new ThemeModule());

        // Интерфейс
        add(new IslandModule());
        add(new WatermarkModule());
        add(new InfoPanelModule());
        add(new KeyStrokesModule());
        add(new StatusBarsModule());
        add(new ArmorHudModule());
        add(new PotionHudModule());
        add(new CompassModule());
        add(new FpsGraphModule());
        add(new ClockModule());
        add(new SpeedometerModule());
        add(new TargetHudModule());
        add(new NotificationsModule());

        // Эффекты
        add(new CrosshairPlusModule());
        add(new HitMarkerModule());
        add(new VignetteModule());
        add(new LowHpModule());
        add(new DamageFlashModule());
        add(new SpeedLinesModule());
        add(new ScreenFrameModule());
        add(new TintModule());
        add(new CinematicBarsModule());
        add(new WorldIntroModule());

        load();
    }

    public static List<VModule> byCat(int cat) {
        List<VModule> r = new ArrayList<>();
        for (VModule m : ALL) if (m.cat == cat) r.add(m);
        return r;
    }

    public static VModule get(String id) {
        for (VModule m : ALL) if (m.id.equals(id)) return m;
        return null;
    }

    public static void toggleHidden() {
        hidden = !hidden;
        hiddenAt = System.currentTimeMillis();
    }

    public static void tick(Minecraft mc) {
        Stats.tick(mc);
        if (hidden) return;
        for (VModule m : ALL) {
            if (m.enabled || m.always()) m.tick(mc);
        }
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            lastLevel = null;
            return;
        }
        long now = System.currentTimeMillis();
        Stats.frame(mc, now);
        Notify.prune(now);

        Ctx c = CTX;
        c.g = g;
        c.mc = mc;
        c.f = mc.font;
        c.sw = mc.getWindow().getGuiScaledWidth();
        c.sh = mc.getWindow().getGuiScaledHeight();
        c.dt = lastT == 0 ? 0.016f : Math.min(0.1f, (now - lastT) / 1000f);
        c.now = now;
        lastT = now;

        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            for (VModule m : ALL) m.onJoin();
        }

        if (hidden) {
            hiddenToast(c);
            return;
        }
        if (now - hiddenAt < 1400 && hiddenAt != 0) hiddenToast(c);

        for (VModule m : ALL) {
            if (!(m.enabled || m.always())) continue;
            try {
                m.render(c);
            } catch (Throwable t) {
                m.enabled = false;
                UltraOptClient.LOG.warn("VModule {} disabled after error: {}", m.id, t.toString());
            }
        }
    }

    private static void hiddenToast(Ctx c) {
        long age = c.now - hiddenAt;
        if (!hidden && age > 1400) return;
        String s = hidden ? "Визуалы скрыты  ·  F12 — вернуть" : "Визуалы возвращены";
        float a = hidden ? Anim.easeOut(age / 250f) : 1f - Anim.clamp((age - 900) / 500f, 0f, 1f);
        if (hidden && age > 2500) a = 0.35f;
        int w = c.f.width(s) + 20, x = (c.sw - w) / 2, y = 6;
        Draw.rect(c.g, x, y, w, 18, 9, Draw.mulA(0xE00A0B0F, a));
        c.g.text(c.f, s, x + 10, y + 5, Draw.mulA(0xFFFFFFFF, a), false);
    }

    // ---------- сохранение ----------
    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("zenith-visuals.properties");
    }

    public static void load() {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        for (VModule m : ALL) {
            String on = p.getProperty(m.id + ".on");
            if (on != null && !m.always()) m.enabled = Boolean.parseBoolean(on.trim());
            for (Setting s : m.settings) {
                String v = p.getProperty(s.id);
                if (v != null) s.deserialize(v);
            }
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (VModule m : ALL) {
            p.setProperty(m.id + ".on", Boolean.toString(m.enabled));
            for (Setting s : m.settings) p.setProperty(s.id, s.serialize());
        }
        try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            p.store(w, "Zenith Visuals settings");
        } catch (IOException ignored) {
        }
    }
}
