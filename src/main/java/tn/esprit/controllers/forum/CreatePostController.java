package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
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

    private ForumCrudLauncher application;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
    }

    @FXML
    private void handleBack() {
        application.showOverviewScene();
    }

    @FXML
    private void handleSavePost() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        if (title.isEmpty()) {
            application.showError("Invalid post", "Title is required.");
            return;
        }

        if (content.isEmpty()) {
            application.showError("Invalid post", "Content is required.");
            return;
        }

        try {
            Post post = new Post();
            LocalDateTime now = LocalDateTime.now();

            post.setType(typeField.getText().trim());
            post.setTitle(title);
            post.setTopic(topicField.getText().trim());
            post.setContent(content);
            post.setCreatedAt(now);
            post.setUpdatedAt(now);
            post.setUserId(null);

            application.getPostService().add(post);
            application.showInfo("Post created", "Post #" + post.getId() + " was created successfully.");
            application.showOverviewScene();
        } catch (SQLException exception) {
            application.showError("Saving post failed", exception.getMessage());
        }
    }
}
