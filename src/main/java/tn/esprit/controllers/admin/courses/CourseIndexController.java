package tn.esprit.controllers.admin.courses;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CourseIndexController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private TextField searchField;

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

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button viewButton;

    private CourseService courseService;
    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("CourseIndexController.initialize() called");
        configureTable();
        configureSearch();
        configureSelectionState();
        loadCourses();
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
    }

    private void configureSearch() {
        FilteredList<Course> filteredCourses = new FilteredList<>(courses, course -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredCourses.setPredicate(course ->
                    keyword.isEmpty()
                            || (course.getTitle() != null && course.getTitle().toLowerCase().contains(keyword)));
        });

        SortedList<Course> sortedCourses = new SortedList<>(filteredCourses);
        sortedCourses.comparatorProperty().bind(courseTable.comparatorProperty());
        courseTable.setItems(sortedCourses);
    }

    private void configureSelectionState() {
        editButton.disableProperty().bind(Bindings.isNull(courseTable.getSelectionModel().selectedItemProperty()));
        deleteButton.disableProperty().bind(Bindings.isNull(courseTable.getSelectionModel().selectedItemProperty()));
        viewButton.disableProperty().bind(Bindings.isNull(courseTable.getSelectionModel().selectedItemProperty()));
    }

    private void loadCourses() {
        try {
            List<Course> items = getCourseService().getAll();
            System.out.println("CourseIndexController.loadCourses(): loaded " + items.size() + " courses");
            courses.setAll(items);
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load courses.", e);
        }
    }

    @FXML
    private void handleAddCourse() {
        System.out.println("CourseIndexController.handleAddCourse() triggered");
        switchScene("/views/admin/courses/course_new.fxml");
    }

    @FXML
    private void handleEditCourse() {
        System.out.println("CourseIndexController.handleEditCourse() triggered");
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("Select a course to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/courses/course_edit.fxml"));
            Parent root = loader.load();
            CourseEditController controller = loader.getController();
            controller.setCourse(selectedCourse);
            setScene(root);
        } catch (IOException e) {
            showError("Unable to open the edit page.", e);
        }
    }

    @FXML
    private void handleDeleteCourse() {
        System.out.println("CourseIndexController.handleDeleteCourse() triggered");
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("Select a course to delete.");
            return;
        }

        try {
            if (getCourseService().delete(selectedCourse.getId())) {
                System.out.println("CourseIndexController.handleDeleteCourse(): deleted course id=" + selectedCourse.getId());
                courses.remove(selectedCourse);
            } else {
                showWarning("The selected course could not be deleted.");
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to delete the selected course.", e);
        }
    }

    @FXML
    private void handleViewCourse() {
        System.out.println("CourseIndexController.handleViewCourse() triggered");
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("Select a course to view.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/courses/course_show.fxml"));
            Parent root = loader.load();
            CourseShowController controller = loader.getController();
            controller.setCourse(selectedCourse);
            setScene(root);
        } catch (IOException e) {
            showError("Unable to open the details page.", e);
        }
    }

    private void switchScene(String resourcePath) {
        try {
            System.out.println("CourseIndexController.switchScene(): loading " + resourcePath);
            Parent root = FXMLLoader.load(getClass().getResource(resourcePath));
            setScene(root);
        } catch (IOException e) {
            showError("Unable to open the requested page.", e);
        }
    }

    private void setScene(Parent root) {
        Stage stage = (Stage) courseTable.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
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
