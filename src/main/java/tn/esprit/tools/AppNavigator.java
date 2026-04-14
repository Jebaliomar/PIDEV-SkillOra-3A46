package tn.esprit.tools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static void showAdminDashboard(Node sourceNode) {
        switchScene(sourceNode, "/views/admin/admin_shell.fxml");
    }

    public static void showFrontHome(Node sourceNode) {
        switchScene(sourceNode, "/views/front/front_shell.fxml");
    }

    private static void switchScene(Node sourceNode, String resourcePath) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = new Scene(root, Math.max(stage.getWidth(), 1240), Math.max(stage.getHeight(), 760));
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/atlantafx/base/theme/primer-light.css")).toExternalForm());
            scene.getStylesheets().add(AppNavigator.class.getResource("/styles/macos-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, e);
        }
    }
}
