package tn.esprit.controllers;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.ExamEvaluationPayload;
import tn.esprit.services.UserEvaluationService;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

public class UserExamResultController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label scoreLabel;
    @FXML private Label aiUsageLabel;
    @FXML private Label plagiarismLabel;
    @FXML private Label fraudLabel;
    @FXML private Label pdfPageLabel;

    @FXML private TextArea examContentArea;
    @FXML private TextArea answerArea;
    @FXML private TextArea feedbackArea;

    @FXML private ScrollPane pdfScrollPane;
    @FXML private ImageView pdfImageView;
    @FXML private Button openPdfButton;

    private Evaluation evaluation;
    private int userId;

    private PDDocument currentPdfDocument;
    private PDFRenderer pdfRenderer;
    private int currentPdfPageIndex = 0;
    private int totalPdfPages = 0;

    private final UserEvaluationService userEvaluationService = new UserEvaluationService();

    public void setUserId(int userId) {
        this.userId = userId;
        tryLoadResult();
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
        tryLoadResult();
    }

    private void tryLoadResult() {
        if (evaluation == null) {
            return;
        }
        if (!resolveConnectedUser()) {
            return;
        }
        loadResult();
    }

    private boolean resolveConnectedUser() {
        try {
            userId = userEvaluationService.resolveAuthenticatedUserId();
            return true;
        } catch (SQLException e) {
            if (userId > 0) {
                try {
                    userEvaluationService.requireExistingUser(userId);
                    System.out.println("Connected user ID = " + userId);
                    return true;
                } catch (SQLException ignored) {
                    // Fall through to the reconnect message.
                }
            }
            showError(UserEvaluationService.USER_NOT_FOUND_MESSAGE);
            return false;
        }
    }

    private void loadResult() {
        titleLabel.setText("Exam Result : " + safe(evaluation.getTitle()));
        subtitleLabel.setText("Voici votre examen et votre réponse soumise");

        loadExamDocument();
        loadUserAnswer();
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
                } catch (Exception ignored) {
                    // fallback DOCX
                }
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
            showDocxFallback("Aucun fichier PDF ou DOCX trouvé pour cet examen.");
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
                    ? "Le document est vide ou illisible."
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
        examContentArea.setText(content);

        totalPdfPages = 0;
        currentPdfPageIndex = 0;
        updatePdfPageLabel();
    }

    private void renderCurrentPdfPage() {
        if (pdfRenderer == null || currentPdfDocument == null || totalPdfPages == 0) {
            return;
        }

        try {
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(currentPdfPageIndex, 140);
            pdfImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

            if (pdfScrollPane.getViewportBounds().getWidth() > 0) {
                pdfImageView.setFitWidth(pdfScrollPane.getViewportBounds().getWidth() - 40);
            } else {
                pdfImageView.setFitWidth(980);
            }

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
            showError("Aucun PDF disponible pour cet examen.");
            return;
        }

        try {
            File pdfFile = new File(evaluation.getPdfPath());
            if (!pdfFile.exists()) {
                showError("Le fichier PDF est introuvable.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            } else {
                showError("Ouverture externe non supportée sur cette machine.");
            }
        } catch (Exception e) {
            showError("Erreur ouverture PDF : " + e.getMessage());
        }
    }

    private void loadUserAnswer() {
        try {
            UserEvaluation ue = userEvaluationService.getByUserIdAndEvaluationId(userId, evaluation.getId());

            if (ue == null) {
                answerArea.setText("Aucune réponse trouvée.");
                feedbackArea.setText("");
                scoreLabel.setText("Score : -");
                aiUsageLabel.setText("Usage IA : -");
                plagiarismLabel.setText("Plagiat : -");
                fraudLabel.setText("Fraude : -");
                return;
            }

            String payload = ue.getAiFeedback();
            ExamEvaluationPayload result = ExamEvaluationPayload.parse(payload);

            answerArea.setText(result.answer().isBlank() ? "Aucune réponse trouvée." : result.answer());
            answerArea.setEditable(false);

            feedbackArea.setText(result.feedback().isBlank() ? "Aucun feedback disponible." : result.feedback());
            feedbackArea.setEditable(false);

            scoreLabel.setText("Score : " + (ue.getScore() != null ? ue.getScore() : "Non disponible"));
            aiUsageLabel.setText("Usage IA : " + result.aiPercent() + "%");
            plagiarismLabel.setText("Plagiat : " + result.plagiarismPercent() + "% - " + result.plagiarismStatus());
            fraudLabel.setText("Fraude : " + (result.fraudAttempt() ? "Oui" : "Non")
                    + (result.fraudAttempt() && !result.fraudReason().isBlank() ? " - " + result.fraudReason() : ""));

        } catch (SQLException e) {
            showError(userEvaluationService.isUserNotFound(e) ? UserEvaluationService.USER_NOT_FOUND_MESSAGE : "Erreur chargement résultat examen : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        closePdfDocument();
        openScene("/UserAssessmentView.fxml");
    }

    private void openScene(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);

            if (url == null) {
                showError("FXML introuvable : " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            resultNode().getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur retour résultat examen : " + e.getMessage());
        }
    }

    private Node resultNode() {
        if (titleLabel != null) return titleLabel;
        if (answerArea != null) return answerArea;
        if (feedbackArea != null) return feedbackArea;
        return examContentArea;
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

    private String extractAnswer(String payload) {
        return extractBlock(payload, "ANSWER:::", ":::END_ANSWER");
    }

    private String extractFeedback(String payload) {
        return extractBlock(payload, "FEEDBACK:::", ":::END_FEEDBACK");
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

    private int extractInt(String payload, String prefix) {
        try {
            if (payload == null || payload.isBlank()) {
                return 0;
            }

            String[] lines = payload.split("\\R");
            for (String line : lines) {
                if (line.startsWith(prefix)) {
                    return Integer.parseInt(line.substring(prefix.length()).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private boolean extractBoolean(String payload, String prefix) {
        if (payload == null || payload.isBlank()) {
            return false;
        }

        String[] lines = payload.split("\\R");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return Boolean.parseBoolean(line.substring(prefix.length()).trim());
            }
        }
        return false;
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
