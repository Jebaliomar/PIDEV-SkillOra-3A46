package tn.esprit.controllers.admin.courses;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class CourseNewController implements AdminShellAware {

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

    private AdminShellController shellController;
    private CourseService courseService;

    @FXML
    public void initialize() {
        System.out.println("CourseNewController.initialize() called");
    }

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    @FXML
    private void handleSave() {
        System.out.println("CourseNewController.handleSave() triggered");
        if (!validateForm()) {
            return;
        }

        Course course = new Course();
        course.setTitle(titleField.getText().trim());
        course.setCategory(categoryField.getText().trim());
        course.setStatus(statusField.getText().trim());
        course.setThumbnail(thumbnailField.getText().trim());
        course.setDescription(descriptionArea.getText().trim());
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());

        try {
            getCourseService().add(course);
            if (shellController != null) {
                shellController.showCoursesIndex();
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to create the course.", e);
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("CourseNewController.handleCancel() triggered");
        if (shellController != null) {
            shellController.showCoursesIndex();
        }
    }

    private boolean validateForm() {
        if (titleField.getText().trim().isEmpty()) {
            showWarning("Title is required.");
            return false;
        }
        return true;
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
