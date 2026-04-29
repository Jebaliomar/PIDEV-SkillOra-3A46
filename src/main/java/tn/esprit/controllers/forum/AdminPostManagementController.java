package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminPostManagementController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm");
    private static final int PREVIEW_LIMIT = 170;

    @FXML private VBox postsContainer;
    @FXML private Label currentUserLabel;
    @FXML private Label postCountLabel;
    @FXML private Label totalPostsLabel;
    @FXML private Label flaggedPostsLabel;
    @FXML private Label activePostsLabel;
    @FXML private Label pageInfoLabel;

    private ForumCrudLauncher application;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        if (currentUserLabel != null) {
            currentUserLabel.setText("Moderator: " + application.getCurrentUserDisplay());
        }
        refreshStats();
    }

    public void loadPosts() {
        if (postsContainer == null) {
            return;
        }

        postsContainer.getChildren().clear();
        try {
            List<Post> posts = application.getPostService().getAll();
            if (postCountLabel != null) {
                postCountLabel.setText(posts.size() + (posts.size() == 1 ? " post" : " posts"));
            }

            if (posts.isEmpty()) {
                postsContainer.getChildren().add(emptyStateLabel("No posts available to moderate."));
                return;
            }

            for (Post post : posts) {
                postsContainer.getChildren().add(createPostCard(post));
            }
        } catch (SQLException e) {
            postsContainer.getChildren().add(emptyStateLabel("Unable to load posts."));
            application.showError("Loading admin posts failed", e.getMessage());
        }

        refreshStats();
    }

    @FXML
    private void handleBack() {
        application.showOverviewScene();
    }

    @FXML
    private void handleRefresh() {
        loadPosts();
    }

    @FXML
    private void handleOpenReports() {
        application.showReportManagementScene();
    }

    private Node createPostCard(Post post) {
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-border-color: #e5eaf2;" +
                "-fx-border-radius: 18;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 18, 0.08, 0, 6);"
        );

        HBox content = new HBox(14);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(16, 18, 16, 18));

        VBox avatar = avatarBubble(initialOf(application.resolveUsername(post.getUserId())));

        VBox main = new VBox(8);
        main.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(main, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(valueOrFallback(post.getTitle(), "Untitled post"));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 15px; -fx-font-weight: 900;");
        HBox.setHgrow(title, Priority.ALWAYS);

        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);
        if (post.getType() != null && !post.getType().isBlank()) {
            chips.getChildren().add(chip(post.getType(), "#dbeafe", "#1d4ed8"));
        }
        if (post.getTopic() != null && !post.getTopic().isBlank()) {
            chips.getChildren().add(chip(post.getTopic(), "#ecfccb", "#3f6212"));
        }

        topRow.getChildren().addAll(title, chips);

        Label preview = new Label(truncate(valueOrFallback(post.getContent(), ""), PREVIEW_LIMIT));
        preview.setWrapText(true);
        preview.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.8px; -fx-line-spacing: 3;");

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label author = new Label("By " + application.resolveUsername(post.getUserId()));
        author.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: 700;");

        Label date = new Label(formatDate(post.getCreatedAt()));
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button openBtn = actionButton("Open", "#eff6ff", "#1d4ed8");
        openBtn.setOnAction(e -> application.showPostDetailsScene(post.getId()));

        Button deleteBtn = actionButton("Delete", "#fef2f2", "#dc2626");
        deleteBtn.setOnAction(e -> handleDeletePost(post));

        actions.getChildren().addAll(openBtn, deleteBtn);
        metaRow.getChildren().addAll(author, dot(), date, spacer, actions);

        main.getChildren().addAll(topRow, preview, metaRow);
        content.getChildren().addAll(avatar, main);

        card.getChildren().add(content);
        return card;
    }

    private VBox avatarBubble(String letter) {
        Label avatarText = new Label(letter);
        avatarText.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 900;");

        VBox avatar = new VBox(avatarText);
        avatar.setAlignment(Pos.CENTER);
        avatar.setMinSize(42, 42);
        avatar.setPrefSize(42, 42);
        avatar.setMaxSize(42, 42);
        avatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2563eb, #1d4ed8);" +
                "-fx-background-radius: 999;"
        );
        return avatar;
    }

    private Label chip(String text, String bg, String fg) {
        Label chip = new Label(text);
        chip.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-size: 11px;" +
                "-fx-font-weight: 800;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 5 10 5 10;"
        );
        return chip;
    }

    private Label dot() {
        Label dot = new Label("•");
        dot.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-font-weight: 900;");
        return dot;
    }

    private Button actionButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 14 8 14;"
        );
        return btn;
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
        } catch (SQLException e) {
            application.showError("Deleting post failed", e.getMessage());
        }
    }

    private void refreshStats() {
        try {
            int totalPosts = application.getPostService().getAll().size();
            int flaggedPosts = application.getReportService().countByStatus("pending");
            int activePosts = application.getPostService().countPostsWithReplies();
            int totalPages = Math.max(1, (int) Math.ceil(totalPosts / 10.0));

            if (totalPostsLabel != null) {
                totalPostsLabel.setText(String.valueOf(totalPosts));
            }
            if (flaggedPostsLabel != null) {
                flaggedPostsLabel.setText(String.valueOf(flaggedPosts));
            }
            if (activePostsLabel != null) {
                activePostsLabel.setText(String.valueOf(activePosts));
            }
            if (pageInfoLabel != null) {
                pageInfoLabel.setText("1/" + totalPages);
            }
        } catch (SQLException e) {
            // Keep the screen usable even if stats fail.
        }
    }

    private Label emptyStateLabel(String message) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-padding: 40 0 40 0;");
        return lbl;
    }

    private String truncate(String value, int max) {
        String text = valueOrFallback(value, "");
        return text.length() <= max ? text : text.substring(0, max).trim() + "…";
    }

    private String formatDate(LocalDateTime dt) {
        if (dt == null) {
            return "–";
        }
        return DATE_FMT.format(dt);
    }

    private String initialOf(String value) {
        String text = valueOrFallback(value, "?");
        return text.substring(0, 1).toUpperCase();
    }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}
