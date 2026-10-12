package com.fullmetalex.propwashvr.mixin.vivecraft;

import com.fullmetalex.propwashvr.client.VrFpv;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.client_vr.VRData;

/**
 * Every eye position and rotation Vivecraft renders with comes from here; during FPV the eyes move to the drone camera.
 */
@Mixin(VRData.class)
public abstract class VRDataMixin {
    @ModifyReturnValue(method = "getEye", at = @At("RETURN"))
    private VRData.VRDevicePose propwashvr$droneEye(VRData.VRDevicePose original, @Local(argsOnly = true) RenderPass pass) {
        return VrFpv.eye((VRData) (Object) this, pass, original);
    }
}
