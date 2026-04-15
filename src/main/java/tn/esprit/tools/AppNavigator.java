package tn.esprit.tools;

import tn.esprit.controllers.AdminPanelController;
import tn.esprit.controllers.front.FrontShellController;
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

    public static void showFrontBrowseCourses(Node sourceNode) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource("/views/front/front_shell.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof FrontShellController frontShellController) {
                frontShellController.showBrowseCourses();
            }
            Scene scene = new Scene(root, Math.max(stage.getWidth(), 1240), Math.max(stage.getHeight(), 760));
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/atlantafx/base/theme/primer-light.css")).toExternalForm());
            scene.getStylesheets().add(AppNavigator.class.getResource("/styles/macos-theme.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load browse courses view.", e);
        }
    }

    public static void showUserAdmin(Node sourceNode) {
        AdminPanelController.setInitialView(AdminPanelController.InitialView.USERS);
        switchLegacyScene(sourceNode, "/fxml/AdminPanel.fxml", "/styles/style.css");
    }

    public static void showUserFront(Node sourceNode) {
        switchLegacyScene(sourceNode, "/fxml/StudentLayout.fxml", "/styles/style.css");
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

    private static void switchLegacyScene(Node sourceNode, String resourcePath, String stylesheetPath) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = new Scene(root, Math.max(stage.getWidth(), 1240), Math.max(stage.getHeight(), 760));
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource(stylesheetPath)).toExternalForm());
            ThemeManager.applyTheme(scene);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, e);
        }
    }
}
