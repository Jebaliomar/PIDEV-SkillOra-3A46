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
        switchScene(sourceNode, "/views/admin/admin_shell.fxml", "SkillORA - Course Admin", true);
    }

    public static void showFrontHome(Node sourceNode) {
        switchScene(sourceNode, "/views/front/front_shell.fxml", "SkillORA - Courses", true);
    }

    public static void showLogin(Node sourceNode) {
        AuthSession.clear();
        switchScene(sourceNode, "/fxml/Login.fxml", "Sign In - SkillORA", false);
    }

    private static void switchScene(Node sourceNode, String resourcePath, String title, boolean courseShell) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = courseShell
                    ? new Scene(root, Math.max(stage.getWidth(), 1240), Math.max(stage.getHeight(), 760))
                    : new Scene(root, 1000, 620);
            applyStyles(scene, courseShell);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, e);
        }
    }

    private static void applyStyles(Scene scene, boolean courseShell) {
        if (courseShell) {
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/atlantafx/base/theme/primer-light.css")).toExternalForm());
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/styles/macos-theme.css")).toExternalForm());
        }
        scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/styles/style.css")).toExternalForm());
        ThemeManager.applyTheme(scene);
    }
}
