package tn.esprit.tools;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;

public final class ThemeManager {

    private static final String THEME_DARK = "theme-dark";
    private static final String THEME_LIGHT = "theme-light";

    private static boolean darkMode = true;

    private ThemeManager() {
    }

    public static void applyTheme(Scene scene) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }

        Pane root = (Pane) scene.getRoot();
        root.getStyleClass().remove(THEME_DARK);
        root.getStyleClass().remove(THEME_LIGHT);
        root.getStyleClass().add(darkMode ? THEME_DARK : THEME_LIGHT);
    }

    public static void toggleTheme(Scene scene) {
        darkMode = !darkMode;
        applyTheme(scene);
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
