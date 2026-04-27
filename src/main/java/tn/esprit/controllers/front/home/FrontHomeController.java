package tn.esprit.controllers.front.home;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;

public class FrontHomeController implements FrontShellAware {

    private static final double COURSE_CARD_WIDTH = 318;
    private static final double COURSE_THUMBNAIL_HEIGHT = 132;

    @FXML
    private FlowPane latestCoursesPane;

    private FrontShellController shellController;
    private CourseService courseService;

    @FXML
    public void initialize() {
        latestCoursesPane.setHgap(18);
        latestCoursesPane.setVgap(18);
        latestCoursesPane.setPadding(new Insets(12, 0, 2, 0));
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

        Label descriptionLabel = new Label(truncate(safeValue(course.getDescription(), "No description yet."), 104));
        descriptionLabel.getStyleClass().add("front-course-description");
        descriptionLabel.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label metaLabel = new Label(safeValue(course.getStatus(), "draft").toUpperCase());
        metaLabel.getStyleClass().add("front-course-card-footer");

        Button startButton = new Button("Start Course");
        startButton.getStyleClass().add("front-card-start-button");
        startButton.setOnAction(event -> {
            event.consume();
            openCourse(course);
        });

        HBox footer = new HBox(12, metaLabel, spacer, startButton);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("front-card-footer-row");

        VBox body = new VBox(12, categoryLabel, titleLabel, descriptionLabel, footer);
        body.getStyleClass().add("front-course-card-body");
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox card = new VBox(thumbnailPane, body);
        card.getStyleClass().add("front-course-card");
        card.getStyleClass().add("front-course-card-clickable");
        card.setPrefWidth(COURSE_CARD_WIDTH);
        card.setMinWidth(COURSE_CARD_WIDTH);
        card.setMaxWidth(COURSE_CARD_WIDTH);
        card.setPrefHeight(334);
        card.setOnMouseClicked(event -> openCourse(course));
        return card;
    }

    private StackPane buildThumbnailPane(Course course) {
        StackPane thumbnailPane = new StackPane();
        thumbnailPane.getStyleClass().add("front-course-thumbnail");
        thumbnailPane.setPrefSize(COURSE_CARD_WIDTH, COURSE_THUMBNAIL_HEIGHT);
        thumbnailPane.setMinSize(COURSE_CARD_WIDTH, COURSE_THUMBNAIL_HEIGHT);
        thumbnailPane.setMaxSize(COURSE_CARD_WIDTH, COURSE_THUMBNAIL_HEIGHT);
        Rectangle clip = new Rectangle(COURSE_CARD_WIDTH, COURSE_THUMBNAIL_HEIGHT);
        clip.setArcWidth(40);
        clip.setArcHeight(40);
        thumbnailPane.setClip(clip);

        Image image = loadThumbnail(course.getThumbnail());
        if (image != null) {
            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(COURSE_THUMBNAIL_HEIGHT);
            imageView.setFitWidth(COURSE_CARD_WIDTH);
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
