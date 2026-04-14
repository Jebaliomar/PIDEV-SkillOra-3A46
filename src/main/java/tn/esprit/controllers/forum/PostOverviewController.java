package tn.esprit.controllers.forum;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.esprit.entities.Post;
import tn.esprit.mains.ForumCrudLauncher;

import java.sql.SQLException;
import java.util.List;

public class PostOverviewController {

    private static final int PREVIEW_LIMIT = 180;

    @FXML private VBox postsContainer;
    @FXML private Label currentUserLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterComboBox;
    @FXML private ComboBox<String> topicFilterComboBox;

    private ForumCrudLauncher application;

    public void setApplication(ForumCrudLauncher application) {
        this.application = application;
        currentUserLabel.setText("Current user: " + application.getCurrentUserDisplay());
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

            if (posts.isEmpty()) {
                postsContainer.getChildren().add(createEmptyState("No posts match the current search and filters."));
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

    @FXML private void handleCreatePost() { application.showCreatePostScene(); }
    @FXML private void handleSearch()     { loadPosts(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        typeFilterComboBox.setValue("All types");
        topicFilterComboBox.setValue("All topics");
        loadPosts();
    }

    private void initializeFilters() {
        typeFilterComboBox.getItems().clear();
        topicFilterComboBox.getItems().clear();
        typeFilterComboBox.getItems().add("All types");
        topicFilterComboBox.getItems().add("All topics");

        try {
            typeFilterComboBox.getItems().addAll(application.getPostService().getDistinctTypes());
            topicFilterComboBox.getItems().addAll(application.getPostService().getDistinctTopics());
        } catch (SQLException exception) {
            application.showError("Loading filters failed", exception.getMessage());
        }

        typeFilterComboBox.setValue("All types");
        topicFilterComboBox.setValue("All topics");
    }

    // ── Card builder ─────────────────────────────────────────────────────────

    private VBox createPostCard(Post post) {
        VBox card = new VBox(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #e2e8f0;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        );

        // ── Thin top colour strip ─────────────────────────────
        Region topStrip = new Region();
        topStrip.setPrefHeight(4);
        topStrip.setMinHeight(4);
        topStrip.setMaxHeight(4);
        topStrip.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #3b82f6);");
        // make strip corners match card
        topStrip.setStyle(
                "-fx-background-color: linear-gradient(to right, #6366f1, #3b82f6);" +
                        "-fx-background-radius: 14 14 0 0;"
        );

        // ── Body ─────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 22, 14, 22));

        // Badge row
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        String typeText  = valueOrFallback(post.getType(),  null);
        String topicText = valueOrFallback(post.getTopic(), null);

        if (typeText != null) {
            badges.getChildren().add(makeBadge(typeText,
                    "#eff6ff", "#1d4ed8"));
        }
        if (topicText != null) {
            badges.getChildren().add(makeBadge(topicText,
                    "#f0fdf4", "#15803d"));
        }

        // Title
        Label titleLabel = new Label(valueOrFallback(post.getTitle(), "Untitled post"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #0f172a;");
        titleLabel.setWrapText(true);

        // Content preview
        Label previewLabel = new Label(truncate(post.getContent(), PREVIEW_LIMIT));
        previewLabel.setWrapText(true);
        previewLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        if (!badges.getChildren().isEmpty()) body.getChildren().add(badges);
        body.getChildren().addAll(titleLabel, previewLabel);

        // ── Divider ───────────────────────────────────────────
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: #f1f5f9;");

        // ── Footer ────────────────────────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(11, 20, 14, 22));

        // Avatar + author
        String author = application.resolveUsername(post.getUserId());
        String initial = (author != null && !author.isEmpty())
                ? author.substring(0, 1).toUpperCase() : "?";

        Label avatar = new Label(initial);
        avatar.setStyle(
                "-fx-background-color: #ede9fe;" +
                        "-fx-text-fill: #6366f1;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 11px;" +
                        "-fx-background-radius: 50;" +
                        "-fx-min-width: 26; -fx-max-width: 26;" +
                        "-fx-min-height: 26; -fx-max-height: 26;" +
                        "-fx-alignment: center;"
        );

        Label userLabel = new Label(author);
        userLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openButton = new Button("Open Post  →");
        openButton.setStyle(
                "-fx-background-color: #6366f1;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 8 18 8 18;"
        );
        openButton.setOnAction(e -> application.showPostDetailsScene(post.getId()));

        footer.getChildren().addAll(avatar, userLabel, spacer, openButton);

        card.getChildren().addAll(topStrip, body, divider, footer);
        card.setOnMouseClicked(e -> application.showPostDetailsScene(post.getId()));

        return card;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Label makeBadge(String text, String bgColor, String textColor) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 3 10 3 10;"
        );
        return badge;
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.setStyle(
                "-fx-text-fill: #94a3b8;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 40 0 40 0;"
        );
        label.setWrapText(true);
        return label;
    }

    private String truncate(String value, int maxLength) {
        String s = valueOrFallback(value, "");
        return s.length() <= maxLength ? s : s.substring(0, maxLength).trim() + "…";
    }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String selectedFilterValue(ComboBox<String> cb) {
        String v = cb.getValue();
        return (v == null || v.startsWith("All ")) ? null : v;
    }
}