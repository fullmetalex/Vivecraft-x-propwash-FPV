package com.fullmetalex.propwashvr.config;

/**
 * How the FPV feed reaches the headset.
 */
public enum ViewMode {
    /**
     * Like real FPV goggles: the camera image sits on a flat, head-locked screen in front of the eyes. Turning the head
     * does not move the picture, both eyes see the same image (the camera is mono).
     */
    GOGGLES,
    /**
     * The eyes sit at the drone camera and the head looks around from there, in stereo. Strong motion, only for pilots
     * with VR legs.
     */
    IMMERSIVE;

    public ViewMode next() {
        return this == GOGGLES ? IMMERSIVE : GOGGLES;
    }

    public String translationKey() {
        return "propwash_vr.view_mode." + this.name().toLowerCase(java.util.Locale.ROOT);
    }
}
