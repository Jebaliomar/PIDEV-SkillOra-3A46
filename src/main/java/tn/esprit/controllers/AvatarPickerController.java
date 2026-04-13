package tn.esprit.controllers;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.tools.AvatarService;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AvatarPickerController implements Initializable {

    @FXML private FlowPane grid;
    @FXML private Button tabAll;
    @FXML private Button tabMale;
    @FXML private Button tabFemale;
    @FXML private Button confirmBtn;
    @FXML private Label selectedLabel;

    private String selectedFilename;
    private String confirmedFilename;
    private VBox selectedCard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showAll();
    }

    @FXML public void showAll() {
        List<String> all = new ArrayList<>();
        all.addAll(AvatarService.MALE_AVATARS);
        all.addAll(AvatarService.FEMALE_AVATARS);
        renderGrid(all);
        setActiveTab(tabAll);
    }

    @FXML public void showMale() {
        renderGrid(AvatarService.MALE_AVATARS);
        setActiveTab(tabMale);
    }

    @FXML public void showFemale() {
        renderGrid(AvatarService.FEMALE_AVATARS);
        setActiveTab(tabFemale);
    }

    private void setActiveTab(Button active) {
        for (Button b : new Button[]{tabAll, tabMale, tabFemale}) {
            b.getStyleClass().remove("filter-btn-active");
        }
        if (!active.getStyleClass().contains("filter-btn-active")) {
            active.getStyleClass().add("filter-btn-active");
        }
    }

    private void renderGrid(List<String> filenames) {
        grid.getChildren().clear();
        for (String file : filenames) {
            grid.getChildren().add(buildCard(file));
        }
    }

    private VBox buildCard(String filename) {
        VBox card = new VBox();
        card.getStyleClass().add("avatar-card");
        card.setSpacing(10);
        card.setPadding(new Insets(12));
        card.setPrefWidth(200);
        card.setAlignment(Pos.CENTER);

        StackPane viewerHolder = new StackPane();
        viewerHolder.setPrefSize(184, 220);
        viewerHolder.setMinSize(184, 220);
        viewerHolder.getStyleClass().addAll("avatar-card-viewer", avatarPaletteClass(filename));

        Circle ring = new Circle(74);
        ring.getStyleClass().add("avatar-picker-ring");

        Label emoji = new Label(avatarEmoji(filename));
        emoji.getStyleClass().add("avatar-picker-emoji");

        RotateTransition spin = new RotateTransition(Duration.seconds(8), ring);
        spin.setByAngle(360);
        spin.setInterpolator(Interpolator.LINEAR);
        spin.setCycleCount(RotateTransition.INDEFINITE);
        spin.play();

        viewerHolder.getChildren().addAll(ring, emoji);

        Label name = new Label(AvatarService.displayName(filename));
        name.getStyleClass().add("avatar-card-label");

        card.getChildren().addAll(viewerHolder, name);

        card.setOnMouseClicked(e -> {
            if (selectedCard != null) {
                selectedCard.getStyleClass().remove("avatar-card-selected");
            }
            selectedCard = card;
            if (!card.getStyleClass().contains("avatar-card-selected")) {
                card.getStyleClass().add("avatar-card-selected");
            }
            selectedFilename = filename;
            selectedLabel.setText("Selected: " + AvatarService.displayName(filename));
            confirmBtn.setDisable(false);
        });

        return card;
    }

    @FXML
    public void handleConfirm() {
        confirmedFilename = selectedFilename;
        close();
    }

    @FXML
    public void handleCancel() {
        confirmedFilename = null;
        close();
    }

    private void close() {
        ((Stage) grid.getScene().getWindow()).close();
    }

    public String getConfirmedFilename() {
        return confirmedFilename;
    }

    static String avatarPaletteClass(String filename) {
        if (filename == null) return "avatar-palette-0";
        int hash = Math.abs(filename.hashCode()) % 11;
        return "avatar-palette-" + hash;
    }

    static String avatarEmoji(String filename) {
        if (filename == null) return "?";
        boolean female = filename.toLowerCase().startsWith("female");
        int idx = 0;
        for (char c : filename.toCharArray()) if (Character.isDigit(c)) { idx = c - '0'; break; }
        String[] male = {"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8"};
        String[] fem = {"F1", "F2", "F3"};
        if (female) return fem[Math.min(idx, fem.length - 1)];
        return male[Math.min(idx, male.length - 1)];
    }
}
