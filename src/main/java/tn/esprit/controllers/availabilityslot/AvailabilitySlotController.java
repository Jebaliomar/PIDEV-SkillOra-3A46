package tn.esprit.controllers.availabilityslot;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.services.AvailabilitySlotService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AvailabilitySlotController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CARD_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private static final double DEFAULT_MAP_LAT = 36.82364;
    private static final double DEFAULT_MAP_LON = 10.15813;
    private static final int CURRENT_USER_ID = parseCurrentUserId();
    private static final int CARDS_PER_ROW = 3;
    private static final int ROWS_PER_PAGE = 3;
    private static final int CARDS_PER_PAGE = CARDS_PER_ROW * ROWS_PER_PAGE;

    private static final String BTN_ACTIVE = "-fx-background-color: #1d4ed8; -fx-text-fill: #dbeafe; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 14;";
    private static final String BTN_INACTIVE = "-fx-background-color: #172746; -fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 14;";

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private GridPane cardsContainer;

    @FXML
    private Pagination slotsPagination;

    @FXML
    private Button filterAllBtn;

    @FXML
    private Button filterAvailableBtn;

    @FXML
    private Button filterBookedBtn;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label availableCountLabel;

    @FXML
    private Label bookedCountLabel;

    private final AvailabilitySlotService availabilitySlotService = new AvailabilitySlotService();
    private List<AvailabilitySlot> allSlots = List.of();
    private List<AvailabilitySlot> filteredSlots = List.of();
    private AvailabilitySlot selectedSlot;
    private SlotFilter currentFilter = SlotFilter.ALL;

    private enum SlotFilter {
        ALL,
        AVAILABLE,
        BOOKED
    }

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
        if (slotsPagination != null) {
            slotsPagination.currentPageIndexProperty().addListener((obs, oldValue, newValue) -> renderCurrentPage());
        }
        setActiveFilter(SlotFilter.ALL);
        refreshSlots();
    }

    @FXML
    private void refreshSlots() {
        try {
            allSlots = availabilitySlotService.getAll();
            filteredSlots = List.of();
            selectedSlot = null;
            if (slotsPagination != null) {
                slotsPagination.setCurrentPageIndex(0);
            }
            updateSummaryCards();
            applyFiltersAndRender();
        } catch (SQLException exception) {
            showError("Unable to load slots", exception);
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        applyFiltersAndRender();
    }

    @FXML
    private void setFilterAll() {
        setActiveFilter(SlotFilter.ALL);
    }

    @FXML
    private void setFilterAvailable() {
        setActiveFilter(SlotFilter.AVAILABLE);
    }

    @FXML
    private void setFilterBooked() {
        setActiveFilter(SlotFilter.BOOKED);
    }

    private void setActiveFilter(SlotFilter filter) {
        currentFilter = filter;
        filterAllBtn.setStyle(filter == SlotFilter.ALL ? BTN_ACTIVE : BTN_INACTIVE);
        filterAvailableBtn.setStyle(filter == SlotFilter.AVAILABLE ? BTN_ACTIVE : BTN_INACTIVE);
        filterBookedBtn.setStyle(filter == SlotFilter.BOOKED ? BTN_ACTIVE : BTN_INACTIVE);
        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filteredSlots = allSlots.stream()
                .filter(slot -> matchesFilter(slot, currentFilter))
                .filter(slot -> matchesSearch(slot, q))
                .collect(Collectors.toList());
        if (slotsPagination != null) {
            slotsPagination.setCurrentPageIndex(0);
        }
        updatePagination();
        renderCurrentPage();
        updateSummaryCards();
        statusLabel.setText(filteredSlots.size() + " créneau(x) affiché(s)");
    }

    private void updateSummaryCards() {
        long total = allSlots.size();
        long available = allSlots.stream().filter(slot -> !Boolean.TRUE.equals(slot.getIsBooked())).count();
        long booked = allSlots.stream().filter(slot -> Boolean.TRUE.equals(slot.getIsBooked())).count();

        if (totalCountLabel != null) {
            totalCountLabel.setText(String.valueOf(total));
        }
        if (availableCountLabel != null) {
            availableCountLabel.setText(String.valueOf(available));
        }
        if (bookedCountLabel != null) {
            bookedCountLabel.setText(String.valueOf(booked));
        }

        filterAllBtn.setText("Tous");
        filterAvailableBtn.setText("Disponibles");
        filterBookedBtn.setText("Réservés");
    }

    private boolean matchesFilter(AvailabilitySlot slot, SlotFilter filter) {
        boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
        return switch (filter) {
            case ALL -> true;
            case AVAILABLE -> !booked;
            case BOOKED -> booked;
        };
    }

    private boolean matchesSearch(AvailabilitySlot slot, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return String.valueOf(slot.getId()).toLowerCase().contains(q)
                || String.valueOf(slot.getProfessorId()).toLowerCase().contains(q)
                || defaultValue(slot.getLocationLabel()).toLowerCase().contains(q)
                || defaultValue(formatDateTime(slot.getStartAt())).toLowerCase().contains(q)
                || defaultValue(formatDateTime(slot.getEndAt())).toLowerCase().contains(q)
                || String.valueOf(Boolean.TRUE.equals(slot.getIsBooked())).toLowerCase().contains(q);
    }

    private void renderCards(List<AvailabilitySlot> slots) {
        cardsContainer.getChildren().clear();
        if (slots.isEmpty()) {
            Label empty = new Label("Aucun créneau trouvé avec les filtres actuels.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px;");
            cardsContainer.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, CARDS_PER_ROW);
            return;
        }

        int index = 0;
        for (AvailabilitySlot slot : slots) {
            VBox card = buildSlotCard(slot);
            int row = index / CARDS_PER_ROW;
            int col = index % CARDS_PER_ROW;
            cardsContainer.add(card, col, row);
            index++;
        }
    }

    private void updatePagination() {
        if (slotsPagination == null) {
            return;
        }
        int pageCount = Math.max(1, (int) Math.ceil((double) filteredSlots.size() / CARDS_PER_PAGE));
        slotsPagination.setPageCount(pageCount);

        if (slotsPagination.getCurrentPageIndex() >= pageCount) {
            slotsPagination.setCurrentPageIndex(pageCount - 1);
        }

        boolean showPagination = filteredSlots.size() > CARDS_PER_PAGE;
        slotsPagination.setVisible(showPagination);
        slotsPagination.setManaged(showPagination);
    }

    private void renderCurrentPage() {
        if (slotsPagination == null) {
            renderCards(filteredSlots);
            return;
        }

        int pageIndex = Math.max(0, slotsPagination.getCurrentPageIndex());
        int from = pageIndex * CARDS_PER_PAGE;
        int to = Math.min(from + CARDS_PER_PAGE, filteredSlots.size());
        if (from >= filteredSlots.size()) {
            renderCards(List.of());
            return;
        }
        renderCards(filteredSlots.subList(from, to));
    }

    @FXML
    private void goToEvents(ActionEvent event) {
        showWarning("Events", "Events page is not implemented yet.");
    }

    @FXML
    private void goToSlots(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/availability-slot/availability-slot-list.fxml", "SkillOra - Availability Slots");
    }

    @FXML
    private void goToRendezVous(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/rendezvous/rendezvous-list.fxml", "SkillOra - RendezVous");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1160, 740));
            stage.setTitle(title);
            stage.show();
        } catch (IOException exception) {
            showError("Unable to open page", exception);
        }
    }

    private VBox buildSlotCard(AvailabilitySlot slot) {
        boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
        boolean selected = selectedSlot != null && selectedSlot.getId() != null && selectedSlot.getId().equals(slot.getId());

        VBox card = new VBox(0);
        String border = selected ? "#3b82f6" : "#163062";
        card.setStyle("-fx-background-color: #081a42; -fx-border-color: " + border + "; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16;");
        card.setPrefWidth(360);
        card.setMinWidth(360);
        card.setMaxWidth(360);

        StackPane banner = new StackPane();
        banner.setMinHeight(150);
        banner.setPrefHeight(150);
        banner.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 16 16 0 0;");
        Label stamp = new Label(booked ? "UNAVAILABLE" : "AVAILABLE");
        stamp.setRotate(-14);
        stamp.setStyle(booked
                ? "-fx-text-fill: #dc2626; -fx-border-color: #dc2626; -fx-border-width: 4; -fx-font-size: 42px; -fx-font-weight: 900; -fx-padding: 6 20;"
                : "-fx-text-fill: #16a34a; -fx-border-color: #16a34a; -fx-border-width: 4; -fx-font-size: 42px; -fx-font-weight: 900; -fx-padding: 6 20;");
        banner.getChildren().add(stamp);

        VBox body = new VBox(7);
        body.setStyle("-fx-padding: 12 14 14 14;");

        HBox topRow = new HBox(8);
        Label statusBadge = new Label(booked ? "Réservé" : "Disponible");
        statusBadge.setStyle(booked
                ? "-fx-background-color: #1f2f57; -fx-text-fill: #bfdbfe; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 12;"
                : "-fx-background-color: #0f3d35; -fx-text-fill: #86efac; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 12;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label month = new Label(slot.getStartAt() == null ? "-" : slot.getStartAt().format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)));
        month.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px; -fx-font-weight: 700;");
        topRow.getChildren().addAll(statusBadge, spacer, month);

        Label dateLabel = new Label(slot.getStartAt() == null ? "-" : capitalize(slot.getStartAt().format(CARD_DATE_FORMATTER)));
        dateLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 20px; -fx-font-weight: 800;");
        dateLabel.setWrapText(true);

        Label professorLabel = new Label("Prof: " + defaultValue(slot.getProfessorId()));
        professorLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-font-weight: 700;");

        Label locationLabel = new Label("Lieu: " + defaultValue(slot.getLocationLabel()));
        locationLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px;");
        locationLabel.setWrapText(true);

        Label timeLabel = new Label(formatTimeRange(slot.getStartAt(), slot.getEndAt()));
        timeLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 13px; -fx-font-weight: 700;");

        HBox actions = new HBox(8);
        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;");
        editBtn.setOnAction(event -> {
            selectedSlot = slot;
            handleEdit();
        });
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color: #1f2f57; -fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 10;");
        deleteBtn.setOnAction(event -> deleteSlot(slot));
        actions.getChildren().addAll(actionSpacer, editBtn, deleteBtn);

        body.getChildren().addAll(topRow, dateLabel, professorLabel, locationLabel, timeLabel, actions);
        card.getChildren().addAll(banner, body);

        card.setOnMouseClicked(event -> {
            selectedSlot = slot;
            applyFiltersAndRender();
        });
        return card;
    }

    @FXML
    private void handleCreate() {
        Optional<AvailabilitySlot> input = showSlotDialog(null);
        if (input.isEmpty()) {
            return;
        }

        try {
            availabilitySlotService.add(input.get());
            refreshSlots();
            statusLabel.setText("Créneau ajouté avec succès");
        } catch (SQLException exception) {
            showError("Unable to add slot", exception);
        }
    }

    @FXML
    private void handleEdit() {
        if (selectedSlot == null) {
            showWarning("Selection required", "Select a slot card before trying to edit.");
            return;
        }

        Optional<AvailabilitySlot> input = showSlotDialog(selectedSlot);
        if (input.isEmpty()) {
            return;
        }

        AvailabilitySlot updated = input.get();
        updated.setId(selectedSlot.getId());
        try {
            boolean ok = availabilitySlotService.update(updated);
            if (ok) {
                refreshSlots();
                statusLabel.setText("Créneau #" + selectedSlot.getId() + " mis à jour");
            } else {
                showWarning("Update failed", "The selected slot could not be updated.");
            }
        } catch (SQLException exception) {
            showError("Unable to update slot", exception);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedSlot == null) {
            showWarning("Selection required", "Select a slot card before trying to delete.");
            return;
        }
        deleteSlot(selectedSlot);
    }

    private void deleteSlot(AvailabilitySlot slot) {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete slot #" + slot.getId() + "?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        confirmation.setHeaderText("Confirm deletion");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            boolean deleted = availabilitySlotService.delete(slot.getId());
            if (deleted) {
                Integer deletedId = slot.getId();
                selectedSlot = null;
                refreshSlots();
                statusLabel.setText("Créneau #" + deletedId + " supprimé");
            } else {
                showWarning("Delete failed", "The selected slot could not be deleted.");
            }
        } catch (SQLException exception) {
            showError("Unable to delete the selected slot", exception);
        }
    }

    private Optional<AvailabilitySlot> showSlotDialog(AvailabilitySlot source) {
        boolean editMode = source != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editMode ? "Modifier un créneau" : "Ajouter un créneau");

        ButtonType closeType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        Button hiddenClose = (Button) dialog.getDialogPane().lookupButton(closeType);
        if (hiddenClose != null) {
            hiddenClose.setVisible(false);
            hiddenClose.setManaged(false);
        }

        AvailabilitySlot[] builtSlot = new AvailabilitySlot[1];
        AtomicBoolean submitted = new AtomicBoolean(false);
        LocationSuggestion[] selectedLocation = new LocationSuggestion[1];

        String professorIdValue = editMode && source.getProfessorId() != null
                ? source.getProfessorId().toString()
                : String.valueOf(CURRENT_USER_ID);
        TextField professorIdField = buildDialogTextField("ID professeur", professorIdValue);
        professorIdField.setEditable(false);
        professorIdField.setMouseTransparent(true);
        professorIdField.setFocusTraversable(false);
        DatePicker startDatePicker = buildDatePicker(editMode && source.getStartAt() != null ? source.getStartAt().toLocalDate() : LocalDate.now());
        ComboBox<String> startTimeCombo = buildTimeCombo(editMode && source.getStartAt() != null ? source.getStartAt().format(TIME_FORMATTER) : null);
        DatePicker endDatePicker = buildDatePicker(editMode && source.getEndAt() != null ? source.getEndAt().toLocalDate() : LocalDate.now());
        ComboBox<String> endTimeCombo = buildTimeCombo(editMode && source.getEndAt() != null ? source.getEndAt().format(TIME_FORMATTER) : null);

        TextField locationSearchField = buildDialogTextField("Rechercher un lieu...", editMode ? defaultEmpty(source.getLocationLabel()) : "");
        Button searchLocationBtn = new Button("Rechercher");
        searchLocationBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 10;");

        ListView<LocationSuggestion> suggestionList = new ListView<>();
        suggestionList.setPrefHeight(120);
        suggestionList.setStyle("-fx-control-inner-background: #101f40; -fx-background-color: #101f40; -fx-border-color: #27406d; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0;");
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        Label selectedAddressValue = new Label("Aucune adresse sélectionnée");
        selectedAddressValue.setWrapText(true);
        selectedAddressValue.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: 600; -fx-padding: 10; -fx-background-color: #071533; -fx-border-color: #1b3768; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label coordinatesValue = new Label("-");
        coordinatesValue.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px;");

        WebView mapView = new WebView();
        mapView.setContextMenuEnabled(false);
        mapView.setPrefSize(760, 210);
        mapView.setMinSize(760, 210);
        mapView.setMaxSize(Double.MAX_VALUE, 210);

        StackPane mapContainer = new StackPane(mapView);
        mapContainer.setMinHeight(210);
        mapContainer.setPrefHeight(210);
        mapContainer.setStyle("-fx-background-color: #071533; -fx-border-color: #1b3768; -fx-border-radius: 12; -fx-background-radius: 12;");

        double initialLat = DEFAULT_MAP_LAT;
        double initialLon = DEFAULT_MAP_LON;

        if (editMode && source.getLocationLabel() != null && source.getLocationLat() != null && source.getLocationLng() != null) {
            selectedLocation[0] = new LocationSuggestion(source.getLocationLabel(), source.getLocationLat(), source.getLocationLng());
            selectedAddressValue.setText(source.getLocationLabel());
            coordinatesValue.setText(formatCoordinates(source.getLocationLat(), source.getLocationLng()));
            locationSearchField.setText(source.getLocationLabel());
            initialLat = source.getLocationLat();
            initialLon = source.getLocationLng();
        }

        InteractiveMapBridge mapBridge = new InteractiveMapBridge((lat, lon) -> {
            coordinatesValue.setText(formatCoordinates(lat, lon));
            setMapMarker(mapView, lat, lon);
            suggestionList.setVisible(false);
            suggestionList.setManaged(false);

            CompletableFuture.supplyAsync(() -> {
                try {
                    return reverseGeocode(lat, lon);
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }).whenComplete((resolved, throwable) -> Platform.runLater(() -> {
                if (throwable != null || resolved == null) {
                    String fallback = String.format(Locale.US, "Position %.6f, %.6f", lat, lon);
                    selectedLocation[0] = new LocationSuggestion(fallback, lat, lon);
                    locationSearchField.setText(fallback);
                    selectedAddressValue.setText(fallback);
                    return;
                }

                selectedLocation[0] = resolved;
                locationSearchField.setText(resolved.displayName());
                selectedAddressValue.setText(resolved.displayName());
            }));
        });
        initializeInteractiveMap(mapView, initialLat, initialLon, mapBridge);

        HBox locationSearchRow = new HBox(8, locationSearchField, searchLocationBtn);
        HBox.setHgrow(locationSearchField, Priority.ALWAYS);

        searchLocationBtn.setOnAction(event -> launchLocationSearch(
                locationSearchField.getText(),
                suggestionList,
                searchLocationBtn
        ));
        locationSearchField.setOnAction(event -> launchLocationSearch(
                locationSearchField.getText(),
                suggestionList,
                searchLocationBtn
        ));

        suggestionList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            selectedLocation[0] = newValue;
            locationSearchField.setText(newValue.displayName());
            selectedAddressValue.setText(newValue.displayName());
            coordinatesValue.setText(formatCoordinates(newValue.lat(), newValue.lon()));
            setMapMarker(mapView, newValue.lat(), newValue.lon());
            suggestionList.setVisible(false);
            suggestionList.setManaged(false);
        });

        VBox form = new VBox(9);
        form.setStyle("-fx-background-color: #08142d; -fx-padding: 18;");
        form.getChildren().addAll(
                formSectionLabel("Professeur (auto)"), professorIdField,
                formSectionLabel("Date de début"), startDatePicker,
                formSectionLabel("Heure de début"), startTimeCombo,
                formSectionLabel("Date de fin"), endDatePicker,
                formSectionLabel("Heure de fin"), endTimeCombo,
                formSectionLabel("Lieu (optionnel)"), locationSearchRow,
                suggestionList,
                formSectionLabel("Adresse sélectionnée"), selectedAddressValue,
                formSectionLabel("Coordonnées"), coordinatesValue,
                mapContainer
        );

        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setPrefViewportHeight(520);
        formScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-font-size: 15px; -fx-font-weight: 700;");
        cancelBtn.setOnAction(event -> dialog.setResult(closeType));

        Button submitBtn = new Button(editMode ? "Enregistrer" : "Créer le créneau");
        submitBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 8 18;");
        submitBtn.setOnAction(event -> {
            try {
                AvailabilitySlot slot = new AvailabilitySlot();
                Integer professorId = editMode && source.getProfessorId() != null
                        ? source.getProfessorId()
                        : CURRENT_USER_ID;
                slot.setProfessorId(professorId);
                slot.setStartAt(parseDateTimeRequired(startDatePicker, startTimeCombo, "Start"));
                slot.setEndAt(parseDateTimeRequired(endDatePicker, endTimeCombo, "End"));
                if (!slot.getEndAt().isAfter(slot.getStartAt())) {
                    throw new IllegalArgumentException("End date/time must be after start date/time.");
                }

                slot.setIsBooked(editMode && source.getIsBooked() != null ? source.getIsBooked() : false);

                if (selectedLocation[0] != null) {
                    slot.setLocationLabel(selectedLocation[0].displayName());
                    slot.setLocationLat((float) selectedLocation[0].lat());
                    slot.setLocationLng((float) selectedLocation[0].lon());
                } else {
                    slot.setLocationLabel(toNull(locationSearchField.getText()));
                    slot.setLocationLat(null);
                    slot.setLocationLng(null);
                }

                slot.setCreatedAt(editMode && source.getCreatedAt() != null ? source.getCreatedAt() : LocalDateTime.now());
                builtSlot[0] = slot;
                submitted.set(true);
                dialog.setResult(closeType);
            } catch (IllegalArgumentException ex) {
                showWarning("Validation error", ex.getMessage());
            }
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        HBox footer = new HBox(10, footerSpacer, cancelBtn, submitBtn);
        footer.setStyle("-fx-padding: 12 18 16 18; -fx-background-color: #050f24; -fx-border-color: #193565; -fx-border-width: 1 0 0 0;");

        Label icon = new Label("⊕");
        icon.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: #e2e8f0; -fx-font-size: 18px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 12;");
        Label title = new Label(editMode ? "Modifier un créneau" : "Ajouter un créneau");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 34px; -fx-font-weight: 800;");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button closeIconBtn = new Button("✕");
        closeIconBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dbeafe; -fx-font-size: 18px;");
        closeIconBtn.setOnAction(event -> dialog.setResult(closeType));
        HBox header = new HBox(10, icon, title, headerSpacer, closeIconBtn);
        header.setStyle("-fx-alignment: center-left; -fx-padding: 14 18; -fx-background-color: linear-gradient(to right, #2563eb, #3b82f6); -fx-background-radius: 16 16 0 0;");

        VBox dialogCard = new VBox(header, formScroll, footer);
        dialogCard.setStyle("-fx-background-color: #08142d; -fx-border-color: #1b3768; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;");
        dialogCard.setPrefWidth(840);
        dialogCard.setMaxWidth(840);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setContent(dialogCard);

        Node buttonBar = pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
        }

        dialog.showAndWait();
        if (!submitted.get() || builtSlot[0] == null) {
            return Optional.empty();
        }
        return Optional.of(builtSlot[0]);
    }

    private void launchLocationSearch(String query, ListView<LocationSuggestion> suggestionList, Button searchButton) {
        String value = query == null ? "" : query.trim();
        if (value.length() < 3) {
            showWarning("Recherche", "Tapez au moins 3 caractères pour rechercher un lieu.");
            return;
        }

        searchButton.setDisable(true);
        suggestionList.getItems().clear();
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        CompletableFuture.supplyAsync(() -> {
            try {
                return fetchLocationSuggestions(value, 8);
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }).whenComplete((results, throwable) -> Platform.runLater(() -> {
            searchButton.setDisable(false);
            if (throwable != null) {
                String details = throwable.getCause() == null ? throwable.getMessage() : throwable.getCause().getMessage();
                showWarning("API Nominatim", "Unable to fetch location suggestions: " + details);
                return;
            }

            if (results.isEmpty()) {
                showWarning("Recherche", "Aucun résultat trouvé pour ce lieu.");
                return;
            }

            suggestionList.setItems(FXCollections.observableArrayList(results));
            suggestionList.setVisible(true);
            suggestionList.setManaged(true);
        }));
    }

    private List<LocationSuggestion> fetchLocationSuggestions(String query, int limit) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit="
                + limit + "&q=" + encoded;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SkillOraDesktop/1.0 (JavaFX availability module)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Nominatim HTTP " + response.statusCode());
        }

        JSONArray jsonArray = new JSONArray(response.body());
        List<LocationSuggestion> suggestions = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);
            String displayName = item.optString("display_name", "");
            String latRaw = item.optString("lat", "");
            String lonRaw = item.optString("lon", "");
            if (displayName.isBlank() || latRaw.isBlank() || lonRaw.isBlank()) {
                continue;
            }
            try {
                double lat = Double.parseDouble(latRaw);
                double lon = Double.parseDouble(lonRaw);
                suggestions.add(new LocationSuggestion(displayName, lat, lon));
            } catch (NumberFormatException ignored) {
                // Skip malformed coordinates.
            }
        }
        return suggestions;
    }

    private void initializeInteractiveMap(WebView mapView, double lat, double lon, InteractiveMapBridge bridge) {
        mapView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            JSObject window = (JSObject) mapView.getEngine().executeScript("window");
            window.setMember("javaBridge", bridge);
        });
        mapView.getEngine().loadContent(buildInteractiveMapHtml(lat, lon));
    }

    private void setMapMarker(WebView mapView, double lat, double lon) {
        mapView.getEngine().executeScript(String.format(
                Locale.US,
                "if(window.setMapMarker){window.setMapMarker(%.6f, %.6f);}",
                lat,
                lon
        ));
    }

    private String buildInteractiveMapHtml(double lat, double lon) {
        return String.format(Locale.US, """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8"/>
                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <style>
                    html, body, #map { height: 100%%; width: 100%%; margin: 0; padding: 0; background: #0b1f3f; }
                    .leaflet-container { font-family: Arial, sans-serif; }
                  </style>
                </head>
                <body>
                  <div id="map"></div>
                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <script>
                    const map = L.map('map').setView([%.6f, %.6f], 15);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      maxZoom: 19,
                      attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);

                    const marker = L.marker([%.6f, %.6f]).addTo(map);

                    window.setMapMarker = function(lat, lon) {
                      marker.setLatLng([lat, lon]);
                      map.setView([lat, lon], map.getZoom());
                    };

                    map.on('click', function(e) {
                      const lat = e.latlng.lat;
                      const lon = e.latlng.lng;
                      marker.setLatLng([lat, lon]);
                      if (window.javaBridge && typeof window.javaBridge.onMapClicked === 'function') {
                        window.javaBridge.onMapClicked(lat, lon);
                      }
                    });
                  </script>
                </body>
                </html>
                """, lat, lon, lat, lon);
    }

    private LocationSuggestion reverseGeocode(double lat, double lon) throws IOException, InterruptedException {
        String url = String.format(
                Locale.US,
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=%.6f&lon=%.6f",
                lat,
                lon
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "SkillOraDesktop/1.0 (JavaFX availability module)")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Nominatim reverse HTTP " + response.statusCode());
        }

        JSONObject json = new JSONObject(response.body());
        String displayName = json.optString("display_name", "").trim();
        if (displayName.isBlank()) {
            return null;
        }
        return new LocationSuggestion(displayName, lat, lon);
    }

    private TextField buildDialogTextField(String prompt, String value) {
        TextField field = new TextField(value);
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #94a3b8; -fx-font-size: 14px;");
        return field;
    }

    private DatePicker buildDatePicker(LocalDate value) {
        DatePicker picker = new DatePicker(value);
        picker.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
        return picker;
    }

    private ComboBox<String> buildTimeCombo(String selected) {
        ComboBox<String> combo = new ComboBox<>();
        List<String> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            times.add(String.format(Locale.US, "%02d:00", hour));
            times.add(String.format(Locale.US, "%02d:30", hour));
        }
        combo.getItems().setAll(times);
        combo.setPromptText("--:--");
        if (selected != null && !selected.isBlank()) {
            combo.setValue(selected);
        }
        combo.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
        return combo;
    }

    private Label formSectionLabel(String value) {
        Label label = new Label(value);
        label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
        return label;
    }

    private Integer parseIntRequired(String raw, String fieldName) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    private LocalDateTime parseDateTimeRequired(DatePicker datePicker, ComboBox<String> timeCombo, String fieldName) {
        if (datePicker.getValue() == null) {
            throw new IllegalArgumentException(fieldName + " date is required.");
        }
        String timeValue = timeCombo.getValue() == null ? "" : timeCombo.getValue().trim();
        if (timeValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " time is required.");
        }
        try {
            LocalTime localTime = LocalTime.parse(timeValue, TIME_FORMATTER);
            return LocalDateTime.of(datePicker.getValue(), localTime);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " time format must be HH:mm.");
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String formatTimeRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null) {
            return "-";
        }
        return startAt.format(TIME_FORMATTER) + " - " + endAt.format(TIME_FORMATTER);
    }

    private String formatCoordinates(double lat, double lon) {
        return String.format(Locale.US, "Lat: %.6f  |  Lng: %.6f", lat, lon);
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.substring(0, 1).toUpperCase(Locale.ENGLISH) + value.substring(1);
    }

    private String toNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultEmpty(String value) {
        return value == null ? "" : value;
    }

    private String defaultValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static int parseCurrentUserId() {
        String raw = System.getProperty("skillora.userId", "20");
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 20;
        }
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    @FunctionalInterface
    private interface MapSelectionHandler {
        void onLocationSelected(double lat, double lon);
    }

    public static class InteractiveMapBridge {
        private final MapSelectionHandler handler;

        InteractiveMapBridge(MapSelectionHandler handler) {
            this.handler = handler;
        }

        public void onMapClicked(double lat, double lon) {
            if (handler != null) {
                Platform.runLater(() -> handler.onLocationSelected(lat, lon));
            }
        }
    }

    private record LocationSuggestion(String displayName, double lat, double lon) {
        @Override
        public String toString() {
            return displayName;
        }
    }
}
