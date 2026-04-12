package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import tn.esprit.entities.Evaluation;
import tn.esprit.services.IUserEvaluationService;
import tn.esprit.services.UserEvaluationService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class UserExamPassController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private TextArea examContentArea;

    @FXML
    private TextArea answerArea;

    private Evaluation evaluation;
    private int userId;
    private boolean startedSaved = false;

    private final IUserEvaluationService userEvaluationService = new UserEvaluationService();

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;

        if (evaluation != null) {
            titleLabel.setText("Exam : " + safe(evaluation.getTitle()));
            subtitleLabel.setText("Rédigez votre réponse");
            loadExamDocument();
            ensureStartedSaved();
        }
    }

    public void setUserId(int userId) {
        this.userId = userId;
        ensureStartedSaved();
    }

    private void ensureStartedSaved() {
        if (startedSaved) {
            return;
        }

        if (evaluation == null || userId <= 0) {
            return;
        }

        try {
            userEvaluationService.createStartedUserEvaluation(userId, evaluation.getId());
            startedSaved = true;
        } catch (Exception e) {
            showError("Erreur création démarrage examen : " + e.getMessage());
        }
    }

    private void loadExamDocument() {
        if (evaluation == null) {
            examContentArea.setText("Aucune évaluation sélectionnée.");
            return;
        }

        String docxPath = evaluation.getDocxPath();

        if (docxPath == null || docxPath.trim().isEmpty()) {
            examContentArea.setText("Aucun fichier DOCX trouvé pour cet examen.");
            return;
        }

        File docxFile = new File(docxPath);

        if (!docxFile.exists()) {
            examContentArea.setText("Le fichier DOCX n'existe pas : " + docxPath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(docxFile);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder content = new StringBuilder();

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    content.append(text).append("\n\n");
                }
            }

            if (content.length() == 0) {
                examContentArea.setText("Le document est vide ou ne contient pas de texte lisible.");
            } else {
                examContentArea.setText(content.toString());
            }

        } catch (Exception e) {
            examContentArea.setText("Erreur lecture DOCX : " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmitExam() {
        if (evaluation == null) {
            showError("Aucune évaluation sélectionnée.");
            return;
        }

        if (userId <= 0) {
            showError("Utilisateur invalide.");
            return;
        }

        String response = answerArea.getText() != null ? answerArea.getText().trim() : "";

        if (response.isEmpty()) {
            showWarning("Veuillez écrire une réponse avant de soumettre.");
            return;
        }

        try {
            userEvaluationService.submitExamResponse(userId, evaluation.getId(), response);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Soumission");
            alert.setHeaderText("Examen soumis");
            alert.setContentText("Votre réponse a été soumise avec succès.");
            alert.showAndWait();

            goBackToAssessments();

        } catch (Exception e) {
            showError("Erreur soumission examen : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        goBackToAssessments();
    }

    private void goBackToAssessments() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour page assessments : " + e.getMessage());
        }
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}