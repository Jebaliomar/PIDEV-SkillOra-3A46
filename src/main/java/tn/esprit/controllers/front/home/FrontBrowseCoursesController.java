package tn.esprit.controllers.front.home;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FrontBrowseCoursesController implements FrontShellAware {

    private static final double COURSE_CARD_WIDTH = 318;
    private static final double COURSE_THUMBNAIL_HEIGHT = 132;

    private static final List<String> CATEGORY_OPTIONS = List.of(
            "Development",
            "Business",
            "Data Science",
            "Marketing",
            "Design",
            "Personal Development",
            "IT & Software",
            "Photography"
    );

    @FXML
    private TextField searchField;

    @FXML
    private ChoiceBox<String> sortChoiceBox;

    @FXML
    private Button toggleFiltersButton;

    @FXML
    private Button clearFiltersButton;

    @FXML
    private Label activeFiltersLabel;

    @FXML
    private VBox filterPane;

    @FXML
    private VBox categoryFilterBox;

    @FXML
    private ScrollPane coursesScrollPane;

    @FXML
    private FlowPane coursesGrid;

    @FXML
    private Label resultsLabel;

    private FrontShellController shellController;
    private CourseService courseService;
    private List<Course> allCourses = List.of();
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();
    private boolean filtersVisible = true;

    @FXML
    public void initialize() {
        sortChoiceBox.setItems(FXCollections.observableArrayList("Newest", "Oldest", "A-Z", "Z-A"));
        sortChoiceBox.getSelectionModel().select("Newest");
        sortChoiceBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshCourses());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshCourses());

        coursesGrid.setHgap(18);
        coursesGrid.setVgap(20);
        coursesGrid.setPadding(new Insets(8, 0, 14, 0));
        if (coursesScrollPane != null) {
            coursesScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) ->
                    coursesGrid.setPrefWrapLength(Math.max(420, newValue.getWidth() - 24)));
        }

        CATEGORY_OPTIONS.forEach(category -> {
            CheckBox checkBox = new CheckBox(category);
            checkBox.getStyleClass().add("front-filter-check");
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshCourses());
            categoryCheckBoxes.add(checkBox);
            categoryFilterBox.getChildren().add(checkBox);
        });

        updateFilterPaneVisibility();
        loadCourses();
    }

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    private void loadCourses() {
        try {
            allCourses = getCourseService().getAll();
            refreshCourses();
        } catch (SQLException | IllegalStateException e) {
            coursesGrid.getChildren().setAll(buildStateCard("Unable to load courses", e.getMessage()));
            if (resultsLabel != null) {
                resultsLabel.setText("0 courses");
            }
        }
    }

    private void refreshCourses() {
        String searchTerm = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        Set<String> selectedCategories = categoryCheckBoxes.stream()
                .filter(CheckBox::isSelected)
                .map(CheckBox::getText)
                .collect(Collectors.toSet());

        List<Course> filtered = allCourses.stream()
                .filter(course -> searchTerm.isBlank() || safeValue(course.getTitle(), "").toLowerCase(Locale.ROOT).contains(searchTerm))
                .filter(course -> selectedCategories.isEmpty() || selectedCategories.contains(safeValue(course.getCategory(), "")))
                .sorted(resolveComparator())
                .toList();

        coursesGrid.getChildren().clear();
        if (filtered.isEmpty()) {
            coursesGrid.getChildren().add(buildStateCard("No matching courses", "Try adjusting the search term, sort order, or category filters."));
        } else {
            filtered.forEach(course -> coursesGrid.getChildren().add(buildCourseCard(course)));
        }
        if (resultsLabel != null) {
            resultsLabel.setText(filtered.size() + (filtered.size() == 1 ? " course" : " courses"));
        }
        updateFilterSummary(searchTerm, selectedCategories.size());
    }

    private Comparator<Course> resolveComparator() {
        String sort = sortChoiceBox.getValue();
        Comparator<Course> byCreatedAt = Comparator.comparing(Course::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        Comparator<Course> byTitle = Comparator.comparing(course -> safeValue(course.getTitle(), "").toLowerCase(Locale.ROOT));
        return switch (sort == null ? "Newest" : sort) {
            case "Oldest" -> byCreatedAt;
            case "A-Z" -> byTitle;
            case "Z-A" -> byTitle.reversed();
            default -> byCreatedAt.reversed();
        };
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
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label metaLabel = new Label(buildCardFooter(course));
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
        card.setPrefHeight(350);
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

    private VBox buildStateCard(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("placeholder-title");
        Label body = new Label(bodyText == null || bodyText.isBlank() ? "No additional details." : bodyText);
        body.getStyleClass().add("placeholder-subtitle");
        body.setWrapText(true);
        VBox card = new VBox(8, title, body);
        card.getStyleClass().add("placeholder-panel");
        card.setPrefWidth(360);
        return card;
    }

    @FXML
    private void handleToggleFilters() {
        filtersVisible = !filtersVisible;
        updateFilterPaneVisibility();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        sortChoiceBox.getSelectionModel().select("Newest");
        categoryCheckBoxes.forEach(checkBox -> checkBox.setSelected(false));
        refreshCourses();
    }

    @FXML
    private void handleBackHome() {
        if (shellController != null) {
            shellController.showHome();
        }
    }

    private void updateFilterPaneVisibility() {
        if (filterPane != null) {
            filterPane.setVisible(filtersVisible);
            filterPane.setManaged(filtersVisible);
        }
        if (toggleFiltersButton != null) {
            toggleFiltersButton.setText(filtersVisible ? "Hide Filters" : "Show Filters");
        }
    }

    private void updateFilterSummary(String searchTerm, int selectedCategoryCount) {
        if (activeFiltersLabel == null || clearFiltersButton == null) {
            return;
        }
        List<String> parts = new ArrayList<>();
        if (!searchTerm.isBlank()) {
            parts.add("Search active");
        }
        if (selectedCategoryCount > 0) {
            parts.add(selectedCategoryCount + (selectedCategoryCount == 1 ? " category" : " categories"));
        }
        activeFiltersLabel.setText(parts.isEmpty() ? "No active filters" : String.join(" • ", parts));
        clearFiltersButton.setDisable(parts.isEmpty());
    }

    private String buildCardFooter(Course course) {
        String category = safeValue(course.getCategory(), "General");
        String status = safeValue(course.getStatus(), "draft").toUpperCase(Locale.ROOT);
        return category + " | " + status;
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

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }

    private void openCourse(Course course) {
        if (shellController != null && course != null) {
            shellController.showCourseShow(course);
        }
    }

}
