package com.fullmetalex.propwashvr.mixin.vivecraft;

import com.fullmetalex.propwashvr.client.VrRadio;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.vivecraft.client_vr.gameplay.trackers.TeleportTracker;

/**
 * The left trigger is a radio switch while the VR controllers fly; it must not teleport the pilot's body.
 */
@Mixin(TeleportTracker.class)
public abstract class TeleportTrackerMixin {
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void propwashvr$noTeleportWhileFlying(LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (VrRadio.flying()) {
            cir.setReturnValue(false);
        }
    }
}
