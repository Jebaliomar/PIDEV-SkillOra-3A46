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
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.tools.FaceIdServer;
import tn.esprit.tools.RightPanelAnimator;
import tn.esprit.tools.ThemeIcon;
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
    private final FaceIdService faceIdService = new FaceIdService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Apply saved theme on load & update button icon
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

                // Route by role: students go to student layout, admins/professors to admin panel
                String normalizedRole = role == null ? "student" : role.toLowerCase().replace("role_", "");
                boolean isStudent = normalizedRole.equals("student");

                String fxmlPath = isStudent ? "/fxml/StudentLayout.fxml" : "/fxml/AdminPanel.fxml";
                String title = isStudent ? "SkillORA" : "SkillORA - Admin Panel";

                if (isStudent) {
                    StudentLayoutController.setCurrentUser(user);
                } else {
                    AdminPanelController.setCurrentUser(user);
                }

                FXMLLoader dashLoader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent dashRoot = dashLoader.load();
                Scene dashScene = new Scene(dashRoot);
                dashScene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
                ThemeManager.applyTheme(dashScene);

                Stage stage = (Stage) emailField.getScene().getWindow();
                stage.setScene(dashScene);
                stage.setTitle(title);
            } else {
                showError("Invalid email or password. Please try again.");
            }
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    public void handleFaceIdLogin() {
        hideError();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Enter your email first - we need to know whose face to verify.");
            return;
        }
        try {
            User user = userService.getByEmail(email);
            if (user == null) {
                showError("No account found for this email.");
                return;
            }
            double[] stored = faceIdService.getDescriptor(user.getId());
            if (stored == null) {
                showError("This account has no Face ID registered.");
                return;
            }
            if (user.getIsActive() == 0) {
                showError("Your account has been deactivated.");
                return;
            }

            FaceIdServer.get();
            FaceIdDialogController ctrl = FaceIdDialogController.show(
                    FaceIdServer.SessionType.VERIFY,
                    "Verify your face",
                    "Scan this QR with your phone, then take a selfie. Your phone must be on the same Wi-Fi.",
                    email);
            if (ctrl == null || !ctrl.isConfirmed() || ctrl.getDescriptor() == null) {
                return;
            }
            double dist = FaceIdService.distance(stored, ctrl.getDescriptor());
            if (dist >= FaceIdService.MATCH_THRESHOLD) {
                showError("Face did not match. (distance " + String.format("%.2f", dist) + ")");
                return;
            }

            String role = userService.getUserRole(user.getId());
            String normalizedRole = role == null ? "student" : role.toLowerCase().replace("role_", "");
            boolean isStudent = normalizedRole.equals("student");
            String fxmlPath = isStudent ? "/fxml/StudentLayout.fxml" : "/fxml/AdminPanel.fxml";
            String title = isStudent ? "SkillORA" : "SkillORA - Admin Panel";
            if (isStudent) StudentLayoutController.setCurrentUser(user);
            else AdminPanelController.setCurrentUser(user);

            FXMLLoader dashLoader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent dashRoot = dashLoader.load();
            Scene dashScene = new Scene(dashRoot);
            dashScene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(dashScene);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(dashScene);
            stage.setTitle(title);
        } catch (Exception e) {
            showError("Face login failed: " + e.getMessage());
        }
    }

    @FXML
    public void goToForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ForgotPassword.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Forgot password - SkillORA");
        } catch (IOException e) {
            e.printStackTrace();
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
