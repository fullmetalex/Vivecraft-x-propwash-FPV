package com.fullmetalex.propwashvr.client;

import com.fullmetalex.propwashvr.PropwashVR;
import com.fullmetalex.propwashvr.config.RadioMode;
import com.fullmetalex.propwashvr.config.VrConfig;
import com.fullmetalex.propwashvr.math.StickLayout;
import de.lardtom.propwash.drone.client.PilotSession;
import de.lardtom.propwash.input.DeviceProfile;
import de.lardtom.propwash.input.ProfileDefaults;
import de.lardtom.propwash.input.client.SimulatedRadio;
import de.lardtom.propwash.input.client.TransmitterInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.vivecraft.client.VivecraftVRMod;
import org.vivecraft.client_vr.ClientDataHolderVR;
import org.vivecraft.client_vr.VRState;
import org.vivecraft.client_vr.provider.MCVR;
import org.vivecraft.client_vr.provider.openvr_lwjgl.VRInputAction;

/**
 * The VR controllers as an RC transmitter.
 * <p>
 * While piloting in VR (and, in {@link RadioMode#AUTO}, without a real radio connected) Propwash gets a simulated
 * radio with the gamepad layout ({@link ProfileDefaults#gamepad}) whose sticks and buttons are read from Vivecraft's
 * input actions every frame, right before Propwash reads its radio. The defaults follow Vivecraft's default bindings
 * for joystick controllers (Quest/Rift Touch, Index, Windows Mixed Reality):
 * <ul>
 *     <li>thumbsticks: RC sticks, mode 2 by default (left: throttle and yaw, right: pitch and roll), throttle from the
 *     upper half of the stick like on a gamepad</li>
 *     <li>right grip: arm (toggle), left grip: angle mode (toggle, starts on)</li>
 *     <li>right trigger: flip over after crash (hold), left trigger: GPS rescue (toggle)</li>
 *     <li>left stick click: beeper (toggle)</li>
 *     <li>A (use): stop piloting, as with the right mouse button</li>
 * </ul>
 * Meanwhile Vivecraft's turning, hotbar switching and teleport are held back so the sticks and grips only fly.
 */
public final class VrRadio {
    public static final String GUID = "propwash_vr:vr_controllers";
    public static final String NAME = "VR controllers";

    private static final int AXES = ProfileDefaults.PAD_AXES;
    private static final int BUTTONS = ProfileDefaults.PAD_BUTTONS;

    private static SimulatedRadio radio;
    private static boolean engaged;

    private VrRadio() {
    }

    /**
     * Runs once per frame, before Propwash evaluates its radio input.
     */
    public static void onFrame() {
        TransmitterInput input = TransmitterInput.INSTANCE;
        if (wanted()) {
            SimulatedRadio controllers = radio();
            if (!engaged || !input.simulating()) {
                input.simulate(controllers);
                engaged = true;
                PropwashVR.LOG.info("Propwash VR: VR controllers act as the radio");
            }
            feed(controllers);
        } else if (engaged) {
            engaged = false;
            if (input.simulating()) {
                input.simulate(null);
            }
            PropwashVR.LOG.info("Propwash VR: VR controllers released the radio");
        }
    }

    /**
     * @return whether the VR controllers fly the drone right now, so Vivecraft's own uses of the sticks and grips
     * have to wait
     */
    public static boolean flying() {
        return engaged;
    }

    /**
     * Throws away Vivecraft's hotbar clicks from the grips while they arm and switch flight modes.
     */
    public static void drainVivecraftKeys() {
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        drain(mod.keyHotbarNext);
        drain(mod.keyHotbarPrev);
    }

    private static boolean wanted() {
        RadioMode mode = VrConfig.get().radio;
        if (mode == RadioMode.OFF || !VRState.VR_RUNNING || MCVR.get() == null) {
            return false;
        }
        if (!PilotSession.INSTANCE.isActive()) {
            return false;
        }
        return mode == RadioMode.ALWAYS || TransmitterInput.INSTANCE.openDevice() == null;
    }

    private static SimulatedRadio radio() {
        if (radio == null) {
            DeviceProfile profile = ProfileDefaults.gamepad(NAME);
            profile.setupDone = true;
            radio = new SimulatedRadio(GUID, NAME, profile, AXES, BUTTONS);
        }
        return radio;
    }

    private static void feed(SimulatedRadio controllers) {
        MCVR vr = MCVR.get();
        VivecraftVRMod mod = VivecraftVRMod.INSTANCE;
        Options options = Minecraft.getInstance().options;
        // left-handed mode loads mirrored bindings; swap back so the stick mode stays on the same physical sticks
        boolean reversed = ClientDataHolderVR.getInstance().vrSettings.reverseHands;
        Vector2fc move = axis(vr, mod.keyFreeMoveStrafe);
        Vector2fc turn = axis(vr, mod.keyRotateAxis);
        Vector2fc left = reversed ? turn : move;
        Vector2fc right = reversed ? move : turn;
        float[] channels = StickLayout.channels(VrConfig.get().stickMode, left.x(), left.y(), right.x(), right.y());
        // gamepad layout: y axes point down, the profile inverts throttle and pitch
        controllers.setAxis(ProfileDefaults.PAD_RIGHT_X, channels[0]);
        controllers.setAxis(ProfileDefaults.PAD_RIGHT_Y, -channels[1]);
        controllers.setAxis(ProfileDefaults.PAD_LEFT_X, channels[2]);
        controllers.setAxis(ProfileDefaults.PAD_LEFT_Y, -channels[3]);
        controllers.setAxis(ProfileDefaults.PAD_LEFT_TRIGGER, 0.0F);
        controllers.setAxis(ProfileDefaults.PAD_RIGHT_TRIGGER, 0.0F);

        boolean rightTrigger = pressed(vr, reversed ? mod.keyTeleport : options.keyAttack);
        boolean leftTrigger = pressed(vr, reversed ? options.keyAttack : mod.keyTeleport);
        controllers.setButton(ProfileDefaults.PAD_RIGHT_SHOULDER, pressed(vr, mod.keyHotbarNext));
        controllers.setButton(ProfileDefaults.PAD_LEFT_SHOULDER, pressed(vr, mod.keyHotbarPrev));
        controllers.setButton(ProfileDefaults.PAD_WEST, rightTrigger);
        controllers.setButton(ProfileDefaults.PAD_EAST, leftTrigger);
        controllers.setButton(ProfileDefaults.PAD_NORTH, pressed(vr, options.keyJump));
    }

    private static Vector2fc axis(MCVR vr, KeyMapping key) {
        VRInputAction action = vr.getInputAction(key);
        if (action == null) {
            return new Vector2f();
        }
        Vector2fc value = action.getAxis2D(false);
        return Float.isFinite(value.x()) && Float.isFinite(value.y()) ? value : new Vector2f();
    }

    private static boolean pressed(MCVR vr, KeyMapping key) {
        VRInputAction action = vr.getInputAction(key);
        return action != null && action.isButtonPressed();
    }

    private static void drain(KeyMapping key) {
        while (key.consumeClick()) {
            // drop the click
        }
    }
}
