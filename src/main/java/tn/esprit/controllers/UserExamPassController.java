package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.IUserEvaluationService;
import tn.esprit.services.UserEvaluationService;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class UserExamPassController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label timerLabel;
    @FXML private Label pdfPageLabel;
    @FXML private Label selectedFileLabel;

    @FXML private TextArea examContentArea;
    @FXML private TextArea answerArea;

    @FXML private ScrollPane pdfScrollPane;
    @FXML private ImageView pdfImageView;

    @FXML private Button submitButton;
    @FXML private Button openPdfButton;

    private Evaluation evaluation;
    private int userId;
    private boolean startedSaved = false;
    private boolean alreadySubmitted = false;

    private Timeline timer;
    private int remainingSeconds;

    private PDDocument currentPdfDocument;
    private PDFRenderer pdfRenderer;
    private int currentPdfPageIndex = 0;
    private int totalPdfPages = 0;

    private File selectedFile;

    private final IUserEvaluationService userEvaluationService = new UserEvaluationService();

    @FXML
    public void initialize() {
        updatePdfPageLabel();

        if (selectedFileLabel != null) {
            selectedFileLabel.setText("Aucun fichier sélectionné");
        }

        if (examContentArea != null) {
            examContentArea.setEditable(false);
            examContentArea.setWrapText(true);
            forceReadableExamTextAreaStyle();
        }
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
        tryInitializeExam();
    }

    public void setUserId(int userId) {
        this.userId = userId;
        tryInitializeExam();
    }

    private void tryInitializeExam() {
        if (evaluation == null || userId <= 0) {
            return;
        }

        titleLabel.setText("Exam : " + safe(evaluation.getTitle()));
        subtitleLabel.setText("Veuillez lire le document et rédiger votre réponse.");

        loadExamDocument();
        ensureStartedSaved();
        loadSubmissionState();

        if (!alreadySubmitted && timer == null) {
            startTimer();
        } else if (alreadySubmitted) {
            timerLabel.setText("Done");
        }
    }

    private void ensureStartedSaved() {
        if (startedSaved || evaluation == null || userId <= 0) {
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

                answerArea.setText(extractAnswerFromPayload(ue.getAiFeedback()));
                answerArea.setEditable(false);

                if (submitButton != null) {
                    submitButton.setDisable(true);
                    submitButton.setText("Already submitted");
                }

                subtitleLabel.setText("Examen déjà soumis");
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

        try {
            submitExamInternal(true);
        } catch (Exception e) {
            showError("Erreur soumission automatique examen : " + e.getMessage());
        }
    }

    private void loadExamDocument() {
        closePdfDocument();

        if (evaluation == null) {
            showDocxFallback("Aucune évaluation sélectionnée.");
            return;
        }

        String pdfPath = evaluation.getPdfPath();
        if (pdfPath != null && !pdfPath.trim().isEmpty()) {
            File pdfFile = new File(pdfPath);

            if (pdfFile.exists()) {
                try {
                    currentPdfDocument = PDDocument.load(pdfFile);
                    pdfRenderer = new PDFRenderer(currentPdfDocument);
                    totalPdfPages = currentPdfDocument.getNumberOfPages();
                    currentPdfPageIndex = 0;

                    pdfScrollPane.setVisible(true);
                    pdfScrollPane.setManaged(true);
                    examContentArea.setVisible(false);
                    examContentArea.setManaged(false);

                    renderCurrentPdfPage();
                    updatePdfPageLabel();
                    return;
                } catch (Exception e) {
                    System.out.println("Impossible de charger le PDF, fallback texte : " + e.getMessage());
                }
            } else {
                System.out.println("PDF introuvable : " + pdfPath);
            }
        }

        loadDocxFallback();
    }

    private void loadDocxFallback() {
        if (evaluation == null) {
            showDocxFallback("Aucune évaluation sélectionnée.");
            return;
        }

        String docxPath = evaluation.getDocxPath();

        if (docxPath == null || docxPath.trim().isEmpty()) {
            StringBuilder fallback = new StringBuilder();

            if (evaluation.getTitle() != null && !evaluation.getTitle().isBlank()) {
                fallback.append("Titre : ").append(evaluation.getTitle()).append("\n\n");
            }

            if (evaluation.getDescription() != null && !evaluation.getDescription().isBlank()) {
                fallback.append("Description : ").append(evaluation.getDescription()).append("\n\n");
            }

            if (fallback.length() == 0) {
                fallback.append("Aucun fichier PDF ou DOCX trouvé pour cet examen.");
            }

            showDocxFallback(fallback.toString());
            return;
        }

        File docxFile = new File(docxPath);

        if (!docxFile.exists()) {
            showDocxFallback("Le fichier DOCX n'existe pas : " + docxPath);
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

            showDocxFallback(content.length() == 0
                    ? "Le document est vide ou ne contient pas de texte lisible."
                    : content.toString());

        } catch (Exception e) {
            showDocxFallback("Erreur lecture DOCX : " + e.getMessage());
        }
    }

    private void showDocxFallback(String content) {
        pdfScrollPane.setVisible(false);
        pdfScrollPane.setManaged(false);

        examContentArea.setVisible(true);
        examContentArea.setManaged(true);
        examContentArea.setEditable(false);
        examContentArea.setWrapText(true);
        examContentArea.setText(content == null ? "" : content);

        forceReadableExamTextAreaStyle();

        totalPdfPages = 0;
        currentPdfPageIndex = 0;
        updatePdfPageLabel();
    }

    private void forceReadableExamTextAreaStyle() {
        examContentArea.setStyle(
                "-fx-text-fill: #111111;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-family: 'Segoe UI';" +
                        "-fx-control-inner-background: white;" +
                        "-fx-background-color: white;" +
                        "-fx-highlight-fill: #dbeafe;" +
                        "-fx-highlight-text-fill: #111111;"
        );
    }

    private void renderCurrentPdfPage() {
        if (pdfRenderer == null || currentPdfDocument == null || totalPdfPages == 0) {
            return;
        }

        try {
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(currentPdfPageIndex, 140);
            pdfImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            pdfImageView.setFitWidth(980);
        } catch (Exception e) {
            showError("Erreur affichage PDF : " + e.getMessage());
        }
    }

    @FXML
    private void handlePreviousPdfPage() {
        if (totalPdfPages > 0 && currentPdfPageIndex > 0) {
            currentPdfPageIndex--;
            renderCurrentPdfPage();
            updatePdfPageLabel();
        }
    }

    @FXML
    private void handleNextPdfPage() {
        if (totalPdfPages > 0 && currentPdfPageIndex < totalPdfPages - 1) {
            currentPdfPageIndex++;
            renderCurrentPdfPage();
            updatePdfPageLabel();
        }
    }

    @FXML
    private void handleOpenPdfExternally() {
        if (evaluation == null || evaluation.getPdfPath() == null || evaluation.getPdfPath().isBlank()) {
            showWarning("Aucun PDF disponible pour cet examen.");
            return;
        }

        try {
            File pdfFile = new File(evaluation.getPdfPath());
            if (!pdfFile.exists()) {
                showWarning("Le fichier PDF est introuvable.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                showWarning("Ouverture externe non supportée sur cette machine.");
            }
        } catch (Exception e) {
            showError("Erreur ouverture PDF : " + e.getMessage());
        }
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.png", "*.jpg", "*.jpeg", "*.zip"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        File chosen = chooser.showOpenDialog(answerArea.getScene().getWindow());

        if (chosen != null) {
            selectedFile = chosen;
            if (selectedFileLabel != null) {
                selectedFileLabel.setText("Fichier sélectionné : " + chosen.getName());
            }
            showInfo("Upload", "Fichier chargé avec succès.");
        }
    }

    @FXML
    private void handleCloudUpload() {
        showInfo("Cloud", "Upload cloud non implémenté pour le moment.");
    }

    @FXML
    private void handleSubmitExam() {
        if (alreadySubmitted) {
            return;
        }

        try {
            submitExamInternal(false);
        } catch (Exception e) {
            showError("Erreur soumission examen : " + e.getMessage());
        }
    }

    private void submitExamInternal(boolean automaticMode) throws Exception {
        if (evaluation == null) {
            showError("Aucune évaluation sélectionnée.");
            return;
        }

        if (userId <= 0) {
            showError("Utilisateur invalide.");
            return;
        }

        String response = answerArea.getText() != null ? answerArea.getText().trim() : "";

        if (response.isEmpty() && selectedFile == null) {
            if (!automaticMode) {
                showWarning("Veuillez écrire une réponse ou ajouter un fichier avant de soumettre.");
                return;
            }
        }

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

        if (automaticMode) {
            showInfo("Temps écoulé", "Le temps est terminé. Votre réponse a été soumise et corrigée automatiquement.");
        } else {
            showInfo("Soumission", "Votre réponse a été soumise et corrigée automatiquement.");
        }

        goBackToAssessments();
    }

    private String extractAnswerFromPayload(String payload) {
        return extractBlock(payload, "ANSWER:::", ":::END_ANSWER");
    }

    private String extractBlock(String payload, String startToken, String endToken) {
        if (payload == null || payload.isBlank()) {
            return "";
        }

        int start = payload.indexOf(startToken);
        int end = payload.indexOf(endToken);

        if (start == -1 || end == -1 || end <= start) {
            return "";
        }

        start += startToken.length();
        return payload.substring(start, end).trim();
    }

    @FXML
    private void handleBack() {
        if (timer != null) {
            timer.stop();
        }
        closePdfDocument();
        goBackToAssessments();
    }

    private void goBackToAssessments() {
        try {
            var url = getClass().getResource("/UserAssessmentView.fxml");

            if (url == null) {
                showError("FXML introuvable : /UserAssessmentView.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur retour page assessments : " + e.getMessage());
        }
    }

    private void updatePdfPageLabel() {
        if (pdfPageLabel == null) return;

        if (totalPdfPages <= 0) {
            pdfPageLabel.setText("Page 0 / 0");
        } else {
            pdfPageLabel.setText("Page " + (currentPdfPageIndex + 1) + " / " + totalPdfPages);
        }
    }

    private void closePdfDocument() {
        try {
            if (currentPdfDocument != null) {
                currentPdfDocument.close();
            }
        } catch (Exception ignored) {
        } finally {
            currentPdfDocument = null;
            pdfRenderer = null;
            totalPdfPages = 0;
            currentPdfPageIndex = 0;
        }
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
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