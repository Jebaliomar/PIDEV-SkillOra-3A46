package tn.esprit.controllers.admin.courses;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CourseNewController {

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

    private CourseService courseService;

    @FXML
    public void initialize() {
        System.out.println("CourseNewController.initialize() called");
    }

    @FXML
    private void handleSave() {
        System.out.println("CourseNewController.handleSave() triggered");
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
            System.out.println("CourseNewController.handleSave(): saved course id=" + course.getId());
            switchToIndex();
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to save the course.", e);
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("CourseNewController.handleCancel() triggered");
        switchToIndex();
    }

    private void switchToIndex() {
        try {
            System.out.println("CourseNewController.switchToIndex() called");
            Parent root = FXMLLoader.load(getClass().getResource("/views/admin/courses/course_index.fxml"));
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Unable to return to the course list.", e);
        }
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }
}
