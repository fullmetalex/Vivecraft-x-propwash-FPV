package com.fullmetalex.propwashvr.client;

import com.fullmetalex.propwashvr.config.VrConfig;
import com.fullmetalex.propwashvr.math.GogglesMath;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.client.extensions.LevelRenderStateExtension;
import org.vivecraft.client_vr.gameplay.screenhandlers.GuiHandler;
import org.vivecraft.client_vr.render.helpers.RenderHelper;
import org.vivecraft.client_vr.render.renderstates.VRRenderState;
import org.vivecraft.client_vr.render.rendertypes.VRRenderTypes;

/**
 * What the headset shows on top of the drone camera, in place of Vivecraft's own overlays (hands, crosshair, the
 * floating HUD panel and so on, which belong to the pilot's body far away from the drone):
 * <ul>
 *     <li>goggles mode: a black goggles frame around the camera picture and the HUD with Propwash's OSD laid exactly
 *     over the picture,</li>
 *     <li>immersive mode: the HUD as a head-locked panel in front of the eyes.</li>
 * </ul>
 * Everything is submitted through Vivecraft's late, depth-ignoring geometry so it stays on top of the world.
 */
public final class GogglesOverlay {
    /** Distance of the virtual goggles screen in front of the lens, in blocks (beyond Vivecraft's near plane). */
    private static final float SCREEN_DISTANCE = 0.1F;
    /** The frame reaches this many screen half widths out, enough to cover any headset's field of view. */
    private static final float FRAME_EXTENT = 1000.0F;
    /** Distance of the head-locked HUD panel in immersive mode, in blocks. */
    private static final float HUD_DISTANCE = 1.0F;
    private static final float[] WHITE = {1.0F, 1.0F, 1.0F, 1.0F};

    private GogglesOverlay() {
    }

    /**
     * @return whether Vivecraft's own overlays are replaced in the pass being rendered
     */
    public static boolean replacesVrOverlays(LevelRenderState levelState) {
        RenderPass pass = vrState(levelState).currentPass;
        return (pass == RenderPass.LEFT || pass == RenderPass.RIGHT || pass == RenderPass.CENTER) && VrFpv.active();
    }

    /**
     * Vivecraft draws its overlays after the world (second call) instead of with it when shaders want the GUI last.
     */
    public static boolean drawsInSecondPass(LevelRenderState levelState) {
        return vrState(levelState).uiAfterWorld;
    }

    public static void submit(SubmitNodeCollector output, LevelRenderState levelState, PoseStack poseStack) {
        VRRenderState vrState = vrState(levelState);
        VrConfig config = VrConfig.get();
        Vec3 cameraPos = levelState.cameraRenderState.pos;
        Vector3dc lens = VrFpv.lens();
        boolean hud = config.osd && vrState.renderGui && GuiHandler.GUI_FRAMEBUFFER != null;
        int order = 0;

        poseStack.pushPose();
        poseStack.translate(lens.x() - cameraPos.x, lens.y() - cameraPos.y, lens.z() - cameraPos.z);
        if (VrFpv.goggles()) {
            poseStack.mulPose(new Matrix4f().rotation(VrFpv.camera()));
            float halfWidth = SCREEN_DISTANCE * GogglesMath.halfWidthAtUnit(VrFpv.cameraFov());
            float halfHeight = halfWidth / VrFpv.imageAspect();
            if (config.gogglesFrame) {
                order = frame(output, poseStack, halfWidth, halfHeight, order);
            }
            if (hud) {
                // the HUD is as tall as the picture; with a 4:3 picture Propwash's own side bars land on the frame
                order = hud(output, poseStack, SCREEN_DISTANCE, 2.0F * halfHeight, order);
            }
        } else if (hud) {
            poseStack.mulPose(new Matrix4f().rotation(VrFpv.headView()));
            float width = 2.0F * HUD_DISTANCE * (float) Math.tan(Math.toRadians(config.screenSizeDegrees) * 0.5);
            RenderTarget gui = GuiHandler.GUI_FRAMEBUFFER;
            order = hud(output, poseStack, HUD_DISTANCE, width * gui.height / (float) gui.width, order);
        }
        poseStack.popPose();
    }

    private static int hud(SubmitNodeCollector output, PoseStack poseStack, float distance, float height, int order) {
        RenderTarget gui = GuiHandler.GUI_FRAMEBUFFER;
        float width = height * gui.width / (float) gui.height;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -distance);
        order = RenderHelper.drawSizedQuad(gui.width, gui.height, width, WHITE, poseStack, gui, true, output, order);
        poseStack.popPose();
        return order;
    }

    private static int frame(SubmitNodeCollector output, PoseStack poseStack, float halfWidth, float halfHeight, int order) {
        float z = -SCREEN_DISTANCE;
        float outer = halfWidth * FRAME_EXTENT;
        RenderHelper.submitLateCustomGeometry(output.order(order++), poseStack, VRRenderTypes.quads(true),
                (pose, consumer) -> {
                    quad(consumer, pose, -outer, halfHeight, outer, outer, z);
                    quad(consumer, pose, -outer, -outer, outer, -halfHeight, z);
                    quad(consumer, pose, -outer, -halfHeight, -halfWidth, halfHeight, z);
                    quad(consumer, pose, halfWidth, -halfHeight, outer, halfHeight, z);
                });
        return order;
    }

    /**
     * Black rectangle, emitted with both windings so it shows whichever way the render type culls.
     */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float z) {
        black(consumer, pose, x0, y0, z);
        black(consumer, pose, x1, y0, z);
        black(consumer, pose, x1, y1, z);
        black(consumer, pose, x0, y1, z);

        black(consumer, pose, x0, y0, z);
        black(consumer, pose, x0, y1, z);
        black(consumer, pose, x1, y1, z);
        black(consumer, pose, x1, y0, z);
    }

    private static void black(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z) {
        consumer.addVertex(pose, x, y, z).setColor(0, 0, 0, 255);
    }

    private static VRRenderState vrState(LevelRenderState levelState) {
        return ((LevelRenderStateExtension) levelState).vivecraft$getVRRenderState();
    }
}
