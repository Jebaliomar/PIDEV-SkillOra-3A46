package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class CreatePostController {

    @FXML
    private TextField typeField;

    @FXML
    private TextField titleField;

    @FXML
    private TextField topicField;

    @FXML
    private TextArea contentArea;

    @FXML
    private Label currentUserLabel;

    @FXML
    private Label postErrorLabel;

    private ForumCrudLauncher application;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentUserLabel.setText("Posting as: " + application.getCurrentUserDisplay());
        clearValidationError();
    }

    @FXML
    private void handleBack() {
        application.showOverviewScene();
    }

    @FXML
    private void handleSavePost() {
        clearValidationError();

        try {
            Post post = new Post();
            LocalDateTime now = LocalDateTime.now();

            post.setType(typeField.getText().trim());
            post.setTitle(titleField.getText().trim());
            post.setTopic(topicField.getText().trim());
            post.setContent(contentArea.getText().trim());
            post.setCreatedAt(now);
            post.setUpdatedAt(now);
            post.setUserId(application.getCurrentUser().getId());

            application.getPostService().add(post);
            application.showInfo("Post created", "Post #" + post.getId() + " was created successfully.");
            application.showOverviewScene();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showValidationError(exception.getMessage());
        } catch (SQLException exception) {
            application.showError("Saving post failed", exception.getMessage());
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
}
