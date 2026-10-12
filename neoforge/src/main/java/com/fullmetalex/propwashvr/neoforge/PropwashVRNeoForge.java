package com.fullmetalex.propwashvr.neoforge;

import com.fullmetalex.propwashvr.PropwashVR;
import com.fullmetalex.propwashvr.client.VrKeys;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = PropwashVR.MOD_ID, dist = Dist.CLIENT)
public final class PropwashVRNeoForge {
    public PropwashVRNeoForge(IEventBus modBus, ModContainer container) {
        KeyMapping.Category category = new KeyMapping.Category(PropwashVR.KEY_CATEGORY);
        PropwashVR.initClient(category, FMLPaths.CONFIGDIR.get());
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            event.registerCategory(category);
            for (KeyMapping mapping : VrKeys.all()) {
                event.register(mapping);
            }
        });
    }
}
