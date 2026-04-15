package tn.esprit.tools;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import java.util.prefs.Preferences;

public final class ThemeManager {

    private static final String THEME_KEY = "skillora_theme";
    private static final String DARK = "dark";
    private static final String LIGHT = "light";
    private static final String THEME_DARK = "theme-dark";
    private static final String THEME_LIGHT = "theme-light";
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static boolean darkMode;

    static {
        darkMode = DARK.equals(PREFS.get(THEME_KEY, LIGHT));
    }

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        String darkCss = ThemeManager.class.getResource("/styles/dark-theme.css").toExternalForm();
        if (darkMode) {
            if (!scene.getStylesheets().contains(darkCss)) {
                scene.getStylesheets().add(darkCss);
            }
        } else {
            scene.getStylesheets().remove(darkCss);
        }

        Pane root = (scene.getRoot() instanceof Pane pane) ? pane : null;
        if (root == null) {
            return;
        }
        root.getStyleClass().remove(THEME_DARK);
        root.getStyleClass().remove(THEME_LIGHT);
        root.getStyleClass().add(darkMode ? THEME_DARK : THEME_LIGHT);
    }

    public static void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        PREFS.put(THEME_KEY, darkMode ? DARK : LIGHT);
        applyTheme(scene);
    }

    public static void toggle(Scene scene) {
        toggleTheme(scene);
    }

    public static void syncToggleButton(Button button) {
        if (button == null) {
            return;
        }

        button.setText(darkMode ? "☀" : "☾");
    }

    public static boolean isDarkMode() {
        return darkMode;
    }
}
