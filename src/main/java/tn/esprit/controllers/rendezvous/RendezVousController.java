package tn.esprit.controllers.rendezvous;

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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.RendezVous;
import tn.esprit.services.RendezVousService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RendezVousController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String BTN_ACTIVE = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";
    private static final String BTN_INACTIVE = "-fx-background-color: #0f1b38; -fx-text-fill: #cbd5e1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private VBox cardsContainer;

    @FXML
    private Button filterAllBtn;

    @FXML
    private Button filterPendingBtn;

    @FXML
    private Button filterFinalBtn;

    private final RendezVousService rendezVousService = new RendezVousService();
    private List<RendezVous> allRendezVous = List.of();
    private RendezVous selectedRendezVous;
    private RendezVousFilter currentFilter = RendezVousFilter.ALL;

    private enum RendezVousFilter {
        ALL,
        PENDING,
        FINAL
    }

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
        setActiveFilter(RendezVousFilter.ALL);
        refreshRendezVous();
    }

    @FXML
    private void refreshRendezVous() {
        try {
            allRendezVous = rendezVousService.getAll();
            selectedRendezVous = null;
            applyFiltersAndRender();
        } catch (SQLException exception) {
            showError("Unable to load rendez-vous", exception);
        }
    }

    @FXML
    private void setFilterAll() {
        setActiveFilter(RendezVousFilter.ALL);
    }

    @FXML
    private void setFilterPending() {
        setActiveFilter(RendezVousFilter.PENDING);
    }

    @FXML
    private void setFilterFinal() {
        setActiveFilter(RendezVousFilter.FINAL);
    }

    private void setActiveFilter(RendezVousFilter filter) {
        currentFilter = filter;
        filterAllBtn.setStyle(filter == RendezVousFilter.ALL ? BTN_ACTIVE : BTN_INACTIVE);
        filterPendingBtn.setStyle(filter == RendezVousFilter.PENDING ? BTN_ACTIVE : BTN_INACTIVE);
        filterFinalBtn.setStyle(filter == RendezVousFilter.FINAL ? BTN_ACTIVE : BTN_INACTIVE);
        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<RendezVous> filtered = allRendezVous.stream()
                .filter(rdv -> matchesFilter(rdv, currentFilter))
                .filter(rdv -> matchesSearch(rdv, q))
                .collect(Collectors.toList());
        renderCards(filtered);
        statusLabel.setText(filtered.size() + " rendez-vous shown");
    }

    private boolean matchesFilter(RendezVous rdv, RendezVousFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case PENDING -> isPendingStatus(rdv.getStatut());
            case FINAL -> !isPendingStatus(rdv.getStatut());
        };
    }

    private boolean matchesSearch(RendezVous rdv, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return String.valueOf(rdv.getId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getStudentId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getProfessorId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getSlotId()).toLowerCase().contains(q)
                || normalizeDefault(rdv.getStatut(), "-").toLowerCase().contains(q)
                || normalizeDefault(rdv.getMeetingType(), "-").toLowerCase().contains(q)
                || normalizeDefault(formatDateTime(rdv.getCreatedAt()), "-").toLowerCase().contains(q);
    }

    private void renderCards(List<RendezVous> rendezVousList) {
        cardsContainer.getChildren().clear();
        if (rendezVousList.isEmpty()) {
            Label empty = new Label("No rendez-vous found with current filter.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (RendezVous rdv : rendezVousList) {
            cardsContainer.getChildren().add(buildRendezVousCard(rdv));
        }
    }

    private VBox buildRendezVousCard(RendezVous rdv) {
        boolean selected = selectedRendezVous != null
                && selectedRendezVous.getId() != null
                && selectedRendezVous.getId().equals(rdv.getId());

        VBox card = new VBox(8);
        String borderColor = selected ? "#38bdf8" : "#1f3b70";
        card.setStyle("-fx-background-color: #0f1b38; -fx-border-color: " + borderColor + "; -fx-border-width: 1.4; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 14;");

        HBox header = new HBox(8);
        Label idLabel = new Label("RendezVous #" + rdv.getId());
        idLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 18px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(normalizeDefault(rdv.getStatut(), "unknown"));
        statusBadge.setStyle(statusStyle(rdv.getStatut()));
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        Label line1 = new Label("Student: " + safeNumber(rdv.getStudentId()) + " | Professor: " + safeNumber(rdv.getProfessorId()) + " | Slot: " + safeNumber(rdv.getSlotId()));
        line1.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
        Label line2 = new Label("Type: " + normalizeDefault(rdv.getMeetingType(), "-")
                + " | Created: " + formatDateTime(rdv.getCreatedAt()));
        line2.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        Label line3 = new Label("Message: " + normalizeDefault(rdv.getMessage(), "-"));
        line3.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px;");

        card.getChildren().addAll(header, line1, line2, line3);
        card.setOnMouseClicked(event -> {
            selectedRendezVous = rdv;
            applyFiltersAndRender();
        });
        return card;
    }

    private String statusStyle(String rawStatus) {
        String status = normalizeDefault(rawStatus, "").toLowerCase();
        if (status.contains("attente")) {
            return "-fx-background-color: #78350f; -fx-text-fill: #fcd34d; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 14;";
        }
        if (status.contains("confirm")) {
            return "-fx-background-color: #14532d; -fx-text-fill: #bbf7d0; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 14;";
        }
        if (status.contains("refus") || status.contains("rejet")) {
            return "-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 14;";
        }
        return "-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-weight: 700; -fx-padding: 4 10; -fx-background-radius: 14;";
    }

    @FXML
    private void handleCreate() {
        Optional<RendezVous> input = showRendezVousDialog(null);
        if (input.isEmpty()) {
            return;
        }

        try {
            rendezVousService.add(input.get());
            refreshRendezVous();
            statusLabel.setText("Rendez-vous added successfully");
        } catch (SQLException exception) {
            showError("Unable to add rendez-vous", exception);
        }
    }

    @FXML
    private void handleEdit() {
        if (selectedRendezVous == null) {
            showWarning("Selection required", "Select a rendez-vous card before trying to edit.");
            return;
        }

        Optional<RendezVous> input = showRendezVousDialog(selectedRendezVous);
        if (input.isEmpty()) {
            return;
        }

        RendezVous updated = input.get();
        updated.setId(selectedRendezVous.getId());
        try {
            boolean ok = rendezVousService.update(updated);
            if (ok) {
                refreshRendezVous();
                statusLabel.setText("Rendez-vous #" + selectedRendezVous.getId() + " updated");
            } else {
                showWarning("Update failed", "The selected rendez-vous could not be updated.");
            }
        } catch (SQLException exception) {
            showError("Unable to update rendez-vous", exception);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedRendezVous == null) {
            showWarning("Selection required", "Select a rendez-vous card before trying to delete.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete rendez-vous #" + selectedRendezVous.getId() + "?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        confirmation.setHeaderText("Confirm deletion");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            boolean deleted = rendezVousService.delete(selectedRendezVous.getId());
            if (deleted) {
                Integer deletedId = selectedRendezVous.getId();
                selectedRendezVous = null;
                refreshRendezVous();
                statusLabel.setText("Rendez-vous #" + deletedId + " deleted");
            } else {
                showWarning("Delete failed", "The selected rendez-vous could not be deleted.");
            }
        } catch (SQLException exception) {
            showError("Unable to delete the selected rendez-vous", exception);
        }
    }

    @FXML
    private void goToMenu(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/menu.fxml", "SkillOra - Menu");
    }

    @FXML
    private void goToRendezVous(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/rendezvous/rendezvous-list.fxml", "SkillOra - RendezVous");
    }

    @FXML
    private void goToSlots(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/availability-slot/availability-slot-list.fxml", "SkillOra - Availability Slots");
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

    private Optional<RendezVous> showRendezVousDialog(RendezVous source) {
        boolean editMode = source != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editMode ? "Edit RendezVous" : "Add RendezVous");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField studentIdField = new TextField(editMode && source.getStudentId() != null ? source.getStudentId().toString() : "");
        TextField professorIdField = new TextField(editMode && source.getProfessorId() != null ? source.getProfessorId().toString() : "");
        TextField courseIdField = new TextField(editMode && source.getCourseId() != null ? source.getCourseId().toString() : "");
        TextField slotIdField = new TextField(editMode && source.getSlotId() != null ? source.getSlotId().toString() : "");
        TextField statutField = new TextField(editMode ? normalizeDefault(source.getStatut(), "en_attente") : "en_attente");
        TextField meetingTypeField = new TextField(editMode ? normalizeDefault(source.getMeetingType(), "en_ligne") : "en_ligne");
        TextField meetingLinkField = new TextField(editMode ? defaultEmpty(source.getMeetingLink()) : "");
        TextField locationField = new TextField(editMode ? defaultEmpty(source.getLocation()) : "");
        TextField messageField = new TextField(editMode ? defaultEmpty(source.getMessage()) : "");
        TextField createdAtField = new TextField(editMode ? formatDateTime(source.getCreatedAt()) : formatDateTime(LocalDateTime.now()));

        grid.add(new Label("Student ID"), 0, 0);
        grid.add(studentIdField, 1, 0);
        grid.add(new Label("Professor ID"), 0, 1);
        grid.add(professorIdField, 1, 1);
        grid.add(new Label("Course ID (optional)"), 0, 2);
        grid.add(courseIdField, 1, 2);
        grid.add(new Label("Slot ID"), 0, 3);
        grid.add(slotIdField, 1, 3);
        grid.add(new Label("Status"), 0, 4);
        grid.add(statutField, 1, 4);
        grid.add(new Label("Meeting type"), 0, 5);
        grid.add(meetingTypeField, 1, 5);
        grid.add(new Label("Meeting link"), 0, 6);
        grid.add(meetingLinkField, 1, 6);
        grid.add(new Label("Location"), 0, 7);
        grid.add(locationField, 1, 7);
        grid.add(new Label("Message"), 0, 8);
        grid.add(messageField, 1, 8);
        grid.add(new Label("Created (yyyy-MM-dd HH:mm)"), 0, 9);
        grid.add(createdAtField, 1, 9);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return Optional.empty();
        }

        try {
            RendezVous rdv = new RendezVous();
            rdv.setStudentId(parseIntRequired(studentIdField.getText(), "Student ID"));
            rdv.setProfessorId(parseIntRequired(professorIdField.getText(), "Professor ID"));
            rdv.setCourseId(parseIntOptional(courseIdField.getText()));
            rdv.setSlotId(parseIntRequired(slotIdField.getText(), "Slot ID"));
            rdv.setStatut(normalizeDefault(statutField.getText(), "en_attente"));
            rdv.setMeetingType(normalizeDefault(meetingTypeField.getText(), "en_ligne"));
            rdv.setMeetingLink(toNull(meetingLinkField.getText()));
            rdv.setLocation(toNull(locationField.getText()));
            rdv.setMessage(toNull(messageField.getText()));
            rdv.setCreatedAt(parseDateTimeRequired(createdAtField.getText(), "Created"));
            rdv.setLocationLabel(editMode ? source.getLocationLabel() : null);
            rdv.setLocationLat(editMode ? source.getLocationLat() : null);
            rdv.setLocationLng(editMode ? source.getLocationLng() : null);
            rdv.setRefusalReason(editMode ? source.getRefusalReason() : null);
            rdv.setCoursePdfName(editMode ? source.getCoursePdfName() : null);
            return Optional.of(rdv);
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

    private Integer parseIntOptional(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Course ID must be a valid integer.");
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

    private String toNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDefault(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() ? fallback : value;
    }

    private boolean isPendingStatus(String rawStatus) {
        return normalizeDefault(rawStatus, "").toLowerCase().contains("attente");
    }

    private String defaultEmpty(String value) {
        return value == null ? "" : value;
    }

    private String safeNumber(Integer value) {
        return value == null ? "-" : value.toString();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
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
