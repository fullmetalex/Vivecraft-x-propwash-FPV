package com.fullmetalex.propwashvr.fabric;

import com.fullmetalex.propwashvr.PropwashVR;
import com.fullmetalex.propwashvr.client.VrKeys;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

public final class PropwashVRFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(PropwashVR.KEY_CATEGORY);
        PropwashVR.initClient(category, FabricLoader.getInstance().getConfigDir());
        for (KeyMapping mapping : VrKeys.all()) {
            KeyMappingHelper.registerKeyMapping(mapping);
        }
    }
}
