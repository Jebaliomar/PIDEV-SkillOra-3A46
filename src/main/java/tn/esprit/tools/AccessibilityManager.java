package tn.esprit.tools;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.Effect;
import javafx.scene.effect.SepiaTone;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.Properties;
import java.util.prefs.Preferences;

public final class AccessibilityManager {

    public enum FontScale {
        SMALL("Small", 0.88, "access-font-small"),
        NORMAL("Normal", 1.0, "access-font-normal"),
        LARGE("Large", 1.15, "access-font-large"),
        EXTRA_LARGE("Extra Large", 1.30, "access-font-xl");

        public final String label; public final double factor; public final String styleClass;
        FontScale(String l, double f, String s) { this.label = l; this.factor = f; this.styleClass = s; }
    }

    public enum FontFamily {
        DEFAULT("System default", null, "access-font-default"),
        DYSLEXIC("Dyslexia friendly", "Comic Sans MS", "access-font-dyslexic");

        public final String label; public final String familyCss; public final String styleClass;
        FontFamily(String l, String f, String s) { this.label = l; this.familyCss = f; this.styleClass = s; }
    }

    public enum Density {
        COMPACT("Compact", "access-density-compact"),
        COMFORTABLE("Comfortable", "access-density-comfortable"),
        SPACIOUS("Spacious", "access-density-spacious");

        public final String label; public final String styleClass;
        Density(String l, String s) { this.label = l; this.styleClass = s; }
    }

    public enum ColorFilter {
        NONE("None"),
        SEPIA("Sepia (warm paper)"),
        NIGHT_SHIFT("Night shift (warm tone)"),
        GRAYSCALE("Grayscale");
        public final String label;
        ColorFilter(String l) { this.label = l; }
    }

    public enum Preset {
        DAY, NIGHT, READING, PRESENTATION
    }

    public enum ColorBlindMode {
        NONE("None", "access-cb-none"),
        PROTANOPIA("Protanopia (red-blind)", "access-cb-protanopia"),
        DEUTERANOPIA("Deuteranopia (green-blind)", "access-cb-deuteranopia"),
        TRITANOPIA("Tritanopia (blue-blind)", "access-cb-tritanopia");

        public final String label; public final String styleClass;
        ColorBlindMode(String l, String s) { this.label = l; this.styleClass = s; }
    }

    private static final Preferences prefs = Preferences.userNodeForPackage(AccessibilityManager.class);
    private static final String K_FONT_SCALE = "access_font_scale";
    private static final String K_FONT_FAMILY = "access_font_family";
    private static final String K_DENSITY = "access_density";
    private static final String K_HIGH_CONTRAST = "access_high_contrast";
    private static final String K_REDUCED_MOTION = "access_reduced_motion";
    private static final String K_COLOR_BLIND = "access_color_blind";
    private static final String K_FOCUS_RING = "access_focus_ring";
    private static final String K_ACCENT = "access_accent_color";
    private static final String K_BG = "access_bg_color";
    private static final String K_ZOOM = "access_zoom";
    private static final String K_SOUND_CUES = "access_sound_cues";
    private static final String K_SHORTCUTS = "access_shortcuts";
    private static final String K_COLOR_FILTER = "access_color_filter";
    private static final String K_AUTO_DARK = "access_auto_dark";
    private static final int AUTO_DARK_START = 19;
    private static final int AUTO_DARK_END = 7;
    private static final double BASE_FONT_PT = 13.0;
    public static final String DEFAULT_ACCENT = "#7C3AED";
    public static final String DEFAULT_BG = "";

    private static FontScale fontScale = read(K_FONT_SCALE, FontScale.NORMAL, FontScale.class);
    private static FontFamily fontFamily = read(K_FONT_FAMILY, FontFamily.DEFAULT, FontFamily.class);
    private static Density density = read(K_DENSITY, Density.COMFORTABLE, Density.class);
    private static ColorBlindMode colorBlind = read(K_COLOR_BLIND, ColorBlindMode.NONE, ColorBlindMode.class);
    private static boolean highContrast = prefs.getBoolean(K_HIGH_CONTRAST, false);
    private static boolean reducedMotion = prefs.getBoolean(K_REDUCED_MOTION, false);
    private static boolean focusRing = prefs.getBoolean(K_FOCUS_RING, false);
    private static String accentColor = prefs.get(K_ACCENT, DEFAULT_ACCENT);
    private static String backgroundColor = prefs.get(K_BG, DEFAULT_BG);
    private static double zoom = prefs.getDouble(K_ZOOM, 1.0);
    private static boolean soundCues = prefs.getBoolean(K_SOUND_CUES, false);
    private static boolean shortcuts = prefs.getBoolean(K_SHORTCUTS, false);
    private static ColorFilter colorFilter = read(K_COLOR_FILTER, ColorFilter.NONE, ColorFilter.class);
    private static boolean autoDark = prefs.getBoolean(K_AUTO_DARK, false);
    private static Timeline autoDarkTimeline;

    private AccessibilityManager() {}

    public static FontScale getFontScale() { return fontScale; }
    public static FontFamily getFontFamily() { return fontFamily; }
    public static Density getDensity() { return density; }
    public static ColorBlindMode getColorBlind() { return colorBlind; }
    public static boolean isHighContrast() { return highContrast; }
    public static boolean isReducedMotion() { return reducedMotion; }
    public static boolean isFocusRing() { return focusRing; }
    public static String getAccentColor() { return accentColor; }
    public static String getBackgroundColor() { return backgroundColor; }
    public static double getZoom() { return zoom; }
    public static boolean isSoundCues() { return soundCues; }
    public static boolean isShortcuts() { return shortcuts; }
    public static ColorFilter getColorFilter() { return colorFilter; }
    public static boolean isAutoDark() { return autoDark; }

    public static void setFontScale(FontScale v) { fontScale = v; prefs.put(K_FONT_SCALE, v.name()); }
    public static void setFontFamily(FontFamily v) { fontFamily = v; prefs.put(K_FONT_FAMILY, v.name()); }
    public static void setDensity(Density v) { density = v; prefs.put(K_DENSITY, v.name()); }
    public static void setColorBlind(ColorBlindMode v) { colorBlind = v; prefs.put(K_COLOR_BLIND, v.name()); }
    public static void setHighContrast(boolean v) { highContrast = v; prefs.putBoolean(K_HIGH_CONTRAST, v); }
    public static void setReducedMotion(boolean v) { reducedMotion = v; prefs.putBoolean(K_REDUCED_MOTION, v); }
    public static void setFocusRing(boolean v) { focusRing = v; prefs.putBoolean(K_FOCUS_RING, v); }
    public static void setAccentColor(String v) { accentColor = v == null ? DEFAULT_ACCENT : v; prefs.put(K_ACCENT, accentColor); }
    public static void setBackgroundColor(String v) { backgroundColor = v == null ? "" : v; prefs.put(K_BG, backgroundColor); }
    public static void setZoom(double v) { zoom = clamp(v, 0.7, 1.6); prefs.putDouble(K_ZOOM, zoom); }
    public static void setSoundCues(boolean v) { soundCues = v; prefs.putBoolean(K_SOUND_CUES, v); }
    public static void setShortcuts(boolean v) { shortcuts = v; prefs.putBoolean(K_SHORTCUTS, v); }
    public static void setColorFilter(ColorFilter v) { colorFilter = v; prefs.put(K_COLOR_FILTER, v.name()); }
    public static void setAutoDark(boolean v) { autoDark = v; prefs.putBoolean(K_AUTO_DARK, v); }

    public static void resetDefaults() {
        setFontScale(FontScale.NORMAL);
        setFontFamily(FontFamily.DEFAULT);
        setDensity(Density.COMFORTABLE);
        setColorBlind(ColorBlindMode.NONE);
        setHighContrast(false);
        setReducedMotion(false);
        setFocusRing(false);
        setAccentColor(DEFAULT_ACCENT);
        setBackgroundColor(DEFAULT_BG);
        setZoom(1.0);
        setSoundCues(false);
        setShortcuts(false);
        setColorFilter(ColorFilter.NONE);
        setAutoDark(false);
    }

    public static void applyPreset(Preset p) {
        switch (p) {
            case DAY -> {
                setHighContrast(false);
                setColorFilter(ColorFilter.NONE);
                setFontScale(FontScale.NORMAL);
                setDensity(Density.COMFORTABLE);
                setAccentColor(DEFAULT_ACCENT);
            }
            case NIGHT -> {
                setColorFilter(ColorFilter.NIGHT_SHIFT);
                setReducedMotion(true);
                setAccentColor("#A78BFA");
            }
            case READING -> {
                setFontScale(FontScale.LARGE);
                setFontFamily(FontFamily.DYSLEXIC);
                setDensity(Density.SPACIOUS);
                setColorFilter(ColorFilter.SEPIA);
                setFocusRing(true);
            }
            case PRESENTATION -> {
                setFontScale(FontScale.EXTRA_LARGE);
                setDensity(Density.SPACIOUS);
                setHighContrast(true);
                setFocusRing(true);
                setReducedMotion(false);
            }
        }
    }

    public static void exportTo(Path file) throws IOException {
        Properties p = new Properties();
        p.setProperty(K_FONT_SCALE, fontScale.name());
        p.setProperty(K_FONT_FAMILY, fontFamily.name());
        p.setProperty(K_DENSITY, density.name());
        p.setProperty(K_COLOR_BLIND, colorBlind.name());
        p.setProperty(K_HIGH_CONTRAST, Boolean.toString(highContrast));
        p.setProperty(K_REDUCED_MOTION, Boolean.toString(reducedMotion));
        p.setProperty(K_FOCUS_RING, Boolean.toString(focusRing));
        p.setProperty(K_ACCENT, accentColor == null ? "" : accentColor);
        p.setProperty(K_BG, backgroundColor == null ? "" : backgroundColor);
        p.setProperty(K_ZOOM, Double.toString(zoom));
        p.setProperty(K_SOUND_CUES, Boolean.toString(soundCues));
        p.setProperty(K_SHORTCUTS, Boolean.toString(shortcuts));
        p.setProperty(K_COLOR_FILTER, colorFilter.name());
        p.setProperty(K_AUTO_DARK, Boolean.toString(autoDark));
        try (OutputStream os = Files.newOutputStream(file)) {
            p.store(os, "SkillORA accessibility settings");
        }
    }

    public static void importFrom(Path file) throws IOException {
        Properties p = new Properties();
        try (InputStream is = Files.newInputStream(file)) { p.load(is); }
        if (p.containsKey(K_FONT_SCALE)) setFontScale(FontScale.valueOf(p.getProperty(K_FONT_SCALE)));
        if (p.containsKey(K_FONT_FAMILY)) setFontFamily(FontFamily.valueOf(p.getProperty(K_FONT_FAMILY)));
        if (p.containsKey(K_DENSITY)) setDensity(Density.valueOf(p.getProperty(K_DENSITY)));
        if (p.containsKey(K_COLOR_BLIND)) setColorBlind(ColorBlindMode.valueOf(p.getProperty(K_COLOR_BLIND)));
        if (p.containsKey(K_HIGH_CONTRAST)) setHighContrast(Boolean.parseBoolean(p.getProperty(K_HIGH_CONTRAST)));
        if (p.containsKey(K_REDUCED_MOTION)) setReducedMotion(Boolean.parseBoolean(p.getProperty(K_REDUCED_MOTION)));
        if (p.containsKey(K_FOCUS_RING)) setFocusRing(Boolean.parseBoolean(p.getProperty(K_FOCUS_RING)));
        if (p.containsKey(K_ACCENT)) setAccentColor(p.getProperty(K_ACCENT));
        if (p.containsKey(K_BG)) setBackgroundColor(p.getProperty(K_BG));
        if (p.containsKey(K_ZOOM)) setZoom(Double.parseDouble(p.getProperty(K_ZOOM)));
        if (p.containsKey(K_SOUND_CUES)) setSoundCues(Boolean.parseBoolean(p.getProperty(K_SOUND_CUES)));
        if (p.containsKey(K_SHORTCUTS)) setShortcuts(Boolean.parseBoolean(p.getProperty(K_SHORTCUTS)));
        if (p.containsKey(K_COLOR_FILTER)) setColorFilter(ColorFilter.valueOf(p.getProperty(K_COLOR_FILTER)));
        if (p.containsKey(K_AUTO_DARK)) setAutoDark(Boolean.parseBoolean(p.getProperty(K_AUTO_DARK)));
    }

    public static boolean shouldBeDarkNow() {
        int h = LocalTime.now().getHour();
        return h >= AUTO_DARK_START || h < AUTO_DARK_END;
    }

    public static void installAutoDarkSchedule(Scene scene) {
        if (autoDarkTimeline != null) { autoDarkTimeline.stop(); autoDarkTimeline = null; }
        if (!autoDark || scene == null) return;
        ThemeManager.setDark(scene, shouldBeDarkNow());
        autoDarkTimeline = new Timeline(new KeyFrame(Duration.minutes(1), e ->
                ThemeManager.setDark(scene, shouldBeDarkNow())));
        autoDarkTimeline.setCycleCount(Timeline.INDEFINITE);
        autoDarkTimeline.play();
    }

    public static void apply(Scene scene) {
        if (scene == null) return;
        String css = AccessibilityManager.class.getResource("/styles/accessibility.css").toExternalForm();
        if (!scene.getStylesheets().contains(css)) {
            scene.getStylesheets().add(css);
        }
        Parent root = scene.getRoot();
        if (root == null) return;

        root.getStyleClass().removeIf(c -> c.startsWith("access-"));
        root.getStyleClass().add(fontScale.styleClass);
        root.getStyleClass().add(fontFamily.styleClass);
        root.getStyleClass().add(density.styleClass);
        root.getStyleClass().add(colorBlind.styleClass);
        if (highContrast) root.getStyleClass().add("access-high-contrast");
        if (reducedMotion) root.getStyleClass().add("access-reduced-motion");
        if (focusRing) root.getStyleClass().add("access-focus-ring");

        StringBuilder inline = new StringBuilder();
        inline.append("-fx-font-size: ").append(BASE_FONT_PT * fontScale.factor).append("px;");
        if (fontFamily.familyCss != null) {
            inline.append("-fx-font-family: \"").append(fontFamily.familyCss).append("\";");
        }
        if (accentColor != null && !accentColor.isBlank()) {
            inline.append("-fx-accent: ").append(accentColor).append(";");
            inline.append("-fx-focus-color: ").append(accentColor).append(";");
            inline.append("-fx-faint-focus-color: ").append(accentColor).append("33;");
        }
        if (backgroundColor != null && !backgroundColor.isBlank()) {
            inline.append("-fx-background-color: ").append(backgroundColor).append(";");
        }
        root.setStyle(inline.toString());

        applyZoom(scene);
        applyColorFilter(scene);
    }

    private static void applyColorFilter(Scene scene) {
        Parent root = scene.getRoot();
        if (root == null) return;
        Effect e = null;
        switch (colorFilter) {
            case SEPIA -> { SepiaTone s = new SepiaTone(); s.setLevel(0.55); e = s; }
            case GRAYSCALE -> { ColorAdjust c = new ColorAdjust(); c.setSaturation(-1.0); e = c; }
            case NIGHT_SHIFT -> { ColorAdjust c = new ColorAdjust(); c.setHue(-0.05); c.setSaturation(-0.15); c.setBrightness(-0.05); e = c; }
            case NONE -> e = null;
        }
        root.setEffect(e);
    }

    private static final String ZOOM_KEY = "access-zoom-transform";

    private static void applyZoom(Scene scene) {
        Parent root = scene.getRoot();
        if (root == null) return;
        Object existing = root.getProperties().get(ZOOM_KEY);
        if (existing instanceof Scale) {
            root.getTransforms().remove(existing);
            root.getProperties().remove(ZOOM_KEY);
        }
        if (Math.abs(zoom - 1.0) > 0.001) {
            Scale s = new Scale(zoom, zoom);
            s.setPivotX(0); s.setPivotY(0);
            root.getTransforms().add(s);
            root.getProperties().put(ZOOM_KEY, s);
        }
    }

    public static void playCue(boolean success) {
        if (!soundCues) return;
        try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static <E extends Enum<E>> E read(String key, E fallback, Class<E> type) {
        try { return Enum.valueOf(type, prefs.get(key, fallback.name())); }
        catch (Exception e) { return fallback; }
    }
}
