package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import tn.esprit.entities.Evaluation;
import tn.esprit.services.EvaluationService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class EvaluationController {

    @FXML
    private TextField idField;

    @FXML
    private TextField titleField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField durationField;

    @FXML
    private TextField scoreField;

    @FXML
    private Label titleErrorLabel;

    @FXML
    private Label descriptionErrorLabel;

    @FXML
    private Label typeErrorLabel;

    @FXML
    private Label durationErrorLabel;

    @FXML
    private Label scoreErrorLabel;

    @FXML
    private Button btnUploadDocx;

    @FXML
    private Label docxLabel;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnModifier;

    private final EvaluationService service = new EvaluationService();

    private static Evaluation evaluationAModifier;

    private String docxPath;

    public static void setEvaluationAModifier(Evaluation evaluation) {
        evaluationAModifier = evaluation;
    }

    @FXML
    public void initialize() {
        clearErrorLabels();

        typeComboBox.getItems().clear();
        typeComboBox.getItems().addAll("QUIZ", "EXAM");

        typeComboBox.setOnAction(event -> gererAffichageUpload());

        if (evaluationAModifier != null) {
            chargerEvaluationPourModification();
        } else {
            modeAjout();
        }
    }

    private void chargerEvaluationPourModification() {
        idField.setText(String.valueOf(evaluationAModifier.getId()));
        titleField.setText(evaluationAModifier.getTitle());
        descriptionField.setText(evaluationAModifier.getDescription());
        typeComboBox.setValue(evaluationAModifier.getType());
        durationField.setText(String.valueOf(evaluationAModifier.getDuration()));
        scoreField.setText(String.valueOf(evaluationAModifier.getTotalScore()));

        docxPath = evaluationAModifier.getDocxPath();

        gererAffichageUpload();

        if (docxPath != null && !docxPath.trim().isEmpty()) {
            File file = new File(docxPath);
            docxLabel.setText("Fichier : " + file.getName());
        }

        btnAjouter.setVisible(false);
        btnAjouter.setManaged(false);

        btnModifier.setVisible(true);
        btnModifier.setManaged(true);
    }

    private void modeAjout() {
        btnAjouter.setVisible(true);
        btnAjouter.setManaged(true);

        btnModifier.setVisible(false);
        btnModifier.setManaged(false);
    }

    private void gererAffichageUpload() {
        String selectedType = typeComboBox.getValue();

        if ("EXAM".equals(selectedType)) {
            btnUploadDocx.setVisible(true);
            btnUploadDocx.setManaged(true);
            docxLabel.setVisible(true);
            docxLabel.setManaged(true);
        } else {
            btnUploadDocx.setVisible(false);
            btnUploadDocx.setManaged(false);
            docxLabel.setVisible(false);
            docxLabel.setManaged(false);
            docxLabel.setText("");
            docxPath = null;
        }
    }

    @FXML
    private void uploadDocx() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un fichier DOCX");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Word", "*.docx")
        );

        File file = fileChooser.showOpenDialog(btnUploadDocx.getScene().getWindow());

        if (file != null) {
            docxPath = file.getAbsolutePath();
            docxLabel.setText("Fichier : " + file.getName());
            typeErrorLabel.setText("");
            typeComboBox.setStyle("");
        }
    }

    @FXML
    private void ajouterEvaluation() {
        clearErrorLabels();
        resetFieldStyles();

        if (!validerChamps()) {
            return;
        }

        try {
            Evaluation e = construireEvaluationDepuisFormulaire(false);
            service.ajouter(e);

            showInfo("Succès", "Evaluation ajoutée avec succès !");
            viderChamps();

        } catch (SQLException ex) {
            showError("Erreur SQL : " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void modifierEvaluation() {
        clearErrorLabels();
        resetFieldStyles();

        if (idField.getText() == null || idField.getText().trim().isEmpty()) {
            showError("Aucune évaluation sélectionnée pour modification.");
            return;
        }

        if (!validerChamps()) {
            return;
        }

        try {
            Evaluation e = construireEvaluationDepuisFormulaire(true);
            service.modifier(e);

            showInfo("Succès", "Evaluation modifiée avec succès !");
            viderChamps();

        } catch (NumberFormatException ex) {
            showError("ID invalide.");
        } catch (SQLException ex) {
            showError("Erreur SQL : " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private Evaluation construireEvaluationDepuisFormulaire(boolean avecId) {
        Evaluation e = new Evaluation();

        if (avecId) {
            e.setId(Integer.parseInt(idField.getText().trim()));
        }

        e.setTitle(titleField.getText().trim());
        e.setDescription(descriptionField.getText().trim());
        e.setType(typeComboBox.getValue());
        e.setDuration(Integer.parseInt(durationField.getText().trim()));
        e.setTotalScore(Integer.parseInt(scoreField.getText().trim()));
        e.setDocxPath(docxPath);
        e.setPdfPath(null);

        return e;
    }

    private boolean validerChamps() {
        boolean isValid = true;

        String title = titleField.getText() != null ? titleField.getText().trim() : "";
        String description = descriptionField.getText() != null ? descriptionField.getText().trim() : "";
        String type = typeComboBox.getValue();
        String durationText = durationField.getText() != null ? durationField.getText().trim() : "";
        String scoreText = scoreField.getText() != null ? scoreField.getText().trim() : "";

        if (title.isEmpty()) {
            titleErrorLabel.setText("Le titre est obligatoire.");
            titleField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else if (title.length() < 3) {
            titleErrorLabel.setText("Le titre doit contenir au moins 3 caractères.");
            titleField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        if (description.isEmpty()) {
            descriptionErrorLabel.setText("La description est obligatoire.");
            descriptionField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else if (description.length() < 5) {
            descriptionErrorLabel.setText("La description doit contenir au moins 5 caractères.");
            descriptionField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        if (type == null || type.trim().isEmpty()) {
            typeErrorLabel.setText("Le type est obligatoire.");
            typeComboBox.setStyle("-fx-border-color: red;");
            isValid = false;
        } else if (!type.equals("QUIZ") && !type.equals("EXAM")) {
            typeErrorLabel.setText("Le type doit être QUIZ ou EXAM.");
            typeComboBox.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        if ("EXAM".equals(type) && (docxPath == null || docxPath.trim().isEmpty())) {
            typeErrorLabel.setText("Veuillez uploader un fichier DOCX pour EXAM.");
            typeComboBox.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        if (durationText.isEmpty()) {
            durationErrorLabel.setText("La durée est obligatoire.");
            durationField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else {
            try {
                int duration = Integer.parseInt(durationText);
                if (duration <= 0) {
                    durationErrorLabel.setText("La durée doit être supérieure à 0.");
                    durationField.setStyle("-fx-border-color: red;");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                durationErrorLabel.setText("La durée doit être un nombre entier.");
                durationField.setStyle("-fx-border-color: red;");
                isValid = false;
            }
        }

        if (scoreText.isEmpty()) {
            scoreErrorLabel.setText("Le score est obligatoire.");
            scoreField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else {
            try {
                int score = Integer.parseInt(scoreText);
                if (score < 0) {
                    scoreErrorLabel.setText("Le score ne doit pas être négatif.");
                    scoreField.setStyle("-fx-border-color: red;");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                scoreErrorLabel.setText("Le score doit être un nombre entier.");
                scoreField.setStyle("-fx-border-color: red;");
                isValid = false;
            }
        }

        return isValid;
    }

    private void clearErrorLabels() {
        titleErrorLabel.setText("");
        descriptionErrorLabel.setText("");
        typeErrorLabel.setText("");
        durationErrorLabel.setText("");
        scoreErrorLabel.setText("");
    }

    private void resetFieldStyles() {
        titleField.setStyle("");
        descriptionField.setStyle("");
        typeComboBox.setStyle("");
        durationField.setStyle("");
        scoreField.setStyle("");
    }

    @FXML
    private void afficherListe(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeEvaluation.fxml"));
            Parent root = loader.load();
            titleField.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur ouverture liste : " + e.getMessage());
        }
    }

    @FXML
    private void viderChamps() {
        idField.clear();
        titleField.clear();
        descriptionField.clear();
        durationField.clear();
        scoreField.setText("0");

        typeComboBox.setValue(null);

        docxPath = null;
        docxLabel.setText("");
        docxLabel.setVisible(false);
        docxLabel.setManaged(false);
        btnUploadDocx.setVisible(false);
        btnUploadDocx.setManaged(false);

        clearErrorLabels();
        resetFieldStyles();

        evaluationAModifier = null;
        modeAjout();
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