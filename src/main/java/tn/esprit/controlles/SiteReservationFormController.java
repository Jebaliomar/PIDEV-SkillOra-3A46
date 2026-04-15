package tn.esprit.controlles;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.entities.SiteReservationFormData;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SiteReservationFormService;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import javafx.util.Duration;

public class SiteReservationFormController {

    @FXML
    private Button themeToggleButton;
    @FXML
    private TextField eventField;
    @FXML
    private TextField salleField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private Label firstNameErrorLabel;
    @FXML
    private Label lastNameErrorLabel;
    @FXML
    private Label phoneErrorLabel;
    @FXML
    private TextField seatsField;
    @FXML
    private Label maxSeatsHelperLabel;
    @FXML
    private TextField addressField;
    @FXML
    private Label addressHelperLabel;
    @FXML
    private TextField reservationDateField;
    @FXML
    private Label reservationDateHelperLabel;
    @FXML
    private StackPane mapPreviewPane;
    @FXML
    private WebView mapWebView;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label successFlashLabel;

    private final SiteReservationFormService formService = new SiteReservationFormService();
    private final ReservationService reservationService = new ReservationService();

    private Event event;
    private Salle salle;
    private List<Integer> selectedSeats;
    private Integer eventId;
    private Integer salleId;
    private WebEngine mapWebEngine;
    private String pendingAddress;
    private final PauseTransition successFlashTimer = new PauseTransition(Duration.seconds(1.8));

    @FXML
    public void initialize() {
        ThemeManager.syncToggleButton(themeToggleButton);
        pageTitleLabel.setText("Reservation");
        pageSubtitleLabel.setText("Reserve your seat for an event.");
        configureMapPreview();
        initializeValidation();
    }

    public void setData(Event event, Salle salle, List<Integer> selectedSeats) {
        this.event = event;
        this.salle = salle;
        this.selectedSeats = selectedSeats;

        SiteReservationFormData data = formService.buildFormData(event, salle, selectedSeats);
        this.eventId = data.getEventId();
        this.salleId = data.getSalleId();

        eventField.setText(data.getEventTitle());
        salleField.setText(data.getSalleName());
        seatsField.setText(data.getSeatsDisplay());
        addressField.setText(data.getAddress());
        reservationDateField.setText(formService.formatDate(data.getReservationDate()));
        maxSeatsHelperLabel.setText(formService.buildMaxSeatsHelper(data.getMaxSeats()));
        addressHelperLabel.setText(formService.buildAddressHelper());
        reservationDateHelperLabel.setText(formService.buildDateHelper());

        pendingAddress = data.getAddress();
        pushAddressToMap();
    }

    @FXML
    private void onToggleTheme() {
        if (eventField.getScene() != null) {
            ThemeManager.toggleTheme(eventField.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
        }
    }

    @FXML
    private void onCancel() {
        goBackToSalleDetails();
    }

    @FXML
    private void onSubmitReservation() {
        try {
            clearValidationState();
            Reservation reservation = buildReservationPayload();
            reservationService.add(reservation);
            showSuccessFlash("Reservation enregistree", true);
        } catch (ValidationException e) {
        } catch (IllegalArgumentException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation");
            alert.setHeaderText("Donnees invalides");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Reservation");
            alert.setHeaderText("Echec enregistrement");
            alert.setContentText(e.getMessage() == null || e.getMessage().isBlank() ? "Erreur inconnue" : e.getMessage());
            alert.showAndWait();
        }
    }

    private void showSuccessFlash(String message, boolean navigateBack) {
        if (successFlashLabel == null) {
            if (navigateBack) {
                goBackToSalleDetails();
            }
            return;
        }
        successFlashTimer.stop();
        successFlashLabel.setText("✓  " + message);
        successFlashLabel.setVisible(true);
        successFlashLabel.setManaged(true);
        successFlashTimer.setOnFinished(evt -> {
            successFlashLabel.setVisible(false);
            successFlashLabel.setManaged(false);
            if (navigateBack) {
                goBackToSalleDetails();
            }
        });
        successFlashTimer.playFromStart();
    }

    private Reservation buildReservationPayload() {
        String firstName = required(firstNameField.getText(), "First Name", firstNameField, firstNameErrorLabel);
        String lastName = required(lastNameField.getText(), "Last Name", lastNameField, lastNameErrorLabel);
        String phone = required(phoneField.getText(), "Phone", phoneField, phoneErrorLabel);

        if (eventId == null) {
            throw new IllegalArgumentException("Event ID est manquant.");
        }
        if (salleId == null) {
            throw new IllegalArgumentException("Salle ID est manquant.");
        }

        Reservation reservation = new Reservation();
        reservation.setNom(lastName);
        reservation.setPrenom(firstName);
        reservation.setTelephone(phone);
        reservation.setAdresse(addressField.getText() == null ? "" : addressField.getText().trim());
        reservation.setNombrePlaces(buildNombrePlaces());
        reservation.setDateReservation(LocalDateTime.now());
        reservation.setEventId(eventId);
        reservation.setSalleId(salleId);
        reservation.setUserId(null);
        return reservation;
    }

    private String buildNombrePlaces() {
        if (selectedSeats != null && !selectedSeats.isEmpty()) {
            return selectedSeats.stream()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
        }
        String raw = seatsField.getText() == null ? "" : seatsField.getText().trim();
        if (raw.isEmpty()) {
            return "1";
        }
        if (raw.contains(",")) {
            int count = (int) java.util.Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .count();
            return String.valueOf(Math.max(count, 1));
        }
        return raw;
    }

    private String required(String value, String label, Node fieldNode, Label errorLabel) {
        if (value == null || value.trim().isEmpty()) {
            markFieldInvalid(fieldNode);
            showFieldError(errorLabel, label + " is required.");
        }
        clearFieldInvalid(fieldNode);
        return value.trim();
    }

    private void initializeValidation() {
        bindValidationReset(firstNameField, firstNameErrorLabel);
        bindValidationReset(lastNameField, lastNameErrorLabel);
        bindValidationReset(phoneField, phoneErrorLabel);
    }

    private void bindValidationReset(TextField field, Label errorLabel) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            clearFieldInvalid(field);
            clearFieldError(errorLabel);
        });
    }

    private void clearValidationState() {
        clearFieldInvalid(firstNameField);
        clearFieldInvalid(lastNameField);
        clearFieldInvalid(phoneField);
        clearFieldError(firstNameErrorLabel);
        clearFieldError(lastNameErrorLabel);
        clearFieldError(phoneErrorLabel);
    }

    private void showFieldError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
        throw new ValidationException();
    }

    private void clearFieldError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    private void markFieldInvalid(Node node) {
        if (!node.getStyleClass().contains("site-form-input-invalid")) {
            node.getStyleClass().add("site-form-input-invalid");
        }
    }

    private void clearFieldInvalid(Node node) {
        node.getStyleClass().remove("site-form-input-invalid");
    }

    private void configureMapPreview() {
        if (mapWebView == null) {
            return;
        }

        mapWebEngine = mapWebView.getEngine();
        mapWebView.setContextMenuEnabled(false);
        mapWebView.setPageFill(Color.TRANSPARENT);

        URL mapResource = getClass().getResource("/viewsadmin/event/reservation-map-preview.html");
        if (mapResource == null) {
            return;
        }

        mapWebEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                pushAddressToMap();
            }
        });

        mapWebEngine.load(mapResource.toExternalForm());
    }

    private void pushAddressToMap() {
        if (mapWebEngine == null || pendingAddress == null || pendingAddress.isBlank()) {
            return;
        }

        try {
            mapWebEngine.executeScript("window.previewMapApi.setAddress(" + quote(pendingAddress) + ");");
        } catch (Exception ignored) {
        }
    }

    private void goBackToSalleDetails() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteSalleDetailView.fxml"));
            Parent root = loader.load();
            SiteSalleDetailController controller = loader.getController();
            controller.setData(event, salle);

            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) eventField.getScene().getWindow();
            stage.setTitle("Skillora Salle Details");
            stage.setScene(scene);
            stage.show();
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Navigation failed");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private String quote(String value) {
        String safeValue = value == null ? "" : value;
        return "'" + safeValue
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n") + "'";
    }

    private static final class ValidationException extends RuntimeException {
    }
}
