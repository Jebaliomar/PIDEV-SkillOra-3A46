package tn.esprit.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.services.PasswordResetService;
import tn.esprit.tools.RightPanelAnimator;
import tn.esprit.tools.ThemeManager;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private Button sendBtn;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;

    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    public void initialize() {
        emailField.sceneProperty().addListener((obs, o, n) -> { if (n != null) RightPanelAnimator.attach(n); });
    }

    @FXML
    public void handleSend() {
        hideError();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Please enter your email address.");
            return;
        }

        sendBtn.setDisable(true);
        sendBtn.setText("Sending...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return resetService.requestReset(email);
            }
        };
        task.setOnSucceeded(e -> {
            sendBtn.setDisable(false);
            sendBtn.setText("Send reset code");
            Boolean result = task.getValue();
            if (Boolean.TRUE.equals(result)) {
                openResetScreen(email);
            } else {
                showError("No account found with that email address.");
            }
        });
        task.setOnFailed(e -> {
            sendBtn.setDisable(false);
            sendBtn.setText("Send reset code");
            Throwable ex = task.getException();
            ex.printStackTrace();
            showError("Could not send email: " + ex.getMessage());
        });
        new Thread(task, "password-reset-request").start();
    }

    @FXML
    public void goBackToLogin() {
        navigate("/fxml/Login.fxml", "Sign in - SkillORA");
    }

    private void openResetScreen(String email) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResetPassword.fxml"));
            Parent root = loader.load();
            ResetPasswordController controller = loader.getController();
            controller.setEmail(email);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Reset password - SkillORA");
        } catch (IOException e) {
            e.printStackTrace();
            showError("Could not open reset screen.");
        }
    }

    private void navigate(String fxml, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
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
