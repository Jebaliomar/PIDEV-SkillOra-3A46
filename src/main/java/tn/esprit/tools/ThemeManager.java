package tn.esprit.tools;

import javafx.scene.Scene;
import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String THEME_KEY = "skillora_theme";
    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private static boolean isDark = false;

    static {
        // Load saved preference on startup (like website's localStorage)
        String saved = prefs.get(THEME_KEY, LIGHT);
        isDark = DARK.equals(saved);
    }

    public static boolean isDarkMode() {
        return isDark;
    }

    public static void toggle(Scene scene) {
        isDark = !isDark;
        prefs.put(THEME_KEY, isDark ? DARK : LIGHT);
        applyTheme(scene);
    }

    public static void applyTheme(Scene scene) {
        String darkCss = ThemeManager.class.getResource("/styles/dark-theme.css").toExternalForm();
        if (isDark) {
            if (!scene.getStylesheets().contains(darkCss)) {
                scene.getStylesheets().add(darkCss);
            }
        } else {
            scene.getStylesheets().remove(darkCss);
        }
    }
}
