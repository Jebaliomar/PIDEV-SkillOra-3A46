package tn.esprit.controllers.front.home;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FrontBrowseCoursesController implements FrontShellAware {

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
    private ComboBox<String> sortComboBox;

    @FXML
    private VBox categoryFilterBox;

    @FXML
    private FlowPane coursesGrid;

    @FXML
    private Label resultsLabel;

    private FrontShellController shellController;
    private CourseService courseService;
    private List<Course> allCourses = List.of();
    private final List<CheckBox> categoryCheckBoxes = new ArrayList<>();

    @FXML
    public void initialize() {
        sortComboBox.setItems(FXCollections.observableArrayList("Newest", "Oldest", "A-Z", "Z-A"));
        sortComboBox.getSelectionModel().select("Newest");
        sortComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshCourses());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshCourses());

        coursesGrid.setHgap(18);
        coursesGrid.setVgap(18);
        coursesGrid.setPadding(new Insets(6, 0, 0, 0));

        CATEGORY_OPTIONS.forEach(category -> {
            CheckBox checkBox = new CheckBox(category);
            checkBox.getStyleClass().add("front-filter-check");
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshCourses());
            categoryCheckBoxes.add(checkBox);
            categoryFilterBox.getChildren().add(checkBox);
        });

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
            resultsLabel.setText("0 courses");
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
        resultsLabel.setText(filtered.size() + (filtered.size() == 1 ? " course" : " courses"));
    }

    private Comparator<Course> resolveComparator() {
        String sort = sortComboBox.getValue();
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

        VBox card = new VBox(14, categoryLabel, titleLabel, descriptionLabel, spacer);
        card.getStyleClass().add("front-course-card");
        card.setPrefWidth(280);
        card.setMinWidth(260);
        card.setPrefHeight(210);
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
        card.setPrefWidth(360);
        return card;
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }
}
