package tn.esprit.controlles;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Salle;
import tn.esprit.services.EventService;
import tn.esprit.services.SalleService;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.LocationMapHelper;
import tn.esprit.tools.Model3dHelper;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class EventDashboardController {

    private static final String ALL_TYPES = "Tous";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter PICKER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private VBox listPane;
    @FXML
    private ScrollPane formPane;
    @FXML
    private Button dashboardNavButton;
    @FXML
    private Button usersNavButton;
    @FXML
    private Button eventsNavButton;
    @FXML
    private Button coursesNavButton;
    @FXML
    private Button statisticsNavButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private VBox cardsContainer;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label deleteFlashLabel;
    @FXML
    private Label formTitleLabel;
    @FXML
    private Label formSubtitleLabel;
    @FXML
    private Label eventIdValueLabel;
    @FXML
    private Label formStatusLabel;
    @FXML
    private Label titleErrorLabel;
    @FXML
    private Label descriptionErrorLabel;
    @FXML
    private Label startDateErrorLabel;
    @FXML
    private Label endDateErrorLabel;
    @FXML
    private Label eventTypeErrorLabel;
    @FXML
    private Label priceTypeErrorLabel;
    @FXML
    private Label priceAmountErrorLabel;
    @FXML
    private Label salleNameErrorLabel;
    @FXML
    private Label salleLocationErrorLabel;
    @FXML
    private Label salleMaxParticipantsErrorLabel;
    @FXML
    private Label salleDurationErrorLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private TextField startTimeField;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextField endTimeField;
    @FXML
    private ComboBox<String> eventTypeField;
    @FXML
    private TextField newTypeField;
    @FXML
    private ComboBox<String> priceTypeField;
    @FXML
    private VBox priceAmountBox;
    @FXML
    private Label priceAmountLabel;
    @FXML
    private TextField priceAmountField;
    @FXML
    private Label priceAmountHintLabel;
    @FXML
    private TextField imageField;
    @FXML
    private ImageView imagePreview;
    @FXML
    private ComboBox<SalleOption> salleComboBox;
    @FXML
    private TextField salleNameField;
    @FXML
    private TextField salleLocationField;
    @FXML
    private VBox salleLocationMapHost;
    @FXML
    private Label salleLocationMapStatusLabel;
    @FXML
    private TextField salleMaxParticipantsField;
    @FXML
    private TextField salleDurationField;
    @FXML
    private TextArea salleEquipmentField;
    @FXML
    private TextField salleImage3dField;

    private final EventService eventService = new EventService();
    private final SalleService salleService = new SalleService();
    private final Map<Integer, Salle> salleMap = new HashMap<>();
    private final Map<String, Label> validationLabels = new HashMap<>();
    private final PauseTransition deleteFlashTimer = new PauseTransition(Duration.seconds(2.8));
    private LocationMapHelper salleLocationMapHelper;
    private Event editingEvent;

    @FXML
    public void initialize() {
        configureDateInputs();

        sortCombo.setItems(FXCollections.observableArrayList(
                "Price Type (A-Z)",
                "Price Type (Z-A)",
                "Title (A-Z)",
                "Title (Z-A)"
        ));
        sortCombo.setValue("Price Type (A-Z)");

        priceTypeField.setItems(FXCollections.observableArrayList("gratuit", "payant"));
        priceTypeField.setValue("gratuit");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> loadEvents());
        typeFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadEvents());
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> loadEvents());
        imageField.textProperty().addListener((obs, oldValue, newValue) -> refreshFormPreview(newValue));
        salleComboBox.valueProperty().addListener((obs, oldValue, newValue) -> populateSalleFields(newValue));
        priceTypeField.valueProperty().addListener((obs, oldValue, newValue) -> syncPriceAmountVisibility());
        salleLocationMapHelper = new LocationMapHelper(salleLocationMapHost, salleLocationMapStatusLabel, salleLocationField);
        salleLocationMapHelper.initialize();
        initializeValidation();

        refreshAll();
        prepareCreateMode();
        showListPane();
        syncPriceAmountVisibility();
        ThemeManager.applyTheme(eventsNavButton.getScene() == null ? null : eventsNavButton.getScene());
        ThemeManager.syncToggleButton(themeToggleButton);
    }

    @FXML
    private void onShowStartCalendar() {
        startDatePicker.show();
    }

    @FXML
    private void onShowEndCalendar() {
        endDatePicker.show();
    }

    @FXML
    private void onToggleTheme() {
        if (themeToggleButton.getScene() != null) {
            ThemeManager.toggleTheme(themeToggleButton.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
        }
    }

    @FXML
    private void onShowEventList() {
        showListPane();
        loadEvents();
    }

    @FXML
    private void onOpenDashboard() {
        AppNavigator.showDashboardAdmin(eventsNavButton);
    }

    @FXML
    private void onOpenUsers() {
        AppNavigator.showUserAdmin(eventsNavButton);
    }

    @FXML
    private void onOpenCourses() {
        AppNavigator.showAdminDashboard(eventsNavButton);
    }

    @FXML
    private void onOpenStatistics() {
        AppNavigator.showStatisticsAdmin(eventsNavButton);
    }

    @FXML
    private void onCreateEvent() {
        prepareCreateMode();
        showFormPane();
    }

    @FXML
    private void onCancelEventForm() {
        prepareCreateMode();
        showListPane();
    }

    @FXML
    private void onAddEventType() {
        String newType = newTypeField.getText() == null ? "" : newTypeField.getText().trim();
        if (newType.isBlank()) {
            return;
        }

        ObservableList<String> items = eventTypeField.getItems();
        if (!containsIgnoreCase(items, newType)) {
            items.add(newType);
        }
        eventTypeField.setValue(newType);
        newTypeField.clear();
    }

    @FXML
    private void onImportImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        Stage owner = imageField.getScene() == null ? null : (Stage) imageField.getScene().getWindow();
        File selected = fileChooser.showOpenDialog(owner);
        if (selected != null) {
            imageField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onImportSalleModel3d() {
        Stage owner = salleImage3dField.getScene() == null ? null : (Stage) salleImage3dField.getScene().getWindow();
        File selected = Model3dHelper.chooseModelFile(owner);
        if (selected != null) {
            salleImage3dField.setText(selected.getAbsolutePath());
        }
    }

    @FXML
    private void onPreviewSalleModel3d() {
        try {
            Model3dHelper.openPreview(salleImage3dField.getText());
        } catch (Exception e) {
            showError("3D preview failed", e.getMessage());
        }
    }

    @FXML
    private void onSaveEvent() {
        try {
            clearValidationState();
            Event eventToPersist = editingEvent == null ? new Event() : editingEvent;

            String eventTypeValue = requiredComboValue(eventTypeField, "Event type", "eventType");
            String priceTypeValue = buildStoredPriceType();
            LocalDateTime start = parseDateTime(startDatePicker, startTimeField, "Start date", "startDate");
            LocalDateTime end = parseDateTime(endDatePicker, endTimeField, "End date", "endDate");

            if (end.isBefore(start)) {
                showValidationError("endDate", "End date must be after start date.");
            }

            eventToPersist.setTitle(required(titleField.getText(), "Title", titleField, "title"));
            eventToPersist.setDescription(required(descriptionField.getText(), "Description", descriptionField, "description"));
            eventToPersist.setStartDate(start);
            eventToPersist.setEndDate(end);
            eventToPersist.setEventType(eventTypeValue);
            eventToPersist.setPriceType(priceTypeValue);
            eventToPersist.setImage(optionalText(imageField.getText()));

            if (editingEvent == null) {
                eventService.add(eventToPersist);
                Integer salleId = resolveSalleId(salleComboBox.getValue(), eventToPersist.getId());
                if (salleId != null) {
                    eventToPersist.setSalleId(salleId);
                    eventService.update(eventToPersist);
                }
                formStatusLabel.setText("Event created");
                showDeleteFlash("Event created successfully.");
            } else if (!eventService.update(eventToPersist)) {
                throw new IllegalArgumentException("Update was not applied.");
            } else {
                Integer salleId = resolveSalleId(salleComboBox.getValue(), eventToPersist.getId());
                eventToPersist.setSalleId(salleId);
                if (!eventService.update(eventToPersist)) {
                    throw new IllegalArgumentException("Salle link update was not applied.");
                }
                formStatusLabel.setText("Event updated");
                showDeleteFlash("Event updated successfully.");
            }

            refreshAll();
            prepareCreateMode();
            showListPane();
        } catch (ValidationException e) {
            formStatusLabel.setText("Please complete the required fields");
        } catch (IllegalArgumentException | SQLException e) {
            showError("Save failed", e.getMessage());
        }
    }

    @FXML
    private void onOpenSalles() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SalleDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setTitle("Skillora Salles");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    @FXML
    private void onBackToSite() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteEventsView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setTitle("SkillHarbor Events");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    @FXML
    private void onOpenReservations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/ReservationDashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/reservations.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setTitle("Skillora Reservations");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    private void refreshAll() {
        loadSalles();
        loadEventTypes();
        loadEvents();
    }

    private void loadSalles() {
        try {
            salleMap.clear();
            List<SalleOption> options = new ArrayList<>();
            options.add(new SalleOption(null, "Aucune salle"));

            for (Salle salle : salleService.getAll()) {
                if (salle.getId() != null) {
                    salleMap.put(salle.getId(), salle);
                    options.add(new SalleOption(salle.getId(), salle.getName() + " - " + safe(salle.getLocation())));
                }
            }

            salleComboBox.setItems(FXCollections.observableArrayList(options));
            if (salleComboBox.getValue() == null && !options.isEmpty()) {
                salleComboBox.setValue(options.get(0));
            }
        } catch (SQLException e) {
            showError("Impossible de charger les salles", e.getMessage());
        }
    }

    private void loadEventTypes() {
        try {
            List<String> items = new ArrayList<>();
            items.add(ALL_TYPES);
            items.addAll(eventService.getDistinctEventTypes());

            String currentFilterValue = typeFilterCombo.getValue();
            typeFilterCombo.setItems(FXCollections.observableArrayList(items));
            if (currentFilterValue == null || !items.contains(currentFilterValue)) {
                typeFilterCombo.setValue(ALL_TYPES);
            } else {
                typeFilterCombo.setValue(currentFilterValue);
            }

            List<String> formItems = new ArrayList<>(items);
            formItems.removeIf(type -> ALL_TYPES.equals(type));
            String currentFormValue = eventTypeField.getValue();
            eventTypeField.setItems(FXCollections.observableArrayList(formItems));
            if (currentFormValue != null && !currentFormValue.isBlank() && !containsIgnoreCase(eventTypeField.getItems(), currentFormValue)) {
                eventTypeField.getItems().add(currentFormValue);
            }
        } catch (SQLException e) {
            showError("Impossible de charger les types", e.getMessage());
        }
    }

    private void loadEvents() {
        try {
            String query = searchField.getText();
            String selectedType = typeFilterCombo.getValue();
            String type = (selectedType == null || ALL_TYPES.equals(selectedType)) ? null : selectedType;

            List<Event> events = eventService.searchAndFilter(query, type);
            sortEvents(events);
            renderRows(events);
        } catch (SQLException e) {
            showError("Impossible de charger les events", e.getMessage());
        }
    }

    private void sortEvents(List<Event> events) {
        String selectedSort = sortCombo.getValue();
        if (selectedSort == null) {
            return;
        }

        switch (selectedSort) {
            case "Price Type (Z-A)" -> events.sort(Comparator.comparing(
                    event -> safe(event.getPriceType()),
                    String.CASE_INSENSITIVE_ORDER.reversed()
            ));
            case "Title (A-Z)" -> events.sort(Comparator.comparing(
                    event -> safe(event.getTitle()),
                    String.CASE_INSENSITIVE_ORDER
            ));
            case "Title (Z-A)" -> events.sort(Comparator.comparing(
                    event -> safe(event.getTitle()),
                    String.CASE_INSENSITIVE_ORDER.reversed()
            ));
            default -> events.sort(Comparator.comparing(
                    event -> safe(event.getPriceType()),
                    String.CASE_INSENSITIVE_ORDER
            ));
        }
    }

    private void renderRows(List<Event> events) {
        cardsContainer.getChildren().clear();

        for (Event event : events) {
            cardsContainer.getChildren().add(createEventRow(event));
        }

        resultCountLabel.setText(events.size() + " event(s)");
    }

    private HBox createEventRow(Event event) {
        HBox row = new HBox(10);
        row.getStyleClass().add("table-row");
        row.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        titleBox.getStyleClass().add("col-title");

        ImageView thumb = buildImagePreview(event.getImage(), 120, 52);
        if (thumb != null) {
            titleBox.getChildren().add(thumb);
        }

        Label title = new Label(safe(event.getTitle()));
        title.getStyleClass().add("row-title");

        Label subtitle = new Label(safe(event.getDescription()));
        subtitle.getStyleClass().add("row-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(270);

        titleBox.getChildren().addAll(title, subtitle);

        Salle salle = event.getSalleId() == null ? null : salleMap.get(event.getSalleId());

        Label dates = new Label(formatDate(event.getStartDate()) + "\n-> " + formatDate(event.getEndDate()));
        dates.getStyleClass().add("col-date");
        dates.setPrefWidth(170);

        Label type = new Label(safe(event.getEventType()));
        type.getStyleClass().add("col-type");
        type.setPrefWidth(120);

        Label price = new Label(safe(event.getPriceType()));
        price.getStyleClass().add("col-price");
        price.setPrefWidth(95);

        Label salleName = new Label(salle == null ? "Unassigned" : safe(salle.getName()));
        salleName.getStyleClass().add("col-salle");
        salleName.setPrefWidth(120);

        HBox actions = new HBox(6);
        actions.getStyleClass().add("col-actions");

        Button detailsButton = new Button("View");
        detailsButton.getStyleClass().add("btn-row-view");
        detailsButton.setOnAction(evt -> openSkillHarborEvents());

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("btn-row-edit");
        editButton.setOnAction(evt -> {
            prepareEditMode(event);
            showFormPane();
        });

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("btn-row-delete");
        deleteButton.setOnAction(evt -> deleteEvent(event));

        actions.getChildren().addAll(detailsButton, editButton, deleteButton);

        HBox.setHgrow(titleBox, Priority.ALWAYS);
        row.getChildren().addAll(titleBox, dates, type, price, salleName, actions);
        return row;
    }

    private void prepareCreateMode() {
        editingEvent = null;
        formTitleLabel.setText("New Event");
        formSubtitleLabel.setText("Create a new event with the same full-page design as Salles.");
        eventIdValueLabel.setText("New");
        formStatusLabel.setText("Creation mode");
        clearValidationState();

        titleField.clear();
        descriptionField.clear();
        startDatePicker.setValue(LocalDate.now());
        startTimeField.setText("09:00");
        endDatePicker.setValue(LocalDate.now());
        endTimeField.setText("10:00");
        eventTypeField.setValue(null);
        newTypeField.clear();
        priceTypeField.setValue("gratuit");
        priceAmountField.clear();
        imageField.clear();
        refreshFormPreview(null);

        if (!salleComboBox.getItems().isEmpty()) {
            salleComboBox.setValue(salleComboBox.getItems().get(0));
        }
        clearSalleFields();
    }

    private void prepareEditMode(Event event) {
        editingEvent = event;
        formTitleLabel.setText("Edit Event");
        formSubtitleLabel.setText("Update the selected event in the same full-page layout as Salles.");
        eventIdValueLabel.setText(event.getId() == null ? "New" : String.valueOf(event.getId()));
        formStatusLabel.setText("Edit mode");
        clearValidationState();

        titleField.setText(safeInput(event.getTitle()));
        descriptionField.setText(safeInput(event.getDescription()));
        startDatePicker.setValue(event.getStartDate() == null ? LocalDate.now() : event.getStartDate().toLocalDate());
        startTimeField.setText(event.getStartDate() == null ? "09:00" : event.getStartDate().toLocalTime().toString());
        endDatePicker.setValue(event.getEndDate() == null ? LocalDate.now() : event.getEndDate().toLocalDate());
        endTimeField.setText(event.getEndDate() == null ? "10:00" : event.getEndDate().toLocalTime().toString());

        String currentType = safeInput(event.getEventType()).trim();
        if (!currentType.isEmpty() && !containsIgnoreCase(eventTypeField.getItems(), currentType)) {
            eventTypeField.getItems().add(currentType);
        }
        eventTypeField.setValue(currentType.isEmpty() ? null : currentType);

        priceTypeField.setValue(safeInput(event.getPriceType()).isEmpty() ? "gratuit" : safeInput(event.getPriceType()));
        applyStoredPriceTypeToForm(safeInput(event.getPriceType()));
        imageField.setText(safeInput(event.getImage()));
        refreshFormPreview(event.getImage());
        salleComboBox.setValue(findSalleOption(event.getSalleId()));
        if (salleComboBox.getValue() == null) {
            populateSalleFields(null);
        }
    }

    private SalleOption findSalleOption(Integer salleId) {
        for (SalleOption option : salleComboBox.getItems()) {
            if ((option.id() == null && salleId == null) || (option.id() != null && option.id().equals(salleId))) {
                return option;
            }
        }
        return salleComboBox.getItems().isEmpty() ? null : salleComboBox.getItems().get(0);
    }

    private void showListPane() {
        listPane.setVisible(true);
        listPane.setManaged(true);
        formPane.setVisible(false);
        formPane.setManaged(false);
        setActiveNav(eventsNavButton);
    }

    private void showFormPane() {
        listPane.setVisible(false);
        listPane.setManaged(false);
        formPane.setVisible(true);
        formPane.setManaged(true);
        setActiveNav(eventsNavButton);
    }

    private void setActiveNav(Button activeButton) {
        eventsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        if (!activeButton.getStyleClass().contains("sidebar-nav-item-active")) {
            activeButton.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private void showDetails(Event event) {
        VBox content = new VBox(12);
        content.getStyleClass().add("details-root");
        content.setPadding(new Insets(20));

        Salle salle = event.getSalleId() == null ? null : salleMap.get(event.getSalleId());

        Label title = new Label(safe(event.getTitle()));
        title.getStyleClass().add("details-title");

        Label description = new Label(safe(event.getDescription()));
        description.setWrapText(true);
        description.getStyleClass().add("details-text");

        Label dateRange = new Label("Periode: " + formatDate(event.getStartDate()) + " -> " + formatDate(event.getEndDate()));
        dateRange.getStyleClass().add("details-text");

        Label type = new Label("Type: " + safe(event.getEventType()));
        type.getStyleClass().add("details-text");

        Label price = new Label("Prix: " + safe(event.getPriceType()));
        price.getStyleClass().add("details-text");

        Label image = new Label("Media: " + safe(event.getImage()));
        image.getStyleClass().add("details-text");

        ImageView detailsImage = buildImagePreview(event.getImage(), 320, 190);

        Label salleTitle = new Label("Salle liee");
        salleTitle.getStyleClass().add("details-subtitle");

        Label salleInfo = new Label(buildSalleDetails(salle));
        salleInfo.setWrapText(true);
        salleInfo.getStyleClass().add("details-text");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("btn-ghost");

        Button editButton = new Button("Modifier");
        editButton.getStyleClass().add("btn-secondary");

        Button deleteButton = new Button("Supprimer");
        deleteButton.getStyleClass().add("btn-danger");

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Details event");

        closeButton.setOnAction(evt -> stage.close());
        editButton.setOnAction(evt -> {
            stage.close();
            prepareEditMode(event);
            showFormPane();
        });
        deleteButton.setOnAction(evt -> {
            stage.close();
            deleteEvent(event);
        });

        actions.getChildren().addAll(closeButton, editButton, deleteButton);

        if (detailsImage != null) {
            content.getChildren().add(detailsImage);
        }
        content.getChildren().addAll(title, description, dateRange, type, price, image, salleTitle, salleInfo, actions);

        Scene scene = new Scene(content, 620, 460);
        scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
        ThemeManager.applyTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void openSkillHarborEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteEventsView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setTitle("SkillHarbor Events");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation impossible", e.getMessage());
        }
    }

    private void deleteEvent(Event event) {
        try {
            boolean deleted = eventService.delete(event.getId());
            if (!deleted) {
                showError("Suppression impossible", "No event was deleted.");
                return;
            }
            refreshAll();
            if (editingEvent != null && event.getId() != null && event.getId().equals(editingEvent.getId())) {
                prepareCreateMode();
                showListPane();
            }
            showDeleteFlash("Event deleted successfully.");
        } catch (SQLException e) {
            showError("Suppression impossible", e.getMessage());
        }
    }

    private String buildSalleDetails(Salle salle) {
        if (salle == null) {
            return "Aucune salle liee.";
        }

        return "Nom: " + safe(salle.getName()) + "\n"
                + "Location: " + safe(salle.getLocation()) + "\n"
                + "Max participants: " + (salle.getMaxParticipants() == null ? "-" : salle.getMaxParticipants()) + "\n"
                + "Duree: " + (salle.getDuration() == null ? "-" : salle.getDuration()) + " minutes\n"
                + "Equipement: " + safe(salle.getEquipment());
    }

    private Integer resolveSalleId(SalleOption option, Integer eventId) throws SQLException {
        String salleName = optionalText(salleNameField.getText());
        String salleLocation = optionalText(salleLocationField.getText());
        String salleEquipment = optionalText(salleEquipmentField.getText());
        String salleImage3d = optionalText(salleImage3dField.getText());

        boolean hasSalleData = !salleName.isBlank() || !salleLocation.isBlank() || !salleEquipment.isBlank() || !salleImage3d.isBlank()
                || !optionalText(salleMaxParticipantsField.getText()).isBlank() || !optionalText(salleDurationField.getText()).isBlank();

        if ((option == null || option.id() == null) && !hasSalleData) {
            return null;
        }

        int maxParticipants = parsePositiveInt(salleMaxParticipantsField.getText(), "Max participants", salleMaxParticipantsField, "salleMaxParticipants");
        int duration = parsePositiveInt(salleDurationField.getText(), "Duree", salleDurationField, "salleDuration");

        if (option == null || option.id() == null) {
            Salle newSalle = new Salle();
            newSalle.setName(required(salleNameField.getText(), "Nom salle", salleNameField, "salleName"));
            newSalle.setLocation(required(salleLocationField.getText(), "Location salle", salleLocationField, "salleLocation"));
            newSalle.setMaxParticipants(maxParticipants);
            newSalle.setDuration(duration);
            newSalle.setEquipment(salleEquipment);
            newSalle.setImage3d(salleImage3d);
            newSalle.setEventId(eventId);
            salleService.add(newSalle);
            return newSalle.getId();
        }

        Salle existingSalle = salleMap.get(option.id());
        if (existingSalle == null) {
            return option.id();
        }

        existingSalle.setName(required(salleNameField.getText(), "Nom salle", salleNameField, "salleName"));
        existingSalle.setLocation(required(salleLocationField.getText(), "Location salle", salleLocationField, "salleLocation"));
        existingSalle.setMaxParticipants(maxParticipants);
        existingSalle.setDuration(duration);
        existingSalle.setEquipment(salleEquipment);
        existingSalle.setImage3d(salleImage3d);
        existingSalle.setEventId(eventId);
        salleService.update(existingSalle);
        return existingSalle.getId();
    }

    private void populateSalleFields(SalleOption option) {
        Salle salle = option == null || option.id() == null ? null : salleMap.get(option.id());
        if (salle == null) {
            clearSalleFields();
            return;
        }

        salleNameField.setText(safeInput(salle.getName()));
        salleLocationField.setText(safeInput(salle.getLocation()));
        if (salleLocationMapHelper != null) {
            salleLocationMapHelper.geocode(safeInput(salle.getLocation()));
        }
        salleMaxParticipantsField.setText(salle.getMaxParticipants() == null ? "" : String.valueOf(salle.getMaxParticipants()));
        salleDurationField.setText(salle.getDuration() == null ? "" : String.valueOf(salle.getDuration()));
        salleEquipmentField.setText(safeInput(salle.getEquipment()));
        salleImage3dField.setText(safeInput(salle.getImage3d()));
    }

    private void clearSalleFields() {
        salleNameField.clear();
        salleLocationField.clear();
        if (salleLocationMapHelper != null) {
            salleLocationMapHelper.setMarker("Tunis, Tunisia", 36.8065, 10.1815);
        }
        salleMaxParticipantsField.setText("50");
        salleDurationField.setText("60");
        salleEquipmentField.clear();
        salleImage3dField.clear();
    }

    private void configureDateInputs() {
        configureDatePicker(startDatePicker);
        configureDatePicker(endDatePicker);

        startTimeField.textProperty().addListener((obs, oldValue, newValue) ->
                normalizeTimeField(startTimeField, oldValue, newValue));
        endTimeField.textProperty().addListener((obs, oldValue, newValue) ->
                normalizeTimeField(endTimeField, oldValue, newValue));
    }

    private void configureDatePicker(DatePicker datePicker) {
        datePicker.setEditable(false);
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : value.format(PICKER_DATE_FORMATTER);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }
                return LocalDate.parse(value, PICKER_DATE_FORMATTER);
            }
        });
    }

    private void normalizeTimeField(TextField field, String oldValue, String newValue) {
        if (newValue == null || newValue.equals(oldValue)) {
            return;
        }

        String digits = newValue.replaceAll("[^0-9]", "");
        if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }

        String normalized = digits;
        if (digits.length() > 2) {
            normalized = digits.substring(0, 2) + ":" + digits.substring(2);
        }

        if (!normalized.equals(newValue)) {
            field.setText(normalized);
            return;
        }

        if (normalized.length() == 5) {
            try {
                LocalTime.parse(normalized);
            } catch (DateTimeParseException e) {
                field.setText(oldValue);
            }
        }
    }

    private void syncPriceAmountVisibility() {
        boolean show = "payant".equalsIgnoreCase(priceTypeField.getValue());
        priceAmountBox.setManaged(show);
        priceAmountBox.setVisible(show);
        priceAmountLabel.setText(show ? "Price amount" : "Prix");
        priceAmountHintLabel.setText(show
                ? "Ajoute le prix en DT. Exemple: 25 ou 25 DT."
                : "Ce champ apparait seulement pour un event payant.");
        if (!show) {
            priceAmountField.clear();
        }
    }

    private String buildStoredPriceType() {
        String selected = requiredComboValue(priceTypeField, "Price type", "priceType");
        if (!"payant".equalsIgnoreCase(selected)) {
            return selected;
        }

        String amount = required(priceAmountField.getText(), "Prix", priceAmountField, "priceAmount");
        String normalizedAmount = amount.replace(',', '.').trim();
        try {
            Double.parseDouble(normalizedAmount.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            showValidationError("priceAmount", "Prix must be numeric, for example 25 or 25 DT.");
        }

        if (!normalizedAmount.toLowerCase().contains("dt")) {
            normalizedAmount = normalizedAmount + " DT";
        }
        return "payant - " + normalizedAmount;
    }

    private void applyStoredPriceTypeToForm(String storedValue) {
        String value = storedValue == null ? "" : storedValue.trim();
        if (value.isEmpty() || "gratuit".equalsIgnoreCase(value)) {
            priceTypeField.setValue("gratuit");
            priceAmountField.clear();
            syncPriceAmountVisibility();
            return;
        }

        if (value.toLowerCase().startsWith("payant")) {
            priceTypeField.setValue("payant");
            String amount = "";
            int dashIndex = value.indexOf('-');
            if (dashIndex >= 0 && dashIndex + 1 < value.length()) {
                amount = value.substring(dashIndex + 1).trim();
            }
            priceAmountField.setText(amount);
            syncPriceAmountVisibility();
            return;
        }

        priceTypeField.setValue(value);
        syncPriceAmountVisibility();
    }

    private LocalDateTime parseDateTime(DatePicker datePicker, TextField timeField, String label, String validationKey) {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            markFieldInvalid(timeField);
            showValidationError(validationKey, label + " is required.");
        }

        String timeText = required(timeField.getText(), label + " time", timeField, validationKey);
        try {
            return LocalDateTime.of(date, LocalTime.parse(timeText));
        } catch (DateTimeParseException e) {
            showValidationError(validationKey, "Invalid time format. Use HH:mm.");
            return LocalDateTime.now();
        }
    }

    private void refreshFormPreview(String source) {
        Image image = loadImage(source, false);
        imagePreview.setImage(image);
        imagePreview.setVisible(image != null);
        imagePreview.setManaged(image != null);
    }

    private ImageView buildImagePreview(String source, double width, double height) {
        Image image = loadImage(source, true);
        if (image == null) {
            return null;
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("card-image");
        return imageView;
    }

    private Image loadImage(String source, boolean backgroundLoading) {
        if (source == null || source.isBlank() || "-".equals(source)) {
            return null;
        }

        String value = source.trim();
        try {
            if (value.startsWith("http://") || value.startsWith("https://") || value.startsWith("file:")) {
                return new Image(value, backgroundLoading);
            }

            File file = new File(value);
            if (!file.exists()) {
                return null;
            }

            return new Image(file.toURI().toString(), backgroundLoading);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_FORMATTER);
    }

    private String required(String value, String label, javafx.scene.Node fieldNode, String validationKey) {
        if (value == null || value.trim().isEmpty()) {
            markFieldInvalid(fieldNode);
            showValidationError(validationKey, label + " is required.");
        }
        clearFieldInvalid(fieldNode);
        return value.trim();
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

    private String requiredComboValue(ComboBox<String> comboBox, String label, String validationKey) {
        String value = comboBox.getValue();
        if (value == null || value.trim().isEmpty()) {
            markFieldInvalid(comboBox);
            showValidationError(validationKey, label + " is required.");
        }
        clearFieldInvalid(comboBox);
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

    private boolean containsIgnoreCase(ObservableList<String> items, String value) {
        if (value == null) {
            return false;
        }
        for (String item : items) {
            if (item != null && item.equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private void initializeValidation() {
        validationLabels.put("title", titleErrorLabel);
        validationLabels.put("description", descriptionErrorLabel);
        validationLabels.put("startDate", startDateErrorLabel);
        validationLabels.put("endDate", endDateErrorLabel);
        validationLabels.put("eventType", eventTypeErrorLabel);
        validationLabels.put("priceType", priceTypeErrorLabel);
        validationLabels.put("priceAmount", priceAmountErrorLabel);
        validationLabels.put("salleName", salleNameErrorLabel);
        validationLabels.put("salleLocation", salleLocationErrorLabel);
        validationLabels.put("salleMaxParticipants", salleMaxParticipantsErrorLabel);
        validationLabels.put("salleDuration", salleDurationErrorLabel);

        bindValidationReset(titleField, "title");
        bindValidationReset(descriptionField, "description");
        bindValidationReset(startTimeField, "startDate");
        bindValidationReset(endTimeField, "endDate");
        bindValidationReset(priceAmountField, "priceAmount");
        bindValidationReset(salleNameField, "salleName");
        bindValidationReset(salleLocationField, "salleLocation");
        bindValidationReset(salleMaxParticipantsField, "salleMaxParticipants");
        bindValidationReset(salleDurationField, "salleDuration");
        eventTypeField.valueProperty().addListener((obs, o, n) -> clearValidationMessage("eventType", eventTypeField));
        priceTypeField.valueProperty().addListener((obs, o, n) -> clearValidationMessage("priceType", priceTypeField));
        startDatePicker.valueProperty().addListener((obs, o, n) -> clearValidationMessage("startDate", startTimeField));
        endDatePicker.valueProperty().addListener((obs, o, n) -> clearValidationMessage("endDate", endTimeField));
    }

    private void bindValidationReset(TextField field, String key) {
        field.textProperty().addListener((obs, o, n) -> clearValidationMessage(key, field));
    }

    private void bindValidationReset(TextArea field, String key) {
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
        clearFieldInvalid(titleField);
        clearFieldInvalid(descriptionField);
        clearFieldInvalid(startTimeField);
        clearFieldInvalid(endTimeField);
        clearFieldInvalid(eventTypeField);
        clearFieldInvalid(priceTypeField);
        clearFieldInvalid(priceAmountField);
        clearFieldInvalid(salleNameField);
        clearFieldInvalid(salleLocationField);
        clearFieldInvalid(salleMaxParticipantsField);
        clearFieldInvalid(salleDurationField);
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Erreur inconnue" : message);
        alert.showAndWait();
    }

    private void showDeleteFlash(String message) {
        if (deleteFlashLabel == null) {
            return;
        }
        deleteFlashTimer.stop();
        deleteFlashLabel.setText("✓  " + message);
        deleteFlashLabel.setVisible(true);
        deleteFlashLabel.setManaged(true);
        deleteFlashTimer.setOnFinished(evt -> {
            deleteFlashLabel.setVisible(false);
            deleteFlashLabel.setManaged(false);
        });
        deleteFlashTimer.playFromStart();
    }

    private record SalleOption(Integer id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ValidationException extends RuntimeException {
    }
}
