package com.ultraopt;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Мелкие помощники для плавных анимаций. */
final class Anim {
    private Anim() {}

    /** Экспоненциальное приближение cur к target. speed ~ 4..20. */
    public static float approach(float cur, float target, float dt, float speed) {
        if (!Theme.anims()) return target;
        float k = 1f - (float) Math.exp(-dt * speed);
        float v = cur + (target - cur) * k;
        return Math.abs(v - target) < 0.002f ? target : v;
    }

    public static float clamp(float v, float a, float b) {
        return v < a ? a : Math.min(v, b);
    }

    public static int clamp(int v, int a, int b) {
        return v < a ? a : Math.min(v, b);
    }

    public static float easeOut(float t) {
        t = clamp(t, 0f, 1f);
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    public static float pulse(long now, float perSecond) {
        return 0.5f + 0.5f * (float) Math.sin(now / 1000.0 * perSecond * Math.PI * 2);
    }
}

/** Контекст кадра: передаётся всем модулям при отрисовке. */
final class Ctx {
    public GuiGraphicsExtractor g;
    public Minecraft mc;
    public Font f;
    public int sw, sh;
    public long now;
    public float dt;
}

/** Безопасные вызовы через reflection: если метод в версии называется иначе, мод не падает, а просто пропускает эффект. */
final class Refl {
    private static final Map<String, Method> CACHE = new HashMap<>();
    private static final Set<String> MISSING = new HashSet<>();
    private static final Map<String, Field> FIELDS = new HashMap<>();

    private Refl() {}

    private static Class<?> wrap(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        if (c == boolean.class) return Boolean.class;
        if (c == short.class) return Short.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        return c;
    }

    private static Method find(Class<?> c, String name, Object[] args) {
        for (Method m : c.getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != args.length) continue;
            Class<?>[] pt = m.getParameterTypes();
            boolean ok = true;
            for (int i = 0; i < pt.length; i++) {
                if (args[i] == null) continue;
                if (!wrap(pt[i]).isInstance(args[i])) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                try {
                    m.setAccessible(true);
                } catch (Throwable ignored) {
                }
                return m;
            }
        }
        return null;
    }

    public static Object call(Object o, String name, Object... args) {
        if (o == null) return null;
        try {
            Class<?> c = o.getClass();
            String key = c.getName() + "#" + name + "#" + args.length;
            if (MISSING.contains(key)) return null;
            Method m = CACHE.get(key);
            if (m == null) {
                m = find(c, name, args);
                if (m == null) {
                    MISSING.add(key);
                    return null;
                }
                CACHE.put(key, m);
            }
            return m.invoke(o, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object callStatic(Class<?> c, String name, Object... args) {
        try {
            String key = "S:" + c.getName() + "#" + name + "#" + args.length;
            if (MISSING.contains(key)) return null;
            Method m = CACHE.get(key);
            if (m == null) {
                m = find(c, name, args);
                if (m == null) {
                    MISSING.add(key);
                    return null;
                }
                CACHE.put(key, m);
            }
            return m.invoke(null, args);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object field(Object o, String name) {
        if (o == null) return null;
        try {
            Class<?> c = o.getClass();
            String key = c.getName() + "." + name;
            Field f = FIELDS.get(key);
            if (f == null) {
                for (Class<?> k = c; k != null && f == null; k = k.getSuperclass()) {
                    try {
                        f = k.getDeclaredField(name);
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                if (f == null) return null;
                f.setAccessible(true);
                FIELDS.put(key, f);
            }
            return f.get(o);
        } catch (Throwable t) {
            return null;
        }
    }

    public static float f(Object o, float def) {
        return o instanceof Number n ? n.floatValue() : def;
    }

    public static int i(Object o, int def) {
        return o instanceof Number n ? n.intValue() : def;
    }

    public static String s(Object o, String def) {
        return o == null ? def : o.toString();
    }
}

/** Опрос мыши через GLFW (reflection), чтобы меню не зависело от сигнатур методов экрана. */
final class Input {
    private static Method getButton, getContext;
    private static boolean tried;

    private Input() {}

    private static void init() {
        if (tried) return;
        tried = true;
        try {
            Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
            getButton = glfw.getMethod("glfwGetMouseButton", long.class, int.class);
            getContext = glfw.getMethod("glfwGetCurrentContext");
        } catch (Throwable ignored) {
        }
    }

    private static long handle(Minecraft mc) {
        try {
            Object w = mc.getWindow();
            Object h = Refl.call(w, "handle");
            if (!(h instanceof Long)) h = Refl.call(w, "getWindow");
            if (h instanceof Long l && l != 0L) return l;
        } catch (Throwable ignored) {
        }
        try {
            if (getContext != null) return (Long) getContext.invoke(null);
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    public static boolean leftDown(Minecraft mc) {
        init();
        if (getButton == null) return false;
        try {
            long h = handle(mc);
            if (h == 0L) return false;
            return ((Integer) getButton.invoke(null, h, 0)) == 1;
        } catch (Throwable t) {
            return false;
        }
    }
}

/** Общая очередь уведомлений: их показывают остров и стопка уведомлений. */
final class Notify {
    public static final class Toast {
        public final String title, text;
        public final int rgb;
        public final long t0, ttl;
        public final int seq;

        Toast(String title, String text, int rgb, long ttl, int seq) {
            this.title = title;
            this.text = text;
            this.rgb = rgb & 0xFFFFFF;
            this.t0 = System.currentTimeMillis();
            this.ttl = ttl;
            this.seq = seq;
        }
    }

    public static final List<Toast> LIST = new ArrayList<>();
    public static Toast last;
    private static int seq = 0;

    private Notify() {}

    public static void push(String title, String text, int rgb) {
        push(title, text, rgb, 3500);
    }

    public static void push(String title, String text, int rgb, long ttl) {
        Toast t = new Toast(title, text, rgb, ttl, ++seq);
        last = t;
        LIST.add(t);
        while (LIST.size() > 8) LIST.remove(0);
    }

    public static void prune(long now) {
        LIST.removeIf(t -> now - t.t0 > t.ttl + 600);
    }
}
