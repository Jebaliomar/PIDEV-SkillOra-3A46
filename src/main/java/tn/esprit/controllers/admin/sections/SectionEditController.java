package tn.esprit.controllers.admin.sections;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.services.CourseSectionService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class SectionEditController implements AdminShellAware {

    @FXML
    private TextField titleField;

    @FXML
    private TextField positionField;

    private AdminShellController shellController;
    private CourseSectionService courseSectionService;
    private Course course;
    private CourseSection section;

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public void setSection(CourseSection section) {
        this.section = section;
        if (section != null) {
            titleField.setText(section.getTitle());
            positionField.setText(section.getPosition() == null ? "" : String.valueOf(section.getPosition()));
        }
    }

    @FXML
    private void handleSave() {
        try {
            if (section == null) {
                throw new IllegalStateException("No section was provided.");
            }
            section.setTitle(titleField.getText().trim());
            section.setPosition(parseInteger(positionField.getText()));
            section.setUpdatedAt(LocalDateTime.now());
            getCourseSectionService().update(section);
            if (shellController != null && course != null) {
                shellController.showSectionIndex(course);
            }
        } catch (SQLException e) {
            showError("Unable to update the section.", e);
        } catch (Exception e) {
            showError("Unable to update the section.", e);
        }
    }

    @FXML
    private void handleCancel() {
        if (shellController != null && course != null) {
            shellController.showSectionIndex(course);
        }
    }

    private Integer parseInteger(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : Integer.parseInt(trimmed);
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
