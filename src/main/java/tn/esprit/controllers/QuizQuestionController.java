package tn.esprit.controllers;

import com.google.gson.JsonObject;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entities.Answer;
import tn.esprit.entities.Evaluation;
import tn.esprit.entities.Question;
import tn.esprit.services.AnswerService;
import tn.esprit.services.GroqQuizService;
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
    private TextField aiTopicField;

    @FXML
    private Button btnGenerateAI;

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
    private final GroqQuizService groqQuizService = new GroqQuizService();

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

        typeComboBox.getItems().clear();
        typeComboBox.getItems().addAll("QCM");

        correctAnswerCombo.getItems().clear();
        correctAnswerCombo.getItems().addAll("A", "B", "C", "D");

        if (evaluationSelectionnee != null) {
            evaluationIdField.setText(String.valueOf(evaluationSelectionnee.getId()));
            evaluationInfoLabel.setText("Quiz : " + safe(evaluationSelectionnee.getTitle()));
        } else {
            evaluationInfoLabel.setText("Quiz");
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
        contentField.setText(safe(questionAModifier.getContent()));
        explanationField.setText(safe(questionAModifier.getExplanation()));
        typeComboBox.setValue(safe(questionAModifier.getType()).isEmpty() ? "QCM" : questionAModifier.getType());
        scoreField.setText(String.valueOf(questionAModifier.getScore()));

        try {
            List<Answer> options = answerService.recupererOptionsQuizParQuestion(questionAModifier.getId());

            optionAField.clear();
            optionBField.clear();
            optionCField.clear();
            optionDField.clear();
            correctAnswerCombo.setValue(null);

            if (options.size() > 0) optionAField.setText(safe(options.get(0).getContent()));
            if (options.size() > 1) optionBField.setText(safe(options.get(1).getContent()));
            if (options.size() > 2) optionCField.setText(safe(options.get(2).getContent()));
            if (options.size() > 3) optionDField.setText(safe(options.get(3).getContent()));

            if (options.size() > 0 && Boolean.TRUE.equals(options.get(0).getIsCorrect())) correctAnswerCombo.setValue("A");
            else if (options.size() > 1 && Boolean.TRUE.equals(options.get(1).getIsCorrect())) correctAnswerCombo.setValue("B");
            else if (options.size() > 2 && Boolean.TRUE.equals(options.get(2).getIsCorrect())) correctAnswerCombo.setValue("C");
            else if (options.size() > 3 && Boolean.TRUE.equals(options.get(3).getIsCorrect())) correctAnswerCombo.setValue("D");

        } catch (SQLException e) {
            showError("Erreur chargement des options : " + e.getMessage());
        }

        btnAjouter.setVisible(false);
        btnAjouter.setManaged(false);

        btnModifier.setVisible(true);
        btnModifier.setManaged(true);
    }

    @FXML
    private void genererParIA() {
        clearErrors();

        String sujet = safe(aiTopicField.getText()).trim();

        if (sujet.isEmpty()) {
            showError("Veuillez saisir un sujet ou un langage.");
            return;
        }

        btnGenerateAI.setDisable(true);
        btnGenerateAI.setText("Génération...");

        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                return groqQuizService.genererQuestionQuiz(sujet);
            }
        };

        task.setOnSucceeded(event -> {
            try {
                JsonObject result = task.getValue();

                if (result == null) {
                    showError("La génération IA a échoué.");
                    return;
                }

                contentField.setText(getString(result, "question"));
                optionAField.setText(getString(result, "optionA"));
                optionBField.setText(getString(result, "optionB"));
                optionCField.setText(getString(result, "optionC"));
                optionDField.setText(getString(result, "optionD"));
                explanationField.setText(getString(result, "explanation"));

                String correct = getString(result, "correctAnswer").trim().toUpperCase();
                if (correct.matches("[ABCD]")) {
                    correctAnswerCombo.setValue(correct);
                } else {
                    correctAnswerCombo.setValue(null);
                }

                if (typeComboBox.getValue() == null || typeComboBox.getValue().trim().isEmpty()) {
                    typeComboBox.setValue("QCM");
                }

                if (safe(scoreField.getText()).trim().isEmpty()) {
                    scoreField.setText("1");
                }

                showInfo("Succès", "Question générée automatiquement par IA.");

            } catch (Exception e) {
                showError("Erreur traitement réponse IA : " + e.getMessage());
            } finally {
                btnGenerateAI.setDisable(false);
                btnGenerateAI.setText("Générer IA");
            }
        });

        task.setOnFailed(event -> {
            btnGenerateAI.setDisable(false);
            btnGenerateAI.setText("Générer IA");

            Throwable ex = task.getException();
            showError("Erreur génération IA : " + (ex != null ? ex.getMessage() : "Erreur inconnue"));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
            showError("Erreur ajout question : " + e.getMessage());
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
            showError("Erreur modification question : " + e.getMessage());
        }
    }

    private boolean validerChamps() {
        boolean ok = true;

        String evaluationId = safe(evaluationIdField.getText()).trim();
        String content = safe(contentField.getText()).trim();
        String explanation = safe(explanationField.getText()).trim();
        String type = typeComboBox.getValue();
        String scoreText = safe(scoreField.getText()).trim();

        String a = safe(optionAField.getText()).trim();
        String b = safe(optionBField.getText()).trim();
        String c = safe(optionCField.getText()).trim();
        String d = safe(optionDField.getText()).trim();

        if (evaluationId.isEmpty()) {
            showError("Aucune évaluation sélectionnée.");
            return false;
        }

        try {
            Integer.parseInt(evaluationId);
        } catch (NumberFormatException e) {
            showError("ID de l'évaluation invalide.");
            return false;
        }

        if (content.isEmpty()) {
            contentErrorLabel.setText("Le contenu est obligatoire.");
            ok = false;
        } else if (content.length() < 5) {
            contentErrorLabel.setText("La question doit contenir au moins 5 caractères.");
            ok = false;
        }

        if (explanation.isEmpty()) {
            explanationErrorLabel.setText("L'explication est obligatoire.");
            ok = false;
        } else if (explanation.length() < 3) {
            explanationErrorLabel.setText("L'explication doit contenir au moins 3 caractères.");
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
                if (score <= 0) {
                    scoreErrorLabel.setText("Le score doit être supérieur à 0.");
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

        if (a.equalsIgnoreCase(b) || a.equalsIgnoreCase(c) || a.equalsIgnoreCase(d)
                || b.equalsIgnoreCase(c) || b.equalsIgnoreCase(d)
                || c.equalsIgnoreCase(d)) {
            optionErrorLabel.setText("Les options doivent être différentes.");
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

        if (aiTopicField != null) {
            aiTopicField.clear();
        }

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

    private String getString(JsonObject json, String key) {
        if (json == null || !json.has(key) || json.get(key).isJsonNull()) {
            return "";
        }
        return json.get(key).getAsString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
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