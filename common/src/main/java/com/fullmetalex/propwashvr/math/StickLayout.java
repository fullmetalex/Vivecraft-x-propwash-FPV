package com.fullmetalex.propwashvr.math;

/**
 * Maps the two VR thumbsticks to the four RC channels for RC stick modes 1 to 4. Values are in the stick's own
 * convention: x right positive, y forward (away from the pilot) positive.
 */
public final class StickLayout {
    private StickLayout() {
    }

    public static boolean validMode(int mode) {
        return mode >= 1 && mode <= 4;
    }

    /**
     * @param mode   RC stick mode 1..4 (invalid values act as mode 2)
     * @param leftX  left stick x
     * @param leftY  left stick y
     * @param rightX right stick x
     * @param rightY right stick y
     * @return channels in the order roll, pitch, yaw, throttle
     */
    public static float[] channels(int mode, float leftX, float leftY, float rightX, float rightY) {
        return switch (mode) {
            // left: yaw + pitch, right: roll + throttle
            case 1 -> new float[]{rightX, leftY, leftX, rightY};
            // left: roll + pitch, right: yaw + throttle
            case 3 -> new float[]{leftX, leftY, rightX, rightY};
            // left: roll + throttle, right: yaw + pitch
            case 4 -> new float[]{leftX, rightY, rightX, leftY};
            // mode 2, the common one: left: yaw + throttle, right: roll + pitch
            default -> new float[]{rightX, rightY, leftX, leftY};
        };
    }
}
