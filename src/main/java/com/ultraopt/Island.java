package com.ultraopt;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/** Динамический остров: покой, чат, уведомления, урон, раскрытая панель по клавише I. */
final class IslandModule extends VModule {
    public static IslandModule I;

    private static final String[] LABELS = {"Сервер", "Пинг", "FPS", "XYZ", "Курс", "Скорость", "ОЗУ", "Предмет"};

    private final Setting chatS, chatTime, damageS, notifS, idleS, styleS, glowS, hpBar, avatarS, expandS, speedS, yOff, widthS, colorS;

    private float cw, ch;
    private boolean expanded;
    private String nTitle = "", nText = "";
    private int nColor = 0xFFFF7A1A;
    private boolean nChat;
    private long nUntil, seenHurt;
    private int seenSeq;

    public IslandModule() {
        super("island", "Динамический остров", "Главный виджет сверху: FPS, чат, уведомления, урон", C_HUD, true);
        I = this;
        styleS = choice("style", "Форма", "Скругление острова", 0, "Таблетка", "Мягкая", "Строгая");
        colorS = color("color", "Цвет акцента", 0);
        glowS = bool("glow", "Свечение", "Ореол цвета акцента вокруг острова", true);
        idleS = choice("idle", "В покое показывает", "Что на острове без событий", 0,
                "FPS и пинг", "Только FPS", "Время", "Ник и HP", "Ничего");
        hpBar = bool("hpbar", "Полоска здоровья", "Тонкая линия HP внизу острова", true);
        chatS = bool("chat", "Чат в острове", "Сообщения всплывают в острове", true);
        chatTime = num("chat_time", "Время сообщения", "Сколько висит чат", 8, 2, 20, 1, " с");
        avatarS = bool("avatar", "Аватар отправителя", "Цветной значок с инициалом", true);
        damageS = bool("damage", "Урон и низкое HP", "Показывает получение урона", true);
        notifS = bool("notif", "Уведомления", "События мода и профилей", true);
        expandS = bool("expand", "Раскрытие по клавише I", "Большая панель со всей информацией", true);
        widthS = num("width", "Ширина в покое", "Размер маленького острова", 150, 80, 240, 10, " px");
        yOff = num("y", "Отступ сверху", "Положение острова", 6, 0, 60, 2, " px");
        speedS = num("speed", "Скорость раскрытия", "Выше = быстрее", 6, 1, 12, 1, "");
    }

    public static void chat(String raw) {
        if (I != null) I.onChat(raw);
    }

    public static void toggleExpand() {
        if (I != null && I.enabled && I.expandS.b && !ModuleManager.hidden) I.expanded = !I.expanded;
    }

    private void onChat(String raw) {
        if (!enabled || !chatS.b || raw == null) return;
        String s = raw.trim();
        if (s.isEmpty()) return;
        String name = "Сервер", text = s;
        if (s.startsWith("<")) {
            int e = s.indexOf('>');
            if (e > 1 && e < 28) {
                name = s.substring(1, e);
                text = s.substring(e + 1).trim();
            }
        } else {
            int lim = Math.min(s.length(), 28);
            for (int i = 1; i < lim; i++) {
                char c = s.charAt(i);
                if (c == ':' || c == '»' || c == '>') {
                    name = s.substring(0, i).trim();
                    text = s.substring(i + 1).trim();
                    break;
                }
            }
        }
        if (text.isEmpty()) return;
        if (name.isEmpty()) name = "Сервер";
        nTitle = name;
        nText = text;
        nChat = true;
        nUntil = System.currentTimeMillis() + chatTime.i * 1000L;
    }

    private void event(String title, String text, int rgb, long ms) {
        nTitle = title;
        nText = text;
        nColor = 0xFF000000 | rgb;
        nChat = false;
        nUntil = System.currentTimeMillis() + ms;
    }

    private static String fmt(float v) {
        return v == (int) v ? Integer.toString((int) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    @Override
    public void tick(Minecraft mc) {
        if (damageS.b && Stats.hurtAt != seenHurt) {
            seenHurt = Stats.hurtAt;
            boolean low = Stats.hp <= 6f;
            event(low ? "Мало здоровья" : "Урон", "-" + fmt(Stats.hurtAmt) + " HP, осталось " + fmt(Stats.hp),
                    low ? 0xFF4D4D : 0xFFB84D, 2500);
        }
    }

    private String idleText() {
        switch (idleS.i) {
            case 0:
                return "FPS " + Stats.fpsS + (Stats.ping >= 0 ? "  ·  " + Stats.pingS + " мс" : "");
            case 1:
                return "FPS " + Stats.fpsS;
            case 2:
                return Stats.clock;
            case 3:
                return Stats.nick + "  ·  HP " + (int) Math.ceil(Stats.hp);
            default:
                return "";
        }
    }

    @Override
    public void render(Ctx c) {
        Font f = c.f;
        if (notifS.b && Notify.last != null && Notify.last.seq != seenSeq) {
            seenSeq = Notify.last.seq;
            Notify.Toast t = Notify.last;
            event(t.title, t.text, t.rgb, t.ttl);
        }

        boolean showInfo = expanded && expandS.b;
        boolean notif = c.now < nUntil;
        String idle = idleText();
        int mode, tw, th;
        if (showInfo) {
            mode = 3;
            tw = 230;
            th = 28 + LABELS.length * 11 + 14;
        } else if (notif && nChat) {
            mode = 1;
            int inner = Math.max(f.width(nTitle), f.width(nText));
            tw = Anim.clamp(inner + (avatarS.b ? 34 : 16) + 12, 150, 300);
            th = 34;
        } else if (notif) {
            mode = 2;
            tw = Anim.clamp(f.width(nTitle) + f.width(nText) + 44, 120, 300);
            th = 24;
        } else if (idleS.i == 4 || idle.isEmpty()) {
            mode = 0;
            tw = 0;
            th = 0;
        } else {
            mode = 0;
            tw = Math.max(widthS.i, f.width(idle) + 36);
            th = hpBar.b ? 22 : 20;
        }

        float sp = speedS.i * 1.5f;
        cw = Anim.approach(cw, tw, c.dt, sp);
        ch = Anim.approach(ch, th, c.dt, sp);
        int w = Math.round(cw), h = Math.round(ch);
        if (w < 4 || h < 4) return;

        int x = (c.sw - w) / 2, y = yOff.i;
        int r = styleS.i == 0 ? Math.min(h / 2, 14) : (styleS.i == 1 ? Math.min(h / 2, 8) : 3);
        int acc = col(colorS), acc2 = col2(colorS);

        if (glowS.b) Draw.glow(c.g, x, y, w, h, r, acc, 3, 0x60);
        Draw.rect(c.g, x, y, w, h, r, 0xF2080A0E);
        if (Theme.glass()) c.g.fill(x + r, y, x + w - r, y + 1, 0x30FFFFFF);
        if (Theme.gradient()) Draw.gradH(c.g, x + r, y + h - 1, Math.max(0, w - 2 * r), 1, Draw.mulA(acc, 0.7f), Draw.mulA(acc2, 0.7f));
        else c.g.fill(x + r, y + h - 1, x + w - r, y + h, Draw.mulA(acc, 0.6f));

        c.g.enableScissor(x + 2, y, x + w - 2, y + h);
        switch (mode) {
            case 0 -> {
                int gw = 12 + f.width(idle);
                int gx = x + (w - gw) / 2;
                int ty = hpBar.b ? y + 4 : y + (h - 8) / 2;
                Draw.disc(c.g, gx + 3, ty + 4, 3, acc);
                Draw.text(c.g, f, idle, gx + 12, ty, 0xFFFFFFFF);
                if (hpBar.b && h >= 20) {
                    int bw = w - 2 * r - 6;
                    float fr = Anim.clamp(Stats.hp / Stats.hpMax, 0f, 1f);
                    c.g.fill(x + r + 3, y + h - 5, x + r + 3 + bw, y + h - 3, 0x30FFFFFF);
                    Draw.gradH(c.g, x + r + 3, y + h - 5, Math.max(1, (int) (bw * fr)), 2, Draw.mix(0xFFFF4D4D, acc, fr), acc2);
                }
            }
            case 1 -> {
                int tx = x + 10;
                if (avatarS.b) {
                    int ay = y + (h - 20) / 2;
                    int col = Draw.hsv((Math.abs(nTitle.hashCode()) % 360) / 360f, 0.55f, 0.95f);
                    Draw.rect(c.g, x + 8, ay, 20, 20, 6, col);
                    String initial = nTitle.isEmpty() ? "?" : nTitle.substring(0, 1).toUpperCase(Locale.ROOT);
                    c.g.text(f, initial, x + 8 + (20 - f.width(initial)) / 2, ay + 6, 0xFF101114, false);
                    tx = x + 34;
                }
                int maxW = w - (tx - x) - 10;
                Draw.text(c.g, f, Draw.fit(f, nTitle, maxW), tx, y + 7, acc);
                Draw.text(c.g, f, Draw.fit(f, nText, maxW), tx, y + 19, 0xFFFFFFFF);
            }
            case 2 -> {
                int gw = 12 + f.width(nTitle) + 8 + f.width(nText);
                int gx = x + (w - gw) / 2;
                Draw.disc(c.g, gx + 3, y + h / 2, 3, nColor);
                Draw.text(c.g, f, nTitle, gx + 12, y + (h - 8) / 2, nColor);
                Draw.text(c.g, f, nText, gx + 12 + f.width(nTitle) + 8, y + (h - 8) / 2, 0xFFFFFFFF);
            }
            default -> {
                Draw.text(c.g, f, Stats.nick, x + 12, y + 8, acc);
                c.g.text(f, Stats.clock, x + w - 12 - f.width(Stats.clock), y + 8, 0xFF9AA0AA, false);
                c.g.fill(x + 12, y + 20, x + w - 12, y + 21, 0x30FFFFFF);
                String[] vals = {Stats.server, Stats.pingS + " мс", Stats.fpsS, Stats.coords, Stats.dir, Stats.speedS, Stats.mem, Stats.held};
                for (int i = 0; i < LABELS.length; i++) {
                    int ry = y + 26 + i * 11;
                    Draw.text(c.g, f, LABELS[i], x + 12, ry, 0xFF9AA0AA);
                    Draw.text(c.g, f, Draw.fit(f, vals[i], w - 90 - 12), x + 78, ry, 0xFFFFFFFF);
                }
                int by = y + 28 + LABELS.length * 11 + 2;
                float fr = Anim.clamp(Stats.hp / Stats.hpMax, 0f, 1f);
                c.g.fill(x + 12, by, x + w - 12, by + 3, 0x30FFFFFF);
                Draw.gradH(c.g, x + 12, by, Math.max(1, (int) ((w - 24) * fr)), 3, Draw.mix(0xFFFF4D4D, acc, fr), acc2);
            }
        }
        c.g.disableScissor();
    }
}
