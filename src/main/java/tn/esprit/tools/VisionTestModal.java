package tn.esprit.tools;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class VisionTestModal {

    private static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "K", "L", "M", "N", "P", "R", "S", "T", "U", "V", "Z"
    };
    private static final int[] LETTER_SIZES = {140, 112, 90, 72, 58, 46, 36, 28, 22, 18};
    private static final String[] DISTANCES = {
            "6 meters", "5 meters", "4 meters", "3 meters", "2.5 meters",
            "2 meters", "1.5 meters", "1 meter", "75 cm", "50 cm"
    };

    private final Window owner;
    private final Collection<String> stylesheets;
    private final Random random = new Random();
    private final List<StackPane> progressDots = new ArrayList<>();

    private Stage stage;
    private VBox content;
    private ProgressBar progressBar;
    private Label levelLabel;
    private Label letterLabel;
    private int currentLevelIndex;
    private int lastVisibleLevel;

    public VisionTestModal(Window owner, Collection<String> stylesheets) {
        this.owner = owner;
        this.stylesheets = stylesheets == null ? List.of() : List.copyOf(stylesheets);
    }

    public void show() {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setTitle("Vision Test");
        stage.setMinWidth(620);
        stage.setMinHeight(620);

        StackPane root = new StackPane();
        root.getStyleClass().add("vision-test-root");
        if (owner != null && owner.getScene() != null && owner.getScene().getRoot().getStyleClass().contains("theme-dark")) {
            root.getStyleClass().add("theme-dark");
        }
        root.setPadding(new Insets(24));

        content = new VBox(22);
        content.getStyleClass().add("vision-test-card");
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28));
        content.setMaxWidth(720);

        root.getChildren().add(content);

        Scene scene = new Scene(root, 680, 680);
        scene.getStylesheets().addAll(stylesheets);
        stage.setScene(scene);

        currentLevelIndex = 0;
        lastVisibleLevel = 0;
        showTestScreen();
        stage.show();
    }

    private void showTestScreen() {
        content.getChildren().setAll(
                buildHeader(),
                buildProgressSection(),
                buildLetterSection(),
                buildAnswerButtons()
        );
        renderLevel();
    }

    private VBox buildHeader() {
        Label title = new Label("Vision Test");
        title.getStyleClass().add("vision-test-title");

        Label subtitle = new Label("Assess your visual acuity");
        subtitle.getStyleClass().add("vision-test-subtitle");

        VBox header = new VBox(6, title, subtitle);
        header.setAlignment(Pos.CENTER);
        return header;
    }

    private VBox buildProgressSection() {
        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("vision-test-progress");
        progressBar.setMaxWidth(Double.MAX_VALUE);

        HBox dots = new HBox(8);
        dots.getStyleClass().add("vision-test-dots");
        dots.setAlignment(Pos.CENTER);
        progressDots.clear();
        for (int index = 0; index < LETTER_SIZES.length; index++) {
            StackPane dot = new StackPane();
            dot.getStyleClass().add("vision-test-dot");
            progressDots.add(dot);
            dots.getChildren().add(dot);
        }

        VBox progress = new VBox(12, progressBar, dots);
        progress.setAlignment(Pos.CENTER);
        progress.setMaxWidth(Double.MAX_VALUE);
        return progress;
    }

    private VBox buildLetterSection() {
        levelLabel = new Label();
        levelLabel.getStyleClass().add("vision-test-level");

        letterLabel = new Label();
        letterLabel.getStyleClass().add("vision-test-letter");
        letterLabel.setMinHeight(220);
        letterLabel.setAlignment(Pos.CENTER);
        letterLabel.setMaxWidth(Double.MAX_VALUE);

        VBox section = new VBox(14, levelLabel, letterLabel);
        section.getStyleClass().add("vision-test-letter-panel");
        section.setAlignment(Pos.CENTER);
        section.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(section, Priority.ALWAYS);
        return section;
    }

    private HBox buildAnswerButtons() {
        Button yesButton = new Button("✓ Yes, I can see it");
        yesButton.getStyleClass().addAll("vision-test-button", "vision-test-button-yes");
        yesButton.setMaxWidth(Double.MAX_VALUE);
        yesButton.setOnAction(event -> answer(true));

        Button noButton = new Button("✗ No, I cannot see it");
        noButton.getStyleClass().addAll("vision-test-button", "vision-test-button-no");
        noButton.setMaxWidth(Double.MAX_VALUE);
        noButton.setOnAction(event -> answer(false));

        HBox buttons = new HBox(12, yesButton, noButton);
        buttons.getStyleClass().add("vision-test-actions");
        buttons.setAlignment(Pos.CENTER);
        HBox.setHgrow(yesButton, Priority.ALWAYS);
        HBox.setHgrow(noButton, Priority.ALWAYS);
        return buttons;
    }

    private void renderLevel() {
        int level = currentLevelIndex + 1;
        int size = LETTER_SIZES[currentLevelIndex];
        levelLabel.setText("Level " + level + " — " + size + "px");
        letterLabel.setText(LETTERS[random.nextInt(LETTERS.length)]);
        letterLabel.setStyle("-fx-font-size: " + size + "px;");
        updateProgress();
        animateLetter();
    }

    private void answer(boolean visible) {
        if (visible) {
            lastVisibleLevel = currentLevelIndex + 1;
        }
        currentLevelIndex++;
        updateProgress();
        if (currentLevelIndex >= LETTER_SIZES.length) {
            showResultScreen();
        } else {
            renderLevel();
        }
    }

    private void updateProgress() {
        double progress = Math.min(currentLevelIndex / (double) LETTER_SIZES.length, 1.0);
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
        for (int index = 0; index < progressDots.size(); index++) {
            StackPane dot = progressDots.get(index);
            dot.getStyleClass().removeAll("vision-test-dot-active", "vision-test-dot-current");
            if (index < currentLevelIndex) {
                dot.getStyleClass().add("vision-test-dot-active");
            } else if (index == currentLevelIndex && currentLevelIndex < LETTER_SIZES.length) {
                dot.getStyleClass().add("vision-test-dot-current");
            }
        }
    }

    private void animateLetter() {
        letterLabel.setOpacity(0);
        letterLabel.setScaleX(0.92);
        letterLabel.setScaleY(0.92);

        FadeTransition fade = new FadeTransition(Duration.millis(220), letterLabel);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(220), letterLabel);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);

        new ParallelTransition(fade, scale).play();
    }

    private void showResultScreen() {
        updateProgress();

        Label title = new Label("Vision Test Result");
        title.getStyleClass().add("vision-test-title");

        Label visibleLevel = new Label(buildVisibleLevelText());
        visibleLevel.getStyleClass().add("vision-test-result-main");
        visibleLevel.setWrapText(true);

        Label distance = new Label(buildDistanceText());
        distance.getStyleClass().add("vision-test-result-distance");
        distance.setWrapText(true);

        Label tip = new Label(buildTipText());
        tip.getStyleClass().add("vision-test-result-tip");
        tip.setWrapText(true);

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().addAll("vision-test-button", "vision-test-button-close");
        closeButton.setOnAction(event -> stage.close());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content.getChildren().setAll(
                title,
                buildProgressSection(),
                buildResultPanel(visibleLevel, distance, tip),
                closeButton
        );
        currentLevelIndex = LETTER_SIZES.length;
        updateProgress();
    }

    private VBox buildResultPanel(Node visibleLevel, Node distance, Node tip) {
        VBox panel = new VBox(14, visibleLevel, distance, tip);
        panel.getStyleClass().add("vision-test-result-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private String buildVisibleLevelText() {
        if (lastVisibleLevel <= 0) {
            return "No visible level was recorded.";
        }
        return "Last visible level: Level " + lastVisibleLevel + " — " + LETTER_SIZES[lastVisibleLevel - 1] + "px";
    }

    private String buildDistanceText() {
        if (lastVisibleLevel <= 0) {
            return "Recommendation: consult an eye specialist.";
        }
        return "Recommended viewing distance: " + DISTANCES[lastVisibleLevel - 1];
    }

    private String buildTipText() {
        if (lastVisibleLevel <= 0) {
            return "Please consult an eye specialist for a complete vision exam.";
        }
        if (lastVisibleLevel <= 3) {
            return "Your visual acuity seems weak. Consider increasing text size and consulting an eye specialist.";
        }
        if (lastVisibleLevel <= 6) {
            return "Your visual acuity seems moderate. Use good lighting and avoid screen fatigue.";
        }
        return "Your visual acuity seems good. Keep healthy viewing habits.";
    }
}
