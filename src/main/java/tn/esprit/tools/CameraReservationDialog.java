package tn.esprit.tools;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.TouchPoint;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CameraReservationDialog {
    private static final double DRAW_MIN_DISTANCE = 0.42;
    private static final double DRAW_INTERPOLATION_STEP = 6.0;
    private static final double DRAW_STROKE_WIDTH = 6.0;
    private static final Color DRAW_STROKE_COLOR = Color.web("#ff4d4f");
    private static final int OCR_PADDING = 24;
    private static final int OCR_SCALE = 5;
    private static final int OCR_THICKNESS_RADIUS = 2;
    private static final int OCR_MIN_CLUSTER = 2;
    private static final int OCR_BRIDGE_RADIUS = 3;

    private final OcrTextRecognizer recognizer;
    private final EnumMap<CameraReservationField, String> pendingValues;
    private final CameraReservationConfig config = CameraReservationConfig.getInstance();
    private final CameraPreviewService cameraPreviewService = new CameraPreviewService(config);
    private final HandTrackingDetector handTrackingDetector = new SimpleColorHandTrackingDetector();
    private final HandTrackingOverlayRenderer handTrackingOverlayRenderer = new HandTrackingOverlayRenderer();
    private final TrackedPointerSmoother trackedPointerSmoother = new TrackedPointerSmoother();
    private CameraReservationField selectedField = CameraReservationField.PRENOM;
    private Canvas landmarksCanvas;
    private GraphicsContext landmarksGraphics;
    private Canvas inkCanvas;
    private GraphicsContext inkGraphics;
    private ProgressBar pinchMeter;
    private Label statusLabel;
    private Label cameraStatusLabel;
    private Label drawingModeLabel;
    private Button cameraToggleButton;
    private Button recognizeButton;
    private TextField resultField;
    private FlowPane pendingBadges;
    private final EnumMap<CameraReservationField, ToggleButton> fieldButtons = new EnumMap<>(CameraReservationField.class);
    private double lastX;
    private double lastY;
    private double lastMidX;
    private double lastMidY;
    private boolean hasInk;
    private boolean trackingStrokeActive;
    private boolean recognitionRunning;
    private int missingTrackingFrames;
    private int activeTrackingFrames;
    private HandLandmarkPoint lastTrackingPoint;
    private TrackedHandState lastDetectedState;
    private Stage stage;
    private EnumMap<CameraReservationField, String> result;
    private static final int TRACKING_GRACE_FRAMES = 8;
    private static final int TRACKING_ACTIVATION_FRAMES = 2;
    private static final double MAX_TRACKING_JUMP = 0.12;

    private CameraReservationDialog(Map<CameraReservationField, String> initialValues, OcrTextRecognizer recognizer) {
        this.recognizer = recognizer;
        this.pendingValues = new EnumMap<>(CameraReservationField.class);
        if (initialValues != null) {
            this.pendingValues.putAll(initialValues);
        }
    }

    public static Optional<EnumMap<CameraReservationField, String>> showAndWait(
            Window owner,
            Scene sourceScene,
            Map<CameraReservationField, String> initialValues,
            OcrTextRecognizer recognizer
    ) {
        CameraReservationDialog dialog = new CameraReservationDialog(initialValues, recognizer);
        dialog.show(owner, sourceScene);
        return Optional.ofNullable(dialog.result);
    }

    private void show(Window owner, Scene sourceScene) {
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.DECORATED);
        stage.setTitle("Reservation Camera");
        stage.setMinWidth(930);
        stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("site-root", "site-camera-dialog");
        root.setTop(buildHeader());
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 980, 760);
        if (sourceScene != null) {
            scene.getStylesheets().addAll(sourceScene.getStylesheets());
            if (sourceScene.getRoot().getStyleClass().contains("theme-dark")) {
                root.getStyleClass().add("theme-dark");
            }
        }

        stage.setOnShown(event -> startCameraPreview());
        stage.setOnHidden(event -> {
            cameraPreviewService.stop();
            handTrackingDetector.stop();
        });
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.toFront();
        refreshSelectedField();
        refreshPendingBadges();
        stage.showAndWait();
    }

    private BorderPane buildHeader() {
        Label title = new Label("Camera - Ecriture gestuelle");
        title.getStyleClass().add("site-camera-modal-title");

        Label badge = new Label("+ OCR IA");
        badge.getStyleClass().add("site-camera-top-badge");

        HBox titleRow = new HBox(8, title, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("site-camera-close");
        closeButton.setOnAction(event -> stage.close());

        BorderPane header = new BorderPane();
        header.getStyleClass().add("site-camera-header");
        header.setLeft(titleRow);
        header.setRight(closeButton);
        header.setPadding(new Insets(16, 18, 12, 18));
        return header;
    }

    private VBox buildContent() {
        VBox content = new VBox(
                0,
                buildFieldSelector(),
                buildPreviewPane(),
                buildPendingPanel(),
                buildToolbar(),
                buildUsageFooter()
        );
        content.setPadding(new Insets(0, 18, 18, 18));
        return content;
    }

    private VBox buildFieldSelector() {
        ToggleGroup group = new ToggleGroup();
        Label activeLabel = new Label("Champ actif :");
        activeLabel.getStyleClass().add("site-camera-inline-label");

        HBox buttons = new HBox(10);
        for (CameraReservationField field : CameraReservationField.values()) {
            ToggleButton button = new ToggleButton(field.getDisplayName());
            button.getStyleClass().add("site-camera-tab");
            button.setToggleGroup(group);
            button.setSelected(field == selectedField);
            button.setOnAction(event -> {
                selectedField = field;
                refreshSelectedField();
            });
            fieldButtons.put(field, button);
            buttons.getChildren().add(button);
        }

        HBox row = new HBox(12, activeLabel, buttons);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(row);
        box.getStyleClass().add("site-camera-strip");
        box.setPadding(new Insets(10, 16, 10, 16));
        return box;
    }

    private StackPane buildPreviewPane() {
        StableCameraPreviewPane previewPane = new StableCameraPreviewPane();
        previewPane.setPrefSize(config.getPreviewWidth(), config.getPreviewHeight());

        cameraStatusLabel = new Label("Connexion a la camera...");
        cameraStatusLabel.getStyleClass().add("site-camera-overlay-status");
        cameraStatusLabel.setWrapText(true);
        cameraStatusLabel.setMaxWidth(360);
        StackPane.setAlignment(cameraStatusLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(cameraStatusLabel, new Insets(0, 0, 14, 0));

        Label liveLabel = new Label("Live camera preview");
        liveLabel.getStyleClass().add("site-camera-live-chip");
        StackPane.setAlignment(liveLabel, Pos.TOP_LEFT);
        StackPane.setMargin(liveLabel, new Insets(14, 0, 0, 14));

        landmarksCanvas = previewPane.getLandmarksCanvas();
        landmarksGraphics = landmarksCanvas.getGraphicsContext2D();
        inkCanvas = previewPane.getDrawingCanvas();
        inkGraphics = inkCanvas.getGraphicsContext2D();
        configureCanvasDrawing();
        clearCanvas();
        inkCanvas.setMouseTransparent(false);
        inkCanvas.setPickOnBounds(true);

        Label pinchLegend = new Label("pinch");
        pinchLegend.getStyleClass().add("site-camera-pinch-label");
        Label minusLegend = new Label("-");
        minusLegend.getStyleClass().add("site-camera-pinch-minus");
        VBox meterLegend = new VBox(72, pinchLegend, minusLegend);
        meterLegend.setMouseTransparent(true);
        meterLegend.setAlignment(Pos.TOP_CENTER);
        StackPane.setAlignment(meterLegend, Pos.TOP_RIGHT);
        StackPane.setMargin(meterLegend, new Insets(10, 14, 0, 0));

        StackPane overlay = new StackPane(previewPane, liveLabel, meterLegend, cameraStatusLabel);
        overlay.setPrefSize(config.getPreviewWidth(), config.getPreviewHeight());

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(28);
        clip.arcHeightProperty().set(28);
        clip.widthProperty().bind(overlay.widthProperty());
        clip.heightProperty().bind(overlay.heightProperty());
        overlay.setClip(clip);

        cameraPreviewService.bind(frame -> {
            previewPane.getImageView().setImage(frame == null ? null : frame.image());
            if (frame != null) {
                previewPane.setSourceDimensions(frame.width(), frame.height());
                renderHandTracking(frame);
            } else if (landmarksGraphics != null && landmarksCanvas != null) {
                landmarksGraphics.clearRect(0, 0, landmarksCanvas.getWidth(), landmarksCanvas.getHeight());
            }
        }, this::updateCameraStatus);
        return overlay;
    }

    private VBox buildPendingPanel() {
        drawingModeLabel = new Label("TRACAGE LIBRE : dessinez directement dans la zone camera.");
        drawingModeLabel.getStyleClass().add("site-camera-helper-note");

        Label valuesTitle = new Label("VALEURS A ENREGISTRER");
        valuesTitle.getStyleClass().add("site-camera-section-title");

        pendingBadges = new FlowPane(8, 8);
        pendingBadges.setPrefWrapLength(860);

        VBox box = new VBox(12, drawingModeLabel, valuesTitle, pendingBadges);
        box.getStyleClass().add("site-camera-bottom-panel");
        box.setPadding(new Insets(10, 18, 12, 18));
        return box;
    }

    private HBox buildToolbar() {
        Button clearButton = new Button("⌫ Effacer");
        clearButton.getStyleClass().add("site-camera-soft-button");
        clearButton.setOnAction(event -> {
            clearCanvas();
            statusLabel.setText("Zone effacee. Recommencez le trace.");
        });

        cameraToggleButton = new Button("Arreter camera");
        cameraToggleButton.getStyleClass().add("site-camera-soft-button");
        cameraToggleButton.setOnAction(event -> toggleCamera());

        resultField = new TextField();
        resultField.setPromptText("Confirmer ou corriger le texte reconnu");
        resultField.getStyleClass().add("site-camera-result-field");
        HBox.setHgrow(resultField, Priority.ALWAYS);

        recognizeButton = new Button("🔎 Reconnaître");
        recognizeButton.getStyleClass().add("site-camera-soft-button");
        recognizeButton.setOnAction(event -> recognizeOrNormalize());
        syncRecognizeButtonState();

        Label sensitivityLabel = new Label("Fluidite");
        sensitivityLabel.getStyleClass().add("site-camera-inline-label");

        pinchMeter = new ProgressBar(0);
        pinchMeter.getStyleClass().add("site-camera-meter");
        pinchMeter.setPrefWidth(110);

        Label sensitivityValue = new Label("lisse");
        sensitivityValue.getStyleClass().add("site-camera-inline-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(10, clearButton, cameraToggleButton, resultField, recognizeButton, spacer, sensitivityLabel, pinchMeter, sensitivityValue);
        toolbar.getStyleClass().add("site-camera-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(12, 18, 12, 18));
        return toolbar;
    }

    private VBox buildUsageFooter() {
        statusLabel = new Label("Pret - Champ : " + selectedField.getDisplayName());
        statusLabel.getStyleClass().add("site-camera-status-inline");
        statusLabel.setWrapText(true);

        Separator separator = new Separator();

        Label usage = new Label("Mode d'emploi : Choisissez un champ -> Dessinez librement sur la zone camera -> Cliquez Reconnaître -> Corrigez si besoin -> Enregistrer -> Transferer au formulaire.");
        usage.getStyleClass().add("site-camera-usage");
        usage.setWrapText(true);

        Button saveFieldButton = new Button("Enregistrer");
        saveFieldButton.getStyleClass().add("site-camera-button");
        saveFieldButton.setOnAction(event -> saveCurrentField());

        Button applyButton = new Button("Transferer au formulaire");
        applyButton.getStyleClass().add("site-submit-button");
        applyButton.setOnAction(event -> {
            saveCurrentFieldIfPresent();
            result = new EnumMap<>(pendingValues);
            stage.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionRow = new HBox(10, saveFieldButton, spacer, applyButton);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox footer = new VBox(10, statusLabel, separator, actionRow, usage);
        footer.getStyleClass().add("site-camera-footer");
        footer.setPadding(new Insets(12, 18, 16, 18));
        return footer;
    }

    private void configureCanvasDrawing() {
        inkCanvas.setOnMousePressed(event -> beginStroke(event.getX(), event.getY()));
        inkCanvas.setOnMouseDragged(event -> continueStroke(event.getX(), event.getY()));
        inkCanvas.setOnMouseReleased(event -> endStroke());

        inkCanvas.setOnTouchPressed(event -> {
            TouchPoint touchPoint = event.getTouchPoint();
            beginStroke(touchPoint.getX(), touchPoint.getY());
            event.consume();
        });
        inkCanvas.setOnTouchMoved(event -> {
            TouchPoint touchPoint = event.getTouchPoint();
            continueStroke(touchPoint.getX(), touchPoint.getY());
            event.consume();
        });
        inkCanvas.setOnTouchReleased(event -> {
            endStroke();
            event.consume();
        });
    }

    private void beginStroke(double x, double y) {
        lastX = x;
        lastY = y;
        lastMidX = lastX;
        lastMidY = lastY;
        hasInk = true;
        configureInkStroke();
        if (pinchMeter != null) {
            pinchMeter.setProgress(1);
        }
        statusLabel.setText("Trace actif - Champ : " + selectedField.getDisplayName());
        if (drawingModeLabel != null) {
            drawingModeLabel.setText("TRACE ACTIF : relachez pour terminer puis cliquez sur Reconnaître.");
        }
        if (resultField != null && hasInk) {
            resultField.clear();
        }

        inkGraphics.beginPath();
        inkGraphics.moveTo(lastX, lastY);
        inkGraphics.lineTo(lastX, lastY);
        inkGraphics.stroke();
        paintInkDot(lastX, lastY);
    }

    private void continueStroke(double x, double y) {
        double distance = Math.hypot(x - lastX, y - lastY);
        if (distance < DRAW_MIN_DISTANCE) {
            return;
        }

        configureInkStroke();

        int steps = Math.max(1, (int) Math.ceil(distance / DRAW_INTERPOLATION_STEP));
        double fromX = lastX;
        double fromY = lastY;
        for (int step = 1; step <= steps; step++) {
            double ratio = step / (double) steps;
            double nextX = fromX + (x - fromX) * ratio;
            double nextY = fromY + (y - fromY) * ratio;
            double midX = (lastX + nextX) / 2.0;
            double midY = (lastY + nextY) / 2.0;
            inkGraphics.beginPath();
            inkGraphics.moveTo(lastMidX, lastMidY);
            inkGraphics.quadraticCurveTo(lastX, lastY, midX, midY);
            inkGraphics.stroke();

            lastMidX = midX;
            lastMidY = midY;
            lastX = nextX;
            lastY = nextY;
        }
        paintInkDot(lastX, lastY);
    }

    private void endStroke() {
        if (pinchMeter != null) {
            pinchMeter.setProgress(0.18);
        }
        statusLabel.setText("Pret - Champ : " + selectedField.getDisplayName());
        if (drawingModeLabel != null) {
            drawingModeLabel.setText("TRACAGE LIBRE : cliquez sur Reconnaître quand le mot est termine.");
        }
    }

    private void clearCanvas() {
        inkGraphics.setFill(Color.TRANSPARENT);
        inkGraphics.clearRect(0, 0, inkCanvas.getWidth(), inkCanvas.getHeight());
        hasInk = false;
        if (pinchMeter != null) {
            pinchMeter.setProgress(0);
        }
        if (drawingModeLabel != null) {
            drawingModeLabel.setText("TRACAGE LIBRE : dessinez directement dans la zone camera.");
        }
        trackingStrokeActive = false;
        activeTrackingFrames = 0;
        lastTrackingPoint = null;
    }

    private void configureInkStroke() {
        inkGraphics.setStroke(DRAW_STROKE_COLOR);
        inkGraphics.setFill(DRAW_STROKE_COLOR);
        inkGraphics.setLineWidth(DRAW_STROKE_WIDTH);
        inkGraphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        inkGraphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    }

    private void paintInkDot(double x, double y) {
        double radius = DRAW_STROKE_WIDTH / 2.0;
        inkGraphics.fillOval(x - radius, y - radius, DRAW_STROKE_WIDTH, DRAW_STROKE_WIDTH);
    }

    private void finalizeInkBeforeRecognition() {
        if (trackingStrokeActive) {
            endStroke();
            trackingStrokeActive = false;
        }
        activeTrackingFrames = 0;
        lastTrackingPoint = null;
        trackedPointerSmoother.reset();
    }

    private void recognizeInk() {
        if (!hasInk) {
            statusLabel.setText("Tracez d'abord quelque chose sur la zone camera.");
            return;
        }
        if (recognitionRunning) {
            return;
        }

        recognitionRunning = true;
        if (recognizeButton != null) {
            recognizeButton.setDisable(true);
            recognizeButton.setText("Reconnaissance...");
        }
        if (statusLabel != null) {
            statusLabel.setText("Analyse OCR en cours...");
        }

        finalizeInkBeforeRecognition();
        WritableImage ocrImage = buildOcrReadyImage();
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return recognizer.recognize(ocrImage, selectedField);
            }
        };
        task.setOnSucceeded(event -> {
            recognitionRunning = false;
            syncRecognizeButtonState();
            String recognized = task.getValue() == null ? "" : task.getValue().trim();
            resultField.setText(recognized);
            pendingValues.put(selectedField, recognized);
            refreshPendingBadges();
            statusLabel.setText(recognized.isBlank()
                    ? "Aucun texte fiable reconnu. Corrigez manuellement si besoin."
                    : "OCR termine - Valeur injectee dans " + selectedField.getDisplayName() + ".");
        });
        task.setOnFailed(event -> {
            recognitionRunning = false;
            syncRecognizeButtonState();
            Throwable exception = task.getException();
            statusLabel.setText(exception == null || exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "OCR indisponible. Saisissez la valeur manuellement puis enregistrez."
                    : exception.getMessage());
        });

        Thread worker = new Thread(task, "camera-ocr-thread");
        worker.setDaemon(true);
        worker.start();
    }

    private void saveCurrentField() {
        String value = resultField.getText() == null ? "" : resultField.getText().trim();
        if (value.isBlank()) {
            statusLabel.setText("Saisissez ou reconnaissez une valeur avant d'enregistrer.");
            return;
        }
        pendingValues.put(selectedField, value);
        refreshPendingBadges();
        clearCanvas();
        statusLabel.setText("Valeur enregistree dans " + selectedField.getDisplayName() + ".");
    }

    private void saveCurrentFieldIfPresent() {
        String value = resultField.getText() == null ? "" : resultField.getText().trim();
        if (!value.isBlank()) {
            pendingValues.put(selectedField, value);
        }
    }

    private void refreshSelectedField() {
        fieldButtons.forEach((field, button) -> button.setSelected(field == selectedField));
        if (resultField != null) {
            resultField.setText(pendingValues.getOrDefault(selectedField, ""));
        }
        if (statusLabel != null) {
            statusLabel.setText("Pret - Champ : " + selectedField.getDisplayName());
        }
        if (recognizeButton != null) {
            syncRecognizeButtonState();
        }
    }

    private void refreshPendingBadges() {
        if (pendingBadges == null) {
            return;
        }
        pendingBadges.getChildren().clear();
        for (CameraReservationField field : CameraReservationField.values()) {
            String value = pendingValues.get(field);
            VBox card = new VBox(6);
            card.getStyleClass().add("site-camera-value-card");

            Label title = new Label(field.getDisplayName());
            title.getStyleClass().add("site-camera-value-title");

            Label content = new Label(value == null || value.isBlank() ? "-" : value);
            content.getStyleClass().add("site-camera-value-text");
            if (value == null || value.isBlank()) {
                card.getStyleClass().add("site-camera-value-card-empty");
            }

            card.getChildren().addAll(title, content);
            pendingBadges.getChildren().add(card);
        }
    }

    private WritableImage buildOcrReadyImage() {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage raw = inkCanvas.snapshot(params, null);
        int width = (int) raw.getWidth();
        int height = (int) raw.getHeight();
        PixelReader reader = raw.getPixelReader();
        boolean[][] active = new boolean[height][width];
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                if (color.getOpacity() > 0.03) {
                    active[y][x] = true;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        active = suppressNoise(active, width, height);
        minX = width;
        minY = height;
        maxX = -1;
        maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!active[y][x]) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            WritableImage blank = new WritableImage(width, height);
            PixelWriter blankWriter = blank.getPixelWriter();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    blankWriter.setColor(x, y, Color.WHITE);
                }
            }
            return blank;
        }

        minX = Math.max(0, minX - OCR_PADDING);
        minY = Math.max(0, minY - OCR_PADDING);
        maxX = Math.min(width - 1, maxX + OCR_PADDING);
        maxY = Math.min(height - 1, maxY + OCR_PADDING);

        int croppedWidth = Math.max(1, maxX - minX + 1);
        int croppedHeight = Math.max(1, maxY - minY + 1);
        int scaledWidth = croppedWidth * OCR_SCALE;
        int scaledHeight = croppedHeight * OCR_SCALE;

        WritableImage prepared = new WritableImage(scaledWidth, scaledHeight);
        PixelWriter writer = prepared.getPixelWriter();
        for (int y = 0; y < scaledHeight; y++) {
            for (int x = 0; x < scaledWidth; x++) {
                writer.setColor(x, y, Color.WHITE);
            }
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!active[y][x]) {
                    continue;
                }
                int scaledX = (x - minX) * OCR_SCALE;
                int scaledY = (y - minY) * OCR_SCALE;
                drawOcrDot(writer, scaledWidth, scaledHeight, scaledX, scaledY);
                bridgeOcrNeighbors(active, writer, width, height, minX, minY, scaledWidth, scaledHeight, x, y);
            }
        }
        return prepared;
    }

    private boolean[][] suppressNoise(boolean[][] source, int width, int height) {
        boolean[][] cleaned = new boolean[height][width];
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!source[y][x]) {
                    continue;
                }
                int neighbors = countActiveNeighbors(source, width, height, x, y, 1);
                if (neighbors >= OCR_MIN_CLUSTER) {
                    cleaned[y][x] = true;
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        if (maxX < minX || maxY < minY) {
            return source;
        }
        return cleaned;
    }

    private int countActiveNeighbors(boolean[][] active, int width, int height, int x, int y, int radius) {
        int count = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= width || py >= height) {
                    continue;
                }
                if (active[py][px]) {
                    count++;
                }
            }
        }
        return count;
    }

    private void bridgeOcrNeighbors(
            boolean[][] active,
            PixelWriter writer,
            int width,
            int height,
            int minX,
            int minY,
            int scaledWidth,
            int scaledHeight,
            int x,
            int y
    ) {
        for (int dy = -OCR_BRIDGE_RADIUS; dy <= OCR_BRIDGE_RADIUS; dy++) {
            for (int dx = -OCR_BRIDGE_RADIUS; dx <= OCR_BRIDGE_RADIUS; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int neighborX = x + dx;
                int neighborY = y + dy;
                if (neighborX < 0 || neighborY < 0 || neighborX >= width || neighborY >= height || !active[neighborY][neighborX]) {
                    continue;
                }
                int fromX = (x - minX) * OCR_SCALE;
                int fromY = (y - minY) * OCR_SCALE;
                int toX = (neighborX - minX) * OCR_SCALE;
                int toY = (neighborY - minY) * OCR_SCALE;
                drawOcrBridge(writer, scaledWidth, scaledHeight, fromX, fromY, toX, toY);
            }
        }
    }

    private void drawOcrBridge(PixelWriter writer, int width, int height, int fromX, int fromY, int toX, int toY) {
        int steps = Math.max(Math.abs(toX - fromX), Math.abs(toY - fromY));
        if (steps <= 0) {
            drawOcrDot(writer, width, height, fromX, fromY);
            return;
        }
        for (int step = 0; step <= steps; step++) {
            double ratio = step / (double) steps;
            int x = (int) Math.round(fromX + (toX - fromX) * ratio);
            int y = (int) Math.round(fromY + (toY - fromY) * ratio);
            drawOcrDot(writer, width, height, x, y);
        }
    }

    private void drawOcrDot(PixelWriter writer, int width, int height, int x, int y) {
        for (int dy = -OCR_THICKNESS_RADIUS; dy <= OCR_THICKNESS_RADIUS; dy++) {
            for (int dx = -OCR_THICKNESS_RADIUS; dx <= OCR_THICKNESS_RADIUS; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= width || py >= height) {
                    continue;
                }
                writer.setColor(px, py, Color.BLACK);
            }
        }
    }

    private void startCameraPreview() {
        try {
            handTrackingDetector.start();
        } catch (Exception ignored) {
        }
        cameraPreviewService.start();
        syncCameraToggleButton();
    }

    private void toggleCamera() {
        if (cameraPreviewService.isRunning()) {
            cameraPreviewService.stop();
        } else {
            startCameraPreview();
        }
        syncCameraToggleButton();
    }

    private void updateCameraStatus(String message) {
        if (cameraStatusLabel == null) {
            return;
        }
        cameraStatusLabel.setText(message);
        boolean hide = "Flux camera actif".equalsIgnoreCase(message == null ? "" : message.trim());
        cameraStatusLabel.setVisible(!hide);
        cameraStatusLabel.setManaged(!hide);
        syncCameraToggleButton();
    }

    private void syncCameraToggleButton() {
        if (cameraToggleButton == null) {
            return;
        }
        cameraToggleButton.setText(cameraPreviewService.isRunning() ? "Arreter camera" : "Demarrer camera");
    }

    private void renderHandTracking(CameraPreviewService.CameraFrame frame) {
        if (landmarksGraphics == null || landmarksCanvas == null || frame == null) {
            return;
        }

        TrackedHandState rawState = mirrorState(handTrackingDetector.detect(frame.bufferedImage()));
        TrackedHandState effectiveState = stabilizeTrackingState(rawState);
        HandLandmarkPoint drawingPoint = trackedPointerSmoother.update(effectiveState.drawingPoint());
        TrackedHandState smoothedState = new TrackedHandState(
                effectiveState.detected(),
                effectiveState.landmarks(),
                drawingPoint,
                effectiveState.drawingActive(),
                effectiveState.statusMessage(),
                effectiveState.gestureStrength()
        );

        handTrackingOverlayRenderer.render(
                landmarksGraphics,
                landmarksCanvas.getWidth(),
                landmarksCanvas.getHeight(),
                smoothedState
        );

        if (pinchMeter != null) {
            pinchMeter.setProgress(Math.max(0.0, Math.min(1.0, smoothedState.gestureStrength())));
        }

        driveDrawingFromTrackedHand(smoothedState);
        if (rawState.detected()) {
            if (drawingModeLabel != null) {
                drawingModeLabel.setText(smoothedState.drawingActive()
                        ? "GESTE ECRITURE ACTIF : le doigt pilote le trace."
                        : "MAIN OUVERTE : pause. Fermez la main ou faites un pinch pour tracer.");
            }
        } else if (missingTrackingFrames > TRACKING_GRACE_FRAMES && drawingModeLabel != null) {
            drawingModeLabel.setText("Main non detectee : placez votre main au centre de la zone camera.");
        }
    }

    private void driveDrawingFromTrackedHand(TrackedHandState state) {
        if (state == null || state.drawingPoint() == null) {
            if (trackingStrokeActive) {
                endStroke();
                trackingStrokeActive = false;
            }
            activeTrackingFrames = 0;
            lastTrackingPoint = null;
            return;
        }

        if (lastTrackingPoint != null) {
            double jump = Math.hypot(state.drawingPoint().x() - lastTrackingPoint.x(), state.drawingPoint().y() - lastTrackingPoint.y());
            if (jump > MAX_TRACKING_JUMP) {
                if (trackingStrokeActive) {
                    endStroke();
                    trackingStrokeActive = false;
                }
                activeTrackingFrames = 0;
                lastTrackingPoint = state.drawingPoint();
                return;
            }
        }
        lastTrackingPoint = state.drawingPoint();

        double x = state.drawingPoint().x() * inkCanvas.getWidth();
        double y = state.drawingPoint().y() * inkCanvas.getHeight();

        if (state.drawingActive()) {
            activeTrackingFrames++;
            if (!trackingStrokeActive && activeTrackingFrames >= TRACKING_ACTIVATION_FRAMES) {
                beginStroke(x, y);
                trackingStrokeActive = true;
            } else if (trackingStrokeActive) {
                continueStroke(x, y);
            }
        } else if (trackingStrokeActive) {
            endStroke();
            trackingStrokeActive = false;
            activeTrackingFrames = 0;
        } else {
            activeTrackingFrames = 0;
        }
    }

    private TrackedHandState stabilizeTrackingState(TrackedHandState rawState) {
        if (rawState != null && rawState.detected()) {
            missingTrackingFrames = 0;
            lastDetectedState = rawState;
            return rawState;
        }

        missingTrackingFrames++;
        if (lastDetectedState != null && missingTrackingFrames <= TRACKING_GRACE_FRAMES) {
            return new TrackedHandState(
                    true,
                    lastDetectedState.landmarks(),
                    lastDetectedState.drawingPoint(),
                    false,
                    "Suivi stabilise.",
                    lastDetectedState.gestureStrength()
            );
        }

        lastDetectedState = null;
        trackedPointerSmoother.reset();
        return rawState == null ? TrackedHandState.unavailable("Main non detectee.") : rawState;
    }

    private TrackedHandState mirrorState(TrackedHandState state) {
        if (state == null) {
            return TrackedHandState.unavailable("Main non detectee.");
        }
        if (!state.detected()) {
            return state;
        }

        List<HandLandmarkPoint> mirroredLandmarks = state.landmarks() == null
                ? List.of()
                : state.landmarks().stream()
                .map(this::mirrorPoint)
                .collect(Collectors.toList());
        HandLandmarkPoint mirroredDrawingPoint = mirrorPoint(state.drawingPoint());
        return new TrackedHandState(
                true,
                mirroredLandmarks,
                mirroredDrawingPoint,
                state.drawingActive(),
                state.statusMessage(),
                state.gestureStrength()
        );
    }

    private HandLandmarkPoint mirrorPoint(HandLandmarkPoint point) {
        if (point == null) {
            return null;
        }
        return new HandLandmarkPoint(1.0 - point.x(), point.y(), point.confidence());
    }

    private void recognizeOrNormalize() {
        if (resultField == null) {
            return;
        }

        if (!recognizer.isAvailable() && statusLabel != null) {
            String message = recognizer.describeAvailability();
            statusLabel.setText(message == null || message.isBlank()
                    ? "OCR local indisponible, tentative de secours au clic sur Reconnaître."
                    : message);
        }

        if (hasInk) {
            recognizeInk();
            return;
        }

        String current = resultField.getText() == null ? "" : resultField.getText().trim();
        if (current.isBlank()) {
            statusLabel.setText("Dessinez d'abord quelque chose sur la zone camera.");
            return;
        }

        String normalized = selectedField == CameraReservationField.TELEPHONE
                ? current.replaceAll("[^+0-9]", "")
                : normalizeText(current);
        resultField.setText(normalized);
        statusLabel.setText("Valeur nettoyee - Champ : " + selectedField.getDisplayName());
    }

    private void syncRecognizeButtonState() {
        if (recognizeButton == null) {
            return;
        }
        recognizeButton.setDisable(recognitionRunning);
        recognizeButton.setText(recognitionRunning ? "Reconnaissance..." : "🔎 Reconnaître");
    }

    private String normalizeText(String raw) {
        String compact = raw.replaceAll("\\s+", " ").trim().toLowerCase();
        if (compact.isBlank()) {
            return "";
        }

        String[] words = compact.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
            if (i < words.length - 1) {
                builder.append(' ');
            }
        }
        return builder.toString();
    }
}
