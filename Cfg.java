package com.ultraopt;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class Cfg {
    public static final List<Setting> ALL = new ArrayList<>();
    public static int version = 0;

    public static final int T_OPT = 0;

    private static Setting bool(int tab, String id, String name, String desc, boolean def) {
        Setting s = new Setting(Setting.Type.BOOL, tab, id, name, desc, def ? 1 : 0, 0, 1, 1, "", null);
        ALL.add(s);
        return s;
    }

    private static Setting num(int tab, String id, String name, String desc, int def, int min, int max, int step, String suffix) {
        Setting s = new Setting(Setting.Type.INT, tab, id, name, desc, def, min, max, step, suffix, null);
        ALL.add(s);
        return s;
    }

    private static Setting choice(int tab, String id, String name, String desc, int def, String... opts) {
        Setting s = new Setting(Setting.Type.CHOICE, tab, id, name, desc, def, 0, opts.length - 1, 1, "", opts);
        ALL.add(s);
        return s;
    }

    // ===================== ОПТИМИЗАЦИЯ =====================
    public static final Setting ENTITY_CULL = bool(T_OPT, "entity_cull", "Скрытие далёких существ", "Не рисует мобов и предметы вдали", true);
    public static final Setting OCCLUSION = bool(T_OPT, "occlusion", "Скрытие за стенами", "Не рисует то, что закрыто блоками", true);
    public static final Setting DIST_MOB = num(T_OPT, "dist_mob", "Дальность: мобы", "Дальше этого мобы не рисуются", 56, 16, 128, 8, " бл.");
    public static final Setting DIST_ITEM = num(T_OPT, "dist_item", "Дальность: предметы", "Предметы и опыт", 28, 8, 64, 4, " бл.");
    public static final Setting DIST_PLAYER = num(T_OPT, "dist_player", "Дальность: игроки", "Другие игроки", 128, 32, 256, 16, " бл.");
    public static final Setting DIST_OTHER = num(T_OPT, "dist_other", "Дальность: прочее", "Рамки, стойки, лодки, вагонетки", 40, 16, 96, 8, " бл.");
    public static final Setting OCC_RECHECK = num(T_OPT, "occ_recheck", "Проверка стен", "Как часто проверять (в тиках)", 3, 1, 10, 1, " т.");
    public static final Setting OCC_MIN = num(T_OPT, "occ_min", "Не скрывать ближе", "Рядом всегда рисуем", 6, 2, 16, 2, " бл.");
    public static final Setting PARTICLE_CULL = bool(T_OPT, "particle_cull", "Отсечение частиц", "Не создаёт далёкие частицы", true);
    public static final Setting PARTICLE_DIST = num(T_OPT, "particle_dist", "Дальность частиц", "Дальше частицы не создаются", 24, 8, 64, 4, " бл.");
    public static final Setting PARTICLE_THIN = num(T_OPT, "particle_thin", "Прореживание частиц", "Часть далёких частиц пропускается", 30, 0, 90, 10, "%");
    public static final Setting TICK_SKIP = bool(T_OPT, "tick_skip", "Пропуск тиков вдали", "Далёкие существа не обновляются", true);
    public static final Setting TICK_DIST = num(T_OPT, "tick_dist", "Дальность тиков", "С какого расстояния пропускать", 96, 32, 192, 16, " бл.");
    public static final Setting HUD_INTERVAL = num(T_OPT, "hud_interval", "Обновление панели", "Реже = меньше нагрузка", 100, 50, 1000, 50, " мс");
    public static final Setting OPT_STATS = bool(T_OPT, "opt_stats", "Статистика мода", "Показывает сколько скрыто", true);

    private Cfg() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("turbine.properties");
    }

    public static void load() {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file(), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        for (Setting s : ALL) {
            String v = p.getProperty(s.id);
            if (v != null) s.deserialize(v);
        }
        version++;
    }

    public static void save() {
        Properties p = new Properties();
        for (Setting s : ALL) p.setProperty(s.id, s.serialize());
        try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
            p.store(w, "Zenith Visuals: optimization settings");
        } catch (IOException ignored) {
        }
    }
}
