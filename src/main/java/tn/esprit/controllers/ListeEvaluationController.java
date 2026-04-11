package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.Evaluation;
import tn.esprit.services.EvaluationService;

import java.io.IOException;
import java.sql.SQLException;

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

    private final EvaluationService service = new EvaluationService();

    @FXML
    public void initialize() {
        try {
            ObservableList<Evaluation> observableList =
                    FXCollections.observableArrayList(service.recuperer());

            idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
            descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
            typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
            durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
            scoreCol.setCellValueFactory(new PropertyValueFactory<>("totalScore"));

            evaluationTable.setItems(observableList);

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @FXML
    private void retourAjout(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterEvaluation.fxml"));
            evaluationTable.getScene().setRoot(root);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}