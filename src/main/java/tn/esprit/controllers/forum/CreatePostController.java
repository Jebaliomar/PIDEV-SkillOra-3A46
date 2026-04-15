package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class CreatePostController {

    @FXML private TextField typeField;
    @FXML private TextField titleField;
    @FXML private TextField topicField;
    @FXML private TextArea contentArea;
    @FXML private Label currentUserLabel;
    @FXML private Label postErrorLabel;
    @FXML private Label screenTitleLabel;
    @FXML private Label screenSubtitleLabel;
    @FXML private Button savePostButton;

    private ForumCrudLauncher application;
    private Post editingPost;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentUserLabel.setText("Posting as: " + application.getCurrentUserDisplay());
        clearValidationError();
        configureCreateMode();
    }

    public boolean loadPostForEdit(int postId) {
        try {
            Post post = application.getPostService().getById(postId);
            if (post == null) {
                application.showError("Post not found", "The selected post does not exist anymore.");
                application.showOverviewScene();
                return false;
            }

            if (!isCurrentUserPostOwner(post)) {
                application.showError("Access denied", "You can only edit your own posts.");
                application.showPostDetailsScene(postId);
                return false;
            }

            editingPost = post;
            typeField.setText(valueOrEmpty(post.getType()));
            titleField.setText(valueOrEmpty(post.getTitle()));
            topicField.setText(valueOrEmpty(post.getTopic()));
            contentArea.setText(valueOrEmpty(post.getContent()));
            configureEditMode();
            return true;
        } catch (SQLException exception) {
            application.showError("Loading post failed", exception.getMessage());
            application.showOverviewScene();
            return false;
        }
    }

    @FXML
    private void handleBack() {
        if (editingPost != null) {
            application.showPostDetailsScene(editingPost.getId());
            return;
        }
        application.showOverviewScene();
    }

    @FXML
    private void handleSavePost() {
        clearValidationError();

        try {
            LocalDateTime now = LocalDateTime.now();
            Post post = editingPost == null ? new Post() : editingPost;

            post.setType(typeField.getText().trim());
            post.setTitle(titleField.getText().trim());
            post.setTopic(topicField.getText().trim());
            post.setContent(contentArea.getText().trim());
            post.setUpdatedAt(now);

            if (editingPost == null) {
                post.setCreatedAt(now);
                post.setUserId(application.getCurrentUser().getId());
                application.getPostService().add(post);
                application.showInfo("Post created", "Post #" + post.getId() + " was created successfully.");
                application.showOverviewScene();
                return;
            }

            application.getPostService().update(post);
            application.showInfo("Post updated", "Post #" + post.getId() + " was updated successfully.");
            application.showPostDetailsScene(post.getId());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showValidationError(exception.getMessage());
        } catch (SQLException exception) {
            application.showError(editingPost == null ? "Saving post failed" : "Updating post failed", exception.getMessage());
        }
    }

    private void configureCreateMode() {
        screenTitleLabel.setText("Create Post");
        screenSubtitleLabel.setText("Share your thoughts with the community.");
        savePostButton.setText("Publish Post");
    }

    private void configureEditMode() {
        screenTitleLabel.setText("Edit Post");
        screenSubtitleLabel.setText("Update your post and save the latest version.");
        savePostButton.setText("Save Changes");
    }

    private boolean isCurrentUserPostOwner(Post post) {
        return post != null
                && post.getUserId() != null
                && application.getCurrentUser() != null
                && post.getUserId().equals(application.getCurrentUser().getId());
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
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
}
