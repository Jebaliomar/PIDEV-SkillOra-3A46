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
import tn.esprit.tools.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminPanelController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button navDashboard;
    @FXML private Button navUsers;
    @FXML private Button navStats;
    @FXML private Button themeToggleBtn;

    private static User currentUser;
    private Button activeNav;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateThemeButton();
        showDashboard();
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
            activeNav.getStyleClass().remove("nav-item-active");
        }
        activeNav = btn;
        if (activeNav != null) {
            activeNav.getStyleClass().add("nav-item-active");
        }
    }

    private void updateThemeButton() {
        themeToggleBtn.setText(ThemeManager.isDarkMode() ? "   Sun" : "   Moon");
        if (themeToggleBtn.getScene() != null) {
            ThemeManager.applyTheme(themeToggleBtn.getScene());
        }
    }
}
