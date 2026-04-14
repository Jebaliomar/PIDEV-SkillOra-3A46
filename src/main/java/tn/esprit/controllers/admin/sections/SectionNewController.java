package tn.esprit.controllers.admin.sections;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.services.CourseSectionService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class SectionNewController implements AdminShellAware {

    @FXML
    private TextField titleField;

    @FXML
    private TextField positionField;

    @FXML
    private Label titleErrorLabel;

    @FXML
    private Label positionErrorLabel;

    private AdminShellController shellController;
    private CourseSectionService courseSectionService;
    private Course course;

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    @FXML
    private void handleSave() {
        try {
            if (course == null || course.getId() == null) {
                throw new IllegalStateException("No course was provided.");
            }
            if (!validateForm()) {
                return;
            }
            CourseSection section = new CourseSection();
            section.setTitle(titleField.getText().trim());
            section.setPosition(Integer.parseInt(positionField.getText().trim()));
            section.setCourseId(course.getId());
            section.setCreatedAt(LocalDateTime.now());
            section.setUpdatedAt(LocalDateTime.now());
            getCourseSectionService().add(section);
            if (shellController != null) {
                shellController.showSectionIndex(course);
            }
        } catch (SQLException e) {
            showError("Unable to create the section.", e);
        } catch (Exception e) {
            showError("Unable to create the section.", e);
        }
    }

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;
        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String position = positionField.getText() == null ? "" : positionField.getText().trim();

        if (title.isEmpty()) {
            setError(titleErrorLabel, "Title is required.");
            valid = false;
        }

        if (position.isEmpty()) {
            setError(positionErrorLabel, "Position is required.");
            valid = false;
        } else {
            try {
                if (Integer.parseInt(position) <= 0) {
                    setError(positionErrorLabel, "Position must be a positive number.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                setError(positionErrorLabel, "Position must be a positive number.");
                valid = false;
            }
        }
        return valid;
    }

    @FXML
    private void handleCancel() {
        if (shellController != null && course != null) {
            shellController.showSectionIndex(course);
        }
    }

    private void setError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    private void clearErrors() {
        clearError(titleErrorLabel);
        clearError(positionErrorLabel);
    }

    private void clearError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
    }

    private CourseSectionService getCourseSectionService() {
        if (courseSectionService == null) {
            courseSectionService = new CourseSectionService();
        }
        return courseSectionService;
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
