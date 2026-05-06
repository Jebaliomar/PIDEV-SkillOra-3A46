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
import tn.esprit.controllers.admin.AdminSidebarController;
import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.entities.Report;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class AdminPostManagementController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm");
    private static final int PREVIEW_LIMIT = 170;
    private static final int REPORT_PREVIEW_LIMIT = 220;

    @FXML private VBox postsContainer;
    @FXML private Label currentUserLabel;
    @FXML private Label postCountLabel;
    @FXML private Label totalPostsLabel;
    @FXML private Label flaggedPostsLabel;
    @FXML private Label activePostsLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Label contentSectionTitleLabel;
    @FXML private Label contentSectionSubtitleLabel;
    @FXML private Label contentHintLabel;
    @FXML private Button viewCommunityButton;
    @FXML private Button reportsButton;
    @FXML private AdminSidebarController adminSidebarController;

    private ForumCrudLauncher application;
    private boolean showReports;

    @FXML
    private void initialize() {
        markForumActive();
    }

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        markForumActive();
        if (currentUserLabel != null) {
            currentUserLabel.setText("Moderator: " + application.getCurrentUserDisplay());
        }
        updateContentHeader();
        refreshStats();
    }

    public void loadPosts() {
        if (postsContainer == null) {
            return;
        }

        showReports = false;
        updateContentHeader();
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
    private void handleShowPosts() {
        loadPosts();
    }

    @FXML
    private void handleRefresh() {
        refreshCurrentView();
    }

    @FXML
    private void handleShowReports() {
        loadReports();
    }

    private void loadReports() {
        if (postsContainer == null) {
            return;
        }

        showReports = true;
        updateContentHeader();
        postsContainer.getChildren().clear();
        try {
            List<Report> reports = application.getReportService().getAll();
            if (postCountLabel != null) {
                postCountLabel.setText(reports.size() + (reports.size() == 1 ? " report" : " reports"));
            }

            if (reports.isEmpty()) {
                postsContainer.getChildren().add(emptyStateLabel("No reported content is available."));
                return;
            }

            for (Report report : reports) {
                postsContainer.getChildren().add(createReportCard(report));
            }
        } catch (SQLException e) {
            postsContainer.getChildren().add(emptyStateLabel("Unable to load reports."));
            application.showError("Loading reports failed", e.getMessage());
        }

        refreshStats();
    }

    private void refreshCurrentView() {
        if (showReports) {
            loadReports();
        } else {
            loadPosts();
        }
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
        openBtn.setOnAction(e -> application.showAdminPostDetailsScene(post.getId()));

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
            refreshCurrentView();
            application.showInfo("Post deleted", "The post was deleted successfully.");
        } catch (SQLException e) {
            application.showError("Deleting post failed", e.getMessage());
        }
    }

    private Node createReportCard(Report report) {
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: rgba(226,232,240,0.88);" +
                "-fx-border-radius: 16;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 22, 0.12, 0, 6);"
        );

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));

        String reporterName = application.resolveUsername(report.getUserId());
        Label avatar = new Label(initialOf(reporterName));
        avatar.setStyle(
                "-fx-background-color: rgba(37,99,235,0.14);" +
                "-fx-text-fill: #1D4ED8;" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 13px;" +
                "-fx-background-radius: 999;" +
                "-fx-min-width: 34; -fx-max-width: 34;" +
                "-fx-min-height: 34; -fx-max-height: 34;" +
                "-fx-alignment: center;"
        );

        VBox reporterBox = new VBox(4);
        Label reporterLine = new Label(reporterName + " reported");
        reporterLine.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 800; -fx-font-size: 14px;");

        Label metaLine = new Label(formatDate(report.getCreatedAt()) + "  ·  " + targetSummary(report));
        metaLine.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        reporterBox.getChildren().addAll(reporterLine, metaLine);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_RIGHT);
        badges.getChildren().addAll(
                reasonBadge(valueOrFallback(report.getReason(), "Report")),
                statusBadge(valueOrFallback(report.getStatus(), "pending"))
        );

        header.getChildren().addAll(avatar, reporterBox, spacer, badges);

        VBox body = new VBox(14);
        body.setPadding(new Insets(0, 20, 18, 20));

        VBox targetBox = new VBox(8);
        targetBox.setStyle(
                "-fx-background-color: #F8FAFC;" +
                "-fx-background-radius: 14;" +
                "-fx-border-color: rgba(226,232,240,0.88);" +
                "-fx-border-radius: 14;" +
                "-fx-border-width: 1;" +
                "-fx-padding: 16;"
        );

        Label targetTitle = new Label(targetHeading(report));
        targetTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: 800; -fx-font-size: 15px;");
        Label targetMeta = new Label(targetMeta(report));
        targetMeta.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label preview = new Label(targetPreview(report));
        preview.setWrapText(true);
        preview.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px; -fx-line-spacing: 3;");

        targetBox.getChildren().addAll(targetTitle, targetMeta, preview);

        Label detailsLabel = new Label("Report details");
        detailsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800;");
        Label details = new Label(valueOrFallback(report.getDescription(), "No additional details were provided."));
        details.setWrapText(true);
        details.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-line-spacing: 3;");
        VBox detailsBox = new VBox(6, detailsLabel, details);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: rgba(226,232,240,0.9);");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button viewButton = actionButton("View Full Post", "rgba(37,99,235,0.10)", "#1D4ED8");
        viewButton.setOnAction(e -> application.showAdminPostDetailsScene(report.getPostId()));

        Button reviewButton = actionButton("Review", "rgba(16,185,129,0.10)", "#059669");
        reviewButton.setOnAction(e -> handleReviewReport(report));

        Button dismissButton = actionButton("Dismiss", "rgba(100,116,139,0.10)", "#475569");
        dismissButton.setOnAction(e -> handleDismissReport(report));

        Button deleteTargetButton = actionButton(
                report.getReplyId() == null ? "Delete Post" : "Delete Reply",
                "rgba(239,68,68,0.10)",
                "#DC2626"
        );
        deleteTargetButton.setOnAction(e -> handleDeleteTarget(report));

        actions.getChildren().addAll(viewButton, reviewButton, dismissButton, deleteTargetButton);

        body.getChildren().addAll(targetBox, detailsBox, divider, actions);
        card.getChildren().addAll(header, body);
        return card;
    }

    private void handleReviewReport(Report report) {
        try {
            if (application.getReportService().markReviewed(report.getId(), currentModeratorId())) {
                loadReports();
                application.showInfo("Report updated", "The report was marked as reviewed.");
            }
        } catch (SQLException e) {
            application.showError("Review failed", e.getMessage());
        }
    }

    private void handleDismissReport(Report report) {
        try {
            if (application.getReportService().dismiss(report.getId(), currentModeratorId())) {
                loadReports();
                application.showInfo("Report updated", "The report was dismissed.");
            }
        } catch (SQLException e) {
            application.showError("Dismiss failed", e.getMessage());
        }
    }

    private void handleDeleteTarget(Report report) {
        String targetName = report.getReplyId() == null ? "post" : "reply";
        boolean confirmed = application.confirmAction(
                "Delete " + targetName,
                "Delete the reported " + targetName + " and remove it from the community?"
        );
        if (!confirmed) {
            return;
        }

        try {
            if (report.getReplyId() == null) {
                application.getPostService().delete(report.getPostId());
            } else {
                application.getReplyService().delete(report.getReplyId());
            }

            loadReports();
            application.showInfo("Deleted", "The reported content was deleted successfully.");
        } catch (SQLException e) {
            application.showError("Deletion failed", e.getMessage());
        }
    }

    private Label reasonBadge(String reason) {
        Label badge = new Label(reason);
        badge.setStyle(
                "-fx-background-color: rgba(239,68,68,0.10);" +
                "-fx-text-fill: #DC2626;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 800;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 5 12 5 12;"
        );
        return badge;
    }

    private Label statusBadge(String status) {
        String normalized = status == null ? "pending" : status.trim().toLowerCase(Locale.ROOT);
        String bg;
        String fg;
        String label;
        switch (normalized) {
            case "reviewed" -> {
                bg = "rgba(16,185,129,0.14)";
                fg = "#059669";
                label = "Reviewed";
            }
            case "dismissed" -> {
                bg = "rgba(100,116,139,0.14)";
                fg = "#475569";
                label = "Dismissed";
            }
            default -> {
                bg = "rgba(245,158,11,0.14)";
                fg = "#D97706";
                label = "Pending Review";
            }
        }

        Label badge = new Label(label);
        badge.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 800;" +
                "-fx-background-radius: 999;" +
                "-fx-padding: 5 12 5 12;"
        );
        return badge;
    }

    private String targetHeading(Report report) {
        if (report.getReplyId() != null) {
            return "Reported Reply #" + report.getReplyId();
        }
        return "Reported Post #" + report.getPostId();
    }

    private String targetMeta(Report report) {
        try {
            if (report.getReplyId() != null) {
                Reply reply = application.getReplyService().getById(report.getReplyId());
                Post post = application.getPostService().getById(report.getPostId());
                String replyAuthor = reply == null ? "Unknown author" : valueOrFallback(reply.getAuthorName(), "Anonymous");
                String postTitle = post == null ? "Deleted post" : valueOrFallback(post.getTitle(), "Untitled post");
                return replyAuthor + " · on " + postTitle;
            }

            Post post = application.getPostService().getById(report.getPostId());
            if (post == null) {
                return "Target already deleted";
            }
            String author = post.getUserId() == null ? "Unknown author" : application.resolveUsername(post.getUserId());
            return author + " · " + valueOrFallback(post.getTopic(), "No topic");
        } catch (SQLException e) {
            return "Unable to load target";
        }
    }

    private String targetPreview(Report report) {
        try {
            if (report.getReplyId() != null) {
                Reply reply = application.getReplyService().getById(report.getReplyId());
                if (reply == null) {
                    return "The reported reply is no longer available.";
                }
                return truncate(valueOrFallback(reply.getContent(), "–"), REPORT_PREVIEW_LIMIT);
            }

            Post post = application.getPostService().getById(report.getPostId());
            if (post == null) {
                return "The reported post is no longer available.";
            }
            return truncate(valueOrFallback(post.getContent(), "–"), REPORT_PREVIEW_LIMIT);
        } catch (SQLException e) {
            return "Unable to load target content.";
        }
    }

    private String targetSummary(Report report) {
        return report.getReplyId() == null
                ? "Post #" + report.getPostId()
                : "Reply #" + report.getReplyId() + " on post #" + report.getPostId();
    }

    private Integer currentModeratorId() {
        return application.getCurrentUser() == null ? null : application.getCurrentUser().getId();
    }

    private void updateContentHeader() {
        if (contentSectionTitleLabel != null) {
            contentSectionTitleLabel.setText(showReports ? "Reports" : "All Posts");
        }
        if (contentSectionSubtitleLabel != null) {
            contentSectionSubtitleLabel.setText(showReports ? "Showing reported community content" : "Showing your moderation feed");
        }
        if (contentHintLabel != null) {
            contentHintLabel.setText(showReports
                    ? "Use the report actions to review, dismiss, or remove reported content quickly."
                    : "Use the card actions to open details or remove problematic posts quickly.");
        }
        setButtonVariant(viewCommunityButton, showReports ? "btn-secondary" : "btn-primary");
        setButtonVariant(reportsButton, showReports ? "btn-primary" : "btn-secondary");
    }

    private void setButtonVariant(Button button, String variant) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("btn-primary", "btn-secondary");
        if (!button.getStyleClass().contains(variant)) {
            button.getStyleClass().add(variant);
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

    private void markForumActive() {
        if (adminSidebarController != null) {
            adminSidebarController.setActive(AdminSidebarController.ActiveItem.FORUM);
        }
    }
}
