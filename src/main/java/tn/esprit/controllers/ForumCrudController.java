package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.services.PostService;
import tn.esprit.services.ReplyService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ForumCrudController {

    // ── Hardcoded guest user – replace with your session/auth layer ──
    private static final int GUEST_USER_ID = 1;
    private static final String GUEST_AUTHOR  = "Guest";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final PostService  postService  = new PostService();
    private final ReplyService replyService = new ReplyService();

    // ── New-post form ──
    @FXML private TextField newPostTitleField;
    @FXML private TextField newPostTopicField;
    @FXML private TextArea  newPostContentArea;

    // ── Post list ──
    @FXML private ListView<Post> postListView;

    // ── Post detail ──
    @FXML private Label  postTitleLabel;
    @FXML private Label  postTopicLabel;
    @FXML private Label  postMetaLabel;
    @FXML private Label  postContentLabel;
    @FXML private Button deletePostBtn;

    // ── Replies area ──
    @FXML private ScrollPane repliesScroll;
    @FXML private VBox        repliesContainer;
    @FXML private Label       repliesPlaceholder;
    @FXML private TextArea    replyContentArea;

    // ── Status bar ──
    @FXML private Label statusLabel;

    /** Currently viewed post – null when none selected. */
    private Post currentPost;

    // ─────────────────────────────────────────────────────────────────
    //  Initialisation
    // ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurePostListView();
        loadPosts();
    }

    /** Renders each post as a compact card inside the ListView. */
    private void configurePostListView() {
        postListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);
                if (empty || post == null) {
                    setGraphic(null);
                    return;
                }

                VBox card = new VBox(4);
                card.setStyle("-fx-padding: 10 12 10 12; -fx-background-radius: 6;");

                Label title = new Label(post.getTitle());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #222; -fx-wrap-text: true;");
                title.setMaxWidth(Double.MAX_VALUE);

                HBox meta = new HBox(6);
                if (post.getTopic() != null && !post.getTopic().isBlank()) {
                    Label topic = new Label(post.getTopic());
                    topic.setStyle("-fx-background-color: #e8f0fe; -fx-text-fill: #1a73e8; "
                            + "-fx-padding: 1 6 1 6; -fx-background-radius: 8; -fx-font-size: 10px;");
                    meta.getChildren().add(topic);
                }
                if (post.getCreatedAt() != null) {
                    Label date = new Label(post.getCreatedAt().format(DATE_FMT));
                    date.setStyle("-fx-text-fill: #aaa; -fx-font-size: 10px;");
                    meta.getChildren().add(date);
                }

                card.getChildren().addAll(title, meta);
                setGraphic(card);
            }
        });

        postListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, post) -> showPost(post));
    }

    // ─────────────────────────────────────────────────────────────────
    //  FXML handlers
    // ─────────────────────────────────────────────────────────────────

    /** Creates a new post from the left-panel form. */
    @FXML
    private void handleCreatePost() {
        String title   = newPostTitleField.getText().trim();
        String topic   = newPostTopicField.getText().trim();
        String content = newPostContentArea.getText().trim();

        if (title.isEmpty()) {
            setStatus("⚠ Please enter a title.");
            newPostTitleField.requestFocus();
            return;
        }
        if (content.isEmpty()) {
            setStatus("⚠ Please enter some content.");
            newPostContentArea.requestFocus();
            return;
        }

        try {
            Post post = new Post();
            post.setTitle(title);
            post.setTopic(topic.isEmpty() ? "General" : topic);
            post.setContent(content);
            post.setType("discussion");
            post.setUserId(GUEST_USER_ID);
            LocalDateTime now = LocalDateTime.now();
            post.setCreatedAt(now);
            post.setUpdatedAt(now);

            postService.add(post);

            newPostTitleField.clear();
            newPostTopicField.clear();
            newPostContentArea.clear();

            loadPosts();
            // Select the new post automatically
            postListView.getItems().stream()
                    .filter(p -> p.getId().equals(post.getId()))
                    .findFirst()
                    .ifPresent(p -> postListView.getSelectionModel().select(p));

            setStatus("✓ Post published.");
        } catch (Exception e) {
            showError("Failed to create post", e.getMessage());
        }
    }

    /** Deletes the currently viewed post after confirmation. */
    @FXML
    private void handleDeletePost() {
        if (currentPost == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Post");
        confirm.setHeaderText("Delete \"" + currentPost.getTitle() + "\"?");
        confirm.setContentText("All replies will also be deleted. This cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                postService.delete(currentPost.getId());
                currentPost = null;
                clearPostDetail();
                loadPosts();
                setStatus("✓ Post deleted.");
            } catch (Exception e) {
                showError("Failed to delete post", e.getMessage());
            }
        }
    }

    /** Submits a reply to the currently viewed post. */
    @FXML
    private void handleSubmitReply() {
        if (currentPost == null) {
            setStatus("⚠ Select a post first.");
            return;
        }

        String content = replyContentArea.getText().trim();
        if (content.isEmpty()) {
            setStatus("⚠ Reply cannot be empty.");
            replyContentArea.requestFocus();
            return;
        }

        try {
            Reply reply = new Reply();
            reply.setPostId(currentPost.getId());
            reply.setContent(content);
            reply.setAuthorName(GUEST_AUTHOR);
            reply.setUpvotes(0);
            reply.setUserId(GUEST_USER_ID);
            LocalDateTime now = LocalDateTime.now();
            reply.setCreatedAt(now);
            reply.setUpdatedAt(now);

            replyService.add(reply);
            replyContentArea.clear();
            loadRepliesFor(currentPost);
            setStatus("✓ Reply posted.");

            // Scroll to bottom so the new reply is visible
            repliesScroll.setVvalue(1.0);
        } catch (Exception e) {
            showError("Failed to post reply", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────

    private void loadPosts() {
        try {
            List<Post> posts = postService.getAll();
            postListView.getItems().setAll(posts);
            setStatus(posts.size() + " post(s) loaded.");
        } catch (SQLException e) {
            showError("Failed to load posts", e.getMessage());
        }
    }

    /** Populates the right panel with the selected post and its replies. */
    private void showPost(Post post) {
        if (post == null) return;
        currentPost = post;

        postTitleLabel.setText(post.getTitle());
        postTopicLabel.setText(post.getTopic() == null || post.getTopic().isBlank()
                ? "General" : post.getTopic());
        postContentLabel.setText(post.getContent());
        postMetaLabel.setText(post.getCreatedAt() != null
                ? "Posted " + post.getCreatedAt().format(DATE_FMT) : "");
        deletePostBtn.setVisible(true);

        loadRepliesFor(post);
    }

    /** Renders the replies for the given post inside the scrollable VBox. */
    private void loadRepliesFor(Post post) {
        repliesContainer.getChildren().clear();

        try {
            List<Reply> replies = replyService.getByPostId(post.getId());

            if (replies.isEmpty()) {
                repliesContainer.getChildren().add(repliesPlaceholder);
                repliesPlaceholder.setVisible(true);
                return;
            }

            repliesPlaceholder.setVisible(false);

            Label heading = new Label(replies.size() + " repl" + (replies.size() == 1 ? "y" : "ies"));
            heading.setStyle("-fx-text-fill: #888; -fx-font-size: 12px; -fx-padding: 0 0 4 0;");
            repliesContainer.getChildren().add(heading);

            for (Reply reply : replies) {
                repliesContainer.getChildren().add(buildReplyCard(reply));
            }

        } catch (SQLException e) {
            Label err = new Label("Could not load replies.");
            err.setStyle("-fx-text-fill: #c00;");
            repliesContainer.getChildren().add(err);
        }
    }

    /** Builds a single reply card node. */
    private VBox buildReplyCard(Reply reply) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: white; -fx-padding: 12 14 12 14; "
                + "-fx-background-radius: 8; -fx-effect: dropshadow(gaussian, #0000000f, 4, 0, 0, 1);");
        VBox.setMargin(card, new Insets(0, 0, 0, 0));

        // Author + date row
        HBox header = new HBox(8);
        header.setStyle("-fx-alignment: CENTER_LEFT;");

        Label avatar = new Label(getInitial(reply.getAuthorName()));
        avatar.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-font-size: 11px; "
                + "-fx-min-width: 28; -fx-min-height: 28; -fx-max-width: 28; -fx-max-height: 28; "
                + "-fx-background-radius: 14; -fx-alignment: CENTER;");

        Label author = new Label(reply.getAuthorName() == null || reply.getAuthorName().isBlank()
                ? "Anonymous" : reply.getAuthorName());
        author.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #333;");

        Label date = new Label(reply.getCreatedAt() != null
                ? reply.getCreatedAt().format(DATE_FMT) : "");
        date.setStyle("-fx-text-fill: #bbb; -fx-font-size: 10px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Delete button for replies
        Button delBtn = new Button("✕");
        String delNormal = "-fx-background-color: transparent; -fx-text-fill: #ccc; "
                + "-fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4 0 4;";
        String delHover  = "-fx-background-color: transparent; -fx-text-fill: #c00; "
                + "-fx-cursor: hand; -fx-font-size: 11px; -fx-padding: 0 4 0 4;";
        delBtn.setStyle(delNormal);
        delBtn.setOnMouseEntered(e -> delBtn.setStyle(delHover));
        delBtn.setOnMouseExited(e -> delBtn.setStyle(delNormal));
        delBtn.setOnAction(e -> handleDeleteReply(reply));

        header.getChildren().addAll(avatar, author, spacer, date, delBtn);

        // Content
        Label content = new Label(reply.getContent());
        content.setStyle("-fx-font-size: 13px; -fx-text-fill: #444; -fx-wrap-text: true;");
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(content, Priority.ALWAYS);

        // Upvotes
        HBox footer = new HBox(4);
        footer.setStyle("-fx-alignment: CENTER_LEFT; -fx-padding: 4 0 0 0;");
        int votes = reply.getUpvotes() == null ? 0 : reply.getUpvotes();
        Button upBtn = new Button("▲ " + votes);
        upBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1a73e8; "
                + "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0;");
        upBtn.setOnAction(e -> handleUpvote(reply, upBtn));
        footer.getChildren().add(upBtn);

        card.getChildren().addAll(header, content, footer);
        return card;
    }

    private void handleDeleteReply(Reply reply) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Reply");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this reply?");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                replyService.delete(reply.getId());
                loadRepliesFor(currentPost);
                setStatus("✓ Reply deleted.");
            } catch (Exception e) {
                showError("Failed to delete reply", e.getMessage());
            }
        }
    }

    private void handleUpvote(Reply reply, Button btn) {
        try {
            int current = reply.getUpvotes() == null ? 0 : reply.getUpvotes();
            reply.setUpvotes(current + 1);
            reply.setUpdatedAt(LocalDateTime.now());
            replyService.update(reply);
            btn.setText("▲ " + reply.getUpvotes());
        } catch (Exception e) {
            showError("Failed to upvote", e.getMessage());
        }
    }

    private void clearPostDetail() {
        postTitleLabel.setText("Select a post to read it");
        postTopicLabel.setText("");
        postMetaLabel.setText("");
        postContentLabel.setText("");
        deletePostBtn.setVisible(false);
        repliesContainer.getChildren().clear();
        repliesContainer.getChildren().add(repliesPlaceholder);
        repliesPlaceholder.setVisible(true);
    }

    private String getInitial(String name) {
        if (name == null || name.isBlank()) return "?";
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        setStatus("✗ " + title);
    }
}