package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.IUserEvaluationService;
import tn.esprit.services.UserEvaluationService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class UserExamPassController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private TextArea examContentArea;

    @FXML
    private TextArea answerArea;

    @FXML
    private Button submitButton;

    private Evaluation evaluation;
    private int userId;
    private boolean startedSaved = false;
    private boolean alreadySubmitted = false;

    private Timeline timer;
    private int remainingSeconds;

    private final IUserEvaluationService userEvaluationService = new UserEvaluationService();

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;

        if (evaluation != null) {
            titleLabel.setText("Exam : " + safe(evaluation.getTitle()));
            subtitleLabel.setText("Rédigez votre réponse");
            loadExamDocument();
            ensureStartedSaved();
            loadSubmissionState();

            if (!alreadySubmitted) {
                startTimer();
            } else {
                timerLabel.setText("Done");
            }
        }
    }

    public void setUserId(int userId) {
        this.userId = userId;
        ensureStartedSaved();
        loadSubmissionState();

        if (evaluation != null && !alreadySubmitted && timer == null) {
            startTimer();
        }
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

    private void loadSubmissionState() {
        if (evaluation == null || userId <= 0) {
            return;
        }

        try {
            UserEvaluation ue = userEvaluationService.getByUserIdAndEvaluationId(userId, evaluation.getId());

            if (ue != null && ue.getSubmittedAt() != null) {
                alreadySubmitted = true;

                answerArea.setText(safe(ue.getAiFeedback()));
                answerArea.setEditable(false);
                answerArea.setDisable(false);

                if (submitButton != null) {
                    submitButton.setDisable(true);
                    submitButton.setText("Already submitted");
                }

                subtitleLabel.setText("Examen déjà soumis - Cliquez sur View result pour voir votre résultat");
            }

        } catch (SQLException e) {
            showError("Erreur chargement état examen : " + e.getMessage());
        }
    }

    private void startTimer() {
        if (evaluation == null) {
            return;
        }

        int durationMinutes = evaluation.getDuration() != null ? evaluation.getDuration() : 0;
        remainingSeconds = durationMinutes * 60;
        updateTimerLabel();

        if (timer != null) {
            timer.stop();
        }

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds--;
            updateTimerLabel();

            if (remainingSeconds <= 0) {
                timer.stop();
                submitExamAutomatically();
            }
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimerLabel() {
        int minutes = Math.max(remainingSeconds, 0) / 60;
        int seconds = Math.max(remainingSeconds, 0) % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void submitExamAutomatically() {
        if (alreadySubmitted) {
            return;
        }

        String response = answerArea.getText() != null ? answerArea.getText().trim() : "";

        try {
            userEvaluationService.submitExamResponse(userId, evaluation.getId(), response);

            alreadySubmitted = true;
            answerArea.setEditable(false);

            if (submitButton != null) {
                submitButton.setDisable(true);
                submitButton.setText("Submitted");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Temps écoulé");
            alert.setHeaderText("Examen soumis automatiquement");
            alert.setContentText("Le temps est terminé. Votre réponse a été soumise automatiquement.");
            alert.showAndWait();

            goBackToAssessments();

        } catch (Exception e) {
            showError("Erreur soumission automatique examen : " + e.getMessage());
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
        if (alreadySubmitted) {
            return;
        }

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

            alreadySubmitted = true;
            answerArea.setEditable(false);

            if (submitButton != null) {
                submitButton.setDisable(true);
                submitButton.setText("Submitted");
            }

            if (timer != null) {
                timer.stop();
            }

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
        if (timer != null) {
            timer.stop();
        }
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