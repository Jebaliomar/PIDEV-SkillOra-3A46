package tn.esprit.tools;

import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retints the entire app toward the chosen accent color in light AND dark
 * mode by reading the existing stylesheets, replacing the brand "harbor"
 * cyan hex shades with shades derived from the chosen accent, and loading
 * the tinted copies as additional stylesheets that override the originals.
 *
 * Each apply() writes uniquely-named temp files so JavaFX's stylesheet
 * cache always sees fresh URLs.
 */
public final class AccentTint {

    private static final String TINT_PREFIX = "tint-";
    private static final String TINT_SUFFIX = ".css";

    /** Brand "harbor cyan" shades present in style.css / dark-theme.css.
     *  Each maps to the corresponding tone in the chosen accent: 0.0 = darkest, 1.0 = lightest. */
    private static final Map<String, Double> HARBOR_TONES = new LinkedHashMap<>();
    static {
        HARBOR_TONES.put("#22d3ee", 0.85);  // cyan-400 (lightest in use)
        HARBOR_TONES.put("#06b6d4", 0.65);  // cyan-500
        HARBOR_TONES.put("#0891b2", 0.45);  // cyan-600 (primary brand)
        HARBOR_TONES.put("#0e7490", 0.30);  // cyan-700
        HARBOR_TONES.put("#155e75", 0.18);  // cyan-800
        HARBOR_TONES.put("#ecfeff", 0.96);  // cyan-50 (very pale bg)
        HARBOR_TONES.put("#cffafe", 0.92);  // cyan-100
        HARBOR_TONES.put("#a5f3fc", 0.88);  // cyan-200
        HARBOR_TONES.put("#67e8f9", 0.80);  // cyan-300
        HARBOR_TONES.put("#f0fdfa", 0.97);  // teal-50 (very pale)
    }

    private AccentTint() {}

    public static void apply(Scene scene, String accentHex) {
        if (scene == null) return;
        try {
            Color accent = parse(accentHex, Color.web("#7C3AED"));
            // Skip if accent is essentially the default cyan (no work needed)
            Path dir = Paths.get(System.getProperty("user.home"), ".skillora");
            Files.createDirectories(dir);

            long ts = System.currentTimeMillis();
            boolean dark = ThemeManager.isDarkMode();

            // Drop any previous tint sheets
            scene.getStylesheets().removeIf(s -> s.contains("/" + TINT_PREFIX));

            // Always tint style.css (base look). Tint dark-theme.css ONLY when dark
            // mode is active — its rules use the same selectors as style.css and
            // would otherwise leak into light mode.
            String lightCss = retint("/styles/style.css", accent);
            if (lightCss != null) {
                lightCss += "\n" + glowCss(accent, false);
                Path lightFile = dir.resolve(TINT_PREFIX + "light-" + ts + TINT_SUFFIX);
                Files.write(lightFile, lightCss.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                scene.getStylesheets().add(lightFile.toUri().toString());
            }
            if (dark) {
                String darkCss = retint("/styles/dark-theme.css", accent);
                if (darkCss != null) {
                    darkCss += "\n" + glowCss(accent, true);
                    Path darkFile = dir.resolve(TINT_PREFIX + "dark-" + ts + TINT_SUFFIX);
                    Files.write(darkFile, darkCss.getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    scene.getStylesheets().add(darkFile.toUri().toString());
                }
            }

            // Cleanup older tint files
            try (var stream = Files.list(dir)) {
                stream.filter(p -> {
                    String n = p.getFileName().toString();
                    boolean isTint = n.startsWith(TINT_PREFIX) && n.endsWith(TINT_SUFFIX);
                    return isTint && !n.contains("-" + ts + ".");
                }).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Generates accent-tinted ambient glows / shadows applied on top of the
     *  retinted base stylesheet. Light mode uses softer translucent glows;
     *  dark mode uses brighter, more saturated halos so the color "breathes". */
    private static String glowCss(Color accent, boolean dark) {
        String rgb = String.format("%d,%d,%d",
                (int) Math.round(accent.getRed() * 255),
                (int) Math.round(accent.getGreen() * 255),
                (int) Math.round(accent.getBlue() * 255));

        // Tuned per mode
        double cardA   = dark ? 0.30 : 0.18;
        double cardR   = dark ? 28   : 22;
        double btnA    = dark ? 0.55 : 0.35;
        double btnR    = dark ? 18   : 14;
        double hoverA  = dark ? 0.75 : 0.55;
        double hoverR  = dark ? 26   : 22;
        double sideA   = dark ? 0.45 : 0.22;
        double sideR   = dark ? 32   : 24;
        double titleA  = dark ? 0.55 : 0.30;
        double titleR  = dark ? 16   : 10;
        double inputA  = dark ? 0.45 : 0.22;

        StringBuilder b = new StringBuilder();
        b.append("/* === SkillORA accent glow layer === */\n");

        // Cards & dialogs - soft accent halo
        b.append(".chart-card, .skill-detail-card, .reason-dialog, .pomodoro-root, .access-card, .preview-card {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(cardA)).append("), ")
         .append(cardR).append(", 0.10, 0, 6); }\n");

        // Sidebar & header - inner accent ambience
        b.append(".sidebar, .student-header, .pf-header {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(sideA)).append("), ")
         .append(sideR).append(", 0.10, 0, 4); }\n");

        // Primary buttons - tinted shadow + lift on hover
        b.append(".btn-primary, .acc-btn-primary, .reason-dialog-btn-confirm, .pomodoro-btn-primary {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(btnA)).append("), ")
         .append(btnR).append(", 0.10, 0, 4); }\n");
        b.append(".btn-primary:hover, .acc-btn-primary:hover, .reason-dialog-btn-confirm:hover, .pomodoro-btn-primary:hover {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(hoverA)).append("), ")
         .append(hoverR).append(", 0.20, 0, 6); }\n");

        // Headlines / brand text - subtle accent halo
        b.append(".dashboard-title, .page-title, .logo-text, .sidebar-brand, .reason-dialog-title, .skill-detail-title {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(titleA)).append("), ")
         .append(titleR).append(", 0.10, 0, 2); }\n");

        // Focus rings on inputs - accent glow when focused
        b.append(".text-input:focused, .custom-text-field:focused, .password-field:focused, .combo-box:focused, .text-area:focused {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(inputA)).append("), 12, 0.10, 0, 0); }\n");

        // Theme chip — pulse glow in chosen color
        b.append(".theme-chip:hover {")
         .append(" -fx-effect: dropshadow(gaussian, rgba(").append(rgb).append(",").append(fmt(hoverA)).append("), 18, 0.20, 0, 4); }\n");

        // Skill graph nodes — extra halo
        b.append(".skill-graph-pane {")
         .append(" -fx-effect: innershadow(gaussian, rgba(").append(rgb).append(",").append(fmt(sideA * 0.6)).append("), 60, 0.10, 0, 0); }\n");

        return b.toString();
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    public static void clear(Scene scene) {
        if (scene == null) return;
        scene.getStylesheets().removeIf(s -> s.contains("/" + TINT_PREFIX));
    }

    private static String retint(String classpath, Color accent) {
        try (InputStream is = AccentTint.class.getResourceAsStream(classpath)) {
            if (is == null) return null;
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
            }
            String css = sb.toString();
            for (Map.Entry<String, Double> e : HARBOR_TONES.entrySet()) {
                String replacement = hex(toneOf(accent, e.getValue()));
                css = replaceIgnoreCase(css, e.getKey(), replacement);
            }
            // Also retint a few common rgba(8,145,178,...) and rgba(6,182,212,...) drop-shadows
            String accentRgb = String.format("%d,%d,%d",
                    (int) Math.round(accent.getRed() * 255),
                    (int) Math.round(accent.getGreen() * 255),
                    (int) Math.round(accent.getBlue() * 255));
            css = css.replace("rgba(8,145,178,",   "rgba(" + accentRgb + ",");
            css = css.replace("rgba(8, 145, 178,", "rgba(" + accentRgb + ",");
            css = css.replace("rgba(6,182,212,",   "rgba(" + accentRgb + ",");
            css = css.replace("rgba(6, 182, 212,", "rgba(" + accentRgb + ",");
            css = css.replace("rgba(34,211,238,",  "rgba(" + accentRgb + ",");
            css = css.replace("rgba(34, 211, 238,", "rgba(" + accentRgb + ",");
            return css;
        } catch (IOException e) {
            return null;
        }
    }

    private static String replaceIgnoreCase(String src, String find, String replacement) {
        // Case-insensitive literal replace (no regex special handling needed for hex strings)
        StringBuilder out = new StringBuilder(src.length());
        String lowerSrc = src.toLowerCase();
        String lowerFind = find.toLowerCase();
        int i = 0;
        while (true) {
            int idx = lowerSrc.indexOf(lowerFind, i);
            if (idx < 0) { out.append(src, i, src.length()); break; }
            out.append(src, i, idx).append(replacement);
            i = idx + find.length();
        }
        return out.toString();
    }

    /** Produce a shade of the accent color: tone ∈ [0,1], 0=black, 1=white. */
    private static Color toneOf(Color base, double tone) {
        // Mix toward black for tone<0.5, toward white for tone>0.5,
        // keeping the hue stable.
        if (tone <= 0.5) {
            double t = 1.0 - (tone * 2.0); // 0..1 (1 = full black mix)
            return mix(base, Color.BLACK, t * 0.85);
        } else {
            double t = (tone - 0.5) * 2.0; // 0..1 (1 = full white mix)
            return mix(base, Color.WHITE, t * 0.95);
        }
    }

    private static Color parse(String hex, Color fallback) {
        try { return Color.web(hex); } catch (Exception e) { return fallback; }
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                a.getRed()   * (1 - t) + b.getRed()   * t,
                a.getGreen() * (1 - t) + b.getGreen() * t,
                a.getBlue()  * (1 - t) + b.getBlue()  * t,
                1.0);
    }
}
