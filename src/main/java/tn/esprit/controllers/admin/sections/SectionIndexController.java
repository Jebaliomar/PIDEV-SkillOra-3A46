package tn.esprit.controllers.admin.sections;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.services.CourseSectionService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class SectionIndexController implements AdminShellAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    @FXML
    private Label pageTitleLabel;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<CourseSection> sectionTable;

    @FXML
    private TableColumn<CourseSection, String> titleColumn;

    @FXML
    private TableColumn<CourseSection, Integer> positionColumn;

    @FXML
    private TableColumn<CourseSection, LocalDateTime> createdAtColumn;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button manageLessonsButton;

    private final ObservableList<CourseSection> sections = FXCollections.observableArrayList();
    private AdminShellController shellController;
    private CourseSectionService courseSectionService;
    private Course course;

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        positionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
                setText(empty || item == null ? null : String.valueOf(item));
            }
        });
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : formatDateTime(item));
            }
        });
        sectionTable.setItems(sections);
        editButton.disableProperty().bind(Bindings.isNull(sectionTable.getSelectionModel().selectedItemProperty()));
        deleteButton.disableProperty().bind(Bindings.isNull(sectionTable.getSelectionModel().selectedItemProperty()));
        manageLessonsButton.disableProperty().bind(Bindings.isNull(sectionTable.getSelectionModel().selectedItemProperty()));
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter());
    }

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        pageTitleLabel.setText(course == null ? "Sections" : "Sections · " + course.getTitle());
        loadSections();
    }

    @FXML
    private void handleBack() {
        if (shellController != null && course != null) {
            shellController.showCourseShow(course);
        }
    }

    @FXML
    private void handleAddSection() {
        if (shellController != null && course != null) {
            shellController.showSectionNew(course);
        }
    }

    @FXML
    private void handleEditSection() {
        CourseSection section = sectionTable.getSelectionModel().getSelectedItem();
        if (section != null && shellController != null && course != null) {
            shellController.showSectionEdit(course, section);
        }
    }

    @FXML
    private void handleDeleteSection() {
        CourseSection section = sectionTable.getSelectionModel().getSelectedItem();
        if (section == null) {
            return;
        }
        if (!confirmDelete(section)) {
            return;
        }
        try {
            if (getCourseSectionService().delete(section.getId())) {
                sections.remove(section);
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to delete the section.", e);
        }
    }

    @FXML
    private void handleManageLessons() {
        CourseSection section = sectionTable.getSelectionModel().getSelectedItem();
        if (section != null && shellController != null && course != null) {
            shellController.showSectionShow(course, section);
        }
    }

    private void loadSections() {
        if (course == null || course.getId() == null) {
            sections.clear();
            return;
        }
        try {
            List<CourseSection> items = getCourseSectionService().getAll().stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .toList();
            sections.setAll(items);
            applyFilter();
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load sections.", e);
        }
    }

    private void applyFilter() {
        if (course == null || course.getId() == null) {
            sections.clear();
            return;
        }
        try {
            String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
            List<CourseSection> items = getCourseSectionService().getAll().stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .filter(section -> keyword.isEmpty()
                            || (section.getTitle() != null && section.getTitle().toLowerCase().contains(keyword)))
                    .toList();
            sections.setAll(items);
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to filter sections.", e);
        }
    }

    private CourseSectionService getCourseSectionService() {
        if (courseSectionService == null) {
            courseSectionService = new CourseSectionService();
        }
        return courseSectionService;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME_FORMATTER.format(value);
    }

    private boolean confirmDelete(CourseSection section) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete section \"" + (section.getTitle() == null ? "Untitled section" : section.getTitle()) + "\" and all its lessons and lesson completions?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        alert.setHeaderText("Confirm section deletion");
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
