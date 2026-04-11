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

public class SignUpController implements Initializable {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private RadioButton studentRadio;
    @FXML private RadioButton professorRadio;
    @FXML private ToggleGroup roleGroup;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private HBox successBox;
    @FXML private Label successLabel;
    @FXML private Button themeToggleBtn;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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
        if (themeToggleBtn.getScene() != null) {
            ThemeManager.applyTheme(themeToggleBtn.getScene());
        }
    }

    @FXML
    public void handleSignUp() {
        hideError();
        hideSuccess();

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Name validation: letters only, 2-50 chars
        if (firstName.isEmpty() || lastName.isEmpty()) {
            showError("First name and last name are required.");
            return;
        }
        if (!firstName.matches("[a-zA-Z\\s]{2,50}")) {
            showError("First name must be 2-50 characters, letters only.");
            return;
        }
        if (!lastName.matches("[a-zA-Z\\s]{2,50}")) {
            showError("Last name must be 2-50 characters, letters only.");
            return;
        }

        // Email validation
        if (email.isEmpty()) {
            showError("Email is required.");
            return;
        }
        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        // Password validation: 8+ chars
        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        // Password strength: requires 3 of 4 types (upper, lower, number, special)
        int types = 0;
        if (password.matches(".*[a-z].*")) types++;
        if (password.matches(".*[A-Z].*")) types++;
        if (password.matches(".*\\d.*")) types++;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) types++;
        if (types < 3) {
            showError("Password must contain at least 3 of: uppercase, lowercase, number, special character.");
            return;
        }

        // Confirm password
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        // Role selection
        RadioButton selectedRole = (RadioButton) roleGroup.getSelectedToggle();
        if (selectedRole == null) {
            showError("Please select a role (Student or Professor).");
            return;
        }
        String role = selectedRole.getText().toUpperCase();

        try {
            if (userService.emailExists(email)) {
                showError("An account with this email already exists.");
                return;
            }

            User user = new User();
            user.setEmail(email);
            user.setPassword(password);
            user.setFirstName(firstName);
            user.setLastName(lastName);

            userService.register(user, role);

            showSuccess("Account created successfully! You can now sign in.");
            clearForm();

        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    @FXML
    public void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Sign In - SkillORA");
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

    private void showSuccess(String message) {
        successLabel.setText(message);
        successBox.setVisible(true);
        successBox.setManaged(true);
    }

    private void hideSuccess() {
        successBox.setVisible(false);
        successBox.setManaged(false);
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        roleGroup.selectToggle(null);
    }
}
