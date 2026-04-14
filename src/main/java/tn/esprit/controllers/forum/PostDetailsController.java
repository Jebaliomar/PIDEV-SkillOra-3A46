package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
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

    @FXML private Label titleValueLabel;
    @FXML private Label idValueLabel;
    @FXML private Label typeValueLabel;
    @FXML private Label topicValueLabel;
    @FXML private Label contentValueLabel;
    @FXML private Label userValueLabel;
    @FXML private Label createdAtValueLabel;
    @FXML private Label updatedAtValueLabel;
    @FXML private VBox repliesContainer;
    @FXML private Label currentReplyUserLabel;
    @FXML private TextArea replyContentArea;
    @FXML private Label replyErrorLabel;

    private ForumCrudLauncher application;
    private Post post;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentReplyUserLabel.setText(application.getCurrentUserDisplay());
        clearReplyError();
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
        if (post == null) return;
        clearReplyError();

        try {
            Reply reply = new Reply();
            LocalDateTime now = LocalDateTime.now();

            reply.setPostId(post.getId());
            reply.setParentId(null);
            reply.setAuthorName(application.getCurrentUser().getUsername());
            reply.setContent(replyContentArea.getText().trim());
            reply.setUpvotes(0);
            reply.setCreatedAt(now);
            reply.setUpdatedAt(now);
            reply.setUserId(application.getCurrentUser().getId());

            application.getReplyService().add(reply);

            replyContentArea.clear();
            loadReplies();
            application.showInfo("Reply added", "Your reply was added successfully.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showReplyError(exception.getMessage());
        } catch (SQLException exception) {
            application.showError("Saving reply failed", exception.getMessage());
        }
    }

    private void populatePostDetails() {
        titleValueLabel.setText(valueOrFallback(post.getTitle(), "Untitled post"));
        idValueLabel.setText(String.valueOf(post.getId()));

        // Style type as a badge via label text — the FXML label already has badge styling
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
        VBox card = new VBox(0);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        // ── Header ───────────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 12, 16));
        header.setStyle(
                "-fx-background-color: #f8faff;" +
                        "-fx-background-radius: 12 12 0 0;"
        );

        // Avatar circle
        String authorName = valueOrFallback(reply.getAuthorName(), "Anonymous");
        String initial = authorName.substring(0, 1).toUpperCase();

        Label avatar = new Label(initial);
        avatar.setStyle(
                "-fx-background-color: #ede9fe;" +
                        "-fx-text-fill: #6366f1;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 12px;" +
                        "-fx-background-radius: 50;" +
                        "-fx-min-width: 30; -fx-max-width: 30;" +
                        "-fx-min-height: 30; -fx-max-height: 30;" +
                        "-fx-alignment: center;"
        );

        Label authorLabel = new Label(authorName);
        authorLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        authorLabel.setStyle("-fx-text-fill: #1e293b;");

        // Date if available
        if (reply.getCreatedAt() != null) {
            Label dateLabel = new Label("· " + DATE_FORMATTER.format(reply.getCreatedAt()));
            dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            header.getChildren().addAll(avatar, authorLabel, dateLabel);
        } else {
            header.getChildren().addAll(avatar, authorLabel);
        }

        // ── Divider ──────────────────────────────────────────
        javafx.scene.layout.Region divider = new javafx.scene.layout.Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: #e2e8f0;");

        // ── Content ──────────────────────────────────────────
        Label contentLabel = new Label(valueOrFallback(reply.getContent(), "-"));
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 14px; -fx-line-spacing: 2;");
        VBox contentBox = new VBox(contentLabel);
        contentBox.setPadding(new Insets(14, 16, 16, 16));

        card.getChildren().addAll(header, divider, contentBox);
        return card;
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.setStyle(
                "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 16 0 8 0;"
        );
        label.setWrapText(true);
        return label;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMATTER.format(dateTime);
    }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private void showReplyError(String message) {
        replyErrorLabel.setText(message);
        replyErrorLabel.setManaged(true);
        replyErrorLabel.setVisible(true);
    }

    private void clearReplyError() {
        replyErrorLabel.setText("");
        replyErrorLabel.setManaged(false);
        replyErrorLabel.setVisible(false);
    }
}
