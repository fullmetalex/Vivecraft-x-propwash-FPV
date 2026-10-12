package com.fullmetalex.propwashvr.config;

/**
 * When the VR controllers act as the RC transmitter.
 */
public enum RadioMode {
    /**
     * While piloting in VR, unless a real radio or gamepad is connected.
     */
    AUTO,
    /**
     * While piloting in VR, even with a real radio connected.
     */
    ALWAYS,
    /**
     * Never; fly with a real radio, gamepad or Propwash's keyboard fallback.
     */
    OFF
}
