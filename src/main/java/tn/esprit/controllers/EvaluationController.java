package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import tn.esprit.entities.Evaluation;
import tn.esprit.services.EvaluationService;

import java.io.IOException;
import java.sql.SQLException;

public class EvaluationController {

    @FXML
    private TextField idField;

    @FXML
    private TextField titleField;

    @FXML
    private TextField descriptionField;

    @FXML
    private TextField typeField;

    @FXML
    private TextField durationField;

    @FXML
    private TextField scoreField;

    private final EvaluationService service = new EvaluationService();

    private static Evaluation evaluationAModifier;

    public static void setEvaluationAModifier(Evaluation evaluation) {
        evaluationAModifier = evaluation;
    }

    @FXML
    public void initialize() {
        if (evaluationAModifier != null) {
            idField.setText(String.valueOf(evaluationAModifier.getId()));
            titleField.setText(evaluationAModifier.getTitle());
            descriptionField.setText(evaluationAModifier.getDescription());
            typeField.setText(evaluationAModifier.getType());
            durationField.setText(String.valueOf(evaluationAModifier.getDuration()));
            scoreField.setText(String.valueOf(evaluationAModifier.getTotalScore()));
        }
    }

    @FXML
    private void ajouterEvaluation() {
        try {
            Evaluation e = new Evaluation();
            e.setTitle(titleField.getText());
            e.setDescription(descriptionField.getText());
            e.setType(typeField.getText());
            e.setDuration(Integer.parseInt(durationField.getText()));
            e.setTotalScore(Integer.parseInt(scoreField.getText()));
            e.setDocxPath(null);
            e.setPdfPath(null);

            service.ajouter(e);
            showInfo("Succès", "Evaluation ajoutée avec succès !");
            viderChamps();

        } catch (NumberFormatException ex) {
            showError("Duration et Total Score doivent être des nombres.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void modifierEvaluation() {
        try {
            if (idField.getText() == null || idField.getText().isEmpty()) {
                showError("Sélectionne une évaluation à modifier depuis la liste.");
                return;
            }

            Evaluation e = new Evaluation();
            e.setId(Integer.parseInt(idField.getText()));
            e.setTitle(titleField.getText());
            e.setDescription(descriptionField.getText());
            e.setType(typeField.getText());
            e.setDuration(Integer.parseInt(durationField.getText()));
            e.setTotalScore(Integer.parseInt(scoreField.getText()));
            e.setDocxPath(null);
            e.setPdfPath(null);

            service.modifier(e);
            showInfo("Succès", "Evaluation modifiée avec succès !");
            viderChamps();

        } catch (NumberFormatException ex) {
            showError("ID, Duration et Total Score doivent être numériques.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void afficherListe(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ListeEvaluation.fxml"));
            titleField.getScene().setRoot(root);
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void viderChamps() {
        idField.clear();
        titleField.clear();
        descriptionField.clear();
        typeField.clear();
        durationField.clear();
        scoreField.clear();
        evaluationAModifier = null;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}