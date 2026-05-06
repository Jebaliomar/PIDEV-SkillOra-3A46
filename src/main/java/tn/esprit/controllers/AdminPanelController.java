package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.controllers.admin.AdminSidebarController;
import tn.esprit.controllers.forum.AdminPostManagementController;
import tn.esprit.entities.User;
import tn.esprit.mains.ForumCrudLauncher;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.Loaders;
import tn.esprit.tools.PomodoroIcon;
import tn.esprit.tools.PomodoroPopup;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class AdminPanelController implements Initializable {

    public enum InitialView {
        DASHBOARD,
        USERS,
        COURSES,
        STATISTICS
    }

    @FXML private StackPane contentArea;
    @FXML private AdminSidebarController adminSidebarController;

    private static User currentUser;
    private static AdminPanelController instance;
    private static InitialView initialView = InitialView.DASHBOARD;
    private boolean courseStylesPending;

    public static AdminPanelController getInstance() { return instance; }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setInitialView(InitialView view) {
        initialView = view == null ? InitialView.DASHBOARD : view;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        if (contentArea != null) {
            contentArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && courseStylesPending) {
                    ensureCourseStylesheets();
                }
            });
        }
        InitialView view = initialView;
        initialView = InitialView.DASHBOARD;
        switch (view) {
            case USERS -> showUserManagement();
            case COURSES -> showCoursesAdmin();
            case STATISTICS -> showStatistics();
            default -> showDashboard();
        }
    }

    @FXML
    public void togglePomodoro() {
        if (contentArea != null && contentArea.getScene() != null) {
            PomodoroPopup.toggle(contentArea.getScene().getWindow());
        }
    }

    @FXML
    public void showAccessibility() {
        try {
            FXMLLoader loader = Loaders.loader(getClass(), "/fxml/AccessibilityContent.fxml");
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
            setActiveNav(AdminSidebarController.ActiveItem.ACCESSIBILITY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Reloads the whole admin shell - used when language changes so the sidebar/header re-render. */
    public void reloadShell() {
        try {
            Scene scene = contentArea.getScene();
            if (scene == null) return;
            FXMLLoader loader = Loaders.loader(getClass(), "/fxml/AdminPanel.fxml");
            Parent root = loader.load();
            scene.setRoot(root);
            ThemeManager.applyTheme(scene);
            AdminPanelController fresh = loader.getController();
            if (fresh != null) fresh.showAccessibility();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void showDashboard() {
        loadContent("/fxml/DashboardContent.fxml");
        setActiveNav(AdminSidebarController.ActiveItem.DASHBOARD);
    }

    @FXML
    public void showUserManagement() {
        loadContent("/fxml/UserManagementContent.fxml");
        setActiveNav(AdminSidebarController.ActiveItem.USERS);
    }

    @FXML
    public void showStatistics() {
        loadContent("/fxml/StatisticsContent.fxml");
        setActiveNav(AdminSidebarController.ActiveItem.STATISTICS);
    }

    @FXML
    public void showCoursesAdmin() {
        ensureCourseStylesheets();
        AdminShellController shell = new AdminShellController();
        shell.bindContentContainer(contentArea);
        shell.showCoursesIndex();
        setActiveNav(AdminSidebarController.ActiveItem.COURSES);
    }

    @FXML
    public void showEvaluationsAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeEvaluation.fxml"));
            Parent root = loader.load();
            // ListeEvaluation ships with its own left sidebar inside the root
            // AnchorPane. Strip it and embed only the main content so the
            // AdminPanel sidebar stays visible.
            Node embedded = root;
            if (root instanceof javafx.scene.layout.AnchorPane ap && ap.getChildren().size() >= 2) {
                javafx.scene.Node main = ap.getChildren().get(1);
                ap.getChildren().clear();
                if (main instanceof javafx.scene.layout.AnchorPane mainAp) {
                    mainAp.setLayoutX(0);
                    mainAp.setLayoutY(0);
                }
                embedded = (Node) main;
            }
            // The FXML references @style.css via a relative path; re-attach it
            // to the embedded subtree so only this view is affected.
            if (embedded instanceof Parent p) {
                try {
                    String css = getClass().getResource("/style.css").toExternalForm();
                    if (!p.getStylesheets().contains(css)) p.getStylesheets().add(css);
                } catch (Exception ignored) {
                }
            }
            contentArea.getChildren().setAll(embedded);
            setActiveNav(AdminSidebarController.ActiveItem.EVALUATIONS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showEventsDashboard() {
        switchAdminScene(
                "/viewsadmin/event/EventDashboard.fxml",
                "Skillora Events",
                "/styles/event.css",
                "/styles/salle.css"
        );
    }

    @FXML
    public void showSallesDashboard() {
        switchAdminScene(
                "/viewsadmin/event/SalleDashboard.fxml",
                "Skillora Salles",
                "/styles/event.css",
                "/styles/salle.css"
        );
    }

    @FXML
    public void showReservationsDashboard() {
        switchAdminScene(
                "/viewsadmin/event/ReservationDashboard.fxml",
                "Skillora Reservations",
                "/styles/event.css",
                "/styles/salle.css",
                "/styles/reservations.css"
        );
    }

    @FXML
    public void showRendezVousAdmin() {
        switchAdminScene(
                "/tn/esprit/views/backoffice/rendezvous-backoffice.fxml",
                "Skillora Rendez-vous",
                "/tn/esprit/views/theme/skillora-pro.css"
        );
    }

    @FXML
    public void showAdminForum() {
        // Open the forum admin view which lists all posts for administrators
        switchAdminScene(
                "/tn/esprit/forum/admin-post-management.fxml",
                "Admin Forum",
                "/tn/esprit/forum/forum.css"
        );
    }

    @FXML
    public void toggleTheme() {
        if (contentArea != null && contentArea.getScene() != null) {
            ThemeManager.toggle(contentArea.getScene());
        }
    }

    @FXML
    public void handleLogout() {
        try {
            currentUser = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) contentArea.getScene().getWindow();
            AppWindow.show(stage, scene, "Sign In - SkillORA", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadContent(String fxmlPath) {
        try {
            Parent content = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(AdminSidebarController.ActiveItem item) {
        if (adminSidebarController != null) {
            adminSidebarController.setActive(item);
        }
    }

    private void switchAdminScene(String fxmlPath, String title, String... stylesheets) {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            initializeForumAdminIfNeeded(loader, stage);
            Scene scene = AppWindow.createScene(root);
            addStylesheet(scene, "/styles/style.css");
            for (String stylesheet : stylesheets) {
                addStylesheet(scene, stylesheet);
            }
            ThemeManager.applyTheme(scene);
            AppWindow.show(stage, scene, title, false);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load admin view: " + fxmlPath, e);
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

    private void ensureCourseStylesheets() {
        Scene scene = contentArea.getScene();
        if (scene == null) {
            courseStylesPending = true;
            return;
        }
        courseStylesPending = false;
        addStylesheet(scene, "/atlantafx/base/theme/primer-light.css");
        addStylesheet(scene, "/styles/macos-theme.css");
        addStylesheet(scene, "/styles/skillora-unified.css");
        ThemeManager.applyTheme(scene);
    }

    private void addStylesheet(Scene scene, String path) {
        try {
            String url = Objects.requireNonNull(getClass().getResource(path)).toExternalForm();
            if (!scene.getStylesheets().contains(url)) scene.getStylesheets().add(url);
        } catch (Exception ignored) {
        }
    }
}
