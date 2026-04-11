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

public class CourseEditController {

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
    private Course course;

    @FXML
    public void initialize() {
        System.out.println("CourseEditController.initialize() called");
    }

    public void setCourse(Course course) {
        System.out.println("CourseEditController.setCourse() called with id=" + (course == null ? null : course.getId()));
        this.course = course;
        populateFields();
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
    }

    @FXML
    private void handleSave() {
        System.out.println("CourseEditController.handleSave() triggered");
        if (course == null) {
            showWarning("No course is loaded for editing.");
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
            System.out.println("CourseEditController.handleSave(): updated course id=" + course.getId());
            switchToIndex();
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to update the course.", e);
        }
    }

    @FXML
    private void handleCancel() {
        System.out.println("CourseEditController.handleCancel() triggered");
        switchToIndex();
    }

    private void switchToIndex() {
        try {
            System.out.println("CourseEditController.switchToIndex() called");
            Parent root = FXMLLoader.load(getClass().getResource("/views/admin/courses/course_index.fxml"));
            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Unable to return to the course list.", e);
        }
    }

    private String valueOrEmpty(String value) {
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

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }
}
