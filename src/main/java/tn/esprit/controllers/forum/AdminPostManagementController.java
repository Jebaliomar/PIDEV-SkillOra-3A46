package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminPostManagementController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PREVIEW_LIMIT = 220;

    @FXML private VBox postsContainer;
    @FXML private Label currentUserLabel;

    private ForumCrudLauncher application;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentUserLabel.setText("Moderator: " + application.getCurrentUserDisplay());
    }

    public void loadPosts() {
        postsContainer.getChildren().clear();

        try {
            List<Post> posts = application.getPostService().getAll();
            if (posts.isEmpty()) {
                postsContainer.getChildren().add(createEmptyState("No posts are available to moderate."));
                return;
            }

            for (Post post : posts) {
                postsContainer.getChildren().add(createPostCard(post));
            }
        } catch (SQLException exception) {
            postsContainer.getChildren().add(createEmptyState("Unable to load posts."));
            application.showError("Loading admin posts failed", exception.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        application.showOverviewScene();
    }

    @FXML
    private void handleRefresh() {
        loadPosts();
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(12);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPadding(new Insets(20));
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;"
        );

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        Label titleLabel = new Label(valueOrFallback(post.getTitle(), "Untitled post"));
        titleLabel.setWrapText(true);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 17));
        titleLabel.setStyle("-fx-text-fill: #0f172a;");

        Label metadataLabel = new Label(
                "Post #" + post.getId()
                        + " | " + valueOrFallback(post.getType(), "-")
                        + " | " + valueOrFallback(post.getTopic(), "-")
                        + " | by " + application.resolveUsername(post.getUserId())
        );
        metadataLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        titleBox.getChildren().addAll(titleLabel, metadataLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        Button openButton = new Button("Open");
        openButton.setOnAction(event -> application.showPostDetailsScene(post.getId()));
        openButton.setStyle(
                "-fx-background-color: #eff6ff;" +
                        "-fx-text-fill: #1d4ed8;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> handleDeletePost(post));
        deleteButton.setStyle(
                "-fx-background-color: #fef2f2;" +
                        "-fx-text-fill: #dc2626;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        actions.getChildren().addAll(openButton, deleteButton);

        titleRow.getChildren().addAll(titleBox, spacer, actions);

        Label previewLabel = new Label(truncate(post.getContent(), PREVIEW_LIMIT));
        previewLabel.setWrapText(true);
        previewLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");

        Label dateLabel = new Label(
                "Created: " + formatDate(post.getCreatedAt())
                        + "    Updated: " + formatDate(post.getUpdatedAt())
        );
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        card.getChildren().addAll(titleRow, previewLabel, dateLabel);
        return card;
    }

    private void handleDeletePost(Post post) {
        boolean confirmed = application.confirmAction(
                "Delete post",
                "Delete post #" + post.getId() + " from the admin moderation screen?"
        );
        if (!confirmed) {
            return;
        }

        try {
            application.getPostService().delete(post.getId());
            loadPosts();
            application.showInfo("Post deleted", "The post was deleted successfully.");
        } catch (SQLException exception) {
            application.showError("Deleting post failed", exception.getMessage());
        }
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle(
                "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 40 0 40 0;"
        );
        return label;
    }

    private String truncate(String value, int maxLength) {
        String text = valueOrFallback(value, "");
        return text.length() <= maxLength ? text : text.substring(0, maxLength).trim() + "...";
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMATTER.format(dateTime);
    }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
