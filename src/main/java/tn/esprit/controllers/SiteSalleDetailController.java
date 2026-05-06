package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.KeyFrame;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.controllers.front.SkilloraNavbarController;
import tn.esprit.services.SiteSalleDetailService;
import tn.esprit.tools.AppIcons;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.BotpressSupportWidget;
import tn.esprit.tools.QrGen;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;
import tn.esprit.tools.VisionTestModal;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javafx.util.Duration;

public class SiteSalleDetailController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_SNAKE_LEVEL = 30;
    private static final int SNAKE_COLUMNS = 30;
    private static final int SNAKE_ROWS = 18;
    private static final String REWARD_QR_TEXT = "Place VIP aande SkillORA";

    @FXML
    private Label heroTitleLabel;
    @FXML
    private Button eventLinkButton;
    @FXML
    private Label locationLabel;
    @FXML
    private Label durationValueLabel;
    @FXML
    private Label capacityValueLabel;
    @FXML
    private FlowPane equipmentFlow;
    @FXML
    private Label priceLabel;
    @FXML
    private Label reservedCountLabel;
    @FXML
    private Label availableCountLabel;
    @FXML
    private Label selectedCountLabel;
    @FXML
    private Label seatsSummaryLabel;
    @FXML
    private GridPane seatsGrid;
    @FXML
    private Label viewerTitleLabel;
    @FXML
    private Label viewerSubtitleLabel;
    @FXML
    private StackPane viewerFrame;
    @FXML
    private StackPane snakeFrame;
    @FXML
    private Canvas snakeCanvas;
    @FXML
    private Label snakeLevelLabel;
    @FXML
    private Label snakeScoreLabel;
    @FXML
    private VBox snakeOverlay;
    @FXML
    private Label snakeOverlayTitle;
    @FXML
    private Label snakeOverlayMessage;
    @FXML
    private VBox snakeRewardPane;
    @FXML
    private ImageView rewardQrImageView;
    @FXML
    private Button snakeStartButton;
    @FXML
    private Button snakeRestartButton;
    @FXML
    private Button snakeOverlayStartButton;
    @FXML
    private Button snakeOverlayRestartButton;
    @FXML
    private StackPane modelFrame;
    @FXML
    private WebView modelWebView;
    @FXML
    private WebView rouletteWebView;
    @FXML
    private VBox modelPlaceholderBox;
    @FXML
    private Label modelStatusTitle;
    @FXML
    private Label modelStatusSubtitle;
    @FXML
    private Button open3dViewerButton;
    @FXML
    private Button playSnakeButton;
    @FXML
    private Button modelCloseButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Button reservationButton;
    @FXML
    private ImageView coverImageView;
    @FXML
    private StackPane coverFallbackPane;
    @FXML
    private MenuButton userMenu;
    @FXML
    private MenuItem menuProfile;
    @FXML
    private MenuItem menuSettings;
    @FXML
    private SkilloraNavbarController navbarController;

    private final SiteSalleDetailService detailService = new SiteSalleDetailService();
    private final Set<Integer> selectedSeats = new LinkedHashSet<>();
    private Set<Integer> reservedSeats = Set.of();

    private Event event;
    private Salle salle;
    private WebEngine webEngine;
    private String pendingModelUrl;
    private String pendingModelName;
    private boolean showingSnakeGame = true;
    private final Random snakeRandom = new Random();
    private final Deque<Cell> snake = new ArrayDeque<>();
    private final Set<Cell> snakeObstacles = new HashSet<>();
    private Timeline snakeTimeline;
    private Cell snakeFood;
    private Direction snakeDirection = Direction.RIGHT;
    private Direction nextSnakeDirection = Direction.RIGHT;
    private int snakeLevel = 1;
    private int snakeScore = 0;
    private int levelFoodCollected = 0;
    private int levelFoodTarget = 3;
    private boolean snakeRunning;
    private boolean snakeGameOver;
    private boolean snakeRewardUnlocked;
    private boolean snakePausedForViewer;
    private boolean levelFiveRewardShown;
    private boolean levelRewardPopupOpen;
    private double currentSeatSize = 44;

    @FXML
    public void initialize() {
        configureHeaderControls();
        configureNavbar();
        configureSnakeGame();
        configureWebView();
        configureRouletteRewardGame();
        if (reservationButton != null) {
            reservationButton.setDisable(true);
        }
        updateSeatSummary();
        BotpressSupportWidget.installForFrontendPage(heroTitleLabel);
    }

    public void setData(Event event, Salle salle) {
        this.event = event;
        this.salle = salle;

        heroTitleLabel.setText(safe(salle == null ? null : salle.getName()));
        eventLinkButton.setText(safe(event == null ? null : event.getTitle()));
        locationLabel.setText(safe(salle == null ? null : salle.getLocation()));
        durationValueLabel.setText(formatInt(salle == null ? null : salle.getDuration()));
        capacityValueLabel.setText(formatInt(salle == null ? null : salle.getMaxParticipants()));
        priceLabel.setText(buildPriceLabel());

        renderEquipment();
        renderCover();
        renderSeats();
        resetSnakeGame();
        renderModel();
        showSnakeGameView(false);
        playIntroAnimations();
    }

    @FXML
    private void onToggleTheme() {
        if (heroTitleLabel.getScene() != null) {
            ThemeManager.toggleTheme(heroTitleLabel.getScene());
            configureHeaderControls();
            if (showingSnakeGame) {
                drawSnakeGame();
            } else if (pendingModelUrl != null) {
                pushModelToViewer(pendingModelUrl, pendingModelName);
            }
        }
    }

    @FXML
    private void onBackToEvents() {
        onShowEvents();
    }

    @FXML
    private void onShowHome() {
        stopSnakeTimeline();
        AppNavigator.showFrontHome(navigationSource());
    }

    @FXML
    private void onShowCourses() {
        stopSnakeTimeline();
        AppNavigator.showFrontBrowseCourses(navigationSource());
    }

    @FXML
    private void onShowEvents() {
        stopSnakeTimeline();
        AppNavigator.showFrontEvents(navigationSource());
    }

    @FXML
    private void onShowForum() {
        stopSnakeTimeline();
        AppNavigator.showFrontForum(navigationSource());
    }

    @FXML
    private void onShowAssessment() {
        stopSnakeTimeline();
        AppNavigator.showFrontAssessment(navigationSource());
    }

    @FXML
    private void onShowProfile() {
        stopSnakeTimeline();
        AppNavigator.showFrontProfile(navigationSource());
    }

    @FXML
    private void onShowSettings() {
        stopSnakeTimeline();
        AppNavigator.showFrontSettings(navigationSource());
    }

    @FXML
    private void onLogout() {
        stopSnakeTimeline();
        AppNavigator.showLogin(navigationSource());
    }

    @FXML
    private void onStartSnakeGame() {
        showSnakeGameView(false);
        startSnakeGame();
    }

    @FXML
    private void onRestartSnakeGame() {
        showSnakeGameView(false);
        startSnakeGame();
    }

    @FXML
    private void onShow3dViewer() {
        openModelInNativeViewer();
    }

    @FXML
    private void onShowSnakeGame() {
        showSnakeGameView(true);
    }

    @FXML
    private void onContinueSnakeReward() {
        levelRewardPopupOpen = false;
        setRewardVisible(false);
        if (showingSnakeGame && snakeRunning && snakeTimeline == null) {
            scheduleSnakeTimeline();
        }
        if (snakeCanvas != null) {
            snakeCanvas.requestFocus();
        }
    }

    @FXML
    private void onOpenReservationForm() {
        if (selectedSeats.isEmpty()) {
            showError("Reservation unavailable", "Select at least one seat before opening the reservation form.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SiteReservationFormView.fxml"));
            Parent root = loader.load();
            SiteReservationFormController controller = loader.getController();
            controller.setData(event, salle, new ArrayList<>(selectedSeats));

            Scene scene = AppWindow.createScene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/site-events.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) heroTitleLabel.getScene().getWindow();
            stopSnakeTimeline();
            AppWindow.show(stage, scene, "SkillORA Reservation", false);
        } catch (Exception exception) {
            showError("Navigation failed", exception.getMessage());
        }
    }

    @FXML
    private void onOpenVisionTest() {
        Scene scene = heroTitleLabel == null ? null : heroTitleLabel.getScene();
        new VisionTestModal(
                scene == null ? null : scene.getWindow(),
                scene == null ? List.of() : scene.getStylesheets()
        ).show();
    }

    private void configureSnakeGame() {
        if (snakeCanvas == null) {
            return;
        }

        snakeCanvas.setFocusTraversable(true);
        snakeCanvas.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSnakeKeyPress);
        snakeCanvas.setOnMouseClicked(event -> snakeCanvas.requestFocus());
        snakeCanvas.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleSnakeKeyPress);
            }
            if (newScene == null) {
                stopSnakeTimeline();
            } else {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleSnakeKeyPress);
            }
        });

        if (viewerFrame != null) {
            viewerFrame.widthProperty().addListener((obs, oldWidth, newWidth) -> resizeViewerContent());
            viewerFrame.heightProperty().addListener((obs, oldHeight, newHeight) -> resizeViewerContent());
        }

        showSnakeOverlay("Snake Challenge", "Use arrows or ZQSD to move. Complete all 30 levels without losing.", true);
        updateSnakeHud();
        resizeViewerContent();
    }

    private void configureWebView() {
        if (modelWebView == null) {
            return;
        }

        webEngine = modelWebView.getEngine();
        modelWebView.setContextMenuEnabled(false);
        modelWebView.setPageFill(Color.TRANSPARENT);

        URL viewerResource = getClass().getResource("/viewsadmin/event/site-salle-model-viewer.html");
        if (viewerResource == null) {
            showModelFallback("No 3D model available", "The embedded 3D viewer page is missing.");
            return;
        }

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SUCCEEDED -> {
                    if (pendingModelUrl != null) {
                        pushModelToViewer(pendingModelUrl, pendingModelName);
                    }
                }
                case FAILED -> showModelFallback("No 3D model available", "JavaFX WebView failed to load the 3D viewer.");
                default -> {
                }
            }
        });

        webEngine.load(viewerResource.toExternalForm());
    }

    private void configureRouletteRewardGame() {
        if (rouletteWebView == null) {
            return;
        }

        rouletteWebView.setContextMenuEnabled(false);
        rouletteWebView.setPageFill(Color.TRANSPARENT);
        rouletteWebView.getEngine().loadContent(rouletteRewardHtml(), "text/html");
    }

    private void show3dViewer() {
        showingSnakeGame = false;
        snakePausedForViewer = snakeRunning;
        stopSnakeTimeline();
        setNodeVisible(snakeFrame, false);
        setNodeVisible(modelFrame, true);
        setNodeVisible(open3dViewerButton, false);
        setNodeVisible(playSnakeButton, true);
        setNodeVisible(modelCloseButton, true);
        setNodeVisible(snakeStartButton, false);
        setNodeVisible(snakeRestartButton, false);
        setNodeVisible(snakeLevelLabel, false);
        setNodeVisible(snakeScoreLabel, false);

        if (viewerTitleLabel != null) {
            viewerTitleLabel.setText("3D Viewer");
        }
        if (viewerSubtitleLabel != null) {
            viewerSubtitleLabel.setText("Rotate, zoom, and inspect the linked room model.");
        }

        resizeViewerContent();
        renderModel();
    }

    private boolean openModelInNativeViewer() {
        if (salle == null || salle.getImage3d() == null || salle.getImage3d().isBlank()) {
            showModelFallbackInViewer("No 3D model available", "This salle does not have a .glb or .gltf model yet.");
            return false;
        }

        File modelFile = detailService.resolveModelFile(salle.getImage3d());
        if (modelFile == null || !modelFile.exists()) {
            showModelFallbackInViewer("No 3D model available", "Unable to find the linked 3D model file.");
            return false;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showModelFallbackInViewer("3D viewer unavailable", "Windows cannot open the 3D model from this JavaFX session.");
            return false;
        }

        try {
            Desktop.getDesktop().open(modelFile);
            return true;
        } catch (Exception exception) {
            showModelFallbackInViewer("3D viewer unavailable", "Unable to open the model in Windows 3D Viewer: " + exception.getMessage());
            return false;
        }
    }

    private void showModelFallbackInViewer(String title, String message) {
        show3dViewer();
        showModelFallback(title, message);
    }

    private void showSnakeGameView(boolean keepState) {
        showingSnakeGame = true;
        setNodeVisible(modelFrame, false);
        setNodeVisible(snakeFrame, true);
        setNodeVisible(open3dViewerButton, true);
        setNodeVisible(playSnakeButton, false);
        setNodeVisible(modelCloseButton, false);
        setNodeVisible(snakeStartButton, true);
        setNodeVisible(snakeRestartButton, true);
        setNodeVisible(snakeLevelLabel, true);
        setNodeVisible(snakeScoreLabel, true);

        if (viewerTitleLabel != null) {
            viewerTitleLabel.setText("Snake Challenge");
        }
        if (viewerSubtitleLabel != null) {
            viewerSubtitleLabel.setText("Complete 30 levels without losing to unlock the gift QR code.");
        }
        if (viewerFrame != null) {
            viewerFrame.getStyleClass().remove("site-model-frame-ready");
        }
        if (!keepState && !snakeRunning) {
            setSnakeOverlayVisible(!snakeRewardUnlocked);
        }
        if (snakePausedForViewer && snakeRunning && snakeTimeline == null && !levelRewardPopupOpen) {
            scheduleSnakeTimeline();
        }
        snakePausedForViewer = false;
        resizeViewerContent();
        drawSnakeGame();
        snakeCanvas.requestFocus();
    }

    private void setNodeVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void renderEquipment() {
        equipmentFlow.getChildren().clear();
        String equipmentText = safe(salle == null ? null : salle.getEquipment());
        if ("-".equals(equipmentText)) {
            equipmentFlow.getChildren().add(createChip("No equipment"));
            return;
        }

        for (String part : equipmentText.split(",")) {
            String value = part == null ? "" : part.trim();
            if (!value.isEmpty()) {
                equipmentFlow.getChildren().add(createChip(value));
            }
        }

        if (equipmentFlow.getChildren().isEmpty()) {
            equipmentFlow.getChildren().add(createChip(equipmentText));
        }
    }

    private Label createChip(String text) {
        Label chip = new Label(text);
        chip.getStyleClass().add("site-equipment-chip");
        return chip;
    }

    private void renderCover() {
        Image image = loadImage(event == null ? null : event.getImage());
        if (image == null) {
            image = loadImage(salle == null ? null : salle.getImage3d());
        }

        if (image != null) {
            coverImageView.setImage(image);
            coverImageView.setVisible(true);
            coverImageView.setManaged(true);
            coverFallbackPane.setVisible(false);
            coverFallbackPane.setManaged(false);
            Rectangle clip = new Rectangle(coverImageView.getFitWidth(), coverImageView.getFitHeight());
            clip.setArcWidth(36);
            clip.setArcHeight(36);
            coverImageView.setClip(clip);
            ScaleTransition zoom = new ScaleTransition(Duration.millis(420), coverImageView);
            zoom.setFromX(1.04);
            zoom.setFromY(1.04);
            zoom.setToX(1.0);
            zoom.setToY(1.0);
            zoom.play();
        } else {
            coverImageView.setVisible(false);
            coverImageView.setManaged(false);
            coverFallbackPane.setVisible(true);
            coverFallbackPane.setManaged(true);
        }
    }

    private void renderSeats() {
        seatsGrid.getChildren().clear();
        seatsGrid.getColumnConstraints().clear();
        seatsGrid.getRowConstraints().clear();
        selectedSeats.clear();

        int capacity = salle == null || salle.getMaxParticipants() == null ? 0 : Math.max(0, salle.getMaxParticipants());
        List<Reservation> reservations = detailService.loadReservationsForSalle(salle == null ? null : salle.getId());
        reservedSeats = detailService.buildReservedSeatIndexes(salle, reservations);
        SiteSalleDetailService.SeatMatrixSpec spec = detailService.buildSeatMatrix(capacity);
        currentSeatSize = calculateSeatSize(spec.columns());
        seatsGrid.setHgap(calculateSeatGap(spec.columns()));
        seatsGrid.setVgap(calculateSeatGap(spec.columns()));

        for (int col = 0; col < spec.columns(); col++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / spec.columns());
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setHalignment(HPos.CENTER);
            seatsGrid.getColumnConstraints().add(constraints);
        }

        for (int row = 0; row < spec.rows(); row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setVgrow(Priority.NEVER);
            constraints.setValignment(VPos.CENTER);
            seatsGrid.getRowConstraints().add(constraints);
        }

        for (int seatNumber = 1; seatNumber <= spec.capacity(); seatNumber++) {
            int row = (seatNumber - 1) / spec.columns();
            int col = (seatNumber - 1) % spec.columns();
            seatsGrid.add(createSeatCell(seatNumber), col, row);
        }

        updateSeatSummary();
    }

    private StackPane createSeatCell(int seatNumber) {
        StackPane seat = new StackPane();
        seat.getStyleClass().add("site-seat");
        seat.setMinSize(currentSeatSize, currentSeatSize);
        seat.setPrefSize(currentSeatSize, currentSeatSize);
        seat.setMaxSize(currentSeatSize, currentSeatSize);

        Label label = new Label(String.valueOf(seatNumber));
        label.getStyleClass().add("site-seat-label");
        if (currentSeatSize < 36) {
            label.getStyleClass().add("site-seat-label-compact");
        }
        seat.getChildren().add(label);

        if (reservedSeats.contains(seatNumber)) {
            seat.getStyleClass().add("site-seat-reserved");
            return seat;
        }

        seat.getStyleClass().add("site-seat-available");
        seat.setOnMouseClicked(event -> toggleSeatSelection(seatNumber, seat));
        return seat;
    }

    private void toggleSeatSelection(int seatNumber, StackPane seat) {
        if (reservedSeats.contains(seatNumber)) {
            return;
        }

        if (selectedSeats.contains(seatNumber)) {
            selectedSeats.remove(seatNumber);
            seat.getStyleClass().remove("site-seat-selected");
        } else {
            selectedSeats.add(seatNumber);
            if (!seat.getStyleClass().contains("site-seat-selected")) {
                seat.getStyleClass().add("site-seat-selected");
            }
        }

        updateSeatSummary();
    }

    private double calculateSeatSize(int columns) {
        int safeColumns = Math.max(1, columns);
        double gap = calculateSeatGap(safeColumns);
        double availableWidth = 424.0;
        double computed = (availableWidth - ((safeColumns - 1) * gap)) / safeColumns;
        return Math.max(28.0, Math.min(44.0, computed));
    }

    private double calculateSeatGap(int columns) {
        return columns > 9 ? 6.0 : 8.0;
    }

    private void updateSeatSummary() {
        int capacity = salle == null || salle.getMaxParticipants() == null ? 0 : Math.max(0, salle.getMaxParticipants());
        int reservedCount = reservedSeats.size();
        int selectedCount = selectedSeats.size();
        int availableCount = Math.max(0, capacity - reservedCount);

        if (reservedCountLabel != null) {
            reservedCountLabel.setText(String.valueOf(reservedCount));
        }
        if (availableCountLabel != null) {
            availableCountLabel.setText(String.valueOf(availableCount));
        }
        if (selectedCountLabel != null) {
            selectedCountLabel.setText(String.valueOf(selectedCount));
        }
        if (seatsSummaryLabel != null) {
            seatsSummaryLabel.setText("Capacity matrix " + capacity + " seats" + (selectedCount > 0 ? " • Selected " + selectedCount : ""));
        }
        if (reservationButton != null) {
            reservationButton.setDisable(selectedCount == 0);
        }
    }

    private void renderModel() {
        pendingModelUrl = null;
        pendingModelName = null;

        if (salle == null || salle.getImage3d() == null || salle.getImage3d().isBlank()) {
            showModelFallback("No 3D model available", "This salle does not have a .glb or .gltf model yet.");
            return;
        }

        try {
            pendingModelUrl = detailService.resolveModelViewerUrl(salle.getImage3d());
            pendingModelName = new File(salle.getImage3d()).getName();

            if (pendingModelUrl == null) {
                showModelFallback("No 3D model available", "Unable to resolve the salle model path.");
                return;
            }

            showModelViewer();
            pushModelToViewer(pendingModelUrl, pendingModelName);
        } catch (Exception exception) {
            showModelFallback("No 3D model available", exception.getMessage());
        }
    }

    private void pushModelToViewer(String modelUrl, String fileName) {
        if (webEngine == null || modelUrl == null) {
            return;
        }

        try {
            webEngine.executeScript(
                    "window.setModelSource(" + quote(modelUrl) + ", " + quote(fileName == null ? "" : fileName) + ");"
            );
            showModelViewer();
        } catch (Exception exception) {
            showModelFallback("No 3D model available", "JavaFX could not render the 3D model in the embedded viewer.");
        }
    }

    private void showModelViewer() {
        modelWebView.setVisible(true);
        modelWebView.setManaged(true);
        modelPlaceholderBox.setVisible(false);
        modelPlaceholderBox.setManaged(false);
        if (!viewerFrame.getStyleClass().contains("site-model-frame-ready")) {
            viewerFrame.getStyleClass().add("site-model-frame-ready");
        }
    }

    private void showModelFallback(String title, String subtitle) {
        modelWebView.setVisible(false);
        modelWebView.setManaged(false);
        modelPlaceholderBox.setVisible(true);
        modelPlaceholderBox.setManaged(true);
        modelStatusTitle.setText(title);
        modelStatusSubtitle.setText(subtitle == null || subtitle.isBlank() ? "No preview available." : subtitle);
        if (viewerFrame != null) {
            viewerFrame.getStyleClass().remove("site-model-frame-ready");
        }
        if (webEngine != null) {
            try {
                webEngine.executeScript("window.showEmpty(" + quote(modelStatusSubtitle.getText()) + ");");
            } catch (Exception ignored) {
            }
        }
    }

    private void resetSnakeGame() {
        stopSnakeTimeline();
        snakePausedForViewer = false;
        snakeLevel = 1;
        snakeScore = 0;
        levelFoodCollected = 0;
        levelFoodTarget = targetForLevel(snakeLevel);
        snakeRunning = false;
        snakeGameOver = false;
        snakeRewardUnlocked = false;
        levelFiveRewardShown = false;
        levelRewardPopupOpen = false;
        buildSnakeLevel();
        setRewardVisible(false);
        showSnakeOverlay("Snake Challenge", "Use arrows or ZQSD to move. Complete all 30 levels without losing.", true);
        updateSnakeHud();
        drawSnakeGame();
    }

    private void startSnakeGame() {
        stopSnakeTimeline();
        snakePausedForViewer = false;
        snakeLevel = 1;
        snakeScore = 0;
        snakeRewardUnlocked = false;
        snakeGameOver = false;
        levelFiveRewardShown = false;
        levelRewardPopupOpen = false;
        startSnakeLevel();
    }

    private void startSnakeLevel() {
        levelFoodCollected = 0;
        levelFoodTarget = targetForLevel(snakeLevel);
        snakeRunning = true;
        snakeGameOver = false;
        levelRewardPopupOpen = false;
        setRewardVisible(false);
        setSnakeOverlayVisible(false);
        buildSnakeLevel();
        updateSnakeHud();
        drawSnakeGame();
        scheduleSnakeTimeline();
        showLevelFiveRewardIfNeeded();
        snakeCanvas.requestFocus();
    }

    private void scheduleSnakeTimeline() {
        stopSnakeTimeline();
        snakeTimeline = new Timeline(new KeyFrame(Duration.millis(speedForLevel(snakeLevel)), event -> tickSnake()));
        snakeTimeline.setCycleCount(Timeline.INDEFINITE);
        snakeTimeline.play();
    }

    private void stopSnakeTimeline() {
        if (snakeTimeline != null) {
            snakeTimeline.stop();
            snakeTimeline = null;
        }
    }

    private void buildSnakeLevel() {
        snake.clear();
        snakeObstacles.clear();
        snakeDirection = Direction.RIGHT;
        nextSnakeDirection = Direction.RIGHT;

        int startCol = SNAKE_COLUMNS / 2;
        int startRow = SNAKE_ROWS / 2;
        snake.addFirst(new Cell(startCol, startRow));
        snake.addLast(new Cell(startCol - 1, startRow));
        snake.addLast(new Cell(startCol - 2, startRow));

        placeObstacles();
        placeFood();
    }

    private void placeObstacles() {
        int obstacleCount = obstacleCountForLevel(snakeLevel);
        int guardCol = SNAKE_COLUMNS / 2;
        int guardRow = SNAKE_ROWS / 2;

        while (snakeObstacles.size() < obstacleCount) {
            Cell candidate = new Cell(snakeRandom.nextInt(SNAKE_COLUMNS), snakeRandom.nextInt(SNAKE_ROWS));
            if (snake.contains(candidate) || snakeObstacles.contains(candidate)) {
                continue;
            }
            if (Math.abs(candidate.col() - guardCol) <= 4 && Math.abs(candidate.row() - guardRow) <= 3) {
                continue;
            }
            snakeObstacles.add(candidate);
        }
    }

    private void placeFood() {
        int attempts = 0;
        do {
            snakeFood = new Cell(snakeRandom.nextInt(SNAKE_COLUMNS), snakeRandom.nextInt(SNAKE_ROWS));
            attempts++;
        } while ((snake.contains(snakeFood) || snakeObstacles.contains(snakeFood)) && attempts < 800);
    }

    private void tickSnake() {
        if (!snakeRunning || snake.isEmpty()) {
            return;
        }

        snakeDirection = nextSnakeDirection;
        Cell head = snake.peekFirst();
        Cell nextHead = new Cell(head.col() + snakeDirection.deltaCol, head.row() + snakeDirection.deltaRow);
        boolean eatsFood = nextHead.equals(snakeFood);

        if (isWallCollision(nextHead) || snakeObstacles.contains(nextHead)) {
            showSnakeGameOver();
            return;
        }

        Cell tail = snake.peekLast();
        boolean hitsSelf = snake.contains(nextHead) && (eatsFood || !nextHead.equals(tail));
        if (hitsSelf) {
            showSnakeGameOver();
            return;
        }

        snake.addFirst(nextHead);
        if (eatsFood) {
            snakeScore += 10 + snakeLevel;
            levelFoodCollected++;
            if (levelFoodCollected >= levelFoodTarget) {
                completeSnakeLevel();
                return;
            }
            placeFood();
        } else {
            snake.removeLast();
        }

        updateSnakeHud();
        drawSnakeGame();
    }

    private void completeSnakeLevel() {
        stopSnakeTimeline();
        if (snakeLevel >= MAX_SNAKE_LEVEL) {
            unlockSnakeReward();
            return;
        }

        snakeLevel++;
        startSnakeLevel();
    }

    private void showLevelFiveRewardIfNeeded() {
        if (snakeLevel != 5 || levelFiveRewardShown) {
            return;
        }

        levelFiveRewardShown = true;
        levelRewardPopupOpen = true;
        stopSnakeTimeline();

        if (rewardQrImageView != null) {
            try {
                rewardQrImageView.setImage(QrGen.make(REWARD_QR_TEXT, 220));
            } catch (Exception exception) {
                rewardQrImageView.setImage(null);
            }
        }

        setSnakeOverlayVisible(false);
        setRewardVisible(true);
    }

    private void showSnakeGameOver() {
        snakeRunning = false;
        snakeGameOver = true;
        stopSnakeTimeline();
        showSnakeOverlay("Game Over", "You hit a wall, obstacle, or yourself. Restart begins again at level 1.", false);
        updateSnakeHud();
        drawSnakeGame();
    }

    private void unlockSnakeReward() {
        snakeRunning = false;
        snakeRewardUnlocked = true;
        levelRewardPopupOpen = false;
        updateSnakeHud();
        drawSnakeGame();
        setRewardVisible(false);
        showSnakeOverlay("Challenge Complete", "You completed all 30 levels. Your reward was unlocked at level 5.", false);
    }

    private void handleSnakeKeyPress(KeyEvent event) {
        if (!showingSnakeGame || levelRewardPopupOpen) {
            return;
        }

        Direction requested = switch (event.getCode()) {
            case UP, Z -> Direction.UP;
            case DOWN, S -> Direction.DOWN;
            case LEFT, Q -> Direction.LEFT;
            case RIGHT, D -> Direction.RIGHT;
            default -> null;
        };

        if (requested == null) {
            return;
        }

        if (snakeRunning && !requested.isOpposite(snakeDirection) && !requested.isOpposite(nextSnakeDirection)) {
            nextSnakeDirection = requested;
        }
        event.consume();
    }

    private boolean isWallCollision(Cell cell) {
        return cell.col() < 0 || cell.col() >= SNAKE_COLUMNS || cell.row() < 0 || cell.row() >= SNAKE_ROWS;
    }

    private int targetForLevel(int level) {
        return Math.min(12, 3 + ((level - 1) / 3));
    }

    private int obstacleCountForLevel(int level) {
        return Math.min(44, Math.max(0, level - 1));
    }

    private double speedForLevel(int level) {
        return Math.max(52, 170 - ((level - 1) * 4.0));
    }

    private void resizeViewerContent() {
        if (viewerFrame == null) {
            return;
        }

        double availableWidth = Math.max(320, viewerFrame.getWidth() - 32);
        double availableHeight = Math.max(260, viewerFrame.getHeight() - 32);
        double boardRatio = (double) SNAKE_COLUMNS / SNAKE_ROWS;
        double boardWidth = availableWidth;
        double boardHeight = boardWidth / boardRatio;

        if (boardHeight > availableHeight) {
            boardHeight = availableHeight;
            boardWidth = boardHeight * boardRatio;
        }

        if (snakeCanvas != null) {
            snakeCanvas.setWidth(boardWidth);
            snakeCanvas.setHeight(boardHeight);
        }
        if (modelWebView != null) {
            modelWebView.setPrefSize(availableWidth, availableHeight);
            modelWebView.setMinSize(availableWidth, availableHeight);
        }
        if (modelPlaceholderBox != null) {
            modelPlaceholderBox.setPrefSize(availableWidth, availableHeight);
        }
        drawSnakeGame();
    }

    private void drawSnakeGame() {
        if (snakeCanvas == null) {
            return;
        }

        GraphicsContext gc = snakeCanvas.getGraphicsContext2D();
        double width = Math.max(1, snakeCanvas.getWidth());
        double height = Math.max(1, snakeCanvas.getHeight());
        double cellWidth = width / SNAKE_COLUMNS;
        double cellHeight = height / SNAKE_ROWS;

        gc.setFill(Color.web("#0f172a"));
        gc.fillRoundRect(0, 0, width, height, 18, 18);

        gc.setStroke(Color.web("#1e2f4a"));
        gc.setLineWidth(1);
        for (int col = 1; col < SNAKE_COLUMNS; col++) {
            double x = col * cellWidth;
            gc.strokeLine(x, 0, x, height);
        }
        for (int row = 1; row < SNAKE_ROWS; row++) {
            double y = row * cellHeight;
            gc.strokeLine(0, y, width, y);
        }

        for (Cell obstacle : snakeObstacles) {
            drawCell(gc, obstacle, cellWidth, cellHeight, Color.web("#475569"), 0.72);
        }

        if (snakeFood != null) {
            drawFood(gc, snakeFood, cellWidth, cellHeight);
        }

        int index = 0;
        for (Cell part : snake) {
            Color fill = index == 0 ? Color.web("#10b981") : Color.web("#2f64e0");
            drawCell(gc, part, cellWidth, cellHeight, fill, 0.82);
            index++;
        }

        if (!snakeRunning && !snakeRewardUnlocked) {
            gc.setFill(Color.rgb(15, 23, 42, 0.36));
            gc.fillRoundRect(0, 0, width, height, 18, 18);
        }
    }

    private void drawCell(GraphicsContext gc, Cell cell, double cellWidth, double cellHeight, Color fill, double insetFactor) {
        double insetX = Math.max(2.0, cellWidth * (1.0 - insetFactor) / 2.0);
        double insetY = Math.max(2.0, cellHeight * (1.0 - insetFactor) / 2.0);
        double x = (cell.col() * cellWidth) + insetX;
        double y = (cell.row() * cellHeight) + insetY;
        double width = Math.max(4.0, cellWidth - (insetX * 2.0));
        double height = Math.max(4.0, cellHeight - (insetY * 2.0));

        gc.setFill(fill);
        gc.fillRoundRect(x, y, width, height, 8, 8);
    }

    private void drawFood(GraphicsContext gc, Cell cell, double cellWidth, double cellHeight) {
        double size = Math.min(cellWidth, cellHeight) * 0.58;
        double x = (cell.col() * cellWidth) + ((cellWidth - size) / 2.0);
        double y = (cell.row() * cellHeight) + ((cellHeight - size) / 2.0);

        gc.setFill(Color.web("#fbbf24"));
        gc.fillOval(x, y, size, size);
        gc.setStroke(Color.web("#fef3c7"));
        gc.setLineWidth(2);
        gc.strokeOval(x + 1, y + 1, Math.max(2, size - 2), Math.max(2, size - 2));
    }

    private void updateSnakeHud() {
        if (snakeLevelLabel != null) {
            snakeLevelLabel.setText("Level: " + snakeLevel + " / " + MAX_SNAKE_LEVEL);
        }
        if (snakeScoreLabel != null) {
            snakeScoreLabel.setText("Score: " + snakeScore + " | Food: " + levelFoodCollected + " / " + levelFoodTarget);
        }
        if (snakeRestartButton != null) {
            snakeRestartButton.setDisable(!snakeRunning && !snakeGameOver && !snakeRewardUnlocked);
        }
        if (snakeOverlayRestartButton != null) {
            snakeOverlayRestartButton.setDisable(!snakeGameOver && !snakeRewardUnlocked);
        }
    }

    private void showSnakeOverlay(String title, String message, boolean startMode) {
        if (snakeOverlayTitle != null) {
            snakeOverlayTitle.setText(title);
        }
        if (snakeOverlayMessage != null) {
            snakeOverlayMessage.setText(message);
        }
        if (snakeOverlayStartButton != null) {
            snakeOverlayStartButton.setVisible(startMode);
            snakeOverlayStartButton.setManaged(startMode);
        }
        if (snakeOverlayRestartButton != null) {
            snakeOverlayRestartButton.setVisible(!startMode);
            snakeOverlayRestartButton.setManaged(!startMode);
        }
        setRewardVisible(false);
        setSnakeOverlayVisible(true);
    }

    private void setSnakeOverlayVisible(boolean visible) {
        if (snakeOverlay != null) {
            snakeOverlay.setVisible(visible);
            snakeOverlay.setManaged(visible);
        }
    }

    private void setRewardVisible(boolean visible) {
        if (snakeRewardPane != null) {
            snakeRewardPane.setVisible(visible);
            snakeRewardPane.setManaged(visible);
        }
    }

    private void playIntroAnimations() {
        animateNode(heroTitleLabel, 0);
        animateNode(locationLabel, 1);
        animateNode(durationValueLabel, 2);
        animateNode(capacityValueLabel, 3);
        animateNode(priceLabel, 4);
        animateNode(coverImageView.isVisible() ? coverImageView : coverFallbackPane, 1);
        animateNode(snakeFrame, 2);
        animateNode(seatsGrid, 2);
    }

    private void animateNode(javafx.scene.Node node, int delayIndex) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(10);

        Duration duration = Duration.millis(280);
        Duration delay = Duration.millis(Math.min(delayIndex * 60L, 300));

        FadeTransition fade = new FadeTransition(duration, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(delay);

        TranslateTransition slide = new TranslateTransition(duration, node);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setDelay(delay);

        fade.play();
        slide.play();
    }

    private String buildPriceLabel() {
        if (event == null) {
            return "-";
        }

        String raw = safe(event.getPriceType());
        LocalDateTime start = event.getStartDate();
        String datePart = start == null ? "" : " • " + DATE_FORMATTER.format(start);
        return raw + datePart;
    }

    private Image loadImage(String source) {
        if (source == null || source.isBlank() || "-".equals(source)) {
            return null;
        }
        try {
            if (source.startsWith("http://") || source.startsWith("https://") || source.startsWith("file:")) {
                return new Image(source, true);
            }
            File file = new File(source);
            if (file.exists()) {
                return new Image(file.toURI().toString(), true);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String formatInt(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String rouletteRewardHtml() {
        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        * { box-sizing: border-box; }
                        html, body {
                            width: 100%;
                            min-height: 100%;
                            margin: 0;
                            font-family: Inter, Segoe UI, Arial, sans-serif;
                            background: transparent;
                            color: #15233f;
                            overflow: visible;
                        }
                        .game {
                            position: relative;
                            display: grid;
                            grid-template-columns: minmax(360px, 1fr) minmax(260px, 0.86fr);
                            align-items: center;
                            gap: 26px;
                            width: 100%;
                            min-height: 470px;
                            padding: 24px 18px 34px 12px;
                            overflow: visible;
                        }
                        .wheel-wrap {
                            display: flex;
                            flex-direction: column;
                            align-items: center;
                            justify-content: center;
                            min-width: 0;
                            min-height: 420px;
                            padding: 20px 0 30px;
                            overflow: visible;
                        }
                        .pointer {
                            width: 0;
                            height: 0;
                            border-left: 19px solid transparent;
                            border-right: 19px solid transparent;
                            border-top: 34px solid #f59e0b;
                            filter: drop-shadow(0 7px 10px rgba(15, 23, 42, 0.32));
                            margin-bottom: -14px;
                            z-index: 8;
                            position: relative;
                        }
                        .pointer::after {
                            content: "";
                            position: absolute;
                            left: -8px;
                            top: -31px;
                            width: 16px;
                            height: 16px;
                            border-radius: 50%;
                            background: #fff7ed;
                            box-shadow: inset 0 0 0 3px rgba(245, 158, 11, 0.55);
                        }
                        .wheel {
                            position: relative;
                            width: min(365px, calc(100vw - 42px));
                            aspect-ratio: 1;
                            border-radius: 50%;
                            border: 12px solid #0f172a;
                            background:
                                radial-gradient(circle at center, #ffffff 0 11%, #dbeafe 12% 20%, transparent 21%),
                                repeating-conic-gradient(from -90deg, #dc2626 0 9.73deg, #111827 9.73deg 19.46deg, #047857 19.46deg 29.19deg);
                            box-shadow:
                                0 24px 52px rgba(15, 23, 42, 0.25),
                                inset 0 0 0 4px rgba(255,255,255,0.5),
                                inset 0 0 0 18px rgba(255,255,255,0.08);
                            transition: transform 3.15s cubic-bezier(.11,.76,.12,1);
                            flex: 0 0 auto;
                        }
                        .hub {
                            position: absolute;
                            inset: 35%;
                            display: grid;
                            place-items: center;
                            border-radius: 50%;
                            background: linear-gradient(135deg, #ffffff, #dbeafe 52%, #ccfbf1);
                            border: 5px solid rgba(255,255,255,0.9);
                            color: #0f172a;
                            font-weight: 900;
                            font-size: 23px;
                            letter-spacing: 0;
                            box-shadow:
                                inset 0 0 0 1px rgba(15,23,42,0.08),
                                0 10px 24px rgba(15, 23, 42, 0.18);
                        }
                        .hub.rewarded {
                            color: #0f766e;
                            font-size: 17px;
                        }
                        .number {
                            position: absolute;
                            top: 50%;
                            left: 50%;
                            width: 30px;
                            height: 30px;
                            margin: -15px;
                            display: grid;
                            place-items: center;
                            border-radius: 50%;
                            background: rgba(255,255,255,0.24);
                            border: 1px solid rgba(255,255,255,0.42);
                            color: #ffffff;
                            font-size: 13px;
                            font-weight: 950;
                            text-shadow: 0 1px 4px rgba(0,0,0,0.85);
                            box-shadow: 0 1px 5px rgba(15, 23, 42, 0.18);
                        }
                        .panel {
                            min-width: 0;
                            display: flex;
                            flex-direction: column;
                            justify-content: center;
                            gap: 14px;
                            padding: 8px 8px 8px 0;
                        }
                        h3 { margin: 0; color: #0f172a; font-size: 24px; font-weight: 900; }
                        p { margin: 0; color: #64748b; line-height: 1.45; font-size: 14px; font-weight: 700; }
                        label { color: #334155; font-size: 13px; font-weight: 900; }
                        .number-input {
                            width: min(230px, 100%);
                            border: 1px solid rgba(47, 100, 224, 0.22);
                            border-radius: 16px;
                            padding: 12px 14px;
                            background: rgba(255, 255, 255, 0.72);
                            color: #0f172a;
                            font: 900 16px Inter, Segoe UI, Arial, sans-serif;
                            outline: none;
                            box-shadow: inset 0 0 0 1px rgba(255,255,255,0.65);
                        }
                        .number-input:focus {
                            border-color: #2f64e0;
                            box-shadow: 0 0 0 4px rgba(47, 100, 224, 0.12);
                        }
                        .status-row {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 8px;
                            align-items: center;
                        }
                        .pill {
                            width: fit-content;
                            padding: 9px 13px;
                            border-radius: 999px;
                            background: rgba(47, 100, 224, 0.12);
                            color: #1d4ed8;
                            font-size: 13px;
                            font-weight: 900;
                        }
                        .pill.result {
                            background: rgba(16, 185, 129, 0.14);
                            color: #047857;
                        }
                        .attempts {
                            width: fit-content;
                            color: #475569;
                            font-size: 13px;
                            font-weight: 900;
                        }
                        button {
                            width: fit-content;
                            min-width: 170px;
                            position: relative;
                            z-index: 3;
                            border: 0;
                            border-radius: 999px;
                            padding: 13px 20px;
                            color: white;
                            background: linear-gradient(90deg, #2f64e0, #10b981);
                            box-shadow: 0 14px 26px rgba(47,100,224,0.25);
                            font-size: 14px;
                            font-weight: 900;
                            cursor: pointer;
                            pointer-events: auto;
                        }
                        button:disabled {
                            cursor: default;
                            background: #94a3b8;
                            box-shadow: none;
                            opacity: 0.8;
                        }
                        .note { color: #94a3b8; font-size: 12px; font-weight: 800; }
                        .message {
                            min-height: 20px;
                            color: #dc2626;
                            font-size: 13px;
                            font-weight: 900;
                        }
                        .message.ok { color: #047857; }
                        .modal {
                            position: fixed;
                            inset: 0;
                            display: none;
                            align-items: center;
                            justify-content: center;
                            padding: 18px;
                            background: rgba(15, 23, 42, 0.42);
                            z-index: 20;
                        }
                        .modal.open { display: flex; }
                        .dialog {
                            width: min(360px, 92vw);
                            border-radius: 22px;
                            padding: 22px;
                            background:
                                linear-gradient(180deg, rgba(255,255,255,0.99), rgba(239, 246, 255, 0.98));
                            border: 1px solid rgba(220,229,242,0.9);
                            box-shadow: 0 28px 70px rgba(15, 23, 42, 0.32);
                            text-align: center;
                        }
                        .dialog h4 { margin: 0 0 8px; color: #0f172a; font-size: 25px; font-weight: 950; }
                        .dialog p { margin-bottom: 14px; }
                        .brand-reward {
                            display: none;
                            margin: 4px auto 10px;
                            width: fit-content;
                            padding: 8px 14px;
                            border-radius: 999px;
                            color: #0f766e;
                            background: rgba(20, 184, 166, 0.13);
                            border: 1px solid rgba(20, 184, 166, 0.24);
                            font-size: 18px;
                            font-weight: 950;
                        }
                        .brand-reward.visible { display: block; }
                        .qr {
                            display: none;
                            width: 178px;
                            height: 178px;
                            margin: 8px auto 14px;
                            padding: 8px;
                            border-radius: 16px;
                            background: white;
                            box-shadow: 0 12px 26px rgba(15, 23, 42, 0.16);
                        }
                        .qr.visible { display: block; }
                        .close { min-width: 120px; padding: 10px 16px; background: #0f172a; box-shadow: none; }
                        @media (max-width: 640px) {
                            .game {
                                grid-template-columns: 1fr;
                                min-height: 720px;
                                padding: 22px 14px 34px;
                                overflow: visible;
                            }
                            .wheel-wrap {
                                min-height: 390px;
                                padding-bottom: 24px;
                            }
                            .wheel { width: min(335px, calc(100vw - 40px)); }
                            .panel { align-items: center; text-align: center; padding: 0; }
                        }
                    </style>
                </head>
                <body>
                    <div class="game">
                        <div class="wheel-wrap">
                            <div class="pointer"></div>
                            <div id="wheel" class="wheel">
                                <div id="hub" class="hub">Spin</div>
                            </div>
                        </div>
                        <div class="panel">
                            <h3>Roulette Reward</h3>
                            <p>Choose your roulette number first, then spin for a SkillORA reward.</p>
                            <label for="chosenNumber">Choose a number (0-36)</label>
                            <input id="chosenNumber" class="number-input" type="number" min="0" max="36" step="1" inputmode="numeric" placeholder="0-36">
                            <div class="status-row">
                                <div id="selected" class="pill">Selected number: -</div>
                                <div id="result" class="pill result">Result number: -</div>
                            </div>
                            <div id="attempts" class="attempts">Attempts: 0 / 5</div>
                            <button id="spinButton" type="button">Spin Roulette</button>
                            <div id="message" class="message"></div>
                            <div id="note" class="note">No real money, no betting, just a SkillORA reward mini-game.</div>
                        </div>
                    </div>

                    <div id="modal" class="modal" role="dialog" aria-modal="true">
                        <div class="dialog">
                            <h4 id="modalTitle">No reward this time.</h4>
                            <div id="brandReward" class="brand-reward">SKILLORA</div>
                            <p id="modalText">Try again next time.</p>
                            <img id="qr" class="qr" alt="SkillORA reward QR code">
                            <button id="closeModal" class="close" type="button">Close</button>
                        </div>
                    </div>

                    <script>
                        var ATTEMPTS_KEY = 'skillora_roulette_reward_attempts';
                        var WON_KEY = 'skillora_roulette_reward_won';
                        var MAX_ATTEMPTS = 5;
                        var reward = { text: 'enti aandek blassa VIP aande SkillORA', qr: '__QR_TWO__' };
                        var wheel = document.getElementById('wheel');
                        var hub = document.getElementById('hub');
                        var spinButton = document.getElementById('spinButton');
                        var chosenNumber = document.getElementById('chosenNumber');
                        var selected = document.getElementById('selected');
                        var result = document.getElementById('result');
                        var attempts = document.getElementById('attempts');
                        var message = document.getElementById('message');
                        var modal = document.getElementById('modal');
                        var modalTitle = document.getElementById('modalTitle');
                        var brandReward = document.getElementById('brandReward');
                        var modalText = document.getElementById('modalText');
                        var qr = document.getElementById('qr');
                        var closeModal = document.getElementById('closeModal');
                        var numbers = [];
                        var currentRotation = 0;
                        var memoryAttempts = 0;
                        var spinning = false;

                        for (var i = 0; i <= 36; i += 1) {
                            numbers.push(i);
                        }

                        for (var index = 0; index < numbers.length; index += 1) {
                            var number = numbers[index];
                            var node = document.createElement('span');
                            var angle = index * (360 / numbers.length);
                            node.className = 'number';
                            node.textContent = String(number);
                            node.style.transform = 'rotate(' + angle + 'deg) translateY(-146px) rotate(' + (-angle) + 'deg)';
                            wheel.appendChild(node);
                        }

                        updateAttempts();

                        if (spinButton.addEventListener) {
                            spinButton.addEventListener('click', handleSpin, false);
                        }
                        spinButton.onclick = handleSpin;
                        closeModal.onclick = function () {
                            modal.className = 'modal';
                        };

                        function handleSpin(event) {
                            if (event && event.preventDefault) {
                                event.preventDefault();
                            }
                            if (spinning) {
                                return false;
                            }
                            if (hasWon()) {
                                disableGame('Reward Unlocked!');
                                addClass(message, 'ok');
                                return false;
                            }
                            var currentAttempts = readAttempts();
                            if (currentAttempts >= MAX_ATTEMPTS) {
                                disableGame('No attempts left');
                                return false;
                            }
                            var picked = parseChoice();
                            if (picked === null) {
                                return false;
                            }

                            var nextAttempts = currentAttempts + 1;
                            var finalNumber = nextAttempts === 4 ? picked : randomLosingNumber(picked);
                            var resultIndex = finalNumber;
                            var sector = 360 / numbers.length;
                            var target = 360 - (resultIndex * sector);
                            var baseRotation = currentRotation + 1440;
                            currentRotation = baseRotation + ((target - (baseRotation % 360) + 360) % 360);

                            spinning = true;
                            spinButton.disabled = true;
                            chosenNumber.disabled = true;
                            message.textContent = '';
                            removeClass(message, 'ok');
                            selected.textContent = 'Selected number: ' + picked;
                            result.textContent = 'Result number: spinning...';
                            hub.textContent = '...';
                            removeClass(hub, 'rewarded');
                            wheel.style.transform = 'rotate(' + currentRotation + 'deg)';

                            window.setTimeout(function () {
                                writeAttempts(nextAttempts);
                                result.textContent = 'Result number: ' + finalNumber;
                                hub.textContent = String(finalNumber);
                                updateAttempts();
                                var won = showResult(picked, finalNumber);
                                spinning = false;
                                if (won) {
                                    writeWon();
                                    disableGame('Reward Unlocked!');
                                    addClass(message, 'ok');
                                } else if (nextAttempts < MAX_ATTEMPTS) {
                                    spinButton.disabled = false;
                                    chosenNumber.disabled = false;
                                } else {
                                    disableGame('No attempts left');
                                }
                            }, 3100);
                            return false;
                        }

                        function randomLosingNumber(picked) {
                            var value = Math.floor(Math.random() * 37);
                            if (value === picked) {
                                value = (value + 1 + Math.floor(Math.random() * 36)) % 37;
                            }
                            return value;
                        }

                        function parseChoice() {
                            var raw = String(chosenNumber.value).replace(/^\\s+|\\s+$/g, '');
                            var value = Number(raw);
                            if (raw === '' || isNaN(value) || Math.floor(value) !== value || value < 0 || value > 36) {
                                message.textContent = 'Choose a valid number from 0 to 36.';
                                removeClass(message, 'ok');
                                selected.textContent = 'Selected number: -';
                                result.textContent = 'Result number: -';
                                return null;
                            }
                            return value;
                        }

                        function showResult(picked, finalNumber) {
                            if (finalNumber === picked) {
                                modalTitle.textContent = 'Reward Unlocked!';
                                modalText.textContent = reward.text;
                                hub.textContent = 'SKILLORA';
                                addClass(hub, 'rewarded');
                                addClass(brandReward, 'visible');
                                qr.src = reward.qr;
                                addClass(qr, 'visible');
                                message.textContent = 'Reward Unlocked!';
                                addClass(message, 'ok');
                                addClass(modal, 'open');
                                return true;
                            } else {
                                modalTitle.textContent = 'Try again';
                                modalText.textContent = 'Try again';
                                removeClass(brandReward, 'visible');
                                qr.removeAttribute('src');
                                removeClass(qr, 'visible');
                                message.textContent = 'Try again';
                                removeClass(message, 'ok');
                            }
                            addClass(modal, 'open');
                            return false;
                        }

                        function readAttempts() {
                            var value = Number(readStoredAttempts() || '0');
                            return isFinite(value) ? Math.min(Math.max(Math.floor(value), 0), MAX_ATTEMPTS) : 0;
                        }

                        function readStoredAttempts() {
                            try {
                                return localStorage.getItem(ATTEMPTS_KEY);
                            } catch (error) {
                                try {
                                    return sessionStorage.getItem(ATTEMPTS_KEY);
                                } catch (fallbackError) {
                                    return String(memoryAttempts);
                                }
                            }
                        }

                        function writeAttempts(value) {
                            try {
                                localStorage.setItem(ATTEMPTS_KEY, String(value));
                            } catch (error) {
                                try {
                                    sessionStorage.setItem(ATTEMPTS_KEY, String(value));
                                } catch (fallbackError) {
                                    memoryAttempts = value;
                                }
                            }
                        }

                        function updateAttempts() {
                            var used = readAttempts();
                            attempts.textContent = 'Attempts: ' + used + ' / ' + MAX_ATTEMPTS;
                            if (hasWon()) {
                                disableGame('Reward Unlocked!');
                                addClass(message, 'ok');
                            } else if (used >= MAX_ATTEMPTS) {
                                disableGame('No attempts left');
                            }
                        }

                        function disableGame(text) {
                            spinButton.disabled = true;
                            chosenNumber.disabled = true;
                            message.textContent = text || 'No attempts left';
                            removeClass(message, 'ok');
                        }

                        function hasWon() {
                            return readStoredValue(WON_KEY) === 'true';
                        }

                        function writeWon() {
                            writeStoredValue(WON_KEY, 'true');
                        }

                        function readStoredValue(key) {
                            try {
                                return localStorage.getItem(key);
                            } catch (error) {
                                try {
                                    return sessionStorage.getItem(key);
                                } catch (fallbackError) {
                                    return null;
                                }
                            }
                        }

                        function writeStoredValue(key, value) {
                            try {
                                localStorage.setItem(key, value);
                            } catch (error) {
                                try {
                                    sessionStorage.setItem(key, value);
                                } catch (fallbackError) {
                                }
                            }
                        }

                        function addClass(element, className) {
                            if (!element) {
                                return;
                            }
                            if (element.classList) {
                                element.classList.add(className);
                                return;
                            }
                            if ((' ' + element.className + ' ').indexOf(' ' + className + ' ') === -1) {
                                element.className = element.className + ' ' + className;
                            }
                        }

                        function removeClass(element, className) {
                            if (!element) {
                                return;
                            }
                            if (element.classList) {
                                element.classList.remove(className);
                                return;
                            }
                            element.className = (' ' + element.className + ' ').replace(' ' + className + ' ', ' ').replace(/^\\s+|\\s+$/g, '');
                        }
                    </script>
                </body>
                </html>
                """
                .replace("__QR_TWO__", rewardQrDataUrl("enti aandek blassa VIP aande SkillORA"));
    }

    private String rewardQrDataUrl(String text) {
        try {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(QrGen.makePngBytes(text, 220));
        } catch (Exception exception) {
            return "";
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

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message == null || message.isBlank() ? "Unknown error" : message);
        alert.showAndWait();
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
        }
    }

    private javafx.scene.Node navigationSource() {
        return themeToggleButton != null ? themeToggleButton : heroTitleLabel;
    }

    private record Cell(int col, int row) {
    }

    private enum Direction {
        UP(0, -1),
        DOWN(0, 1),
        LEFT(-1, 0),
        RIGHT(1, 0);

        private final int deltaCol;
        private final int deltaRow;

        Direction(int deltaCol, int deltaRow) {
            this.deltaCol = deltaCol;
            this.deltaRow = deltaRow;
        }

        private boolean isOpposite(Direction other) {
            return other != null && deltaCol + other.deltaCol == 0 && deltaRow + other.deltaRow == 0;
        }
    }
}
