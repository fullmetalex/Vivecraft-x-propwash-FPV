package com.fullmetalex.propwashvr.mixin.vivecraft;

import com.fullmetalex.propwashvr.client.VrFpv;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.client_vr.provider.VRRenderer;

/**
 * In goggles mode the eye projections shrink the camera picture onto the virtual screen. The cached matrices are
 * never modified; a new one is handed out instead (callers copy it).
 */
@Mixin(VRRenderer.class)
public abstract class VRRendererMixin {
    @ModifyReturnValue(method = {"getCachedProjectionMatrix", "getCachedReverseProjectionMatrix"}, at = @At("RETURN"))
    private Matrix4f propwashvr$gogglesProjection(Matrix4f original, @Local(argsOnly = true) int eyeType) {
        return VrFpv.projection(eyeType, original);
    }
}
