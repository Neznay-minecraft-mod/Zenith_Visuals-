package com.ultraopt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Броня и предмет в руке с иконками и полоской прочности. */
final class ArmorHudModule extends VModule {
    private static final String[] SLOTS = {"HEAD", "CHEST", "LEGS", "FEET", "MAINHAND"};
    private static Object[] slotEnums;

    private final Setting orient, nums, bar, hand, barColor, corner, ox, oy, opacity, colorS;

    public ArmorHudModule() {
        super("armor", "Броня", "Иконки брони с прочностью", C_HUD, true);
        orient = choice("orient", "Расположение", "Столбиком или в ряд", 0, "Вертикально", "Горизонтально");
        nums = choice("nums", "Числа", "Что писать рядом с иконкой", 1, "Нет", "Проценты", "Остаток");
        bar = bool("bar", "Полоска прочности", "Под каждой иконкой", true);
        barColor = choice("bar_color", "Цвет полоски", "Как красить прочность", 0, "По износу", "Акцент");
        hand = bool("hand", "Предмет в руке", "Пятая ячейка", true);
        colorS = color("color", "Цвет акцента", 0);
        corner = corner("corner", 1);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 30, 0, 160, 2, " px");
        opacity = opacity(50);
    }

    private static Object slot(int i) {
        if (slotEnums == null) {
            slotEnums = new Object[SLOTS.length];
            try {
                Class<?> cls = Class.forName("net.minecraft.world.entity.EquipmentSlot");
                for (Object o : cls.getEnumConstants()) {
                    for (int k = 0; k < SLOTS.length; k++) {
                        if (((Enum<?>) o).name().equals(SLOTS[k])) slotEnums[k] = o;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        return slotEnums[i];
    }

    @Override
    public void render(Ctx c) {
        var p = c.mc.player;
        int n = hand.b ? 5 : 4;
        ItemStack[] st = new ItemStack[n];
        int count = 0;
        for (int i = 0; i < n; i++) {
            Object o = slot(i) == null ? null : Refl.call(p, "getItemBySlot", slot(i));
            st[i] = o instanceof ItemStack s ? s : ItemStack.EMPTY;
            if (!st[i].isEmpty()) count++;
        }
        if (count == 0) return;

        boolean vert = orient.i == 0;
        int cell = 20, pad = 4, numW = nums.i == 0 ? 0 : 30;
        int cw = cell + (vert ? numW : 0), chh = cell;
        int w = pad * 2 + (vert ? cw : (cw + numW) * count - 0);
        int h = pad * 2 + (vert ? chh * count : chh);
        if (!vert) w = pad * 2 + (cell + numW) * count;
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);

        int idx = 0;
        for (int i = 0; i < n; i++) {
            ItemStack s = st[i];
            if (s.isEmpty()) continue;
            int x = vert ? px + pad : px + pad + idx * (cell + numW);
            int y = vert ? py + pad + idx * chh : py + pad;
            boolean icon = Draw.item(c.g, s, x + 2, y + 1);
            if (!icon) {
                String t = s.getHoverName().getString();
                c.g.text(c.f, t.isEmpty() ? "?" : t.substring(0, 1), x + 6, y + 6, 0xFFFFFFFF, true);
            }
            float f = 1f;
            String num = "";
            if (s.isDamageableItem()) {
                int max = s.getMaxDamage(), left = max - s.getDamageValue();
                f = max <= 0 ? 1f : left / (float) max;
                num = nums.i == 1 ? Math.round(f * 100) + "%" : Integer.toString(left);
            } else if (s.getCount() > 1) {
                num = "x" + s.getCount();
            }
            if (bar.b && s.isDamageableItem()) {
                int bc = barColor.i == 0 ? Draw.mix(0xFFFF4D4D, 0xFF3DDC84, f) : col(colorS);
                c.g.fill(x + 2, y + 17, x + 18, y + 18, 0x50FFFFFF);
                c.g.fill(x + 2, y + 17, x + 2 + Math.max(1, (int) (16 * f)), y + 18, bc);
            }
            if (nums.i != 0 && !num.isEmpty()) {
                Draw.text(c.g, c.f, num, x + cell + 2, y + 6, Theme.sub());
            }
            idx++;
        }
    }
}

/** Список активных эффектов с таймером и полоской. */
final class PotionHudModule extends VModule {
    private static final String[] ROMAN = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};

    private final Setting rows, bars, level, colorMode, corner, ox, oy, opacity, colorS, hideVanilla;
    private final Map<String, Integer> maxDur = new HashMap<>();

    public PotionHudModule() {
        super("potions", "Эффекты зелий", "Активные эффекты с таймером", C_HUD, true);
        rows = num("rows", "Максимум строк", "Сколько эффектов показывать", 6, 2, 12, 1, "");
        bars = bool("bars", "Полоска времени", "Сколько осталось от максимума", true);
        level = bool("level", "Уровень эффекта", "Римские цифры", true);
        colorMode = choice("color_mode", "Цвет значка", "Как красить маркер", 0, "Польза/вред", "Акцент");
        colorS = color("color", "Цвет акцента", 0);
        hideVanilla = bool("hide_vanilla", "Скрыть ванильные значки", "Убирает иконки эффектов Minecraft", true);
        corner = corner("corner", 1);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 80, 0, 200, 2, " px");
        opacity = opacity(60);
    }

    public boolean hidesVanilla() {
        return enabled && hideVanilla.b;
    }

    private static String time(int ticks) {
        if (ticks < 0 || ticks > 99999) return "∞";
        int s = ticks / 20;
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

    @Override
    public void render(Ctx c) {
        Object coll = Refl.call(c.mc.player, "getActiveEffects");
        if (!(coll instanceof Collection<?> all) || all.isEmpty()) return;

        List<String> names = new ArrayList<>();
        List<Integer> durs = new ArrayList<>();
        List<Boolean> good = new ArrayList<>();
        for (Object inst : all) {
            if (names.size() >= rows.i) break;
            Object holder = Refl.call(inst, "getEffect");
            Object eff = Refl.call(holder, "value");
            Object comp = Refl.call(eff == null ? holder : eff, "getDisplayName");
            String nm = Refl.s(Refl.call(comp, "getString"), "Эффект");
            int amp = Refl.i(Refl.call(inst, "getAmplifier"), 0);
            int dur = Refl.i(Refl.call(inst, "getDuration"), 0);
            Object ben = Refl.call(eff, "isBeneficial");
            if (level.b && amp > 0) nm += amp < ROMAN.length ? ROMAN[amp + 1 > 10 ? 10 : amp + 1] : " " + (amp + 1);
            names.add(nm);
            durs.add(dur);
            good.add(!(ben instanceof Boolean b) || b);
            maxDur.merge(nm, dur, Math::max);
        }
        if (names.isEmpty()) return;

        int lh = bars.b ? 14 : 11, pad = 6, tw = 0;
        for (int i = 0; i < names.size(); i++) tw = Math.max(tw, c.f.width(names.get(i)) + c.f.width(time(durs.get(i))));
        int w = pad * 2 + 8 + tw + 14, h = pad * 2 + names.size() * lh - (bars.b ? 3 : 2);
        place(c, corner, ox, oy, w, h);
        Draw.panel(c.g, px, py, w, h, opacity.i);

        int acc = col(colorS);
        for (int i = 0; i < names.size(); i++) {
            int y = py + pad + i * lh;
            int mc = colorMode.i == 0 ? (good.get(i) ? 0xFF3DDC84 : 0xFFFF4D4D) : acc;
            Draw.disc(c.g, px + pad + 2, y + 4, 2, mc);
            Draw.text(c.g, c.f, names.get(i), px + pad + 8, y, 0xFFFFFFFF);
            Draw.textRight(c.g, c.f, time(durs.get(i)), px + w - pad, y, Theme.sub());
            if (bars.b) {
                int mx = Math.max(1, maxDur.getOrDefault(names.get(i), durs.get(i)));
                float f = Anim.clamp(durs.get(i) / (float) mx, 0f, 1f);
                int bw = w - pad * 2;
                c.g.fill(px + pad, y + 10, px + pad + bw, y + 11, 0x30FFFFFF);
                c.g.fill(px + pad, y + 10, px + pad + Math.max(1, (int) (bw * f)), y + 11, mc);
            }
        }
        if (maxDur.size() > 40) maxDur.clear();
    }
}

/** Компас-лента сверху экрана. */
final class CompassModule extends VModule {
    private static final String[] N = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};

    private final Setting width, scale, fade, degrees, yOff, opacity, colorS;

    public CompassModule() {
        super("compass", "Компас", "Лента направлений сверху", C_HUD, true);
        width = num("width", "Ширина", "Длина ленты", 200, 120, 360, 20, " px");
        scale = num("scale", "Масштаб", "Пикселей на градус", 2, 1, 4, 1, "x");
        fade = bool("fade", "Затухание по краям", "Плавно гасит края ленты", true);
        degrees = bool("degrees", "Градусы по центру", "Числовой курс", true);
        colorS = color("color", "Цвет метки", 0);
        yOff = num("y", "Отступ сверху", "Под островом", 34, 0, 100, 2, " px");
        opacity = opacity(45);
    }

    @Override
    public void render(Ctx c) {
        int w = width.i, h = 18, x = (c.sw - w) / 2, y = yOff.i;
        float yaw = c.mc.player.getYRot();
        float hd = (((yaw + 180f) % 360f) + 360f) % 360f;
        Draw.panel(c.g, x, y, w, h, opacity.i);

        float ppd = scale.i * 0.8f + 0.4f;
        float range = w / ppd;
        int cx = x + w / 2;
        int start = (int) (Math.floor((hd - range / 2) / 15f) * 15);
        c.g.enableScissor(x + 3, y, x + w - 3, y + h);
        for (int a = start; a <= hd + range / 2; a += 15) {
            float dx = (a - hd) * ppd;
            int px0 = Math.round(cx + dx);
            float k = fade.b ? 1f - Anim.clamp(Math.abs(dx) / (w / 2f), 0f, 1f) : 1f;
            k = Math.max(0.12f, k);
            int am = ((a % 360) + 360) % 360;
            if (am % 45 == 0) {
                String s = N[am / 45];
                int col = am % 90 == 0 ? col(colorS) : 0xFFFFFFFF;
                Draw.textCenter(c.g, c.f, s, px0, y + 5, Draw.mulA(col, k));
            } else {
                c.g.fill(px0, y + 6, px0 + 1, y + 12, Draw.mulA(0xFFAAAAAA, k * 0.8f));
            }
        }
        c.g.disableScissor();
        int acc = col(colorS);
        c.g.fill(cx - 1, y + h - 3, cx + 2, y + h, acc);
        c.g.fill(cx, y + h - 4, cx + 1, y + h - 3, acc);
        if (degrees.b) Draw.textCenter(c.g, c.f, Math.round(hd) + "°", cx, y + h + 3, Theme.sub());
    }
}

/** Карточка цели: имя и здоровье существа, на которое вы смотрите. */
final class TargetHudModule extends VModule {
    private final Setting width, yOff, showName, numbers, smooth, fadeT, colorMode, opacity, colorS;
    private Entity last;
    private long seenAt;
    private float hpA;

    public TargetHudModule() {
        super("target", "Карточка цели", "Имя и HP того, на кого смотрите", C_HUD, true);
        width = num("width", "Ширина", "Размер карточки", 140, 100, 220, 10, " px");
        yOff = num("y", "Смещение вниз от центра", "Положение под прицелом", 36, 10, 140, 2, " px");
        showName = bool("name", "Имя", "Название существа", true);
        numbers = bool("numbers", "Числа HP", "Текущее и максимальное", true);
        smooth = bool("smooth", "Плавная полоска", "Анимация изменения HP", true);
        fadeT = num("fade", "Время показа", "После потери цели", 2, 1, 6, 1, " с");
        colorMode = choice("color_mode", "Цвет полоски", "Как красить HP", 0, "По уровню", "Акцент");
        colorS = color("color", "Цвет акцента", 0);
        opacity = opacity(65);
    }

    @Override
    public void render(Ctx c) {
        Entity t = Stats.target;
        if (t instanceof LivingEntity) {
            if (t != last) hpA = -1f;
            last = t;
            seenAt = c.now;
        }
        if (!(last instanceof LivingEntity le) || last.isRemoved()) return;
        long age = c.now - seenAt;
        long life = fadeT.i * 1000L;
        if (age > life + 300) return;
        float a = age > life ? 1f - (age - life) / 300f : 1f;

        float max = Math.max(1f, Refl.f(Refl.call(le, "getMaxHealth"), 20f));
        float hp = Anim.clamp(le.getHealth() / max, 0f, 1f);
        hpA = hpA < 0 || !smooth.b ? hp : Anim.approach(hpA, hp, c.dt, 9f);

        int w = width.i, h = (showName.b ? 14 : 4) + 14;
        int x = (c.sw - w) / 2, y = c.sh / 2 + yOff.i;
        Draw.rect(c.g, x, y, w, h, Theme.radius(), Draw.mulA(Theme.panelBg(opacity.i), a));
        int acc = col(colorS), acc2 = col2(colorS);
        int ty = y + 5;
        if (showName.b) {
            Draw.text(c.g, c.f, Draw.fit(c.f, le.getName().getString(), w - 16), x + 8, ty, Draw.mulA(0xFFFFFFFF, a));
            ty += 11;
        }
        int bw = w - 16;
        c.g.fill(x + 8, ty, x + 8 + bw, ty + 4, Draw.mulA(0x50FFFFFF, a));
        int fw = Math.max(1, (int) (bw * hpA));
        int c1 = colorMode.i == 0 ? Draw.mix(0xFFFF4D4D, 0xFF3DDC84, hpA) : acc;
        int c2 = colorMode.i == 0 ? Draw.mix(0xFFFF8A6D, 0xFF8DF0B0, hpA) : acc2;
        Draw.gradH(c.g, x + 8, ty, fw, 4, Draw.mulA(c1, a), Draw.mulA(c2, a));
        if (numbers.b) {
            String s = Math.round(le.getHealth()) + "/" + Math.round(max);
            Draw.textRight(c.g, c.f, s, x + w - 8, y + 5, Draw.mulA(Theme.sub(), a));
        }
    }
}

/** Стопка всплывающих уведомлений в углу. */
final class NotificationsModule extends VModule {
    private final Setting maxShown, width, anim, bar, corner, ox, oy, opacity, colorS;

    public NotificationsModule() {
        super("toasts", "Уведомления", "Всплывающие карточки событий", C_STYLE, false);
        maxShown = num("max", "Максимум на экране", "Сколько карточек одновременно", 4, 1, 6, 1, "");
        width = num("width", "Ширина", "Размер карточки", 170, 120, 260, 10, " px");
        anim = choice("anim", "Появление", "Стиль анимации", 0, "Сдвиг", "Проявление", "Без анимации");
        bar = bool("bar", "Цветная полоса", "Слева на карточке", true);
        colorS = color("color", "Цвет акцента", 0);
        corner = corner("corner", 3);
        ox = num("ox", "Отступ по X", "От края экрана", 6, 0, 80, 2, " px");
        oy = num("oy", "Отступ по Y", "От края экрана", 130, 0, 240, 2, " px");
        opacity = opacity(75);
    }

    @Override
    public void render(Ctx c) {
        int n = 0, h = 26, gp = 4;
        boolean bottom = corner.i >= 2;
        boolean right = corner.i == 1 || corner.i == 3;
        for (int i = Notify.LIST.size() - 1; i >= 0 && n < maxShown.i; i--) {
            Notify.Toast t = Notify.LIST.get(i);
            long age = c.now - t.t0;
            if (age > t.ttl + 400) continue;
            float in = Anim.easeOut(age / 280f);
            float out = age > t.ttl ? 1f - (age - t.ttl) / 400f : 1f;
            float a = anim.i == 2 ? 1f : in * out;
            int w = width.i;
            int slide = anim.i == 0 ? (int) ((1f - in) * 40) * (right ? 1 : -1) : 0;
            int x = (right ? c.sw - w - ox.i : ox.i) + slide;
            int off = n * (h + gp);
            int y = bottom ? c.sh - oy.i - h - off : oy.i + off;

            Draw.rect(c.g, x, y, w, h, Theme.radius(), Draw.mulA(Theme.panelBg(opacity.i), a));
            int tc = t.rgb == 0 ? col(colorS) : 0xFF000000 | t.rgb;
            if (bar.b) c.g.fill(x + 4, y + 5, x + 6, y + h - 5, Draw.mulA(tc, a));
            Draw.text(c.g, c.f, Draw.fit(c.f, t.title, w - 20), x + 12, y + 4, Draw.mulA(tc, a));
            Draw.text(c.g, c.f, Draw.fit(c.f, t.text, w - 20), x + 12, y + 14, Draw.mulA(0xFFFFFFFF, a));
            n++;
        }
    }
}
