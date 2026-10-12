package com.fullmetalex.propwashvr.math;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GogglesMathTest {
    private static final float EPS = 1.0E-4F;

    /** Asymmetric headset-like projection with reversed depth, like Vivecraft's eye projections. */
    private static Matrix4f eyeProjection() {
        float near = 0.02F;
        return new Matrix4f().frustum(-1.2F * near, 0.9F * near, -1.1F * near, 1.0F * near, 512.0F, near, true);
    }

    private static Vector3f ndc(Matrix4f projection, Vector3f eyeDirection) {
        Vector4f clip = projection.transform(new Vector4f(eyeDirection, 1.0F));
        return new Vector3f(clip.x / clip.w, clip.y / clip.w, clip.z / clip.w);
    }

    @Test
    void zoomIsOneWhenScreenMatchesCamera() {
        assertEquals(1.0F, GogglesMath.zoom(120.0, 120.0), EPS);
    }

    @Test
    void zoomShrinksWideCameras() {
        float zoom = GogglesMath.zoom(70.0, 140.0);
        assertEquals(Math.tan(Math.toRadians(35.0)) / Math.tan(Math.toRadians(70.0)), zoom, EPS);
        assertTrue(zoom < 1.0F);
    }

    @Test
    void zoomClampsSillyCameraFov() {
        assertEquals(GogglesMath.zoom(70.0, GogglesMath.MAX_CAMERA_FOV), GogglesMath.zoom(70.0, 400.0), EPS);
        assertEquals(GogglesMath.zoom(70.0, GogglesMath.MIN_CAMERA_FOV), GogglesMath.zoom(70.0, 0.0), EPS);
    }

    @Test
    void identityEyeOnlyScalesFocalTerms() {
        Matrix4f projection = eyeProjection();
        Matrix4f goggles = GogglesMath.gogglesProjection(projection, new Matrix4f(), 0.25F, new Matrix4f());
        assertEquals(projection.m00() * 0.25F, goggles.m00(), EPS);
        assertEquals(projection.m11() * 0.25F, goggles.m11(), EPS);
        assertEquals(projection.m20(), goggles.m20(), EPS);
        assertEquals(projection.m21(), goggles.m21(), EPS);
        assertEquals(projection.m22(), goggles.m22(), EPS);
        assertEquals(projection.m32(), goggles.m32(), EPS);
        assertEquals(projection.m23(), goggles.m23(), EPS);
    }

    @Test
    void doesNotTouchTheCachedMatrix() {
        Matrix4f projection = eyeProjection();
        Matrix4f copy = new Matrix4f(projection);
        GogglesMath.gogglesProjection(projection, new Matrix4f().rotationY(0.1F), 0.3F, new Matrix4f());
        assertTrue(copy.equals(projection, 0.0F));
    }

    @Test
    void cameraDirectionsLandOnTheShrunkScreenForCantedEyes() {
        Matrix4f projection = eyeProjection();
        float zoom = GogglesMath.zoom(70.0, 150.0);
        for (float cant : new float[]{0.0F, 0.087F, -0.087F}) {
            Matrix4f eyeInHead = new Matrix4f().rotationY(cant);
            Matrix4f goggles = GogglesMath.gogglesProjection(projection, eyeInHead, zoom, new Matrix4f());
            Matrix4f view = new Matrix4f(eyeInHead).invert();
            for (float[] t : new float[][]{{0.0F, 0.0F}, {1.5F, 0.0F}, {-2.0F, 0.5F}, {0.3F, -1.0F}}) {
                // a camera-space direction, seen by this eye through the virtual goggles ...
                Vector3f camera = new Vector3f(t[0], t[1], -1.0F);
                Vector3f actual = ndc(goggles, view.transformDirection(new Vector3f(camera)));
                // ... appears where the head would see the direction with tangents scaled by zoom
                Vector3f head = new Vector3f(t[0] * zoom, t[1] * zoom, -1.0F);
                Vector3f expected = ndc(projection, view.transformDirection(head));
                assertEquals(expected.x, actual.x, 1.0E-3F, "x for cant " + cant);
                assertEquals(expected.y, actual.y, 1.0E-3F, "y for cant " + cant);
            }
        }
    }

    @Test
    void yawOfMatchesRotationY() {
        for (float angle : new float[]{0.0F, 0.5F, -1.2F, 3.0F}) {
            Vector3f forward = new Matrix4f().rotationY(angle).transformDirection(new Vector3f(0.0F, 0.0F, -1.0F));
            assertEquals(angle, GogglesMath.yawOf(forward), EPS);
        }
        assertEquals(0.0F, GogglesMath.yawOf(new Vector3f(0.0F, 1.0F, 0.0F)), EPS);
    }

    @Test
    void relativeOfSameRotationIsIdentity() {
        Quaternionf head = new Quaternionf().rotateXYZ(0.3F, -1.1F, 0.2F);
        Quaternionf relative = GogglesMath.relative(head, head);
        assertEquals(1.0F, Math.abs(relative.w), EPS);
    }

    @Test
    void roomConversionRoundTripsThroughVivecraftsFormula() {
        Vector3d world = new Vector3d(1012.25, 87.5, -340.75);
        double ox = 1000.5;
        double oy = 64.0;
        double oz = -330.0;
        float rotation = 1.3F;
        float scale = 0.75F;
        Vector3f room = GogglesMath.roomPosition(world, ox, oy, oz, rotation, scale);
        // VRData.VRDevicePose#getPosition
        Vector3f local = new Vector3f(room).mul(scale).rotateY(rotation);
        assertEquals(world.x, ox + local.x, 1.0E-3);
        assertEquals(world.y, oy + local.y, 1.0E-3);
        assertEquals(world.z, oz + local.z, 1.0E-3);

        Quaternionf orientation = new Quaternionf().rotateYXZ(0.4F, -0.2F, 0.9F);
        Matrix4f roomMatrix = GogglesMath.roomMatrix(orientation, rotation);
        // VRData.VRDevicePose#getMatrix
        Matrix4f worldMatrix = new Matrix4f().rotationY(rotation).mul(roomMatrix);
        assertTrue(worldMatrix.equals(new Matrix4f().rotation(orientation), EPS));
    }

    @Test
    void halfWidthAtUnit() {
        assertEquals(1.0F, GogglesMath.halfWidthAtUnit(90.0), EPS);
    }
}
