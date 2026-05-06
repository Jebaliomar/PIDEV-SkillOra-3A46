package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
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
import tn.esprit.controllers.front.SkilloraNavbarController;
import tn.esprit.services.EventService;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SalleService;
import tn.esprit.services.VoiceRecognitionService;
import tn.esprit.tools.AppIcons;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.BotpressSupportWidget;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javafx.util.Duration;

public class SiteEventsController {

    private static final String ALL_TYPES = "All types";
    private static final DateTimeFormatter SITE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final double EVENT_CARD_WIDTH = 380;
    private static final double EVENT_IMAGE_HEIGHT = 210;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private VBox sectionsContainer;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Button micButton;
    @FXML
    private Button stopListeningButton;
    @FXML
    private Label voiceStatusLabel;
    @FXML
    private MenuButton userMenu;
    @FXML
    private MenuItem menuProfile;
    @FXML
    private MenuItem menuSettings;
    @FXML
    private SkilloraNavbarController navbarController;

    private EventService eventService;
    private SalleService salleService;
    private ReservationService reservationService;
    private final VoiceRecognitionService voiceRecognitionService = new VoiceRecognitionService();
    private final Map<Integer, Salle> salleMap = new LinkedHashMap<>();
    private final Map<String, Integer> reservedSeatsByEventSalle = new HashMap<>();
    private CompletableFuture<String> activeVoiceSearch;
    private long voiceSearchGeneration;

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshView());
        typeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshView());

        try {
            eventService = new EventService();
            salleService = new SalleService();
            reservationService = new ReservationService();
            loadData();
        } catch (Exception e) {
            sectionsContainer.getChildren().setAll(new Label("Database unavailable. Start MySQL to load public events."));
            sectionsContainer.getChildren().get(0).getStyleClass().add("site-empty");
            typeFilterCombo.setItems(FXCollections.observableArrayList(ALL_TYPES));
            typeFilterCombo.setValue(ALL_TYPES);
        }

        configureHeaderControls();
        configureNavbar();
        BotpressSupportWidget.installForFrontendPage(sectionsContainer);
    }

    @FXML
    private void onToggleTheme() {
        if (sectionsContainer.getScene() != null) {
            ThemeManager.toggleTheme(sectionsContainer.getScene());
            configureHeaderControls();
        }
    }

    @FXML
    private void onShowHome() {
        AppNavigator.showFrontHome(navigationSource());
    }

    @FXML
    private void onShowCourses() {
        AppNavigator.showFrontBrowseCourses(navigationSource());
    }

    @FXML
    private void onShowEvents() {
        AppNavigator.showFrontEvents(navigationSource());
    }

    @FXML
    private void onShowForum() {
        AppNavigator.showFrontForum(navigationSource());
    }

    @FXML
    private void onShowAssessment() {
        AppNavigator.showFrontAssessment(navigationSource());
    }

    @FXML
    private void onShowProfile() {
        AppNavigator.showFrontProfile(navigationSource());
    }

    @FXML
    private void onShowSettings() {
        AppNavigator.showFrontSettings(navigationSource());
    }

    @FXML
    private void onLogout() {
        AppNavigator.showLogin(navigationSource());
    }

    @FXML
    private void onStartVoiceSearch() {
        if (activeVoiceSearch != null && !activeVoiceSearch.isDone()) {
            showVoiceStatus("Listening...", false);
            return;
        }

        long requestId = ++voiceSearchGeneration;
        setListeningState(true, "Please say the event name to search. Listening...");

        activeVoiceSearch = voiceRecognitionService.recognizeEventSearchText();
        activeVoiceSearch.whenComplete((recognizedText, error) -> Platform.runLater(() -> {
            if (requestId != voiceSearchGeneration) {
                return;
            }

            activeVoiceSearch = null;
            setListeningState(false, null);

            if (error != null) {
                showVoiceStatus("Voice search failed: " + getVoiceErrorMessage(error), true);
                return;
            }

            String query = recognizedText == null ? "" : recognizedText.trim();
            if (query.isEmpty()) {
                showVoiceStatus("No speech recognized. Try again.", true);
                return;
            }

            searchField.setText(query);
            refreshView();
            showVoiceStatus("Recognized: " + query, false);
        }));
    }

    @FXML
    private void onStopVoiceSearch() {
        cancelVoiceSearch();
        setListeningState(false, "Listening stopped.");
    }

    @FXML
    private void onBackToAdmin() {
        cancelVoiceSearch();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/EventDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) sectionsContainer.getScene().getWindow();
            AppWindow.show(stage, scene, "SkillORA Events", false);
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

        reservedSeatsByEventSalle.clear();
        reservedSeatsByEventSalle.putAll(reservationService.getReservedSeatsByEventAndSalle());

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

        imageShell.getChildren().add(createEventImageFallback(event));

        Image image = loadImage(event.getImage());
        if (image != null) {
            imageShell.getChildren().add(createCoverImageView(image, EVENT_CARD_WIDTH, EVENT_IMAGE_HEIGHT));
        }

        Label badge = new Label(safe(event.getEventType()));
        badge.getStyleClass().add("site-event-badge");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(14));

        imageShell.getChildren().add(badge);

        VBox body = new VBox(12);
        body.getStyleClass().add("site-event-body");
        body.setPadding(new Insets(22));

        Label title = new Label(safe(event.getTitle()));
        title.getStyleClass().add("site-event-title");
        title.setWrapText(true);
        title.setMaxWidth(EVENT_CARD_WIDTH - 44);

        Salle salle = event.getSalleId() == null ? null : salleMap.get(event.getSalleId());
        int reservedSeats = getReservedSeats(event, salle);
        int rating = calculateRatingFromReservedSeats(reservedSeats);

        HBox ratingRow = new HBox(8);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        ratingRow.getStyleClass().add("site-event-rating-row");

        Label ratingStars = new Label(generateStars(rating));
        ratingStars.getStyleClass().add("site-event-rating-stars");

        Label reservedSeatsLabel = new Label(formatReservedSeats(reservedSeats));
        reservedSeatsLabel.getStyleClass().add("site-event-rating-count");
        ratingRow.getChildren().addAll(ratingStars, reservedSeatsLabel);

        Label description = new Label(safe(event.getDescription()));
        description.getStyleClass().add("site-event-copy");
        description.setWrapText(true);
        description.setMaxWidth(EVENT_CARD_WIDTH - 44);

        Label location = new Label("Lieu : " + (salle == null ? "-" : safe(salle.getLocation())));
        location.getStyleClass().add("site-event-meta-strong");

        FlowPane metaRow = new FlowPane(8, 8);
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
        body.getChildren().addAll(title, ratingRow, description, location, metaRow, separator, footer);
        card.getChildren().addAll(imageShell, body);
        return card;
    }

    private StackPane createEventImageFallback(Event event) {
        StackPane fallback = new StackPane();
        fallback.getStyleClass().add("site-event-image-fallback");
        fallback.setPrefSize(EVENT_CARD_WIDTH, EVENT_IMAGE_HEIGHT);
        fallback.setMinSize(EVENT_CARD_WIDTH, EVENT_IMAGE_HEIGHT);
        fallback.setMaxSize(EVENT_CARD_WIDTH, EVENT_IMAGE_HEIGHT);

        VBox fallbackContent = new VBox(8);
        fallbackContent.setAlignment(Pos.CENTER);

        Label mark = new Label(buildEventInitial(event));
        mark.getStyleClass().add("site-event-image-mark");

        Label type = new Label(safe(event == null ? null : event.getEventType()));
        type.getStyleClass().add("site-event-image-type");

        fallbackContent.getChildren().addAll(mark, type);
        fallback.getChildren().add(fallbackContent);
        return fallback;
    }

    private String buildEventInitial(Event event) {
        String value = safe(event == null ? null : event.getEventType());
        return "-".equals(value) ? "E" : value.substring(0, 1).toUpperCase();
    }

    private ImageView createCoverImageView(Image image, double width, double height) {
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("site-event-image");

        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        imageView.setClip(clip);

        applyCoverViewport(imageView, image, width, height);
        image.widthProperty().addListener((obs, oldValue, newValue) -> applyCoverViewport(imageView, image, width, height));
        image.heightProperty().addListener((obs, oldValue, newValue) -> applyCoverViewport(imageView, image, width, height));
        image.errorProperty().addListener((obs, wasError, isError) -> {
            imageView.setVisible(!isError);
            imageView.setManaged(!isError);
        });
        if (image.isError()) {
            imageView.setVisible(false);
            imageView.setManaged(false);
        }

        return imageView;
    }

    private void applyCoverViewport(ImageView imageView, Image image, double fitWidth, double fitHeight) {
        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();
        if (imageWidth <= 0 || imageHeight <= 0 || fitWidth <= 0 || fitHeight <= 0) {
            return;
        }

        double imageRatio = imageWidth / imageHeight;
        double targetRatio = fitWidth / fitHeight;
        double viewportWidth = imageWidth;
        double viewportHeight = imageHeight;

        if (imageRatio > targetRatio) {
            viewportWidth = imageHeight * targetRatio;
        } else {
            viewportHeight = imageWidth / targetRatio;
        }

        double x = (imageWidth - viewportWidth) / 2.0;
        double y = (imageHeight - viewportHeight) / 2.0;
        imageView.setViewport(new Rectangle2D(x, y, viewportWidth, viewportHeight));
    }

    private int getReservedSeats(Event event, Salle salle) {
        if (event == null || event.getId() == null) {
            return 0;
        }

        Integer salleId = event.getSalleId() != null ? event.getSalleId() : salle == null ? null : salle.getId();
        if (salleId == null) {
            return 0;
        }

        return reservedSeatsByEventSalle.getOrDefault(reservationService.buildEventSalleKey(event.getId(), salleId), 0);
    }

    private int calculateRatingFromReservedSeats(int reservedSeats) {
        if (reservedSeats <= 0) {
            return 0;
        }
        if (reservedSeats <= 5) {
            return 1;
        }
        if (reservedSeats <= 10) {
            return 2;
        }
        if (reservedSeats <= 15) {
            return 3;
        }
        if (reservedSeats <= 20) {
            return 4;
        }
        return 5;
    }

    private String generateStars(int rating) {
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? "★" : "☆");
        }
        return stars.toString();
    }

    private String formatReservedSeats(int reservedSeats) {
        return reservedSeats == 1 ? "1 reserved seat" : reservedSeats + " reserved seats";
    }

    private void setListeningState(boolean listening, String message) {
        micButton.setDisable(listening);
        stopListeningButton.setVisible(listening);
        stopListeningButton.setManaged(listening);
        if (message != null) {
            showVoiceStatus(message, false);
        }
    }

    private void showVoiceStatus(String message, boolean error) {
        String text = message == null ? "" : message.trim();
        voiceStatusLabel.setText(text);
        voiceStatusLabel.setVisible(!text.isEmpty());
        voiceStatusLabel.setManaged(!text.isEmpty());
        voiceStatusLabel.getStyleClass().remove("site-voice-status-error");
        if (error) {
            voiceStatusLabel.getStyleClass().add("site-voice-status-error");
        }
    }

    private String getVoiceErrorMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unknown error" : message;
    }

    private void cancelVoiceSearch() {
        voiceSearchGeneration++;
        activeVoiceSearch = null;
        voiceRecognitionService.stopListening();
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
        if (source == null || source.isBlank() || "-".equals(source.trim())) {
            return null;
        }
        String value = source.trim();
        try {
            if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:")) {
                return new Image(value, true);
            }
            File file = new File(value);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }
            String relativePath = value.replaceFirst("^[\\\\/]+", "");
            File projectFile = new File(System.getProperty("user.dir"), relativePath);
            if (projectFile.exists()) {
                return new Image(projectFile.toURI().toString(), true);
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
        cancelVoiceSearch();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteSalleDetailView.fxml"));
            Parent root = loader.load();
            SiteSalleDetailController controller = loader.getController();
            controller.setData(event, salle);

            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) sectionsContainer.getScene().getWindow();
            AppWindow.show(stage, scene, "SkillORA Salle Details", false);
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void configureHeaderControls() {
        if (themeToggleButton != null) {
            themeToggleButton.setText("");
            themeToggleButton.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
        }
        if (userMenu != null) {
            userMenu.setText("");
            userMenu.setGraphic(AppIcons.user());
        }
        if (menuProfile != null) {
            menuProfile.setGraphic(AppIcons.user());
        }
        if (menuSettings != null) {
            menuSettings.setGraphic(AppIcons.gear());
        }
    }

    private void configureNavbar() {
        if (navbarController != null) {
            navbarController.setActivePage(SkilloraNavbarController.ActivePage.EVENTS);
            navbarController.setBeforeNavigation(this::cancelVoiceSearch);
        }
    }

    private javafx.scene.Node navigationSource() {
        return themeToggleButton != null ? themeToggleButton : sectionsContainer;
    }
}
