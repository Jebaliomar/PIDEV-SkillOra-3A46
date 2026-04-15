package tn.esprit.controllers.auth;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.services.AuthService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

public class LoginController {

    @FXML
    private TextField identifierField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String identifier = identifierField.getText() == null ? "" : identifierField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir l'email/username et le mot de passe.");
            return;
        }

        try {
            Optional<AuthService.AuthResult> authenticated = authService.authenticate(identifier, password);
            if (authenticated.isEmpty()) {
                showError("Identifiants invalides ou compte inactif.");
                return;
            }

            AuthService.AuthResult session = authenticated.get();
            System.setProperty("skillora.userId", String.valueOf(session.userId()));
            System.setProperty("skillora.role", session.role());
            openHomePage((Stage) identifierField.getScene().getWindow(), session.role());
        } catch (SQLException exception) {
            showError("Erreur SQL: " + exception.getMessage());
        } catch (IOException exception) {
            showError("Erreur d'ouverture de page: " + exception.getMessage());
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        identifierField.clear();
        passwordField.clear();
        if (errorLabel != null) {
            errorLabel.setText("");
        }
    }

    private void openHomePage(Stage stage, String role) throws IOException {
        String normalizedRole = role == null ? "student" : role.trim().toLowerCase();
        String fxmlPath = (normalizedRole.contains("prof") || normalizedRole.contains("admin"))
                ? "/tn/esprit/views/availability-slot/availability-slot-list.fxml"
                : "/tn/esprit/views/rendezvous/rendezvous-list.fxml";
        String title = (normalizedRole.contains("prof") || normalizedRole.contains("admin"))
                ? "SkillOra - Availability Slots"
                : "SkillOra - RendezVous";

        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root, 1160, 740));
        stage.setTitle(title);
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.show();
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message == null ? "" : message);
        }
    }
}
