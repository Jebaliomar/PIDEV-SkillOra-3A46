package tn.esprit.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import tn.esprit.services.PasswordResetService;
import tn.esprit.tools.ThemeManager;

import java.io.IOException;

public class ResetPasswordController {

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetBtn;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private HBox successBox;
    @FXML private Label successLabel;

    private final PasswordResetService resetService = new PasswordResetService();

    public void setEmail(String email) {
        if (emailField != null) emailField.setText(email);
    }

    @FXML
    public void handleReset() {
        hideError();
        hideSuccess();

        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String code = codeField.getText() == null ? "" : codeField.getText().trim();
        String password = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (email.isEmpty() || code.isEmpty() || password == null || password.isEmpty()) {
            showError("Please fill in every field.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters long.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        resetBtn.setDisable(true);
        resetBtn.setText("Resetting...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return resetService.resetPassword(email, code, password);
            }
        };
        task.setOnSucceeded(e -> {
            resetBtn.setDisable(false);
            resetBtn.setText("Reset password");
            if (Boolean.TRUE.equals(task.getValue())) {
                showSuccess("Password updated. Redirecting to sign in...");
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::goBackToLogin);
                }, "reset-redirect").start();
            } else {
                showError("Invalid or expired code.");
            }
        });
        task.setOnFailed(e -> {
            resetBtn.setDisable(false);
            resetBtn.setText("Reset password");
            Throwable ex = task.getException();
            ex.printStackTrace();
            showError("Something went wrong: " + ex.getMessage());
        });
        new Thread(task, "password-reset-apply").start();
    }

    @FXML
    public void goBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Sign in - SkillORA");
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
}
