package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;
import tn.esprit.services.ShareService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostOverviewController {

    private static final int PREVIEW_LIMIT = 180;
    private static final List<String> REACTION_TYPES = List.of("like", "love", "haha", "wow", "sad", "angry");
    private static final Map<String, String> REACTION_EMOJIS = Map.of(
            "like", "👍",
            "love", "❤️",
            "haha", "😂",
            "wow", "😮",
            "sad", "😢",
            "angry", "😡"
    );

    @FXML private VBox postsContainer;
    @FXML private VBox popularTopicsBox;
    @FXML private Label totalPostsStatLabel;
    @FXML private Label totalTopicsStatLabel;
    @FXML private Label searchSourceLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> topicFilterComboBox;
    @FXML private Button adminPostsButton;
    @FXML private Button adminPostsSidebarButton;

    private ForumCrudLauncher application;
    private ShareService shareService;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        this.shareService = application.getShareService();

        boolean canModerate = application.canModeratePosts();
        if (adminPostsButton != null) {
            adminPostsButton.setManaged(canModerate);
            adminPostsButton.setVisible(canModerate);
        }
        if (adminPostsSidebarButton != null) {
            adminPostsSidebarButton.setManaged(canModerate);
            adminPostsSidebarButton.setVisible(canModerate);
        }

        initializeFilters();
    }

    public void loadPosts() {
        postsContainer.getChildren().clear();
        try {
            List<Post> posts = application.getPostService().searchAndFilter(
                    searchField.getText(),
                    selectedFilterValue(typeFilterComboBox),
                    selectedFilterValue(topicFilterComboBox)
            );
            Map<Integer, Integer> replyCountsByPost = application.getPostService().getReplyCountsByPost();
            Map<Integer, Integer> reactionCountsByPost = application.getPostService().getReactionCountsByPost();
            Map<Integer, String> userReactionsByPost = getUserReactionsByPost(posts);

            // Update stats
            if (totalPostsStatLabel != null) {
                totalPostsStatLabel.setText(String.valueOf(posts.size()));
            }

            updateSearchSourceLabel();

            if (posts.isEmpty()) {
                postsContainer.getChildren().add(emptyStateNode("No posts match your search."));
                return;
            }
            for (Post post : posts) {
                int replyCount = replyCountsByPost.getOrDefault(post.getId(), 0);
                int reactionCount = reactionCountsByPost.getOrDefault(post.getId(), 0);
                String userReaction = userReactionsByPost.get(post.getId());
                postsContainer.getChildren().add(createPostCard(post, replyCount, reactionCount, userReaction));
            }
        } catch (SQLException e) {
            postsContainer.getChildren().add(emptyStateNode("Unable to load posts."));
            application.showError("Loading posts failed", e.getMessage());
            updateSearchSourceLabel();
        }
    }

    @FXML private void handleCreatePost()       { application.showCreatePostScene(); }
    @FXML private void handleOpenAdminPosts()   { application.showAdminPostManagementScene(); }
    @FXML private void handleSearch()           { loadPosts(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        typeFilterComboBox.setValue("All types");
        topicFilterComboBox.setValue("All topics");
        loadPosts();
    }

    // ─── Card builder ────────────────────────────────────────────────────────

    private VBox createPostCard(Post post, int replyCount, int reactionCount, String userReaction) {
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: rgba(255,255,255,0.62);"
            + "-fx-background-radius: 18;"
            + "-fx-border-color: rgba(255,255,255,0.78);"
            + "-fx-border-radius: 18;"
                + "-fx-border-width: 1;"
                + "-fx-cursor: hand;"
            + "-fx-effect: dropshadow(gaussian, rgba(30,41,59,0.10), 24, 0.14, 0, 8);"
        );

        // Body
        HBox body = new HBox(14);
        body.setPadding(new Insets(18, 20, 12, 20));
        body.setAlignment(Pos.TOP_LEFT);

        VBox contentBox = new VBox(8);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        body.getChildren().add(contentBox);

        String imageUrl = valueOrFallback(post.getImageUrl(), null);
        if (imageUrl != null) {
            StackPane thumbnailWrap = new StackPane();
            thumbnailWrap.setPrefSize(92, 92);
            thumbnailWrap.setMinSize(92, 92);
            thumbnailWrap.setMaxSize(92, 92);
            thumbnailWrap.setAlignment(Pos.CENTER);
            thumbnailWrap.setStyle(
                    "-fx-background-color: #f8fafc;"
                    + "-fx-background-radius: 14;"
                    + "-fx-border-color: #e2e8f0;"
                    + "-fx-border-radius: 14;"
                    + "-fx-border-width: 1;"
                    + "-fx-cursor: hand;"
            );

            ImageView thumbnail = new ImageView(new Image(imageUrl, 92, 92, true, true, true));
            thumbnail.setFitWidth(92);
            thumbnail.setFitHeight(92);
            thumbnail.setPreserveRatio(true);
            thumbnail.setSmooth(true);

            Rectangle clip = new Rectangle(92, 92);
            clip.setArcWidth(18);
            clip.setArcHeight(18);
            thumbnail.setClip(clip);

            VBox overlay = new VBox();
            overlay.setAlignment(Pos.CENTER);
            overlay.setPrefSize(92, 92);
            overlay.setStyle(
                    "-fx-background-color: rgba(15,23,42,0.45);"
                    + "-fx-background-radius: 14;"
            );
            Label overlayText = new Label("View image");
            overlayText.setStyle(
                    "-fx-text-fill: white;"
                    + "-fx-font-size: 11px;"
                    + "-fx-font-weight: 800;"
                    + "-fx-background-color: rgba(255,255,255,0.16);"
                    + "-fx-background-radius: 999;"
                    + "-fx-padding: 5 10 5 10;"
            );
            overlay.getChildren().add(overlayText);
            overlay.setOpacity(0);
            overlay.setVisible(false);

            thumbnailWrap.getChildren().addAll(thumbnail, overlay);
            thumbnailWrap.setOnMouseEntered(e -> {
                overlay.setVisible(true);
                overlay.setOpacity(1);
            });
            thumbnailWrap.setOnMouseExited(e -> {
                overlay.setOpacity(0);
                overlay.setVisible(false);
            });
            body.getChildren().add(thumbnailWrap);
        }

        String author  = application.resolveUsername(post.getUserId());
        String initial = (author != null && !author.isEmpty()) ? author.substring(0, 1).toUpperCase() : "?";

        HBox authorRow = new HBox(8);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label(initial);
        avatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #0ea5e9, #0d9488);"
                + "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 11px;"
                + "-fx-background-radius: 50;"
                + "-fx-min-width: 28; -fx-max-width: 28;"
                + "-fx-min-height: 28; -fx-max-height: 28;"
                + "-fx-alignment: center;"
        );

        Label userLabel = new Label(author != null ? author : "Unknown");
        userLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px; -fx-font-weight: 600;");
        authorRow.getChildren().addAll(avatar, userLabel);

        if (post.getCreatedAt() != null) {
            Label dot  = new Label("·");
            dot.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            Label time = new Label(formatRelativeTime(post.getCreatedAt()));
            time.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
            authorRow.getChildren().addAll(dot, time);
        }

        contentBox.getChildren().add(authorRow);

        Button shareButton = new Button("↗ Share");
        shareButton.setStyle(
                "-fx-background-color: rgba(29,78,216,0.10);" +
                "-fx-text-fill: #1d4ed8;" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 999;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 12 8 12;"
        );
        shareButton.setOnAction(e -> {
            e.consume();
            shareService.showShareDialog(post);
        });

        // Badges row
        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        String typeText  = valueOrFallback(post.getType(),  null);
        String topicText = valueOrFallback(post.getTopic(), null);
        if (typeText  != null) badges.getChildren().add(makeBadge(typeText,  "rgba(13,148,136,0.12)",  "#0d9488"));
        if (topicText != null) badges.getChildren().add(makeBadge(topicText, "rgba(139,92,246,0.12)", "#7c3aed"));
        if (!badges.getChildren().isEmpty()) contentBox.getChildren().add(badges);

        // Title
        Label titleLabel = new Label(valueOrFallback(post.getTitle(), "Untitled post"));
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 700; -fx-font-size: 15px;");
        contentBox.getChildren().add(titleLabel);

        // Preview
        Label previewLabel = new Label(truncate(post.getContent(), PREVIEW_LIMIT));
        previewLabel.setWrapText(true);
        previewLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        contentBox.getChildren().add(previewLabel);

        // Divider
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: rgba(226,232,240,0.8);");

        Label replyIcon = new Label("💬  " + replyCount + (replyCount == 1 ? " reply" : " replies"));
        replyIcon.setStyle("-fx-background-color: rgba(255,255,255,0.72); -fx-background-radius: 999;"
            + " -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: 700;"
            + " -fx-padding: 5 10 5 10; -fx-border-color: rgba(226,232,240,0.95); -fx-border-radius: 999;");

        Label likeIcon = new Label("⚡  " + reactionCount + (reactionCount == 1 ? " reaction" : " reactions"));
        likeIcon.setStyle("-fx-background-color: rgba(219,234,254,0.72); -fx-background-radius: 999;"
            + " -fx-text-fill: #1d4ed8; -fx-font-size: 11px; -fx-font-weight: 800;"
            + " -fx-padding: 5 10 5 10; -fx-border-color: rgba(29,78,216,0.18); -fx-border-radius: 999;");

        HBox reactionBar = new HBox(6);
        reactionBar.setAlignment(Pos.CENTER_LEFT);
        reactionBar.setPadding(new Insets(0, 20, 14, 20));
        reactionBar.setStyle("-fx-background-color: rgba(255,255,255,0.44); -fx-background-radius: 0 0 18 18;"
            + " -fx-border-color: rgba(255,255,255,0.72); -fx-border-width: 1 0 0 0; -fx-padding: 10 20 12 20;");

        for (String reactionType : REACTION_TYPES) {
            reactionBar.getChildren().add(createReactionButton(post, reactionType, reactionType.equals(userReaction)));
        }

        Region reactionSpacer = new Region();
        HBox.setHgrow(reactionSpacer, Priority.ALWAYS);
        reactionBar.getChildren().addAll(reactionSpacer, replyIcon, likeIcon);

        card.getChildren().addAll(body, divider, reactionBar);
        card.setOnMouseClicked(e -> application.showPostDetailsScene(post.getId()));

        // Hover effect
        String baseStyle = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(baseStyle
            + "-fx-border-color: rgba(59,130,246,0.48);"
            + "-fx-effect: dropshadow(gaussian, rgba(59,130,246,0.18), 26, 0.18, 0, 10);"));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    private void updateSearchSourceLabel() {
        if (searchSourceLabel == null || application == null || application.getPostService() == null) {
            return;
        }

        boolean hasSearchText = searchField != null && searchField.getText() != null && !searchField.getText().isBlank();
        boolean hasTypeFilter = selectedFilterValue(typeFilterComboBox) != null;
        boolean hasTopicFilter = selectedFilterValue(topicFilterComboBox) != null;

        if (hasSearchText || hasTypeFilter || hasTopicFilter) {
            searchSourceLabel.setText("Filtered results");
        } else {
            searchSourceLabel.setText("All posts");
        }
    }

    private Button createReactionButton(Post post, String reactionType, boolean selected) {
        String emoji = REACTION_EMOJIS.getOrDefault(reactionType, "👍");
        Button btn = new Button(emoji);
        btn.setFocusTraversable(false);
        btn.setStyle(selected
            ? "-fx-background-color: linear-gradient(to right, rgba(191,219,254,0.74), rgba(186,230,253,0.74));"
            + " -fx-text-fill: #1d4ed8; -fx-font-size: 14px; -fx-font-weight: 800;"
            + " -fx-background-radius: 999; -fx-border-color: rgba(59,130,246,0.38);"
            + " -fx-border-radius: 999; -fx-cursor: hand; -fx-padding: 5 10 5 10;"
            : "-fx-background-color: rgba(255,255,255,0.70); -fx-text-fill: #475569; -fx-font-size: 14px;"
            + " -fx-font-weight: 700; -fx-background-radius: 999; -fx-border-color: rgba(219,227,239,0.95);"
            + " -fx-border-radius: 999; -fx-cursor: hand; -fx-padding: 5 10 5 10;");

        btn.setOnMouseClicked(event -> event.consume());
        btn.setOnAction(event -> {
            event.consume();
            if (application.getCurrentUser() == null || application.getCurrentUser().getId() == null) {
                application.showError("No active user", "A logged-in user is required to react to posts.");
                return;
            }

            try {
                application.getReactionService().reactToPost(post.getId(), application.getCurrentUser().getId(), reactionType);
                loadPosts();
            } catch (IllegalArgumentException | SQLException e) {
                application.showError("Reaction failed", e.getMessage());
            }
        });

        return btn;
    }

    private Map<Integer, String> getUserReactionsByPost(List<Post> posts) {
        Map<Integer, String> reactions = new HashMap<>();
        if (application.getCurrentUser() == null || application.getCurrentUser().getId() == null) {
            return reactions;
        }

        int userId = application.getCurrentUser().getId();
        for (Post post : posts) {
            try {
                String reaction = application.getReactionService().getUserReactionForPost(post.getId(), userId);
                if (reaction != null && !reaction.isBlank()) {
                    reactions.put(post.getId(), reaction.trim().toLowerCase());
                }
            } catch (SQLException e) {
                // Keep loading other cards even if one reaction lookup fails.
            }
        }
        return reactions;
    }

    // ─── Sidebar helpers ─────────────────────────────────────────────────────

    private void initializeFilters() {
        typeFilterComboBox.getItems().clear();
        topicFilterComboBox.getItems().clear();
        typeFilterComboBox.getItems().add("All types");
        topicFilterComboBox.getItems().add("All topics");

        try {
            List<String> types  = application.getPostService().getDistinctTypes();
            List<String> topics = application.getPostService().getDistinctTopics();
            typeFilterComboBox.getItems().addAll(types);
            topicFilterComboBox.getItems().addAll(topics);

            // Populate popular topics sidebar
            if (popularTopicsBox != null) {
                popularTopicsBox.getChildren().clear();
                Label title = new Label("Popular topics");
                title.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 800;");
                FlowPane topicFlow = new FlowPane(6, 6);
                for (String topic : topics) {
                    Label pill = new Label(topic);
                    pill.setStyle(
                            "-fx-background-color: #f1f5f9;"
                            + "-fx-text-fill: #334155;"
                            + "-fx-font-size: 11px; -fx-font-weight: 700;"
                            + "-fx-background-radius: 999;"
                            + "-fx-padding: 5 12 5 12;"
                            + "-fx-border-color: rgba(226,232,240,0.9);"
                            + "-fx-border-radius: 999; -fx-border-width: 1;"
                            + "-fx-cursor: hand;"
                    );
                    pill.setOnMouseClicked(e -> {
                        topicFilterComboBox.setValue(topic);
                        loadPosts();
                    });
                    topicFlow.getChildren().add(pill);
                }
                popularTopicsBox.getChildren().addAll(title, topicFlow);
            }

            // Topic count stat
            if (totalTopicsStatLabel != null) {
                totalTopicsStatLabel.setText(String.valueOf(topics.size()));
            }
        } catch (SQLException e) {
            application.showError("Loading filters failed", e.getMessage());
        }

        typeFilterComboBox.setValue("All types");
        topicFilterComboBox.setValue("All topics");
    }

    // ─── Utility helpers ─────────────────────────────────────────────────────

    private Label makeBadge(String text, String bg, String fg) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color: " + bg + ";"
                + "-fx-text-fill: " + fg + ";"
                + "-fx-font-size: 11px; -fx-font-weight: 700;"
                + "-fx-background-radius: 999;"
                + "-fx-padding: 4 12 4 12;"
        );
        return badge;
    }

    private Label emptyStateNode(String message) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-padding: 48 0 48 0;");
        return lbl;
    }

    private String truncate(String value, int max) {
        String s = valueOrFallback(value, "");
        return s.length() <= max ? s : s.substring(0, max).trim() + "…";
    }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String selectedFilterValue(ComboBox<String> cb) {
        String v = cb.getValue();
        return (v == null || v.startsWith("All ")) ? null : v;
    }

    private String formatRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) return "–";
        long minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now());
        if (minutes < 1)   return "just now";
        if (minutes < 60)  return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24)    return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        long days = hours / 24;
        if (days < 30)     return days + " day" + (days == 1 ? "" : "s") + " ago";
        long months = days / 30;
        if (months < 12)   return months + " month" + (months == 1 ? "" : "s") + " ago";
        return (months / 12) + " year" + (months / 12 == 1 ? "" : "s") + " ago";
    }
}