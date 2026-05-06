package tn.esprit.tools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.controllers.AdminPanelController;
import tn.esprit.controllers.ProfessorLayoutController;
import tn.esprit.controllers.StudentLayoutController;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

public final class AppNavigator {

    private AppNavigator() {
    }

    public static void showAdminDashboard(Node sourceNode) {
        if (sourceNode != null && "coursesNavButton".equals(sourceNode.getId())) {
            showCoursesAdmin(sourceNode);
            return;
        }
        switchLegacyScene(sourceNode, "/fxml/AdminPanel.fxml", "/styles/style.css");
    }

    public static void showFrontHome(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showHome, "SkillORA");
    }

    public static void showFrontBrowseCourses(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showCourses, "SkillORA Courses");
    }

    public static void showFrontMyLearning(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showMyLearning, "SkillORA My Learning");
    }

    public static void showFrontEvents(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showEvents, "SkillORA Events");
    }

    public static void showFrontRendezVous(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showRendezVous, "SkillORA Rendez-vous");
    }

    public static void showFrontSlots(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showSlots, "SkillORA Slots");
    }

    public static void showFrontForum(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showForum, "SkillORA Forum");
    }

    public static void showFrontAssessment(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showAssessment, "SkillORA Assessment");
    }

    public static void showFrontMyAssessments(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showMyAssessments, "SkillORA My Assessments");
    }

    public static void showFrontProfile(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showProfile, "SkillORA Profile");
    }

    public static void showFrontSettings(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showSettings, "SkillORA Settings");
    }

    public static void showLogin(Node sourceNode) {
        AuthSession.clear();
        StudentLayoutController.setCurrentUser(null);
        ProfessorLayoutController.setCurrentUser(null);
        AdminPanelController.setCurrentUser(null);
        switchLegacyScene(sourceNode, "/fxml/Login.fxml", "/styles/style.css", "SkillORA - Sign In");
    }

    public static void showUserAdmin(Node sourceNode) {
        AdminPanelController.setInitialView(AdminPanelController.InitialView.USERS);
        switchLegacyScene(sourceNode, "/fxml/AdminPanel.fxml", "/styles/style.css");
    }

    public static void showDashboardAdmin(Node sourceNode) {
        AdminPanelController.setInitialView(AdminPanelController.InitialView.DASHBOARD);
        switchLegacyScene(sourceNode, "/fxml/AdminPanel.fxml", "/styles/style.css");
    }

    public static void showStatisticsAdmin(Node sourceNode) {
        AdminPanelController.setInitialView(AdminPanelController.InitialView.STATISTICS);
        switchLegacyScene(sourceNode, "/fxml/AdminPanel.fxml", "/styles/style.css");
    }

    public static void showCoursesAdmin(Node sourceNode) {
        AdminPanelController.setInitialView(AdminPanelController.InitialView.COURSES);
        switchLegacyScene(sourceNode, "/fxml/AdminPanel.fxml", "/styles/style.css");
    }

    public static void showUserFront(Node sourceNode) {
        showStudentLayout(sourceNode, StudentLayoutController::showHome, "SkillORA");
    }

    private static void showStudentLayout(Node sourceNode,
                                          Consumer<StudentLayoutController> afterLoad,
                                          String title) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource("/fxml/StudentLayout.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (!(controller instanceof StudentLayoutController studentLayoutController)) {
                throw new IllegalStateException("Student layout controller is unavailable.");
            }

            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/styles/style.css")).toExternalForm());
            ThemeManager.applyTheme(scene);
            afterLoad.accept(studentLayoutController);
            ThemeManager.applyTheme(scene);
            AppWindow.show(stage, scene, title, false);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load student workspace.", e);
        }
    }

    private static void switchScene(Node sourceNode, String resourcePath) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/atlantafx/base/theme/primer-light.css")).toExternalForm());
            scene.getStylesheets().add(AppNavigator.class.getResource("/styles/macos-theme.css").toExternalForm());
            AppWindow.show(stage, scene, null, false);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, e);
        }
    }

    private static void switchLegacyScene(Node sourceNode, String resourcePath, String stylesheetPath) {
        switchLegacyScene(sourceNode, resourcePath, stylesheetPath, null);
    }

    private static void switchLegacyScene(Node sourceNode, String resourcePath, String stylesheetPath, String title) {
        if (sourceNode == null || sourceNode.getScene() == null || sourceNode.getScene().getWindow() == null) {
            throw new IllegalStateException("No active window is available for navigation.");
        }

        Stage stage = (Stage) sourceNode.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource(stylesheetPath)).toExternalForm());
            if ("/fxml/AdminPanel.fxml".equals(resourcePath)) {
                scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/styles/event.css")).toExternalForm());
                scene.getStylesheets().add(Objects.requireNonNull(AppNavigator.class.getResource("/styles/salle.css")).toExternalForm());
            }
            ThemeManager.applyTheme(scene);
            AppWindow.show(stage, scene, title, false);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load view: " + resourcePath, e);
        }
    }
}
