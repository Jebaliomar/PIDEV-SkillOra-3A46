package tn.esprit.controlles;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.SnapshotParameters;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import javafx.stage.Window;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.entities.SiteReservationFormData;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SiteReservationFormService;
import tn.esprit.services.TwilioSmsService;
import tn.esprit.services.VoiceRecognitionService;
import tn.esprit.tools.CameraReservationDialog;
import tn.esprit.tools.CameraReservationField;
import tn.esprit.tools.OcrTextRecognizer;
import tn.esprit.tools.TesseractCliTextRecognizer;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Duration;

public class SiteReservationFormController {

    private static final Pattern RESERVATION_VOICE_MARKER_PATTERN = Pattern.compile(
            "\\b(first\\s+name|first|prenom|friste|frist|last\\s+name|last|laste|nom|phone|telephone|tel|iphone)\\b"
    );
    private static final int[] VERIFICATION_ANGLES = {0, 90, 180, 270};
    private static final List<String> CAPTCHA_IMAGE_PATHS = List.of(
            "/images/captcha/dog.png",
            "/images/captcha/cat.png",
            "/images/captcha/object.png"
    );

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
    private Button reservationVoiceButton;
    @FXML
    private Label reservationVoiceStatusLabel;
    @FXML
    private Button submitReservationButton;
    @FXML
    private Button submitVerificationButton;
    @FXML
    private Button cameraReservationButton;
    @FXML
    private Label cameraHelperLabel;
    @FXML
    private FlowPane cameraPendingValuesPane;
    @FXML
    private Label firstNameErrorLabel;
    @FXML
    private Label lastNameErrorLabel;
    @FXML
    private Label phoneErrorLabel;
    @FXML
    private Label seatsErrorLabel;
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
    private ImageView verificationReferenceImageView;
    @FXML
    private ImageView verificationChallengeImageView;
    @FXML
    private Label verificationStatusDot;
    @FXML
    private Label verificationStatusLabel;

    private final SiteReservationFormService formService = new SiteReservationFormService();
    private final ReservationService reservationService = new ReservationService();
    private final TwilioSmsService twilioSmsService = new TwilioSmsService();
    private final OcrTextRecognizer ocrTextRecognizer = new TesseractCliTextRecognizer();
    private final VoiceRecognitionService voiceRecognitionService = new VoiceRecognitionService();

    private Event event;
    private Salle salle;
    private List<Integer> selectedSeats;
    private Integer eventId;
    private Integer salleId;
    private WebEngine mapWebEngine;
    private String pendingAddress;
    private final PauseTransition successFlashTimer = new PauseTransition(Duration.seconds(1.8));
    private final EnumMap<CameraReservationField, String> pendingCameraValues = new EnumMap<>(CameraReservationField.class);
    private CompletableFuture<String> activeReservationVoiceFill;
    private long reservationVoiceFillGeneration;
    private int verificationTargetRotation;
    private int verificationCurrentRotation;
    private boolean verificationPassed;
    private boolean verificationHasError;

    @FXML
    public void initialize() {
        submitReservationButton.setDisable(true);
        ThemeManager.syncToggleButton(themeToggleButton);
        pageTitleLabel.setText("Reservation");
        pageSubtitleLabel.setText("Reserve your seat for an event.");
        configureImageVerification();
        configureMapPreview();
        initializeValidation();
        configureCameraSupport();
        updateSubmitReservationState();
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
        updateSubmitReservationState();
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
        cancelReservationVoiceFill();
        goBackToSalleDetails();
    }

    @FXML
    private void onSubmitReservation() {
        cancelReservationVoiceFill();
        if (!isReservationFormValid() || !verificationPassed) {
            updateSubmitReservationState();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Verification");
            alert.setHeaderText("Reservation is not ready");
            alert.setContentText("Fill all required fields and match the image direction before submitting.");
            alert.showAndWait();
            return;
        }

        try {
            clearValidationState();
            Reservation reservation = buildReservationPayload();
            reservationService.add(reservation);
            showSuccessFlash("Reservation enregistree, SMS en cours", false);
            sendReservationSmsThenFinish(reservation);
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

    @FXML
    private void startReservationVoiceFill() {
        if (activeReservationVoiceFill != null && !activeReservationVoiceFill.isDone()) {
            showReservationVoiceStatus("Listening... please speak now", false);
            return;
        }

        long requestId = ++reservationVoiceFillGeneration;
        setReservationVoiceListening(true);
        showReservationVoiceStatus("Listening... please speak now", false);

        activeReservationVoiceFill = voiceRecognitionService.recognizeReservationVoiceFill();
        activeReservationVoiceFill.whenComplete((recognizedText, error) -> Platform.runLater(() -> {
            if (requestId != reservationVoiceFillGeneration) {
                return;
            }

            activeReservationVoiceFill = null;
            setReservationVoiceListening(false);

            if (error != null) {
                showReservationVoiceStatus(getReservationVoiceErrorMessage(error), true);
                return;
            }

            String text = recognizedText == null ? "" : recognizedText.trim();
            if (text.isEmpty()) {
                showReservationVoiceStatus("No voice detected, please try again", true);
                return;
            }

            ReservationVoiceValues values = parseReservationVoiceText(text);
            if (!values.isComplete()) {
                System.out.println("[VoiceRecognition] recognition failed: could not detect reservation fields");
                showReservationVoiceStatus("Recognition failed. Please say first name, last name, and phone.", true);
                return;
            }

            applyReservationVoiceValues(values);
            showReservationVoiceStatus("Speech recognized. Form filled successfully", false);
        }));
    }

    @FXML
    private void onOpenCameraReservation() {
        try {
            Window owner = eventField.getScene() == null ? null : eventField.getScene().getWindow();
            EnumMap<CameraReservationField, String> seedValues = buildCameraSeedValues();
            CameraReservationDialog.showAndWait(owner, eventField.getScene(), seedValues, ocrTextRecognizer)
                    .ifPresent(result -> {
                        pendingCameraValues.clear();
                        pendingCameraValues.putAll(result);
                        applyCameraValuesToForm();
                        refreshCameraPendingValues();
                        showSuccessFlash("Camera values applied", false);
                    });
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Camera");
            alert.setHeaderText("Impossible d'ouvrir la camera");
            alert.setContentText(exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "Le popup camera n'a pas pu etre ouvert."
                    : exception.getMessage());
            alert.showAndWait();
        }
    }

    private ReservationVoiceValues parseReservationVoiceText(String recognizedText) {
        String normalizedText = normalizeReservationVoiceText(recognizedText);
        List<ReservationVoiceMarker> markers = findReservationVoiceMarkers(normalizedText);

        String firstName = toDisplayName(extractReservationVoiceValue(normalizedText, markers, ReservationVoiceField.FIRST_NAME));
        String lastName = toDisplayName(extractReservationVoiceValue(normalizedText, markers, ReservationVoiceField.LAST_NAME));
        String phone = extractPhoneNumber(extractReservationVoiceValue(normalizedText, markers, ReservationVoiceField.PHONE));

        ReservationVoiceValues labeledValues = new ReservationVoiceValues(firstName, lastName, phone);
        if (labeledValues.isComplete()) {
            return labeledValues;
        }

        ReservationVoiceValues compactValues = parseCompactReservationVoiceText(normalizedText);
        return compactValues.isComplete() ? compactValues : labeledValues;
    }

    private ReservationVoiceValues parseCompactReservationVoiceText(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return new ReservationVoiceValues("", "", "");
        }

        List<String> nameTokens = new ArrayList<>();
        StringBuilder phoneDigits = new StringBuilder();
        boolean phoneStarted = false;
        for (String token : normalizedText.split("\\s+")) {
            if (token.isBlank() || isReservationVoiceFillerToken(token)) {
                continue;
            }

            String digits = toPhoneDigits(token);
            if (digits != null) {
                phoneDigits.append(digits);
                phoneStarted = true;
                continue;
            }

            if (!phoneStarted && isLikelyNameToken(token)) {
                nameTokens.add(token);
            }
        }

        if (nameTokens.size() < 2 || phoneDigits.length() < 6) {
            return new ReservationVoiceValues("", "", "");
        }

        String firstName = toDisplayName(nameTokens.get(0));
        String lastName = toDisplayName(String.join(" ", nameTokens.subList(1, nameTokens.size())));
        return new ReservationVoiceValues(firstName, lastName, phoneDigits.toString());
    }

    private void applyReservationVoiceValues(ReservationVoiceValues values) {
        if (!values.firstName().isBlank()) {
            firstNameField.setText(values.firstName());
        }
        if (!values.lastName().isBlank()) {
            lastNameField.setText(values.lastName());
        }
        if (!values.phone().isBlank()) {
            phoneField.setText(values.phone());
        }
        updateSubmitReservationState();
    }

    @FXML
    private void rotateVerificationLeft() {
        rotateVerification(-90);
    }

    @FXML
    private void rotateVerificationRight() {
        rotateVerification(90);
    }

    @FXML
    private void submitImageVerification() {
        verificationPassed = verificationCurrentRotation == verificationTargetRotation;
        verificationHasError = !verificationPassed;
        if (verificationPassed) {
            submitVerificationButton.setDisable(true);
        }
        updateVerificationStatus();
        updateSubmitReservationState();
    }

    private void configureImageVerification() {
        Image verificationImage = loadVerificationImage();
        verificationReferenceImageView.setImage(verificationImage);
        verificationChallengeImageView.setImage(verificationImage);

        verificationTargetRotation = randomVerificationAngle();
        int offset = VERIFICATION_ANGLES[ThreadLocalRandom.current().nextInt(1, VERIFICATION_ANGLES.length)];
        verificationCurrentRotation = normalizeRotation(verificationTargetRotation + offset);

        verificationReferenceImageView.setRotate(verificationTargetRotation);
        verificationChallengeImageView.setRotate(verificationCurrentRotation);
        verificationPassed = false;
        verificationHasError = false;
        submitVerificationButton.setDisable(false);
        updateVerificationStatus();
    }

    private Image loadVerificationImage() {
        String imagePath = CAPTCHA_IMAGE_PATHS.get(ThreadLocalRandom.current().nextInt(CAPTCHA_IMAGE_PATHS.size()));
        URL resource = getClass().getResource(imagePath);
        if (resource != null) {
            return new Image(resource.toExternalForm(), false);
        }
        return createFallbackVerificationImage();
    }

    private Image createFallbackVerificationImage() {
        Canvas canvas = new Canvas(120, 120);
        GraphicsContext graphics = canvas.getGraphicsContext2D();

        graphics.setFill(Color.web("#f8fbff"));
        graphics.fillRoundRect(2, 2, 116, 116, 24, 24);
        graphics.setStroke(Color.web("#cfe0fb"));
        graphics.setLineWidth(3);
        graphics.strokeRoundRect(3.5, 3.5, 113, 113, 24, 24);

        graphics.setFill(Color.color(0, 0, 0, 0.14));
        graphics.fillOval(28, 94, 68, 12);

        graphics.setFill(Color.web("#60a5fa"));
        graphics.fillPolygon(new double[]{34, 60, 94, 66}, new double[]{38, 20, 42, 62}, 4);
        graphics.setFill(Color.web("#2563eb"));
        graphics.fillPolygon(new double[]{66, 94, 92, 64}, new double[]{62, 42, 86, 106}, 4);
        graphics.setFill(Color.web("#14b8a6"));
        graphics.fillPolygon(new double[]{34, 66, 64, 32}, new double[]{38, 62, 106, 80}, 4);
        graphics.setStroke(Color.web("#0f172a"));
        graphics.setLineWidth(2.5);
        graphics.strokePolygon(new double[]{34, 60, 94, 92, 64, 32}, new double[]{38, 20, 42, 86, 106, 80}, 6);
        graphics.setStroke(Color.color(1, 1, 1, 0.75));
        graphics.setLineWidth(2);
        graphics.strokeLine(44, 40, 60, 28);
        graphics.strokeLine(74, 61, 90, 48);

        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, null);
    }

    private int randomVerificationAngle() {
        return VERIFICATION_ANGLES[ThreadLocalRandom.current().nextInt(VERIFICATION_ANGLES.length)];
    }

    private void rotateVerification(int delta) {
        if (verificationPassed) {
            return;
        }

        verificationCurrentRotation = normalizeRotation(verificationCurrentRotation + delta);
        verificationChallengeImageView.setRotate(verificationCurrentRotation);
        verificationHasError = false;
        updateVerificationStatus();
        updateSubmitReservationState();
    }

    private int normalizeRotation(int angle) {
        int normalized = angle % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private void updateVerificationStatus() {
        verificationStatusDot.getStyleClass().removeAll(
                "site-verification-status-dot-pending",
                "site-verification-status-dot-success",
                "site-verification-status-dot-error"
        );
        if (verificationPassed) {
            verificationStatusDot.getStyleClass().add("site-verification-status-dot-success");
            verificationStatusLabel.setText("Verification complete");
        } else if (verificationHasError) {
            verificationStatusDot.getStyleClass().add("site-verification-status-dot-error");
            verificationStatusLabel.setText("Images do not match. Try again.");
        } else {
            verificationStatusDot.getStyleClass().add("site-verification-status-dot-pending");
            verificationStatusLabel.setText("Rotate the right image, then submit verification.");
        }
    }

    private List<ReservationVoiceMarker> findReservationVoiceMarkers(String normalizedText) {
        List<ReservationVoiceMarker> markers = new ArrayList<>();
        Matcher matcher = RESERVATION_VOICE_MARKER_PATTERN.matcher(normalizedText);
        while (matcher.find()) {
            ReservationVoiceField field = toReservationVoiceField(matcher.group(1));
            if (field != null) {
                markers.add(new ReservationVoiceMarker(field, matcher.start(), matcher.end()));
            }
        }
        markers.sort(Comparator.comparingInt(ReservationVoiceMarker::start));
        return markers;
    }

    private ReservationVoiceField toReservationVoiceField(String keyword) {
        String value = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (value.equals("first") || value.equals("first name") || value.equals("prenom")
                || value.equals("friste") || value.equals("frist")) {
            return ReservationVoiceField.FIRST_NAME;
        }
        if (value.equals("last") || value.equals("last name") || value.equals("laste") || value.equals("nom")) {
            return ReservationVoiceField.LAST_NAME;
        }
        if (value.equals("phone") || value.equals("telephone") || value.equals("tel") || value.equals("iphone")) {
            return ReservationVoiceField.PHONE;
        }
        return null;
    }

    private String extractReservationVoiceValue(String text, List<ReservationVoiceMarker> markers, ReservationVoiceField field) {
        for (int index = 0; index < markers.size(); index++) {
            ReservationVoiceMarker marker = markers.get(index);
            if (marker.field() != field) {
                continue;
            }

            int valueStart = marker.end();
            int valueEnd = index + 1 < markers.size() ? markers.get(index + 1).start() : text.length();
            String value = cleanReservationVoiceValue(text.substring(valueStart, valueEnd));
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String cleanReservationVoiceValue(String value) {
        String cleaned = value == null ? "" : value.trim();
        cleaned = cleaned.replaceAll("^(?:is|equals|equal|est|cest|c est|the|my|mon|ma|le|la|number|numero|num)\\s+", "");
        cleaned = cleaned.replaceAll("\\s+(?:and|et)$", "");
        return cleaned.trim().replaceAll("\\s+", " ");
    }

    private String normalizeReservationVoiceText(String text) {
        String normalized = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9+\\s]", " ");
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private String toDisplayName(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[^a-z\\s'-]", " ").trim().replaceAll("\\s+", " ");
        if (cleaned.isBlank()) {
            return "";
        }

        StringBuilder displayName = new StringBuilder();
        for (String token : cleaned.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1) {
                displayName.append(token.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return displayName.toString();
    }

    private String extractPhoneNumber(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (!digits.isBlank()) {
            return digits;
        }

        StringBuilder spokenDigits = new StringBuilder();
        String normalized = normalizeReservationVoiceText(value);
        for (String token : normalized.split("\\s+")) {
            String digit = spokenDigitToNumber(token);
            if (digit != null) {
                spokenDigits.append(digit);
            }
        }
        return spokenDigits.toString();
    }

    private String spokenDigitToNumber(String token) {
        return switch (token) {
            case "zero", "oh", "o" -> "0";
            case "one", "un" -> "1";
            case "two", "to", "too", "deux" -> "2";
            case "three", "tree", "trois" -> "3";
            case "four", "for", "quatre" -> "4";
            case "five", "cinq" -> "5";
            case "six" -> "6";
            case "seven", "sept" -> "7";
            case "eight", "ate", "huit" -> "8";
            case "nine", "neuf" -> "9";
            default -> null;
        };
    }

    private String toPhoneDigits(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (token.matches("\\+?\\d+")) {
            return token.replaceAll("\\D", "");
        }
        return spokenDigitToNumber(token);
    }

    private boolean isLikelyNameToken(String token) {
        return token != null && token.matches("[a-z]{2,}");
    }

    private boolean isReservationVoiceFillerToken(String token) {
        return switch (token) {
            case "first", "last", "name", "prenom", "nom", "phone", "telephone", "tel", "number",
                 "numero", "my", "mon", "ma", "is", "equals", "equal", "est", "cest", "c", "the",
                 "le", "la", "and", "et", "please", "reservation", "fill" -> true;
            default -> false;
        };
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
        required(seatsField.getText(), "Seats", seatsField, seatsErrorLabel);

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

    private void sendReservationSmsThenFinish(Reservation reservation) {
        Task<String> smsTask = new Task<>() {
            @Override
            protected String call() {
                return sendReservationThankYouSms(reservation);
            }
        };
        smsTask.setOnSucceeded(event -> {
            String smsError = smsTask.getValue();
            if (smsError == null) {
                showSuccessFlash("Reservation enregistree, SMS envoye", true);
            } else {
                showSuccessFlash("Reservation enregistree", false);
                showSmsWarning(smsError);
                goBackToSalleDetails();
            }
        });
        smsTask.setOnFailed(event -> {
            showSuccessFlash("Reservation enregistree", false);
            showSmsWarning("Reservation enregistree, mais le SMS n'a pas pu etre envoye.");
            goBackToSalleDetails();
        });

        Thread thread = new Thread(smsTask, "twilio-reservation-sms");
        thread.setDaemon(true);
        thread.start();
    }

    private String sendReservationThankYouSms(Reservation reservation) {
        try {
            twilioSmsService.sendReservationThankYou(reservation, eventField.getText(), salleField.getText());
            return null;
        } catch (TwilioSmsService.SmsException exception) {
            return exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "Reservation enregistree, mais le SMS n'a pas pu etre envoye."
                    : exception.getMessage();
        }
    }

    private void showSmsWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("SMS Twilio");
        alert.setHeaderText("Reservation enregistree, SMS non envoye");
        alert.setContentText(message);
        alert.showAndWait();
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
        bindValidationReset(seatsField, seatsErrorLabel);
    }

    private void configureCameraSupport() {
        if (cameraHelperLabel != null) {
            cameraHelperLabel.setText(ocrTextRecognizer.describeAvailability());
        }
        refreshCameraPendingValues();
    }

    private EnumMap<CameraReservationField, String> buildCameraSeedValues() {
        EnumMap<CameraReservationField, String> seedValues = new EnumMap<>(CameraReservationField.class);
        for (CameraReservationField field : CameraReservationField.values()) {
            String pendingValue = pendingCameraValues.get(field);
            if (pendingValue != null && !pendingValue.isBlank()) {
                seedValues.put(field, pendingValue);
                continue;
            }
            String formValue = switch (field) {
                case PRENOM -> firstNameField.getText();
                case NOM -> lastNameField.getText();
                case TELEPHONE -> phoneField.getText();
            };
            if (formValue != null && !formValue.isBlank()) {
                seedValues.put(field, formValue.trim());
            }
        }
        return seedValues;
    }

    private void applyCameraValuesToForm() {
        firstNameField.setText(pendingCameraValues.getOrDefault(CameraReservationField.PRENOM, ""));
        lastNameField.setText(pendingCameraValues.getOrDefault(CameraReservationField.NOM, ""));
        phoneField.setText(pendingCameraValues.getOrDefault(CameraReservationField.TELEPHONE, ""));
    }

    private void refreshCameraPendingValues() {
        if (cameraPendingValuesPane == null) {
            return;
        }
        cameraPendingValuesPane.getChildren().clear();
        for (CameraReservationField field : CameraReservationField.values()) {
            Label badge = new Label(buildCameraBadgeText(field));
            badge.getStyleClass().add("site-camera-badge");
            if (!pendingCameraValues.containsKey(field) || pendingCameraValues.get(field).isBlank()) {
                badge.getStyleClass().add("site-camera-badge-empty");
            }
            cameraPendingValuesPane.getChildren().add(badge);
        }
    }

    private String buildCameraBadgeText(CameraReservationField field) {
        String value = pendingCameraValues.get(field);
        if (value == null || value.isBlank()) {
            return field.getDisplayName() + ": pending";
        }
        return field.getDisplayName() + ": " + value;
    }

    private void bindValidationReset(TextField field, Label errorLabel) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            clearFieldInvalid(field);
            clearFieldError(errorLabel);
            updateSubmitReservationState();
        });
    }

    private void updateSubmitReservationState() {
        submitReservationButton.setDisable(!isReservationFormValid() || !verificationPassed);
    }

    private boolean isReservationFormValid() {
        return isRequiredFilled(firstNameField)
                && isRequiredFilled(lastNameField)
                && isRequiredFilled(phoneField)
                && isRequiredFilled(seatsField);
    }

    private boolean isRequiredFilled(TextField field) {
        return field != null && field.getText() != null && !field.getText().trim().isEmpty();
    }

    private void clearValidationState() {
        clearFieldInvalid(firstNameField);
        clearFieldInvalid(lastNameField);
        clearFieldInvalid(phoneField);
        clearFieldInvalid(seatsField);
        clearFieldError(firstNameErrorLabel);
        clearFieldError(lastNameErrorLabel);
        clearFieldError(phoneErrorLabel);
        clearFieldError(seatsErrorLabel);
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

    private void setReservationVoiceListening(boolean listening) {
        reservationVoiceButton.setDisable(listening);
    }

    private void showReservationVoiceStatus(String message, boolean error) {
        String text = message == null ? "" : message.trim();
        reservationVoiceStatusLabel.setText(text);
        reservationVoiceStatusLabel.setVisible(!text.isEmpty());
        reservationVoiceStatusLabel.setManaged(!text.isEmpty());
        reservationVoiceStatusLabel.getStyleClass().remove("site-reservation-voice-status-error");
        if (error) {
            reservationVoiceStatusLabel.getStyleClass().add("site-reservation-voice-status-error");
        }
    }

    private String getReservationVoiceErrorMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof VoiceRecognitionService.MicrophoneUnavailableException) {
            return "Microphone not detected or permission denied";
        }
        if (cause instanceof VoiceRecognitionService.NoVoiceDetectedException) {
            return "No voice detected, please try again";
        }

        String message = cause.getMessage();
        if (message != null) {
            if (message.contains("Microphone not detected or permission denied")) {
                return "Microphone not detected or permission denied";
            }
            if (message.contains("No voice detected")) {
                return "No voice detected, please try again";
            }
        }

        return "Recognition failed. Please try again";
    }

    private void cancelReservationVoiceFill() {
        reservationVoiceFillGeneration++;
        activeReservationVoiceFill = null;
        voiceRecognitionService.stopListening();
        setReservationVoiceListening(false);
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

    private enum ReservationVoiceField {
        FIRST_NAME,
        LAST_NAME,
        PHONE
    }

    private record ReservationVoiceMarker(ReservationVoiceField field, int start, int end) {
    }

    private record ReservationVoiceValues(String firstName, String lastName, String phone) {
        boolean isComplete() {
            return !firstName.isBlank() && !lastName.isBlank() && !phone.isBlank();
        }
    }
}
