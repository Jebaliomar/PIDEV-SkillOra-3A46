package tn.esprit.controllers.front.courses;

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
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.EnrollmentService;
import tn.esprit.services.LessonService;
import tn.esprit.tools.AuthSession;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
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

    private FrontShellController shellController;
    private CourseSectionService courseSectionService;
    private LessonService lessonService;
    private EnrollmentService enrollmentService;
    private CourseProgressService courseProgressService;
    private CertificateService certificateService;
    private Course course;
    private Enrollment enrollment;
    private Certificate certificate;
    private Set<Integer> completedLessonIds = Set.of();

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        populateCourseHeader();
        loadLearningState();
        loadCurriculum();
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
}
