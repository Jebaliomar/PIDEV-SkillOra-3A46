package tn.esprit.controlles;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Salle;
import tn.esprit.services.EventService;
import tn.esprit.services.SalleService;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Duration;

public class SiteEventsController {

    private static final String ALL_TYPES = "All types";
    private static final DateTimeFormatter SITE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private TextField heroSearchField;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private VBox sectionsContainer;
    @FXML
    private Button themeToggleButton;

    private EventService eventService;
    private SalleService salleService;
    private final Map<Integer, Salle> salleMap = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshView());
        heroSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!searchField.getText().equals(newValue)) {
                searchField.setText(newValue);
            }
        });
        typeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshView());

        try {
            eventService = new EventService();
            salleService = new SalleService();
            loadData();
        } catch (Exception e) {
            sectionsContainer.getChildren().setAll(new Label("Database unavailable. Start MySQL to load public events."));
            sectionsContainer.getChildren().get(0).getStyleClass().add("site-empty");
            typeFilterCombo.setItems(FXCollections.observableArrayList(ALL_TYPES));
            typeFilterCombo.setValue(ALL_TYPES);
        }

        ThemeManager.syncToggleButton(themeToggleButton);
    }

    @FXML
    private void onToggleTheme() {
        if (sectionsContainer.getScene() != null) {
            ThemeManager.toggleTheme(sectionsContainer.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
        }
    }

    @FXML
    private void onBackToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/EventDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) sectionsContainer.getScene().getWindow();
            stage.setTitle("Skillora Events");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void loadData() throws SQLException {
        salleMap.clear();
        for (Salle salle : salleService.getAll()) {
            if (salle.getId() != null) {
                salleMap.put(salle.getId(), salle);
            }
        }

        List<String> types = new ArrayList<>();
        types.add(ALL_TYPES);
        types.addAll(eventService.getDistinctEventTypes());
        typeFilterCombo.setItems(FXCollections.observableArrayList(types));
        typeFilterCombo.setValue(ALL_TYPES);

        refreshView();
    }

    private void refreshView() {
        if (eventService == null) {
            return;
        }

        try {
            String query = searchField.getText();
            String selectedType = typeFilterCombo.getValue();
            String filterType = selectedType == null || ALL_TYPES.equals(selectedType) ? null : selectedType;
            List<Event> events = eventService.searchAndFilter(query, filterType);
            events.sort(Comparator.comparing((Event event) -> safe(event.getEventType())).thenComparing(event -> safe(event.getTitle())));
            renderSections(events);
        } catch (SQLException e) {
            showError("Unable to load events", e.getMessage());
        }
    }

    private void renderSections(List<Event> events) {
        sectionsContainer.getChildren().clear();
        Map<String, List<Event>> grouped = new LinkedHashMap<>();

        for (Event event : events) {
            String key = safe(event.getEventType());
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(event);
        }

        if (grouped.isEmpty()) {
            Label emptyLabel = new Label("No events found for the selected filters.");
            emptyLabel.getStyleClass().add("site-empty");
            sectionsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Map.Entry<String, List<Event>> entry : grouped.entrySet()) {
            VBox section = new VBox(18);
            section.setAlignment(Pos.TOP_CENTER);
            section.getStyleClass().add("site-section-block");
            Label title = new Label(entry.getKey());
            title.getStyleClass().add("site-section-title");

            FlowPane cardsFlow = new FlowPane();
            cardsFlow.getStyleClass().add("site-cards-flow");
            cardsFlow.setAlignment(Pos.TOP_CENTER);
            cardsFlow.prefWrapLengthProperty().bind(sectionsContainer.widthProperty().subtract(28));

            int cardIndex = 0;
            for (Event event : entry.getValue()) {
                VBox card = createEventCard(event);
                attachCardHoverAnimation(card);
                animateCardEntry(card, cardIndex++);
                cardsFlow.getChildren().add(card);
            }

            section.getChildren().addAll(title, cardsFlow);
            sectionsContainer.getChildren().add(section);
        }
    }

    private VBox createEventCard(Event event) {
        VBox card = new VBox();
        card.getStyleClass().add("site-event-card");

        StackPane imageShell = new StackPane();
        imageShell.getStyleClass().add("site-event-image-shell");

        ImageView imageView = new ImageView();
        Image image = loadImage(event.getImage());
        imageView.setFitWidth(395);
        imageView.setFitHeight(235);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("site-event-image");
        if (image != null) {
            imageView.setImage(image);
        }

        Rectangle clip = new Rectangle(395, 235);
        clip.setArcWidth(26);
        clip.setArcHeight(26);
        imageView.setClip(clip);

        Label badge = new Label(safe(event.getEventType()));
        badge.getStyleClass().add("site-event-badge");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(14));

        imageShell.getChildren().addAll(imageView, badge);

        VBox body = new VBox(12);
        body.getStyleClass().add("site-event-body");
        body.setPadding(new Insets(22));

        Label title = new Label(safe(event.getTitle()));
        title.getStyleClass().add("site-event-title");

        Label description = new Label(safe(event.getDescription()));
        description.getStyleClass().add("site-event-copy");
        description.setWrapText(true);

        Salle salle = event.getSalleId() == null ? null : salleMap.get(event.getSalleId());

        Label location = new Label("Lieu : " + (salle == null ? "-" : safe(salle.getLocation())));
        location.getStyleClass().add("site-event-meta-strong");

        HBox metaRow = new HBox(12);
        metaRow.getStyleClass().add("site-event-meta-row");

        Label date = new Label("Date : " + formatDate(event.getStartDate()));
        date.getStyleClass().add("site-event-meta");

        Label price = new Label("Prix : " + safe(event.getPriceType()));
        price.getStyleClass().add("site-event-meta");

        Label participants = new Label("Participants max : " + (salle == null || salle.getMaxParticipants() == null ? "-" : salle.getMaxParticipants()));
        participants.getStyleClass().add("site-event-meta");

        metaRow.getChildren().addAll(date, price, participants);

        Separator separator = new Separator();
        separator.getStyleClass().add("site-card-separator");

        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label salleName = new Label("Salle: " + (salle == null ? "-" : safe(salle.getName())));
        salleName.getStyleClass().add("site-salle-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button openButton = new Button("→");
        openButton.getStyleClass().add("site-open-button");
        openButton.setOnAction(actionEvent -> openSalleDetail(event, salle));

        footer.getChildren().addAll(salleName, spacer, openButton);
        body.getChildren().addAll(title, description, location, metaRow, separator, footer);
        card.getChildren().addAll(imageShell, body);
        return card;
    }

    private void animateCardEntry(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(14);

        Duration duration = Duration.millis(320);
        Duration delay = Duration.millis(Math.min(index * 55L, 360));

        FadeTransition fade = new FadeTransition(duration, card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(delay);

        TranslateTransition slide = new TranslateTransition(duration, card);
        slide.setFromY(14);
        slide.setToY(0);
        slide.setDelay(delay);

        fade.play();
        slide.play();
    }

    private void attachCardHoverAnimation(VBox card) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(170), card);
        grow.setToX(1.02);
        grow.setToY(1.02);

        ScaleTransition shrink = new ScaleTransition(Duration.millis(170), card);
        shrink.setToX(1.0);
        shrink.setToY(1.0);

        card.setOnMouseEntered(evt -> {
            shrink.stop();
            grow.playFromStart();
        });
        card.setOnMouseExited(evt -> {
            grow.stop();
            shrink.playFromStart();
        });
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : value.format(SITE_DATE_FORMATTER);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Image loadImage(String source) {
        if (source == null || source.isBlank() || "-".equals(source)) {
            return null;
        }
        try {
            if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file:")) {
                return new Image(source, true);
            }
            File file = new File(source);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Unknown error" : message);
        alert.showAndWait();
    }

    private void openSalleDetail(Event event, Salle salle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteSalleDetailView.fxml"));
            Parent root = loader.load();
            SiteSalleDetailController controller = loader.getController();
            controller.setData(event, salle);

            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) sectionsContainer.getScene().getWindow();
            stage.setTitle("Skillora Salle Details");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }
}
