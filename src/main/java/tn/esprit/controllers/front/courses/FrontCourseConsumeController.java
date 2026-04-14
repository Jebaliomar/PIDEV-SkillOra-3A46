package tn.esprit.controllers.front.courses;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Lesson;
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.LessonService;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrontCourseConsumeController implements FrontShellAware {

    private static final double VIDEO_PREVIEW_HEIGHT = 360;
    private static final double PDF_PREVIEW_HEIGHT = 560;

    @FXML
    private Button backButton;

    @FXML
    private Label courseTitleLabel;

    @FXML
    private Label lessonTitleLabel;

    @FXML
    private Label lessonTypeLabel;

    @FXML
    private Accordion sectionsAccordion;

    @FXML
    private StackPane contentContainer;

    @FXML
    private Label previewStatusLabel;

    @FXML
    private HBox previewActionsBox;

    @FXML
    private Button previousButton;

    @FXML
    private Button nextButton;

    @FXML
    private BorderPane rootPane;

    private FrontShellController shellController;
    private CourseSectionService courseSectionService;
    private LessonService lessonService;
    private Course course;
    private Lesson initialLesson;
    private MediaPlayer mediaPlayer;
    private boolean seeking;
    private final List<Lesson> orderedLessons = new ArrayList<>();
    private final Map<Integer, CourseSection> sectionsById = new HashMap<>();
    private final Map<Integer, Button> lessonButtons = new HashMap<>();
    private Lesson currentLesson;

    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(contentContainer.widthProperty());
        clip.heightProperty().bind(contentContainer.heightProperty());
        contentContainer.setClip(clip);

        rootPane.parentProperty().addListener((observable, oldParent, newParent) -> {
            if (newParent == null) {
                disposeMedia();
            }
        });
    }

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        courseTitleLabel.setText(course == null ? "Course" : course.getTitle());
        loadCurriculum();
    }

    public void setInitialLesson(Lesson lesson) {
        this.initialLesson = lesson;
        if (!orderedLessons.isEmpty()) {
            selectLesson(resolveInitialLesson());
        }
    }

    @FXML
    private void handleBack() {
        disposeMedia();
        if (shellController != null && course != null) {
            shellController.showCourseShow(course);
        }
    }

    @FXML
    private void handlePrevious() {
        int index = orderedLessons.indexOf(currentLesson);
        if (index > 0) {
            selectLesson(orderedLessons.get(index - 1));
        }
    }

    @FXML
    private void handleNext() {
        int index = orderedLessons.indexOf(currentLesson);
        if (index >= 0 && index < orderedLessons.size() - 1) {
            selectLesson(orderedLessons.get(index + 1));
        }
    }

    private void loadCurriculum() {
        sectionsAccordion.getPanes().clear();
        orderedLessons.clear();
        lessonButtons.clear();
        sectionsById.clear();

        if (course == null || course.getId() == null) {
            return;
        }

        try {
            List<CourseSection> sections = getCourseSectionService().getAll().stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .sorted(Comparator.comparing(CourseSection::getPosition, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(CourseSection::getId, Comparator.nullsLast(Integer::compareTo)))
                    .toList();

            Map<Integer, List<Lesson>> lessonsBySection = loadLessonsBySection(sections);
            sections.forEach(section -> sectionsById.put(section.getId(), section));

            for (CourseSection section : sections) {
                List<Lesson> lessons = lessonsBySection.getOrDefault(section.getId(), List.of());
                orderedLessons.addAll(lessons);
                TitledPane pane = buildSectionPane(section, lessons);
                sectionsAccordion.getPanes().add(pane);
            }

            if (orderedLessons.isEmpty()) {
                contentContainer.getChildren().setAll(buildMessagePreview("No lessons available.", "This course does not contain any lesson content yet."));
                previousButton.setDisable(true);
                nextButton.setDisable(true);
                return;
            }

            Platform.runLater(() -> selectLesson(resolveInitialLesson()));
        } catch (SQLException | IllegalStateException e) {
            contentContainer.getChildren().setAll(buildMessagePreview("Unable to load course content.", e.getMessage()));
            previousButton.setDisable(true);
            nextButton.setDisable(true);
        }
    }

    private Map<Integer, List<Lesson>> loadLessonsBySection(List<CourseSection> sections) throws SQLException {
        Map<Integer, List<Lesson>> lessonsBySection = new HashMap<>();
        List<Lesson> lessons = getLessonService().getAll().stream()
                .filter(lesson -> lesson.getSectionId() != null && sectionsByIdOrList(sections).containsKey(lesson.getSectionId()))
                .sorted(Comparator.comparing(Lesson::getPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Lesson::getId, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        for (Lesson lesson : lessons) {
            lessonsBySection.computeIfAbsent(lesson.getSectionId(), key -> new ArrayList<>()).add(lesson);
        }
        return lessonsBySection;
    }

    private Map<Integer, CourseSection> sectionsByIdOrList(List<CourseSection> sections) {
        Map<Integer, CourseSection> sectionMap = new HashMap<>();
        sections.forEach(section -> sectionMap.put(section.getId(), section));
        return sectionMap;
    }

    private TitledPane buildSectionPane(CourseSection section, List<Lesson> lessons) {
        VBox lessonList = new VBox(8);
        lessonList.getStyleClass().add("front-consume-lessons");
        if (lessons.isEmpty()) {
            Label emptyLabel = new Label("No lessons in this section.");
            emptyLabel.getStyleClass().add("front-section-empty");
            lessonList.getChildren().add(emptyLabel);
        } else {
            lessons.forEach(lesson -> lessonList.getChildren().add(buildLessonButton(lesson)));
        }

        TitledPane pane = new TitledPane(section.getTitle(), lessonList);
        pane.getStyleClass().add("front-section-pane");
        pane.setAnimated(true);
        return pane;
    }

    private Button buildLessonButton(Lesson lesson) {
        Label iconLabel = new Label(typeBadge(lesson.getType()));
        iconLabel.getStyleClass().add("front-lesson-icon");

        Label titleLabel = new Label(safeValue(lesson.getTitle(), "Untitled Lesson"));
        titleLabel.getStyleClass().add("front-consume-lesson-title");

        HBox content = new HBox(10, iconLabel, titleLabel);
        content.setAlignment(Pos.CENTER_LEFT);

        Button button = new Button();
        button.setGraphic(content);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("front-lesson-item");
        button.setOnAction(event -> selectLesson(lesson));
        lessonButtons.put(lesson.getId(), button);
        return button;
    }

    private Lesson resolveInitialLesson() {
        if (initialLesson != null) {
            return orderedLessons.stream()
                    .filter(lesson -> lesson.getId() != null && lesson.getId().equals(initialLesson.getId()))
                    .findFirst()
                    .orElse(orderedLessons.get(0));
        }
        return orderedLessons.get(0);
    }

    private void selectLesson(Lesson lesson) {
        if (lesson == null) {
            return;
        }
        currentLesson = lesson;
        lessonTitleLabel.setText(safeValue(lesson.getTitle(), "Lesson"));
        lessonTypeLabel.setText(typeLabel(lesson.getType()));
        highlightCurrentLesson();
        expandCurrentSection();
        renderLessonContent();
        updateNavigationButtons();
    }

    private void highlightCurrentLesson() {
        lessonButtons.values().forEach(button -> {
            button.getStyleClass().remove("front-lesson-item-active");
            if (!button.getStyleClass().contains("front-lesson-item")) {
                button.getStyleClass().add("front-lesson-item");
            }
        });
        if (currentLesson != null && currentLesson.getId() != null) {
            Button currentButton = lessonButtons.get(currentLesson.getId());
            if (currentButton != null) {
                currentButton.getStyleClass().remove("front-lesson-item");
                if (!currentButton.getStyleClass().contains("front-lesson-item-active")) {
                    currentButton.getStyleClass().add("front-lesson-item-active");
                }
            }
        }
    }

    private void expandCurrentSection() {
        if (currentLesson == null || currentLesson.getSectionId() == null) {
            return;
        }
        CourseSection currentSection = sectionsById.get(currentLesson.getSectionId());
        if (currentSection == null) {
            return;
        }
        for (TitledPane pane : sectionsAccordion.getPanes()) {
            if (currentSection.getTitle().equals(pane.getText())) {
                sectionsAccordion.setExpandedPane(pane);
                break;
            }
        }
    }

    private void updateNavigationButtons() {
        int currentIndex = orderedLessons.indexOf(currentLesson);
        previousButton.setDisable(currentIndex <= 0);
        nextButton.setDisable(currentIndex < 0 || currentIndex >= orderedLessons.size() - 1);
    }

    private void renderLessonContent() {
        disposeMedia();
        resetPreviewMeta();
        if (currentLesson == null) {
            return;
        }
        try {
            String lessonType = currentLesson.getType() == null ? "" : currentLesson.getType().toUpperCase();
            switch (lessonType) {
                case "TEXT" -> contentContainer.getChildren().setAll(buildTextPreview());
                case "PDF" -> contentContainer.getChildren().setAll(buildPdfPreview());
                case "VIDEO" -> contentContainer.getChildren().setAll(buildVideoPreview());
                default -> contentContainer.getChildren().setAll(buildMessagePreview("Unsupported lesson type.", "This lesson cannot be previewed in the learning interface."));
            }
        } catch (IllegalStateException | MediaException exception) {
            contentContainer.getChildren().setAll(buildMessagePreview("Preview unavailable.", exception.getMessage()));
        }
    }

    private TextArea buildTextPreview() {
        TextArea textArea = new TextArea(currentLesson.getContent() == null ? "" : currentLesson.getContent());
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setFocusTraversable(false);
        textArea.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        return textArea;
    }

    private WebView buildPdfPreview() {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.prefWidthProperty().bind(contentContainer.widthProperty().subtract(24));
        webView.prefHeightProperty().bind(contentContainer.heightProperty().subtract(24));

        Button openExternallyButton = new Button("Open PDF externally");
        openExternallyButton.getStyleClass().add("secondary-button");
        openExternallyButton.setOnAction(event -> openExternally());
        showPreviewMeta("If the embedded preview fails, open the file with the system PDF viewer.", openExternallyButton);

        webView.getEngine().getLoadWorker().exceptionProperty().addListener((observable, oldException, newException) -> {
            if (newException != null) {
                previewStatusLabel.setText("Embedded PDF preview failed. Use the external viewer instead.");
            }
        });
        webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            switch (newState) {
                case FAILED, CANCELLED -> previewStatusLabel.setText("Embedded PDF preview failed. Use the external viewer instead.");
                case SUCCEEDED -> previewStatusLabel.setText("Embedded PDF preview loaded.");
                default -> {
                }
            }
        });

        webView.getEngine().load(createPdfViewerUrl(resolveFile(currentLesson.getFilePath())));
        return webView;
    }

    private StackPane buildVideoPreview() {
        String mediaUrl = resolveFileUrl(currentLesson.getFilePath());
        Media media = new Media(mediaUrl);
        MediaPlayer player = new MediaPlayer(media);
        mediaPlayer = player;
        player.setAutoPlay(false);

        MediaView mediaView = new MediaView(player);
        mediaView.setPreserveRatio(true);
        mediaView.setFitHeight(VIDEO_PREVIEW_HEIGHT - 24);
        mediaView.fitWidthProperty().bind(Bindings.max(320, contentContainer.widthProperty().subtract(24)));

        Button playPauseButton = new Button("Play");
        playPauseButton.getStyleClass().add("hero-button");

        Slider progressSlider = new Slider(0, 100, 0);
        progressSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(progressSlider, Priority.ALWAYS);

        Slider volumeSlider = new Slider(0, 1, 0.7);
        volumeSlider.setPrefWidth(120);
        player.setVolume(volumeSlider.getValue());

        Label timeLabel = new Label("00:00 / 00:00");
        timeLabel.getStyleClass().add("detail-body");

        showPreviewMeta(null, playPauseButton, progressSlider, timeLabel, volumeSlider);
        playPauseButton.setOnAction(event -> togglePlayback(player, playPauseButton));
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (player == mediaPlayer) {
                player.setVolume(newValue.doubleValue());
            }
        });
        progressSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            seeking = isChanging;
            if (!isChanging) {
                seekToSliderPosition(player, progressSlider);
            }
        });

        player.setOnReady(() -> {
            if (player != mediaPlayer) {
                return;
            }
            Duration totalDuration = player.getTotalDuration();
            progressSlider.setMax(Math.max(totalDuration.toSeconds(), 100));
            updateTimeLabel(timeLabel, Duration.ZERO, totalDuration);
        });
        player.currentTimeProperty().addListener((observable, oldTime, newTime) -> {
            if (player != mediaPlayer) {
                return;
            }
            if (!seeking) {
                progressSlider.setValue(newTime.toSeconds());
            }
            updateTimeLabel(timeLabel, newTime, player.getTotalDuration());
        });
        player.statusProperty().addListener((observable, oldStatus, newStatus) -> {
            if (player != mediaPlayer) {
                return;
            }
            switch (newStatus) {
                case PLAYING -> playPauseButton.setText("Pause");
                default -> playPauseButton.setText("Play");
            }
        });
        player.setOnEndOfMedia(() -> {
            if (player != mediaPlayer) {
                return;
            }
            player.pause();
            player.seek(Duration.ZERO);
            progressSlider.setValue(0);
            playPauseButton.setText("Play");
        });
        player.setOnError(() -> {
            if (player != mediaPlayer) {
                return;
            }
            contentContainer.getChildren().setAll(buildMessagePreview("Video preview unavailable.", player.getError() == null ? "Unable to load the video file." : player.getError().getMessage()));
            disposeMedia();
        });

        StackPane videoSurface = new StackPane(mediaView);
        videoSurface.setAlignment(Pos.CENTER);
        videoSurface.setStyle("-fx-background-color: linear-gradient(to bottom, #0f172a, #1e293b);");
        return videoSurface;
    }

    private VBox buildMessagePreview(String title, String body) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("detail-value");
        Label bodyLabel = new Label(body);
        bodyLabel.getStyleClass().add("detail-body");
        bodyLabel.setWrapText(true);
        return new VBox(10, titleLabel, bodyLabel);
    }

    private void togglePlayback(MediaPlayer player, Button playPauseButton) {
        if (player == null || player != mediaPlayer) {
            return;
        }
        switch (player.getStatus()) {
            case PLAYING -> player.pause();
            default -> {
                player.play();
                playPauseButton.setText("Pause");
            }
        }
    }

    private void seekToSliderPosition(MediaPlayer player, Slider progressSlider) {
        if (player == null || player != mediaPlayer) {
            return;
        }
        Duration totalDuration = player.getTotalDuration();
        if (totalDuration == null || totalDuration.isUnknown() || totalDuration.lessThanOrEqualTo(Duration.ZERO)) {
            return;
        }
        double targetSeconds = Math.max(0, Math.min(progressSlider.getValue(), totalDuration.toSeconds()));
        double snappedSeconds = Math.rint(targetSeconds);
        progressSlider.setValue(snappedSeconds);
        player.seek(Duration.seconds(snappedSeconds));
    }

    private void updateTimeLabel(Label timeLabel, Duration currentTime, Duration totalDuration) {
        timeLabel.setText(formatDuration(currentTime) + " / " + formatDuration(totalDuration));
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.lessThan(Duration.ZERO)) {
            return "00:00";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void resetPreviewMeta() {
        previewStatusLabel.setText("");
        previewStatusLabel.setVisible(false);
        previewStatusLabel.setManaged(false);
        previewActionsBox.getChildren().clear();
        previewActionsBox.setVisible(false);
        previewActionsBox.setManaged(false);
    }

    private void showPreviewMeta(String statusText, javafx.scene.Node... actions) {
        if (statusText != null && !statusText.isBlank()) {
            previewStatusLabel.setText(statusText);
            previewStatusLabel.setVisible(true);
            previewStatusLabel.setManaged(true);
        }
        if (actions != null && actions.length > 0) {
            previewActionsBox.getChildren().setAll(actions);
            previewActionsBox.setVisible(true);
            previewActionsBox.setManaged(true);
        }
    }

    private void openExternally() {
        try {
            Desktop.getDesktop().open(resolveFile(currentLesson.getFilePath()).toFile());
        } catch (IOException | IllegalStateException | UnsupportedOperationException e) {
            showError("Unable to open the file externally.", e);
        }
    }

    private String createPdfViewerUrl(Path pdfPath) {
        try {
            byte[] bytes = Files.readAllBytes(pdfPath);
            String pdfBase64 = Base64.getEncoder().encodeToString(bytes);
            String sourceUrl = escapeForHtml(resolveFileUrl(pdfPath.toString()));
            String html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>Lesson PDF Preview</title>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.min.js"></script>
                        <style>
                            html, body { margin: 0; height: 100%%; background: #f8fafc; font-family: Arial, sans-serif; color: #0f172a; }
                            body { padding: 18px; box-sizing: border-box; }
                            #viewer { display: flex; flex-direction: column; gap: 18px; }
                            .page-card { background: white; border-radius: 14px; box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08); padding: 12px; }
                            canvas { display: block; width: 100%%; height: auto; }
                            #status { font-size: 13px; color: #475569; margin-bottom: 12px; }
                        </style>
                    </head>
                    <body>
                        <div id="status">Loading PDF preview from %s</div>
                        <div id="viewer"></div>
                        <script>
                            pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';
                            const raw = atob('%s');
                            const pdfData = new Uint8Array(raw.length);
                            for (let i = 0; i < raw.length; i += 1) { pdfData[i] = raw.charCodeAt(i); }
                            const viewer = document.getElementById('viewer');
                            const status = document.getElementById('status');
                            pdfjsLib.getDocument({ data: pdfData }).promise.then(async (pdf) => {
                                status.textContent = 'Previewing ' + pdf.numPages + ' page(s)';
                                for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
                                    const page = await pdf.getPage(pageNumber);
                                    const viewport = page.getViewport({ scale: 1.2 });
                                    const card = document.createElement('div');
                                    card.className = 'page-card';
                                    const canvas = document.createElement('canvas');
                                    const context = canvas.getContext('2d');
                                    canvas.width = viewport.width;
                                    canvas.height = viewport.height;
                                    card.appendChild(canvas);
                                    viewer.appendChild(card);
                                    await page.render({ canvasContext: context, viewport: viewport }).promise;
                                }
                            }).catch((error) => {
                                status.textContent = 'Embedded PDF preview failed: ' + error.message;
                            });
                        </script>
                    </body>
                    </html>
                    """.formatted(sourceUrl, pdfBase64);

            Path tempHtml = Files.createTempFile("skillora-pdf-preview-", ".html");
            Files.writeString(tempHtml, html, StandardCharsets.UTF_8);
            tempHtml.toFile().deleteOnExit();
            return tempHtml.toUri().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare the PDF preview.", e);
        }
    }

    private String escapeForHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String resolveFileUrl(String filePath) {
        Path path = resolveFile(filePath);
        String absolutePath = path.toAbsolutePath().normalize().toString().replace("\\", "/");
        if (!absolutePath.startsWith("/")) {
            absolutePath = "/" + absolutePath;
        }
        return "file:///" + absolutePath.substring(1).replace(" ", "%20");
    }

    private Path resolveFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("No file is available for this lesson.");
        }
        Path path = Path.of(filePath);
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(filePath);
        }
        File file = path.normalize().toFile();
        if (!file.exists()) {
            throw new IllegalStateException("Lesson file not found: " + file.getAbsolutePath());
        }
        return file.toPath();
    }

    private void disposeMedia() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private String typeBadge(String type) {
        if (type == null) {
            return "TXT";
        }
        return switch (type.toUpperCase()) {
            case "VIDEO" -> "VID";
            case "PDF" -> "PDF";
            default -> "TXT";
        };
    }

    private String typeLabel(String type) {
        return type == null || type.isBlank() ? "Unknown" : type;
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private CourseSectionService getCourseSectionService() {
        if (courseSectionService == null) {
            courseSectionService = new CourseSectionService();
        }
        return courseSectionService;
    }

    private LessonService getLessonService() {
        if (lessonService == null) {
            lessonService = new LessonService();
        }
        return lessonService;
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
