package tn.esprit.controllers.admin.courses;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class CourseNewController implements AdminShellAware {

    private static final List<String> COURSE_CATEGORIES = List.of(
            "Development",
            "Business",
            "Data Science",
            "Marketing",
            "Design",
            "Personal Development",
            "IT & Software",
            "Photography"
    );
    private static final List<String> COURSE_STATUSES = List.of("Published", "Archived", "Draft");

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> categoryComboBox;

    @FXML
    private ComboBox<String> statusComboBox;

    @FXML
    private TextField thumbnailField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private Label titleErrorLabel;

    @FXML
    private Label categoryErrorLabel;

    @FXML
    private Label statusErrorLabel;

    @FXML
    private Label thumbnailErrorLabel;

    @FXML
    private Label descriptionErrorLabel;

    private AdminShellController shellController;
    private CourseService courseService;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList(COURSE_CATEGORIES));
        statusComboBox.setItems(FXCollections.observableArrayList(COURSE_STATUSES));
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
        course.setCategory(categoryComboBox.getValue());
        course.setStatus(statusComboBox.getValue());
        course.setThumbnail(normalizeOptional(thumbnailField.getText()));
        course.setDescription(normalizeOptional(descriptionArea.getText()));
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
        clearErrors();
        boolean valid = true;

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        if (title.isEmpty()) {
            setError(titleErrorLabel, "Title is required.");
            valid = false;
        } else if (title.length() < 3) {
            setError(titleErrorLabel, "Title must contain at least 3 characters.");
            valid = false;
        }

        if (categoryComboBox.getValue() == null || categoryComboBox.getValue().isBlank()) {
            setError(categoryErrorLabel, "Please select a category.");
            valid = false;
        }

        if (statusComboBox.getValue() == null || statusComboBox.getValue().isBlank()) {
            setError(statusErrorLabel, "Please select a status.");
            valid = false;
        }

        String thumbnail = normalizeOptional(thumbnailField.getText());
        if (thumbnail != null && !isValidThumbnail(thumbnail)) {
            setError(thumbnailErrorLabel, "Thumbnail must be a valid image URL or image path.");
            valid = false;
        }

        String description = normalizeOptional(descriptionArea.getText());
        if (description != null && description.length() > 1000) {
            setError(descriptionErrorLabel, "Description must stay under 1000 characters.");
            valid = false;
        }

        return valid;
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }

    private String normalizeOptional(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isValidThumbnail(String value) {
        if (looksLikeHttpUrl(value)) {
            try {
                URI uri = URI.create(value);
                return uri.getScheme() != null && uri.getHost() != null && hasImageExtension(uri.getPath());
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        try {
            return hasImageExtension(value);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean looksLikeHttpUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private boolean hasImageExtension(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".gif") || lower.endsWith(".webp");
    }

    private void clearErrors() {
        clearError(titleErrorLabel);
        clearError(categoryErrorLabel);
        clearError(statusErrorLabel);
        clearError(thumbnailErrorLabel);
        clearError(descriptionErrorLabel);
    }

    private void setError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    private void clearError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
