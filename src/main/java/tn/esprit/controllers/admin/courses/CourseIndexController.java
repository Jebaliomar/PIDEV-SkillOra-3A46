package tn.esprit.controllers.admin.courses;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CourseIndexController implements AdminShellAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    @FXML
    private TextField searchField;

    @FXML
    private Label resultsLabel;

    @FXML
    private Button openButton;

    @FXML
    private TableView<Course> courseTable;

    @FXML
    private TableColumn<Course, String> titleColumn;

    @FXML
    private TableColumn<Course, String> categoryColumn;

    @FXML
    private TableColumn<Course, String> statusColumn;

    @FXML
    private TableColumn<Course, LocalDateTime> createdAtColumn;

    private AdminShellController shellController;
    private CourseService courseService;
    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("CourseIndexController.initialize() called");
        configureTable();
        configureSearch();
        configureActions();
        loadCourses();
    }

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    private void configureTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatDateTime(item));
            }
        });
        courseTable.setRowFactory(table -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    courseTable.getSelectionModel().select(row.getItem());
                    handleOpenCourse();
                }
            });
            return row;
        });
    }

    private void configureSearch() {
        FilteredList<Course> filteredCourses = new FilteredList<>(courses, course -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredCourses.setPredicate(course ->
                    keyword.isEmpty()
                            || safe(course.getTitle()).toLowerCase().contains(keyword)
                            || safe(course.getCategory()).toLowerCase().contains(keyword)
                            || safe(course.getStatus()).toLowerCase().contains(keyword));
        });

        SortedList<Course> sortedCourses = new SortedList<>(filteredCourses);
        sortedCourses.comparatorProperty().bind(courseTable.comparatorProperty());
        courseTable.setItems(sortedCourses);

        StringBinding resultsBinding = Bindings.createStringBinding(() -> {
            int count = filteredCourses.size();
            return count == 1 ? "1 course" : count + " courses";
        }, filteredCourses);
        resultsLabel.textProperty().bind(resultsBinding);
    }

    private void configureActions() {
        openButton.disableProperty().bind(Bindings.isNull(courseTable.getSelectionModel().selectedItemProperty()));
    }

    private void loadCourses() {
        try {
            List<Course> items = getCourseService().getAll();
            courses.setAll(items);
            System.out.println("CourseIndexController.loadCourses(): loaded " + items.size() + " courses");
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load courses.", e);
        }
    }

    @FXML
    private void handleAddCourse() {
        System.out.println("CourseIndexController.handleAddCourse() triggered");
        if (shellController != null) {
            shellController.showCourseNew();
        }
    }

    @FXML
    private void handleOpenCourse() {
        System.out.println("CourseIndexController.handleOpenCourse() triggered");
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("Select a course to open.");
            return;
        }

        if (shellController != null) {
            shellController.showCourseShow(selectedCourse);
        }
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME_FORMATTER.format(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
