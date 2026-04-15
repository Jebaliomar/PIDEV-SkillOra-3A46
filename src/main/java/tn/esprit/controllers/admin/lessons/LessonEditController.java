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

    @FXML
    private Label titleErrorLabel;

    @FXML
    private Label typeErrorLabel;

    @FXML
    private Label positionErrorLabel;

    @FXML
    private Label contentErrorLabel;

    @FXML
    private Label fileErrorLabel;

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
        if (!validateForm()) {
            return;
        }
        lesson.setTitle(titleField.getText().trim());
        lesson.setType(typeComboBox.getValue());
        lesson.setPosition(Integer.parseInt(positionField.getText().trim()));
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
            if (lesson != null) {
                lesson.setFilePath(null);
            }
            clearError(fileErrorLabel);
        } else if (selectedFile == null && (lesson == null || lesson.getFilePath() == null || lesson.getFilePath().isBlank())) {
            selectedFileLabel.setText("No file selected");
            clearError(contentErrorLabel);
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

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;

        String title = titleField.getText() == null ? "" : titleField.getText().trim();
        String type = typeComboBox.getValue();
        String position = positionField.getText() == null ? "" : positionField.getText().trim();

        if (title.isEmpty()) {
            setError(titleErrorLabel, "Title is required.");
            valid = false;
        }

        if (type == null || type.isBlank()) {
            setError(typeErrorLabel, "Type is required.");
            valid = false;
        }

        if (position.isEmpty()) {
            setError(positionErrorLabel, "Position is required.");
            valid = false;
        } else {
            try {
                if (Integer.parseInt(position) <= 0) {
                    setError(positionErrorLabel, "Position must be a positive number.");
                    valid = false;
                }
            } catch (NumberFormatException e) {
                setError(positionErrorLabel, "Position must be a positive number.");
                valid = false;
            }
        }

        if ("TEXT".equals(type)) {
            if (contentArea.getText() == null || contentArea.getText().trim().isEmpty()) {
                setError(contentErrorLabel, "Content is required for text lessons.");
                valid = false;
            }
        } else if ("PDF".equals(type)) {
            if (selectedFile == null && (lesson == null || lesson.getFilePath() == null || lesson.getFilePath().isBlank())) {
                setError(fileErrorLabel, "Please choose a PDF file.");
                valid = false;
            } else if (selectedFile != null && !isPdfFile(selectedFile)) {
                setError(fileErrorLabel, "Selected file must be a PDF.");
                valid = false;
            } else if (selectedFile == null && lesson != null && !isPdfPath(lesson.getFilePath())) {
                setError(fileErrorLabel, "Current lesson file is not a PDF.");
                valid = false;
            }
        } else if ("VIDEO".equals(type)) {
            if (selectedFile == null && (lesson == null || lesson.getFilePath() == null || lesson.getFilePath().isBlank())) {
                setError(fileErrorLabel, "Please choose a video file.");
                valid = false;
            } else if (selectedFile != null && !isVideoFile(selectedFile.getName())) {
                setError(fileErrorLabel, "Selected file must be a supported video.");
                valid = false;
            } else if (selectedFile == null && lesson != null && !isVideoFile(lesson.getFilePath())) {
                setError(fileErrorLabel, "Current lesson file is not a supported video.");
                valid = false;
            }
        }

        return valid;
    }

    private boolean isPdfFile(File file) {
        return file != null && isPdfPath(file.getName());
    }

    private boolean isPdfPath(String path) {
        return path != null && path.toLowerCase().endsWith(".pdf");
    }

    private boolean isVideoFile(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v")
                || lower.endsWith(".mkv") || lower.endsWith(".webm");
    }

    private void clearErrors() {
        clearError(titleErrorLabel);
        clearError(typeErrorLabel);
        clearError(positionErrorLabel);
        clearError(contentErrorLabel);
        clearError(fileErrorLabel);
    }

    private void setError(Label label, String message) {
        label.setText(message);
        label.setManaged(true);
        label.setVisible(true);
    }

    private void clearError(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
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
