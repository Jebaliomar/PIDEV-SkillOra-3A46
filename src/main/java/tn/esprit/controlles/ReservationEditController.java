package tn.esprit.controlles;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.services.EventService;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SalleService;
import tn.esprit.tools.ThemeManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javafx.util.Duration;

public class ReservationEditController {

    private static final DateTimeFormatter FORM_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private Button eventsNavButton;
    @FXML
    private Button sallesNavButton;
    @FXML
    private Button reservationsNavButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private ComboBox<EventOption> eventComboBox;
    @FXML
    private ComboBox<SalleOption> salleComboBox;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField placesField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField reservationDateField;
    @FXML
    private Label firstNameErrorLabel;
    @FXML
    private Label lastNameErrorLabel;
    @FXML
    private Label phoneErrorLabel;
    @FXML
    private Label placesErrorLabel;
    @FXML
    private Label addressErrorLabel;
    @FXML
    private Label reservationDateErrorLabel;
    @FXML
    private Label successFlashLabel;

    private final ReservationService reservationService = new ReservationService();
    private final EventService eventService = new EventService();
    private final SalleService salleService = new SalleService();

    private final List<Event> events = new ArrayList<>();
    private final List<Salle> salles = new ArrayList<>();
    private Reservation editingReservation;
    private final PauseTransition successFlashTimer = new PauseTransition(Duration.seconds(1.8));

    @FXML
    public void initialize() {
        setActiveNav(reservationsNavButton);
        ThemeManager.syncToggleButton(themeToggleButton);
        pageTitleLabel.setText("Edit Reservation");
        pageSubtitleLabel.setText("Update reservation details.");
        initializeValidation();
        loadReferenceData();
    }

    public void setReservationId(Integer reservationId) {
        if (reservationId == null) {
            return;
        }
        try {
            editingReservation = reservationService.getById(reservationId);
            if (editingReservation == null) {
                showError("Reservation not found", "The selected reservation could not be loaded.");
                return;
            }
            populateForm();
        } catch (SQLException e) {
            showError("Load failed", e.getMessage());
        }
    }

    @FXML
    private void onToggleTheme() {
        if (themeToggleButton.getScene() != null) {
            ThemeManager.toggleTheme(themeToggleButton.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
        }
    }

    @FXML
    private void onOpenEvents() {
        navigateTo("/viewsadmin/event/EventDashboard.fxml", "Skillora Events", "/styles/event.css", "/styles/salle.css");
    }

    @FXML
    private void onOpenSalles() {
        navigateTo("/viewsadmin/event/SalleDashboard.fxml", "Skillora Salles", "/styles/event.css", "/styles/salle.css");
    }

    @FXML
    private void onBackToSite() {
        navigateTo("/viewsadmin/event/SiteEventsView.fxml", "SkillHarbor Events", "/styles/site-events.css");
    }

    @FXML
    private void onCancel() {
        openReservationDashboard();
    }

    @FXML
    private void onSaveChanges() {
        if (editingReservation == null) {
            showError("Update failed", "No reservation is selected.");
            return;
        }

        try {
            clearValidationState();

            EventOption selectedEvent = requiredEvent();
            SalleOption selectedSalle = requiredSalle();

            editingReservation.setPrenom(required(firstNameField.getText(), "First Name", firstNameErrorLabel));
            editingReservation.setNom(required(lastNameField.getText(), "Last Name", lastNameErrorLabel));
            editingReservation.setTelephone(required(phoneField.getText(), "Phone", phoneErrorLabel));
            editingReservation.setNombrePlaces(required(placesField.getText(), "Places", placesErrorLabel));
            editingReservation.setAdresse(required(addressField.getText(), "Address", addressErrorLabel));
            editingReservation.setDateReservation(parseDate(required(reservationDateField.getText(), "Reservation Date", reservationDateErrorLabel)));
            editingReservation.setEventId(selectedEvent.id());
            editingReservation.setSalleId(selectedSalle.id());

            if (!reservationService.update(editingReservation)) {
                throw new IllegalArgumentException("No reservation was updated.");
            }
            showSuccessAndReturn("Save changes completed.");
        } catch (ValidationException ignored) {
        } catch (IllegalArgumentException | SQLException e) {
            showError("Update failed", e.getMessage());
        }
    }

    private void showSuccessAndReturn(String message) {
        if (successFlashLabel == null) {
            openReservationDashboard();
            return;
        }
        successFlashTimer.stop();
        successFlashLabel.setText("✓  " + message);
        successFlashLabel.setVisible(true);
        successFlashLabel.setManaged(true);
        successFlashTimer.setOnFinished(evt -> {
            successFlashLabel.setVisible(false);
            successFlashLabel.setManaged(false);
            openReservationDashboard();
        });
        successFlashTimer.playFromStart();
    }

    private void loadReferenceData() {
        try {
            events.clear();
            events.addAll(eventService.getAll());
            salles.clear();
            salles.addAll(salleService.getAll());

            List<EventOption> eventOptions = new ArrayList<>();
            for (Event event : events) {
                if (event.getId() != null) {
                    eventOptions.add(new EventOption(event.getId(), safe(event.getTitle())));
                }
            }
            eventComboBox.setItems(FXCollections.observableArrayList(eventOptions));

            List<SalleOption> salleOptions = new ArrayList<>();
            for (Salle salle : salles) {
                if (salle.getId() != null) {
                    salleOptions.add(new SalleOption(salle.getId(), safe(salle.getName())));
                }
            }
            salleComboBox.setItems(FXCollections.observableArrayList(salleOptions));
        } catch (SQLException e) {
            showError("Load failed", e.getMessage());
        }
    }

    private void populateForm() {
        eventComboBox.setValue(findEventOption(editingReservation.getEventId()));
        salleComboBox.setValue(findSalleOption(editingReservation.getSalleId()));
        firstNameField.setText(safeInput(editingReservation.getPrenom()));
        lastNameField.setText(safeInput(editingReservation.getNom()));
        phoneField.setText(safeInput(editingReservation.getTelephone()));
        placesField.setText(safeInput(editingReservation.getNombrePlaces()));
        addressField.setText(safeInput(editingReservation.getAdresse()));
        reservationDateField.setText(editingReservation.getDateReservation() == null ? "" : FORM_DATE_FORMATTER.format(editingReservation.getDateReservation()));
    }

    private EventOption findEventOption(Integer eventId) {
        if (eventId == null) {
            return null;
        }
        return eventComboBox.getItems().stream()
                .filter(option -> eventId.equals(option.id()))
                .findFirst()
                .orElse(null);
    }

    private SalleOption findSalleOption(Integer salleId) {
        if (salleId == null) {
            return null;
        }
        return salleComboBox.getItems().stream()
                .filter(option -> salleId.equals(option.id()))
                .findFirst()
                .orElse(null);
    }

    private EventOption requiredEvent() {
        EventOption option = eventComboBox.getValue();
        if (option == null) {
            showError("Validation", "Event is required.");
            throw new ValidationException();
        }
        return option;
    }

    private SalleOption requiredSalle() {
        SalleOption option = salleComboBox.getValue();
        if (option == null) {
            showError("Validation", "Salle is required.");
            throw new ValidationException();
        }
        return option;
    }

    private String required(String value, String label, Label errorLabel) {
        if (value == null || value.trim().isEmpty()) {
            errorLabel.setText(label + " is required.");
            errorLabel.setManaged(true);
            errorLabel.setVisible(true);
            throw new ValidationException();
        }
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        return value.trim();
    }

    private LocalDateTime parseDate(String value) {
        try {
            return LocalDateTime.parse(value, FORM_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            reservationDateErrorLabel.setText("Use format dd/MM/yyyy HH:mm.");
            reservationDateErrorLabel.setManaged(true);
            reservationDateErrorLabel.setVisible(true);
            throw new ValidationException();
        }
    }


    private void initializeValidation() {
        bindValidationReset(firstNameField, firstNameErrorLabel);
        bindValidationReset(lastNameField, lastNameErrorLabel);
        bindValidationReset(phoneField, phoneErrorLabel);
        bindValidationReset(placesField, placesErrorLabel);
        bindValidationReset(addressField, addressErrorLabel);
        bindValidationReset(reservationDateField, reservationDateErrorLabel);
    }

    private void bindValidationReset(TextField field, Label errorLabel) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            errorLabel.setText("");
            errorLabel.setManaged(false);
            errorLabel.setVisible(false);
        });
    }

    private void clearValidationState() {
        firstNameErrorLabel.setText("");
        firstNameErrorLabel.setManaged(false);
        firstNameErrorLabel.setVisible(false);
        lastNameErrorLabel.setText("");
        lastNameErrorLabel.setManaged(false);
        lastNameErrorLabel.setVisible(false);
        phoneErrorLabel.setText("");
        phoneErrorLabel.setManaged(false);
        phoneErrorLabel.setVisible(false);
        placesErrorLabel.setText("");
        placesErrorLabel.setManaged(false);
        placesErrorLabel.setVisible(false);
        addressErrorLabel.setText("");
        addressErrorLabel.setManaged(false);
        addressErrorLabel.setVisible(false);
        reservationDateErrorLabel.setText("");
        reservationDateErrorLabel.setManaged(false);
        reservationDateErrorLabel.setVisible(false);
    }

    private void openReservationDashboard() {
        navigateTo("/viewsadmin/event/ReservationDashboard.fxml", "Skillora Reservations", "/styles/event.css", "/styles/salle.css", "/styles/reservations.css");
    }

    private void navigateTo(String fxml, String title, String... stylesheets) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            for (String stylesheet : stylesheets) {
                scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
            }
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) pageTitleLabel.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void setActiveNav(Button activeButton) {
        eventsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        sallesNavButton.getStyleClass().remove("sidebar-nav-item-active");
        reservationsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        if (!activeButton.getStyleClass().contains("sidebar-nav-item-active")) {
            activeButton.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String safeInput(String value) {
        return value == null ? "" : value;
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content == null || content.isBlank() ? "Unknown error" : content);
        alert.showAndWait();
    }

    private record EventOption(Integer id, String label) {
        @Override
        public String toString() {
            return label;
        }
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
