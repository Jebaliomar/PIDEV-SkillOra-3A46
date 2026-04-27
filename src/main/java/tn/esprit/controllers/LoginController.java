package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.FaceIdService;
import tn.esprit.services.UserService;
import tn.esprit.tools.AuthSession;
import tn.esprit.tools.FaceIdServer;
import tn.esprit.tools.PasswordToggle;
import tn.esprit.tools.RightPanelAnimator;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordShowField;
    @FXML private Button passwordEyeBtn;
    @FXML private CheckBox rememberMeCheck;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private Button themeToggleBtn;

    private final UserService userService = new UserService();
    private final FaceIdService faceIdService = new FaceIdService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        updateThemeButton();
        themeToggleBtn.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) {
                RightPanelAnimator.attach(n);
            }
        });
        PasswordToggle.wire(passwordField, passwordShowField, passwordEyeBtn);
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
    public void handleLogin() {
        hideError();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

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
            if (user == null) {
                showError("Invalid email or password. Please try again.");
                return;
            }
            openCourseWorkspace(user, userService.getUserRole(user.getId()));
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

            openCourseWorkspace(user, userService.getUserRole(user.getId()));
        } catch (Exception e) {
            showError("Face login failed: " + e.getMessage());
        }
    }

    @FXML
    public void goToForgotPassword() {
        openAuthScreen("/fxml/ForgotPassword.fxml", "Forgot password - SkillORA");
    }

    @FXML
    public void goToSignUp() {
        openAuthScreen("/fxml/SignUp.fxml", "Create Account - SkillORA");
    }

    private void openCourseWorkspace(User user, String role) throws IOException {
        String normalizedRole = AuthSession.normalizeRole(role);
        boolean student = "student".equals(normalizedRole);
        AuthSession.setCurrentUser(user, normalizedRole);

        String fxmlPath = student ? "/views/front/front_shell.fxml" : "/views/admin/admin_shell.fxml";
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Stage stage = (Stage) emailField.getScene().getWindow();
        Scene scene = new Scene(root, Math.max(stage.getWidth(), 1240), Math.max(stage.getHeight(), 760));
        applyAppStyles(scene);
        stage.setScene(scene);
        stage.setTitle(student ? "SkillORA - Courses" : "SkillORA - Course Admin");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.show();
    }

    private void openAuthScreen(String resourcePath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyAuthStyles(scene);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void applyAuthStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());
        ThemeManager.applyTheme(scene);
    }

    private void applyAppStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/atlantafx/base/theme/primer-light.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/macos-theme.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());
        ThemeManager.applyTheme(scene);
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
