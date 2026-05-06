package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import tn.esprit.entities.HangmanAttempt;
import tn.esprit.entities.HangmanGame;
import tn.esprit.services.HangmanAttemptService;
import tn.esprit.services.HangmanSessionService;
import tn.esprit.services.UserEvaluationService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HangmanGameController {

    @FXML private Label lblTopic;
    @FXML private Label lblLevel;
    @FXML private Label lblQuestionIndex;
    @FXML private Label lblQuestionTitle;
    @FXML private Label lblHint;
    @FXML private Label lblWord;
    @FXML private Label lblMistakes;

    @FXML private Button btnHint;
    @FXML private Button btnNext;

    @FXML private GridPane keyboardGrid;

    @FXML private Circle headCircle;
    @FXML private Line bodyLine;
    @FXML private Line leftArmLine;
    @FXML private Line rightArmLine;
    @FXML private Line leftLegLine;
    @FXML private Line rightLegLine;

    private final HangmanAttemptService attemptService = new HangmanAttemptService();
    private final HangmanSessionService sessionService = new HangmanSessionService();
    private final UserEvaluationService userEvaluationService = new UserEvaluationService();

    private int userId;
    private String topic;
    private String level;
    private List<HangmanGame> games = new ArrayList<>();
    private int currentIndex;
    private int correctAnswers;
    private boolean sessionFinished;

    public void initSession(int userId, String topic, String level, List<HangmanGame> sessionGames) {
        this.userId = userId;
        this.topic = topic;
        this.level = level;
        this.games = new ArrayList<>(sessionGames);
        this.currentIndex = 0;
        this.correctAnswers = 0;
        this.sessionFinished = false;

        startCurrentGame();
    }

    private void startCurrentGame() {
        if (games == null || games.isEmpty()) {
            showError("Aucune question disponible.");
            return;
        }

        if (currentIndex >= games.size()) {
            openSessionResult();
            return;
        }

        HangmanGame game = games.get(currentIndex);
        sessionService.startGame(game);

        lblTopic.setText("Topic : " + topic);
        lblLevel.setText("Level : " + level);
        lblQuestionIndex.setText("Question " + (currentIndex + 1) + " / " + games.size());
        lblQuestionTitle.setText(game.getTitle() == null ? "-" : game.getTitle());
        lblHint.setText("Hint : " + (game.getHint() == null ? "-" : game.getHint()));
        lblWord.setText(sessionService.getMaskedWord());
        lblWord.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: black;");
        lblMistakes.setText("Mistakes : 0 / " + game.getMaxMistakes());

        resetHangmanDrawing();

        btnHint.setDisable(false);
        btnNext.setDisable(true);
        btnNext.setText(currentIndex < games.size() - 1 ? "Next Question" : "Finish");

        sessionFinished = false;
        buildKeyboard();
    }

    private void resetHangmanDrawing() {
        headCircle.setVisible(false);
        bodyLine.setVisible(false);
        leftArmLine.setVisible(false);
        rightArmLine.setVisible(false);
        leftLegLine.setVisible(false);
        rightLegLine.setVisible(false);
    }

    private void updateHangmanDrawing() {
        int mistakes = sessionService.getMistakes();
        headCircle.setVisible(mistakes >= 1);
        bodyLine.setVisible(mistakes >= 2);
        leftArmLine.setVisible(mistakes >= 3);
        rightArmLine.setVisible(mistakes >= 4);
        leftLegLine.setVisible(mistakes >= 5);
        rightLegLine.setVisible(mistakes >= 6);
    }

    private void buildKeyboard() {
        keyboardGrid.getChildren().clear();

        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int col = 0;
        int row = 0;

        for (int i = 0; i < letters.length(); i++) {
            char letter = letters.charAt(i);

            Button btn = new Button(String.valueOf(letter));
            btn.setPrefWidth(56);
            btn.setPrefHeight(38);
            btn.setStyle("-fx-font-size: 16px; -fx-text-fill: black;");

            btn.setOnAction(e -> handleLetterClick(btn, letter));
            keyboardGrid.add(btn, col, row);

            col++;
            if (col == 7) {
                col = 0;
                row++;
            }
        }
    }

    private void handleLetterClick(Button btn, char letter) {
        boolean correct = sessionService.guessLetter(letter);
        btn.setDisable(true);

        if (correct) {
            btn.setStyle("-fx-background-color: #b8f5b1; -fx-text-fill: black; -fx-font-weight: bold;");
        } else {
            btn.setStyle("-fx-background-color: #ffb3b3; -fx-text-fill: black; -fx-font-weight: bold;");
        }

        HangmanGame currentGame = sessionService.getCurrentGame();
        lblWord.setText(sessionService.getMaskedWord());
        lblWord.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: black;");
        lblMistakes.setText("Mistakes : " + sessionService.getMistakes() + " / " + currentGame.getMaxMistakes());

        updateHangmanDrawing();

        if (sessionService.isWon()) {
            correctAnswers++;
            saveAttemptIfPersisted(true, false);
            disableKeyboard();

            if (currentIndex < games.size() - 1) {
                btnNext.setDisable(false);
                showInfo("Bonne réponse ! Passe à la question suivante.");
            } else {
                sessionFinished = true;
                btnNext.setDisable(false);
                btnNext.setText("Finish");
                showInfo("Bravo ! Session terminée avec succès.");
            }

        } else if (sessionService.isLost()) {
            saveAttemptIfPersisted(false, true);
            disableKeyboard();
            lblWord.setText(sessionService.revealAnswer());
            lblWord.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: black;");
            sessionFinished = true;

            showInfo("Réponse incorrecte. La session s'arrête ici. Réponse : " + sessionService.revealAnswer());
            openSessionResult();
        }
    }

    @FXML
    private void handleHint() {
        HangmanGame game = sessionService.getCurrentGame();
        if (game == null) {
            return;
        }

        showInfo("Indice : " + (game.getHint() == null ? "-" : game.getHint()));
        btnHint.setDisable(true);
    }

    @FXML
    private void handleNextQuestion() {
        if (!sessionService.isFinished() && !sessionFinished) {
            showWarning("Tu dois d'abord terminer la question actuelle.");
            return;
        }

        if (currentIndex >= games.size() - 1) {
            openSessionResult();
            return;
        }

        currentIndex++;
        startCurrentGame();
    }

    @FXML
    private void handleBackThemes() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HangmanAssessment.fxml"));
            Parent root = loader.load();

            HangmanAssessmentController controller = loader.getController();
            controller.setUserId(userId);

            lblWord.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur retour mini game Hangman : " + e.getMessage());
        }
    }

    private void saveAttemptIfPersisted(boolean won, boolean lost) {
        if (userId <= 0 && !resolveConnectedUser()) {
            return;
        }
        HangmanGame game = sessionService.getCurrentGame();

        if (game == null || game.getId() == null || game.getId() <= 0) {
            return;
        }

        try {
            HangmanAttempt attempt = new HangmanAttempt();
            attempt.setUserId(userId);
            attempt.setGameId(game.getId());
            attempt.setGuessed(sessionService.getGuessedLettersAsString());
            attempt.setWon(won);
            attempt.setLost(lost);
            attempt.setScore(sessionService.calculateScore());
            attempt.setMistakes((short) sessionService.getMistakes());
            attempt.setUpdatedAt(LocalDateTime.now());
            attempt.setPlayedAt(LocalDateTime.now());

            attemptService.saveOrUpdateAttempt(attempt);

        } catch (SQLException e) {
            showError("Erreur sauvegarde tentative : " + e.getMessage());
        }
    }

    private boolean resolveConnectedUser() {
        try {
            userId = userEvaluationService.resolveAuthenticatedUserId();
            return true;
        } catch (SQLException e) {
            if (userId > 0) {
                try {
                    userEvaluationService.requireExistingUser(userId);
                    System.out.println("Connected user ID = " + userId);
                    return true;
                } catch (SQLException ignored) {
                    // Fall through to the reconnect message.
                }
            }
            showError(UserEvaluationService.USER_NOT_FOUND_MESSAGE);
            return false;
        }
    }

    private void disableKeyboard() {
        keyboardGrid.getChildren().forEach(node -> node.setDisable(true));
    }

    private void openSessionResult() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HangmanThemeResult.fxml"));
            Parent root = loader.load();

            HangmanThemeResultController controller = loader.getController();
            controller.initSessionResult(userId, topic, level, correctAnswers, games.size());

            lblWord.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur ouverture résultat final : " + e.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
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

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
