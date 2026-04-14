package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.tools.FaceIdServer;
import tn.esprit.tools.RightPanelAnimator;
import tn.esprit.tools.ThemeIcon;
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
    @FXML private Button faceIdBtn;
    @FXML private Label faceIdStatus;

    private final UserService userService = new UserService();
    private final FaceIdService faceIdService = new FaceIdService();
    private double[] pendingFaceDescriptor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateThemeButton();
        themeToggleBtn.sceneProperty().addListener((obs, o, n) -> { if (n != null) RightPanelAnimator.attach(n); });
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        updateThemeButton();
    }

    private void updateThemeButton() {
        themeToggleBtn.setText("");
        themeToggleBtn.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
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

            // Open avatar picker first
            String avatar = pickAvatar();
            if (avatar == null) {
                showError("Please select a 3D avatar to finish registration.");
                return;
            }

            User user = new User();
            user.setEmail(email);
            user.setPassword(password);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setAvatarType(avatar);

            userService.register(user, role);

            if (pendingFaceDescriptor != null) {
                User created = userService.getByEmail(email);
                if (created != null) {
                    faceIdService.saveDescriptor(created.getId(), pendingFaceDescriptor);
                }
            }

            showSuccess("Account created successfully! You can now sign in.");
            clearForm();
            pendingFaceDescriptor = null;
            faceIdStatus.setText("Not set");
            faceIdStatus.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    private String pickAvatar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AvatarPicker.fxml"));
            Parent root = loader.load();
            AvatarPickerController ctrl = loader.getController();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Select Your Avatar");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
            return ctrl.getConfirmedFilename();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    public void handleFaceIdRegister() {
        hideError();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Enter your email first - it identifies your face on the phone.");
            return;
        }
        try {
            FaceIdServer.get();
        } catch (Exception e) {
            showError("Face ID server failed: " + e.getMessage());
            return;
        }
        FaceIdDialogController ctrl = FaceIdDialogController.show(
                FaceIdServer.SessionType.REGISTER,
                "Register your face",
                "Scan this QR with your phone, then take a selfie. Your phone must be on the same Wi-Fi.",
                email);
        if (ctrl != null && ctrl.isConfirmed() && ctrl.getDescriptor() != null) {
            pendingFaceDescriptor = ctrl.getDescriptor();
            faceIdStatus.setText("Face captured");
            faceIdStatus.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px; -fx-font-weight: bold;");
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
