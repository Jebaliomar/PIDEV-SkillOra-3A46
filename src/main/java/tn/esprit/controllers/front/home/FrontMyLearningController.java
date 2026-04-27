package tn.esprit.controllers.front.home;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Certificate;
import tn.esprit.entities.Course;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.Lesson;
import tn.esprit.entities.User;
import tn.esprit.services.CertificateService;
import tn.esprit.services.CourseProgressService;
import tn.esprit.services.CourseService;
import tn.esprit.services.EnrollmentService;
import tn.esprit.tools.AuthSession;

import java.awt.Desktop;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FrontMyLearningController implements FrontShellAware {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    @FXML
    private Label enrolledCountLabel;

    @FXML
    private Label completedCountLabel;

    @FXML
    private Label averageProgressLabel;

    @FXML
    private Label summaryLabel;

    @FXML
    private javafx.scene.layout.FlowPane enrolledCoursesPane;

    private FrontShellController shellController;
    private EnrollmentService enrollmentService;
    private CourseService courseService;
    private CourseProgressService courseProgressService;
    private CertificateService certificateService;

    @FXML
    public void initialize() {
        loadLearning();
    }

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void handleBrowseCourses() {
        if (shellController != null) {
            shellController.showBrowseCourses();
        }
    }

    private void loadLearning() {
        enrolledCoursesPane.getChildren().clear();
        User user = AuthSession.getCurrentUser();
        if (user == null || user.getId() == null) {
            enrolledCountLabel.setText("0");
            completedCountLabel.setText("0");
            averageProgressLabel.setText("0%");
            summaryLabel.setText("Sign in to track your course progress.");
            enrolledCoursesPane.getChildren().add(buildStateCard("No active session", "Please sign in to see your enrolled courses."));
            return;
        }

        try {
            List<Enrollment> enrollments = getEnrollmentService().findByUser(user.getId());
            if (enrollments.isEmpty()) {
                enrolledCountLabel.setText("0");
                completedCountLabel.setText("0");
                averageProgressLabel.setText("0%");
                summaryLabel.setText("You have not started a course yet.");
                enrolledCoursesPane.getChildren().add(buildStateCard("No courses yet", "Start a course from Browse Courses and it will appear here."));
                return;
            }

            int completedCount = 0;
            int progressTotal = 0;
            for (Enrollment enrollment : enrollments) {
                Course course = enrollment.getCourseId() == null ? null : getCourseService().getById(enrollment.getCourseId());
                if (course == null) {
                    continue;
                }
                getCourseProgressService().recalculateEnrollmentProgress(enrollment, course);
                Certificate certificate = getCertificateService().findOneByEnrollment(enrollment.getId());
                int totalLessons = getCourseProgressService().getTotalLessons(course);
                int completedLessons = getCourseProgressService().getCompletedLessons(enrollment);
                int progress = enrollment.getProgressPercent() == null ? 0 : enrollment.getProgressPercent();
                if (progress >= 100 && certificate == null) {
                    certificate = getCertificateService().issueIfEligible(enrollment, user, course, totalLessons);
                }
                if (progress >= 100) {
                    completedCount++;
                }
                progressTotal += progress;
                enrolledCoursesPane.getChildren().add(buildCourseCard(course, enrollment, certificate, completedLessons, totalLessons, progress));
            }

            enrolledCountLabel.setText(String.valueOf(enrollments.size()));
            completedCountLabel.setText(String.valueOf(completedCount));
            averageProgressLabel.setText(Math.round(progressTotal / (double) enrollments.size()) + "%");
            summaryLabel.setText(enrollments.size() + (enrollments.size() == 1 ? " course in progress" : " courses in progress"));
        } catch (Exception e) {
            enrolledCoursesPane.getChildren().setAll(buildStateCard("Unable to load learning", e.getMessage()));
        }
    }

    private VBox buildCourseCard(Course course, Enrollment enrollment, Certificate certificate, int completedLessons, int totalLessons, int progress) {
        Label categoryLabel = new Label(safeValue(course.getCategory(), "General"));
        categoryLabel.getStyleClass().add("front-course-category");

        Label titleLabel = new Label(safeValue(course.getTitle(), "Untitled Course"));
        titleLabel.getStyleClass().add("learning-card-title");
        titleLabel.setWrapText(true);

        Label enrolledLabel = new Label("Enrolled " + (enrollment.getEnrolledAt() == null ? "" : enrollment.getEnrolledAt().format(DATE_FORMAT)));
        enrolledLabel.getStyleClass().add("learning-card-meta");

        Label progressLabel = new Label(progress + "%");
        progressLabel.getStyleClass().add("learning-progress-value");
        Label lessonCountLabel = new Label(completedLessons + " / " + totalLessons + " lessons");
        lessonCountLabel.getStyleClass().add("learning-card-meta");

        ProgressBar progressBar = new ProgressBar(progress / 100.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("learning-progress-bar");

        Button continueButton = new Button(progress >= 100 ? "Review Course" : "Continue");
        continueButton.getStyleClass().add("primary-button");
        continueButton.setOnAction(event -> openCourse(course, enrollment));

        Button certificateButton = new Button("Certificate");
        certificateButton.getStyleClass().add("secondary-button");
        certificateButton.setDisable(certificate == null);
        Certificate finalCertificate = certificate;
        certificateButton.setOnAction(event -> openCertificate(finalCertificate));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox progressRow = new HBox(10, progressLabel, spacer, lessonCountLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        HBox actions = new HBox(10, continueButton, certificateButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(13, categoryLabel, titleLabel, enrolledLabel, progressRow, progressBar, actions);
        card.getStyleClass().add("learning-course-card");
        card.setPrefWidth(354);
        card.setMinWidth(354);
        card.setMaxWidth(354);
        return card;
    }

    private VBox buildStateCard(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");
        Label body = new Label(bodyText == null || bodyText.isBlank() ? "No additional details." : bodyText);
        body.getStyleClass().add("placeholder-subtitle");
        body.setWrapText(true);
        VBox card = new VBox(8, title, body);
        card.getStyleClass().add("placeholder-panel");
        card.setPrefWidth(420);
        return card;
    }

    private void openCourse(Course course, Enrollment enrollment) {
        try {
            Lesson nextLesson = getCourseProgressService().getNextUncompletedLesson(enrollment, course);
            if (shellController != null) {
                if (nextLesson != null) {
                    shellController.showCourseConsume(course, nextLesson);
                } else {
                    shellController.showCourseShow(course);
                }
            }
        } catch (SQLException e) {
            showError("Unable to open this course.", e);
        }
    }

    private void openCertificate(Certificate certificate) {
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

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showError(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(title);
        alert.setContentText(e == null || e.getMessage() == null ? "Unexpected error." : e.getMessage());
        alert.showAndWait();
    }

    private EnrollmentService getEnrollmentService() {
        if (enrollmentService == null) {
            enrollmentService = new EnrollmentService();
        }
        return enrollmentService;
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
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
