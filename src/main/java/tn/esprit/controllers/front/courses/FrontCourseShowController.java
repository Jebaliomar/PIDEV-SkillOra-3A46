package tn.esprit.controllers.front.courses;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Certificate;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.Lesson;
import tn.esprit.entities.User;
import tn.esprit.services.CertificateService;
import tn.esprit.services.CourseInsightsService;
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.EnrollmentService;
import tn.esprit.services.LessonService;
import tn.esprit.tools.AuthSession;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FrontCourseShowController implements FrontShellAware {

    @FXML
    private Label courseTitleLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private ImageView thumbnailImageView;

    @FXML
    private Label thumbnailFallbackLabel;

    @FXML
    private VBox sectionsAccordion;

    @FXML
    private ScrollPane rootScrollPane;

    @FXML
    private Label progressLabel;

    @FXML
    private Label completedLessonsLabel;

    @FXML
    private ProgressBar courseProgressBar;

    @FXML
    private Button continueButton;

    @FXML
    private Button certificateButton;

    @FXML
    private Button loadInsightsButton;

    @FXML
    private VBox insightsLoadingBox;

    @FXML
    private Label insightsErrorLabel;

    @FXML
    private VBox insightsResultBox;

    @FXML
    private Label insightsKeywordLabel;

    @FXML
    private VBox insightsCardsBox;

    @FXML
    private VBox insightsChartBox;

    private FrontShellController shellController;
    private CourseSectionService courseSectionService;
    private LessonService lessonService;
    private EnrollmentService enrollmentService;
    private CourseProgressService courseProgressService;
    private CertificateService certificateService;
    private CourseInsightsService courseInsightsService;
    private Course course;
    private Enrollment enrollment;
    private Certificate certificate;
    private Set<Integer> completedLessonIds = Set.of();
    private CourseInsightsService.CourseInsights loadedInsights;

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        populateCourseHeader();
        loadLearningState();
        loadCurriculum();
        resetInsightsUi();
    }

    private void populateCourseHeader() {
        if (course == null) {
            return;
        }
        courseTitleLabel.setText(safeValue(course.getTitle(), "Untitled Course"));
        categoryLabel.setText(safeValue(course.getCategory(), "Uncategorized"));
        descriptionLabel.setText(safeValue(course.getDescription(), "No description is available for this course yet."));
        Image image = loadThumbnail(course.getThumbnail());
        thumbnailImageView.setImage(image);
        thumbnailImageView.setVisible(image != null);
        thumbnailImageView.setManaged(image != null);
        thumbnailFallbackLabel.setVisible(image == null);
        thumbnailFallbackLabel.setManaged(image == null);
    }

    @FXML
    private void handleContinueCourse() {
        try {
            Enrollment currentEnrollment = ensureEnrollment();
            Lesson nextLesson = getCourseProgressService().getNextUncompletedLesson(currentEnrollment, course);
            if (nextLesson == null) {
                showInfo("No lessons available", "This course does not contain any lesson content yet.");
                return;
            }
            openLesson(nextLesson);
        } catch (SQLException | IllegalStateException e) {
            showError("Unable to start this course.", e);
        }
    }

    @FXML
    private void handleOpenCertificate() {
        if (certificate == null) {
            return;
        }
        try {
            Path pdfPath = getCertificateService().ensurePdfGenerated(certificate);
            Desktop.getDesktop().open(pdfPath.toFile());
        } catch (Exception e) {
            showError("Unable to open the certificate.", e);
        }
    }

    @FXML
    private void handleLoadInsights() {
        if (course == null) {
            return;
        }
        if (loadedInsights != null) {
            renderInsights(loadedInsights);
            return;
        }

        setInsightsLoading(true);
        insightsErrorLabel.setVisible(false);
        insightsErrorLabel.setManaged(false);
        insightsResultBox.setVisible(false);
        insightsResultBox.setManaged(false);

        Task<CourseInsightsService.CourseInsights> task = new Task<>() {
            @Override
            protected CourseInsightsService.CourseInsights call() {
                return getCourseInsightsService().getInsightsForCourse(course);
            }
        };
        task.setOnSucceeded(event -> {
            loadedInsights = task.getValue();
            setInsightsLoading(false);
            renderInsights(loadedInsights);
        });
        task.setOnFailed(event -> {
            setInsightsLoading(false);
            Throwable error = task.getException();
            showInsightsError(error == null || error.getMessage() == null
                    ? "Unable to load insights right now. Please try again."
                    : error.getMessage());
        });

        Thread thread = new Thread(task, "course-insights-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadLearningState() {
        enrollment = null;
        certificate = null;
        completedLessonIds = Set.of();
        updateProgressUi(0, 0, 0);
        certificateButton.setVisible(false);
        certificateButton.setManaged(false);
        continueButton.setText("Start Course");

        User user = AuthSession.getCurrentUser();
        if (course == null || course.getId() == null || user == null || user.getId() == null) {
            return;
        }

        try {
            enrollment = getEnrollmentService().findOneByUserAndCourse(user.getId(), course.getId());
            int totalLessons = getCourseProgressService().getTotalLessons(course);
            int completedLessons = enrollment == null ? 0 : getCourseProgressService().getCompletedLessons(enrollment);
            if (enrollment != null) {
                getCourseProgressService().recalculateEnrollmentProgress(enrollment, course);
                completedLessonIds = getCourseProgressService().findCompletedLessonIds(enrollment);
                certificate = getCertificateService().findOneByEnrollment(enrollment.getId());
                if (certificate == null && enrollment.getProgressPercent() != null && enrollment.getProgressPercent() >= 100) {
                    certificate = getCertificateService().issueIfEligible(enrollment, user, course, totalLessons);
                }
                continueButton.setText(completedLessons >= totalLessons && totalLessons > 0 ? "Review Course" : "Continue Course");
            }
            updateProgressUi(enrollment == null ? 0 : enrollment.getProgressPercent(), completedLessons, totalLessons);
            updateCertificateButton();
        } catch (Exception e) {
            updateProgressUi(0, 0, 0);
        }
    }

    private void loadCurriculum() {
        sectionsAccordion.getChildren().clear();
        if (course == null || course.getId() == null) {
            return;
        }

        try {
            List<CourseSection> sections = getCourseSectionService().getAll().stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .sorted(Comparator.comparing(CourseSection::getPosition, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(CourseSection::getId, Comparator.nullsLast(Integer::compareTo)))
                    .toList();

            if (sections.isEmpty()) {
                sectionsAccordion.getChildren().add(buildEmptyPane("No sections yet", "This course does not contain any sections."));
                return;
            }

            Map<Integer, List<Lesson>> lessonsBySection = loadLessonsForSections(sections);
            for (int index = 0; index < sections.size(); index++) {
                CourseSection section = sections.get(index);
                TitledPane pane = buildSectionPane(section, lessonsBySection.getOrDefault(section.getId(), List.of()));
                sectionsAccordion.getChildren().add(pane);
                if (index == 0) {
                    pane.setExpanded(true);
                }
            }
        } catch (SQLException | IllegalStateException e) {
            sectionsAccordion.getChildren().add(buildEmptyPane("Unable to load curriculum", e.getMessage()));
        }
    }

    private Map<Integer, List<Lesson>> loadLessonsForSections(List<CourseSection> sections) throws SQLException {
        Set<Integer> sectionIds = sections.stream()
                .map(CourseSection::getId)
                .collect(Collectors.toSet());

        return getLessonService().getAll().stream()
                .filter(lesson -> lesson.getSectionId() != null && sectionIds.contains(lesson.getSectionId()))
                .sorted(Comparator.comparing(Lesson::getPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Lesson::getId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.groupingBy(Lesson::getSectionId, Collectors.mapping(Function.identity(), Collectors.toList())));
    }

    private TitledPane buildSectionPane(CourseSection section, List<Lesson> lessons) {
        VBox lessonList = new VBox(10);
        lessonList.getStyleClass().add("front-section-lessons");

        if (lessons.isEmpty()) {
            Label emptyLabel = new Label("No lessons in this section yet.");
            emptyLabel.getStyleClass().add("front-section-empty");
            lessonList.getChildren().add(emptyLabel);
        } else {
            lessons.forEach(lesson -> lessonList.getChildren().add(buildLessonRow(lesson)));
        }

        TitledPane pane = new TitledPane(section.getTitle(), lessonList);
        pane.getStyleClass().add("front-section-pane");
        pane.setAnimated(true);
        return pane;
    }

    private HBox buildLessonRow(Lesson lesson) {
        Label iconLabel = new Label(typeBadge(lesson.getType()));
        iconLabel.getStyleClass().add("front-lesson-icon");

        Label titleLabel = new Label(safeValue(lesson.getTitle(), "Untitled Lesson"));
        titleLabel.getStyleClass().add("front-lesson-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        boolean completed = lesson.getId() != null && completedLessonIds.contains(lesson.getId());
        Label actionLabel = new Label(completed ? "Completed" : "Open");
        actionLabel.getStyleClass().add("front-lesson-action");

        HBox row = new HBox(12, iconLabel, titleLabel, spacer, actionLabel);
        row.getStyleClass().add("front-lesson-row");
        if (completed) {
            row.getStyleClass().add("front-lesson-row-completed");
        }
        row.setOnMouseClicked(event -> openLesson(lesson));
        return row;
    }

    private TitledPane buildEmptyPane(String title, String body) {
        Label label = new Label(body == null || body.isBlank() ? "No additional details." : body);
        label.setWrapText(true);
        label.getStyleClass().add("front-section-empty");
        TitledPane pane = new TitledPane(title, new VBox(label));
        pane.setExpanded(true);
        pane.getStyleClass().add("front-section-pane");
        return pane;
    }

    private void openLesson(Lesson lesson) {
        if (shellController != null && course != null && lesson != null) {
            try {
                ensureEnrollment();
                shellController.showCourseConsume(course, lesson);
            } catch (SQLException | IllegalStateException e) {
                showError("Unable to open this lesson.", e);
            }
        }
    }

    private Enrollment ensureEnrollment() throws SQLException {
        if (course == null || course.getId() == null) {
            throw new IllegalStateException("No course is selected.");
        }
        User user = AuthSession.getCurrentUser();
        if (user == null || user.getId() == null) {
            throw new IllegalStateException("Please sign in before starting a course.");
        }
        enrollment = getEnrollmentService().enrollIfMissing(user.getId(), course.getId());
        loadLearningState();
        return enrollment;
    }

    private void updateProgressUi(Number progressValue, int completedLessons, int totalLessons) {
        int progress = progressValue == null ? 0 : Math.max(0, Math.min(100, progressValue.intValue()));
        progressLabel.setText(progress + "% complete");
        completedLessonsLabel.setText(completedLessons + " / " + totalLessons + (totalLessons == 1 ? " lesson" : " lessons"));
        courseProgressBar.setProgress(progress / 100.0);
    }

    private void updateCertificateButton() {
        boolean hasCertificate = certificate != null;
        certificateButton.setVisible(hasCertificate);
        certificateButton.setManaged(hasCertificate);
    }

    private void resetInsightsUi() {
        loadedInsights = null;
        if (loadInsightsButton != null) {
            loadInsightsButton.setDisable(false);
            loadInsightsButton.setText("Show Insights");
        }
        setManagedVisible(insightsLoadingBox, false);
        setManagedVisible(insightsResultBox, false);
        if (insightsErrorLabel != null) {
            insightsErrorLabel.setText("");
            insightsErrorLabel.setVisible(false);
            insightsErrorLabel.setManaged(false);
        }
        if (insightsCardsBox != null) {
            insightsCardsBox.getChildren().clear();
        }
        if (insightsChartBox != null) {
            insightsChartBox.getChildren().clear();
        }
    }

    private void setInsightsLoading(boolean loading) {
        if (loadInsightsButton != null) {
            loadInsightsButton.setDisable(loading);
            loadInsightsButton.setText(loading ? "Loading..." : "Show Insights");
        }
        setManagedVisible(insightsLoadingBox, loading);
    }

    private void showInsightsError(String message) {
        if (insightsErrorLabel == null) {
            return;
        }
        insightsErrorLabel.setText(message == null || message.isBlank() ? "Unable to load insights right now. Please try again." : message);
        insightsErrorLabel.setVisible(true);
        insightsErrorLabel.setManaged(true);
    }

    private void renderInsights(CourseInsightsService.CourseInsights insights) {
        if (insights == null) {
            return;
        }

        insightsKeywordLabel.setText(insights.keyword());
        insightsCardsBox.getChildren().setAll(buildReasonRows(insights.reasons()));
        renderStrengthChart(insights.chart());
        setManagedVisible(insightsResultBox, true);
    }

    private List<HBox> buildReasonRows(List<CourseInsightsService.InsightReason> reasons) {
        List<HBox> rows = new java.util.ArrayList<>();
        for (int index = 0; index < reasons.size(); index += 2) {
            HBox row = new HBox(14);
            row.getStyleClass().add("course-insights-card-row");

            VBox first = buildReasonCard(reasons.get(index));
            HBox.setHgrow(first, Priority.ALWAYS);
            row.getChildren().add(first);

            if (index + 1 < reasons.size()) {
                VBox second = buildReasonCard(reasons.get(index + 1));
                HBox.setHgrow(second, Priority.ALWAYS);
                row.getChildren().add(second);
            }
            rows.add(row);
        }
        return rows;
    }

    private VBox buildReasonCard(CourseInsightsService.InsightReason reason) {
        Label title = new Label(safeValue(reason.title(), "Insight").toUpperCase(Locale.ROOT));
        title.getStyleClass().add("course-insights-card-kicker");

        Label value = new Label(safeValue(reason.value(), "Unavailable"));
        value.getStyleClass().add("course-insights-card-value");
        value.setWrapText(true);

        Label detail = new Label(safeValue(reason.detail(), "No detail available."));
        detail.getStyleClass().add("course-insights-card-detail");
        detail.setWrapText(true);

        VBox card = new VBox(8, title, value, detail);
        card.getStyleClass().add("course-insights-reason-card");
        card.setMaxWidth(Double.MAX_VALUE);

        String link = reason.link();
        if (link != null && !link.isBlank()) {
            Button linkButton = new Button("Learn more");
            linkButton.setFocusTraversable(false);
            linkButton.getStyleClass().add("course-insights-link");
            linkButton.setOnAction(event -> openExternalLink(link));
            card.getChildren().add(linkButton);
        }
        return card;
    }

    private void renderStrengthChart(CourseInsightsService.InsightChart chart) {
        insightsChartBox.getChildren().clear();
        if (chart == null || chart.labels().isEmpty()) {
            Label empty = new Label("No chart data available yet.");
            empty.getStyleClass().add("front-section-empty");
            insightsChartBox.getChildren().add(empty);
            return;
        }

        for (int index = 0; index < chart.labels().size(); index++) {
            String labelText = chart.labels().get(index);
            int normalized = safeInt(chart.normalized(), index);
            int raw = safeInt(chart.values(), index);

            Label label = new Label(labelText);
            label.getStyleClass().add("course-insights-bar-label");

            Label value = new Label(formatNumber(raw));
            value.getStyleClass().add("course-insights-bar-value");

            Region track = new Region();
            track.getStyleClass().add("course-insights-bar-track");
            track.setMinHeight(12);
            track.setPrefHeight(12);
            track.setMaxHeight(12);
            track.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(track, Priority.ALWAYS);

            Region fill = new Region();
            fill.getStyleClass().addAll("course-insights-bar-fill", "course-insights-bar-fill-" + ((index % 4) + 1));
            fill.setMinHeight(12);
            fill.setPrefHeight(12);
            fill.setMaxHeight(12);
            fill.setMaxWidth(Region.USE_PREF_SIZE);
            fill.prefWidthProperty().bind(track.widthProperty().multiply(Math.max(0, Math.min(100, normalized)) / 100.0));

            StackPane bar = new StackPane();
            bar.getStyleClass().add("course-insights-bar-shell");
            bar.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);
            bar.getChildren().addAll(track, fill);

            HBox header = new HBox(10, label, new Region(), value);
            HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
            header.getStyleClass().add("course-insights-bar-header");

            VBox row = new VBox(6, header, bar);
            row.getStyleClass().add("course-insights-bar-row");
            insightsChartBox.getChildren().add(row);
        }
    }

    private void openExternalLink(String link) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(link));
            }
        } catch (Exception ignored) {
            // External links are helpful, but the insights remain useful if the OS blocks opening them.
        }
    }

    private void setManagedVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(e == null || e.getMessage() == null ? "Unexpected error." : e.getMessage());
        alert.showAndWait();
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

    private Image loadThumbnail(String thumbnail) {
        if (thumbnail == null || thumbnail.isBlank()) {
            return null;
        }
        try {
            if (thumbnail.startsWith("http://") || thumbnail.startsWith("https://")) {
                return new Image(thumbnail, true);
            }
            File file = new File(thumbnail);
            String imageUrl = file.exists() ? file.toURI().toString() : new File(thumbnail.replace("/", File.separator)).toURI().toString();
            return new Image(imageUrl, true);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int safeInt(List<Integer> values, int index) {
        return values == null || index < 0 || index >= values.size() || values.get(index) == null ? 0 : values.get(index);
    }

    private String formatNumber(int value) {
        return String.format(Locale.US, "%,d", value);
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

    private EnrollmentService getEnrollmentService() {
        if (enrollmentService == null) {
            enrollmentService = new EnrollmentService();
        }
        return enrollmentService;
    }

    private CourseProgressService getCourseProgressService() {
        if (courseProgressService == null) {
            courseProgressService = new CourseProgressService();
        }
        return courseProgressService;
    }

    private CertificateService getCertificateService() {
        if (certificateService == null) {
            certificateService = new CertificateService();
        }
        return certificateService;
    }

    private CourseInsightsService getCourseInsightsService() {
        if (courseInsightsService == null) {
            courseInsightsService = new CourseInsightsService();
        }
        return courseInsightsService;
    }
}
