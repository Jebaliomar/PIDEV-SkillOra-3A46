package tn.esprit.controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.controllers.AdminPanelController;
import tn.esprit.controllers.forum.AdminPostManagementController;
import tn.esprit.mains.ForumCrudLauncher;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.PomodoroIcon;
import tn.esprit.tools.PomodoroPopup;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public class AdminSidebarController {

    public enum ActiveItem {
        DASHBOARD,
        USERS,
        EVENTS,
        SALLES,
        RESERVATIONS,
        RENDEZVOUS,
        FORUM,
        COURSES,
        EVALUATIONS,
        STATISTICS,
        ACCESSIBILITY
    }

    @FXML private VBox sidebarRoot;
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navEvents;
    @FXML private Button navSalles;
    @FXML private Button navReservations;
    @FXML private Button navRendezVous;
    @FXML private Button navForum;
    @FXML private Button navCourses;
    @FXML private Button navEvaluations;
    @FXML private Button navStats;
    @FXML private Button navAccessibility;
    @FXML private Button themeToggleBtn;
    @FXML private Button pomodoroBtn;
    @FXML private Button signOutButton;

    private ActiveItem activeItem;
    private Button activeButton;
    private static final String ACTIVE_CLASS = "admin-sidebar-nav-item-active";
    private static final String LEGACY_ACTIVE_CLASS = "sidebar-nav-item-active";

    @FXML
    public void initialize() {
        if (pomodoroBtn != null) {
            pomodoroBtn.setText("");
            pomodoroBtn.setGraphic(PomodoroIcon.small());
            pomodoroBtn.setTooltip(new Tooltip("Pomodoro"));
        }
        updateThemeButton();

        if (sidebarRoot != null) {
            sidebarRoot.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    if (!sidebarRoot.isManaged() || !sidebarRoot.isVisible()) {
                        return;
                    }
                    addStylesheet(newScene, "/styles/style.css", true);
                    ThemeManager.applyTheme(newScene);
                    updateThemeButton();
                }
            });
        }
    }

    public void setActive(ActiveItem item) {
        activeItem = item;
        if (activeButton != null) {
            activeButton.getStyleClass().remove(ACTIVE_CLASS);
            activeButton.getStyleClass().remove(LEGACY_ACTIVE_CLASS);
        }

        activeButton = buttonFor(item);
        if (activeButton != null && !activeButton.getStyleClass().contains(ACTIVE_CLASS)) {
            activeButton.getStyleClass().add(ACTIVE_CLASS);
        }
    }

    @FXML
    private void showDashboard() {
        navigateToAdminShell(ActiveItem.DASHBOARD, navDashboard, AdminPanelController::showDashboard, "SkillORA - Admin Panel");
    }

    @FXML
    private void showUserManagement() {
        navigateToAdminShell(ActiveItem.USERS, navUsers, AdminPanelController::showUserManagement, "SkillORA - User Management");
    }

    @FXML
    private void showEventsDashboard() {
        navigateToAdminPage(ActiveItem.EVENTS, navEvents, "/viewsadmin/event/EventDashboard.fxml",
                "Skillora Events", "/styles/event.css", "/styles/salle.css");
    }

    @FXML
    private void showSallesDashboard() {
        navigateToAdminPage(ActiveItem.SALLES, navSalles, "/viewsadmin/event/SalleDashboard.fxml",
                "Skillora Salles", "/styles/event.css", "/styles/salle.css");
    }

    @FXML
    private void showReservationsDashboard() {
        navigateToAdminPage(ActiveItem.RESERVATIONS, navReservations, "/viewsadmin/event/ReservationDashboard.fxml",
                "Skillora Reservations", "/styles/event.css", "/styles/salle.css", "/styles/reservations.css");
    }

    @FXML
    private void showRendezVousAdmin() {
        navigateToAdminPage(ActiveItem.RENDEZVOUS, navRendezVous, "/tn/esprit/views/backoffice/rendezvous-backoffice.fxml",
                "Skillora Rendez-vous", "/tn/esprit/views/theme/skillora-pro.css");
    }

    @FXML
    private void showAdminForum() {
        navigateToAdminPage(ActiveItem.FORUM, navForum, "/tn/esprit/forum/admin-post-management.fxml",
                "Admin Forum", "/tn/esprit/forum/forum.css");
    }

    @FXML
    private void showCoursesAdmin() {
        navigateToAdminShell(ActiveItem.COURSES, navCourses, AdminPanelController::showCoursesAdmin, "SkillORA - Courses Admin");
    }

    @FXML
    private void showEvaluationsAdmin() {
        navigateToAdminShell(ActiveItem.EVALUATIONS, navEvaluations, AdminPanelController::showEvaluationsAdmin, "SkillORA - Evaluations");
    }

    @FXML
    private void showStatistics() {
        navigateToAdminShell(ActiveItem.STATISTICS, navStats, AdminPanelController::showStatistics, "SkillORA - Statistics");
    }

    @FXML
    private void showAccessibility() {
        navigateToAdminShell(ActiveItem.ACCESSIBILITY, navAccessibility, AdminPanelController::showAccessibility, "SkillORA - Accessibility");
    }

    @FXML
    private void togglePomodoro() {
        if (sidebarRoot != null && sidebarRoot.getScene() != null) {
            PomodoroPopup.toggle(sidebarRoot.getScene().getWindow());
        }
    }

    @FXML
    private void toggleTheme() {
        if (themeToggleBtn != null && themeToggleBtn.getScene() != null) {
            ThemeManager.toggle(themeToggleBtn.getScene());
            updateThemeButton();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            AppNavigator.showLogin(signOutButton);
        } catch (Exception e) {
            showError("Sign out failed", e.getMessage());
        }
    }

    private void navigateToAdminShell(ActiveItem target,
                                      Button source,
                                      Consumer<AdminPanelController> afterLoad,
                                      String title) {
        if (activeItem == target) {
            return;
        }

        Stage stage = resolveStage(source);
        if (stage == null) {
            showError("Navigation failed", "No active window is available.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminPanel.fxml"));
            Parent root = loader.load();
            Scene scene = AppWindow.createScene(root);
            addStylesheet(scene, "/styles/style.css", false);
            addStylesheet(scene, "/styles/event.css", false);
            addStylesheet(scene, "/styles/salle.css", false);
            AppWindow.show(stage, scene, title, false);

            AdminPanelController controller = loader.getController();
            if (controller != null) {
                afterLoad.accept(controller);
            }
            ThemeManager.applyTheme(scene);
        } catch (IOException e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void navigateToAdminPage(ActiveItem target,
                                     Button source,
                                     String fxmlPath,
                                     String title,
                                     String... stylesheets) {
        if (activeItem == target) {
            return;
        }

        Stage stage = resolveStage(source);
        if (stage == null) {
            showError("Navigation failed", "No active window is available.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            initializeForumAdminIfNeeded(loader, stage);
            Scene scene = AppWindow.createScene(root);
            addStylesheet(scene, "/styles/style.css", false);
            for (String stylesheet : stylesheets) {
                addStylesheet(scene, stylesheet, false);
            }
            ThemeManager.applyTheme(scene);
            AppWindow.show(stage, scene, title, false);
        } catch (IOException e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void initializeForumAdminIfNeeded(FXMLLoader loader, Stage stage) {
        Object controller = loader.getController();
        if (controller instanceof AdminPostManagementController forumController) {
            ForumCrudLauncher forum = new ForumCrudLauncher();
            forum.initForEmbedded(stage);
            forumController.setApplication(forum);
            forumController.loadPosts();
        }
    }

    private Stage resolveStage(Button source) {
        if (source == null || source.getScene() == null || source.getScene().getWindow() == null) {
            return null;
        }
        return (Stage) source.getScene().getWindow();
    }

    private Button buttonFor(ActiveItem item) {
        if (item == null) {
            return null;
        }
        return switch (item) {
            case DASHBOARD -> navDashboard;
            case USERS -> navUsers;
            case EVENTS -> navEvents;
            case SALLES -> navSalles;
            case RESERVATIONS -> navReservations;
            case RENDEZVOUS -> navRendezVous;
            case FORUM -> navForum;
            case COURSES -> navCourses;
            case EVALUATIONS -> navEvaluations;
            case STATISTICS -> navStats;
            case ACCESSIBILITY -> navAccessibility;
        };
    }

    private void updateThemeButton() {
        if (themeToggleBtn == null) {
            return;
        }
        themeToggleBtn.setText("");
        themeToggleBtn.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
    }

    private void addStylesheet(Scene scene, String path, boolean first) {
        if (scene == null || path == null || path.isBlank()) {
            return;
        }
        URL resource = getClass().getResource(path);
        if (resource == null) {
            return;
        }
        String stylesheet = resource.toExternalForm();
        if (scene.getStylesheets().contains(stylesheet)) {
            return;
        }
        if (first) {
            scene.getStylesheets().add(0, stylesheet);
        } else {
            scene.getStylesheets().add(stylesheet);
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Admin Navigation");
        alert.setHeaderText(header);
        alert.setContentText(content == null || content.isBlank() ? "Unknown error" : content);
        alert.showAndWait();
    }
}
