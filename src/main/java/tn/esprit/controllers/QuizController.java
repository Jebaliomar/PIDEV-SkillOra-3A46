package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.services.AnswerService;
import tn.esprit.services.QuestionService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class QuizController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private VBox questionContainer;

    private static Evaluation evaluationSelectionnee;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    public static void setEvaluationSelectionnee(Evaluation evaluation) {
        evaluationSelectionnee = evaluation;
    }

    @FXML
    public void initialize() {
        if (evaluationSelectionnee == null) {
            showError("Aucun quiz sélectionné.");
            return;
        }

        titleLabel.setText("Quiz : " + evaluationSelectionnee.getTitle());
        subtitleLabel.setText("Gestion des questions et options");

        chargerQuestions();
    }

    private void chargerQuestions() {
        questionContainer.getChildren().clear();

        try {
            List<Question> questions = questionService.filtrerParEvaluationId(evaluationSelectionnee.getId());

            if (questions == null || questions.isEmpty()) {
                Label empty = new Label("Aucune question pour ce quiz.");
                empty.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                questionContainer.getChildren().add(empty);
                return;
            }

            int numero = 1;
            for (Question q : questions) {
                questionContainer.getChildren().add(creerCarteQuestion(q, numero));
                numero++;
            }

        } catch (SQLException e) {
            showError("Erreur chargement questions : " + e.getMessage());
        }
    }

    private VBox creerCarteQuestion(Question q, int numero) throws SQLException {
        VBox box = new VBox(10);
        box.setStyle(
                "-fx-background-color:#0b1730;" +
                        "-fx-padding:18;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#22365b;" +
                        "-fx-border-radius:16;"
        );

        Label questionLabel = new Label("Question " + numero + " : " + q.getContent());
        questionLabel.setStyle("-fx-text-fill:white; -fx-font-size:16px; -fx-font-weight:bold;");
        questionLabel.setWrapText(true);
        box.getChildren().add(questionLabel);

        if (q.getExplanation() != null && !q.getExplanation().trim().isEmpty()) {
            Label explanationLabel = new Label("Explication : " + q.getExplanation());
            explanationLabel.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");
            explanationLabel.setWrapText(true);
            box.getChildren().add(explanationLabel);
        }

        List<Answer> options = answerService.recupererOptionsQuizParQuestion(q.getId());

        if (options == null || options.isEmpty()) {
            Label noOptionsLabel = new Label("Aucune option enregistrée pour cette question.");
            noOptionsLabel.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:14px; -fx-font-weight:bold;");
            box.getChildren().add(noOptionsLabel);
        } else {
            char letter = 'A';
            String bonneReponse = "";

            for (Answer option : options) {
                Label optionLabel = new Label(letter + ") " + option.getContent());
                optionLabel.setStyle("-fx-text-fill:#dbe4f5; -fx-font-size:14px;");
                optionLabel.setWrapText(true);
                box.getChildren().add(optionLabel);

                if (Boolean.TRUE.equals(option.getIsCorrect())) {
                    bonneReponse = String.valueOf(letter);
                }

                letter++;
            }

            if (bonneReponse.isEmpty()) {
                Label noCorrectLabel = new Label("Aucune bonne réponse définie.");
                noCorrectLabel.setStyle("-fx-text-fill:#f59e0b; -fx-font-size:14px; -fx-font-weight:bold;");
                box.getChildren().add(noCorrectLabel);
            } else {
                Label correctLabel = new Label("Bonne réponse : " + bonneReponse);
                correctLabel.setStyle("-fx-text-fill:#22c55e; -fx-font-size:14px; -fx-font-weight:bold;");
                box.getChildren().add(correctLabel);
            }
        }

        Button btnModifier = new Button("Modifier");
        Button btnSupprimer = new Button("Supprimer");

        btnModifier.getStyleClass().add("button-secondary");
        btnSupprimer.getStyleClass().add("button-danger");

        btnModifier.setOnAction(e -> ouvrirModificationQuestion(q));
        btnSupprimer.setOnAction(e -> supprimerQuestion(q));

        HBox actions = new HBox(10, btnModifier, btnSupprimer);
        box.getChildren().add(actions);

        return box;
    }

    @FXML
    private void ouvrirAjoutQuestion() {
        try {
            QuizQuestionController.setEvaluationSelectionnee(evaluationSelectionnee);
            QuizQuestionController.setQuestionAModifier(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterQuizQuestion.fxml"));
            Parent root = loader.load();

            questionContainer.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture ajout question : " + e.getMessage());
        }
    }

    private void ouvrirModificationQuestion(Question question) {
        try {
            QuizQuestionController.setEvaluationSelectionnee(evaluationSelectionnee);
            QuizQuestionController.setQuestionAModifier(question);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterQuizQuestion.fxml"));
            Parent root = loader.load();

            questionContainer.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture modification : " + e.getMessage());
        }
    }

    private void supprimerQuestion(Question question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Voulez-vous supprimer cette question et ses options ?");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    answerService.supprimerOptionsQuizParQuestion(question.getId());
                    questionService.supprimer(question);
                    chargerQuestions();
                } catch (SQLException e) {
                    showError("Erreur suppression : " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void retour() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ListeEvaluation.fxml"));
            Parent root = loader.load();
            questionContainer.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour liste : " + e.getMessage());
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