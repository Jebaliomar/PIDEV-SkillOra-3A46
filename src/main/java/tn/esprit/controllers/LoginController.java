package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;
import tn.esprit.tools.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Apply saved theme on load & update button icon
        updateThemeButton();
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (ThemeManager.isDarkMode()) {
            themeToggleBtn.setText("Sun");
        } else {
            themeToggleBtn.setText("Moon");
        }
        // Apply theme after scene is ready
        if (themeToggleBtn.getScene() != null) {
            ThemeManager.applyTheme(themeToggleBtn.getScene());
        }
    }

    @FXML
    public void handleLogin() {
        hideError();

        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }
        if (password.isEmpty()) {
            showError("Please enter your password.");
            return;
        }

        try {
            User user = userService.authenticate(email, password);
            if (user != null) {
                String role = userService.getUserRole(user.getId());
                System.out.println("Login successful! User: " + user.getFirstName() + " " + user.getLastName() + " | Role: " + role);

                // Navigate to Admin Panel
                AdminPanelController.setCurrentUser(user);
                FXMLLoader dashLoader = new FXMLLoader(getClass().getResource("/fxml/AdminPanel.fxml"));
                Parent dashRoot = dashLoader.load();
                Scene dashScene = new Scene(dashRoot);
                dashScene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
                ThemeManager.applyTheme(dashScene);

                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setScene(dashScene);
                stage.setTitle("SkillORA - Admin Panel");
            } else {
                showError("Invalid email or password. Please try again.");
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void goToSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SignUp.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Create Account - SkillORA");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    private void hideError() {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
    }
}
