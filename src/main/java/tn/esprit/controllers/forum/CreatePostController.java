package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;
import tn.esprit.services.CloudinaryService;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CreatePostController {

    // ── FXML bindings ─────────────────────────────────────────────────────────
    @FXML private Button    typeQuestionBtn;
    @FXML private Button    typeDiscussionBtn;
    @FXML private Button    typeArticleBtn;
    @FXML private Button    uploadImageButton;      // add in FXML
    @FXML private Label     imageStatusLabel;        // add in FXML
    @FXML private TextField typeField;               // hidden, populated by tile selection
    @FXML private TextField titleField;
    @FXML private TextField topicField;
    @FXML private TextArea  contentArea;
    @FXML private Label     currentUserLabel;
    @FXML private Label     postErrorLabel;
    @FXML private Label     screenTitleLabel;
    @FXML private Label     screenSubtitleLabel;
    @FXML private Label     titleCharCountLabel;
    @FXML private Label     contentCharCountLabel;
    @FXML private Button    savePostButton;

    // ── State ─────────────────────────────────────────────────────────────────
    private ForumCrudLauncher application;
    private Post editingPost;
    private String selectedType = "Question";
    private String uploadedImageUrl;
    private String uploadedImageName;

    private final CloudinaryService cloudinaryService = new CloudinaryService();

    // Styles
    private static final String TILE_SELECTED =
            "-fx-background-color: rgba(13,148,136,0.08); -fx-background-radius: 12;"
            + "-fx-cursor: hand; -fx-border-color: #0d9488;"
            + "-fx-border-radius: 12; -fx-border-width: 2; -fx-padding: 0;";
    private static final String TILE_UNSELECTED =
            "-fx-background-color: white; -fx-background-radius: 12;"
            + "-fx-cursor: hand; -fx-border-color: rgba(226,232,240,0.9);"
            + "-fx-border-radius: 12; -fx-border-width: 1.5; -fx-padding: 0;";

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        updateSessionLabel();
        clearValidationError();
        configureCreateMode();
        attachCharCountListeners();
        updateImageStatusLabel();
    }

    public boolean loadPostForEdit(int postId) {
        if (application.getCurrentUser() == null) {
            application.showError("Access denied", "No active user session.");
            return false;
        }
        try {
            Post post = application.getPostService().getById(postId);
            if (post == null) {
                application.showError("Post not found", "The selected post no longer exists.");
                application.showOverviewScene();
                return false;
            }
            if (!isCurrentUserPostOwner(post)) {
                application.showError("Access denied", "You can only edit your own posts.");
                application.showPostDetailsScene(postId);
                return false;
            }

            editingPost = post;

            selectedType = valueOrEmpty(post.getType());
            applyTypeSelection(selectedType);

            titleField.setText(valueOrEmpty(post.getTitle()));
            topicField.setText(valueOrEmpty(post.getTopic()));
            contentArea.setText(valueOrEmpty(post.getContent()));

            // If your Post entity already has imageUrl, preserve it while editing.
            uploadedImageUrl = valueOrEmpty(post.getImageUrl());
            uploadedImageName = uploadedImageUrl.isBlank() ? null : extractFileName(uploadedImageUrl);

            configureEditMode();
            updateImageStatusLabel();
            return true;
        } catch (SQLException e) {
            application.showError("Loading post failed", e.getMessage());
            application.showOverviewScene();
            return false;
        }
    }

    // ── Type tile handlers ────────────────────────────────────────────────────

    @FXML private void handleTypeQuestionSelect()   { applyTypeSelection("Question"); }
    @FXML private void handleTypeDiscussionSelect() { applyTypeSelection("Discussion"); }
    @FXML private void handleTypeArticleSelect()    { applyTypeSelection("Article"); }

    private void applyTypeSelection(String type) {
        selectedType = type;
        typeField.setText(type);
        typeQuestionBtn.setStyle("Question".equals(type) ? TILE_SELECTED : TILE_UNSELECTED);
        typeDiscussionBtn.setStyle("Discussion".equals(type) ? TILE_SELECTED : TILE_UNSELECTED);
        typeArticleBtn.setStyle("Article".equals(type) ? TILE_SELECTED : TILE_UNSELECTED);
    }

    // ── Image upload ──────────────────────────────────────────────────────────

    @FXML
    private void handleUploadImage() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select an image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
            );

            Window owner = currentUserLabel != null && currentUserLabel.getScene() != null
                    ? currentUserLabel.getScene().getWindow()
                    : null;

            File file = fileChooser.showOpenDialog(owner);
            if (file == null) {
                return;
            }

            String imageUrl = cloudinaryService.uploadImage(file);
            uploadedImageUrl = imageUrl;
            uploadedImageName = file.getName();

            updateImageStatusLabel();
            application.showInfo("Image uploaded", "The image was uploaded successfully.");
        } catch (Exception e) {
            application.showError("Image upload failed", e.getMessage());
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        if (editingPost != null) application.showPostDetailsScene(editingPost.getId());
        else                     application.showOverviewScene();
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    @FXML
    private void handleSavePost() {
        clearValidationError();
        if (application.getCurrentUser() == null) {
            showValidationError("No active user session. Please restart the application.");
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            Post post = editingPost == null ? new Post() : editingPost;

            post.setType(selectedType.isBlank() ? typeField.getText() : selectedType);
            post.setTitle(titleField.getText());
            post.setTopic(topicField.getText());
            post.setContent(contentArea.getText());

            // Cloudinary image URL
            if (uploadedImageUrl != null && !uploadedImageUrl.isBlank()) {
                post.setImageUrl(uploadedImageUrl);
            } else if (editingPost != null) {
                post.setImageUrl(valueOrEmpty(editingPost.getImageUrl()));
            }

            if (editingPost == null) {
                post.setCreatedAt(now);
                post.setUpdatedAt(now);
                post.setUserId(application.getCurrentUser().getId());

                application.getPostService().add(post);
                application.showInfo("Post created", "Post #" + post.getId() + " was published.");
                application.showOverviewScene();
            } else {
                post.setUpdatedAt(now);
                application.getPostService().update(post);
                application.showInfo("Post updated", "Post #" + post.getId() + " was updated.");
                application.showPostDetailsScene(post.getId());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            showValidationError(e.getMessage());
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("user account") || msg.contains("foreign key"))) {
                showValidationError("Your account could not be linked to this post.");
            } else {
                application.showError(editingPost == null ? "Saving failed" : "Update failed", msg);
            }
        }
    }

    // ── Mode configuration ────────────────────────────────────────────────────

    private void configureCreateMode() {
        screenTitleLabel.setText("Create New Post");
        screenSubtitleLabel.setText("Share your knowledge or ask the community");
        savePostButton.setText("Publish Post");
        savePostButton.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 700;"
                + "-fx-font-size: 14px; -fx-background-radius: 10; -fx-border-color: transparent;"
                + "-fx-cursor: hand; -fx-padding: 12 32 12 32;"
                + "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.28), 14, 0.12, 0, 4);");
        editingPost = null;
        uploadedImageUrl = null;
        uploadedImageName = null;
        applyTypeSelection("Question");
        updateImageStatusLabel();
    }

    private void configureEditMode() {
        screenTitleLabel.setText("Edit Post");
        screenSubtitleLabel.setText("Update your post and save the latest version.");
        savePostButton.setText("Save Changes");
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void attachCharCountListeners() {
        if (titleCharCountLabel != null) {
            titleField.textProperty().addListener((obs, o, n) ->
                    titleCharCountLabel.setText(n.length() + "/255"));
        }
        if (contentCharCountLabel != null) {
            contentArea.textProperty().addListener((obs, o, n) ->
                    contentCharCountLabel.setText(n.length() + " characters"));
        }
    }

    private boolean isCurrentUserPostOwner(Post post) {
        return post != null && post.getUserId() != null
                && application.getCurrentUser() != null
                && post.getUserId().equals(application.getCurrentUser().getId());
    }

    private void updateSessionLabel() {
        if (application.getCurrentUser() == null) {
            currentUserLabel.setText("No active session — please restart.");
            currentUserLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600; -fx-font-size: 11px;");
            savePostButton.setDisable(true);
        } else {
            currentUserLabel.setText("Posting as: " + application.getCurrentUserDisplay());
            savePostButton.setDisable(false);
        }
    }

    private void updateImageStatusLabel() {
        if (imageStatusLabel == null) {
            return;
        }

        if (uploadedImageUrl == null || uploadedImageUrl.isBlank()) {
            imageStatusLabel.setText("No image selected");
            imageStatusLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        } else {
            String label = uploadedImageName != null ? uploadedImageName : "Image attached";
            imageStatusLabel.setText("Selected: " + label);
            imageStatusLabel.setStyle("-fx-text-fill: #059669; -fx-font-size: 12px; -fx-font-weight: 700;");
        }
    }

    private void showValidationError(String message) {
        postErrorLabel.setText(message);
        postErrorLabel.setManaged(true);
        postErrorLabel.setVisible(true);
    }

    private void clearValidationError() {
        postErrorLabel.setText("");
        postErrorLabel.setManaged(false);
        postErrorLabel.setVisible(false);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private String extractFileName(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        int slash = url.lastIndexOf('/');
        return slash >= 0 && slash < url.length() - 1 ? url.substring(slash + 1) : url;
    }
}