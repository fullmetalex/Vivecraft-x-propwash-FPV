package com.fullmetalex.propwashvr.client;

import com.fullmetalex.propwashvr.config.VrConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.sdl.SDLScancode;
import org.vivecraft.client.utils.ClientUtils;

import java.util.List;

/**
 * Key mappings of Propwash VR. They start unbound; besides the keyboard they show up in Vivecraft's "Mod" action set,
 * so they can be put on a controller button in the SteamVR binding editor.
 */
public final class VrKeys {
    private static KeyMapping viewMode;
    private static KeyMapping recenter;
    private static KeyMapping fpvInHeadset;

    private VrKeys() {
    }

    /**
     * Creates the key mappings in the category the loader made; the loader registers {@link #all()} afterwards.
     */
    public static void create(KeyMapping.Category category) {
        viewMode = new KeyMapping("key.propwash_vr.view_mode", SDLScancode.SDL_SCANCODE_UNKNOWN, category);
        recenter = new KeyMapping("key.propwash_vr.recenter", SDLScancode.SDL_SCANCODE_UNKNOWN, category);
        fpvInHeadset = new KeyMapping("key.propwash_vr.fpv_in_headset", SDLScancode.SDL_SCANCODE_UNKNOWN, category);
    }

    public static List<KeyMapping> all() {
        return viewMode == null ? List.of() : List.of(viewMode, recenter, fpvInHeadset);
    }

    /**
     * Runs once per frame.
     */
    public static void poll() {
        if (viewMode == null) {
            return;
        }
        VrConfig config = VrConfig.get();
        boolean changed = false;
        while (viewMode.consumeClick()) {
            config.viewMode = config.viewMode.next();
            changed = true;
            ClientUtils.addChatMessage(Component.translatable("propwash_vr.message.view_mode",
                    Component.translatable(config.viewMode.translationKey())));
        }
        while (recenter.consumeClick()) {
            VrFpv.requestRecenter();
            ClientUtils.addChatMessage(Component.translatable("propwash_vr.message.recentered"));
        }
        while (fpvInHeadset.consumeClick()) {
            config.fpvInHeadset = !config.fpvInHeadset;
            changed = true;
            ClientUtils.addChatMessage(Component.translatable(config.fpvInHeadset
                    ? "propwash_vr.message.fpv_on" : "propwash_vr.message.fpv_off"));
        }
        if (changed) {
            VrConfig.save();
        }
    }
}
