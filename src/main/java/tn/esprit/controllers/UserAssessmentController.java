package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.IUserEvaluationService;
import tn.esprit.services.QuestionService;
import tn.esprit.services.UserEvaluationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserAssessmentController {

    @FXML
    private VBox evaluationContainer;

    @FXML
    private Button btnAll;
    @FXML
    private Button btnTodo;
    @FXML
    private Button btnCompleted;
    @FXML
    private Button btnExam;
    @FXML
    private Button btnQuiz;

    private final IUserEvaluationService service = new UserEvaluationService();
    private List<Object[]> allData = new ArrayList<>();

    private final int connectedUserId = 1;

    @FXML
    public void initialize() {
        loadData();
        setActiveButton(btnAll);
        applyFilter("ALL");
    }

    private void loadData() {
        try {
            allData = service.getUserEvaluationsWithEvaluation(connectedUserId);
        } catch (SQLException e) {
            showError("Erreur chargement évaluations : " + e.getMessage());
        }
    }

    @FXML
    public void handleAllFilter() {
        setActiveButton(btnAll);
        applyFilter("ALL");
    }

    @FXML
    public void handleTodoFilter() {
        setActiveButton(btnTodo);
        applyFilter("TODO");
    }

    @FXML
    public void handleCompletedFilter() {
        setActiveButton(btnCompleted);
        applyFilter("COMPLETED");
    }

    @FXML
    public void handleExamFilter() {
        setActiveButton(btnExam);
        applyFilter("EXAM");
    }

    @FXML
    public void handleQuizFilter() {
        setActiveButton(btnQuiz);
        applyFilter("QUIZ");
    }

    private void applyFilter(String filter) {
        evaluationContainer.getChildren().clear();

        for (Object[] row : allData) {
            Evaluation evaluation = (Evaluation) row[0];
            UserEvaluation userEvaluation = (UserEvaluation) row[1];

            if (evaluation == null || evaluation.getType() == null) {
                continue;
            }

            String type = evaluation.getType().trim().toUpperCase();
            String status = getStatus(userEvaluation);

            if (!type.equals("EXAM") && !type.equals("QUIZ")) {
                continue;
            }

            boolean show = switch (filter) {
                case "TODO" -> status.equals("TO DO");
                case "COMPLETED" -> status.equals("COMPLETED");
                case "EXAM" -> type.equals("EXAM");
                case "QUIZ" -> type.equals("QUIZ");
                default -> true;
            };

            if (show) {
                evaluationContainer.getChildren().add(createCard(evaluation, userEvaluation, status));
            }
        }
    }

    private String getStatus(UserEvaluation ue) {
        if (ue == null || ue.getSubmittedAt() == null) {
            return "TO DO";
        }
        return "COMPLETED";
    }

    private int getQuestionCountForEvaluation(int evaluationId) {
        try {
            QuestionService questionService = new QuestionService();
            return questionService.filtrerParEvaluationId(evaluationId).size();
        } catch (SQLException e) {
            return 0;
        }
    }

    private VBox createCard(Evaluation evaluation, UserEvaluation userEvaluation, String status) {
        VBox card = new VBox(12);
        card.getStyleClass().add("assessment-card");
        card.setPadding(new Insets(16));

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(evaluation.getTitle() != null ? evaluation.getTitle() : "Sans titre");
        title.getStyleClass().add("card-title");

        Label typeBadge = new Label(evaluation.getType() != null ? evaluation.getType().toUpperCase() : "TYPE");
        typeBadge.getStyleClass().add("badge");
        if ("EXAM".equalsIgnoreCase(evaluation.getType())) {
            typeBadge.getStyleClass().add("badge-exam");
        } else {
            typeBadge.getStyleClass().add("badge-quiz");
        }

        Label statusBadge = new Label(status);
        statusBadge.getStyleClass().add("badge");
        if ("COMPLETED".equals(status)) {
            statusBadge.getStyleClass().add("badge-completed");
        } else {
            statusBadge.getStyleClass().add("badge-todo");
        }

        topRow.getChildren().addAll(title, typeBadge, statusBadge);

        Label description = new Label(
                evaluation.getDescription() != null ? evaluation.getDescription() : "Aucune description"
        );
        description.getStyleClass().add("card-description");
        description.setWrapText(true);

        Label duration = new Label("Duration: " + (evaluation.getDuration() != null ? evaluation.getDuration() : 0) + " minutes");
        duration.getStyleClass().add("card-duration");

        VBox leftInfo = new VBox(6);
        leftInfo.getChildren().add(duration);

        if ("COMPLETED".equals(status) && userEvaluation != null) {
            int totalQuestions = getQuestionCountForEvaluation(evaluation.getId());
            int score = userEvaluation.getScore() != null ? userEvaluation.getScore().intValue() : 0;

            Label scoreLabel = new Label("Score: " + score + "/" + totalQuestions);
            scoreLabel.getStyleClass().add("card-score");
            leftInfo.getChildren().add(scoreLabel);
        }

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button actionButton = new Button();
        actionButton.getStyleClass().add("take-button");

        if ("COMPLETED".equals(status)) {
            actionButton.setText("View result");
        } else {
            actionButton.setText("Take assessment");
        }

        actionButton.setOnAction(event -> {
            if ("TO DO".equals(status)) {
                openAssessmentPage(evaluation);
            } else {
                openResultPage(evaluation, userEvaluation);
            }
        });

        bottomRow.getChildren().addAll(leftInfo, spacer, actionButton);
        card.getChildren().addAll(topRow, description, bottomRow);

        return card;
    }

    private void openAssessmentPage(Evaluation evaluation) {
        try {
            if ("QUIZ".equalsIgnoreCase(evaluation.getType())) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserQuizPassView.fxml"));
                Parent root = loader.load();

                UserQuizPassController controller = loader.getController();
                controller.setUserId(connectedUserId);
                controller.setEvaluation(evaluation);

                evaluationContainer.getScene().setRoot(root);

            } else if ("EXAM".equalsIgnoreCase(evaluation.getType())) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserExamPassView.fxml"));
                Parent root = loader.load();

                UserExamPassController controller = loader.getController();
                controller.setEvaluation(evaluation);
                controller.setUserId(connectedUserId);

                evaluationContainer.getScene().setRoot(root);
            }
        } catch (IOException e) {
            showError("Erreur ouverture évaluation : " + e.getMessage());
        }
    }

    private void openResultPage(Evaluation evaluation, UserEvaluation userEvaluation) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Résultat");
        alert.setHeaderText(evaluation.getTitle());

        int totalQuestions = getQuestionCountForEvaluation(evaluation.getId());
        int score = 0;

        if (userEvaluation != null && userEvaluation.getScore() != null) {
            score = userEvaluation.getScore().intValue();
        }

        String message = "Type : " + evaluation.getType()
                + "\nScore : " + score + "/" + totalQuestions;

        if (userEvaluation != null) {
            message += "\nFeedback : " + (userEvaluation.getAiFeedback() != null ? userEvaluation.getAiFeedback() : "Aucun");
        }

        alert.setContentText(message);
        alert.show();
    }

    private void setActiveButton(Button activeButton) {
        btnAll.getStyleClass().remove("active-filter");
        btnTodo.getStyleClass().remove("active-filter");
        btnCompleted.getStyleClass().remove("active-filter");
        btnExam.getStyleClass().remove("active-filter");
        btnQuiz.getStyleClass().remove("active-filter");

        if (!activeButton.getStyleClass().contains("active-filter")) {
            activeButton.getStyleClass().add("active-filter");
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}