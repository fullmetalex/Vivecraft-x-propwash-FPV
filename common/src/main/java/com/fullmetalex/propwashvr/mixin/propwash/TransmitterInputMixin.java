package com.fullmetalex.propwashvr.mixin.propwash;

import com.fullmetalex.propwashvr.client.VrFpv;
import com.fullmetalex.propwashvr.client.VrKeys;
import com.fullmetalex.propwashvr.client.VrRadio;
import de.lardtom.propwash.input.client.TransmitterInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Propwash reads its radio once per frame here; the VR controllers are fed in just before.
 */
@Mixin(TransmitterInput.class)
public abstract class TransmitterInputMixin {
    @Inject(method = "onFrame", at = @At("HEAD"))
    private void propwashvr$beforeRadio(CallbackInfo ci) {
        VrFpv.nextFrame();
        VrKeys.poll();
        VrRadio.onFrame();
    }
}
