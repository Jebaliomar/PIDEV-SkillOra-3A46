package tn.esprit.controllers.forum;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.entities.Report;
import tn.esprit.mains.ForumCrudLauncher;
import tn.esprit.services.ShareService;
import tn.esprit.services.TranslationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javafx.concurrent.Task;
import tn.esprit.services.SummarizationService;

public class PostDetailsController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> POST_REACTION_TYPES = List.of("like", "love", "haha", "wow", "sad", "angry");
    private static final Map<String, String> POST_REACTION_EMOJIS = new LinkedHashMap<>();
    private static final List<String> REPORT_REASONS = List.of(
            "Harassment or bullying",
            "Spam or scams",
            "Hate speech",
            "Misinformation",
            "Inappropriate content",
            "Other"
    );

    static {
        POST_REACTION_EMOJIS.put("like", "👍");
        POST_REACTION_EMOJIS.put("love", "❤️");
        POST_REACTION_EMOJIS.put("haha", "😂");
        POST_REACTION_EMOJIS.put("wow", "😮");
        POST_REACTION_EMOJIS.put("sad", "😢");
        POST_REACTION_EMOJIS.put("angry", "😡");
    }

    @FXML private Label   titleValueLabel;
    @FXML private Label   idValueLabel;
    @FXML private Label   typeValueLabel;
    @FXML private Label   topicValueLabel;
    @FXML private Label   contentValueLabel;
    @FXML private Label   postReplyCountLabel;
    @FXML private Label   postReactionCountLabel;
    @FXML private HBox    postReactionsContainer;
    @FXML private VBox    imageSection;
    @FXML private ImageView postImageView;
    @FXML private Label   imageStatusLabel;
    @FXML private Label   userValueLabel;
    @FXML private Label   createdAtValueLabel;
    @FXML private Label   updatedAtValueLabel;
    @FXML private HBox    postActionsContainer;
    @FXML private Button  editPostButton;
    @FXML private Button  deletePostButton;
    @FXML private VBox    repliesContainer;
    @FXML private Label   currentReplyUserLabel;
    @FXML private TextArea replyContentArea;
    @FXML private Label   replyErrorLabel;
    @FXML private Button summarizePostButton;
    @FXML private VBox summarySection;
    @FXML private Label summaryStatusLabel;
    @FXML private TextArea summaryTextArea;

    private final SummarizationService summarizationService = new SummarizationService();
    private ForumCrudLauncher application;
    private Post post;
    private Button reportPostButton;
    private boolean reportPostButtonInstalled;
    private final TranslationService translationService = new TranslationService();
    private final ShareService shareService = new ShareService();

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        if (currentReplyUserLabel != null) {
            currentReplyUserLabel.setText(application.getCurrentUserDisplay());
        }
        ensureReportPostButtonInstalled();
        clearReplyError();
        updatePostActionVisibility();
    }

    @FXML
    private void handleSharePost() {
        if (post == null) {
            application.showError("Share unavailable", "No post is loaded.");
            return;
        }

        try {
            shareService.showShareDialog(post);
        } catch (Exception e) {
            application.showError("Sharing failed", e.getMessage());
        }
    }

    public boolean loadPost(int postId) {
        try {
            post = application.getPostService().getById(postId);
            if (post == null) {
                application.showError("Post not found", "The selected post no longer exists.");
                application.showOverviewScene();
                return false;
            }
            populatePostDetails();
            updatePostActionVisibility();
            loadReplies();
            refreshPostReactions();
            return true;
        } catch (SQLException e) {
            application.showError("Loading post failed", e.getMessage());
            application.showOverviewScene();
            return false;
        }
    }

    @FXML private void handleBack() { application.showOverviewScene(); }

    @FXML
    private void handleEditPost() {
        if (post != null && canEditPost()) {
            application.showEditPostScene(post.getId());
        }
    }

    @FXML
    private void handleDeletePost() {
        if (post == null || !canDeletePost()) return;

        boolean confirmed = application.confirmAction(
                "Delete post",
                "Delete post #" + post.getId() + " and all related replies?");
        if (!confirmed) return;

        try {
            application.getPostService().delete(post.getId());
            application.showInfo("Post deleted", "The post was deleted successfully.");
            application.showOverviewScene();
        } catch (SQLException e) {
            application.showError("Deleting post failed", e.getMessage());
        }
    }

    private void handleReportPost() {
        if (post == null || !canReportContent()) {
            return;
        }

        Optional<ReportDraft> draft = showReportDialog(
                "Report post",
                "Help us understand why this post should be reviewed.",
                valueOrFallback(post.getTitle(), "Untitled post"),
                valueOrFallback(post.getContent(), "–")
        );

        draft.ifPresent(result -> {
            try {
                Report report = new Report();
                report.setPostId(post.getId());
                report.setReplyId(null);
                report.setUserId(application.getCurrentUser().getId());
                report.setReason(result.reason());
                report.setDescription(result.details());
                application.getReportService().add(report);
                application.showInfo("Report submitted", "Thanks — the post was sent to moderation.");
            } catch (IllegalArgumentException | SQLException e) {
                application.showError("Reporting failed", e.getMessage());
            }
        });
    }

    @FXML
    private void handleAddReply() {
        if (post == null) return;
        clearReplyError();

        if (application.getCurrentUser() == null) {
            showReplyError("No active user session. Please restart the application.");
            return;
        }

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
        } catch (IllegalArgumentException | IllegalStateException e) {
            showReplyError(e.getMessage());
        } catch (SQLException e) {
            application.showError("Saving reply failed", e.getMessage());
        }
    }

    private void populatePostDetails() {
        titleValueLabel.setText(valueOrFallback(post.getTitle(), "Untitled post"));
        idValueLabel.setText(String.valueOf(post.getId()));
        typeValueLabel.setText(valueOrFallback(post.getType(), "–"));
        topicValueLabel.setText(valueOrFallback(post.getTopic(), "–"));
        contentValueLabel.setText(valueOrFallback(post.getContent(), "–"));
        updateImagePreview(post.getImageUrl());

        String author = post.getUserId() == null
                ? "Unknown user"
                : application.resolveUsername(post.getUserId()) + " (ID: " + post.getUserId() + ")";
        userValueLabel.setText(author);
        createdAtValueLabel.setText(formatDate(post.getCreatedAt()));
        updatedAtValueLabel.setText(formatDate(post.getUpdatedAt()));
        updateSummaryVisibility();
    }

    private void updateImagePreview(String imageUrl) {
        if (imageSection == null || postImageView == null || imageStatusLabel == null) {
            return;
        }

        String url = valueOrFallback(imageUrl, "");
        if (url.isBlank()) {
            imageSection.setManaged(false);
            imageSection.setVisible(false);
            postImageView.setImage(null);
            imageStatusLabel.setText("No image attached");
            return;
        }

        try {
            postImageView.setImage(new Image(url, true));
            imageStatusLabel.setText("Image from Cloudinary");
            imageSection.setManaged(true);
            imageSection.setVisible(true);
        } catch (Exception e) {
            postImageView.setImage(null);
            imageStatusLabel.setText("Image could not be loaded");
            imageSection.setManaged(true);
            imageSection.setVisible(true);
        }
    }

    private void loadReplies() {
        repliesContainer.getChildren().clear();
        try {
            List<Reply> replies = application.getReplyService().getByPostId(post.getId());
            updateReplyCounter(replies.size());
            if (replies.isEmpty()) {
                repliesContainer.getChildren().add(emptyStateLabel("No replies yet for this post."));
                return;
            }
            for (Reply r : replies) {
                repliesContainer.getChildren().add(createReplyCard(r));
            }
        } catch (SQLException e) {
            repliesContainer.getChildren().add(emptyStateLabel("Could not load replies."));
            application.showError("Loading replies failed", e.getMessage());
        }
    }

    private void updateReplyCounter(int replyCount) {
        if (postReplyCountLabel == null) {
            return;
        }
        postReplyCountLabel.setText(replyCount + (replyCount == 1 ? " reply" : " replies"));
    }

    private void refreshPostReactions() {
        if (post == null || postReactionsContainer == null || postReactionCountLabel == null) {
            return;
        }

        try {
            Map<String, Integer> counts = application.getReactionService().getReactionCountsForPost(post.getId());
            String currentUserReaction = null;
            if (application.getCurrentUser() != null && application.getCurrentUser().getId() != null) {
                currentUserReaction = application.getReactionService()
                        .getUserReactionForPost(post.getId(), application.getCurrentUser().getId());
                if (currentUserReaction != null) {
                    currentUserReaction = currentUserReaction.trim().toLowerCase(Locale.ROOT);
                }
            }

            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            postReactionCountLabel.setText(total + (total == 1 ? " reaction" : " reactions"));

            postReactionsContainer.getChildren().clear();
            postReactionsContainer.setSpacing(8);
            for (String reactionType : POST_REACTION_TYPES) {
                postReactionsContainer.getChildren().add(
                        createPostReactionButton(reactionType, counts.getOrDefault(reactionType, 0), reactionType.equals(currentUserReaction))
                );
            }
        } catch (SQLException e) {
            postReactionsContainer.getChildren().clear();
            postReactionsContainer.getChildren().add(emptyStateLabel("Reactions unavailable"));
            postReactionCountLabel.setText("0 reactions");
        }
    }

    private Button createPostReactionButton(String reactionType, int count, boolean selected) {
        String emoji = POST_REACTION_EMOJIS.getOrDefault(reactionType, "👍");
        String caption = emoji + " " + count + " " + reactionType.substring(0, 1).toUpperCase() + reactionType.substring(1);
        Button btn = new Button(caption);
        btn.setFocusTraversable(false);
        btn.setStyle(selected
            ? "-fx-background-color: linear-gradient(to right, rgba(191,219,254,0.78), rgba(186,230,253,0.80));"
            + " -fx-text-fill: #1d4ed8; -fx-font-size: 12px;"
            + " -fx-font-weight: 900; -fx-background-radius: 999; -fx-border-color: rgba(59,130,246,0.40);"
            + " -fx-border-radius: 999; -fx-cursor: hand; -fx-padding: 8 13 8 13;"
            : "-fx-background-color: rgba(255,255,255,0.72); -fx-text-fill: #334155; -fx-font-size: 12px;"
            + " -fx-font-weight: 700; -fx-background-radius: 999; -fx-border-color: rgba(219,227,239,0.95);"
            + " -fx-border-radius: 999; -fx-cursor: hand; -fx-padding: 8 13 8 13;");

        btn.setOnAction(event -> {
            if (post == null || application.getCurrentUser() == null || application.getCurrentUser().getId() == null) {
                application.showError("Reaction unavailable", "A logged-in user is required to react.");
                return;
            }

            try {
                application.getReactionService().reactToPost(post.getId(), application.getCurrentUser().getId(), reactionType);
                refreshPostReactions();
            } catch (IllegalArgumentException | SQLException e) {
                application.showError("Reaction failed", e.getMessage());
            }
        });

        return btn;
    }

    private VBox createReplyCard(Reply reply) {
        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color: rgba(255,255,255,0.64);" +
                        "-fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.78);" +
                        "-fx-border-radius: 10;" +
                "-fx-border-width: 1;" +
                "-fx-effect: dropshadow(gaussian, rgba(30,41,59,0.08), 14, 0.12, 0, 4);"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 10, 14));
        header.setStyle(
            "-fx-background-color: rgba(255,255,255,0.56);" +
                        "-fx-background-radius: 10 10 0 0;"
        );

        String authorName = valueOrFallback(reply.getAuthorName(), "Anonymous");

        Label avatar = new Label(authorName.substring(0, 1).toUpperCase());
        avatar.setStyle(
                "-fx-background-color: rgba(0,122,255,0.12);" +
                        "-fx-text-fill: #007AFF;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;" +
                        "-fx-background-radius: 50;" +
                        "-fx-min-width: 28; -fx-max-width: 28;" +
                        "-fx-min-height: 28; -fx-max-height: 28;" +
                        "-fx-alignment: center;"
        );

        Label authorLabel = new Label(authorName);
        authorLabel.setStyle("-fx-text-fill: #1C1C1E; -fx-font-weight: 700; -fx-font-size: 13px;");

        header.getChildren().addAll(avatar, authorLabel);

        if (reply.getCreatedAt() != null) {
            Label dateLabel = new Label("· " + DATE_FMT.format(reply.getCreatedAt()));
            dateLabel.setStyle("-fx-text-fill: #AEAEB2; -fx-font-size: 11.5px;");
            header.getChildren().add(dateLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        HBox replyActions = new HBox(6);
        replyActions.setAlignment(Pos.CENTER_RIGHT);

        if (canReportContent()) {
            replyActions.getChildren().add(
                    replyActionButton("⚑ Report", "rgba(245,158,11,0.10)", "#D97706", e -> handleReportReply(reply))
            );
        }

        if (canManageReply(reply)) {
            replyActions.getChildren().addAll(
                    replyActionButton("Edit", "rgba(0,122,255,0.10)", "#007AFF", e -> handleEditReply(reply)),
                    replyActionButton("Delete", "rgba(255,59,48,0.10)", "#FF3B30", e -> handleDeleteReply(reply))
            );
        }

        if (!replyActions.getChildren().isEmpty()) {
            header.getChildren().add(replyActions);
        }

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: rgba(60,60,67,0.10);");

        Label contentLabel = new Label(valueOrFallback(reply.getContent(), "–"));
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill: #3C3C43; -fx-font-size: 13px; -fx-line-spacing: 2;");
        VBox contentBox = new VBox(contentLabel);
        contentBox.setPadding(new Insets(12, 16, 14, 14));

        card.getChildren().addAll(header, divider, contentBox);
        return card;
    }

    private Button replyActionButton(String text, String bg, String fg,
                                     EventHandler<ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setOnAction(handler);
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-font-size: 11.5px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 5 11 5 11;"
        );
        return btn;
    }

    private void handleReportReply(Reply reply) {
        if (reply == null || !canReportContent()) {
            return;
        }

        Optional<ReportDraft> draft = showReportDialog(
                "Report reply",
                "Help us understand why this reply should be reviewed.",
                "Reply by " + valueOrFallback(reply.getAuthorName(), "Anonymous"),
                valueOrFallback(reply.getContent(), "–")
        );

        draft.ifPresent(result -> {
            try {
                Report report = new Report();
                report.setPostId(post.getId());
                report.setReplyId(reply.getId());
                report.setUserId(application.getCurrentUser().getId());
                report.setReason(result.reason());
                report.setDescription(result.details());
                application.getReportService().add(report);
                application.showInfo("Report submitted", "Thanks — the reply was sent to moderation.");
            } catch (IllegalArgumentException | SQLException e) {
                application.showError("Reporting failed", e.getMessage());
            }
        });
    }

    private void handleEditReply(Reply reply) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Reply");
        dialog.setHeaderText("Update your reply");

        ButtonType saveType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextArea editor = new TextArea(valueOrFallback(reply.getContent(), ""));
        editor.setWrapText(true);
        editor.setPrefRowCount(5);
        editor.setPromptText("Update your reply…");
        dialog.getDialogPane().setContent(editor);
        dialog.setResultConverter(bt -> bt == saveType ? editor.getText().trim() : null);

        dialog.showAndWait().ifPresent(updated -> {
            try {
                reply.setContent(updated);
                reply.setUpdatedAt(LocalDateTime.now());
                application.getReplyService().update(reply);
                loadReplies();
                application.showInfo("Reply updated", "Your reply was updated successfully.");
            } catch (IllegalArgumentException | IllegalStateException e) {
                showReplyError(e.getMessage());
            } catch (SQLException e) {
                application.showError("Updating reply failed", e.getMessage());
            }
        });
    }

    private void handleDeleteReply(Reply reply) {
        if (!application.confirmAction("Delete reply", "Remove this reply from the conversation?")) return;
        try {
            application.getReplyService().delete(reply.getId());
            loadReplies();
            application.showInfo("Reply deleted", "The reply was deleted successfully.");
        } catch (SQLException e) {
            application.showError("Deleting reply failed", e.getMessage());
        }
    }

    private void ensureReportPostButtonInstalled() {
        if (reportPostButtonInstalled || postActionsContainer == null) {
            return;
        }

        reportPostButton = new Button("⚑ Report");
        reportPostButton.setOnAction(e -> handleReportPost());
        reportPostButton.setStyle(
                "-fx-background-color: rgba(245,158,11,0.10);" +
                        "-fx-text-fill: #D97706;" +
                        "-fx-font-weight: 700;" +
                        "-fx-font-size: 12.5px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 9 16 9 16;"
        );
        reportPostButton.setManaged(false);
        reportPostButton.setVisible(false);
        postActionsContainer.getChildren().add(reportPostButton);
        reportPostButtonInstalled = true;
    }

    private void updatePostActionVisibility() {
        boolean canEdit = canEditPost();
        boolean canDelete = canDeletePost();
        boolean canReport = canReportContent();
        boolean showActions = canEdit || canDelete || canReport;

        setManagedVisible(postActionsContainer, showActions);
        setManagedVisible(editPostButton, canEdit);
        setManagedVisible(deletePostButton, canDelete);
        setManagedVisible(reportPostButton, canReport);
    }

    private void setManagedVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setManaged(visible);
            node.setVisible(visible);
        }
    }

    private boolean canEditPost() {
        return isCurrentUser(post == null ? null : post.getUserId());
    }

    private boolean canDeletePost() {
        return canEditPost() || application.canModeratePosts();
    }

    private boolean canReportContent() {
        return application != null && application.getCurrentUser() != null;
    }

    private boolean canManageReply(Reply reply) {
        return reply != null && isCurrentUser(reply.getUserId());
    }

    private boolean isCurrentUser(Integer userId) {
        return userId != null
                && application != null
                && application.getCurrentUser() != null
                && userId.equals(application.getCurrentUser().getId());
    }

    private Optional<ReportDraft> showReportDialog(String title, String header, String targetLabel, String targetContent) {
        Dialog<ReportDraft> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        ButtonType reportType = new ButtonType("Submit Report", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(reportType, ButtonType.CANCEL);

        Label targetTitle = new Label(targetLabel);
        targetTitle.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: 800; -fx-font-size: 14px;");
        Label targetBody = new Label(truncate(targetContent, 260));
        targetBody.setWrapText(true);
        targetBody.setStyle("-fx-text-fill: #475569; -fx-font-size: 12.5px;");

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll(REPORT_REASONS);
        reasonCombo.getSelectionModel().selectFirst();
        reasonCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea detailsArea = new TextArea();
        detailsArea.setPromptText("Optional details to help the moderators understand the issue...");
        detailsArea.setPrefRowCount(5);
        detailsArea.setWrapText(true);

        VBox content = new VBox(12);
        content.setPadding(new Insets(10, 4, 4, 4));

        Label reasonLabel = new Label("Reason");
        reasonLabel.setStyle("-fx-text-fill: #334155; -fx-font-weight: 700; -fx-font-size: 12px;");
        Label detailsLabel = new Label("Additional details");
        detailsLabel.setStyle("-fx-text-fill: #334155; -fx-font-weight: 700; -fx-font-size: 12px;");

        VBox targetBox = new VBox(6, targetTitle, targetBody);
        targetBox.setStyle(
                "-fx-background-color: #F8FAFC;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: rgba(226,232,240,0.9);" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 14;"
        );

        content.getChildren().addAll(targetBox, reasonLabel, reasonCombo, detailsLabel, detailsArea);

        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(bt -> {
            if (bt == reportType) {
                return new ReportDraft(
                        reasonCombo.getValue(),
                        detailsArea.getText() == null ? null : detailsArea.getText().trim()
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private record ReportDraft(String reason, String details) {}

    private Label emptyStateLabel(String message) {
        Label lbl = new Label(message);
        lbl.setStyle("-fx-text-fill: #AEAEB2; -fx-font-size: 13px; -fx-padding: 14 0 8 0;");
        lbl.setWrapText(true);
        return lbl;
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

    private String formatDate(LocalDateTime dt) { return dt == null ? "–" : DATE_FMT.format(dt); }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String truncate(String value, int max) {
        String text = valueOrFallback(value, "");
        return text.length() <= max ? text : text.substring(0, max).trim() + "…";
    }

    @FXML
    private void handleTranslatePost() {
        if (post == null || post.getContent() == null || post.getContent().isBlank()) {
            application.showError("Translation unavailable", "There is no post content to translate.");
            return;
        }

        if (!translationService.isConfigured()) {
            application.showError(
                    "Translation unavailable",
                    "Missing Hugging Face translation token or endpoint. Set HF_TOKEN and HF_TRANSLATION_URL."
            );
            return;
        }

        Dialog<LanguageOption> dialog = new Dialog<>();
        dialog.setTitle("Translate Post");
        dialog.setHeaderText(null);

        ButtonType translateType = new ButtonType("Translate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(translateType, ButtonType.CANCEL);
        styleDialogPane(dialog.getDialogPane());

        ComboBox<LanguageOption> targetBox = new ComboBox<>();
        targetBox.setVisible(true);
        targetBox.setManaged(true);
        targetBox.setMinHeight(38);
        targetBox.setPrefHeight(38);
        targetBox.setPrefWidth(320);
        targetBox.setMaxWidth(320);
        targetBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; "
            + "-fx-border-color: #dbe3ef; -fx-border-radius: 12; -fx-border-width: 1.2; "
            + "-fx-font-size: 13px; -fx-text-fill: #0f172a;");
        targetBox.getItems().addAll(
                new LanguageOption("English", "en_XX"),
                new LanguageOption("French", "fr_XX"),
                new LanguageOption("Arabic", "ar_AR")
        );
        targetBox.getSelectionModel().selectFirst();

        Label sectionTitle = new Label("Target language");
        sectionTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 13px; -fx-font-weight: 800;");

        Label helperText = new Label("Pick the language to generate a polished version of this post.");
        helperText.setWrapText(true);
        helperText.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        VBox container = new VBox(10);
        container.setPadding(new Insets(4, 2, 2, 2));
        container.getChildren().addAll(helperText, sectionTitle, targetBox);
        dialog.getDialogPane().setContent(container);

        styleDialogButtons(dialog.getDialogPane(), translateType);

        dialog.setResultConverter(buttonType -> buttonType == translateType ? targetBox.getValue() : null);

        dialog.showAndWait().ifPresent(target -> {
            try {
                String translated = translationService.translateAuto(post.getContent(), target.code);
                showTranslationResult(target.name, translated);
            } catch (Exception e) {
                application.showError("Translation failed", e.getMessage());
            }
        });
    }

    private void showTranslationResult(String languageName, String translatedText) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Translated Post");
        dialog.setHeaderText(null);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        styleDialogPane(dialog.getDialogPane());

        Label resultTitle = new Label("Translation in " + languageName);
        resultTitle.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 18px; -fx-font-weight: 900;");

        TextArea area = new TextArea(translatedText);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(12);
        area.setPrefColumnCount(58);
        area.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; "
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1.2; "
                + "-fx-font-size: 14px; -fx-text-fill: #334155; -fx-padding: 12;");

        VBox content = new VBox(12, resultTitle, area);
        content.setPadding(new Insets(2, 2, 2, 2));
        dialog.getDialogPane().setContent(content);

        styleDialogButtons(dialog.getDialogPane(), ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void styleDialogPane(DialogPane pane) {
        pane.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; "
                + "-fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 18;");
        pane.setPrefWidth(560);
    }

    private void styleDialogButtons(DialogPane pane, ButtonType primaryButtonType) {
        Button primaryButton = (Button) pane.lookupButton(primaryButtonType);
        if (primaryButton != null) {
            primaryButton.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; "
                    + "-fx-font-weight: 900; -fx-font-size: 13px; -fx-background-radius: 12; "
                    + "-fx-cursor: hand; -fx-padding: 10 16 10 16;");
        }

        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #334155; "
                    + "-fx-font-weight: 800; -fx-font-size: 13px; -fx-background-radius: 12; "
                    + "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-cursor: hand; "
                    + "-fx-padding: 10 16 10 16;");
        }

        Button closeButton = (Button) pane.lookupButton(ButtonType.CLOSE);
        if (closeButton != null) {
            closeButton.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #334155; "
                    + "-fx-font-weight: 800; -fx-font-size: 13px; -fx-background-radius: 12; "
                    + "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-cursor: hand; "
                    + "-fx-padding: 10 16 10 16;");
        }
    }

    private static final class LanguageOption {
        private final String name;
        private final String code;

        private LanguageOption(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @FXML
    private void handleSummarizePost() {
        if (post == null || post.getContent() == null || post.getContent().isBlank()) {
            application.showError("Summary unavailable", "There is no post content to summarize.");
            return;
        }

        if (!summarizationService.isConfigured()) {
            application.showError(
                    "Summary unavailable",
                    "Missing Hugging Face summary token or endpoint. Set HF_TOKEN and HF_SUMMARY_URL."
            );
            return;
        }

        if (summarySection != null) {
            summarySection.setManaged(true);
            summarySection.setVisible(true);
        }
        if (summaryStatusLabel != null) {
            summaryStatusLabel.setText("Generating summary...");
        }
        if (summaryTextArea != null) {
            summaryTextArea.clear();
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return summarizationService.summarize(post.getContent());
            }
        };

        task.setOnSucceeded(e -> {
            if (summaryTextArea != null) {
                summaryTextArea.setText(task.getValue());
            }
            if (summaryStatusLabel != null) {
                summaryStatusLabel.setText("AI summary ready");
            }
        });

        task.setOnFailed(e -> {
            if (summaryStatusLabel != null) {
                summaryStatusLabel.setText("Unable to generate summary");
            }
            application.showError("Summarization failed", task.getException().getMessage());
        });

        Thread thread = new Thread(task, "post-summary-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void updateSummaryVisibility() {
        boolean show = post != null && summarizationService.shouldSummarize(post.getContent());

        if (summarizePostButton != null) {
            summarizePostButton.setManaged(show);
            summarizePostButton.setVisible(show);
        }

        if (summarySection != null) {
            summarySection.setManaged(false);
            summarySection.setVisible(false);
        }

        if (summaryStatusLabel != null && show) {
            summaryStatusLabel.setText("Long post detected. Tap Summarize to generate a short AI version.");
        }
    }
}
