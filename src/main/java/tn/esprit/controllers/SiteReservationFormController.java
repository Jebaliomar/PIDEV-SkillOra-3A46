package tn.esprit.controllers;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.controllers.front.SkilloraNavbarController;
import tn.esprit.entities.User;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.entities.SiteReservationFormData;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SiteReservationFormService;
import tn.esprit.services.VoiceRecognitionService;
import tn.esprit.tools.AppIcons;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.AuthSession;
import tn.esprit.tools.BotpressSupportWidget;
import tn.esprit.tools.CameraReservationDialog;
import tn.esprit.tools.CameraReservationField;
import tn.esprit.tools.TesseractCliTextRecognizer;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
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
    @FXML
    private MenuButton userMenu;
    @FXML
    private MenuItem menuProfile;
    @FXML
    private MenuItem menuSettings;
    @FXML
    private SkilloraNavbarController navbarController;
    @FXML
    private Label reservationVoiceStatusLabel;
    @FXML
    private Button reservationVoiceButton;
    @FXML
    private Button cameraReservationButton;
    @FXML
    private FlowPane cameraPendingValuesPane;
    @FXML
    private Label cameraHelperLabel;
    @FXML
    private ImageView verificationReferenceImageView;
    @FXML
    private ImageView verificationChallengeImageView;
    @FXML
    private Label verificationStatusDot;
    @FXML
    private Label verificationStatusLabel;
    @FXML
    private Button submitReservationButton;

    private final SiteReservationFormService formService = new SiteReservationFormService();
    private final ReservationService reservationService = new ReservationService();
    private final VoiceRecognitionService voiceRecognitionService = new VoiceRecognitionService();

    private Event event;
    private Salle salle;
    private List<Integer> selectedSeats;
    private Integer eventId;
    private Integer salleId;
    private WebEngine mapWebEngine;
    private String pendingAddress;
    private CompletableFuture<String> activeVoiceFill;
    private long voiceFillGeneration;
    private final PauseTransition successFlashTimer = new PauseTransition(Duration.seconds(1.8));
    private final EnumMap<CameraReservationField, String> cameraPendingValues = new EnumMap<>(CameraReservationField.class);
    private boolean accountVerified;
    private int verificationTargetRotation;
    private int verificationChallengeRotation;

    @FXML
    public void initialize() {
        configureHeaderControls();
        configureNavbar();
        pageTitleLabel.setText("Reservation");
        pageSubtitleLabel.setText("Reserve your seat for an event.");
        configureMapPreview();
        initializeValidation();
        initializeImageVerification();
        refreshCameraPendingValues();
        syncSubmitReservationState();
        BotpressSupportWidget.installForFrontendPage(pageTitleLabel);
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
            configureHeaderControls();
        }
    }

    @FXML
    private void onShowHome() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontHome(navigationSource());
    }

    @FXML
    private void onShowCourses() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontBrowseCourses(navigationSource());
    }

    @FXML
    private void onShowEvents() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontEvents(navigationSource());
    }

    @FXML
    private void onShowForum() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontForum(navigationSource());
    }

    @FXML
    private void onShowAssessment() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontAssessment(navigationSource());
    }

    @FXML
    private void onShowProfile() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontProfile(navigationSource());
    }

    @FXML
    private void onShowSettings() {
        cancelReservationVoiceFill();
        AppNavigator.showFrontSettings(navigationSource());
    }

    @FXML
    private void onLogout() {
        cancelReservationVoiceFill();
        AppNavigator.showLogin(navigationSource());
    }

    @FXML
    private void onCancel() {
        cancelReservationVoiceFill();
        goBackToSalleDetails();
    }

    @FXML
    private void startReservationVoiceFill() {
        if (activeVoiceFill != null && !activeVoiceFill.isDone()) {
            setReservationVoiceStatus("Voice capture is already running.", false);
            return;
        }

        setReservationVoiceControlsListening(true);
        setReservationVoiceStatus("Listening... say first name, last name and phone.", false);
        long requestId = ++voiceFillGeneration;
        activeVoiceFill = voiceRecognitionService.recognizeReservationVoiceFill();
        activeVoiceFill.whenComplete((recognizedText, error) -> Platform.runLater(() -> {
            if (requestId != voiceFillGeneration) {
                return;
            }
            activeVoiceFill = null;
            setReservationVoiceControlsListening(false);

            if (error != null) {
                setReservationVoiceStatus("Voice fill failed: " + getVoiceErrorMessage(error), true);
                return;
            }

            VoiceFillData voiceFillData = parseVoiceFillData(recognizedText);
            int appliedFields = 0;
            if (voiceFillData.firstName != null && !voiceFillData.firstName.isBlank()) {
                firstNameField.setText(voiceFillData.firstName);
                appliedFields++;
            }
            if (voiceFillData.lastName != null && !voiceFillData.lastName.isBlank()) {
                lastNameField.setText(voiceFillData.lastName);
                appliedFields++;
            }
            if (voiceFillData.phone != null && !voiceFillData.phone.isBlank()) {
                phoneField.setText(voiceFillData.phone);
                appliedFields++;
            }

            if (appliedFields == 0) {
                setReservationVoiceStatus("Voice recognized, but no reservation fields could be mapped. Try: first name Mohamed, last name Ben Ali, phone 55123456.", true);
                return;
            }

            setReservationVoiceStatus("Filled " + appliedFields + " field(s) from voice: " + safeTrim(recognizedText), false);
        }));
    }

    @FXML
    private void onOpenCameraReservation() {
        try {
            EnumMap<CameraReservationField, String> initialValues = new EnumMap<>(CameraReservationField.class);
            putIfNotBlank(initialValues, CameraReservationField.PRENOM, firstNameField.getText());
            putIfNotBlank(initialValues, CameraReservationField.NOM, lastNameField.getText());
            putIfNotBlank(initialValues, CameraReservationField.TELEPHONE, phoneField.getText());
            initialValues.putAll(cameraPendingValues);

            CameraReservationDialog.showAndWait(
                    eventField.getScene() == null ? null : eventField.getScene().getWindow(),
                    eventField.getScene(),
                    initialValues,
                    new TesseractCliTextRecognizer()
            ).ifPresent(this::applyCameraReservationValues);
        } catch (Exception exception) {
            setReservationVoiceStatus("Camera reservation failed: " + safeMessage(exception), true);
        }
    }

    @FXML
    private void rotateVerificationLeft() {
        rotateVerificationChallenge(-90);
    }

    @FXML
    private void rotateVerificationRight() {
        rotateVerificationChallenge(90);
    }

    @FXML
    private void onSubmitReservation() {
        try {
            clearValidationState();
            if (!accountVerified) {
                setVerificationStatus("Match the verification image before submitting the reservation.", VerificationState.ERROR);
                syncSubmitReservationState();
                return;
            }
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
        User currentUser = AuthSession.getCurrentUser();
        reservation.setUserId(currentUser == null ? null : currentUser.getId());
        return reservation;
    }

    private String buildNombrePlaces() {
        if (selectedSeats != null && !selectedSeats.isEmpty()) {
            return String.valueOf(selectedSeats.size());
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
        return value == null ? "" : value.trim();
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
        cancelReservationVoiceFill();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteSalleDetailView.fxml"));
            Parent root = loader.load();
            SiteSalleDetailController controller = loader.getController();
            controller.setData(event, salle);

            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) eventField.getScene().getWindow();
            AppWindow.show(stage, scene, "SkillORA Salle Details", false);
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
            navbarController.setBeforeNavigation(this::cancelReservationVoiceFill);
        }
    }

    private void initializeImageVerification() {
        accountVerified = false;
        List<String> resources = List.of(
                "/images/captcha/cat.png",
                "/images/captcha/dog.png",
                "/images/captcha/object.png"
        );
        String selectedResource = resources.get(ThreadLocalRandom.current().nextInt(resources.size()));
        URL imageUrl = getClass().getResource(selectedResource);
        if (imageUrl == null) {
            setVerificationStatus("Verification image is missing: " + selectedResource, VerificationState.ERROR);
            syncSubmitReservationState();
            return;
        }

        Image image = new Image(imageUrl.toExternalForm());
        if (verificationReferenceImageView != null) {
            verificationReferenceImageView.setImage(image);
        }
        if (verificationChallengeImageView != null) {
            verificationChallengeImageView.setImage(image);
        }

        int[] rotations = {0, 90, 180, 270};
        verificationTargetRotation = rotations[ThreadLocalRandom.current().nextInt(rotations.length)];
        do {
            verificationChallengeRotation = rotations[ThreadLocalRandom.current().nextInt(rotations.length)];
        } while (verificationChallengeRotation == verificationTargetRotation);

        if (verificationReferenceImageView != null) {
            verificationReferenceImageView.setRotate(verificationTargetRotation);
        }
        applyChallengeRotation();
        updateVerificationFromRotation(true);
    }

    private void rotateVerificationChallenge(int delta) {
        if (accountVerified) {
            setVerificationStatus("Account already verified. You can submit the reservation.", VerificationState.SUCCESS);
            return;
        }
        verificationChallengeRotation = normalizeRotation(verificationChallengeRotation + delta);
        applyChallengeRotation();
        updateVerificationFromRotation(false);
    }

    private void applyChallengeRotation() {
        if (verificationChallengeImageView != null) {
            verificationChallengeImageView.setRotate(verificationChallengeRotation);
        }
    }

    private int normalizeRotation(int rotation) {
        int normalized = rotation % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private void updateVerificationFromRotation(boolean initial) {
        if (verificationReferenceImageView == null || verificationChallengeImageView == null
                || verificationReferenceImageView.getImage() == null || verificationChallengeImageView.getImage() == null) {
            accountVerified = false;
            setVerificationStatus("Verification image could not be loaded. Please reopen the form and try again.", VerificationState.ERROR);
            syncSubmitReservationState();
            return;
        }

        if (normalizeRotation(verificationChallengeRotation) == normalizeRotation(verificationTargetRotation)) {
            accountVerified = true;
            setVerificationStatus("Account verified. Submit Reservation is now active.", VerificationState.SUCCESS);
        } else {
            accountVerified = false;
            String message = initial
                    ? "Rotate the right image until it matches the reference. Submit Reservation will activate automatically."
                    : "Not matched yet. Keep rotating until both images match.";
            setVerificationStatus(message, VerificationState.PENDING);
        }
        syncSubmitReservationState();
    }

    private void setVerificationStatus(String message, VerificationState state) {
        if (verificationStatusLabel != null) {
            verificationStatusLabel.setText(message == null ? "" : message);
        }
        if (verificationStatusDot == null) {
            return;
        }
        verificationStatusDot.getStyleClass().removeAll(
                "site-verification-status-dot-pending",
                "site-verification-status-dot-success",
                "site-verification-status-dot-error"
        );
        String stateClass = switch (state) {
            case SUCCESS -> "site-verification-status-dot-success";
            case ERROR -> "site-verification-status-dot-error";
            case PENDING -> "site-verification-status-dot-pending";
        };
        if (!verificationStatusDot.getStyleClass().contains(stateClass)) {
            verificationStatusDot.getStyleClass().add(stateClass);
        }
    }

    private void syncSubmitReservationState() {
        if (submitReservationButton != null) {
            submitReservationButton.setDisable(!accountVerified);
        }
    }

    private void applyCameraReservationValues(Map<CameraReservationField, String> values) {
        if (values == null || values.isEmpty()) {
            setReservationVoiceStatus("Camera reservation closed without values.", false);
            return;
        }

        cameraPendingValues.clear();
        values.forEach((field, value) -> {
            String normalized = normalizeCameraField(field, value);
            if (normalized.isBlank()) {
                return;
            }
            cameraPendingValues.put(field, normalized);
            switch (field) {
                case PRENOM -> firstNameField.setText(normalized);
                case NOM -> lastNameField.setText(normalized);
                case TELEPHONE -> phoneField.setText(normalized);
            }
        });
        refreshCameraPendingValues();
        setReservationVoiceStatus("Camera values applied to the reservation form.", false);
    }

    private String normalizeCameraField(CameraReservationField field, String value) {
        String current = value == null ? "" : value.trim();
        if (current.isBlank()) {
            return "";
        }
        if (field == CameraReservationField.TELEPHONE) {
            return current.replaceAll("[^+0-9]", "");
        }
        return titleCase(current.replaceAll("[^\\p{L}\\s'-]", " ").replaceAll("\\s+", " ").trim());
    }

    private void refreshCameraPendingValues() {
        if (cameraPendingValuesPane == null) {
            return;
        }
        cameraPendingValuesPane.getChildren().clear();
        for (CameraReservationField field : CameraReservationField.values()) {
            String value = cameraPendingValues.get(field);
            Label badge = new Label(field.getDisplayName() + ": " + (value == null || value.isBlank() ? "empty" : value));
            badge.getStyleClass().add("site-camera-badge");
            if (value == null || value.isBlank()) {
                badge.getStyleClass().add("site-camera-badge-empty");
            }
            cameraPendingValuesPane.getChildren().add(badge);
        }
    }

    private void putIfNotBlank(EnumMap<CameraReservationField, String> values, CameraReservationField field, String value) {
        String normalized = normalizeCameraField(field, value);
        if (!normalized.isBlank()) {
            values.put(field, normalized);
        }
    }

    private Node navigationSource() {
        return themeToggleButton != null ? themeToggleButton : eventField;
    }

    private void setReservationVoiceControlsListening(boolean listening) {
        if (reservationVoiceButton != null) {
            reservationVoiceButton.setDisable(listening);
            reservationVoiceButton.setText(listening ? "Listening..." : "🎤 Fill reservation by voice");
        }
        if (cameraReservationButton != null) {
            cameraReservationButton.setDisable(listening);
        }
        if (cameraHelperLabel != null && listening) {
            cameraHelperLabel.setText("Voice capture is active.");
        } else if (cameraHelperLabel != null) {
            cameraHelperLabel.setText("Fill the fields with gesture handwriting.");
        }
    }

    private void cancelReservationVoiceFill() {
        voiceFillGeneration++;
        activeVoiceFill = null;
        voiceRecognitionService.stopListening();
        setReservationVoiceControlsListening(false);
    }

    private void setReservationVoiceStatus(String message, boolean error) {
        if (reservationVoiceStatusLabel == null) {
            return;
        }

        reservationVoiceStatusLabel.setText(message == null ? "" : message);
        reservationVoiceStatusLabel.setVisible(message != null && !message.isBlank());
        reservationVoiceStatusLabel.setManaged(message != null && !message.isBlank());
        reservationVoiceStatusLabel.getStyleClass().remove("site-reservation-voice-status-error");
        if (error && !reservationVoiceStatusLabel.getStyleClass().contains("site-reservation-voice-status-error")) {
            reservationVoiceStatusLabel.getStyleClass().add("site-reservation-voice-status-error");
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String getVoiceErrorMessage(Throwable error) {
        Throwable current = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        while (current != null && current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current == null ? null : current.getMessage();
        return message == null || message.isBlank() ? "Voice input failed." : message;
    }

    private VoiceFillData parseVoiceFillData(String recognizedText) {
        String text = recognizedText == null ? "" : recognizedText.trim();
        if (text.isEmpty()) {
            return new VoiceFillData(null, null, null);
        }

        String phone = extractPhone(text);
        String cleaned = removePhoneSegment(text)
                .replaceAll("(?i)\\b(?:phone|tel|telephone|num[eé]ro|number|mobile|gsm)\\b", " ")
                .replaceAll("[,:;=]", " ");

        String firstName = extractLabeledValue(cleaned,
                "(?i)\\b(?:pr[eé]nom|first\\s*name)\\b\\s*(?:is|est|c[' ]?est|:)?\\s*([\\p{L}'-]+)");
        String lastName = extractLabeledValue(cleaned,
                "(?i)\\b(?:nom|last\\s*name|family\\s*name)\\b\\s*(?:is|est|c[' ]?est|:)?\\s*([\\p{L}'-]+(?:\\s+[\\p{L}'-]+){0,2})");

        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            String[] words = cleaned.split("\\s+");
            int foundNames = 0;
            String fallbackFirst = null;
            String fallbackLast = null;
            for (String word : words) {
                if (word.isBlank() || word.matches(".*\\d.*")) {
                    continue;
                }
                String normalized = word.replaceAll("[^\\p{L}'-]", "");
                if (normalized.isBlank() || isVoiceFillerWord(normalized)) {
                    continue;
                }
                if (foundNames == 0) {
                    fallbackFirst = normalized;
                } else if (foundNames == 1) {
                    fallbackLast = normalized;
                    break;
                }
                foundNames++;
            }
            if (firstName == null || firstName.isBlank()) {
                firstName = fallbackFirst;
            }
            if (lastName == null || lastName.isBlank()) {
                lastName = fallbackLast;
            }
        }

        return new VoiceFillData(firstName, lastName, phone);
    }

    private String extractLabeledValue(String text, String pattern) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String extractPhone(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\+?\\d[\\d\\s().-]{6,})").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("[^\\d+]", "").trim();
        }
        return extractSpokenPhone(text);
    }

    private String extractSpokenPhone(String text) {
        String best = "";
        StringBuilder current = new StringBuilder();
        for (String rawToken : text.split("\\s+")) {
            String token = normalizeVoiceToken(rawToken);
            String digit = DIGIT_WORDS.get(token);
            if (digit != null) {
                current.append(digit);
                continue;
            }
            if (current.length() > best.length()) {
                best = current.toString();
            }
            current.setLength(0);
        }
        if (current.length() > best.length()) {
            best = current.toString();
        }
        return best.length() >= 6 ? best : null;
    }

    private String removePhoneSegment(String text) {
        String[] tokens = text.split("\\s+");
        StringBuilder builder = new StringBuilder();
        boolean skippingPhone = false;
        for (String rawToken : tokens) {
            String token = normalizeVoiceToken(rawToken);
            if (PHONE_LABEL_WORDS.contains(token)) {
                skippingPhone = true;
                continue;
            }
            if (skippingPhone && (DIGIT_WORDS.containsKey(token) || rawToken.matches(".*\\d.*"))) {
                continue;
            }
            skippingPhone = false;
            if (!DIGIT_WORDS.containsKey(token)) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(rawToken);
            }
        }
        return builder.toString();
    }

    private boolean isVoiceFillerWord(String word) {
        return FILLER_WORDS.contains(normalizeVoiceToken(word));
    }

    private String normalizeVoiceToken(String token) {
        if (token == null) {
            return "";
        }
        return token.toLowerCase(Locale.ROOT)
                .replace('é', 'e')
                .replace('è', 'e')
                .replace('ê', 'e')
                .replace('à', 'a')
                .replace('ç', 'c')
                .replaceAll("[^a-z0-9'+-]", "");
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeMessage(Throwable error) {
        if (error == null) {
            return "Unknown error.";
        }
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private String titleCase(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return Arrays.stream(raw.trim().toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(part -> !part.isBlank())
                .map(part -> Character.toUpperCase(part.charAt(0)) + (part.length() > 1 ? part.substring(1) : ""))
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private static final Map<String, String> DIGIT_WORDS = Map.ofEntries(
            Map.entry("zero", "0"),
            Map.entry("oh", "0"),
            Map.entry("o", "0"),
            Map.entry("one", "1"),
            Map.entry("two", "2"),
            Map.entry("three", "3"),
            Map.entry("four", "4"),
            Map.entry("five", "5"),
            Map.entry("six", "6"),
            Map.entry("seven", "7"),
            Map.entry("eight", "8"),
            Map.entry("nine", "9"),
            Map.entry("un", "1"),
            Map.entry("une", "1"),
            Map.entry("deux", "2"),
            Map.entry("trois", "3"),
            Map.entry("quatre", "4"),
            Map.entry("cinq", "5"),
            Map.entry("sept", "7"),
            Map.entry("huit", "8"),
            Map.entry("neuf", "9")
    );
    private static final Set<String> PHONE_LABEL_WORDS = Set.of("phone", "tel", "telephone", "numero", "number", "mobile", "gsm");
    private static final Set<String> FILLER_WORDS = Set.of(
            "my", "is", "the", "a", "an", "first", "last", "name", "family", "phone", "tel", "telephone",
            "numero", "number", "mobile", "gsm", "prenom", "nom", "je", "suis", "est", "cest", "c'est",
            "appelle", "mappelle", "mon", "ma"
    );

    private enum VerificationState {
        PENDING,
        SUCCESS,
        ERROR
    }

    private record VoiceFillData(String firstName, String lastName, String phone) {
    }
}
