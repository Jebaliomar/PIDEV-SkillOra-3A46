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
import javafx.scene.layout.HBox;
import tn.esprit.entities.Evaluation;
import tn.esprit.services.EvaluationService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class ListeEvaluationController {

    @FXML
    private TableView<Evaluation> evaluationTable;

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
    private TableColumn<Evaluation, String> docxCol;

    @FXML
    private TableColumn<Evaluation, String> actionCol;

    @FXML
    private ComboBox<String> typeFilterCombo;

    private final EvaluationService service = new EvaluationService();

    @FXML
    public void initialize() {
        evaluationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        typeFilterCombo.getItems().addAll("Tous", "EXAM", "QUIZ");
        typeFilterCombo.setValue("Tous");

        configurerColonneTitre();
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("totalScore"));

        configurerColonneDocx();
        configurerColonneActions();

        chargerEvaluations();
    }

    private void configurerColonneTitre() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        titleCol.setCellFactory(col -> new TableCell<>() {
            private final Hyperlink link = new Hyperlink();

            {
                link.setOnAction(e -> {
                    Evaluation evaluation = getTableView().getItems().get(getIndex());

                    if ("QUIZ".equalsIgnoreCase(evaluation.getType())) {
                        ouvrirQuiz(evaluation);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Evaluation evaluation = getTableView().getItems().get(getIndex());

                if ("QUIZ".equalsIgnoreCase(evaluation.getType())) {
                    link.setText(item);
                    setText(null);
                    setGraphic(link);
                } else {
                    setGraphic(null);
                    setText(item);
                }
            }
        });
    }

    private void configurerColonneDocx() {
        docxCol.setCellValueFactory(data -> new SimpleStringProperty("DOCX"));

        docxCol.setCellFactory(param -> new TableCell<>() {
            private final Button btnDocx = new Button("Voir DOCX");

            {
                btnDocx.getStyleClass().add("button-primary");
                btnDocx.setPrefWidth(95);

                btnDocx.setOnAction(event -> {
                    Evaluation evaluation = getTableView().getItems().get(getIndex());
                    ouvrirDocumentDocx(evaluation);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }

                Evaluation evaluation = getTableView().getItems().get(getIndex());

                if ("EXAM".equalsIgnoreCase(evaluation.getType())
                        && evaluation.getDocxPath() != null
                        && !evaluation.getDocxPath().trim().isEmpty()) {
                    setGraphic(btnDocx);
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private void configurerColonneActions() {
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
                    ouvrirFormulaireModification(evaluation);
                });

                btnSupprimer.setOnAction(event -> {
                    Evaluation evaluation = getTableView().getItems().get(getIndex());
                    supprimerDepuisTable(evaluation);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void chargerEvaluations() {
        try {
            List<Evaluation> evaluations = service.recuperer();
            ObservableList<Evaluation> observableList = FXCollections.observableArrayList(evaluations);
            evaluationTable.setItems(observableList);
        } catch (SQLException e) {
            showError("Erreur chargement évaluations : " + e.getMessage());
        }
    }

    @FXML
    private void filtrerParType() {
        try {
            String selectedType = typeFilterCombo.getValue();

            if (selectedType == null || selectedType.equalsIgnoreCase("Tous")) {
                chargerEvaluations();
                return;
            }

            List<Evaluation> evaluations = service.filtrerParType(selectedType);
            evaluationTable.setItems(FXCollections.observableArrayList(evaluations));

        } catch (SQLException e) {
            showError("Erreur filtre type : " + e.getMessage());
        }
    }

    private void ouvrirFormulaireModification(Evaluation evaluation) {
        try {
            EvaluationController.setEvaluationAModifier(evaluation);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterEvaluation.fxml"));
            Parent root = loader.load();

            evaluationTable.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture formulaire : " + e.getMessage());
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
                    showError("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    private void ouvrirDocumentDocx(Evaluation evaluation) {
        try {
            if (evaluation == null || evaluation.getDocxPath() == null || evaluation.getDocxPath().trim().isEmpty()) {
                showError("Aucun document DOCX associé à cette évaluation.");
                return;
            }

            File file = new File(evaluation.getDocxPath());

            if (!file.exists()) {
                showError("Le fichier DOCX est introuvable : " + evaluation.getDocxPath());
                return;
            }

            if (!Desktop.isDesktopSupported()) {
                showError("L'ouverture automatique du document n'est pas supportée sur cette machine.");
                return;
            }

            Desktop.getDesktop().open(file);

        } catch (IOException e) {
            showError("Erreur lors de l'ouverture du document : " + e.getMessage());
        }
    }

    private void ouvrirQuiz(Evaluation evaluation) {
        try {
            QuizController.setEvaluationSelectionnee(evaluation);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QuizView.fxml"));
            Parent root = loader.load();

            evaluationTable.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture quiz : " + e.getMessage());
        }
    }

    @FXML
    private void retourAjout(ActionEvent event) {
        try {
            EvaluationController.setEvaluationAModifier(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterEvaluation.fxml"));
            Parent root = loader.load();

            evaluationTable.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture ajout : " + e.getMessage());
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