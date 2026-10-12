package com.fullmetalex.propwashvr.mixin.vivecraft;

import com.fullmetalex.propwashvr.client.VrRadio;
import com.fullmetalex.propwashvr.config.VrConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.provider.MCVR;

/**
 * While the VR controllers fly the drone, the right stick must not turn the player and the grips must not switch the
 * hotbar (the in-game remote has to stay in hand).
 */
@Mixin(MCVR.class)
public abstract class MCVRMixin {
    @Shadow
    public float seatedRot;

    @Unique
    private boolean propwashvr$holding;

    @Unique
    private float propwashvr$worldRotation;

    @Inject(method = "processBindings", at = @At("HEAD"))
    private void propwashvr$holdControls(CallbackInfo ci) {
        this.propwashvr$holding = VrRadio.flying();
        if (this.propwashvr$holding) {
            VrRadio.drainVivecraftKeys();
            this.propwashvr$worldRotation = ClientDataHolderVR.getInstance().vrSettings.worldRotation;
        }
    }

    @Inject(method = "processBindings", at = @At("RETURN"))
    private void propwashvr$keepRotation(CallbackInfo ci) {
        if (this.propwashvr$holding && VrConfig.get().lockTurning) {
            ClientDataHolderVR.getInstance().vrSettings.worldRotation = this.propwashvr$worldRotation;
            this.seatedRot = this.propwashvr$worldRotation;
        }
    }
}
