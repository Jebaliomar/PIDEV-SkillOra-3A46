package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.tools.AppNavigator;
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
        STATISTICS
    }

    @FXML private StackPane contentArea;
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navEvents;
    @FXML private Button navSalles;
    @FXML private Button navReservations;
    @FXML private Button navCourses;
    @FXML private Button navStats;
    @FXML private Button themeToggleBtn;

    private static User currentUser;
    private static InitialView initialView = InitialView.DASHBOARD;
    private Button activeNav;

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
        updateThemeButton();
        InitialView view = initialView;
        initialView = InitialView.DASHBOARD;
        switch (view) {
            case USERS -> showUserManagement();
            case STATISTICS -> showStatistics();
            default -> showDashboard();
        }
    }

    @FXML
    public void showDashboard() {
        loadContent("/fxml/DashboardContent.fxml");
        setActiveNav(navDashboard);
    }

    @FXML
    public void showUserManagement() {
        loadContent("/fxml/UserManagementContent.fxml");
        setActiveNav(navUsers);
    }

    @FXML
    public void showStatistics() {
        loadContent("/fxml/StatisticsContent.fxml");
        setActiveNav(navStats);
    }

    @FXML
    public void showCoursesAdmin() {
        AppNavigator.showAdminDashboard(navCourses != null ? navCourses : contentArea);
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
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        updateThemeButton();
    }

    @FXML
    public void handleLogout() {
        try {
            currentUser = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Sign In - SkillORA");
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

    private void setActiveNav(Button btn) {
        if (activeNav != null) {
            activeNav.getStyleClass().remove("sidebar-nav-item-active");
        }
        activeNav = btn;
        if (activeNav != null) {
            if (!activeNav.getStyleClass().contains("sidebar-nav-item-active")) {
                activeNav.getStyleClass().add("sidebar-nav-item-active");
            }
        }
    }

    private void updateThemeButton() {
        themeToggleBtn.setText("");
        themeToggleBtn.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
        if (themeToggleBtn.getScene() != null) {
            ThemeManager.applyTheme(themeToggleBtn.getScene());
        }
    }

    private void switchAdminScene(String fxmlPath, String title, String... stylesheets) {
        Stage stage = (Stage) contentArea.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, Math.max(stage.getWidth(), 1240), Math.max(stage.getHeight(), 760));
            for (String stylesheet : stylesheets) {
                scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource(stylesheet)).toExternalForm());
            }
            ThemeManager.applyTheme(scene);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load admin view: " + fxmlPath, e);
        }
    }
}
