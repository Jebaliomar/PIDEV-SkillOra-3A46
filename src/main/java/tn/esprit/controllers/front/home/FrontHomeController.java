package tn.esprit.controllers.front.home;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class FrontHomeController implements FrontShellAware {

    @FXML
    private FlowPane latestCoursesPane;

    private FrontShellController shellController;
    private CourseService courseService;

    @FXML
    public void initialize() {
        latestCoursesPane.setHgap(18);
        latestCoursesPane.setVgap(18);
        latestCoursesPane.setPadding(new Insets(4, 0, 0, 0));
        loadLatestCourses();
    }

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void handleViewAll() {
        if (shellController != null) {
            shellController.showBrowseCourses();
        }
    }

    private void loadLatestCourses() {
        latestCoursesPane.getChildren().clear();
        try {
            List<Course> latestCourses = getCourseService().getAll().stream()
                    .sorted(Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(3)
                    .toList();

            if (latestCourses.isEmpty()) {
                latestCoursesPane.getChildren().add(buildEmptyState());
                return;
            }

            latestCourses.forEach(course -> latestCoursesPane.getChildren().add(buildCourseCard(course)));
        } catch (SQLException | IllegalStateException e) {
            latestCoursesPane.getChildren().add(buildErrorState(e.getMessage()));
        }
    }

    private VBox buildCourseCard(Course course) {
        StackPane thumbnailPane = buildThumbnailPane(course);
        Label categoryLabel = new Label(safeValue(course.getCategory(), "Uncategorized"));
        categoryLabel.getStyleClass().add("front-course-category");

        Label titleLabel = new Label(safeValue(course.getTitle(), "Untitled Course"));
        titleLabel.getStyleClass().add("front-course-title");
        titleLabel.setWrapText(true);

        Label descriptionLabel = new Label(truncate(safeValue(course.getDescription(), "No description yet."), 110));
        descriptionLabel.getStyleClass().add("front-course-description");
        descriptionLabel.setWrapText(true);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox card = new VBox(14, thumbnailPane, categoryLabel, titleLabel, descriptionLabel, spacer);
        card.getStyleClass().add("front-course-card");
        card.getStyleClass().add("front-course-card-clickable");
        card.setPrefWidth(300);
        card.setMinWidth(280);
        card.setPrefHeight(270);
        card.setOnMouseClicked(event -> openCourse(course));
        return card;
    }

    private StackPane buildThumbnailPane(Course course) {
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.getStyleClass().add("front-course-thumbnail");
        thumbnailPane.setPrefHeight(120);
        thumbnailPane.setMinHeight(120);
        thumbnailPane.setMaxWidth(Double.MAX_VALUE);

        Image image = loadThumbnail(course.getThumbnail());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(120);
            imageView.setFitWidth(264);
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

    private VBox buildEmptyState() {
        Label title = new Label("No courses yet");
        title.getStyleClass().add("placeholder-title");
        Label body = new Label("Latest courses will appear here once they are created.");
        body.getStyleClass().add("placeholder-subtitle");
        body.setWrapText(true);
        VBox box = new VBox(8, title, body);
        box.getStyleClass().add("placeholder-panel");
        box.setPrefWidth(360);
        return box;
    }

    private VBox buildErrorState(String message) {
        Label title = new Label("Unable to load courses");
        title.getStyleClass().add("placeholder-title");
        Label body = new Label(message == null || message.isBlank() ? "An unexpected error occurred." : message);
        body.getStyleClass().add("placeholder-subtitle");
        body.setWrapText(true);
        VBox box = new VBox(8, title, body);
        box.getStyleClass().add("placeholder-panel");
        box.setPrefWidth(360);
        return box;
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private void openCourse(Course course) {
        if (shellController != null && course != null) {
            shellController.showCourseShow(course);
        }
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }
}
