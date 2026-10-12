package com.fullmetalex.propwashvr.client;

import com.fullmetalex.propwashvr.config.ViewMode;
import com.fullmetalex.propwashvr.config.VrConfig;
import com.fullmetalex.propwashvr.math.GogglesMath;
import de.lardtom.propwash.drone.client.FpvView;
import de.lardtom.propwash.video.client.FpvImage;
import de.lardtom.propwash.video.client.VideoState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.vivecraft.api.client.Tracker;
import org.vivecraft.api.client.data.RenderPass;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRData;
import org.vivecraft.client_vr.VRState;
import org.vivecraft.client_vr.gameplay.VRPlayer;
import org.vivecraft.client_vr.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.client_vr.gameplay.screenhandlers.RadialHandler;

import java.util.Arrays;

/**
 * Puts the headset's eyes at the drone camera while Propwash flies FPV.
 * <p>
 * Vivecraft renders every eye from {@link VRData#getEye(RenderPass)} of the world render data (camera position, view
 * rotation, culling, overlays and the sound listener all read it). {@code VRDataMixin} routes the LEFT, RIGHT and
 * CENTER poses of that data through {@link #eye}, which swaps in poses built from Propwash's own FPV camera pose
 * ({@link FpvView#cameraPose}, the same latency-delayed pose the flat screen shows). The head pose itself stays
 * untouched, so aiming, menus and the pilot's body keep working.
 * <ul>
 *     <li>{@link ViewMode#GOGGLES}: both eyes at the lens, looking where the camera looks; the projection is shrunk
 *     ({@link #projection}) so the whole camera picture fits a virtual screen in front of the head.</li>
 *     <li>{@link ViewMode#IMMERSIVE}: the head looks around from the lens, relative to the forward direction it had
 *     when FPV started (or when re-centred), in stereo.</li>
 * </ul>
 */
public final class VrFpv {
    private static final ClientDataHolderVR DH = ClientDataHolderVR.getInstance();
    private static final int EYE_LEFT = 0;
    private static final int EYE_RIGHT = 1;
    private static final int EYE_CENTER = 2;

    private static long frame;
    private static long preparedFrame = -1L;
    private static boolean activeFrame;
    private static boolean wasActive;
    private static boolean recenterRequested;
    private static float referenceYaw;

    private static ViewMode mode = ViewMode.GOGGLES;
    private static float zoom = 1.0F;
    private static float imageAspect = 16.0F / 9.0F;
    private static double cameraFov = 120.0;

    private static final Vector3d LENS = new Vector3d();
    private static final Quaternionf CAMERA = new Quaternionf();
    private static final Matrix4f[] EYE_IN_HEAD = {new Matrix4f(), new Matrix4f()};
    private static final Quaternionf HEAD_VIEW = new Quaternionf();

    private static VRData.VRDevicePose[] poses = new VRData.VRDevicePose[3];
    private static VRData posesFor;

    private VrFpv() {
    }

    /**
     * Starts a new frame: the camera pose and the eye poses are worked out again on first use.
     */
    public static void nextFrame() {
        frame++;
    }

    public static void requestRecenter() {
        recenterRequested = true;
    }

    /**
     * @return whether the headset shows the drone camera this frame
     */
    public static boolean active() {
        prepare();
        return activeFrame;
    }

    public static boolean goggles() {
        return active() && mode == ViewMode.GOGGLES;
    }

    public static ViewMode mode() {
        return mode;
    }

    /**
     * World position of the camera lens this frame.
     */
    public static Vector3dc lens() {
        return LENS;
    }

    /**
     * World orientation of the drone camera this frame (looks along its local -Z).
     */
    public static Quaternionfc camera() {
        return CAMERA;
    }

    /**
     * World orientation of the head as seen from the drone in immersive mode (camera, then the head relative to its
     * reference yaw).
     */
    public static Quaternionfc headView() {
        return HEAD_VIEW;
    }

    /**
     * Width / height of the FPV picture (16:9 or 4:3, Propwash's image format).
     */
    public static float imageAspect() {
        return imageAspect;
    }

    /**
     * Horizontal field of view of the drone camera in degrees.
     */
    public static double cameraFov() {
        return cameraFov;
    }

    /**
     * Called for every {@link VRData#getEye} result.
     */
    public static VRData.VRDevicePose eye(VRData data, RenderPass pass, VRData.VRDevicePose original) {
        int index = switch (pass) {
            case LEFT -> EYE_LEFT;
            case RIGHT -> EYE_RIGHT;
            case CENTER -> EYE_CENTER;
            default -> -1;
        };
        if (index < 0) {
            return original;
        }
        VRPlayer player = DH.vrPlayer;
        if (player == null || data != player.vrdata_world_render || !active()) {
            return original;
        }
        if (posesFor != data) {
            posesFor = data;
            Arrays.fill(poses, null);
        }
        VRData.VRDevicePose pose = poses[index];
        if (pose == null) {
            pose = build(data, index);
            poses[index] = pose;
        }
        return pose;
    }

    /**
     * Called for every eye projection Vivecraft hands out. In goggles mode the current eye pass gets the shrunk
     * projection; everything else keeps the headset's own.
     */
    public static Matrix4f projection(int eyeType, Matrix4f original) {
        if (eyeType != EYE_LEFT && eyeType != EYE_RIGHT) {
            return original;
        }
        RenderPass pass = DH.currentPass;
        if (pass != (eyeType == EYE_LEFT ? RenderPass.LEFT : RenderPass.RIGHT) || !goggles()) {
            return original;
        }
        return GogglesMath.gogglesProjection(original, EYE_IN_HEAD[eyeType], zoom, new Matrix4f());
    }

    /**
     * Whether the headset may show the drone camera now; menus, the keyboard and the radial menu need the normal view
     * to be pointed at with the controllers.
     */
    private static boolean headsetFree() {
        if (!VrConfig.get().fpvInHeadset) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.gui.screen() != null) {
            return false;
        }
        return !KeyboardHandler.SHOWING && !RadialHandler.isShowing();
    }

    private static void prepare() {
        if (preparedFrame == frame) {
            return;
        }
        preparedFrame = frame;
        posesFor = null;
        Arrays.fill(poses, null);

        VRPlayer player = DH.vrPlayer;
        VRData data = player == null ? null : player.vrdata_world_render;
        // Propwash's camera mixin never runs in VR (Vivecraft places the camera itself), so the pose is sampled here
        // whenever Propwash flies FPV; this also keeps Propwash's own bookkeeping of the displayed drone up to date
        boolean fpv = data != null && VRState.VR_RUNNING && FpvView.active() && FpvView.cameraPose(LENS, CAMERA);
        boolean on = fpv && headsetFree();
        if (on) {
            VrConfig config = VrConfig.get();
            mode = config.viewMode;
            cameraFov = VideoState.INSTANCE.horizontalFov();
            imageAspect = FpvImage.format().boxed() ? 4.0F / 3.0F : 16.0F / 9.0F;
            zoom = GogglesMath.zoom(config.screenSizeDegrees, cameraFov);

            Quaternionf head = roomRotation(data, data.hmd);
            if (!wasActive || recenterRequested) {
                referenceYaw = GogglesMath.yawOf(head.transform(new Vector3f(0.0F, 0.0F, -1.0F)));
                recenterRequested = false;
            }
            EYE_IN_HEAD[EYE_LEFT].rotation(GogglesMath.relative(head, roomRotation(data, data.eye0)));
            EYE_IN_HEAD[EYE_RIGHT].rotation(GogglesMath.relative(head, roomRotation(data, data.eye1)));
            HEAD_VIEW.set(CAMERA).rotateY(-referenceYaw).mul(head);
        }
        wasActive = on;
        activeFrame = on;
    }

    private static VRData.VRDevicePose build(VRData data, int index) {
        VRData.VRDevicePose source = switch (index) {
            case EYE_LEFT -> data.eye0;
            case EYE_RIGHT -> data.eye1;
            default -> data.center;
        };
        Quaternionf head = roomRotation(data, data.hmd);
        Quaternionf eye = roomRotation(data, source);
        Quaternionf world = new Quaternionf(CAMERA);
        Vector3d position = new Vector3d(LENS);
        if (mode == ViewMode.GOGGLES) {
            if (index != EYE_CENTER) {
                // keep the eye's own rotation inside the head (canted displays), drop the head tracking;
                // the desktop mirror (CENTER) shows the plain camera picture
                world.mul(GogglesMath.relative(head, eye));
            }
        } else {
            Quaternionf reference = new Quaternionf().rotationY(-referenceYaw);
            world.mul(reference).mul(eye);
            Vec3 offsetWorld = source.getPosition().subtract(data.hmd.getPosition());
            Vector3f offset = new Vector3f((float) offsetWorld.x, (float) offsetWorld.y, (float) offsetWorld.z)
                    .rotateY(-data.rotation_radians);
            reference.transform(offset);
            CAMERA.transform(offset);
            offset.mul(VrConfig.get().immersiveStereo);
            position.add(offset.x, offset.y, offset.z);
        }
        return devicePose(data, position, world);
    }

    /**
     * Rotation of a pose in room space (without Vivecraft's world rotation).
     */
    private static Quaternionf roomRotation(VRData data, VRData.VRDevicePose pose) {
        return GogglesMath.rotationOf(new Matrix4f().rotationY(-data.rotation_radians).mul(pose.getMatrix()));
    }

    /**
     * Builds a pose of {@code data} whose world position and orientation are the given ones.
     */
    private static VRData.VRDevicePose devicePose(VRData data, Vector3dc worldPosition, Quaternionfc worldRotation) {
        Vector3f roomPosition = GogglesMath.roomPosition(worldPosition, data.origin.x, data.origin.y, data.origin.z,
                data.rotation_radians, data.worldScale);
        Matrix4f roomMatrix = GogglesMath.roomMatrix(worldRotation, data.rotation_radians);
        Vector3f direction = roomMatrix.transformDirection(new Vector3f(0.0F, 0.0F, -1.0F));
        return data.new VRDevicePose(data, roomMatrix, roomPosition, direction);
    }

    /**
     * Vivecraft tracker that only marks the start of each VR frame.
     */
    public static final class FrameTracker implements Tracker {
        @Override
        public ProcessType processType() {
            return ProcessType.PER_FRAME;
        }

        @Override
        public boolean isActive(LocalPlayer player) {
            return true;
        }

        @Override
        public void activeProcess(LocalPlayer player) {
            nextFrame();
        }
    }
}
