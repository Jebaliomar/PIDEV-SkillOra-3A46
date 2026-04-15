package tn.esprit.controllers;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.services.AnswerService;
import tn.esprit.services.QuestionService;
import tn.esprit.services.UserEvaluationService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserResponsesController {

    @FXML
    private Label titleLabel;

    @FXML
    private TableView<UserEvaluation> responseTable;

    @FXML
    private TableColumn<UserEvaluation, String> userCol;

    @FXML
    private TableColumn<UserEvaluation, String> dateCol;

    @FXML
    private TableColumn<UserEvaluation, Integer> scoreCol;

    @FXML
    private TableColumn<UserEvaluation, String> actionCol;

    private final UserEvaluationService userEvaluationService = new UserEvaluationService();
    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final Map<Integer, String> userNamesCache = new HashMap<>();

    private Evaluation evaluation;

    public void setEvaluation(Evaluation evaluation) {
        this.evaluation = evaluation;
        titleLabel.setText("Réponses - " + evaluation.getTitle());
        loadResponses();
    }

    @FXML
    public void initialize() {
        userCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(getUserFullName(cellData.getValue().getUserId()))
        );

        dateCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getSubmittedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getSubmittedAt().format(formatter));
            }
            return new SimpleStringProperty("-");
        });

        scoreCol.setCellValueFactory(cellData -> {
            Integer score = cellData.getValue().getScore();
            return new SimpleIntegerProperty(score != null ? score : 0).asObject();
        });

        configurerColonneActions();
    }

    private void loadResponses() {
        if (evaluation == null) {
            return;
        }

        try {
            List<UserEvaluation> responses =
                    userEvaluationService.getSubmittedUserEvaluationsByEvaluationId(evaluation.getId());

            responseTable.getItems().setAll(responses);

        } catch (SQLException e) {
            showError("Erreur chargement réponses : " + e.getMessage());
        }
    }

    private void configurerColonneActions() {
        actionCol.setCellValueFactory(cellData -> new SimpleStringProperty("Voir détail"));

        actionCol.setCellFactory(param -> new TableCell<>() {
            private final Button detailButton = new Button("Voir détail");
            private final HBox box = new HBox(detailButton);

            {
                detailButton.getStyleClass().add("button-primary");
                box.setStyle("-fx-alignment: center-left;");

                detailButton.setOnAction(event -> {
                    UserEvaluation ue = getTableView().getItems().get(getIndex());
                    showResponseDetail(ue);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                } else {
                    setGraphic(box);
                }
            }
        });
    }

    private void showResponseDetail(UserEvaluation ue) {
        if (evaluation != null && "QUIZ".equalsIgnoreCase(evaluation.getType())) {
            showQuizResponseDetail(ue);
        } else {
            showExamResponseDetail(ue);
        }
    }

    private void showExamResponseDetail(UserEvaluation ue) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détail de la réponse");
        alert.setHeaderText(getUserFullName(ue.getUserId()));

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(720);
        textArea.setPrefHeight(450);

        StringBuilder sb = new StringBuilder();
        sb.append("Utilisateur : ").append(getUserFullName(ue.getUserId())).append("\n");
        sb.append("Date de soumission : ")
                .append(ue.getSubmittedAt() != null ? ue.getSubmittedAt().format(formatter) : "-")
                .append("\n");
        sb.append("Score : ").append(ue.getScore() != null ? ue.getScore() : 0).append("\n\n");
        sb.append("Réponse soumise :\n\n");
        sb.append(ue.getAiFeedback() != null ? ue.getAiFeedback() : "Aucune réponse");

        textArea.setText(sb.toString());
        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.showAndWait();
    }

    private void showQuizResponseDetail(UserEvaluation ue) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détail du quiz");
        alert.setHeaderText(getUserFullName(ue.getUserId()));

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(760);
        textArea.setPrefHeight(500);

        StringBuilder sb = new StringBuilder();
        sb.append("Utilisateur : ").append(getUserFullName(ue.getUserId())).append("\n");
        sb.append("Date de soumission : ")
                .append(ue.getSubmittedAt() != null ? ue.getSubmittedAt().format(formatter) : "-")
                .append("\n");
        sb.append("Score : ").append(ue.getScore() != null ? ue.getScore() : 0).append("\n\n");

        String raw = ue.getAiFeedback();

        if (raw == null || raw.isBlank()) {
            sb.append("Aucune réponse sauvegardée.");
            textArea.setText(sb.toString());
            alert.getDialogPane().setContent(textArea);
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }

        if (!raw.startsWith("QUIZ_ANSWERS|")) {
            sb.append("Ancien format de réponse quiz :\n\n");
            sb.append(raw);
            sb.append("\n\nImpossible d'afficher les questions et choix car les réponses détaillées n'ont pas été sauvegardées.");
            textArea.setText(sb.toString());
            alert.getDialogPane().setContent(textArea);
            alert.setResizable(true);
            alert.showAndWait();
            return;
        }

        try {
            Map<Integer, Integer> selectedAnswersMap = parseSelectedAnswers(raw);
            List<Question> questions = questionService.filtrerParEvaluationId(evaluation.getId());

            int i = 1;
            for (Question q : questions) {
                sb.append("Q").append(i).append(". ").append(safe(q.getContent())).append("\n");

                List<Answer> answers = answerService.recupererOptionsQuizParQuestion(q.getId());
                Integer selectedAnswerId = selectedAnswersMap.get(q.getId());

                String userAnswerText = "Aucune réponse";
                String correctAnswerText = "Aucune";

                for (Answer a : answers) {
                    boolean isSelected = selectedAnswerId != null && a.getId() == selectedAnswerId;
                    boolean isCorrect = Boolean.TRUE.equals(a.getIsCorrect());

                    if (isSelected) {
                        userAnswerText = safe(a.getContent());
                    }

                    if (isCorrect) {
                        correctAnswerText = safe(a.getContent());
                    }

                    sb.append("   - ").append(safe(a.getContent()));

                    if (isSelected) {
                        sb.append("   [Votre choix]");
                    }
                    if (isCorrect) {
                        sb.append("   [Bonne réponse]");
                    }

                    sb.append("\n");
                }

                sb.append("Votre réponse : ").append(userAnswerText).append("\n");
                sb.append("Bonne réponse : ").append(correctAnswerText).append("\n");

                if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                    sb.append("Correction : ").append(q.getExplanation()).append("\n");
                }

                sb.append("\n----------------------------------------\n\n");
                i++;
            }

        } catch (SQLException e) {
            sb.append("Erreur lors du chargement des questions/réponses : ").append(e.getMessage());
        }

        textArea.setText(sb.toString());
        alert.getDialogPane().setContent(textArea);
        alert.setResizable(true);
        alert.showAndWait();
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
    private void handleClose() {
        titleLabel.getScene().getWindow().hide();
    }

    private String getUserFullName(Integer userId) {
        if (userId == null) {
            return "Utilisateur inconnu";
        }

        if (userNamesCache.containsKey(userId)) {
            return userNamesCache.get(userId);
        }

        try {
            String fullName = userEvaluationService.getUserFullNameById(userId);

            if (fullName == null || fullName.isBlank()) {
                fullName = "Utilisateur #" + userId;
            }

            userNamesCache.put(userId, fullName);
            return fullName;

        } catch (SQLException e) {
            return "Utilisateur #" + userId;
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}