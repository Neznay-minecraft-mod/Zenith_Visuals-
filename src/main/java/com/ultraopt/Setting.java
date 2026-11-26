package com.ultraopt;

import java.util.Locale;

public final class Setting {
    public enum Type { BOOL, INT, CHOICE, COLOR }

    public final Type type;
    public final int tab;
    public final String id, name, desc, suffix;
    public final int min, max, step, def;
    public final String[] choices;
    public boolean b;
    public int i;

    Setting(Type type, int tab, String id, String name, String desc, int def,
            int min, int max, int step, String suffix, String[] choices) {
        this.type = type;
        this.tab = tab;
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.def = def;
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.choices = choices;
        setDefault();
    }

    private void setDefault() {
        if (type == Type.BOOL) b = def != 0;
        else i = def;
    }

    public void next() {
        switch (type) {
            case BOOL -> b = !b;
            case INT -> {
                i += step;
                if (i > max) i = min;
            }
            default -> i = (i + 1) % choices.length;
        }
        Cfg.version++;
    }

    public void prev() {
        switch (type) {
            case BOOL -> b = !b;
            case INT -> {
                i -= step;
                if (i < min) i = max;
            }
            default -> i = (i + choices.length - 1) % choices.length;
        }
        Cfg.version++;
    }

    public void reset() {
        setDefault();
        Cfg.version++;
    }

    public float frac() {
        return max == min ? 0f : (i - min) / (float) (max - min);
    }

    public void setFrac(float f) {
        f = Anim.clamp(f, 0f, 1f);
        int raw = min + Math.round(f * (max - min));
        int snapped = min + Math.round((raw - min) / (float) step) * step;
        i = Anim.clamp(snapped, min, max);
        Cfg.version++;
    }

    public String text() {
        switch (type) {
            case BOOL:
                return b ? "ВКЛ" : "ВЫКЛ";
            case INT:
                return i + suffix;
            default:
                return choices[Anim.clamp(i, 0, choices.length - 1)];
        }
    }

    public String serialize() {
        return type == Type.BOOL ? Boolean.toString(b) : Integer.toString(i);
    }

    public void deserialize(String v) {
        try {
            if (type == Type.BOOL) {
                b = Boolean.parseBoolean(v.trim());
            } else {
                int n = Integer.parseInt(v.trim());
                if (type == Type.INT) n = Math.max(min, Math.min(max, n));
                else n = Math.max(0, Math.min(choices.length - 1, n));
                i = n;
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String toString() {
        return id + "=" + String.format(Locale.ROOT, "%s", serialize());
    }
}
