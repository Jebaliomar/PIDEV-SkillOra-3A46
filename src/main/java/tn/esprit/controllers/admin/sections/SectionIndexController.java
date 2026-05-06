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
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.services.CourseSectionService;
import tn.esprit.tools.AppIcons;

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
    private TableColumn<CourseSection, Void> actionsColumn;

    @FXML
    private Button backButton;

    @FXML
    private Button addSectionButton;

    private final ObservableList<CourseSection> sections = FXCollections.observableArrayList();
    private AdminShellController shellController;
    private CourseSectionService courseSectionService;
    private Course course;

    @FXML
    public void initialize() {
        setButtonIcon(backButton, AppIcons.back());
        setButtonIcon(addSectionButton, AppIcons.plus());
        sectionTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
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
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button openButton = createActionButton("Open", "manage-action-button");
            private final Button editButton = createActionButton("Edit", "icon-action-button");
            private final Button deleteButton = createActionButton("Delete", "icon-action-danger");
            private final HBox actionsBox = new HBox(10, openButton, editButton, deleteButton);

            {
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                openButton.setOnAction(event -> openSectionFromRow());
                editButton.setOnAction(event -> editSectionFromRow());
                deleteButton.setOnAction(event -> deleteSectionFromRow());
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
                editSection(section);
            }

            private void deleteSectionFromRow() {
                CourseSection section = getTableRow() == null ? null : (CourseSection) getTableRow().getItem();
                deleteSection(section);
            }
        });
        sectionTable.setItems(sections);
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
        editSection(section);
    }

    @FXML
    private void handleDeleteSection() {
        CourseSection section = sectionTable.getSelectionModel().getSelectedItem();
        deleteSection(section);
    }

    @FXML
    private void handleManageLessons() {
        CourseSection section = sectionTable.getSelectionModel().getSelectedItem();
        openSection(section);
    }

    private void editSection(CourseSection section) {
        if (section != null && shellController != null && course != null) {
            shellController.showSectionEdit(course, section);
        }
    }

    private void deleteSection(CourseSection section) {
        if (section == null) {
            return;
        }
        if (!confirmDeletion(
                "Delete section",
                "Delete \"" + (section.getTitle() == null || section.getTitle().isBlank() ? "this section" : section.getTitle()) + "\"?",
                "This will permanently delete the section and all lessons inside it."
        )) {
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

    private void openSection(CourseSection section) {
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

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME_FORMATTER.format(value);
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
