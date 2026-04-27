package tn.esprit.controllers.admin.courses;

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
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.CourseService;
import tn.esprit.tools.AppIcons;

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
    private Label courseInitialLabel;

    @FXML
    private Label sectionCountLabel;

    @FXML
    private Button backButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button addSectionButton;

    @FXML
    private TableView<CourseSection> sectionTable;

    @FXML
    private TableColumn<CourseSection, String> sectionTitleColumn;

    @FXML
    private TableColumn<CourseSection, Integer> positionColumn;

    @FXML
    private TableColumn<CourseSection, LocalDateTime> sectionCreatedAtColumn;

    @FXML
    private TableColumn<CourseSection, Void> sectionActionsColumn;

    private AdminShellController shellController;
    private CourseSectionService courseSectionService;
    private CourseService courseService;
    private Course course;

    @FXML
    public void initialize() {
        System.out.println("CourseShowController.initialize() called");
        setButtonIcon(backButton, AppIcons.back());
        setButtonIcon(editButton, AppIcons.edit());
        setButtonIcon(deleteButton, AppIcons.trash());
        setButtonIcon(addSectionButton, AppIcons.plus());
        sectionTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        sectionTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        positionColumn.setCellValueFactory(new PropertyValueFactory<>("position"));
        positionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER);
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
        sectionActionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button openButton = createActionButton("Open", "manage-action-button");
            private final Button editSectionButton = createActionButton("Edit", "icon-action-button");
            private final Button deleteSectionButton = createActionButton("Delete", "icon-action-danger");
            private final HBox actionsBox = new HBox(10, openButton, editSectionButton, deleteSectionButton);

            {
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                openButton.setOnAction(event -> openSectionFromRow());
                editSectionButton.setOnAction(event -> editSectionFromRow());
                deleteSectionButton.setOnAction(event -> deleteSectionFromRow());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionsBox);
            }

            private void openSectionFromRow() {
                CourseSection section = getTableRow() == null ? null : (CourseSection) getTableRow().getItem();
                openSection(section);
            }

            private void editSectionFromRow() {
                CourseSection section = getTableRow() == null ? null : (CourseSection) getTableRow().getItem();
                if (section != null && shellController != null && course != null) {
                    shellController.showSectionEdit(course, section);
                }
            }

            private void deleteSectionFromRow() {
                CourseSection section = getTableRow() == null ? null : (CourseSection) getTableRow().getItem();
                deleteSection(section);
            }
        });
        sectionTable.setRowFactory(table -> {
            TableRow<CourseSection> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openSection(row.getItem());
                } else if (!row.isEmpty() && event.getClickCount() == 1) {
                    sectionTable.getSelectionModel().select(row.getItem());
                }
            });
            return row;
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
        if (!confirmDeletion(
                "Delete course",
                "Delete \"" + blankAsDash(course.getTitle()) + "\"?",
                "This will permanently delete the course, its sections, and their lessons."
        )) {
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
    private void handleAddSection() {
        if (shellController != null && course != null) {
            shellController.showSectionNew(course);
        }
    }

    private void populateCourseDetails() {
        if (course == null) {
            return;
        }

        headerTitleLabel.setText(blankAsDash(course.getTitle()));
        courseInitialLabel.setText(firstLetter(course.getTitle()));
        titleValueLabel.setText(blankAsDash(course.getTitle()));
        categoryValueLabel.setText(blankAsDash(course.getCategory()));
        statusValueLabel.setText(blankAsDash(course.getStatus()));
        statusValueLabel.getStyleClass().removeAll("status-published", "status-archived", "status-draft");
        statusValueLabel.getStyleClass().add(statusStyleClass(course.getStatus()));
        descriptionValueLabel.setText(blankAsDash(course.getDescription()));
        createdAtValueLabel.setText(formatDateTime(course.getCreatedAt()));
        updatedAtValueLabel.setText(formatDateTime(course.getUpdatedAt()));
        thumbnailValueLabel.setText(blankAsDash(course.getThumbnail()));
    }

    private void loadSections() {
        if (course == null || course.getId() == null) {
            sectionTable.setItems(FXCollections.observableArrayList());
            sectionCountLabel.setText("0");
            return;
        }

        try {
            List<CourseSection> sections = getCourseSectionService().getAll()
                    .stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .toList();
            sectionTable.setItems(FXCollections.observableArrayList(sections));
            sectionCountLabel.setText(String.valueOf(sections.size()));
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

    private void openSection(CourseSection section) {
        if (section != null && shellController != null && course != null) {
            shellController.showSectionShow(course, section);
        }
    }

    private void deleteSection(CourseSection section) {
        if (section == null) {
            return;
        }
        if (!confirmDeletion(
                "Delete section",
                "Delete \"" + blankAsDash(section.getTitle()) + "\"?",
                "This will permanently delete the section and all lessons inside it."
        )) {
            return;
        }
        try {
            if (getCourseSectionService().delete(section.getId())) {
                loadSections();
            }
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to delete the section.", e);
        }
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

    private void setButtonIcon(Button button, javafx.scene.Node icon) {
        if (button != null) {
            button.setGraphic(icon);
            setGraphicScale(button, 0.88);
            button.setGraphicTextGap(8);
        }
    }

    private void setGraphicScale(Button button, double scale) {
        if (button != null && button.getGraphic() != null) {
            button.getGraphic().setScaleX(scale);
            button.getGraphic().setScaleY(scale);
        }
    }

    private String statusStyleClass(String value) {
        String status = value == null ? "" : value.trim().toLowerCase();
        return switch (status) {
            case "published" -> "status-published";
            case "archived" -> "status-archived";
            default -> "status-draft";
        };
    }

    private String blankAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String firstLetter(String value) {
        String safeValue = value == null ? "" : value.trim();
        return safeValue.isBlank() ? "C" : safeValue.substring(0, 1).toUpperCase();
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
