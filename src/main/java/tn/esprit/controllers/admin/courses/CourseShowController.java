package tn.esprit.controllers.admin.courses;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.services.CourseSectionService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CourseShowController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label titleValueLabel;

    @FXML
    private Label categoryValueLabel;

    @FXML
    private Label statusValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private TableView<CourseSection> sectionTable;

    @FXML
    private TableColumn<CourseSection, String> sectionTitleColumn;

    @FXML
    private TableColumn<CourseSection, Integer> positionColumn;

    @FXML
    private TableColumn<CourseSection, LocalDateTime> sectionCreatedAtColumn;

    private CourseSectionService courseSectionService;
    private Course course;

    @FXML
    public void initialize() {
        System.out.println("CourseShowController.initialize() called");
        sectionTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        positionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });
        sectionCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        sectionCreatedAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatDateTime(item));
            }
        });
    }

    public void setCourse(Course course) {
        System.out.println("CourseShowController.setCourse() called with id=" + (course == null ? null : course.getId()));
        this.course = course;
        populateCourseDetails();
        loadSections();
    }

    private void populateCourseDetails() {
        if (course == null) {
            return;
        }

        titleValueLabel.setText(valueOrDash(course.getTitle()));
        categoryValueLabel.setText(valueOrDash(course.getCategory()));
        statusValueLabel.setText(valueOrDash(course.getStatus()));
        createdAtValueLabel.setText(formatDateTime(course.getCreatedAt()));
    }

    private void loadSections() {
        if (course == null || course.getId() == null) {
            sectionTable.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            List<CourseSection> sections = getCourseSectionService().getAll()
                    .stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .toList();
            System.out.println("CourseShowController.loadSections(): loaded " + sections.size()
                    + " sections for course id=" + course.getId());
            sectionTable.setItems(FXCollections.observableArrayList(sections));
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load course sections.", e);
        }
    }

    @FXML
    private void handleBack() {
        System.out.println("CourseShowController.handleBack() triggered");
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/admin/courses/course_index.fxml"));
            Stage stage = (Stage) sectionTable.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showError("Unable to return to the course list.", e);
        }
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }

    private CourseSectionService getCourseSectionService() {
        if (courseSectionService == null) {
            courseSectionService = new CourseSectionService();
        }
        return courseSectionService;
    }
}
