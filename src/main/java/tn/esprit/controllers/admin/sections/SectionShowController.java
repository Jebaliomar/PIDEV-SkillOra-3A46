package tn.esprit.controllers.admin.sections;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Lesson;
import tn.esprit.services.LessonService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SectionShowController implements AdminShellAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    @FXML
    private Label headerTitleLabel;

    @FXML
    private Label titleValueLabel;

    @FXML
    private Label positionValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private TableView<Lesson> lessonTable;

    @FXML
    private TableColumn<Lesson, String> lessonTitleColumn;

    @FXML
    private TableColumn<Lesson, String> lessonTypeColumn;

    @FXML
    private TableColumn<Lesson, Integer> lessonPositionColumn;

    @FXML
    private Button openLessonButton;

    private AdminShellController shellController;
    private LessonService lessonService;
    private Course course;
    private CourseSection section;

    @FXML
    public void initialize() {
        lessonTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        lessonTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        lessonPositionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        lessonPositionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });
        openLessonButton.disableProperty().bind(Bindings.isNull(lessonTable.getSelectionModel().selectedItemProperty()));
    }

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
            headerTitleLabel.setText(section.getTitle());
            titleValueLabel.setText(section.getTitle());
            positionValueLabel.setText(section.getPosition() == null ? "--" : String.valueOf(section.getPosition()));
            createdAtValueLabel.setText(formatDateTime(section.getCreatedAt()));
            loadLessons();
        }
    }

    @FXML
    private void handleBack() {
        if (shellController != null && course != null) {
            shellController.showSectionIndex(course);
        }
    }

    @FXML
    private void handleAddLesson() {
        if (shellController != null && course != null && section != null) {
            shellController.showLessonNew(course, section);
        }
    }

    @FXML
    private void handleOpenLesson() {
        Lesson lesson = lessonTable.getSelectionModel().getSelectedItem();
        if (lesson != null && shellController != null && course != null && section != null) {
            shellController.showLessonShow(course, section, lesson);
        }
    }

    private void loadLessons() {
        if (section == null || section.getId() == null) {
            lessonTable.setItems(FXCollections.observableArrayList());
            return;
        }
        try {
            List<Lesson> lessons = getLessonService().getAll().stream()
                    .filter(lesson -> section.getId().equals(lesson.getSectionId()))
                    .toList();
            lessonTable.setItems(FXCollections.observableArrayList(lessons));
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load lessons.", e);
        }
    }

    private LessonService getLessonService() {
        if (lessonService == null) {
            lessonService = new LessonService();
        }
        return lessonService;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME_FORMATTER.format(value);
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
