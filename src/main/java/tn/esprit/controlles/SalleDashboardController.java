package tn.esprit.controlles;

import javafx.animation.PauseTransition;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.services.EventService;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SalleService;
import tn.esprit.tools.LocationMapHelper;
import tn.esprit.tools.Model3dHelper;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javafx.util.Duration;

public class SalleDashboardController {

    @FXML
    private ScrollPane listPane;
    @FXML
    private ScrollPane formPane;
    @FXML
    private Button eventsNavButton;
    @FXML
    private Button sallesNavButton;
    @FXML
    private Button reservationsNavButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private VBox cardsContainer;
    @FXML
    private Label deleteFlashLabel;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label formSubtitleLabel;
    @FXML
    private Label idValueLabel;
    @FXML
    private Label formStatusLabel;
    @FXML
    private Label nameErrorLabel;
    @FXML
    private Label locationErrorLabel;
    @FXML
    private Label maxParticipantsErrorLabel;
    @FXML
    private Label durationErrorLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField locationField;
    @FXML
    private VBox locationMapHost;
    @FXML
    private Label locationMapStatusLabel;
    @FXML
    private TextField maxParticipantsField;
    @FXML
    private TextField durationField;
    @FXML
    private TextArea equipmentField;
    @FXML
    private TextField image3dField;

    private final SalleService salleService = new SalleService();
    private final EventService eventService = new EventService();
    private final ReservationService reservationService = new ReservationService();
    private final java.util.Map<String, Label> validationLabels = new java.util.HashMap<>();
    private LocationMapHelper locationMapHelper;
    private Salle editingSalle;
    private final PauseTransition deleteFlashTimer = new PauseTransition(Duration.seconds(2.8));

    @FXML
    public void initialize() {
        sortCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "Max Participants (High to Low)",
                "Max Participants (Low to High)",
                "Name (A-Z)",
                "Name (Z-A)"
        ));
        sortCombo.setValue("Max Participants (High to Low)");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> loadSalles());
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadSalles());
        locationMapHelper = new LocationMapHelper(locationMapHost, locationMapStatusLabel, locationField);
        locationMapHelper.initialize();
        initializeValidation();

        loadSalles();
        showListPane();
        prepareCreateMode();
        setActiveNav(sallesNavButton);
        ThemeManager.syncToggleButton(themeToggleButton);
    }

    @FXML
    private void onToggleTheme() {
        if (themeToggleButton.getScene() != null) {
            ThemeManager.toggleTheme(themeToggleButton.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
        }
    }

    @FXML
    private void onShowList() {
        showListPane();
        loadSalles();
    }

    @FXML
    private void onBackToEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/EventDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) listPane.getScene().getWindow();
            stage.setTitle("Skillora Events");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    @FXML
    private void onBackToSite() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteEventsView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            Stage stage = (Stage) listPane.getScene().getWindow();
            stage.setTitle("SkillHarbor Events");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    @FXML
    private void onReservationsPlaceholder() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/ReservationDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/reservations.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) listPane.getScene().getWindow();
            stage.setTitle("Skillora Reservations");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    @FXML
    private void onCreateSalle() {
        prepareCreateMode();
        showFormPane();
    }

    @FXML
    private void onCancel() {
        prepareCreateMode();
        showListPane();
    }

    @FXML
    private void onSaveSalle() {
        try {
            clearValidationState();
            Salle payload = editingSalle == null ? new Salle() : editingSalle;
            payload.setName(required(nameField.getText(), "Nom salle", nameField, "name"));
            payload.setLocation(required(locationField.getText(), "Location salle", locationField, "location"));
            payload.setMaxParticipants(parsePositiveInt(maxParticipantsField.getText(), "Max participants", maxParticipantsField, "maxParticipants"));
            payload.setDuration(parsePositiveInt(durationField.getText(), "Duree", durationField, "duration"));
            payload.setEquipment(optionalText(equipmentField.getText()));
            payload.setImage3d(optionalText(image3dField.getText()));

            if (editingSalle == null) {
                salleService.add(payload);
                formStatusLabel.setText("Salle created");
                showDeleteFlash("Salle enregistree");
            } else if (!salleService.update(payload)) {
                throw new IllegalArgumentException("Update was not applied.");
            } else {
                formStatusLabel.setText("Salle updated");
                showDeleteFlash("Save changes completed.");
            }

            loadSalles();
            prepareCreateMode();
            showListPane();
        } catch (ValidationException e) {
            formStatusLabel.setText("Please complete the required fields");
        } catch (IllegalArgumentException | SQLException e) {
            showError("Save failed", e.getMessage());
        }
    }

    @FXML
    private void onImportModel3d() {
        Stage owner = image3dField.getScene() == null ? null : (Stage) image3dField.getScene().getWindow();
        File selected = tn.esprit.tools.Model3dHelper.chooseModelFile(owner);
        if (selected != null) {
            image3dField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onPreviewModel3d() {
        try {
            Model3dHelper.openPreview(image3dField.getText());
        } catch (Exception e) {
            showError("3D preview failed", e.getMessage());
        }
    }

    private void loadSalles() {
        try {
            List<Salle> salles = new ArrayList<>(salleService.getAll());
            String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            if (!query.isEmpty()) {
                salles.removeIf(salle -> !matchesQuery(salle, query));
            }

            sortSalles(salles);
            renderSalleCards(salles);
        } catch (SQLException e) {
            showError("Load failed", e.getMessage());
        }
    }

    private boolean matchesQuery(Salle salle, String query) {
        return safeInput(salle.getName()).toLowerCase().contains(query)
                || safeInput(salle.getEquipment()).toLowerCase().contains(query)
                || safeInput(salle.getLocation()).toLowerCase().contains(query);
    }

    private void sortSalles(List<Salle> salles) {
        String selectedSort = sortCombo.getValue();
        if (selectedSort == null) {
            return;
        }

        switch (selectedSort) {
            case "Max Participants (Low to High)" -> salles.sort(Comparator.comparing(salle -> safeNumber(salle.getMaxParticipants())));
            case "Name (A-Z)" -> salles.sort(Comparator.comparing(salle -> safeInput(salle.getName()).toLowerCase()));
            case "Name (Z-A)" -> salles.sort(Comparator.comparing((Salle salle) -> safeInput(salle.getName()).toLowerCase()).reversed());
            default -> salles.sort(Comparator.comparing((Salle salle) -> safeNumber(salle.getMaxParticipants())).reversed());
        }
    }

    private void renderSalleCards(List<Salle> salles) throws SQLException {
        cardsContainer.getChildren().clear();
        List<Reservation> reservations = reservationService.getAll();

        for (Salle salle : salles) {
            cardsContainer.getChildren().add(createSalleCard(salle, reservations));
        }
    }

    private VBox createSalleCard(Salle salle, List<Reservation> reservations) {
        List<Reservation> salleReservations = reservations.stream()
                .filter(reservation -> reservation.getSalleId() != null && reservation.getSalleId().equals(salle.getId()))
                .toList();

        VBox card = new VBox(18);
        card.getStyleClass().add("salle-card");
        card.setPadding(new Insets(24));

        HBox topRow = new HBox(18);
        topRow.setAlignment(Pos.TOP_LEFT);

        StackPane mediaBox = new StackPane();
        mediaBox.getStyleClass().add("salle-media-box");
        mediaBox.setMinSize(88, 88);
        mediaBox.setPrefSize(88, 88);
        mediaBox.setMaxSize(88, 88);

        String mediaSource = safeInput(salle.getImage3d()).trim();
        Image image = isImageAsset(mediaSource) ? loadImage(mediaSource) : null;
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(84);
            imageView.setFitHeight(84);
            imageView.setPreserveRatio(true);
            mediaBox.getChildren().add(imageView);
        } else if (isModelAsset(mediaSource)) {
            mediaBox.getChildren().add(createModelThumbnail());
            mediaBox.setOnMouseClicked(event -> previewSalleModel(salle));
        } else {
            Label placeholder = new Label("Salle");
            placeholder.getStyleClass().add("salle-media-placeholder");
            mediaBox.getChildren().add(placeholder);
        }

        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(safe(salle.getName()));
        nameLabel.getStyleClass().add("salle-name");

        Label equipmentLabel = new Label(safe(salle.getEquipment()));
        equipmentLabel.getStyleClass().add("salle-meta");

        Label locationLabel = new Label(safe(salle.getLocation()));
        locationLabel.getStyleClass().add("salle-meta");

        infoBox.getChildren().addAll(nameLabel, equipmentLabel, locationLabel);

        HBox metricsRow = new HBox(22);
        metricsRow.setAlignment(Pos.TOP_RIGHT);

        metricsRow.getChildren().addAll(
                createTopMetric((salle.getMaxParticipants() == null ? 0 : salle.getMaxParticipants()) + " places"),
                createTopMetric((salle.getDuration() == null ? 0 : salle.getDuration()) + " min"),
                createTopMetric(salleReservations.size() + " reservations")
        );

        topRow.getChildren().addAll(mediaBox, infoBox, metricsRow);

        HBox reservationHeader = new HBox();
        reservationHeader.setAlignment(Pos.CENTER_LEFT);
        Label reservationTitle = new Label("Reservations 2026");
        reservationTitle.getStyleClass().add("reservation-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label reservationCount = new Label(salleReservations.size() + " reservations au total");
        reservationCount.getStyleClass().add("reservation-count");
        reservationHeader.getChildren().addAll(reservationTitle, spacer, reservationCount);

        HBox legend = new HBox(12);
        legend.getStyleClass().add("legend-row");
        legend.getChildren().addAll(
                createLegendItem("Aucune", "legend-none"),
                createLegendItem("Faible (1-1)", "legend-low"),
                createLegendItem("Moyen (2-2)", "legend-medium"),
                createLegendItem("Eleve (3-3)", "legend-high"),
                createLegendItem("Maximum (4+)", "legend-max"),
                createLegendItem("Jour d'evenement", "legend-event")
        );

        VBox heatmapWrap = new VBox(8);
        heatmapWrap.getChildren().add(createMonthHeader());
        heatmapWrap.getChildren().add(createHeatmapGrid(salleReservations));

        HBox statsRow = new HBox(14);
        statsRow.getChildren().addAll(
                createStatBox(String.valueOf(countActiveDays(salleReservations)), "Jours actifs"),
                createStatBox(String.valueOf(computeAveragePerDay(salleReservations)), "Moy. / jour"),
                createStatBox(String.valueOf(computeMaxPerDay(salleReservations)), "Max / jour"),
                createStatBox(String.valueOf(countEventDays(salle)), "Jours evenement")
        );

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button editButton = new Button("Modifier");
        editButton.getStyleClass().addAll("btn-row", "btn-chip-action");
        editButton.setOnAction(event -> {
            prepareEditMode(salle);
            showFormPane();
        });

        Button detailsButton = new Button("Voir details");
        detailsButton.getStyleClass().addAll("btn-row", "btn-chip-action");
        detailsButton.setOnAction(event -> openSalleDetail(salle));

        if (isModelAsset(mediaSource)) {
            Button preview3dButton = new Button("Voir 3D");
            preview3dButton.getStyleClass().addAll("btn-row", "btn-chip-action", "btn-chip-model");
            preview3dButton.setOnAction(event -> previewSalleModel(salle));
            actionRow.getChildren().add(preview3dButton);
        }

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().addAll("btn-row", "btn-chip-action");
        deleteButton.setOnAction(event -> deleteSalle(salle));

        actionRow.getChildren().addAll(editButton, detailsButton, deleteButton);

        card.getChildren().addAll(topRow, reservationHeader, legend, heatmapWrap, statsRow, actionRow);
        return card;
    }

    private HBox createTopMetric(String value) {
        HBox box = new HBox();
        box.setAlignment(Pos.CENTER_RIGHT);
        Label label = new Label(value);
        label.getStyleClass().add("top-metric");
        box.getChildren().add(label);
        return box;
    }

    private HBox createLegendItem(String text, String swatchStyle) {
        StackPane swatch = new StackPane();
        swatch.getStyleClass().addAll("legend-swatch", swatchStyle);
        HBox row = new HBox(8, swatch, new Label(text));
        row.getStyleClass().add("legend-item");
        return row;
    }

    private HBox createMonthHeader() {
        HBox row = new HBox(12);
        row.getStyleClass().add("month-row");
        row.getChildren().add(new Label(""));
        String[] months = {"Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan"};
        for (String month : months) {
            Label label = new Label(month);
            label.getStyleClass().add("month-label");
            label.setPrefWidth(68);
            row.getChildren().add(label);
        }
        return row;
    }

    private HBox createHeatmapGrid(List<Reservation> reservations) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(Pos.TOP_LEFT);

        VBox daysCol = new VBox(6);
        String[] dayLabels = {"D", "L", "M", "M", "J", "V", "S"};
        for (String label : dayLabels) {
            Label day = new Label(label);
            day.getStyleClass().add("day-label");
            day.setMinWidth(20);
            daysCol.getChildren().add(day);
        }

        FlowPane grid = new FlowPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPrefWrapLength(1040);
        grid.getStyleClass().add("heatmap-grid");

        LocalDate start = LocalDate.of(2026, 2, 1);
        for (int i = 0; i < 336; i++) {
            LocalDate current = start.plusDays(i);
            int count = countReservationsForDay(reservations, current);
            StackPane cell = new StackPane();
            cell.getStyleClass().addAll("heat-cell", resolveHeatClass(count, current.getDayOfWeek()));
            grid.getChildren().add(cell);
        }

        wrapper.getChildren().addAll(daysCol, grid);
        return wrapper;
    }

    private int countReservationsForDay(List<Reservation> reservations, LocalDate date) {
        int count = 0;
        for (Reservation reservation : reservations) {
            if (reservation.getDateReservation() != null && reservation.getDateReservation().toLocalDate().equals(date)) {
                count++;
            }
        }
        return count;
    }

    private String resolveHeatClass(int count, DayOfWeek dayOfWeek) {
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return "heat-event";
        }
        if (count <= 0) {
            return "heat-none";
        }
        if (count == 1) {
            return "heat-low";
        }
        if (count == 2) {
            return "heat-medium";
        }
        if (count == 3) {
            return "heat-high";
        }
        return "heat-max";
    }

    private VBox createStatBox(String value, String labelText) {
        VBox box = new VBox(4);
        box.getStyleClass().add("stat-box");
        box.setAlignment(Pos.CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        Label label = new Label(labelText);
        label.getStyleClass().add("stat-label");
        box.getChildren().addAll(valueLabel, label);
        return box;
    }

    private int countActiveDays(List<Reservation> reservations) {
        return (int) reservations.stream()
                .filter(reservation -> reservation.getDateReservation() != null)
                .map(reservation -> reservation.getDateReservation().toLocalDate())
                .distinct()
                .count();
    }

    private int computeAveragePerDay(List<Reservation> reservations) {
        int activeDays = countActiveDays(reservations);
        if (activeDays == 0) {
            return 0;
        }
        return Math.round((float) reservations.size() / activeDays);
    }

    private int computeMaxPerDay(List<Reservation> reservations) {
        int max = 0;
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 366; i++) {
            max = Math.max(max, countReservationsForDay(reservations, start.plusDays(i)));
        }
        return max;
    }

    private int countEventDays(Salle salle) {
        return salle.getEventId() == null ? 0 : 62;
    }

    private void openSalleDetail(Salle salle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteSalleDetailView.fxml"));
            Parent root = loader.load();
            SiteSalleDetailController controller = loader.getController();
            controller.setData(resolveLinkedEvent(salle), salle);

            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) listPane.getScene().getWindow();
            stage.setTitle("Skillora Salle Details");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private Event resolveLinkedEvent(Salle salle) throws SQLException {
        if (salle == null) {
            return null;
        }
        if (salle.getEventId() != null) {
            Event byId = eventService.getById(salle.getEventId());
            if (byId != null) {
                return byId;
            }
        }
        return eventService.getAll().stream()
                .filter(event -> event.getSalleId() != null && event.getSalleId().equals(salle.getId()))
                .findFirst()
                .orElse(null);
    }

    private void previewSalleModel(Salle salle) {
        try {
            Model3dHelper.openPreview(salle.getImage3d());
        } catch (Exception e) {
            showError("3D preview failed", e.getMessage());
        }
    }

    private StackPane createModelThumbnail() {
        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("model-thumb-wrap");
        thumbnail.setPrefSize(84, 84);
        thumbnail.setMaxSize(84, 84);

        Polygon topFace = new Polygon(
                18.0, 18.0,
                54.0, 8.0,
                74.0, 26.0,
                38.0, 35.0
        );
        topFace.getStyleClass().add("model-top-face");

        Polygon sideFace = new Polygon(
                38.0, 35.0,
                74.0, 26.0,
                72.0, 50.0,
                36.0, 58.0
        );
        sideFace.getStyleClass().add("model-side-face");

        Polygon frontFace = new Polygon(
                18.0, 18.0,
                38.0, 35.0,
                36.0, 58.0,
                16.0, 43.0
        );
        frontFace.getStyleClass().add("model-front-face");

        Rectangle shadow = new Rectangle(44, 10);
        shadow.getStyleClass().add("model-shadow");
        shadow.setArcWidth(10);
        shadow.setArcHeight(10);
        shadow.setTranslateY(26);
        shadow.setTranslateX(4);

        thumbnail.getChildren().addAll(shadow, frontFace, sideFace, topFace);
        return thumbnail;
    }

    private void deleteSalle(Salle salle) {
        try {
            if (!salleService.delete(salle.getId())) {
                showWarning("Delete", "No salle was deleted.");
                return;
            }
            loadSalles();
            showDeleteFlash("salle deleted successfully");
        } catch (SQLException e) {
            showError("Delete failed", e.getMessage());
        }
    }

    private void prepareCreateMode() {
        editingSalle = null;
        formTitleLabel.setText("Create Salle");
        formSubtitleLabel.setText("Fill in salle information with the same polished layout as Events.");
        idValueLabel.setText("New");
        formStatusLabel.setText("Creation mode");
        clearValidationState();
        clearForm();
    }

    private void prepareEditMode(Salle salle) {
        editingSalle = salle;
        formTitleLabel.setText("Edit Salle");
        formSubtitleLabel.setText("Update the selected salle with the same polished full-page layout as Events.");
        idValueLabel.setText(String.valueOf(salle.getId()));
        formStatusLabel.setText("Edit mode");
        clearValidationState();
        nameField.setText(safeInput(salle.getName()));
        locationField.setText(safeInput(salle.getLocation()));
        if (locationMapHelper != null) {
            locationMapHelper.geocode(safeInput(salle.getLocation()));
        }
        maxParticipantsField.setText(salle.getMaxParticipants() == null ? "" : String.valueOf(salle.getMaxParticipants()));
        durationField.setText(salle.getDuration() == null ? "" : String.valueOf(salle.getDuration()));
        equipmentField.setText(safeInput(salle.getEquipment()));
        image3dField.setText(safeInput(salle.getImage3d()));
    }

    private void showListPane() {
        listPane.setVisible(true);
        listPane.setManaged(true);
        formPane.setVisible(false);
        formPane.setManaged(false);
        setActiveNav(sallesNavButton);
    }

    private void showFormPane() {
        listPane.setVisible(false);
        listPane.setManaged(false);
        formPane.setVisible(true);
        formPane.setManaged(true);
        setActiveNav(sallesNavButton);
    }

    private void setActiveNav(Button activeButton) {
        eventsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        sallesNavButton.getStyleClass().remove("sidebar-nav-item-active");
        reservationsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        if (!activeButton.getStyleClass().contains("sidebar-nav-item-active")) {
            activeButton.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private void clearForm() {
        nameField.clear();
        locationField.clear();
        if (locationMapHelper != null) {
            locationMapHelper.setMarker("Tunis, Tunisia", 36.8065, 10.1815);
        }
        maxParticipantsField.clear();
        durationField.clear();
        equipmentField.clear();
        image3dField.clear();
    }

    private int parsePositiveInt(String value, String label, javafx.scene.Node fieldNode, String validationKey) {
        try {
            int parsed = Integer.parseInt(required(value, label, fieldNode, validationKey));
            if (parsed <= 0) {
                markFieldInvalid(fieldNode);
                showValidationError(validationKey, label + " must be greater than 0.");
            }
            clearFieldInvalid(fieldNode);
            return parsed;
        } catch (NumberFormatException e) {
            markFieldInvalid(fieldNode);
            showValidationError(validationKey, label + " must be numeric.");
            return 0;
        }
    }

    private String required(String value, String label, javafx.scene.Node fieldNode, String validationKey) {
        if (value == null || value.trim().isEmpty()) {
            markFieldInvalid(fieldNode);
            showValidationError(validationKey, label + " is required.");
        }
        clearFieldInvalid(fieldNode);
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeInput(String value) {
        return value == null ? "" : value;
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private Image loadImage(String source) {
        if (source == null || source.isBlank()) {
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

    private boolean isImageAsset(String source) {
        String value = source == null ? "" : source.trim().toLowerCase();
        return value.endsWith(".png")
                || value.endsWith(".jpg")
                || value.endsWith(".jpeg")
                || value.endsWith(".gif")
                || value.endsWith(".webp")
                || value.endsWith(".bmp");
    }

    private boolean isModelAsset(String source) {
        String value = source == null ? "" : source.trim().toLowerCase();
        return value.endsWith(".glb") || value.endsWith(".gltf");
    }

    private void initializeValidation() {
        validationLabels.put("name", nameErrorLabel);
        validationLabels.put("location", locationErrorLabel);
        validationLabels.put("maxParticipants", maxParticipantsErrorLabel);
        validationLabels.put("duration", durationErrorLabel);

        bindValidationReset(nameField, "name");
        bindValidationReset(locationField, "location");
        bindValidationReset(maxParticipantsField, "maxParticipants");
        bindValidationReset(durationField, "duration");
    }

    private void bindValidationReset(TextField field, String key) {
        field.textProperty().addListener((obs, o, n) -> clearValidationMessage(key, field));
    }

    private void clearValidationState() {
        validationLabels.forEach((key, label) -> {
            if (label != null) {
                label.setText("");
                label.setVisible(false);
                label.setManaged(false);
            }
        });
        clearFieldInvalid(nameField);
        clearFieldInvalid(locationField);
        clearFieldInvalid(maxParticipantsField);
        clearFieldInvalid(durationField);
    }

    private void clearValidationMessage(String key, javafx.scene.Node fieldNode) {
        Label label = validationLabels.get(key);
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
        clearFieldInvalid(fieldNode);
    }

    private void showValidationError(String key, String message) {
        Label label = validationLabels.get(key);
        if (label != null) {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
        }
        throw new ValidationException();
    }

    private void markFieldInvalid(javafx.scene.Node node) {
        if (!node.getStyleClass().contains("field-invalid")) {
            node.getStyleClass().add("field-invalid");
        }
    }

    private void clearFieldInvalid(javafx.scene.Node node) {
        node.getStyleClass().remove("field-invalid");
    }

    private void showWarning(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showDeleteFlash(String content) {
        if (deleteFlashLabel == null) {
            return;
        }
        deleteFlashTimer.stop();
        deleteFlashLabel.setText("✓  " + content);
        deleteFlashLabel.setVisible(true);
        deleteFlashLabel.setManaged(true);
        deleteFlashTimer.setOnFinished(evt -> {
            deleteFlashLabel.setVisible(false);
            deleteFlashLabel.setManaged(false);
        });
        deleteFlashTimer.playFromStart();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content == null || content.isBlank() ? "Unknown error" : content);
        alert.showAndWait();
    }

    private static final class ValidationException extends RuntimeException {
    }
}
