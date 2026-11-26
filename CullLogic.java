package com.ultraopt;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public final class CullLogic {
    // счётчики для статистики
    public static int entCulled, partCulled, tickSkipped;

    private static final HashMap<Integer, Long> CACHE = new HashMap<>();
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
    private static int rng = 0x2545F491;

    private CullLogic() {}

    public static void clear() {
        CACHE.clear();
    }

    /** Быстрый псевдослучайный 0..bound-1 */
    public static int rnd(int bound) {
        rng ^= rng << 13;
        rng ^= rng >>> 17;
        rng ^= rng << 5;
        return (rng & 0x7FFFFFFF) % bound;
    }

    public static boolean visible(Entity e, double cx, double cy, double cz) {
        if (!Cfg.ENTITY_CULL.b) return true;
        boolean v = visible0(e, cx, cy, cz);
        if (!v) entCulled++;
        return v;
    }

    private static boolean visible0(Entity e, double cx, double cy, double cz) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null || e == mc.player || e == mc.getCameraEntity()) return true;
        if (e.isCurrentlyGlowing()) return true;

        double dx = e.getX() - cx;
        double dy = e.getY() - cy;
        double dz = e.getZ() - cz;
        double d2 = dx * dx + dy * dy + dz * dz;

        if (e.getBbWidth() > 2.5f || e.getBbHeight() > 3.5f) {
            return d2 < 160 * 160;
        }

        double max;
        if (e instanceof Player) max = Cfg.DIST_PLAYER.i;
        else if (e instanceof ItemEntity || e instanceof ExperienceOrb) max = Cfg.DIST_ITEM.i;
        else if (e instanceof LivingEntity) max = Cfg.DIST_MOB.i;
        else max = Cfg.DIST_OTHER.i;

        if (d2 > max * max) return false;
        if (!Cfg.OCCLUSION.b) return true;

        double near = Cfg.OCC_MIN.i;
        if (d2 < near * near) return true;

        long now = level.getGameTime();
        Long c = CACHE.get(e.getId());
        if (c != null && now - (c >> 1) < Cfg.OCC_RECHECK.i) {
            return (c & 1L) == 1L;
        }

        double h = e.getBbHeight();
        boolean vis = rayClear(level, cx, cy, cz, e.getX(), e.getY() + h * 0.5, e.getZ())
                || rayClear(level, cx, cy, cz, e.getX(), e.getY() + h * 0.95, e.getZ())
                || rayClear(level, cx, cy, cz, e.getX(), e.getY() + 0.1, e.getZ());

        CACHE.put(e.getId(), (now << 1) | (vis ? 1L : 0L));
        if (CACHE.size() > 4096) CACHE.clear();
        return vis;
    }

    public static boolean skipTick(Entity e) {
        if (!Cfg.TICK_SKIP.b) return false;
        Player p = Minecraft.getInstance().player;
        if (p == null || e instanceof Player || p.getRootVehicle() == e) return false;
        double dx = e.getX() - p.getX();
        double dy = e.getY() - p.getY();
        double dz = e.getZ() - p.getZ();
        double lim = Cfg.TICK_DIST.i;
        if (dx * dx + dy * dy + dz * dz > lim * lim) {
            tickSkipped++;
            return true;
        }
        return false;
    }

    private static boolean rayClear(Level level, double cx, double cy, double cz,
                                    double tx, double ty, double tz) {
        double dx = tx - cx, dy = ty - cy, dz = tz - cz;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) (len / 0.8);
        if (steps < 2) return true;
        double sx = dx / steps, sy = dy / steps, sz = dz / steps;
        for (int i = 1; i < steps; i++) {
            POS.set(Mth.floor(cx + sx * i), Mth.floor(cy + sy * i), Mth.floor(cz + sz * i));
            if (level.getBlockState(POS).isSolidRender()) return false;
        }
        return true;
    }
}
