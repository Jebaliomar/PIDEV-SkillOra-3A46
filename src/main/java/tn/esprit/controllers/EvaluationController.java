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

    @FXML
    public void initialize() {
        System.out.println("Vue AjouterEvaluation chargée");
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

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Evaluation ajoutée avec succès !");
            alert.show();

            clearFields();

        } catch (NumberFormatException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Duration et Total Score doivent être des nombres.");
            alert.show();

        } catch (IllegalArgumentException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.show();

        } catch (SQLException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur SQL");
            alert.setHeaderText(null);
            alert.setContentText(ex.getMessage());
            alert.show();
        }
    }

    @FXML
    private void afficherListe(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ListeEvaluation.fxml"));
            titleField.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void clearFields() {
        titleField.clear();
        descriptionField.clear();
        typeField.clear();
        durationField.clear();
        scoreField.clear();
    }
}