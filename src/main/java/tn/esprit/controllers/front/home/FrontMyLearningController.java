package tn.esprit.controllers.front.home;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.StudentLayoutController;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.User;
import tn.esprit.services.CourseService;
import tn.esprit.services.EnrollmentService;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FrontMyLearningController implements FrontShellAware {

    @FXML
    private Label subtitleLabel;

    @FXML
    private Label resultsLabel;

    @FXML
    private FlowPane coursesPane;

    private FrontShellController shellController;
    private EnrollmentService enrollmentService;
    private CourseService courseService;

    @FXML
    public void initialize() {
        coursesPane.setHgap(18);
        coursesPane.setVgap(18);
        coursesPane.setPadding(new Insets(6, 0, 0, 0));
        loadLearningCourses();
    }

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    private void loadLearningCourses() {
        User currentUser = StudentLayoutController.getCurrentUser();
        coursesPane.getChildren().clear();

        if (currentUser == null || currentUser.getId() == null) {
            subtitleLabel.setText("Sign in as a student to view your enrolled courses.");
            resultsLabel.setText("0 courses");
            coursesPane.getChildren().add(buildStateCard("No active student session", "My Learning is available when a student is logged in."));
            return;
        }

        subtitleLabel.setText("Courses connected to " + safeValue(currentUser.getFirstName(), "your") + "'s learning path.");

        try {
            List<Enrollment> enrollments = getEnrollmentService().getByUserId(currentUser.getId()).stream()
                    .filter(enrollment -> enrollment.getCourseId() != null)
                    .toList();

            if (enrollments.isEmpty()) {
                resultsLabel.setText("0 courses");
                coursesPane.getChildren().add(buildStateCard("No courses yet", "Start a course from Browse Courses and it will appear here."));
                return;
            }

            Map<Integer, Enrollment> enrollmentByCourseId = new LinkedHashMap<>();
            for (Enrollment enrollment : enrollments) {
                enrollmentByCourseId.putIfAbsent(enrollment.getCourseId(), enrollment);
            }

            List<Course> courses = getCourseService().getByIds(enrollmentByCourseId.keySet().stream().toList());
            if (courses.isEmpty()) {
                resultsLabel.setText("0 courses");
                coursesPane.getChildren().add(buildStateCard("No available courses", "The enrolled courses could not be loaded from the catalog."));
                return;
            }

            resultsLabel.setText(courses.size() + (courses.size() == 1 ? " course" : " courses"));
            courses.forEach(course -> coursesPane.getChildren().add(buildCourseCard(course, enrollmentByCourseId.get(course.getId()))));
        } catch (SQLException | IllegalStateException e) {
            resultsLabel.setText("0 courses");
            coursesPane.getChildren().add(buildStateCard("Unable to load My Learning", e.getMessage()));
        }
    }

    private VBox buildCourseCard(Course course, Enrollment enrollment) {
        StackPane thumbnailPane = buildThumbnailPane(course);

        Label progressLabel = new Label(buildProgressText(enrollment));
        progressLabel.getStyleClass().add("front-course-progress-pill");

        Label categoryLabel = new Label(safeValue(course.getCategory(), "Uncategorized"));
        categoryLabel.getStyleClass().add("front-course-category");

        Label titleLabel = new Label(safeValue(course.getTitle(), "Untitled Course"));
        titleLabel.getStyleClass().add("front-course-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(truncate(safeValue(course.getDescription(), "No description yet."), 120));
        descriptionLabel.getStyleClass().add("front-course-description");
        descriptionLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(14, thumbnailPane, progressLabel, categoryLabel, titleLabel, descriptionLabel, spacer);
        card.getStyleClass().add("front-course-card");
        card.getStyleClass().add("front-course-card-clickable");
        card.setPrefWidth(290);
        card.setMinWidth(270);
        card.setPrefHeight(300);
        card.setOnMouseClicked(event -> openCourse(course));
        return card;
    }

    private StackPane buildThumbnailPane(Course course) {
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.getStyleClass().add("front-course-thumbnail");
        thumbnailPane.setPrefHeight(130);
        thumbnailPane.setMinHeight(130);
        thumbnailPane.setMaxWidth(Double.MAX_VALUE);

        Image image = loadThumbnail(course.getThumbnail());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(130);
            imageView.setFitWidth(254);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);
            imageView.setCache(true);
            thumbnailPane.getChildren().add(imageView);
        } else {
            Label placeholderLabel = new Label("No Thumbnail");
            placeholderLabel.getStyleClass().add("front-course-thumbnail-placeholder");
            thumbnailPane.getChildren().add(placeholderLabel);
        }
        return thumbnailPane;
    }

    private VBox buildStateCard(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");
        Label body = new Label(bodyText == null || bodyText.isBlank() ? "No additional details." : bodyText);
        body.getStyleClass().add("placeholder-subtitle");
        body.setWrapText(true);
        VBox card = new VBox(8, title, body);
        card.getStyleClass().add("placeholder-panel");
        card.setPrefWidth(380);
        return card;
    }

    private String buildProgressText(Enrollment enrollment) {
        if (enrollment == null || enrollment.getProgressPercent() == null) {
            return "In progress";
        }
        return "Progress " + enrollment.getProgressPercent() + "%";
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
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

    private void openCourse(Course course) {
        if (shellController != null && course != null) {
            shellController.showCourseShow(course);
        }
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
}
