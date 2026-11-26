package com.ultraopt;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltraOptClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("ZenithVisuals");

    private static KeyMapping openKey;
    private static KeyMapping islandKey;
    private static KeyMapping hideKey;

    @Override
    public void onInitializeClient() {
        Cfg.load();
        ModuleManager.init();

        InputConstants.Type type;
        try {
            type = InputConstants.Type.valueOf("KEYSYM");
        } catch (IllegalArgumentException e) {
            type = InputConstants.Type.valueOf("KEYBOARD");
        }
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ultraopt", "main"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ultraopt.settings", type, 344, cat));  // Right Shift
        islandKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ultraopt.island", type, 73, cat));   // I
        hideKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.ultraopt.hide", type, 301, cat));      // F12

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("ultraopt", "hud"), ModuleManager::render);
        HudHide.install();

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) IslandModule.chat(message.getString());
        });
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, time) ->
                IslandModule.chat(message.getString()));

        LOG.info("Zenith Visuals loaded: {} modules", ModuleManager.ALL.size());
    }

    private void onTick(Minecraft mc) {
        while (openKey.consumeClick()) {
            mc.gui.setScreen(new SettingsScreen(mc.gui.screen()));
        }
        while (islandKey.consumeClick()) {
            IslandModule.toggleExpand();
        }
        while (hideKey.consumeClick()) {
            ModuleManager.toggleHidden();
        }
        if (mc.player == null || mc.level == null) {
            CullLogic.clear();
        }
        ModuleManager.tick(mc);
    }
}

/**
 * Прячет ванильные элементы HUD, если вместо них включён наш модуль.
 * Всё через reflection: если Fabric API называет что-то иначе, мод просто оставит ваниль.
 */
final class HudHide {
    private HudHide() {}

    public static void install() {
        hide("crosshair", () -> {
            VModule m = ModuleManager.get("crosshair");
            return !ModuleManager.hidden && m instanceof CrosshairPlusModule c && c.hidesVanilla();
        });
        hide("mob_effects", () -> {
            VModule m = ModuleManager.get("potions");
            return !ModuleManager.hidden && m instanceof PotionHudModule p && p.hidesVanilla();
        });
        for (String id : new String[]{"health_bar", "armor_bar", "food_bar"}) {
            hide(id, () -> {
                VModule m = ModuleManager.get("bars");
                return !ModuleManager.hidden && m instanceof StatusBarsModule s && s.hidesVanilla();
            });
        }
    }

    private static void hide(String path, BooleanSupplier hideNow) {
        try {
            Class<?> registry = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry");
            Class<?> element = Class.forName("net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement");
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
            Function<Object, Object> wrap = orig -> Proxy.newProxyInstance(element.getClassLoader(), new Class<?>[]{element},
                    (InvocationHandler) (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) return method.invoke(orig, args);
                        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
                        if (hideNow.getAsBoolean()) return null;
                        return method.invoke(orig, args);
                    });
            for (Method m : registry.getMethods()) {
                if (m.getName().equals("replaceElement") && m.getParameterCount() == 2) {
                    m.invoke(null, id, wrap);
                    return;
                }
            }
        } catch (Throwable t) {
            UltraOptClient.LOG.info("Cannot hook vanilla element {}: {}", path, t.toString());
        }
    }
}
