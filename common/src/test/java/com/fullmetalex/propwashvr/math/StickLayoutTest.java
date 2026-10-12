package com.fullmetalex.propwashvr.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StickLayoutTest {
    // left x, left y, right x, right y
    private static final float LX = 0.1F;
    private static final float LY = 0.2F;
    private static final float RX = 0.3F;
    private static final float RY = 0.4F;

    // channels: roll, pitch, yaw, throttle
    @Test
    void mode2ThrottleAndYawLeft() {
        assertArrayEquals(new float[]{RX, RY, LX, LY}, StickLayout.channels(2, LX, LY, RX, RY));
    }

    @Test
    void mode1ThrottleRightPitchLeft() {
        assertArrayEquals(new float[]{RX, LY, LX, RY}, StickLayout.channels(1, LX, LY, RX, RY));
    }

    @Test
    void mode3MirrorsMode2() {
        assertArrayEquals(new float[]{LX, LY, RX, RY}, StickLayout.channels(3, LX, LY, RX, RY));
    }

    @Test
    void mode4ThrottleLeftRollLeft() {
        assertArrayEquals(new float[]{LX, RY, RX, LY}, StickLayout.channels(4, LX, LY, RX, RY));
    }

    @Test
    void invalidModeActsAsMode2() {
        assertArrayEquals(StickLayout.channels(2, LX, LY, RX, RY), StickLayout.channels(7, LX, LY, RX, RY));
        assertFalse(StickLayout.validMode(0));
        assertTrue(StickLayout.validMode(4));
    }
}
