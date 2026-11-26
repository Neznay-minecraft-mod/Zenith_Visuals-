package com.ultraopt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Меню Zenith Visuals (Правый Shift). Нарисовано вручную: вкладки, список модулей, настройки
 * с переключателями, ползунками, выбором и палитрой. Мышь читается через GLFW, поэтому
 * меню не зависит от внутренних сигнатур экранов Minecraft.
 */
public class SettingsScreen extends Screen {
    private static final class Entry {
        final String name, desc;
        final VModule mod;
        final Setting master;
        final List<Setting> list;

        Entry(String name, String desc, VModule mod, Setting master, List<Setting> list) {
            this.name = name;
            this.desc = desc;
            this.mod = mod;
            this.master = master;
            this.list = list;
        }

        boolean on() {
            return mod != null ? (mod.enabled || mod.always()) : (master == null || master.b);
        }

        boolean canToggle() {
            return mod != null ? !mod.always() : master != null;
        }

        void toggle() {
            if (mod != null) {
                mod.enabled = !mod.enabled;
                Notify.push(mod.name, mod.enabled ? "включён" : "выключен", mod.enabled ? 0x3DDC84 : 0xFF6B6B, 1800);
            } else {
                master.next();
            }
        }

        List<Setting> settings() {
            return mod != null ? mod.settings : list;
        }
    }

    private static List<Entry> optEntries;

    private static List<Entry> opt() {
        if (optEntries == null) {
            optEntries = new ArrayList<>();
            optEntries.add(new Entry("Существа", "Скрытие далёких и закрытых существ", null, Cfg.ENTITY_CULL,
                    List.of(Cfg.OCCLUSION, Cfg.DIST_MOB, Cfg.DIST_ITEM, Cfg.DIST_PLAYER, Cfg.DIST_OTHER, Cfg.OCC_RECHECK, Cfg.OCC_MIN)));
            optEntries.add(new Entry("Частицы", "Отсечение далёких частиц", null, Cfg.PARTICLE_CULL,
                    List.of(Cfg.PARTICLE_DIST, Cfg.PARTICLE_THIN)));
            optEntries.add(new Entry("Тики существ", "Пропуск обновления далёких существ", null, Cfg.TICK_SKIP,
                    List.of(Cfg.TICK_DIST)));
            optEntries.add(new Entry("Прочее", "Статистика и частота обновления", null, null,
                    List.of(Cfg.OPT_STATS, Cfg.HUD_INTERVAL)));
        }
        return optEntries;
    }

    private final Screen parent;
    private int cat = 0;
    private final int[] selByCat = new int[4];
    private float scrollL, scrollR;
    private boolean wasDown, down, clicked;
    private int mx, my;
    private Setting dragS;
    private int dragScroll;

    public SettingsScreen(Screen parent) {
        super(Component.literal("Zenith Visuals"));
        this.parent = parent;
    }

    private List<Entry> entries() {
        if (cat == 3) return opt();
        List<Entry> r = new ArrayList<>();
        for (VModule m : ModuleManager.byCat(cat)) r.add(new Entry(m.name, m.desc, m, null, null));
        return r;
    }

    @Override
    public void onClose() {
        Cfg.save();
        ModuleManager.save();
        Notify.push("Zenith", "Настройки сохранены", 0x3DDC84, 1800);
        this.minecraft.gui.setScreen(this.parent);
    }

    /** Колёсико. Без @Override: если сигнатура совпадёт с игровой, метод подхватится. */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listEnd = this.width / 2 - 20;
        if (mouseX < listEnd) scrollL -= (float) scrollY * 24f;
        else scrollR -= (float) scrollY * 24f;
        return true;
    }

    private boolean hit(int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean hitV(int x, int y, int w, int h, int vy, int vh) {
        return hit(x, y, w, h) && my >= vy && my < vy + vh;
    }

    private static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y + 1, x + 1, y + h - 1, c);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    private static void toggle(GuiGraphicsExtractor g, int x, int y, boolean on, int acc) {
        Draw.rect(g, x, y, 24, 12, 6, on ? acc : 0x50FFFFFF);
        int kx = on ? x + 13 : x + 1;
        Draw.rect(g, kx, y + 1, 10, 10, 5, 0xFFFFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        Minecraft mc = this.minecraft;
        Font f = this.font;
        mx = mouseX;
        my = mouseY;
        down = Input.leftDown(mc);
        clicked = down && !wasDown;
        wasDown = down;
        if (!down) {
            dragS = null;
            dragScroll = 0;
        }

        int acc = Theme.accent(), acc2 = Theme.accent2();
        int pw = Math.min(530, this.width - 12), ph = Math.min(318, this.height - 12);
        int px = (this.width - pw) / 2, py = (this.height - ph) / 2;

        g.fill(0, 0, this.width, this.height, 0xB0000000);
        Draw.glow(g, px, py, pw, ph, 10, acc, 4, 0x60);
        Draw.rect(g, px, py, pw, ph, 10, 0xF2101218);
        g.fill(px + 10, py, px + pw - 10, py + 1, 0x38FFFFFF);

        // ---- шапка ----
        Draw.textGradient(g, f, "ZENITH VISUALS", px + 14, py + 10, acc, acc2);
        g.text(f, "визуал мира  ·  " + ModuleManager.ALL.size() + " модулей", px + 14, py + 22, 0xFF7C8390, false);
        if (ModuleManager.hidden) {
            String s = "ВИЗУАЛЫ СКРЫТЫ (F12)";
            Draw.rect(g, px + pw - f.width(s) - 26, py + 9, f.width(s) + 16, 16, 8, 0x60FF4D4D);
            g.text(f, s, px + pw - f.width(s) - 18, py + 13, 0xFFFFB0B0, false);
        }
        g.fill(px + 8, py + 36, px + pw - 8, py + 37, 0x24FFFFFF);

        // ---- боковая панель ----
        int sx = px + 8, sw = 112;
        for (int i = 0; i < ModuleManager.CATS.length; i++) {
            int y = py + 44 + i * 26;
            boolean sel = i == cat, hov = hit(sx, y, sw, 22);
            if (sel) Draw.rect(g, sx, y, sw, 22, 6, Draw.alpha(acc, 0x30));
            else if (hov) Draw.rect(g, sx, y, sw, 22, 6, 0x18FFFFFF);
            if (sel) g.fill(sx + 1, y + 4, sx + 3, y + 18, acc);
            Draw.disc(g, sx + 14, y + 11, 3, sel ? acc : 0xFF4A505A);
            g.text(f, ModuleManager.CATS[i], sx + 24, y + 7, sel ? 0xFFFFFFFF : 0xFFB4BAC4, false);
            if (clicked && hov && i != cat) {
                cat = i;
                scrollL = 0;
                scrollR = 0;
            }
        }
        int py2 = py + 44 + ModuleManager.CATS.length * 26 + 8;
        g.text(f, "ПРОФИЛИ", sx + 4, py2, 0xFF6B717C, false);
        for (int i = 0; i < Profiles.LIST.size(); i++) {
            int y = py2 + 12 + i * 17;
            boolean hov = hit(sx, y, sw, 15);
            Draw.rect(g, sx, y, sw, 15, 5, hov ? 0x30FFFFFF : 0x14FFFFFF);
            g.text(f, Profiles.LIST.get(i).name, sx + 8, y + 4, 0xFFD5D9E0, false);
            if (clicked && hov) Profiles.apply(Profiles.LIST.get(i));
        }

        // ---- список модулей ----
        List<Entry> es = entries();
        if (selByCat[cat] >= es.size()) selByCat[cat] = 0;
        int lx = sx + sw + 8, lw = pw < 460 ? 124 : 150, ly = py + 44, lh = ph - 44 - 30;
        int rowH = 24;
        float contentL = es.size() * (rowH + 2);
        scrollL = Anim.clamp(scrollL, 0f, Math.max(0f, contentL - lh));
        g.enableScissor(lx, ly, lx + lw, ly + lh);
        for (int i = 0; i < es.size(); i++) {
            Entry e = es.get(i);
            int y = ly + i * (rowH + 2) - (int) scrollL;
            if (y + rowH < ly || y > ly + lh) continue;
            boolean sel = i == selByCat[cat];
            boolean hov = hitV(lx, y, lw - 8, rowH, ly, lh);
            Draw.rect(g, lx, y, lw - 8, rowH, 6, sel ? Draw.alpha(acc, 0x2C) : (hov ? 0x26FFFFFF : 0x14FFFFFF));
            if (sel) g.fill(lx + 1, y + 5, lx + 3, y + rowH - 5, acc);
            int nameW = lw - 8 - 14 - (e.canToggle() ? 30 : 0);
            g.text(f, Draw.fit(f, e.name, nameW), lx + 9, y + 8, e.on() ? 0xFFFFFFFF : 0xFF8A909C, false);
            if (e.canToggle()) {
                int tx = lx + lw - 8 - 30;
                toggle(g, tx, y + 6, e.on(), acc);
                if (clicked && hitV(tx - 2, y, 32, rowH, ly, lh)) e.toggle();
                else if (clicked && hov) {
                    selByCat[cat] = i;
                    scrollR = 0;
                }
            } else if (clicked && hov) {
                selByCat[cat] = i;
                scrollR = 0;
            }
        }
        g.disableScissor();
        scrollL = bar(g, lx + lw - 4, ly, lh, scrollL, contentL, lh, 1, acc);

        // ---- настройки выбранного модуля ----
        Entry cur = es.isEmpty() ? null : es.get(selByCat[cat]);
        int rx = lx + lw + 6, rw = px + pw - 8 - rx, ry = ly;
        Draw.rect(g, rx - 2, ry - 2, rw + 4, lh + 4, 8, 0x0CFFFFFF);
        if (cur != null) {
            g.text(f, Draw.fit(f, cur.name, rw - 80), rx + 6, ry + 2, acc, false);
            g.text(f, Draw.fit(f, cur.desc, rw - 80), rx + 6, ry + 14, 0xFF7C8390, false);
            String rs = "Сброс";
            int rbw = f.width(rs) + 14, rbx = rx + rw - rbw - 6;
            boolean hovR = hit(rbx, ry + 3, rbw, 15);
            Draw.rect(g, rbx, ry + 3, rbw, 15, 6, hovR ? 0x40FFFFFF : 0x20FFFFFF);
            g.text(f, rs, rbx + 7, ry + 7, 0xFFD5D9E0, false);
            if (clicked && hovR) for (Setting s : cur.settings()) s.reset();
            g.fill(rx + 4, ry + 27, rx + rw - 4, ry + 28, 0x1CFFFFFF);

            int vy = ry + 32, vh = lh - 32 - 2;
            int x0 = rx + 4, w = rw - 16;
            List<Setting> set = cur.settings();
            float total = 0;
            for (Setting s : set) total += rowHeight(s);
            scrollR = Anim.clamp(scrollR, 0f, Math.max(0f, total - vh));
            g.enableScissor(rx, vy, rx + rw, vy + vh);
            int y = vy - (int) scrollR;
            for (Setting s : set) {
                int h = rowHeight(s);
                if (y + h >= vy && y <= vy + vh) row(g, f, s, x0, y, w, vy, vh, acc, acc2);
                else if (dragS == s && down) drag(s, x0, w);
                y += h;
            }
            g.disableScissor();
            scrollR = bar(g, rx + rw - 8, vy, vh, scrollR, total, vh, 2, acc);
        }

        // ---- низ ----
        g.text(f, "Правый Shift — меню   ·   I — остров   ·   F12 — скрыть/вернуть всё", px + 14, py + ph - 14, 0xFF6B717C, false);
        String ok = "Готово";
        int okw = f.width(ok) + 26, okx = px + pw - okw - 10, oky = py + ph - 22;
        boolean hovO = hit(okx, oky, okw, 16);
        Draw.rect(g, okx, oky, okw, 16, 8, hovO ? Draw.alpha(acc, 0xE0) : Draw.alpha(acc, 0xA0));
        g.text(f, ok, okx + 13, oky + 4, 0xFF101114, false);
        if (clicked && hovO) onClose();
    }

    private static int rowHeight(Setting s) {
        return switch (s.type) {
            case BOOL -> 22;
            case INT -> 30;
            case CHOICE -> 24;
            default -> 38;
        };
    }

    private void drag(Setting s, int x0, int w) {
        int sx = x0 + 4, sw = w - 8;
        s.setFrac((mx - sx) / (float) Math.max(1, sw));
    }

    private void row(GuiGraphicsExtractor g, Font f, Setting s, int x0, int y, int w, int vy, int vh, int acc, int acc2) {
        int h = rowHeight(s);
        boolean hov = hitV(x0 - 2, y, w + 4, h, vy, vh);
        if (hov) Draw.rect(g, x0 - 2, y, w + 4, h - 1, 5, 0x0EFFFFFF);
        switch (s.type) {
            case BOOL -> {
                g.text(f, Draw.fit(f, s.name, w - 40), x0 + 2, y + 7, 0xFFFFFFFF, false);
                toggle(g, x0 + w - 26, y + 5, s.b, acc);
                if (clicked && hov) s.next();
            }
            case INT -> {
                g.text(f, Draw.fit(f, s.name, w - 70), x0 + 2, y + 3, 0xFFFFFFFF, false);
                String v = s.text();
                g.text(f, v, x0 + w - f.width(v) - 2, y + 3, acc, false);
                int sx = x0 + 4, sw = w - 8, ty = y + 19;
                g.fill(sx, ty, sx + sw, ty + 3, 0x40FFFFFF);
                int fw = (int) (sw * s.frac());
                if (fw > 0) Draw.gradH(g, sx, ty, fw, 3, acc, acc2);
                Draw.disc(g, sx + fw, ty + 1, 4, 0xFFFFFFFF);
                if (clicked && hitV(x0, y + 12, w, 15, vy, vh)) dragS = s;
                if (dragS == s && down) drag(s, x0, w);
            }
            case CHOICE -> {
                g.text(f, Draw.fit(f, s.name, w / 2 - 4), x0 + 2, y + 8, 0xFFFFFFFF, false);
                String v = Draw.fit(f, s.text(), w / 2 - 28);
                int cw = f.width(v) + 28, cx = x0 + w - cw;
                boolean hc = hitV(cx, y + 3, cw, 17, vy, vh);
                Draw.rect(g, cx, y + 3, cw, 17, 6, hc ? 0x34FFFFFF : 0x20FFFFFF);
                g.text(f, "<", cx + 5, y + 8, 0xFF8A909C, false);
                g.text(f, ">", cx + cw - 11, y + 8, 0xFF8A909C, false);
                g.text(f, v, cx + 14, y + 8, 0xFFFFFFFF, false);
                if (clicked && hc) {
                    if (mx < cx + cw / 2) s.prev();
                    else s.next();
                }
            }
            default -> {
                g.text(f, Draw.fit(f, s.name, w - 80), x0 + 2, y + 3, 0xFFFFFFFF, false);
                String v = s.text();
                g.text(f, v, x0 + w - f.width(v) - 2, y + 3, 0xFF9AA0AA, false);
                int n = s.choices.length;
                int cw = Math.max(8, (w - 4) / n);
                for (int i = 0; i < n; i++) {
                    int cx = x0 + 2 + i * cw, cy = y + 17;
                    int cwi = cw - 2;
                    if (i == 0) Draw.gradH(g, cx, cy, cwi, 14, Theme.accent(), Theme.accent2());
                    else if (i == n - 1) {
                        float t = (System.currentTimeMillis() % 4000L) / 4000f;
                        Draw.gradH(g, cx, cy, cwi / 2, 14, Draw.hsv(t, 0.65f, 1f), Draw.hsv(t + 0.2f, 0.65f, 1f));
                        Draw.gradH(g, cx + cwi / 2, cy, cwi - cwi / 2, 14, Draw.hsv(t + 0.2f, 0.65f, 1f), Draw.hsv(t + 0.5f, 0.65f, 1f));
                    } else {
                        g.fill(cx, cy, cx + cwi, cy + 14, Theme.swatch(i));
                    }
                    boolean sel = i == s.i;
                    if (sel) box(g, cx - 1, cy - 1, cwi + 2, 16, 0xFFFFFFFF);
                    else box(g, cx, cy, cwi, 14, 0x30FFFFFF);
                    if (clicked && hitV(cx, cy, cwi, 14, vy, vh)) {
                        s.i = i;
                        Cfg.version++;
                    }
                }
            }
        }
    }

    /** Полоса прокрутки. Возвращает новое значение прокрутки (с учётом перетаскивания). */
    private float bar(GuiGraphicsExtractor g, int x, int y, int h, float scroll, float content, int view, int id, int acc) {
        if (content <= view + 1) return 0f;
        float max = content - view;
        int hh = Math.max(16, (int) (view * (view / content)));
        int ty = y + (int) ((view - hh) * (scroll / max));
        g.fill(x, y, x + 3, y + h, 0x20FFFFFF);
        boolean hov = hit(x - 3, y, 9, h);
        Draw.rect(g, x, ty, 3, hh, 1, dragScroll == id ? acc : (hov ? 0xAAFFFFFF : 0x80FFFFFF));
        if (clicked && hov) dragScroll = id;
        if (dragScroll == id && down) {
            float f = (my - y - hh / 2f) / Math.max(1f, view - hh);
            scroll = Anim.clamp(f, 0f, 1f) * max;
        }
        return Anim.clamp(scroll, 0f, max);
    }
}
