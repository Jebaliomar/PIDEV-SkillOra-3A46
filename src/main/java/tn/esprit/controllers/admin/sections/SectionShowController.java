package tn.esprit.controllers.admin.sections;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Lesson;
import tn.esprit.services.LessonService;
import tn.esprit.tools.AppIcons;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
    private Label sectionInitialLabel;

    @FXML
    private Label lessonCountLabel;

    @FXML
    private TableView<Lesson> lessonTable;

    @FXML
    private TableColumn<Lesson, String> lessonTitleColumn;

    @FXML
    private TableColumn<Lesson, String> lessonTypeColumn;

    @FXML
    private TableColumn<Lesson, Integer> lessonPositionColumn;

    @FXML
    private TableColumn<Lesson, Void> lessonActionsColumn;

    @FXML
    private Button backButton;

    @FXML
    private Button addLessonButton;

    @FXML
    private Button openLessonButton;

    private AdminShellController shellController;
    private LessonService lessonService;
    private Course course;
    private CourseSection section;

    @FXML
    public void initialize() {
        setButtonIcon(backButton, AppIcons.back(), 0.88);
        setButtonIcon(openLessonButton, AppIcons.open(), 0.88);
        setButtonIcon(addLessonButton, AppIcons.plus(), 0.88);
        lessonTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
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
        lessonActionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button openButton = createActionButton("Open", "manage-action-button");
            private final Button editButton = createActionButton("Edit", "icon-action-button");
            private final Button deleteButton = createActionButton("Delete", "icon-action-danger");
            private final HBox actionsBox = new HBox(10, openButton, editButton, deleteButton);

            {
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                openButton.setOnAction(event -> openLessonFromRow());
                editButton.setOnAction(event -> editLessonFromRow());
                deleteButton.setOnAction(event -> deleteLessonFromRow());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionsBox);
            }

            private void openLessonFromRow() {
                Lesson lesson = getTableRow() == null ? null : (Lesson) getTableRow().getItem();
                openLesson(lesson);
            }

            private void editLessonFromRow() {
                Lesson lesson = getTableRow() == null ? null : (Lesson) getTableRow().getItem();
                if (lesson != null && shellController != null && course != null && section != null) {
                    shellController.showLessonEdit(course, section, lesson);
                }
            }

            private void deleteLessonFromRow() {
                Lesson lesson = getTableRow() == null ? null : (Lesson) getTableRow().getItem();
                deleteLesson(lesson);
            }
        });
        lessonTable.setRowFactory(table -> {
            TableRow<Lesson> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openLesson(row.getItem());
                } else if (!row.isEmpty() && event.getClickCount() == 1) {
                    lessonTable.getSelectionModel().select(row.getItem());
                }
            });
            return row;
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
            sectionInitialLabel.setText(firstLetter(section.getTitle()));
            titleValueLabel.setText(section.getTitle());
            positionValueLabel.setText(section.getPosition() == null ? "--" : String.valueOf(section.getPosition()));
            createdAtValueLabel.setText(formatDateTime(section.getCreatedAt()));
            loadLessons();
        }
    }

    @FXML
    private void handleBack() {
        if (shellController != null && course != null) {
            shellController.showCourseShow(course);
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
        openLesson(lesson);
    }

    private void openLesson(Lesson lesson) {
        if (lesson != null && shellController != null && course != null && section != null) {
            shellController.showLessonShow(course, section, lesson);
        }
    }

    private void deleteLesson(Lesson lesson) {
        if (lesson == null) {
            return;
        }
        if (!confirmDeletion(
                "Delete lesson",
                "Delete \"" + (lesson.getTitle() == null || lesson.getTitle().isBlank() ? "this lesson" : lesson.getTitle()) + "\"?",
                "This action permanently deletes the lesson."
        )) {
            return;
        }
        try {
            if (getLessonService().delete(lesson.getId())) {
                loadLessons();
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to delete the lesson.", e);
        }
    }

    private void loadLessons() {
        if (section == null || section.getId() == null) {
            lessonTable.setItems(FXCollections.observableArrayList());
            lessonCountLabel.setText("0");
            return;
        }
        try {
            List<Lesson> lessons = getLessonService().getAll().stream()
                    .filter(lesson -> section.getId().equals(lesson.getSectionId()))
                    .toList();
            lessonTable.setItems(FXCollections.observableArrayList(lessons));
            lessonCountLabel.setText(String.valueOf(lessons.size()));
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

    private String firstLetter(String value) {
        String safeValue = value == null ? "" : value.trim();
        return safeValue.isBlank() ? "S" : safeValue.substring(0, 1).toUpperCase();
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setGraphic(switch (text) {
            case "Open" -> AppIcons.lessons();
            case "Edit" -> AppIcons.edit();
            case "Delete" -> AppIcons.trash();
            default -> null;
        });
        setGraphicScale(button, 0.78);
        button.setGraphicTextGap(7);
        return button;
    }

    private void setButtonIcon(Button button, javafx.scene.Node icon, double scale) {
        if (button != null) {
            button.setGraphic(icon);
            setGraphicScale(button, scale);
            button.setGraphicTextGap(8);
        }
    }

    private void setGraphicScale(Button button, double scale) {
        if (button != null && button.getGraphic() != null) {
            button.getGraphic().setScaleX(scale);
            button.getGraphic().setScaleY(scale);
        }
    }

    private boolean confirmDeletion(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
