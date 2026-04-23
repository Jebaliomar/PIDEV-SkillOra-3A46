package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.AnswerService;
import tn.esprit.services.QuestionAudioAttemptService;
import tn.esprit.services.QuestionService;
import tn.esprit.services.QuizAudioService;
import tn.esprit.services.UserEvaluationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserQuizResultController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label scoreLabel;
    @FXML private VBox questionContainer;

    private Evaluation evaluation;
    private int userId;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();
    private final UserEvaluationService userEvaluationService = new UserEvaluationService();
    private final QuizAudioService quizAudioService = new QuizAudioService();
    private final QuestionAudioAttemptService questionAudioAttemptService = new QuestionAudioAttemptService();

    private boolean audioUnavailableMessageShown = false;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
        quizAudioService.testAvailability();
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

                HBox header = new HBox(10);
                Label questionLabel = new Label("Q" + i + ". " + safe(q.getContent()));
                questionLabel.getStyleClass().add("question-label");
                questionLabel.setWrapText(true);
                questionLabel.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(questionLabel, Priority.ALWAYS);

                Button questionAudioButton = buildAudioButton("🔊 Question", () -> {
                    playAudio(q.getId(), q.getContent());
                });

                header.getChildren().addAll(questionLabel, questionAudioButton);

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

                VBox optionsAudioBox = new VBox(8);
                for (Answer a : answers) {
                    HBox optionRow = new HBox(10);

                    Label optionLabel = new Label("- " + safe(a.getContent()));
                    optionLabel.setWrapText(true);
                    optionLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(optionLabel, Priority.ALWAYS);

                    Button optionAudioButton = buildAudioButton("🔊", () -> {
                        playAudio(q.getId(), a.getContent());
                    });

                    optionRow.getChildren().addAll(optionLabel, optionAudioButton);
                    optionsAudioBox.getChildren().add(optionRow);
                }

                card.getChildren().addAll(header, optionsAudioBox, yourAnswerLabel, correctAnswerLabel, explanationLabel);
                questionContainer.getChildren().add(card);
                i++;
            }

        } catch (SQLException e) {
            showError("Erreur chargement résultat quiz : " + e.getMessage());
        }
    }

    private Button buildAudioButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("audio-button");
        button.setOnAction(e -> action.run());

        if (!quizAudioService.isAudioAvailable()) {
            button.setDisable(true);
            button.setText("Audio indisponible");
        }

        return button;
    }

    private void playAudio(int questionId, String text) {
        if (!quizAudioService.isAudioAvailable()) {
            if (!audioUnavailableMessageShown) {
                audioUnavailableMessageShown = true;
                showWarning("Audio indisponible sur cette machine. Aucun moteur TTS local n'a été trouvé.");
            }
            return;
        }

        boolean started = quizAudioService.speakAsync(text);

        if (!started) {
            if (!audioUnavailableMessageShown) {
                audioUnavailableMessageShown = true;
                showWarning(quizAudioService.getLastErrorMessage());
            }
            return;
        }

        questionAudioAttemptService.incrementPlayCount(userId, questionId);
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
        quizAudioService.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour résultats quiz : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}