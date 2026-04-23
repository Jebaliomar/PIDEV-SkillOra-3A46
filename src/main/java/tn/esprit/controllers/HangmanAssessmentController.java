package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.entities.HangmanGame;
import tn.esprit.services.GroqHangmanGeneratorService;
import tn.esprit.services.HangmanAttemptService;
import tn.esprit.services.HangmanGameService;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HangmanAssessmentController {

    @FXML
    private ComboBox<String> topicComboBox;

    @FXML
    private ComboBox<String> levelComboBox;

    @FXML
    private Button btnNewGame;

    @FXML
    private Label lblInfo;

    @FXML
    private Label lblSessionCount;

    @FXML
    private FlowPane gamesFlowPane;

    private final HangmanGameService gameService = new HangmanGameService();
    private final GroqHangmanGeneratorService groqService = new GroqHangmanGeneratorService();
    private final HangmanAttemptService attemptService = new HangmanAttemptService();

    private int userId = 1;

    @FXML
    public void initialize() {
        loadTopics();
        loadLevels();
        loadHistoryCards();
    }

    public void setUserId(int userId) {
        this.userId = userId;
        loadHistoryCards();
    }

    private void loadTopics() {
        topicComboBox.getItems().clear();
        topicComboBox.getItems().addAll(
                "Java",
                "JavaFX",
                "SpringBoot",
                "Symfony",
                "CSS",
                "JavaScript",
                "Python",
                "C",
                "C++",
                "PHP"
        );
        topicComboBox.setValue("Java");
    }

    private void loadLevels() {
        levelComboBox.getItems().clear();
        levelComboBox.getItems().addAll("EASY", "MEDIUM", "HARD");
        levelComboBox.setValue("EASY");
    }

    private void loadHistoryCards() {
        if (gamesFlowPane == null) {
            return;
        }

        gamesFlowPane.getChildren().clear();

        try {
            List<Object[]> history = attemptService.getUserHistory(userId);
            lblSessionCount.setText(history.size() + " SESSION(S)");

            if (history.isEmpty()) {
                VBox emptyCard = new VBox(10);
                emptyCard.setPrefWidth(320);
                emptyCard.setPadding(new Insets(20));
                emptyCard.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 18;" +
                                "-fx-border-color: #e7ebf2;" +
                                "-fx-border-radius: 18;"
                );

                Label emptyTitle = new Label("No games yet");
                emptyTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

                Label emptyText = new Label("Start your first Hangman session and it will appear here.");
                emptyText.setWrapText(true);
                emptyText.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");

                emptyCard.getChildren().addAll(emptyTitle, emptyText);
                gamesFlowPane.getChildren().add(emptyCard);
                return;
            }

            for (Object[] row : history) {
                gamesFlowPane.getChildren().add(createGameCard(row));
            }

        } catch (SQLException e) {
            showError("Erreur chargement historique : " + e.getMessage());
        }
    }

    private VBox createGameCard(Object[] row) {
        String topic = row[0] == null ? "-" : row[0].toString();
        String level = row[1] == null ? "-" : row[1].toString();
        String title = row[2] == null ? "-" : row[2].toString();
        String answer = row[3] == null ? "-" : row[3].toString();
        boolean won = row[4] != null && (Boolean) row[4];
        int score = row[6] == null ? 0 : ((Number) row[6]).intValue();
        int mistakes = row[7] == null ? 0 : ((Number) row[7]).intValue();
        Timestamp playedAt = (Timestamp) row[8];

        VBox card = new VBox(14);
        card.setPrefWidth(290);
        card.setMinWidth(290);
        card.setMaxWidth(290);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 22;" +
                        "-fx-border-color: #e7ebf2;" +
                        "-fx-border-radius: 22;"
        );

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.TOP_RIGHT);

        Label arrow = new Label("↗");
        arrow.setStyle("-fx-font-size: 18px; -fx-text-fill: #94a3b8;");
        topRow.getChildren().add(arrow);

        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        HBox badges = new HBox(10);

        Label topicBadge = new Label(topic.toUpperCase());
        topicBadge.setStyle(
                "-fx-background-color: #dbeafe;" +
                        "-fx-text-fill: #1d4ed8;" +
                        "-fx-padding: 7 14 7 14;" +
                        "-fx-background-radius: 18;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        Label levelBadge = new Label(level.toUpperCase());
        levelBadge.setStyle(
                "-fx-background-color: #f8e7b1;" +
                        "-fx-text-fill: #b45309;" +
                        "-fx-padding: 7 14 7 14;" +
                        "-fx-background-radius: 18;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );

        Label statusBadge = new Label(won ? "SUCCESS" : "FAILED");
        statusBadge.setStyle(won
                ? "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 7 14 7 14; -fx-background-radius: 18; -fx-font-size: 13px; -fx-font-weight: bold;"
                : "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 7 14 7 14; -fx-background-radius: 18; -fx-font-size: 13px; -fx-font-weight: bold;");

        badges.getChildren().addAll(topicBadge, levelBadge, statusBadge);

        Label answerLabel = new Label("Correct answer: " + answer.toUpperCase());
        answerLabel.setWrapText(true);
        answerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1d4ed8; -fx-font-weight: bold;");

        String playedText = "-";
        if (playedAt != null) {
            playedText = playedAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }

        Label footer = new Label("Created on " + playedText);
        footer.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        Label stats = new Label("Score: " + score + "   |   Mistakes: " + mistakes);
        stats.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        card.getChildren().addAll(topRow, titleLabel, badges, answerLabel, stats, footer);
        return card;
    }

    @FXML
    private void handleNewGame() {
        String topic = topicComboBox.getValue();
        String level = levelComboBox.getValue();

        if (topic == null || topic.trim().isEmpty()) {
            showWarning("Veuillez choisir un topic.");
            return;
        }

        if (level == null || level.trim().isEmpty()) {
            showWarning("Veuillez choisir un niveau.");
            return;
        }

        try {
            List<HangmanGame> sessionGames = buildUniqueSessionGames(topic, level, 3);

            if (sessionGames.isEmpty()) {
                showWarning("Impossible de préparer une session.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/HangmanGame.fxml"));
            Parent root = loader.load();

            HangmanGameController controller = loader.getController();
            controller.initSession(userId, topic, level, sessionGames);

            lblInfo.setText("Session of " + sessionGames.size() + " questions started for " + topic + " / " + level + ".");
            btnNewGame.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture jeu : " + e.getMessage());
        } catch (SQLException e) {
            showError("Erreur base de données : " + e.getMessage());
        } catch (Exception e) {
            showError("Erreur génération IA : " + e.getMessage());
        }
    }

    private List<HangmanGame> buildUniqueSessionGames(String topic, String level, int targetCount) throws Exception {
        List<HangmanGame> sessionGames = new ArrayList<>();
        List<HangmanGame> databaseGames = gameService.getUniqueGamesByTopicAndLevel(topic, level);

        Set<String> usedAnswers = new HashSet<>();
        Set<String> usedTitles = new HashSet<>();

        for (HangmanGame game : databaseGames) {
            if (sessionGames.size() >= targetCount) {
                break;
            }

            if (isDuplicate(game, usedAnswers, usedTitles)) {
                continue;
            }

            sessionGames.add(game);
            remember(game, usedAnswers, usedTitles);
        }

        int maxGenerationAttempts = 10;

        while (sessionGames.size() < targetCount && maxGenerationAttempts > 0) {
            maxGenerationAttempts--;

            HangmanGame generatedGame;
            try {
                generatedGame = groqService.generateQuestion(topic, level, usedAnswers);
            } catch (Exception e) {
                continue;
            }

            if (generatedGame == null || isDuplicate(generatedGame, usedAnswers, usedTitles)) {
                continue;
            }

            HangmanGame savedGame = gameService.saveGeneratedGame(generatedGame);
            sessionGames.add(savedGame);
            remember(savedGame, usedAnswers, usedTitles);
        }

        if (sessionGames.size() < targetCount) {
            showWarning("Seulement " + sessionGames.size() + " question(s) unique(s) trouvée(s) pour " + topic + " / " + level + ".");
        }

        return sessionGames;
    }

    private boolean isDuplicate(HangmanGame game, Set<String> usedAnswers, Set<String> usedTitles) {
        if (game == null) {
            return true;
        }

        String answerKey = normalize(game.getAnswer());
        String titleKey = normalize(game.getTitle());

        return (!answerKey.isBlank() && usedAnswers.contains(answerKey))
                || (!titleKey.isBlank() && usedTitles.contains(titleKey));
    }

    private void remember(HangmanGame game, Set<String> usedAnswers, Set<String> usedTitles) {
        String answerKey = normalize(game.getAnswer());
        String titleKey = normalize(game.getTitle());

        if (!answerKey.isBlank()) {
            usedAnswers.add(answerKey);
        }
        if (!titleKey.isBlank()) {
            usedTitles.add(titleKey);
        }
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    @FXML
    private void handleBackToAssessments() {
        openScene("/UserAssessmentView.fxml");
    }

    private void openScene(String fxmlPath) {
        try {
            var url = getClass().getResource(fxmlPath);

            if (url == null) {
                showError("FXML introuvable : " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            btnNewGame.getScene().setRoot(root);

        } catch (IOException e) {
            showError("Erreur ouverture écran : " + e.getMessage());
        }
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