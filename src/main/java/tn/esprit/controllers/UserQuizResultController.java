package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.AnswerService;
import tn.esprit.services.QuestionService;
import tn.esprit.services.UserEvaluationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserQuizResultController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label scoreLabel;

    @FXML
    private VBox questionContainer;

    private Evaluation evaluation;
    private int userId;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();
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

        titleLabel.setText("Quiz Result : " + evaluation.getTitle());
        subtitleLabel.setText("Voici vos réponses et la correction");
        questionContainer.getChildren().clear();

        try {
            UserEvaluation ue = userEvaluationService.getByUserIdAndEvaluationId(userId, evaluation.getId());

            int score = 0;
            if (ue != null && ue.getScore() != null) {
                score = ue.getScore();
            }

            List<Question> questions = questionService.filtrerParEvaluationId(evaluation.getId());
            scoreLabel.setText("Score: " + score + "/" + questions.size());

            Map<Integer, Integer> selectedAnswersMap = parseSelectedAnswers(
                    ue != null ? ue.getAiFeedback() : null
            );

            int i = 1;
            for (Question q : questions) {
                VBox card = new VBox(10);
                card.getStyleClass().add("question-card");
                card.setPadding(new Insets(16));

                Label questionLabel = new Label("Q" + i + ". " + safe(q.getContent()));
                questionLabel.getStyleClass().add("question-label");
                questionLabel.setWrapText(true);

                List<Answer> answers = answerService.recupererOptionsQuizParQuestion(q.getId());

                String userAnswerText = "Aucune réponse";
                String correctAnswerText = "Aucune";
                boolean correct = false;

                Integer selectedAnswerId = selectedAnswersMap.get(q.getId());

                for (Answer a : answers) {
                    if (selectedAnswerId != null && a.getId() == selectedAnswerId) {
                        userAnswerText = safe(a.getContent());
                    }

                    if (Boolean.TRUE.equals(a.getIsCorrect())) {
                        correctAnswerText = safe(a.getContent());
                        if (selectedAnswerId != null && a.getId() == selectedAnswerId) {
                            correct = true;
                        }
                    }
                }

                Label yourAnswerLabel = new Label("Votre réponse : " + userAnswerText);
                yourAnswerLabel.setWrapText(true);
                yourAnswerLabel.setStyle(correct
                        ? "-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;"
                        : "-fx-text-fill: #f87171; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label correctAnswerLabel = new Label("Bonne réponse : " + correctAnswerText);
                correctAnswerLabel.setWrapText(true);
                correctAnswerLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 14px; -fx-font-weight: bold;");

                Label explanationLabel = new Label("Correction : " + safe(q.getExplanation()));
                explanationLabel.setWrapText(true);
                explanationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

                card.getChildren().addAll(questionLabel, yourAnswerLabel, correctAnswerLabel, explanationLabel);
                questionContainer.getChildren().add(card);
                i++;
            }

        } catch (SQLException e) {
            showError("Erreur chargement résultat quiz : " + e.getMessage());
        }
    }

    private Map<Integer, Integer> parseSelectedAnswers(String raw) {
        Map<Integer, Integer> map = new HashMap<>();

        if (raw == null || raw.isBlank()) {
            return map;
        }

        if (!raw.startsWith("QUIZ_ANSWERS|")) {
            return map;
        }

        String content = raw.substring("QUIZ_ANSWERS|".length());

        if (content.isBlank()) {
            return map;
        }

        String[] pairs = content.split(",");

        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                try {
                    int questionId = Integer.parseInt(parts[0].trim());
                    int answerId = Integer.parseInt(parts[1].trim());
                    map.put(questionId, answerId);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return map;
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour résultats quiz : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}