package tn.esprit.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Evaluation;
import tn.esprit.services.EvaluationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ListeEvaluationController {

    @FXML
    private TableView<Evaluation> evaluationTable;

    @FXML
    private TableColumn<Evaluation, Integer> idCol;

    @FXML
    private TableColumn<Evaluation, String> titleCol;

    @FXML
    private TableColumn<Evaluation, String> descriptionCol;

    @FXML
    private TableColumn<Evaluation, String> typeCol;

    @FXML
    private TableColumn<Evaluation, Integer> durationCol;

    @FXML
    private TableColumn<Evaluation, Integer> scoreCol;

    @FXML
    private TableColumn<Evaluation, String> actionCol;

    @FXML
    private TextField searchField;

    private final EvaluationService service = new EvaluationService();

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("totalScore"));

        actionCol.setCellValueFactory(data -> new SimpleStringProperty("Actions"));
        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnSupprimer = new Button("Supprimer");
            private final HBox box = new HBox(10, btnModifier, btnSupprimer);

            {
                btnModifier.getStyleClass().add("button-secondary");
                btnSupprimer.getStyleClass().add("button-danger");

                btnModifier.setOnAction(event -> {
                    Evaluation evaluation = getTableView().getItems().get(getIndex());
                    modifierDepuisTable(evaluation);
                });

                btnSupprimer.setOnAction(event -> {
                    Evaluation evaluation = getTableView().getItems().get(getIndex());
                    supprimerDepuisTable(evaluation);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });

        chargerEvaluations();
    }

    private void chargerEvaluations() {
        try {
            List<Evaluation> evaluations = service.recuperer();
            ObservableList<Evaluation> observableList = FXCollections.observableArrayList(evaluations);
            evaluationTable.setItems(observableList);
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void rechercherEvaluation(KeyEvent event) {
        try {
            String motCle = searchField.getText();

            if (motCle == null || motCle.trim().isEmpty()) {
                chargerEvaluations();
                return;
            }

            List<Evaluation> evaluations = service.rechercherParMotCle(motCle);
            evaluationTable.setItems(FXCollections.observableArrayList(evaluations));

        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void modifierDepuisTable(Evaluation evaluation) {
        try {
            EvaluationController.setEvaluationAModifier(evaluation);
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterEvaluation.fxml"));
            evaluationTable.getScene().setRoot(root);
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void supprimerDepuisTable(Evaluation evaluation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous supprimer cette évaluation ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.supprimer(evaluation);
                    chargerEvaluations();
                } catch (SQLException e) {
                    showError(e.getMessage());
                }
            }
        });
    }

    @FXML
    private void retourAjout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterEvaluation.fxml"));
            evaluationTable.getScene().setRoot(root);
        } catch (IOException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}