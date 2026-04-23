package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.tools.Avatar3D;
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
        card.setPrefWidth(210);
        card.setAlignment(Pos.CENTER);

        StackPane viewer = Avatar3D.buildViewer(filename, 186, 230);
        viewer.getStyleClass().add("avatar-card-viewer");

        Label name = new Label(AvatarService.displayName(filename));
        name.getStyleClass().add("avatar-card-label");

        card.getChildren().addAll(viewer, name);

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
}
