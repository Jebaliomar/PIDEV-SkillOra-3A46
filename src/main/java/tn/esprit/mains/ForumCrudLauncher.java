package tn.esprit.mains;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.services.PostService;
import tn.esprit.services.ReplyService;
import tn.esprit.services.UserService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForumCrudLauncher extends Application {

    private static final double WINDOW_WIDTH = 980;
    private static final double WINDOW_HEIGHT = 720;
    private static final int PREVIEW_LIMIT = 180;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Stage primaryStage;
    private PostService postService;
    private ReplyService replyService;
    private UserService userService;
    private final Map<Integer, String> usernameCache = new HashMap<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            this.postService = new PostService();
            this.replyService = new ReplyService();
            this.userService = new UserService();
        } catch (RuntimeException exception) {
            showError("Database connection failed", exception.getMessage());
            throw exception;
        }

        primaryStage.setTitle("SkillOra Forum");
        showOverviewScene();
        primaryStage.show();
    }

    private void showOverviewScene() {
        BorderPane root = createBaseLayout("Posts Overview");

        Button createPostButton = new Button("Create Post");
        createPostButton.setOnAction(event -> showCreatePostScene());
        root.setTop(createHeader("Posts Overview", "Browse posts and open one to see its details and replies.", null, createPostButton));

        VBox postsContainer = new VBox(14);
        postsContainer.setPadding(new Insets(20));

        try {
            List<Post> posts = postService.getAll();
            if (posts.isEmpty()) {
                postsContainer.getChildren().add(createEmptyState("No posts found in the database."));
            } else {
                for (Post post : posts) {
                    postsContainer.getChildren().add(createPostCard(post));
                }
            }
        } catch (SQLException exception) {
            postsContainer.getChildren().add(createEmptyState("Unable to load posts."));
            showError("Loading posts failed", exception.getMessage());
        }

        ScrollPane scrollPane = new ScrollPane(postsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private void showCreatePostScene() {
        BorderPane root = createBaseLayout("Create Post");

        Button backButton = new Button("Back to Posts");
        backButton.setOnAction(event -> showOverviewScene());
        root.setTop(createHeader("Create Post", "Create a new forum post with all post entity information.", backButton, null));

        VBox form = new VBox(12);
        form.setPadding(new Insets(20));
        form.setMaxWidth(720);

        TextField idField = createReadOnlyField("Generated automatically after save");
        TextField typeField = new TextField();
        typeField.setPromptText("Type");
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextField topicField = new TextField();
        topicField.setPromptText("Topic");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Content");
        contentArea.setPrefRowCount(8);
        TextField createdAtField = createReadOnlyField("Auto-filled on save");
        TextField updatedAtField = createReadOnlyField("Auto-filled on save");
        TextField userIdField = new TextField();
        userIdField.setPromptText("User ID");

        form.getChildren().addAll(
                createFieldBlock("ID", idField),
                createFieldBlock("Type", typeField),
                createFieldBlock("Title", titleField),
                createFieldBlock("Topic", topicField),
                createFieldBlock("Content", contentArea),
                createFieldBlock("Created At", createdAtField),
                createFieldBlock("Updated At", updatedAtField),
                createFieldBlock("User ID", userIdField)
        );

        Button saveButton = new Button("Save Post");
        saveButton.setOnAction(event -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                Post post = new Post();
                post.setType(typeField.getText().trim());
                post.setTitle(titleField.getText().trim());
                post.setTopic(topicField.getText().trim());
                post.setContent(contentArea.getText().trim());
                post.setCreatedAt(now);
                post.setUpdatedAt(now);
                post.setUserId(parseNullableInteger(userIdField.getText(), "User ID"));

                postService.add(post);

                idField.setText(String.valueOf(post.getId()));
                createdAtField.setText(formatDateTime(post.getCreatedAt()));
                updatedAtField.setText(formatDateTime(post.getUpdatedAt()));

                showInfo("Post created", "Post #" + post.getId() + " was created successfully.");
                showOverviewScene();
            } catch (IllegalArgumentException exception) {
                showError("Invalid input", exception.getMessage());
            } catch (SQLException exception) {
                showError("Saving post failed", exception.getMessage());
            }
        });

        VBox content = new VBox(16, form, saveButton);
        content.setPadding(new Insets(0, 20, 20, 20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);

        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private void showPostDetailsScene(int postId) {
        BorderPane root = createBaseLayout("Post Details");

        Button backButton = new Button("Back to Posts");
        backButton.setOnAction(event -> showOverviewScene());
        root.setTop(createHeader("Post Details", "View the full post information and add replies below it.", backButton, null));

        VBox content = new VBox(18);
        content.setPadding(new Insets(20));

        try {
            Post post = postService.getById(postId);
            if (post == null) {
                content.getChildren().add(createEmptyState("The selected post was not found."));
            } else {
                content.getChildren().add(createPostDetailsCard(post));
                content.getChildren().add(new Separator());
                content.getChildren().add(createRepliesSection(post));
            }
        } catch (SQLException exception) {
            content.getChildren().add(createEmptyState("Unable to load the selected post."));
            showError("Loading post failed", exception.getMessage());
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        root.setCenter(scrollPane);

        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    private VBox createRepliesSection(Post post) throws SQLException {
        VBox repliesSection = new VBox(14);

        Label repliesTitle = new Label("Replies");
        repliesTitle.setFont(Font.font("System", FontWeight.BOLD, 20));

        VBox repliesList = new VBox(10);
        List<Reply> replies = replyService.getByPostId(post.getId());
        if (replies.isEmpty()) {
            repliesList.getChildren().add(createEmptyState("No replies yet for this post."));
        } else {
            for (Reply reply : replies) {
                repliesList.getChildren().add(createReplyCard(reply));
            }
        }

        VBox form = new VBox(10);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color: #f7f9fc; -fx-background-radius: 12; -fx-border-color: #d7deea; -fx-border-radius: 12;");

        TextField postIdField = createReadOnlyField(String.valueOf(post.getId()));
        TextField parentIdField = new TextField();
        parentIdField.setPromptText("Optional parent reply ID");
        TextField authorNameField = new TextField();
        authorNameField.setPromptText("Author name");
        TextField upvotesField = new TextField("0");
        TextField createdAtField = createReadOnlyField("Auto-filled on save");
        TextField updatedAtField = createReadOnlyField("Auto-filled on save");
        TextField userIdField = new TextField();
        userIdField.setPromptText("Optional user ID");
        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Reply content");
        contentArea.setPrefRowCount(5);

        form.getChildren().addAll(
                createFieldBlock("Post ID", postIdField),
                createFieldBlock("Parent ID", parentIdField),
                createFieldBlock("Author Name", authorNameField),
                createFieldBlock("Content", contentArea),
                createFieldBlock("Upvotes", upvotesField),
                createFieldBlock("Created At", createdAtField),
                createFieldBlock("Updated At", updatedAtField),
                createFieldBlock("User ID", userIdField)
        );

        Button submitReplyButton = new Button("Add Reply");
        submitReplyButton.setOnAction(event -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                Reply reply = new Reply();
                reply.setPostId(post.getId());
                reply.setParentId(parseNullableInteger(parentIdField.getText(), "Parent ID"));
                reply.setContent(contentArea.getText().trim());
                reply.setAuthorName(authorNameField.getText().trim().isEmpty() ? "Anonymous" : authorNameField.getText().trim());
                reply.setUpvotes(parseIntegerOrDefault(upvotesField.getText(), 0, "Upvotes"));
                reply.setCreatedAt(now);
                reply.setUpdatedAt(now);
                reply.setUserId(parseNullableInteger(userIdField.getText(), "User ID"));

                replyService.add(reply);

                createdAtField.setText(formatDateTime(reply.getCreatedAt()));
                updatedAtField.setText(formatDateTime(reply.getUpdatedAt()));
                showInfo("Reply added", "Reply #" + reply.getId() + " was added successfully.");
                showPostDetailsScene(post.getId());
            } catch (IllegalArgumentException exception) {
                showError("Invalid input", exception.getMessage());
            } catch (SQLException exception) {
                showError("Saving reply failed", exception.getMessage());
            }
        });

        repliesSection.getChildren().addAll(repliesTitle, repliesList, new Separator(), new Label("Add Reply"), form, submitReplyButton);
        return repliesSection;
    }

    private VBox createPostCard(Post post) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #d7deea; -fx-border-radius: 14;");

        Label titleLabel = new Label(nullToPlaceholder(post.getTitle(), "Untitled post"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setWrapText(true);

        Label userLabel = new Label("User: " + getUsername(post.getUserId()));
        Label topicLabel = new Label("Topic: " + nullToPlaceholder(post.getTopic(), "No topic"));
        Label previewLabel = new Label(truncate(post.getContent(), PREVIEW_LIMIT));
        previewLabel.setWrapText(true);

        Button openButton = new Button("Open Post");
        openButton.setOnAction(event -> showPostDetailsScene(post.getId()));

        card.setOnMouseClicked(event -> showPostDetailsScene(post.getId()));
        card.getChildren().addAll(titleLabel, userLabel, topicLabel, previewLabel, openButton);
        return card;
    }

    private VBox createPostDetailsCard(Post post) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(18));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #d7deea; -fx-border-radius: 14;");

        Label titleLabel = new Label(nullToPlaceholder(post.getTitle(), "Untitled post"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        titleLabel.setWrapText(true);

        card.getChildren().addAll(
                titleLabel,
                createValueLine("ID", String.valueOf(post.getId())),
                createValueLine("Type", nullToPlaceholder(post.getType(), "-")),
                createValueLine("Topic", nullToPlaceholder(post.getTopic(), "-")),
                createValueLine("Content", nullToPlaceholder(post.getContent(), "-")),
                createValueLine("User ID", post.getUserId() == null ? "-" : String.valueOf(post.getUserId())),
                createValueLine("Username", getUsername(post.getUserId())),
                createValueLine("Created At", formatDateTime(post.getCreatedAt())),
                createValueLine("Updated At", formatDateTime(post.getUpdatedAt()))
        );

        return card;
    }

    private VBox createReplyCard(Reply reply) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12; -fx-border-color: #d7deea; -fx-border-radius: 12;");

        Label authorLabel = new Label(nullToPlaceholder(reply.getAuthorName(), "Anonymous"));
        authorLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label contentLabel = new Label(nullToPlaceholder(reply.getContent(), "-"));
        contentLabel.setWrapText(true);

        card.getChildren().addAll(
                authorLabel,
                contentLabel,
                createValueLine("Reply ID", String.valueOf(reply.getId())),
                createValueLine("Parent ID", reply.getParentId() == null ? "-" : String.valueOf(reply.getParentId())),
                createValueLine("User ID", reply.getUserId() == null ? "-" : String.valueOf(reply.getUserId())),
                createValueLine("Upvotes", reply.getUpvotes() == null ? "0" : String.valueOf(reply.getUpvotes())),
                createValueLine("Created At", formatDateTime(reply.getCreatedAt())),
                createValueLine("Updated At", formatDateTime(reply.getUpdatedAt()))
        );

        return card;
    }

    private BorderPane createBaseLayout(String title) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(0));
        root.setStyle("-fx-background-color: #eef3f8;");
        primaryStage.setTitle("SkillOra Forum - " + title);
        return root;
    }

    private HBox createHeader(String title, String subtitle, Button leftButton, Button rightButton) {
        VBox textBox = new VBox(6);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-text-fill: #52627a;");
        subtitleLabel.setWrapText(true);
        textBox.getChildren().addAll(titleLabel, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: white; -fx-border-color: transparent transparent #d7deea transparent;");

        if (leftButton != null) {
            header.getChildren().add(leftButton);
        }
        header.getChildren().addAll(textBox, spacer);
        if (rightButton != null) {
            header.getChildren().add(rightButton);
        }

        return header;
    }

    private VBox createFieldBlock(String labelText, javafx.scene.Node field) {
        VBox block = new VBox(6);
        Label label = new Label(labelText);
        label.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
        block.getChildren().addAll(label, field);
        return block;
    }

    private Label createValueLine(String label, String value) {
        Label valueLabel = new Label(label + ": " + value);
        valueLabel.setWrapText(true);
        return valueLabel;
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #52627a; -fx-font-size: 15;");
        label.setWrapText(true);
        return label;
    }

    private TextField createReadOnlyField(String value) {
        TextField field = new TextField(value);
        field.setEditable(false);
        return field;
    }

    private Integer parseNullableInteger(String value, String fieldName) {
        String trimmedValue = value == null ? "" : value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(trimmedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private Integer parseIntegerOrDefault(String value, int defaultValue, String fieldName) {
        String trimmedValue = value == null ? "" : value.trim();
        if (trimmedValue.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(trimmedValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number.");
        }
    }

    private String truncate(String value, int maxLength) {
        String safeValue = nullToPlaceholder(value, "");
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength).trim() + "...";
    }

    private String nullToPlaceholder(String value, String placeholder) {
        return value == null || value.isBlank() ? placeholder : value;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMATTER.format(dateTime);
    }

    private String getUsername(Integer userId) {
        if (userId == null) {
            return "Unknown user";
        }

        if (usernameCache.containsKey(userId)) {
            return usernameCache.get(userId);
        }

        try {
            String username = userService.getUsernameById(userId);
            usernameCache.put(userId, username);
            return username;
        } catch (SQLException exception) {
            return "User #" + userId;
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
