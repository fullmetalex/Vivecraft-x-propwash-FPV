package com.fullmetalex.propwashvr.mixin.propwash;

import de.lardtom.propwash.drone.client.LosView;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.VRState;

/**
 * Line of sight in VR: the pilot follows the drone with their own head, so Propwash must not turn the view.
 */
@Mixin(LosView.class)
public abstract class LosViewMixin {
    @Inject(method = "onFrame", at = @At("HEAD"), cancellable = true)
    private static void propwashvr$headFollows(Minecraft minecraft, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (VRState.VR_RUNNING) {
            ci.cancel();
        }
    }

    @Inject(method = "snapTo", at = @At("HEAD"), cancellable = true)
    private static void propwashvr$noSnap(LocalPlayer player, double x, double y, double z, CallbackInfo ci) {
        if (VRState.VR_RUNNING) {
            ci.cancel();
        }
    }
}
