package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.UserEvaluationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class UserExamResultController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private TextArea examContentArea;

    @FXML
    private TextArea answerArea;

    private Evaluation evaluation;
    private int userId;

    private final UserEvaluationService userEvaluationService = new UserEvaluationService();

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
        loadResult();
    }

    private void loadResult() {
        if (evaluation == null) {
            return;
        }

        titleLabel.setText("Exam Result : " + safe(evaluation.getTitle()));
        subtitleLabel.setText("Voici votre examen et votre réponse soumise");

        loadExamDocument();
        loadUserAnswer();
    }

    private void loadExamDocument() {
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

            examContentArea.setText(content.length() == 0
                    ? "Le document est vide ou illisible."
                    : content.toString());

        } catch (Exception e) {
            examContentArea.setText("Erreur lecture DOCX : " + e.getMessage());
        }
    }

    private void loadUserAnswer() {
        try {
            UserEvaluation ue = userEvaluationService.getByUserIdAndEvaluationId(userId, evaluation.getId());

            if (ue == null) {
                answerArea.setText("Aucune réponse trouvée.");
                scoreLabel.setText("Score : -");
                return;
            }

            answerArea.setText(safe(ue.getAiFeedback()));
            answerArea.setEditable(false);

            if (ue.getScore() != null) {
                scoreLabel.setText("Score : " + ue.getScore());
            } else {
                scoreLabel.setText("Score : Non disponible");
            }

        } catch (SQLException e) {
            showError("Erreur chargement résultat examen : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour résultat examen : " + e.getMessage());
        }
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