package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.services.AnswerService;
import tn.esprit.services.QuestionService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class QuizQuestionController {

    @FXML
    private Label pageTitleLabel;

    @FXML
    private TextField idField;

    @FXML
    private TextField evaluationIdField;

    @FXML
    private Label evaluationInfoLabel;

    @FXML
    private TextArea contentField;

    @FXML
    private TextArea explanationField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField scoreField;

    @FXML
    private TextField optionAField;

    @FXML
    private TextField optionBField;

    @FXML
    private TextField optionCField;

    @FXML
    private TextField optionDField;

    @FXML
    private ComboBox<String> correctAnswerCombo;

    @FXML
    private Label contentErrorLabel;

    @FXML
    private Label explanationErrorLabel;

    @FXML
    private Label typeErrorLabel;

    @FXML
    private Label scoreErrorLabel;

    @FXML
    private Label optionErrorLabel;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnModifier;

    private final QuestionService questionService = new QuestionService();
    private final AnswerService answerService = new AnswerService();

    private static Question questionAModifier;
    private static Evaluation evaluationSelectionnee;

    public static void setQuestionAModifier(Question question) {
        questionAModifier = question;
    }

    public static void setEvaluationSelectionnee(Evaluation evaluation) {
        evaluationSelectionnee = evaluation;
    }

    @FXML
    public void initialize() {
        clearErrors();

        typeComboBox.getItems().addAll("QCM");
        correctAnswerCombo.getItems().addAll("A", "B", "C", "D");

        if (evaluationSelectionnee != null) {
            evaluationIdField.setText(String.valueOf(evaluationSelectionnee.getId()));
            evaluationInfoLabel.setText("Quiz : " + evaluationSelectionnee.getTitle());
        }

        if (questionAModifier != null) {
            modeModification();
        } else {
            modeAjout();
        }
    }

    private void modeAjout() {
        pageTitleLabel.setText("Ajouter Question Quiz");

        btnAjouter.setVisible(true);
        btnAjouter.setManaged(true);

        btnModifier.setVisible(false);
        btnModifier.setManaged(false);

        typeComboBox.setValue("QCM");
    }

    private void modeModification() {
        pageTitleLabel.setText("Modifier Question Quiz");

        idField.setText(String.valueOf(questionAModifier.getId()));
        contentField.setText(questionAModifier.getContent());
        explanationField.setText(questionAModifier.getExplanation());
        typeComboBox.setValue(questionAModifier.getType());
        scoreField.setText(String.valueOf(questionAModifier.getScore()));

        try {
            List<Answer> options = answerService.recupererOptionsQuizParQuestion(questionAModifier.getId());

            if (options.size() > 0) optionAField.setText(options.get(0).getContent());
            if (options.size() > 1) optionBField.setText(options.get(1).getContent());
            if (options.size() > 2) optionCField.setText(options.get(2).getContent());
            if (options.size() > 3) optionDField.setText(options.get(3).getContent());

            if (options.size() > 0 && Boolean.TRUE.equals(options.get(0).getIsCorrect())) correctAnswerCombo.setValue("A");
            if (options.size() > 1 && Boolean.TRUE.equals(options.get(1).getIsCorrect())) correctAnswerCombo.setValue("B");
            if (options.size() > 2 && Boolean.TRUE.equals(options.get(2).getIsCorrect())) correctAnswerCombo.setValue("C");
            if (options.size() > 3 && Boolean.TRUE.equals(options.get(3).getIsCorrect())) correctAnswerCombo.setValue("D");

        } catch (SQLException e) {
            showError(e.getMessage());
        }

        btnAjouter.setVisible(false);
        btnAjouter.setManaged(false);

        btnModifier.setVisible(true);
        btnModifier.setManaged(true);
    }

    @FXML
    private void ajouterQuestion() {
        clearErrors();

        if (!validerChamps()) {
            return;
        }

        try {
            Question q = new Question();
            q.setContent(contentField.getText().trim());
            q.setExplanation(explanationField.getText().trim());
            q.setType(typeComboBox.getValue());
            q.setScore(Integer.parseInt(scoreField.getText().trim()));
            q.setEvaluationId(Integer.parseInt(evaluationIdField.getText().trim()));

            int questionId = questionService.ajouterEtRetournerId(q);

            answerService.enregistrerOptionsQuiz(
                    questionId,
                    optionAField.getText().trim(),
                    optionBField.getText().trim(),
                    optionCField.getText().trim(),
                    optionDField.getText().trim(),
                    correctAnswerCombo.getValue()
            );

            showInfo("Succès", "Question ajoutée avec succès !");
            retourListeQuestions();

        } catch (SQLException | IOException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void modifierQuestion() {
        clearErrors();

        if (!validerChamps()) {
            return;
        }

        try {
            Question q = new Question();
            q.setId(Integer.parseInt(idField.getText().trim()));
            q.setContent(contentField.getText().trim());
            q.setExplanation(explanationField.getText().trim());
            q.setType(typeComboBox.getValue());
            q.setScore(Integer.parseInt(scoreField.getText().trim()));
            q.setEvaluationId(Integer.parseInt(evaluationIdField.getText().trim()));

            questionService.modifier(q);

            answerService.enregistrerOptionsQuiz(
                    q.getId(),
                    optionAField.getText().trim(),
                    optionBField.getText().trim(),
                    optionCField.getText().trim(),
                    optionDField.getText().trim(),
                    correctAnswerCombo.getValue()
            );

            showInfo("Succès", "Question modifiée avec succès !");
            retourListeQuestions();

        } catch (SQLException | IOException e) {
            showError(e.getMessage());
        }
    }

    private boolean validerChamps() {
        boolean ok = true;

        String content = contentField.getText() == null ? "" : contentField.getText().trim();
        String explanation = explanationField.getText() == null ? "" : explanationField.getText().trim();
        String type = typeComboBox.getValue();
        String scoreText = scoreField.getText() == null ? "" : scoreField.getText().trim();

        String a = optionAField.getText() == null ? "" : optionAField.getText().trim();
        String b = optionBField.getText() == null ? "" : optionBField.getText().trim();
        String c = optionCField.getText() == null ? "" : optionCField.getText().trim();
        String d = optionDField.getText() == null ? "" : optionDField.getText().trim();

        if (content.isEmpty()) {
            contentErrorLabel.setText("Le contenu est obligatoire.");
            ok = false;
        }

        if (explanation.isEmpty()) {
            explanationErrorLabel.setText("L'explication est obligatoire.");
            ok = false;
        }

        if (type == null || type.trim().isEmpty()) {
            typeErrorLabel.setText("Le type est obligatoire.");
            ok = false;
        }

        if (scoreText.isEmpty()) {
            scoreErrorLabel.setText("Le score est obligatoire.");
            ok = false;
        } else {
            try {
                int score = Integer.parseInt(scoreText);
                if (score < 0) {
                    scoreErrorLabel.setText("Le score doit être positif.");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                scoreErrorLabel.setText("Le score doit être numérique.");
                ok = false;
            }
        }

        if (a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty()) {
            optionErrorLabel.setText("Les 4 options A/B/C/D sont obligatoires.");
            ok = false;
        }

        if (correctAnswerCombo.getValue() == null || correctAnswerCombo.getValue().trim().isEmpty()) {
            optionErrorLabel.setText("Choisissez la bonne réponse.");
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        contentErrorLabel.setText("");
        explanationErrorLabel.setText("");
        typeErrorLabel.setText("");
        scoreErrorLabel.setText("");
        optionErrorLabel.setText("");
    }

    @FXML
    private void viderChamps() {
        idField.clear();
        contentField.clear();
        explanationField.clear();
        scoreField.clear();
        optionAField.clear();
        optionBField.clear();
        optionCField.clear();
        optionDField.clear();
        typeComboBox.setValue("QCM");
        correctAnswerCombo.setValue(null);
        clearErrors();
    }

    @FXML
    private void retourListeQuestions() throws IOException {
        QuizController.setEvaluationSelectionnee(evaluationSelectionnee);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/QuizView.fxml"));
        Parent root = loader.load();
        contentField.getScene().setRoot(root);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}