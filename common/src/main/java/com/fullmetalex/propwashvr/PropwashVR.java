package com.fullmetalex.propwashvr;

import com.fullmetalex.propwashvr.client.VrFpv;
import com.fullmetalex.propwashvr.client.VrKeys;
import com.fullmetalex.propwashvr.config.VrConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivecraft.api.client.VRClientAPI;

import java.nio.file.Path;

/**
 * Client entry point shared by both loaders. Propwash VR only runs on the client: it changes what the headset shows
 * and where the radio sticks come from, nothing on the server.
 */
public final class PropwashVR {
    public static final String MOD_ID = "propwash_vr";
    public static final Logger LOG = LoggerFactory.getLogger("Propwash VR");
    public static final Identifier KEY_CATEGORY = Identifier.fromNamespaceAndPath(MOD_ID, "main");

    private PropwashVR() {
    }

    /**
     * @param keyCategory key mapping category for {@link #KEY_CATEGORY}, created the way the loader wants it
     * @param configDir   the loader's config folder
     */
    public static void initClient(KeyMapping.Category keyCategory, Path configDir) {
        VrConfig.load(configDir.resolve(MOD_ID + ".json"));
        VrKeys.create(keyCategory);
        // Trackers have to be registered before the game loop starts.
        VRClientAPI.instance().addClientRegistrationHandler(event -> event.registerTrackers(new VrFpv.FrameTracker()));
        LOG.info("Propwash VR loaded (view mode {}, VR radio {})", VrConfig.get().viewMode, VrConfig.get().radio);
    }
}
