package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.util.List;

public class PostOverviewController {

    private static final int PREVIEW_LIMIT = 180;

    @FXML
    private VBox postsContainer;

    @FXML
    private Label currentUserLabel;

    private ForumCrudLauncher application;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentUserLabel.setText("Current user: " + application.getCurrentUserDisplay());
    }

    public void loadPosts() {
        postsContainer.getChildren().clear();

        try {
            List<Post> posts = application.getPostService().getAll();
            if (posts.isEmpty()) {
                postsContainer.getChildren().add(createEmptyState("No posts found in the database."));
                return;
            }

            for (Post post : posts) {
                postsContainer.getChildren().add(createPostCard(post));
            }
        } catch (SQLException exception) {
            postsContainer.getChildren().add(createEmptyState("Unable to load posts."));
            application.showError("Loading posts failed", exception.getMessage());
        }
    }

    @FXML
    private void handleCreatePost() {
        application.showCreatePostScene();
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #d7deea; -fx-border-radius: 14;");

        Label titleLabel = new Label(valueOrFallback(post.getTitle(), "Untitled post"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setWrapText(true);

        Label userLabel = new Label("User: " + application.resolveUsername(post.getUserId()));
        Label previewLabel = new Label(truncate(post.getContent(), PREVIEW_LIMIT));
        previewLabel.setWrapText(true);

        Button openButton = new Button("Open Post");
        openButton.setOnAction(event -> application.showPostDetailsScene(post.getId()));

        card.setOnMouseClicked(event -> application.showPostDetailsScene(post.getId()));
        card.getChildren().addAll(titleLabel, userLabel, previewLabel, openButton);
        return card;
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #52627a; -fx-font-size: 15;");
        label.setWrapText(true);
        return label;
    }

    private String truncate(String value, int maxLength) {
        String safeValue = valueOrFallback(value, "");
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength).trim() + "...";
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
