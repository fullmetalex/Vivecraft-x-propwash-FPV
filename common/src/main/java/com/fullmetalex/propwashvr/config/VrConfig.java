package com.fullmetalex.propwashvr.config;

import com.fullmetalex.propwashvr.PropwashVR;
import com.fullmetalex.propwashvr.math.StickLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings in {@code config/propwash_vr.json}. Unknown or broken values fall back to the defaults.
 */
public final class VrConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VrConfig current = new VrConfig();
    private static Path file;

    /** Show the drone camera in the headset while flying FPV (off: the headset keeps the pilot's own view). */
    public boolean fpvInHeadset = true;
    public ViewMode viewMode = ViewMode.GOGGLES;
    /** Horizontal size of the virtual goggles screen in degrees (goggles mode), also the OSD width in immersive mode. */
    public float screenSizeDegrees = 70.0F;
    /** Black goggles frame around the screen (goggles mode). */
    public boolean gogglesFrame = true;
    /** Show the OSD/HUD in front of the eyes while flying FPV. */
    public boolean osd = true;
    /** Eye separation in immersive mode, 1 = your real IPD, 0 = mono. */
    public float immersiveStereo = 1.0F;
    public RadioMode radio = RadioMode.AUTO;
    /** RC stick mode 1 to 4 for the VR controller sticks (mode 2: throttle and yaw on the left stick). */
    public int stickMode = 2;
    /** Block Vivecraft's snap and smooth turning while the VR controllers fly the drone. */
    public boolean lockTurning = true;

    public static VrConfig get() {
        return current;
    }

    public static void load(Path path) {
        file = path;
        VrConfig loaded = null;
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, VrConfig.class);
            } catch (IOException | JsonParseException e) {
                PropwashVR.LOG.warn("Propwash VR: could not read {}, using defaults", file, e);
            }
        }
        current = loaded == null ? new VrConfig() : loaded;
        current.sanitize();
        save();
    }

    public static void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(current, writer);
            }
        } catch (IOException e) {
            PropwashVR.LOG.warn("Propwash VR: could not write {}", file, e);
        }
    }

    void sanitize() {
        if (this.viewMode == null) {
            this.viewMode = ViewMode.GOGGLES;
        }
        if (this.radio == null) {
            this.radio = RadioMode.AUTO;
        }
        this.screenSizeDegrees = clamp(this.screenSizeDegrees, 20.0F, 140.0F, 70.0F);
        this.immersiveStereo = clamp(this.immersiveStereo, 0.0F, 2.0F, 1.0F);
        if (!StickLayout.validMode(this.stickMode)) {
            this.stickMode = 2;
        }
    }

    private static float clamp(float value, float min, float max, float fallback) {
        if (!Float.isFinite(value)) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }
}
