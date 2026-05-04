package tn.esprit.controllers;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.tools.PomodoroIcon;

import java.net.URL;
import java.util.ResourceBundle;

public class PomodoroController implements Initializable {

    public enum Mode {
        FOCUS(25 * 60, "pomodoro.phase.focus"),
        SHORT_BREAK(5 * 60, "pomodoro.phase.short"),
        LONG_BREAK(15 * 60, "pomodoro.phase.long");

        public final int seconds;
        public final String key;
        Mode(int s, String k) { seconds = s; key = k; }
    }

    @FXML private HBox headerBar;
    @FXML private Label titleLabel;
    @FXML private Label phaseLabel;
    @FXML private Label timeLabel;
    @FXML private Label hintLabel;
    @FXML private Label cycleLabel;
    @FXML private ProgressBar progressBar;
    @FXML private StackPane tomatoStack;
    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button resetBtn;
    @FXML private Button skipBtn;
    @FXML private Button closeBtn;
    @FXML private ToggleButton modeFocus;
    @FXML private ToggleButton modeShortBreak;
    @FXML private ToggleButton modeLongBreak;

    private Mode mode = Mode.FOCUS;
    private int remaining = Mode.FOCUS.seconds;
    private int totalForPhase = Mode.FOCUS.seconds;
    private Timeline countdown;
    private boolean running = false;
    private int completedFocusSessions = 0;
    private ResourceBundle bundle;

    private ScaleTransition breath;
    private RotateTransition wiggle;
    private Group bigTomato;

    private double dragOffsetX;
    private double dragOffsetY;

    private final StringProperty timeText = new SimpleStringProperty("25:00");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.bundle = resources;

        bigTomato = PomodoroIcon.large();
        tomatoStack.getChildren().add(bigTomato);

        // Idle gentle breathing
        breath = new ScaleTransition(Duration.millis(1800), tomatoStack);
        breath.setFromX(1.0); breath.setFromY(1.0);
        breath.setToX(1.05);  breath.setToY(1.05);
        breath.setAutoReverse(true);
        breath.setCycleCount(Animation.INDEFINITE);
        breath.setInterpolator(Interpolator.EASE_BOTH);
        breath.play();

        // Subtle wiggle when running
        wiggle = new RotateTransition(Duration.millis(900), tomatoStack);
        wiggle.setFromAngle(-4);
        wiggle.setToAngle(4);
        wiggle.setAutoReverse(true);
        wiggle.setCycleCount(Animation.INDEFINITE);
        wiggle.setInterpolator(Interpolator.EASE_BOTH);

        timeLabel.textProperty().bind(timeText);
        modeFocus.setSelected(true);
        updateUiForMode();
        renderTime();

        // Drag-to-move via header
        headerBar.setOnMousePressed(this::onHeaderPress);
        headerBar.setOnMouseDragged(this::onHeaderDrag);
    }

    private void onHeaderPress(MouseEvent e) {
        Stage st = stage();
        if (st == null) return;
        dragOffsetX = e.getScreenX() - st.getX();
        dragOffsetY = e.getScreenY() - st.getY();
    }

    private void onHeaderDrag(MouseEvent e) {
        Stage st = stage();
        if (st == null) return;
        st.setX(e.getScreenX() - dragOffsetX);
        st.setY(e.getScreenY() - dragOffsetY);
    }

    private Stage stage() {
        return tomatoStack.getScene() == null ? null : (Stage) tomatoStack.getScene().getWindow();
    }

    @FXML public void handleStart() {
        if (running) return;
        running = true;
        startBtn.setVisible(false); startBtn.setManaged(false);
        pauseBtn.setVisible(true);  pauseBtn.setManaged(true);
        hintLabel.setText(t("pomodoro.hint.running"));
        wiggle.playFromStart();

        if (countdown != null) countdown.stop();
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> tick()));
        countdown.setCycleCount(Animation.INDEFINITE);
        countdown.play();
    }

    @FXML public void handlePause() {
        if (!running) return;
        running = false;
        if (countdown != null) countdown.pause();
        wiggle.pause();
        startBtn.setVisible(true); startBtn.setManaged(true);
        pauseBtn.setVisible(false); pauseBtn.setManaged(false);
        hintLabel.setText(t("pomodoro.hint.paused"));
    }

    @FXML public void handleReset() {
        if (countdown != null) countdown.stop();
        running = false;
        wiggle.stop();
        tomatoStack.setRotate(0);
        remaining = mode.seconds;
        totalForPhase = mode.seconds;
        startBtn.setVisible(true); startBtn.setManaged(true);
        pauseBtn.setVisible(false); pauseBtn.setManaged(false);
        hintLabel.setText(t("pomodoro.hint.idle"));
        renderTime();
    }

    @FXML public void handleSkip() { finishPhase(); }

    @FXML public void handleModeFocus()      { switchMode(Mode.FOCUS); }
    @FXML public void handleModeShort()      { switchMode(Mode.SHORT_BREAK); }
    @FXML public void handleModeLong()       { switchMode(Mode.LONG_BREAK); }

    @FXML public void handleClose() {
        Stage st = stage();
        if (st != null) st.hide();
    }

    private void switchMode(Mode m) {
        mode = m;
        if (countdown != null) countdown.stop();
        running = false;
        wiggle.stop();
        tomatoStack.setRotate(0);
        remaining = m.seconds;
        totalForPhase = m.seconds;
        startBtn.setVisible(true); startBtn.setManaged(true);
        pauseBtn.setVisible(false); pauseBtn.setManaged(false);
        modeFocus.setSelected(m == Mode.FOCUS);
        modeShortBreak.setSelected(m == Mode.SHORT_BREAK);
        modeLongBreak.setSelected(m == Mode.LONG_BREAK);
        updateUiForMode();
        renderTime();
        hintLabel.setText(t("pomodoro.hint.idle"));
    }

    private void updateUiForMode() {
        phaseLabel.setText(t(mode.key));
        tomatoStack.getStyleClass().removeAll("phase-focus", "phase-short", "phase-long");
        switch (mode) {
            case FOCUS:       tomatoStack.getStyleClass().add("phase-focus"); break;
            case SHORT_BREAK: tomatoStack.getStyleClass().add("phase-short"); break;
            case LONG_BREAK:  tomatoStack.getStyleClass().add("phase-long");  break;
        }
    }

    private void tick() {
        remaining--;
        if (remaining <= 0) {
            finishPhase();
            return;
        }
        renderTime();
    }

    private void finishPhase() {
        if (countdown != null) countdown.stop();
        running = false;
        wiggle.stop();
        tomatoStack.setRotate(0);

        // Celebrate pop
        ScaleTransition pop = new ScaleTransition(Duration.millis(220), tomatoStack);
        pop.setFromX(1.0); pop.setFromY(1.0);
        pop.setToX(1.18);  pop.setToY(1.18);
        pop.setAutoReverse(true);
        pop.setCycleCount(2);
        pop.play();

        if (mode == Mode.FOCUS) {
            completedFocusSessions++;
            cycleLabel.setText("🍅 " + completedFocusSessions);
            switchMode(completedFocusSessions % 4 == 0 ? Mode.LONG_BREAK : Mode.SHORT_BREAK);
            hintLabel.setText(t("pomodoro.hint.breakReady"));
        } else {
            switchMode(Mode.FOCUS);
            hintLabel.setText(t("pomodoro.hint.focusReady"));
        }

        Platform.runLater(() -> {
            try { java.awt.Toolkit.getDefaultToolkit().beep(); } catch (Exception ignored) {}
        });
    }

    private void renderTime() {
        int m = Math.max(0, remaining) / 60;
        int s = Math.max(0, remaining) % 60;
        timeText.set(String.format("%02d:%02d", m, s));
        double prog = totalForPhase == 0 ? 0 : 1.0 - ((double) remaining / totalForPhase);
        progressBar.setProgress(Math.max(0, Math.min(1, prog)));
    }

    private String t(String key) {
        try {
            return bundle != null && bundle.containsKey(key) ? bundle.getString(key) : key;
        } catch (Exception e) {
            return key;
        }
    }
}
