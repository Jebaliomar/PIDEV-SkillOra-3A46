package tn.esprit.controllers.admin.lessons;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.esprit.controllers.admin.AdminShellAware;
import tn.esprit.controllers.admin.AdminShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Lesson;
import tn.esprit.services.LessonService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class LessonEditController implements AdminShellAware {

    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField positionField;

    @FXML
    private VBox contentBox;

    @FXML
    private TextArea contentArea;

    @FXML
    private VBox fileBox;

    @FXML
    private Label selectedFileLabel;

    private AdminShellController shellController;
    private LessonService lessonService;
    private Course course;
    private CourseSection section;
    private Lesson lesson;
    private File selectedFile;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList("TEXT", "PDF", "VIDEO"));
        typeComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateTypeUI(newValue));
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
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
        if (lesson != null) {
            titleField.setText(lesson.getTitle());
            positionField.setText(lesson.getPosition() == null ? "" : String.valueOf(lesson.getPosition()));
            typeComboBox.getSelectionModel().select(lesson.getType());
            contentArea.setText(lesson.getContent() == null ? "" : lesson.getContent());
            selectedFileLabel.setText(lesson.getFilePath() == null ? "No file selected" : Path.of(lesson.getFilePath()).getFileName().toString());
            updateTypeUI(lesson.getType());
        }
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        String type = typeComboBox.getValue();
        if ("PDF".equals(type)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mov", "*.m4v", "*.mkv", "*.webm"));
        }
        selectedFile = chooser.showOpenDialog(titleField.getScene().getWindow());
        selectedFileLabel.setText(selectedFile == null ? "No file selected" : selectedFile.getName());
    }

    @FXML
    private void handleSave() {
        if (lesson == null) {
            showError("Lesson context is missing.", new IllegalStateException("No lesson was provided."));
            return;
        }
        lesson.setTitle(titleField.getText().trim());
        lesson.setType(typeComboBox.getValue());
        lesson.setPosition(parseInteger(positionField.getText()));
        lesson.setUpdatedAt(LocalDateTime.now());

        try {
            applyTypeSpecificData();
            getLessonService().update(lesson);
            if (shellController != null && course != null && section != null) {
                shellController.showLessonShow(course, section, lesson);
            }
        } catch (Exception e) {
            showError("Unable to update the lesson.", e);
        }
    }

    @FXML
    private void handleCancel() {
        if (shellController != null && course != null && section != null && lesson != null) {
            shellController.showLessonShow(course, section, lesson);
        }
    }

    private void updateTypeUI(String type) {
        boolean textType = "TEXT".equals(type);
        contentBox.setManaged(textType);
        contentBox.setVisible(textType);
        fileBox.setManaged(!textType);
        fileBox.setVisible(!textType);
        if (textType) {
            selectedFile = null;
            selectedFileLabel.setText("No file selected");
            lesson.setFilePath(null);
        } else if (selectedFile == null && (lesson == null || lesson.getFilePath() == null || lesson.getFilePath().isBlank())) {
            selectedFileLabel.setText("No file selected");
        }
    }

    private void applyTypeSpecificData() throws IOException {
        if ("TEXT".equals(lesson.getType())) {
            lesson.setContent(contentArea.getText().trim());
            lesson.setFilePath(null);
            return;
        }

        lesson.setContent(null);
        if (selectedFile != null) {
            lesson.setFilePath(storeLessonFile(selectedFile));
        } else if (lesson.getFilePath() == null || lesson.getFilePath().isBlank()) {
            throw new IllegalStateException("Please choose a file.");
        }
    }

    private String storeLessonFile(File file) throws IOException {
        Path uploadDir = Path.of("uploads", "lessons");
        Files.createDirectories(uploadDir);
        String extension = extensionOf(file.getName());
        String fileName = UUID.randomUUID() + extension;
        Path destination = uploadDir.resolve(fileName);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return uploadDir.resolve(fileName).toString().replace("\\", "/");
    }

    private String extensionOf(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex >= 0 ? name.substring(dotIndex) : "";
    }

    private Integer parseInteger(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : Integer.parseInt(trimmed);
    }

    private LessonService getLessonService() {
        if (lessonService == null) {
            lessonService = new LessonService();
        }
        return lessonService;
    }

    private void showError(String message, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
