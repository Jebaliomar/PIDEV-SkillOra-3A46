package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import tn.esprit.tools.Loaders;
import tn.esprit.tools.ThemeManager;

import java.util.Optional;

public class ReasonDialogController {

    public enum Variant {
        DANGER("#dc2626", "#fee2e2", "!"),
        WARNING("#d97706", "#fef3c7", "!"),
        INFO("#2563eb", "#dbeafe", "i");

        public final String accent;
        public final String tint;
        public final String icon;
        Variant(String a, String t, String i) { accent = a; tint = t; icon = i; }
    }

    @FXML private HBox headerBar;
    @FXML private StackPane iconBox;
    @FXML private Label iconLabel;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private TextArea reasonArea;
    @FXML private Label counterLabel;
    @FXML private Label errorLabel;
    @FXML private Button confirmBtn;

    private static final int MAX_LEN = 300;
    private static final int MIN_LEN = 5;
    private Stage stage;
    private boolean confirmed = false;

    @FXML
    private void initialize() {
        reasonArea.textProperty().addListener((o, a, v) -> {
            int len = v == null ? 0 : v.length();
            if (len > MAX_LEN) {
                reasonArea.setText(v.substring(0, MAX_LEN));
                return;
            }
            counterLabel.setText(len + " / " + MAX_LEN);
            hideError();
        });
    }

    @FXML
    public void handleCancel() {
        confirmed = false;
        if (stage != null) stage.close();
    }

    @FXML
    public void handleConfirm() {
        String reason = reasonArea.getText() == null ? "" : reasonArea.getText().trim();
        if (reason.length() < MIN_LEN) {
            showError("Please enter at least " + MIN_LEN + " characters explaining why.");
            return;
        }
        confirmed = true;
        if (stage != null) stage.close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void configure(String title, String subtitle, String confirmText, Variant variant) {
        titleLabel.setText(title);
        if (subtitle == null || subtitle.isBlank()) {
            subtitleLabel.setVisible(false);
            subtitleLabel.setManaged(false);
        } else {
            subtitleLabel.setText(subtitle);
        }
        confirmBtn.setText(confirmText);
        iconLabel.setText(variant.icon);
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, " + variant.accent + ", derive(" + variant.accent + ", 25%));");
        iconBox.setStyle("-fx-background-color: rgba(255,255,255,0.22); -fx-background-radius: 12; -fx-min-width: 44; -fx-min-height: 44;");
        confirmBtn.setStyle(
                "-fx-background-color: " + variant.accent + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 10 22 10 22;" +
                "-fx-background-radius: 10;" +
                "-fx-cursor: hand;"
        );
    }

    public static Optional<String> prompt(Window owner, String title, String subtitle, String confirmText, Variant variant) {
        try {
            FXMLLoader loader = Loaders.loader(ReasonDialogController.class, "/fxml/ReasonDialog.fxml");
            Parent root = loader.load();
            ReasonDialogController ctrl = loader.getController();
            ctrl.configure(title, subtitle, confirmText, variant);

            Stage dialog = new Stage(StageStyle.UTILITY);
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) dialog.initOwner(owner);
            dialog.setTitle(title);
            dialog.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(ReasonDialogController.class.getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            dialog.setScene(scene);
            ctrl.stage = dialog;
            dialog.showAndWait();

            if (ctrl.confirmed) {
                return Optional.of(ctrl.reasonArea.getText().trim());
            }
            return Optional.empty();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
