package tn.esprit.controllers.admin.courses;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.services.CourseService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class CourseIndexController implements AdminShellAware {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm");

    @FXML
    private TextField searchField;

    @FXML
    private Label resultsLabel;

    @FXML
    private Button openButton;
    
    @FXML
    private TableView<Course> courseTable;

    @FXML
    private TableColumn<Course, String> titleColumn;

    @FXML
    private TableColumn<Course, String> categoryColumn;

    @FXML
    private TableColumn<Course, String> statusColumn;

    @FXML
    private TableColumn<Course, LocalDateTime> createdAtColumn;

    @FXML
    private TableColumn<Course, Void> actionsColumn;

    private AdminShellController shellController;
    private CourseService courseService;
    private final ObservableList<Course> courses = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        System.out.println("CourseIndexController.initialize() called");
        configureTable();
        configureSearch();
        configureActions();
        loadCourses();
    }

    @Override
    public void setShellController(AdminShellController shellController) {
        this.shellController = shellController;
    }

    private void configureTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        titleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Course course = getTableRow().getItem();
                StackPane badge = new StackPane();
                badge.getStyleClass().add("course-badge");
                Label badgeLabel = new Label(firstLetter(course.getTitle()));
                badgeLabel.getStyleClass().add("course-badge-label");
                badge.getChildren().add(badgeLabel);

                Label title = new Label(safe(course.getTitle()));
                title.getStyleClass().add("course-title-cell");
                title.setWrapText(true);

                VBox textBox = new VBox(title);
                textBox.setAlignment(Pos.CENTER_LEFT);

                HBox rowBox = new HBox(16, badge, textBox);
                rowBox.setAlignment(Pos.CENTER_LEFT);
                rowBox.setPadding(new Insets(0, 0, 0, 6));
                setGraphic(rowBox);
            }
        });
        categoryColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label label = new Label(safe(item));
                label.getStyleClass().add("category-cell-label");
                label.setWrapText(true);
                setGraphic(label);
                setText(null);
                setAlignment(Pos.CENTER_LEFT);
            }
        });
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label label = new Label(normalizeStatus(item));
                label.getStyleClass().add("table-status-pill");
                setGraphic(label);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(Pos.CENTER_LEFT);
                setText(empty ? null : formatDateTime(item));
            }
        });
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button manageButton = createActionButton("Manage", "manage-action-button");
            private final Button editButton = createActionButton("✎", "icon-action-button");
            private final Button deleteButton = createActionButton("🗑", "icon-action-button", "danger-icon-action-button");
            private final HBox actionsBox = new HBox(12, manageButton, editButton, deleteButton);

            {
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                manageButton.setOnAction(event -> openCourseFromRow());
                editButton.setOnAction(event -> editCourseFromRow());
                deleteButton.setOnAction(event -> deleteCourseFromRow());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionsBox);
            }

            private void openCourseFromRow() {
                Course course = getTableRow() == null ? null : (Course) getTableRow().getItem();
                if (course != null && shellController != null) {
                    shellController.showCourseShow(course);
                }
            }

            private void editCourseFromRow() {
                Course course = getTableRow() == null ? null : (Course) getTableRow().getItem();
                if (course != null && shellController != null) {
                    shellController.showCourseEdit(course);
                }
            }

            private void deleteCourseFromRow() {
                Course course = getTableRow() == null ? null : (Course) getTableRow().getItem();
                if (course == null) {
                    return;
                }
                if (!confirmDelete(course)) {
                    return;
                }
                try {
                    if (getCourseService().delete(course.getId())) {
                        courses.remove(course);
                    }
                } catch (IllegalStateException e) {
                    showError("Database connection failed.", e);
                } catch (SQLException e) {
                    showError("Unable to delete courses.", e);
                }
            }
        });
        courseTable.setRowFactory(table -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    courseTable.getSelectionModel().select(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureSearch() {
        FilteredList<Course> filteredCourses = new FilteredList<>(courses, course -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredCourses.setPredicate(course ->
                    keyword.isEmpty()
                            || safe(course.getTitle()).toLowerCase().contains(keyword)
                            || safe(course.getCategory()).toLowerCase().contains(keyword)
                            || safe(course.getStatus()).toLowerCase().contains(keyword));
        });

        SortedList<Course> sortedCourses = new SortedList<>(filteredCourses);
        sortedCourses.comparatorProperty().bind(courseTable.comparatorProperty());
        courseTable.setItems(sortedCourses);

        StringBinding resultsBinding = Bindings.createStringBinding(() -> {
            int count = filteredCourses.size();
            return count == 1 ? "1 course" : count + " courses";
        }, filteredCourses);
        resultsLabel.textProperty().bind(resultsBinding);
    }

    private void configureActions() {
        if (openButton != null) {
            openButton.disableProperty().bind(Bindings.isNull(courseTable.getSelectionModel().selectedItemProperty()));
        }
    }

    private void loadCourses() {
        try {
            List<Course> items = getCourseService().getAll();
            courses.setAll(items);
            System.out.println("CourseIndexController.loadCourses(): loaded " + items.size() + " courses");
        } catch (IllegalStateException e) {
            showError("Database connection failed.", e);
        } catch (SQLException e) {
            showError("Unable to load courses.", e);
        }
    }

    @FXML
    private void handleAddCourse() {
        System.out.println("CourseIndexController.handleAddCourse() triggered");
        if (shellController != null) {
            shellController.showCourseNew();
        }
    }

    @FXML
    private void handleOpenCourse() {
        System.out.println("CourseIndexController.handleOpenCourse() triggered");
        Course selectedCourse = courseTable.getSelectionModel().getSelectedItem();
        if (selectedCourse == null) {
            showWarning("Select a course to open.");
            return;
        }

        if (shellController != null) {
            shellController.showCourseShow(selectedCourse);
        }
    }

    private CourseService getCourseService() {
        if (courseService == null) {
            courseService = new CourseService();
        }
        return courseService;
    }

    private Button createActionButton(String text, String... styleClasses) {
        Button button = new Button(text);
        button.getStyleClass().addAll(styleClasses);
        return button;
    }

    private boolean confirmDelete(Course course) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete \"" + safe(course.getTitle()) + "\" and all its sections, lessons, enrollments, and lesson completions?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        alert.setHeaderText("Confirm course deletion");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private String firstLetter(String value) {
        return safe(value).isBlank() ? "C" : safe(value).substring(0, 1).toUpperCase();
    }

    private String normalizeStatus(String value) {
        return safe(value).isBlank() ? "DRAFT" : safe(value).toUpperCase();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "--" : DATE_TIME_FORMATTER.format(value);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
