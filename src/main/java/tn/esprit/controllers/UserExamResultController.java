package tn.esprit.controllers;

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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.UserEvaluationService;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        if (evaluation == null || userId <= 0) {
            return;
        }
        loadResult();
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
                }
            }
        }

        loadDocxFallback();
    }

    private void loadDocxFallback() {
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

            answerArea.setText(extractAnswer(payload));
            answerArea.setEditable(false);

            feedbackArea.setText(extractFeedback(payload));
            feedbackArea.setEditable(false);

            scoreLabel.setText("Score : " + (ue.getScore() != null ? ue.getScore() : "Non disponible"));
            aiUsageLabel.setText("Usage IA : " + extractInt(payload, "AI_PERCENT:::") + "%");
            plagiarismLabel.setText("Plagiat : " + extractInt(payload, "PLAGIARISM_PERCENT:::") + "%");
            fraudLabel.setText("Fraude : " + (extractBoolean(payload, "FRAUD_ATTEMPT:::") ? "Oui" : "Non"));

        } catch (SQLException e) {
            showError("Erreur chargement résultat examen : " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        closePdfDocument();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent root = loader.load();
            resultNode().getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour résultat examen : " + e.getMessage());
        }
    }

    private javafx.scene.Node resultNode() {
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
        String[] lines = payload.split("\\R");
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                return Boolean.parseBoolean(line.substring(prefix.length()).trim());
            }
        }
        return false;
    }
}