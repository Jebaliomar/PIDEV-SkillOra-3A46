package tn.esprit.tools;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class AppWindow {

    public static final double DEFAULT_WIDTH = 1280;
    public static final double DEFAULT_HEIGHT = 800;

    private AppWindow() {
    }

    public static Scene createScene(Parent root) {
        return new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public static void applyDefaultSize(Stage stage) {
        if (stage == null) {
            return;
        }

        stage.setWidth(DEFAULT_WIDTH);
        stage.setHeight(DEFAULT_HEIGHT);
        stage.setMinWidth(DEFAULT_WIDTH);
        stage.setMinHeight(DEFAULT_HEIGHT);
    }

    public static void show(Stage stage, Scene scene, String title, boolean centerOnScreen) {
        if (stage == null || scene == null) {
            return;
        }

        stage.setScene(scene);
        applyDefaultSize(stage);
        if (title != null && !title.isBlank()) {
            stage.setTitle(title);
        }
        if (centerOnScreen) {
            stage.centerOnScreen();
        }
        stage.show();
    }
}
