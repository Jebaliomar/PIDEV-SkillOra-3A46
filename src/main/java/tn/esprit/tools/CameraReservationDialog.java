package tn.esprit.tools;

import javafx.animation.AnimationTimer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class CameraReservationDialog {
    private static final Logger LOG = LoggerFactory.getLogger(CameraReservationDialog.class);
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
    private final CoordinateMapper coordinateMapper = new CoordinateMapper();
    private final TrackedPointerSmoother trackedPointerSmoother = TrackedPointerSmoother.fromConfig(config);
    private final AssistedTrackedPen assistedTrackedPen = new AssistedTrackedPen();
    private final OneEuroPointFilter guidePointFilter = new OneEuroPointFilter();
    private CameraReservationField selectedField = CameraReservationField.PRENOM;
    private boolean directScreenMode = true;
    private Canvas landmarksCanvas;
    private GraphicsContext landmarksGraphics;
    private Canvas inkCanvas;
    private GraphicsContext inkGraphics;
    private StableCameraPreviewPane previewPane;
    private HandwritingStrokeEngine strokeEngine;
    private ProgressBar pinchMeter;
    private Label statusLabel;
    private Label cameraStatusLabel;
    private Label trackingDebugLabel;
    private Label drawingModeLabel;
    private Label activeFieldBadgeLabel;
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
    private HandLandmarkPoint lastTrackingPoint;
    private TrackedHandState lastDetectedState;
    private Stage stage;
    private EnumMap<CameraReservationField, String> result;
    private AnimationTimer renderLoop;
    private CameraPreviewService.CameraFrame lastRenderedFrame;
    private long lastRenderTick;
    private long lastDebugLogNanos;
    private static final int TRACKING_GRACE_FRAMES = 8;
    private static final int DRAWING_GRACE_FRAMES = 4;
    private static final double MAX_TRACKING_JUMP = 0.12;
    private static final long DEBUG_LOG_INTERVAL_NANOS = 300_000_000L;
    private int drawingGraceFrames;

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
        stage.setMinWidth(1060);
        stage.setMinHeight(720);

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("site-root", "site-camera-dialog");
        root.setTop(buildHeader());
        root.setCenter(buildContent());

        Scene scene = new Scene(root, 1160, 780);
        if (sourceScene != null) {
            scene.getStylesheets().addAll(sourceScene.getStylesheets());
            if (sourceScene.getRoot().getStyleClass().contains("theme-dark")) {
                root.getStyleClass().add("theme-dark");
            }
        }

        stage.setOnShown(event -> startCameraPreview());
        stage.setOnHidden(event -> {
            stopRenderLoop();
            cameraPreviewService.stop();
            handTrackingDetector.stop();
            assistedTrackedPen.reset();
            guidePointFilter.reset();
        });
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.toFront();
        refreshSelectedField();
        refreshPendingBadges();
        stage.showAndWait();
    }

    private BorderPane buildHeader() {
        Label title = new Label("Reservation Camera");
        title.getStyleClass().add("site-camera-modal-title");

        Label subtitle = new Label("Ecriture gestuelle et reconnaissance OCR");
        subtitle.getStyleClass().add("site-camera-header-subtitle");

        Label badge = new Label("Live OCR");
        badge.getStyleClass().add("site-camera-top-badge");

        VBox titleBlock = new VBox(3, title, subtitle);
        HBox titleRow = new HBox(12, titleBlock, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Button closeButton = new Button("✕");
        closeButton.getStyleClass().add("site-camera-close");
        closeButton.setOnAction(event -> stage.close());

        BorderPane header = new BorderPane();
        header.getStyleClass().add("site-camera-header");
        header.setLeft(titleRow);
        header.setRight(closeButton);
        header.setPadding(new Insets(18, 22, 16, 22));
        return header;
    }

    private HBox buildContent() {
        VBox previewCard = buildPreviewCard();
        VBox.setVgrow(previewCard, Priority.ALWAYS);

        VBox mainPanel = new VBox(14, previewCard, buildToolbar());
        mainPanel.getStyleClass().add("site-camera-main-panel");
        mainPanel.setFillWidth(true);
        HBox.setHgrow(mainPanel, Priority.ALWAYS);

        VBox sidePanel = new VBox(
                14,
                buildFieldSelector(),
                buildInputModeSelector(),
                buildPendingPanel(),
                buildUsageFooter()
        );
        sidePanel.getStyleClass().add("site-camera-side-panel");
        sidePanel.setPrefWidth(336);
        sidePanel.setMinWidth(312);
        sidePanel.setMaxWidth(360);

        HBox content = new HBox(18, mainPanel, sidePanel);
        content.getStyleClass().add("site-camera-workspace");
        content.setPadding(new Insets(18, 22, 22, 22));
        return content;
    }

    private VBox buildPreviewCard() {
        Label title = new Label("Zone camera");
        title.getStyleClass().add("site-camera-panel-title");

        Label subtitle = new Label("Tracez le texte dans la zone sombre.");
        subtitle.getStyleClass().add("site-camera-panel-subtitle");

        activeFieldBadgeLabel = new Label("Champ : " + selectedField.getDisplayName());
        activeFieldBadgeLabel.getStyleClass().add("site-camera-active-field-pill");

        VBox titleBlock = new VBox(2, title, subtitle);
        HBox header = new HBox(12, titleBlock, new Region(), activeFieldBadgeLabel);
        header.getStyleClass().add("site-camera-panel-header");
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);

        StackPane preview = buildPreviewPane();
        VBox.setVgrow(preview, Priority.ALWAYS);

        VBox card = new VBox(14, header, preview);
        card.getStyleClass().add("site-camera-preview-card");
        card.setFillWidth(true);
        return card;
    }

    private VBox buildFieldSelector() {
        ToggleGroup group = new ToggleGroup();
        Label activeLabel = new Label("Champ a remplir");
        activeLabel.getStyleClass().add("site-camera-inline-label");

        HBox buttons = new HBox(10);
        for (CameraReservationField field : CameraReservationField.values()) {
            ToggleButton button = new ToggleButton(field.getDisplayName());
            button.getStyleClass().add("site-camera-tab");
            button.setToggleGroup(group);
            button.setSelected(field == selectedField);
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setOnAction(event -> {
                selectedField = field;
                refreshSelectedField();
            });
            fieldButtons.put(field, button);
            buttons.getChildren().add(button);
        }

        HBox.setHgrow(buttons, Priority.ALWAYS);

        VBox box = new VBox(10, activeLabel, buttons);
        box.getStyleClass().addAll("site-camera-strip", "site-camera-control-card");
        box.setPadding(new Insets(14, 16, 16, 16));
        return box;
    }

    private VBox buildInputModeSelector() {
        ToggleGroup modeGroup = new ToggleGroup();

        Label modeLabel = new Label("Mode d'ecriture");
        modeLabel.getStyleClass().add("site-camera-inline-label");

        ToggleButton directButton = new ToggleButton("Ecran assisté");
        directButton.getStyleClass().add("site-camera-tab");
        directButton.setToggleGroup(modeGroup);
        directButton.setSelected(true);
        directButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(directButton, Priority.ALWAYS);
        directButton.setOnAction(event -> {
            directScreenMode = true;
            stopTrackingStroke();
            if (drawingModeLabel != null) {
                drawingModeLabel.setText("MODE ECRAN ASSISTE : dessinez directement sur l'ecran avec la souris ou le tactile.");
            }
        });

        ToggleButton trackingButton = new ToggleButton("Suivi caméra");
        trackingButton.getStyleClass().add("site-camera-tab");
        trackingButton.setToggleGroup(modeGroup);
        trackingButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(trackingButton, Priority.ALWAYS);
        trackingButton.setOnAction(event -> {
            directScreenMode = false;
            if (drawingModeLabel != null) {
                drawingModeLabel.setText("MODE SUIVI CAMERA : fermez la main ou faites un pinch pour tracer.");
            }
        });

        HBox row = new HBox(10, directButton, trackingButton);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox box = new VBox(10, modeLabel, row);
        box.getStyleClass().addAll("site-camera-strip", "site-camera-control-card");
        box.setPadding(new Insets(14, 16, 16, 16));
        return box;
    }

    private StackPane buildPreviewPane() {
        previewPane = new StableCameraPreviewPane();
        previewPane.setPrefSize(760, config.getPreviewHeight());
        previewPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        cameraStatusLabel = new Label("Connexion a la camera...");
        cameraStatusLabel.getStyleClass().add("site-camera-overlay-status");
        cameraStatusLabel.setWrapText(true);
        cameraStatusLabel.setMaxWidth(360);
        StackPane.setAlignment(cameraStatusLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(cameraStatusLabel, new Insets(0, 0, 14, 0));

        trackingDebugLabel = new Label("debug tracking: attente de donnees");
        trackingDebugLabel.getStyleClass().add("site-camera-debug-label");
        trackingDebugLabel.setWrapText(true);
        trackingDebugLabel.setMaxWidth(340);
        trackingDebugLabel.setVisible(false);
        trackingDebugLabel.setManaged(false);
        StackPane.setAlignment(trackingDebugLabel, Pos.BOTTOM_LEFT);
        StackPane.setMargin(trackingDebugLabel, new Insets(0, 0, 14, 14));

        Label liveLabel = new Label("Live camera preview");
        liveLabel.getStyleClass().add("site-camera-live-chip");
        StackPane.setAlignment(liveLabel, Pos.TOP_LEFT);
        StackPane.setMargin(liveLabel, new Insets(14, 0, 0, 14));

        landmarksCanvas = previewPane.getLandmarksCanvas();
        landmarksGraphics = landmarksCanvas.getGraphicsContext2D();
        inkCanvas = previewPane.getDrawingCanvas();
        inkGraphics = inkCanvas.getGraphicsContext2D();
        strokeEngine = new HandwritingStrokeEngine(inkCanvas, inkGraphics, config);
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

        StackPane overlay = new StackPane(previewPane, liveLabel, meterLegend, cameraStatusLabel, trackingDebugLabel);
        overlay.setMinSize(620, 390);
        overlay.setPrefSize(760, config.getPreviewHeight());
        overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(28);
        clip.arcHeightProperty().set(28);
        clip.widthProperty().bind(overlay.widthProperty());
        clip.heightProperty().bind(overlay.heightProperty());
        overlay.setClip(clip);

        cameraPreviewService.bind(null, this::updateCameraStatus);
        return overlay;
    }

    private VBox buildPendingPanel() {
        drawingModeLabel = new Label("MODE ECRAN ASSISTE : dessinez directement sur l'ecran avec la souris ou le tactile.");
        drawingModeLabel.getStyleClass().add("site-camera-helper-note");

        Label valuesTitle = new Label("VALEURS A ENREGISTRER");
        valuesTitle.getStyleClass().add("site-camera-section-title");

        pendingBadges = new FlowPane(8, 8);
        pendingBadges.setPrefWrapLength(300);

        VBox box = new VBox(12, drawingModeLabel, valuesTitle, pendingBadges);
        box.getStyleClass().addAll("site-camera-bottom-panel", "site-camera-control-card");
        box.setPadding(new Insets(14, 16, 16, 16));
        return box;
    }

    private VBox buildToolbar() {
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
        recognizeButton.getStyleClass().addAll("site-camera-soft-button", "site-camera-recognize-button");
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

        HBox resultRow = new HBox(10, resultField, recognizeButton);
        resultRow.getStyleClass().add("site-camera-result-row");
        resultRow.setAlignment(Pos.CENTER_LEFT);

        HBox utilityRow = new HBox(10, clearButton, cameraToggleButton, spacer, sensitivityLabel, pinchMeter, sensitivityValue);
        utilityRow.getStyleClass().add("site-camera-utility-row");
        utilityRow.setAlignment(Pos.CENTER_LEFT);

        VBox toolbar = new VBox(10, resultRow, utilityRow);
        toolbar.getStyleClass().add("site-camera-toolbar");
        toolbar.setPadding(new Insets(14, 16, 16, 16));
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

        Button saveFieldButton = new Button("Enregistrer le champ");
        saveFieldButton.getStyleClass().addAll("site-camera-soft-button", "site-camera-save-button");
        saveFieldButton.setMaxWidth(Double.MAX_VALUE);
        saveFieldButton.setOnAction(event -> saveCurrentField());

        Button applyButton = new Button("Transferer au formulaire");
        applyButton.getStyleClass().add("site-submit-button");
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setOnAction(event -> {
            saveCurrentFieldIfPresent();
            result = new EnumMap<>(pendingValues);
            stage.close();
        });

        VBox actionRow = new VBox(8, saveFieldButton, applyButton);

        VBox footer = new VBox(10, statusLabel, separator, actionRow, usage);
        footer.getStyleClass().addAll("site-camera-footer", "site-camera-control-card");
        footer.setPadding(new Insets(14, 16, 16, 16));
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
        lastMidX = x;
        lastMidY = y;
        if (strokeEngine != null) {
            strokeEngine.beginStroke(x, y);
            hasInk = strokeEngine.hasInk();
        } else {
            hasInk = true;
            configureInkStroke();
            inkGraphics.beginPath();
            inkGraphics.moveTo(lastX, lastY);
            inkGraphics.lineTo(lastX, lastY);
            inkGraphics.stroke();
            paintInkDot(lastX, lastY);
        }
        if (pinchMeter != null) {
            pinchMeter.setProgress(1);
        }
        statusLabel.setText("Trace actif - Champ : " + selectedField.getDisplayName());
        if (drawingModeLabel != null) {
            drawingModeLabel.setText(directScreenMode
                    ? "TRACE ECRAN ACTIF : relachez pour terminer puis cliquez sur Reconnaître."
                    : "TRACE CAMERA ACTIVE : relachez pour terminer puis cliquez sur Reconnaître.");
        }
        if (resultField != null && hasInk) {
            resultField.clear();
        }
    }

    private void continueStroke(double x, double y) {
        if (strokeEngine != null) {
            strokeEngine.continueStroke(x, y);
            hasInk = strokeEngine.hasInk();
            lastX = x;
            lastY = y;
            return;
        }

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
        if (strokeEngine != null) {
            strokeEngine.endStroke();
            hasInk = strokeEngine.hasInk();
        }
        if (pinchMeter != null) {
            pinchMeter.setProgress(0.18);
        }
        statusLabel.setText("Pret - Champ : " + selectedField.getDisplayName());
        if (drawingModeLabel != null) {
            drawingModeLabel.setText(directScreenMode
                    ? "MODE ECRAN ASSISTE : cliquez sur Reconnaître quand le mot est termine."
                    : "MODE SUIVI CAMERA : cliquez sur Reconnaître quand le mot est termine.");
        }
    }

    private void clearCanvas() {
        if (strokeEngine != null) {
            strokeEngine.clear();
            hasInk = strokeEngine.hasInk();
        } else {
            inkGraphics.setFill(Color.TRANSPARENT);
            inkGraphics.clearRect(0, 0, inkCanvas.getWidth(), inkCanvas.getHeight());
            hasInk = false;
        }
        if (pinchMeter != null) {
            pinchMeter.setProgress(0);
        }
        if (drawingModeLabel != null) {
            drawingModeLabel.setText(directScreenMode
                    ? "MODE ECRAN ASSISTE : dessinez directement sur l'ecran avec la souris ou le tactile."
                    : "MODE SUIVI CAMERA : fermez la main ou faites un pinch pour tracer.");
        }
        trackingStrokeActive = false;
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
        if (activeFieldBadgeLabel != null) {
            activeFieldBadgeLabel.setText("Champ : " + selectedField.getDisplayName());
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
            content.setWrapText(true);
            content.setMaxWidth(118);
            if (value == null || value.isBlank()) {
                card.getStyleClass().add("site-camera-value-card-empty");
            }

            card.getChildren().addAll(title, content);
            pendingBadges.getChildren().add(card);
        }
    }

    private WritableImage buildOcrReadyImage() {
        if (strokeEngine != null) {
            return CanvasOcrPreprocessor.prepare(strokeEngine.snapshot());
        }

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
        startRenderLoop();
        cameraPreviewService.start();
        syncCameraToggleButton();
    }

    private void toggleCamera() {
        if (cameraPreviewService.isRunning()) {
            cameraPreviewService.stop();
            clearPreviewFrame();
        } else {
            startCameraPreview();
        }
        syncCameraToggleButton();
    }

    private void startRenderLoop() {
        if (renderLoop != null) {
            return;
        }
        renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastRenderTick < getRenderIntervalNanos()) {
                    return;
                }
                lastRenderTick = now;
                renderLatestFrame();
            }
        };
        lastRenderTick = 0L;
        renderLoop.start();
    }

    private void stopRenderLoop() {
        if (renderLoop != null) {
            renderLoop.stop();
            renderLoop = null;
        }
        lastRenderTick = 0L;
        lastRenderedFrame = null;
    }

    private void renderLatestFrame() {
        CameraPreviewService.CameraFrame frame = cameraPreviewService.getLatestFrame();
        if (frame == null) {
            if (lastRenderedFrame != null) {
                clearPreviewFrame();
            }
            return;
        }
        if (frame == lastRenderedFrame) {
            return;
        }

        lastRenderedFrame = frame;
        if (previewPane != null) {
            previewPane.getImageView().setImage(frame.image());
            previewPane.setSourceDimensions(frame.width(), frame.height());
        }
        renderHandTracking(frame);
    }

    private void clearPreviewFrame() {
        lastRenderedFrame = null;
        if (previewPane != null) {
            previewPane.getImageView().setImage(null);
        }
        if (landmarksGraphics != null && landmarksCanvas != null) {
            landmarksGraphics.clearRect(0, 0, landmarksCanvas.getWidth(), landmarksCanvas.getHeight());
        }
    }

    private long getRenderIntervalNanos() {
        int fps = Math.max(30, config.getRenderFps());
        return 1_000_000_000L / fps;
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

        // ÉTAPE 1: Détecter la main (coordonnées brutes caméra)
        TrackedHandState detectorState = handTrackingDetector.detect(frame.bufferedImage());

        // ÉTAPE 2: Stabiliser l'état de suivi (gérer les frames manquantes)
        TrackedHandState effectiveState = stabilizeTrackingState(detectorState);

        // ÉTAPE 3: Déterminer si on peut afficher le point rouge
        boolean canShowRedPoint = effectiveState.phase() == HandTrackingPhase.PINCH_ACTIVE || effectiveState.phase() == HandTrackingPhase.DRAWING;
        HandLandmarkPoint trackingBasePoint = effectiveState.drawingPoint();
        HandLandmarkPoint visualGuidePoint = canShowRedPoint ? trackingBasePoint : null;

        // ÉTAPE 4: Appliquer le smoothing (en coordonnées normalisées)
        HandLandmarkPoint smoothedFingerPoint = trackedPointerSmoother.update(trackingBasePoint);
        HandLandmarkPoint finalTrackingPoint = canShowRedPoint
                ? guidePointFilter.filter(smoothedFingerPoint)
                : null;

        // ÉTAPE 5: Gérer la phase de dessin et les transitions
        HandTrackingPhase visualPhase = effectiveState.phase();
        boolean drawingEnabled = false;
        if (visualPhase == HandTrackingPhase.PINCH_ACTIVE && finalTrackingPoint != null) {
            visualPhase = HandTrackingPhase.DRAWING;
            drawingEnabled = true;
            drawingGraceFrames = DRAWING_GRACE_FRAMES;
        } else if (visualPhase == HandTrackingPhase.DRAWING && finalTrackingPoint != null) {
            drawingEnabled = true;
            drawingGraceFrames = DRAWING_GRACE_FRAMES;
        } else if (drawingGraceFrames > 0 && trackingStrokeActive && lastTrackingPoint != null) {
            visualPhase = HandTrackingPhase.DRAWING;
            finalTrackingPoint = lastTrackingPoint;
            drawingEnabled = true;
            drawingGraceFrames--;
        } else {
            drawingGraceFrames = 0;
        }

        // ÉTAPE 6: Créer l'état final avec les coordonnées brutes pour le renderer
        // (le renderer appliquera le mirroring automatiquement via CoordinateMapper)
        TrackedHandState smoothedState = new TrackedHandState(
                effectiveState.detected(),
                effectiveState.landmarks(),
                visualGuidePoint,
                finalTrackingPoint,
                drawingEnabled,
                visualPhase,
                effectiveState.statusMessage(),
                effectiveState.gestureStrength(),
                effectiveState.pinchDistance()
        );

        // ÉTAPE 7: Mettre à jour les dimensions du mapper et du renderer
        coordinateMapper.updateCanvasDimensions(landmarksCanvas.getWidth(), landmarksCanvas.getHeight());
        handTrackingOverlayRenderer.updateCanvasDimensions(landmarksCanvas.getWidth(), landmarksCanvas.getHeight());

        // ÉTAPE 8: Rendre l'overlay des points rouges
        handTrackingOverlayRenderer.render(
                landmarksGraphics,
                landmarksCanvas.getWidth(),
                landmarksCanvas.getHeight(),
                smoothedState
        );

        if (pinchMeter != null) {
            pinchMeter.setProgress(Math.max(0.0, Math.min(1.0, smoothedState.gestureStrength())));
        }

        // ÉTAPE 9: Piloter le dessin à partir de la main suivi
        driveDrawingFromTrackedHand(smoothedState);

        // ÉTAPE 10: Mettre à jour les labels de debug
        updateTrackingDebug(detectorState, smoothedState);
        logTrackingState(detectorState, smoothedState);

        if (detectorState.detected()) {
            if (drawingModeLabel != null) {
                if (directScreenMode) {
                    drawingModeLabel.setText("MODE ECRAN ASSISTE : dessinez directement sur l'ecran avec la souris ou le tactile.");
                } else {
                    drawingModeLabel.setText(switch (smoothedState.phase()) {
                        case PINCH_ACTIVE -> "PINCH ACTIF : point rouge visible, pret a dessiner.";
                        case DRAWING -> "DESSIN ACTIF : point rouge visible, trace en cours.";
                        case HAND_OPEN -> "MAIN OUVERTE : point rouge cache, dessin desactive.";
                        case NO_HAND -> "Main non detectee : placez votre main au centre de la zone camera.";
                    });
                }
            }
        } else if (missingTrackingFrames > TRACKING_GRACE_FRAMES && drawingModeLabel != null) {
            drawingModeLabel.setText(directScreenMode
                    ? "MODE ECRAN ASSISTE : dessinez directement sur l'ecran avec la souris ou le tactile."
                    : "Main non detectee : placez votre main au centre de la zone camera.");
        }
    }

    private void driveDrawingFromTrackedHand(TrackedHandState state) {
        if (directScreenMode) {
            stopTrackingStroke();
            return;
        }

        if (state == null || state.phase() != HandTrackingPhase.DRAWING || state.drawingPoint() == null) {
            stopTrackingStroke();
            return;
        }

        if (lastTrackingPoint != null) {
            double jump = Math.hypot(state.drawingPoint().x() - lastTrackingPoint.x(), state.drawingPoint().y() - lastTrackingPoint.y());
            if (jump > MAX_TRACKING_JUMP) {
                lastTrackingPoint = limitTrackingJump(lastTrackingPoint, state.drawingPoint());
            } else {
                lastTrackingPoint = state.drawingPoint();
            }
        } else {
            lastTrackingPoint = state.drawingPoint();
        }

        // Convertir les coordonnées normalisées en pixels du canvas en utilisant CoordinateMapper
        HandLandmarkPoint canvasPoint = coordinateMapper.normalizedToCanvasPixels(lastTrackingPoint);
        double x = canvasPoint.x();
        double y = canvasPoint.y();

        boolean writingVisibleAndActive = state.drawingActive() && state.drawingPoint() != null;
        if (writingVisibleAndActive) {
            if (!trackingStrokeActive) {
                beginStroke(x, y);
                trackingStrokeActive = true;
            } else if (trackingStrokeActive) {
                continueStroke(x, y);
            }
        } else if (trackingStrokeActive) {
            endStroke();
            trackingStrokeActive = false;
        }
    }

    private void stopTrackingStroke() {
        if (trackingStrokeActive) {
            endStroke();
            trackingStrokeActive = false;
        }
        drawingGraceFrames = 0;
        lastTrackingPoint = null;
        assistedTrackedPen.reset();
        guidePointFilter.reset();
    }

    private TrackedHandState stabilizeTrackingState(TrackedHandState rawState) {
        if (rawState != null && rawState.detected()) {
            missingTrackingFrames = 0;
            lastDetectedState = rawState;
            return rawState;
        }

        missingTrackingFrames++;
        if (lastDetectedState != null && missingTrackingFrames <= TRACKING_GRACE_FRAMES) {
            HandTrackingPhase heldPhase = lastDetectedState.phase() == HandTrackingPhase.PINCH_ACTIVE || lastDetectedState.phase() == HandTrackingPhase.DRAWING
                    ? HandTrackingPhase.DRAWING
                    : HandTrackingPhase.HAND_OPEN;
            return new TrackedHandState(
                    true,
                    lastDetectedState.landmarks(),
                    heldPhase == HandTrackingPhase.DRAWING ? lastDetectedState.guidePoint() : null,
                    heldPhase == HandTrackingPhase.DRAWING ? lastDetectedState.drawingPoint() : null,
                    heldPhase == HandTrackingPhase.DRAWING,
                    heldPhase,
                    "Suivi stabilise.",
                    lastDetectedState.gestureStrength(),
                    lastDetectedState.pinchDistance()
            );
        }

         lastDetectedState = null;
         trackedPointerSmoother.reset();
         assistedTrackedPen.reset();
         guidePointFilter.reset();
         return rawState == null ? TrackedHandState.unavailable("Main non detectee.") : rawState;
     }

     private void updateTrackingDebug(TrackedHandState detectorState, TrackedHandState finalState) {
        if (trackingDebugLabel == null) {
            return;
        }
        HandLandmarkPoint rawPoint = detectorState == null ? null : detectorState.drawingPoint();
        HandLandmarkPoint finalPoint = finalState == null ? null : finalState.guidePoint();
        HandLandmarkPoint canvasPoint = finalPoint == null ? null : coordinateMapper.normalizedToCanvasPixels(finalPoint);
        trackingDebugLabel.setText(String.format(
                "raw=(%s,%s) canvas=(%s,%s)",
                formatCoord(rawPoint == null ? null : rawPoint.x()),
                formatCoord(rawPoint == null ? null : rawPoint.y()),
                formatCoord(canvasPoint == null ? null : canvasPoint.x()),
                formatCoord(canvasPoint == null ? null : canvasPoint.y())
        ));
    }

    private void logTrackingState(TrackedHandState detectorState, TrackedHandState state) {
        if (!LOG.isInfoEnabled()) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastDebugLogNanos < DEBUG_LOG_INTERVAL_NANOS) {
            return;
        }
        lastDebugLogNanos = now;
        HandLandmarkPoint point = state == null ? null : state.guidePoint();
        HandLandmarkPoint rawPoint = detectorState == null ? null : detectorState.drawingPoint();
        HandLandmarkPoint canvasPoint = point == null ? null : coordinateMapper.normalizedToCanvasPixels(point);
        LOG.info(
                "handDetected={}, pinchDistance={}, pinchActive={}, drawingActive={}, currentState={}, rawFingertip=({}, {}), canvasPoint=({}, {})",
                state != null && state.detected(),
                state == null ? -1.0 : state.pinchDistance(),
                state != null && (state.phase() == HandTrackingPhase.PINCH_ACTIVE || state.phase() == HandTrackingPhase.DRAWING),
                state != null && state.drawingActive(),
                state == null ? HandTrackingPhase.NO_HAND : state.phase(),
                formatCoord(rawPoint == null ? null : rawPoint.x()),
                formatCoord(rawPoint == null ? null : rawPoint.y()),
                formatCoord(canvasPoint == null ? null : canvasPoint.x()),
                formatCoord(canvasPoint == null ? null : canvasPoint.y())
        );
    }

    private String formatCoord(Double value) {
        return value == null ? "-" : String.format("%.4f", value);
    }


    private HandLandmarkPoint limitTrackingJump(HandLandmarkPoint previous, HandLandmarkPoint candidate) {
        if (previous == null) {
            return candidate;
        }
        double deltaX = candidate.x() - previous.x();
        double deltaY = candidate.y() - previous.y();
        double distance = Math.hypot(deltaX, deltaY);
        if (distance <= MAX_TRACKING_JUMP || distance == 0.0) {
            return candidate;
        }
        double ratio = MAX_TRACKING_JUMP / distance;
        return new HandLandmarkPoint(
                previous.x() + deltaX * ratio,
                previous.y() + deltaY * ratio,
                candidate.confidence()
        );
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
