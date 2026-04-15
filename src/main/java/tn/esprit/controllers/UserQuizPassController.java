package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.AnswerService;
import tn.esprit.services.QuestionService;
import tn.esprit.services.UserEvaluationService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserQuizPassController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label timerLabel;
    @FXML private VBox questionContainer;
    @FXML private Button submitButton;
    @FXML private Button backButton;

    private Evaluation evaluation;
    private int userId;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();
    private final UserEvaluationService userEvaluationService = new UserEvaluationService();

    private final Map<Integer, ToggleGroup> answersMap = new HashMap<>();

    private Timeline timer;
    private int remainingSeconds;
    private boolean alreadySubmitted = false;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;

        try {
            userEvaluationService.createStartedUserEvaluation(userId, evaluation.getId());
        } catch (SQLException e) {
            showError("Erreur création suivi quiz : " + e.getMessage());
        }

        loadQuiz();
        loadSubmissionState();

        if (!alreadySubmitted) {
            startTimer();
        } else {
            timerLabel.setText("Done");
        }
    }

    private void loadSubmissionState() {
        try {
            UserEvaluation ue = userEvaluationService.getByUserIdAndEvaluationId(userId, evaluation.getId());

            if (ue != null && ue.getSubmittedAt() != null) {
                alreadySubmitted = true;
                submitButton.setDisable(true);
                submitButton.setText("Already submitted");

                int total = getTotalQuestions();
                int score = ue.getScore() != null ? ue.getScore() : 0;
                subtitleLabel.setText("Quiz déjà complété - Cliquez sur View result pour voir la correction");

                disableAllAnswers();
            }

        } catch (SQLException e) {
            showError("Erreur lors du chargement de l'état du quiz : " + e.getMessage());
        }
    }

    private void disableAllAnswers() {
        for (ToggleGroup group : answersMap.values()) {
            for (Toggle toggle : group.getToggles()) {
                if (toggle instanceof RadioButton rb) {
                    rb.setDisable(true);
                }
            }
        }
    }

    private void loadQuiz() {
        titleLabel.setText("Quiz : " + evaluation.getTitle());
        subtitleLabel.setText("Répondez aux questions avant la fin du temps");

        questionContainer.getChildren().clear();
        answersMap.clear();

        try {
            List<Question> questions = questionService.filtrerParEvaluationId(evaluation.getId());

            int i = 1;
            for (Question q : questions) {
                VBox box = new VBox(10);
                box.getStyleClass().add("question-card");
                box.setPadding(new Insets(16));

                Label questionLabel = new Label("Q" + i + ". " + q.getContent());
                questionLabel.getStyleClass().add("question-label");
                questionLabel.setWrapText(true);

                ToggleGroup group = new ToggleGroup();
                answersMap.put(q.getId(), group);

                List<Answer> answers = answerService.recupererOptionsQuizParQuestion(q.getId());

                box.getChildren().add(questionLabel);

                for (Answer a : answers) {
                    RadioButton rb = new RadioButton(a.getContent());
                    rb.setUserData(a);
                    rb.setToggleGroup(group);
                    rb.setWrapText(true);
                    rb.setMaxWidth(Double.MAX_VALUE);
                    box.getChildren().add(rb);
                }

                questionContainer.getChildren().add(box);
                i++;
            }

        } catch (SQLException e) {
            showError("Erreur lors du chargement du quiz : " + e.getMessage());
        }
    }

    private void startTimer() {
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
                submitQuiz(true);
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

    @FXML
    private void handleSubmitQuiz() {
        submitQuiz(false);
    }

    private void submitQuiz(boolean automatic) {
        if (alreadySubmitted) {
            return;
        }

        int score = 0;

        for (Map.Entry<Integer, ToggleGroup> entry : answersMap.entrySet()) {
            Toggle selected = entry.getValue().getSelectedToggle();

            if (selected != null) {
                Answer a = (Answer) selected.getUserData();
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    score++;
                }
            }
        }

        try {
            saveUserEvaluation(score);
        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement du score : " + e.getMessage());
            return;
        }

        alreadySubmitted = true;
        submitButton.setDisable(true);
        submitButton.setText("Submitted");

        if (timer != null) {
            timer.stop();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat");
        alert.setHeaderText(automatic ? "Temps écoulé" : "Quiz soumis");
        alert.setContentText("Votre score = " + score + "/" + getTotalQuestions());
        alert.showAndWait();

        goBackToAssessmentPage();
    }

    private void saveUserEvaluation(int score) throws SQLException {
        UserEvaluation ue = userEvaluationService.getByUserIdAndEvaluationId(userId, evaluation.getId());

        if (ue == null) {
            ue = new UserEvaluation();
            ue.setUserId(userId);
            ue.setEvaluationId(evaluation.getId());
            ue.setStartedAt(LocalDateTime.now());
        }

        ue.setSubmittedAt(LocalDateTime.now());
        ue.setScore(score);
        ue.setAiFeedback(buildSelectedAnswersPayload());
        ue.setIsCorrected(true);

        userEvaluationService.saveOrUpdate(ue);
    }
    private int getTotalQuestions() {
        return answersMap.size();
    }

    @FXML
    private void handleBack() {
        if (timer != null) {
            timer.stop();
        }
        goBackToAssessmentPage();
    }

    private void goBackToAssessmentPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent root = loader.load();
            titleLabel.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Impossible de retourner à la page des évaluations : " + e.getMessage());
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    private String buildSelectedAnswersPayload() {
        StringBuilder sb = new StringBuilder("QUIZ_ANSWERS|");

        boolean first = true;
        for (Map.Entry<Integer, ToggleGroup> entry : answersMap.entrySet()) {
            Toggle selected = entry.getValue().getSelectedToggle();

            if (selected != null) {
                Answer a = (Answer) selected.getUserData();

                if (!first) {
                    sb.append(",");
                }

                sb.append(entry.getKey()).append(":").append(a.getId());
                first = false;
            }
        }

        return sb.toString();
    }
}