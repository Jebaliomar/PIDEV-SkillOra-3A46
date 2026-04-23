package tn.esprit.controllers.admin.courses;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.CourseService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CourseShowController implements AdminShellAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    @FXML
    private Label headerTitleLabel;

    @FXML
    private Label titleValueLabel;

    @FXML
    private Label categoryValueLabel;

    @FXML
    private Label statusValueLabel;

    @FXML
    private Label descriptionValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private Label updatedAtValueLabel;

    @FXML
    private Label thumbnailValueLabel;

    @FXML
    private TableView<CourseSection> sectionTable;

    @FXML
    private TableColumn<CourseSection, String> sectionTitleColumn;

    @FXML
    private TableColumn<CourseSection, Integer> positionColumn;

    @FXML
    private TableColumn<CourseSection, LocalDateTime> sectionCreatedAtColumn;

    private AdminShellController shellController;
    private CourseSectionService courseSectionService;
    private CourseService courseService;
    private Course course;

    @FXML
    public void initialize() {
        System.out.println("CourseShowController.initialize() called");
        sectionTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        sectionCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        sectionCreatedAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatDateTime(item));
            }
        });
    }

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        populateCourseDetails();
        loadSections();
    }

    @FXML
    private void handleBack() {
        System.out.println("CourseShowController.handleBack() triggered");
        if (shellController != null) {
            shellController.showCoursesIndex();
        }
    }

    @FXML
    private void handleEdit() {
        System.out.println("CourseShowController.handleEdit() triggered");
        if (shellController != null && course != null) {
            shellController.showCourseEdit(course);
        }
    }

    @FXML
    private void handleDelete() {
        System.out.println("CourseShowController.handleDelete() triggered");
        if (course == null) {
            showWarning("No course is loaded.");
            return;
        }
        if (!confirmDelete()) {
            return;
        }

        try {
            if (getCourseService().delete(course.getId())) {
                if (shellController != null) {
                    shellController.showCoursesIndex();
                }
            } else {
                showWarning("The selected course could not be deleted.");
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to delete the course.", e);
        }
    }

    @FXML
    private void handleManageSections() {
        System.out.println("CourseShowController.handleManageSections() triggered");
        if (shellController != null && course != null) {
            shellController.showSectionIndex(course);
        }
    }

    private void populateCourseDetails() {
        if (course == null) {
            return;
        }

        headerTitleLabel.setText(blankAsDash(course.getTitle()));
        titleValueLabel.setText(blankAsDash(course.getTitle()));
        categoryValueLabel.setText(blankAsDash(course.getCategory()));
        statusValueLabel.setText(blankAsDash(course.getStatus()));
        descriptionValueLabel.setText(blankAsDash(course.getDescription()));
        createdAtValueLabel.setText(formatDateTime(course.getCreatedAt()));
        updatedAtValueLabel.setText(formatDateTime(course.getUpdatedAt()));
        thumbnailValueLabel.setText(blankAsDash(course.getThumbnail()));
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
            sectionTable.setItems(FXCollections.observableArrayList(sections));
            System.out.println("CourseShowController.loadSections(): loaded " + sections.size() + " sections");
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load course sections.", e);
        }
    }

    private CourseSectionService getCourseSectionService() {
        if (courseSectionService == null) {
            courseSectionService = new CourseSectionService();
        }
        return courseSectionService;
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : DATE_TIME_FORMATTER.format(value);
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean confirmDelete() {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete this course and all its sections, lessons, enrollments, and lesson completions?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        alert.setHeaderText("Confirm course deletion");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
