package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.util.ResourceBundle;

public class StudentLayoutController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button themeToggleBtn;
    @FXML private Button navHome;
    @FXML private Button navProfile;
    @FXML private Button navSettings;
    @FXML private Label userNameLabel;

    private static User currentUser;
    private static StudentLayoutController instance;

    public static StudentLayoutController getInstance() {
        return instance;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        if (currentUser != null && userNameLabel != null) {
            userNameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
        }
        updateThemeButton();
        showHome();
    }

    @FXML
    public void showHome() {
        loadContent("/fxml/HomeContent.fxml");
        setActiveNav(navHome);
    }

    @FXML
    public void showProfile() {
        loadContent("/fxml/ProfileContent.fxml");
        setActiveNav(navProfile);
    }

    @FXML
    public void showSettings() {
        loadContent("/fxml/SettingsContent.fxml");
        setActiveNav(navSettings);
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(Button active) {
        for (Button b : new Button[]{navHome, navProfile, navSettings}) {
            b.getStyleClass().remove("student-nav-btn-active");
        }
        if (!active.getStyleClass().contains("student-nav-btn-active")) {
            active.getStyleClass().add("student-nav-btn-active");
        }
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (themeToggleBtn == null) return;
        themeToggleBtn.setText("");
        themeToggleBtn.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
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
            stage.setTitle("SkillORA - Sign In");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
