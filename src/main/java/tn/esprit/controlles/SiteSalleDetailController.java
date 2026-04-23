package tn.esprit.controlles;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.entities.SiteReservationFormData;
import tn.esprit.services.SiteSalleDetailService;
import tn.esprit.services.SiteReservationFormService;
import tn.esprit.tools.Model3dHelper;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.util.Duration;

public class SiteSalleDetailController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private Label heroTitleLabel;
    @FXML
    private Button eventLinkButton;
    @FXML
    private Label locationLabel;
    @FXML
    private Label durationValueLabel;
    @FXML
    private Label capacityValueLabel;
    @FXML
    private FlowPane equipmentFlow;
    @FXML
    private Label priceLabel;
    @FXML
    private Label reservedCountLabel;
    @FXML
    private Label availableCountLabel;
    @FXML
    private Label selectedCountLabel;
    @FXML
    private Label seatsSummaryLabel;
    @FXML
    private GridPane seatsGrid;
    @FXML
    private StackPane modelFrame;
    @FXML
    private WebView modelWebView;
    @FXML
    private VBox modelPlaceholderBox;
    @FXML
    private Label modelStatusTitle;
    @FXML
    private Label modelStatusSubtitle;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Button fullscreenButton;
    @FXML
    private Button reservationButton;
    @FXML
    private ImageView coverImageView;
    @FXML
    private StackPane coverFallbackPane;

    private final SiteSalleDetailService detailService = new SiteSalleDetailService();
    private final SiteReservationFormService reservationFormService = new SiteReservationFormService();
    private final Set<Integer> selectedSeats = new LinkedHashSet<>();
    private Set<Integer> reservedSeats = Set.of();

    private Event event;
    private Salle salle;
    private WebEngine webEngine;
    private String pendingModelUrl;
    private String pendingModelName;

    @FXML
    public void initialize() {
        ThemeManager.syncToggleButton(themeToggleButton);
        configureWebView();
        if (reservationButton != null) {
            reservationButton.setDisable(true);
        }
        updateSeatSummary();
    }

    public void setData(Event event, Salle salle) {
        this.event = event;
        this.salle = salle;

        heroTitleLabel.setText(safe(salle == null ? null : salle.getName()));
        eventLinkButton.setText(safe(event == null ? null : event.getTitle()));
        locationLabel.setText(safe(salle == null ? null : salle.getLocation()));
        durationValueLabel.setText(formatInt(salle == null ? null : salle.getDuration()));
        capacityValueLabel.setText(formatInt(salle == null ? null : salle.getMaxParticipants()));
        priceLabel.setText(buildPriceLabel());

        renderEquipment();
        renderCover();
        renderSeats();
        renderModel();
        playIntroAnimations();
    }

    @FXML
    private void onToggleTheme() {
        if (heroTitleLabel.getScene() != null) {
            ThemeManager.toggleTheme(heroTitleLabel.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
            if (pendingModelUrl != null) {
                pushModelToViewer(pendingModelUrl, pendingModelName);
            }
        }
    }

    @FXML
    private void onBackToEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteEventsView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) heroTitleLabel.getScene().getWindow();
            stage.setTitle("Skillora Events");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    @FXML
    private void onOpenFullscreen3d() {
        String modelPath = salle == null ? null : salle.getImage3d();
        if (modelPath == null || modelPath.isBlank()) {
            showError("3D unavailable", "No 3D model is linked to this salle.");
            return;
        }

        try {
            // Keep same behavior as "Voir 3D" button in Salle dashboard.
            Model3dHelper.openPreview(modelPath);
        } catch (Exception e) {
            showError("3D unavailable", e.getMessage());
        }
    }

    @FXML
    private void onOpenReservationForm() {
        if (selectedSeats.isEmpty()) {
            showError("Reservation unavailable", "Select at least one seat before opening the reservation form.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteReservationFormView.fxml"));
            Parent root = loader.load();
            SiteReservationFormController controller = loader.getController();
            controller.setData(event, salle, new ArrayList<>(selectedSeats));

            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) heroTitleLabel.getScene().getWindow();
            stage.setTitle("Skillora Reservation");
            stage.setScene(scene);
            stage.show();
        } catch (Exception exception) {
            showError("Navigation failed", exception.getMessage());
        }
    }

    private void configureWebView() {
        if (modelWebView == null) {
            return;
        }

        webEngine = modelWebView.getEngine();
        modelWebView.setContextMenuEnabled(false);
        modelWebView.setPageFill(Color.TRANSPARENT);

        URL viewerResource = getClass().getResource("/viewsadmin/event/site-salle-model-viewer.html");
        if (viewerResource == null) {
            showModelFallback("No 3D model available", "The embedded 3D viewer page is missing.");
            return;
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SUCCEEDED -> {
                    if (pendingModelUrl != null) {
                        pushModelToViewer(pendingModelUrl, pendingModelName);
                    }
                }
                case FAILED -> showModelFallback("No 3D model available", "JavaFX WebView failed to load the 3D viewer.");
                default -> {
                }
            }
        });

        webEngine.load(viewerResource.toExternalForm());
    }

    private void renderEquipment() {
        equipmentFlow.getChildren().clear();
        String equipmentText = safe(salle == null ? null : salle.getEquipment());
        if ("-".equals(equipmentText)) {
            equipmentFlow.getChildren().add(createChip("No equipment"));
            return;
        }

        for (String part : equipmentText.split(",")) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                equipmentFlow.getChildren().add(createChip(value));
            }
        }

        if (equipmentFlow.getChildren().isEmpty()) {
            equipmentFlow.getChildren().add(createChip(equipmentText));
        }
    }

    private Label createChip(String text) {
        Label chip = new Label(text);
        chip.getStyleClass().add("site-equipment-chip");
        return chip;
    }

    private void renderCover() {
        Image image = loadImage(event == null ? null : event.getImage());
        if (image == null) {
            image = loadImage(salle == null ? null : salle.getImage3d());
        }

        if (image != null) {
            coverImageView.setImage(image);
            coverImageView.setVisible(true);
            coverImageView.setManaged(true);
            coverFallbackPane.setVisible(false);
            coverFallbackPane.setManaged(false);
            Rectangle clip = new Rectangle(coverImageView.getFitWidth(), coverImageView.getFitHeight());
            clip.setArcWidth(36);
            clip.setArcHeight(36);
            coverImageView.setClip(clip);
            ScaleTransition zoom = new ScaleTransition(Duration.millis(420), coverImageView);
            zoom.setFromX(1.04);
            zoom.setFromY(1.04);
            zoom.setToX(1.0);
            zoom.setToY(1.0);
            zoom.play();
        } else {
            coverImageView.setVisible(false);
            coverImageView.setManaged(false);
            coverFallbackPane.setVisible(true);
            coverFallbackPane.setManaged(true);
        }
    }

    private void renderSeats() {
        seatsGrid.getChildren().clear();
        seatsGrid.getColumnConstraints().clear();
        seatsGrid.getRowConstraints().clear();
        selectedSeats.clear();

        int capacity = salle == null || salle.getMaxParticipants() == null ? 0 : Math.max(0, salle.getMaxParticipants());
        List<Reservation> reservations = detailService.loadReservationsForSalle(salle == null ? null : salle.getId());
        reservedSeats = detailService.buildReservedSeatIndexes(salle, reservations);
        SiteSalleDetailService.SeatMatrixSpec spec = detailService.buildSeatMatrix(capacity);

        for (int col = 0; col < spec.columns(); col++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / spec.columns());
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setHalignment(HPos.CENTER);
            seatsGrid.getColumnConstraints().add(constraints);
        }

        for (int row = 0; row < spec.rows(); row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setVgrow(Priority.NEVER);
            constraints.setValignment(VPos.CENTER);
            seatsGrid.getRowConstraints().add(constraints);
        }

        for (int seatNumber = 1; seatNumber <= spec.capacity(); seatNumber++) {
            int row = (seatNumber - 1) / spec.columns();
            int col = (seatNumber - 1) % spec.columns();
            seatsGrid.add(createSeatCell(seatNumber), col, row);
        }

        updateSeatSummary();
    }

    private StackPane createSeatCell(int seatNumber) {
        StackPane seat = new StackPane();
        seat.getStyleClass().add("site-seat");

        Label label = new Label(String.valueOf(seatNumber));
        label.getStyleClass().add("site-seat-label");
        seat.getChildren().add(label);

        if (reservedSeats.contains(seatNumber)) {
            seat.getStyleClass().add("site-seat-reserved");
            return seat;
        }

        seat.getStyleClass().add("site-seat-available");
        seat.setOnMouseClicked(event -> toggleSeatSelection(seatNumber, seat));
        return seat;
    }

    private void toggleSeatSelection(int seatNumber, StackPane seat) {
        if (reservedSeats.contains(seatNumber)) {
            return;
        }

        if (selectedSeats.contains(seatNumber)) {
            selectedSeats.remove(seatNumber);
            seat.getStyleClass().remove("site-seat-selected");
        } else {
            selectedSeats.add(seatNumber);
            if (!seat.getStyleClass().contains("site-seat-selected")) {
                seat.getStyleClass().add("site-seat-selected");
            }
        }

        updateSeatSummary();
    }

    private void updateSeatSummary() {
        int capacity = salle == null || salle.getMaxParticipants() == null ? 0 : Math.max(0, salle.getMaxParticipants());
        int reservedCount = reservedSeats.size();
        int selectedCount = selectedSeats.size();
        int availableCount = Math.max(0, capacity - reservedCount);

        if (reservedCountLabel != null) {
            reservedCountLabel.setText(String.valueOf(reservedCount));
        }
        if (availableCountLabel != null) {
            availableCountLabel.setText(String.valueOf(availableCount));
        }
        if (selectedCountLabel != null) {
            selectedCountLabel.setText(String.valueOf(selectedCount));
        }
        if (seatsSummaryLabel != null) {
            seatsSummaryLabel.setText("Capacity matrix " + capacity + " seats" + (selectedCount > 0 ? " • Selected " + selectedCount : ""));
        }
        if (reservationButton != null) {
            reservationButton.setDisable(selectedCount == 0);
        }
    }

    private void renderModel() {
        pendingModelUrl = null;
        pendingModelName = null;

        if (salle == null || salle.getImage3d() == null || salle.getImage3d().isBlank()) {
            showModelFallback("No 3D model available", "This salle does not have a .glb or .gltf model yet.");
            return;
        }

        try {
            pendingModelUrl = detailService.resolveModelViewerUrl(salle.getImage3d());
            pendingModelName = new File(salle.getImage3d()).getName();

            if (pendingModelUrl == null) {
                showModelFallback("No 3D model available", "Unable to resolve the salle model path.");
                return;
            }

            showModelViewer();
            pushModelToViewer(pendingModelUrl, pendingModelName);
        } catch (Exception exception) {
            showModelFallback("No 3D model available", exception.getMessage());
        }
    }

    private void pushModelToViewer(String modelUrl, String fileName) {
        if (webEngine == null || modelUrl == null) {
            return;
        }

        try {
            webEngine.executeScript(
                    "window.setModelSource(" + quote(modelUrl) + ", " + quote(fileName == null ? "" : fileName) + ");"
            );
            showModelViewer();
        } catch (Exception exception) {
            showModelFallback("No 3D model available", "JavaFX could not render the 3D model in the embedded viewer.");
        }
    }

    private void showModelViewer() {
        modelWebView.setVisible(true);
        modelWebView.setManaged(true);
        modelPlaceholderBox.setVisible(false);
        modelPlaceholderBox.setManaged(false);
        fullscreenButton.setDisable(false);
        if (!modelFrame.getStyleClass().contains("site-model-frame-ready")) {
            modelFrame.getStyleClass().add("site-model-frame-ready");
        }
    }

    private void playIntroAnimations() {
        animateNode(heroTitleLabel, 0);
        animateNode(locationLabel, 1);
        animateNode(durationValueLabel, 2);
        animateNode(capacityValueLabel, 3);
        animateNode(priceLabel, 4);
        animateNode(coverImageView.isVisible() ? coverImageView : coverFallbackPane, 1);
        animateNode(modelFrame, 2);
        animateNode(seatsGrid, 2);
    }

    private void animateNode(javafx.scene.Node node, int delayIndex) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(10);

        Duration duration = Duration.millis(280);
        Duration delay = Duration.millis(Math.min(delayIndex * 60L, 300));

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(delay);

        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setDelay(delay);

        fade.play();
        slide.play();
    }

    private void showModelFallback(String title, String subtitle) {
        modelWebView.setVisible(false);
        modelWebView.setManaged(false);
        modelPlaceholderBox.setVisible(true);
        modelPlaceholderBox.setManaged(true);
        modelStatusTitle.setText(title);
        modelStatusSubtitle.setText(subtitle == null || subtitle.isBlank() ? "No preview available." : subtitle);
        fullscreenButton.setDisable(true);
        modelFrame.getStyleClass().remove("site-model-frame-ready");
        if (webEngine != null) {
            try {
                webEngine.executeScript("window.showEmpty(" + quote(modelStatusSubtitle.getText()) + ");");
            } catch (Exception ignored) {
            }
        }
    }

    private String buildPriceLabel() {
        if (event == null) {
            return "-";
        }

        String raw = safe(event.getPriceType());
        LocalDateTime start = event.getStartDate();
        String datePart = start == null ? "" : " • " + DATE_FORMATTER.format(start);
        return raw + datePart;
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

    private String formatInt(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String quote(String value) {
        String safeValue = value == null ? "" : value;
        return "'" + safeValue
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n") + "'";
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Unknown error" : message);
        alert.showAndWait();
    }
}
