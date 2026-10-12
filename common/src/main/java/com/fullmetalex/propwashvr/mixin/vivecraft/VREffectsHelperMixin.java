package com.fullmetalex.propwashvr.mixin.vivecraft;

import com.fullmetalex.propwashvr.client.GogglesOverlay;
import com.fullmetalex.propwashvr.client.VrFpv;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.vivecraft.client_vr.render.helpers.VREffectsHelper;

/**
 * Vivecraft's hands, crosshair, HUD panel, teleport arc and camera widgets belong to the pilot's body; seen through the
 * drone camera they would float somewhere in the world. During FPV they make way for the goggles overlay, and the
 * pilot's body is drawn like any other player (with its head) when the drone looks at it.
 */
@Mixin(VREffectsHelper.class)
public abstract class VREffectsHelperMixin {
    @Inject(method = "renderVROIT", at = @At("HEAD"), cancellable = true)
    private static void propwashvr$gogglesOit(
            SubmitNodeStorage output, LevelRenderState levelState, PoseStack poseStack, CallbackInfo ci)
    {
        if (GogglesOverlay.replacesVrOverlays(levelState)) {
            GogglesOverlay.submit(output, levelState, poseStack);
            ci.cancel();
        }
    }

    @Inject(method = "renderVrFast", at = @At("HEAD"), cancellable = true)
    private static void propwashvr$gogglesFast(
            SubmitNodeCollector output, LevelRenderState levelState, PoseStack poseStack, boolean secondPassOnly,
            CallbackInfo ci)
    {
        if (GogglesOverlay.replacesVrOverlays(levelState)) {
            if (secondPassOnly == GogglesOverlay.drawsInSecondPass(levelState)) {
                GogglesOverlay.submit(output, levelState, poseStack);
            }
            ci.cancel();
        }
    }

    @ModifyReturnValue(method = "isFirstPersonEntityPass", at = @At("RETURN"))
    private static boolean propwashvr$pilotBodySeenFromDrone(boolean firstPerson) {
        return firstPerson && !VrFpv.active();
    }
}
