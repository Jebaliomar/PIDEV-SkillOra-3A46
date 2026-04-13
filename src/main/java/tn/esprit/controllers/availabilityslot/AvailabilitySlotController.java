package tn.esprit.controllers.availabilityslot;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.services.AvailabilitySlotService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AvailabilitySlotController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String BTN_ACTIVE = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";
    private static final String BTN_INACTIVE = "-fx-background-color: #0f1b38; -fx-text-fill: #cbd5e1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane cardsContainer;

    @FXML
    private Button filterAllBtn;

    @FXML
    private Button filterAvailableBtn;

    @FXML
    private Button filterBookedBtn;

    private final AvailabilitySlotService availabilitySlotService = new AvailabilitySlotService();
    private List<AvailabilitySlot> allSlots = List.of();
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
        setActiveFilter(SlotFilter.ALL);
        refreshSlots();
    }

    @FXML
    private void refreshSlots() {
        try {
            allSlots = availabilitySlotService.getAll();
            selectedSlot = null;
            applyFiltersAndRender();
        } catch (SQLException exception) {
            showError("Unable to load slots", exception);
        }
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
        List<AvailabilitySlot> filtered = allSlots.stream()
                .filter(slot -> matchesFilter(slot, currentFilter))
                .filter(slot -> matchesSearch(slot, q))
                .collect(Collectors.toList());
        renderCards(filtered);
        statusLabel.setText(filtered.size() + " slot(s) shown");
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
            Label empty = new Label("No slots found with current filter.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (AvailabilitySlot slot : slots) {
            cardsContainer.getChildren().add(buildSlotCard(slot));
        }
    }

    private VBox buildSlotCard(AvailabilitySlot slot) {
        boolean booked = Boolean.TRUE.equals(slot.getIsBooked());
        boolean selected = selectedSlot != null && selectedSlot.getId() != null && selectedSlot.getId().equals(slot.getId());

        VBox card = new VBox(8);
        String borderColor = selected ? "#38bdf8" : "#1f3b70";
        card.setStyle("-fx-background-color: #0f1b38; -fx-border-color: " + borderColor + "; -fx-border-width: 1.4; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 14;");
        card.setPrefWidth(540);
        card.setMinWidth(520);

        HBox header = new HBox(8);
        Label idLabel = new Label("Slot #" + slot.getId());
        idLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 18px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(booked ? "RESERVE" : "DISPONIBLE");
        statusBadge.setStyle(booked
                ? "-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 14;"
                : "-fx-background-color: #14532d; -fx-text-fill: #bbf7d0; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 14;");
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        Label line1 = new Label("Prof: " + defaultValue(slot.getProfessorId()) + "   |   " + formatDateTime(slot.getStartAt()) + " -> " + formatDateTime(slot.getEndAt()));
        line1.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
        Label line2 = new Label("Lieu: " + defaultValue(slot.getLocationLabel())
                + "   (lat: " + defaultValue(slot.getLocationLat()) + ", lng: " + defaultValue(slot.getLocationLng()) + ")");
        line2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        card.getChildren().addAll(header, line1, line2);
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
            statusLabel.setText("Slot added successfully");
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
                statusLabel.setText("Slot #" + selectedSlot.getId() + " updated");
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

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete slot #" + selectedSlot.getId() + "?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        confirmation.setHeaderText("Confirm deletion");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            boolean deleted = availabilitySlotService.delete(selectedSlot.getId());
            if (deleted) {
                Integer deletedId = selectedSlot.getId();
                selectedSlot = null;
                refreshSlots();
                statusLabel.setText("Slot #" + deletedId + " deleted");
            } else {
                showWarning("Delete failed", "The selected slot could not be deleted.");
            }
        } catch (SQLException exception) {
            showError("Unable to delete the selected slot", exception);
        }
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

    private Optional<AvailabilitySlot> showSlotDialog(AvailabilitySlot source) {
        boolean editMode = source != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editMode ? "Edit Slot" : "Add Slot");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField professorIdField = new TextField(editMode && source.getProfessorId() != null ? source.getProfessorId().toString() : "");
        TextField startAtField = new TextField(editMode ? formatDateTime(source.getStartAt()) : "");
        TextField endAtField = new TextField(editMode ? formatDateTime(source.getEndAt()) : "");
        TextField bookedField = new TextField(editMode && source.getIsBooked() != null ? source.getIsBooked().toString() : "false");
        TextField locationField = new TextField(editMode ? defaultEmpty(source.getLocationLabel()) : "");
        TextField latField = new TextField(editMode && source.getLocationLat() != null ? source.getLocationLat().toString() : "");
        TextField lngField = new TextField(editMode && source.getLocationLng() != null ? source.getLocationLng().toString() : "");

        grid.add(new Label("Professor ID"), 0, 0);
        grid.add(professorIdField, 1, 0);
        grid.add(new Label("Start (yyyy-MM-dd HH:mm)"), 0, 1);
        grid.add(startAtField, 1, 1);
        grid.add(new Label("End (yyyy-MM-dd HH:mm)"), 0, 2);
        grid.add(endAtField, 1, 2);
        grid.add(new Label("Booked (true/false)"), 0, 3);
        grid.add(bookedField, 1, 3);
        grid.add(new Label("Location"), 0, 4);
        grid.add(locationField, 1, 4);
        grid.add(new Label("Latitude"), 0, 5);
        grid.add(latField, 1, 5);
        grid.add(new Label("Longitude"), 0, 6);
        grid.add(lngField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return Optional.empty();
        }

        try {
            AvailabilitySlot slot = new AvailabilitySlot();
            slot.setProfessorId(parseIntRequired(professorIdField.getText(), "Professor ID"));
            slot.setStartAt(parseDateTimeRequired(startAtField.getText(), "Start"));
            slot.setEndAt(parseDateTimeRequired(endAtField.getText(), "End"));

            if (!slot.getEndAt().isAfter(slot.getStartAt())) {
                showWarning("Validation error", "End date/time must be after start date/time.");
                return Optional.empty();
            }

            slot.setIsBooked(parseBoolean(bookedField.getText()));
            slot.setLocationLabel(toNull(locationField.getText()));
            slot.setLocationLat(parseFloatOptional(latField.getText()));
            slot.setLocationLng(parseFloatOptional(lngField.getText()));
            slot.setCreatedAt(editMode && source.getCreatedAt() != null ? source.getCreatedAt() : LocalDateTime.now());
            return Optional.of(slot);
        } catch (IllegalArgumentException ex) {
            showWarning("Validation error", ex.getMessage());
            return Optional.empty();
        }
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

    private LocalDateTime parseDateTimeRequired(String raw, String fieldName) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " format must be yyyy-MM-dd HH:mm.");
        }
    }

    private Boolean parseBoolean(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase();
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value) || value.isEmpty()) {
            return false;
        }
        throw new IllegalArgumentException("Booked must be true or false.");
    }

    private Float parseFloatOptional(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Latitude/Longitude must be numeric.");
        }
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    private String defaultValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
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
}
