package com.fullmetalex.propwashvr.math;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3dc;
import org.joml.Vector3f;

/**
 * Pure math for the VR views, kept free of Minecraft and Vivecraft so it can be unit tested.
 */
public final class GogglesMath {
    public static final float MIN_CAMERA_FOV = 10.0F;
    public static final float MAX_CAMERA_FOV = 175.0F;

    private GogglesMath() {
    }

    /**
     * Factor that shrinks the camera picture so its full horizontal field of view fills a virtual screen of
     * {@code screenDegrees} in front of the eye: a direction at tangent t from the camera axis shows up at tangent
     * {@code t * zoom} from the head's forward direction.
     */
    public static float zoom(double screenDegrees, double cameraHorizontalFovDegrees) {
        double camera = Math.max(MIN_CAMERA_FOV, Math.min(MAX_CAMERA_FOV, cameraHorizontalFovDegrees));
        double screen = Math.max(1.0, Math.min(179.0, screenDegrees));
        return (float) (Math.tan(Math.toRadians(screen) * 0.5) / Math.tan(Math.toRadians(camera) * 0.5));
    }

    /**
     * Projection for one eye of the virtual goggles. The eye renders with the orientation {@code camera * eyeInHead},
     * and the returned projection scales the picture around the head's forward direction, so both eyes of a headset
     * with canted displays still agree on where the screen is:
     * {@code projection * eyeInHead⁻¹ * scale(zoom, zoom, 1) * eyeInHead}.
     *
     * @param projection the headset's projection for this eye
     * @param eyeInHead  rotation of this eye relative to the head (identity on most headsets)
     * @param zoom       see {@link #zoom(double, double)}
     * @param dest       receives the result
     * @return {@code dest}
     */
    public static Matrix4f gogglesProjection(Matrix4fc projection, Matrix4fc eyeInHead, float zoom, Matrix4f dest) {
        Matrix4f around = new Matrix4f(eyeInHead).invert().scale(zoom, zoom, 1.0F).mul(eyeInHead);
        return dest.set(projection).mul(around);
    }

    /**
     * Angle around +Y of the horizontal part of {@code forward}, such that {@code rotationY(angle)} turns (0, 0, -1)
     * into that direction. Returns 0 for a forward vector pointing straight up or down.
     */
    public static float yawOf(Vector3f forward) {
        if (forward.x * forward.x + forward.z * forward.z < 1.0E-8F) {
            return 0.0F;
        }
        return (float) Math.atan2(-forward.x, -forward.z);
    }

    /**
     * Rotation part of a pose matrix as a unit quaternion (drops translation and scale).
     */
    public static Quaternionf rotationOf(Matrix4fc pose) {
        return pose.getNormalizedRotation(new Quaternionf());
    }

    /**
     * Rotation of {@code eye} relative to {@code head}: {@code head⁻¹ * eye}.
     */
    public static Quaternionf relative(Quaternionfc head, Quaternionfc eye) {
        return new Quaternionf(head).conjugate().mul(eye).normalize();
    }

    /**
     * Room-space position for a Vivecraft pose whose world position should be {@code world}: Vivecraft places a pose
     * at {@code origin + rotateY(rotation) * (room * scale)}.
     */
    public static Vector3f roomPosition(Vector3dc world, double originX, double originY, double originZ, float rotation, float scale) {
        return new Vector3f((float) (world.x() - originX), (float) (world.y() - originY), (float) (world.z() - originZ))
                .rotateY(-rotation)
                .div(scale);
    }

    /**
     * Room-space orientation for a Vivecraft pose whose world orientation should be {@code world}: Vivecraft's world
     * matrix is {@code rotationY(rotation) * room}.
     */
    public static Matrix4f roomMatrix(Quaternionfc world, float rotation) {
        return new Matrix4f().rotationY(-rotation).rotate(world);
    }

    /**
     * Half width of the camera picture at distance 1 in front of the camera.
     */
    public static float halfWidthAtUnit(double cameraHorizontalFovDegrees) {
        double camera = Math.max(MIN_CAMERA_FOV, Math.min(MAX_CAMERA_FOV, cameraHorizontalFovDegrees));
        return (float) Math.tan(Math.toRadians(camera) * 0.5);
    }
}
