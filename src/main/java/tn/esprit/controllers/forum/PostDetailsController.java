package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PostDetailsController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    private Label titleValueLabel;

    @FXML
    private Label idValueLabel;

    @FXML
    private Label typeValueLabel;

    @FXML
    private Label topicValueLabel;

    @FXML
    private Label contentValueLabel;

    @FXML
    private Label userValueLabel;

    @FXML
    private Label createdAtValueLabel;

    @FXML
    private Label updatedAtValueLabel;

    @FXML
    private VBox repliesContainer;

    @FXML
    private Label currentReplyUserLabel;

    @FXML
    private TextArea replyContentArea;

    private ForumCrudLauncher application;
    private Post post;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentReplyUserLabel.setText(application.getCurrentUserDisplay());
    }

    public void loadPost(int postId) {
        try {
            post = application.getPostService().getById(postId);
            if (post == null) {
                application.showError("Post not found", "The selected post does not exist anymore.");
                application.showOverviewScene();
                return;
            }

            populatePostDetails();
            loadReplies();
        } catch (SQLException exception) {
            application.showError("Loading post failed", exception.getMessage());
            application.showOverviewScene();
        }
    }

    @FXML
    private void handleBack() {
        application.showOverviewScene();
    }

    @FXML
    private void handleAddReply() {
        if (post == null) {
            return;
        }

        String replyContent = replyContentArea.getText().trim();
        if (replyContent.isEmpty()) {
            application.showError("Invalid reply", "Reply content is required.");
            return;
        }

        try {
            Reply reply = new Reply();
            LocalDateTime now = LocalDateTime.now();

            reply.setPostId(post.getId());
            reply.setParentId(null);
            reply.setAuthorName(application.getCurrentUser().getUsername());
            reply.setContent(replyContent);
            reply.setUpvotes(0);
            reply.setCreatedAt(now);
            reply.setUpdatedAt(now);
            reply.setUserId(application.getCurrentUser().getId());

            application.getReplyService().add(reply);

            replyContentArea.clear();
            loadReplies();
            application.showInfo("Reply added", "Your reply was added successfully.");
        } catch (SQLException exception) {
            application.showError("Saving reply failed", exception.getMessage());
        }
    }

    private void populatePostDetails() {
        titleValueLabel.setText(valueOrFallback(post.getTitle(), "Untitled post"));
        idValueLabel.setText(String.valueOf(post.getId()));
        typeValueLabel.setText(valueOrFallback(post.getType(), "-"));
        topicValueLabel.setText(valueOrFallback(post.getTopic(), "-"));
        contentValueLabel.setText(valueOrFallback(post.getContent(), "-"));

        String userDisplay = post.getUserId() == null
                ? "Unknown user"
                : application.resolveUsername(post.getUserId()) + " (ID: " + post.getUserId() + ")";
        userValueLabel.setText(userDisplay);

        createdAtValueLabel.setText(formatDateTime(post.getCreatedAt()));
        updatedAtValueLabel.setText(formatDateTime(post.getUpdatedAt()));
    }

    private void loadReplies() throws SQLException {
        repliesContainer.getChildren().clear();

        List<Reply> replies = application.getReplyService().getByPostId(post.getId());
        if (replies.isEmpty()) {
            repliesContainer.getChildren().add(createEmptyState("No replies yet for this post."));
            return;
        }

        for (Reply reply : replies) {
            repliesContainer.getChildren().add(createReplyCard(reply));
        }
    }

    private VBox createReplyCard(Reply reply) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #d7deea; -fx-border-radius: 12;");

        Label authorLabel = new Label("User: " + valueOrFallback(reply.getAuthorName(), "Anonymous"));
        authorLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        Label contentLabel = new Label(valueOrFallback(reply.getContent(), "-"));
        contentLabel.setWrapText(true);

        card.getChildren().addAll(authorLabel, contentLabel);
        return card;
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #52627a; -fx-font-size: 15;");
        label.setWrapText(true);
        return label;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMATTER.format(dateTime);
    }

    private String valueOrFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
