package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.io.IOException;
import java.net.URL;

public class HangmanThemeResultController {

    @FXML private Label lblTopic;
    @FXML private Label lblLevel;
    @FXML private Label lblSummary;
    @FXML private Label lblStars;

    private int userId;
    private String topic;
    private String level;
    private int correctAnswers;
    private int totalQuestions;

    public void initSessionResult(int userId, String topic, String level, int correctAnswers, int totalQuestions) {
        this.userId = userId;
        this.topic = topic;
        this.level = level;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        loadResult();
    }

    private void loadResult() {
        int stars = Math.max(0, Math.min(3, correctAnswers));

        lblTopic.setText("Topic : " + topic);
        lblLevel.setText("Level : " + level);
        lblSummary.setText("Correct answers in this session : " + correctAnswers + " / " + totalQuestions);
        lblStars.setText(buildStars(stars));
    }

    @FXML
    private void handleReplayTheme() {
        try {
            FXMLLoader loader = new FXMLLoader(requireResource("/HangmanAssessment.fxml"));
            Parent root = loader.load();

            HangmanAssessmentController controller = loader.getController();
            controller.setUserId(userId);

            lblTopic.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur replay thème : " + e.getMessage());
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleBackThemes() {
        try {
            FXMLLoader loader = new FXMLLoader(requireResource("/HangmanAssessment.fxml"));
            Parent root = loader.load();

            HangmanAssessmentController controller = loader.getController();
            controller.setUserId(userId);

            lblTopic.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour mini game Hangman : " + e.getMessage());
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleBackToAssessments() {
        openScene("/UserAssessmentView.fxml");
    }

    private void openScene(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(requireResource(fxmlPath));
            Parent root = loader.load();
            lblTopic.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur ouverture écran : " + e.getMessage());
        } catch (IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    private URL requireResource(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            throw new IllegalStateException("FXML introuvable : " + path);
        }
        return url;
    }

    private String buildStars(int stars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 3; i++) {
            sb.append(i <= stars ? "★ " : "☆ ");
        }
        return sb.toString().trim();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}