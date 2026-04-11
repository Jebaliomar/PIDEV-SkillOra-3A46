package tn.esprit.controllers.admin.courses;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CourseEditController implements AdminShellAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    @FXML
    private TextField titleField;

    @FXML
    private TextField categoryField;

    @FXML
    private TextField statusField;

    @FXML
    private TextField thumbnailField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label editingMetaLabel;

    private AdminShellController shellController;
    private CourseService courseService;
    private Course course;

    @FXML
    public void initialize() {
        System.out.println("CourseEditController.initialize() called");
    }

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        populateFields();
    }

    @FXML
    private void handleSave() {
        System.out.println("CourseEditController.handleSave() triggered");
        if (course == null) {
            showWarning("No course is loaded for editing.");
            return;
        }
        if (!validateForm()) {
            return;
        }

        course.setTitle(titleField.getText().trim());
        course.setCategory(categoryField.getText().trim());
        course.setStatus(statusField.getText().trim());
        course.setThumbnail(thumbnailField.getText().trim());
        course.setDescription(descriptionArea.getText().trim());
        course.setUpdatedAt(LocalDateTime.now());

        try {
            getCourseService().update(course);
            if (shellController != null) {
                shellController.showCourseShow(course);
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to update the course.", e);
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("CourseEditController.handleCancel() triggered");
        if (shellController != null) {
            if (course != null) {
                shellController.showCourseShow(course);
            } else {
                shellController.showCoursesIndex();
            }
        }
    }

    private void populateFields() {
        if (course == null) {
            return;
        }

        titleField.setText(valueOrEmpty(course.getTitle()));
        categoryField.setText(valueOrEmpty(course.getCategory()));
        statusField.setText(valueOrEmpty(course.getStatus()));
        thumbnailField.setText(valueOrEmpty(course.getThumbnail()));
        descriptionArea.setText(valueOrEmpty(course.getDescription()));
        editingMetaLabel.setText("Created " + formatDateTime(course.getCreatedAt())
                + "\nLast updated " + formatDateTime(course.getUpdatedAt()));
    }

    private boolean validateForm() {
        if (titleField.getText().trim().isEmpty()) {
            showWarning("Title is required.");
            return false;
        }
        return true;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME_FORMATTER.format(value);
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
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
