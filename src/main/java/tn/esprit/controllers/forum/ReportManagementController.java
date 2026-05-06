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

public class ReportManagementController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PREVIEW_LIMIT = 220;

    @FXML private VBox postsContainer;
    @FXML private HBox statsContainer;
    @FXML private Label currentUserLabel;
    @FXML private Label postCountLabel;
    @FXML private AdminSidebarController adminSidebarController;

    private ForumCrudLauncher application;
    private String activeFilter = "pending";

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
        rebuildFilterChips();
    }

    public void loadReports() {
        if (postsContainer == null) {
            return;
        }

        postsContainer.getChildren().clear();
        try {
            List<Report> reports = loadFilteredReports();
            if (postCountLabel != null) {
                postCountLabel.setText(reports.size() + (reports.size() == 1 ? " report" : " reports"));
            }

            if (reports.isEmpty()) {
                postsContainer.getChildren().add(emptyStateLabel("No reported content matches the selected filter."));
                return;
            }

            for (Report report : reports) {
                postsContainer.getChildren().add(createReportCard(report));
            }
        } catch (SQLException e) {
            postsContainer.getChildren().add(emptyStateLabel("Unable to load reports."));
            application.showError("Loading reports failed", e.getMessage());
        }
    }

    @FXML private void handleBack() {
        application.showAdminPostManagementScene();
    }

    @FXML private void handleRefresh() {
        loadReports();
        rebuildFilterChips();
    }

    private void rebuildFilterChips() {
        if (statsContainer == null) {
            return;
        }

        statsContainer.getChildren().clear();

        try {
            statsContainer.getChildren().addAll(
                    filterChip("Pending", "pending", application.getReportService().countByStatus("pending"), "#FEF3C7", "#D97706"),
                    filterChip("Reviewed", "reviewed", application.getReportService().countByStatus("reviewed"), "#E0E7FF", "#4F46E5"),
                    filterChip("Dismissed", "dismissed", application.getReportService().countByStatus("dismissed"), "#FEE2E2", "#DC2626"),
                    filterChip("All Reports", "all", application.getReportService().countAll(), "#FFFFFF", "#334155")
            );
        } catch (SQLException e) {
            statsContainer.getChildren().add(emptyStateLabel("Unable to load report summary."));
            application.showError("Loading summary failed", e.getMessage());
        }
    }

    private Button filterChip(String label, String filter, int count, String background, String foreground) {
        Button chip = new Button(label + "  " + count);
        chip.setOnAction(e -> {
            activeFilter = filter;
            loadReports();
            rebuildFilterChips();
        });

        boolean selected = activeFilter.equalsIgnoreCase(filter);
        chip.setStyle(
                "-fx-background-color: " + (selected ? background : "white") + ";" +
                        "-fx-text-fill: " + foreground + ";" +
                        "-fx-font-weight: 800;" +
                        "-fx-font-size: 12.5px;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + (selected ? "transparent" : "rgba(226,232,240,0.9)") + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 14;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 16 10 16;"
        );
        return chip;
    }

    private List<Report> loadFilteredReports() throws SQLException {
        if (activeFilter == null || activeFilter.equalsIgnoreCase("all")) {
            return application.getReportService().getAll();
        }
        return application.getReportService().getByStatus(activeFilter);
    }

    private VBox createReportCard(Report report) {
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
        String reporterInitial = initialOf(reporterName);

        Label avatar = new Label(reporterInitial);
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

        Label metaLine = new Label(humanReadableDate(report.getCreatedAt()) + "  ·  " + targetSummary(report));
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
                rebuildFilterChips();
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
                rebuildFilterChips();
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
            rebuildFilterChips();
            application.showInfo("Deleted", "The reported content was deleted successfully.");
        } catch (SQLException e) {
            application.showError("Deletion failed", e.getMessage());
        }
    }

    private Button actionButton(String text, String bg, String fg) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-font-weight: 800;" +
                        "-fx-font-size: 12.5px;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 16 10 16;"
        );
        return btn;
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
                return truncate(valueOrFallback(reply.getContent(), "–"), PREVIEW_LIMIT);
            }

            Post post = application.getPostService().getById(report.getPostId());
            if (post == null) {
                return "The reported post is no longer available.";
            }
            return truncate(valueOrFallback(post.getContent(), "–"), PREVIEW_LIMIT);
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

    private Label emptyStateLabel(String message) {
        Label lbl = new Label(message);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px; -fx-padding: 40 0 40 0;");
        return lbl;
    }

    private String humanReadableDate(LocalDateTime dt) {
        return dt == null ? "–" : DATE_FMT.format(dt);
    }

    private String initialOf(String value) {
        String text = valueOrFallback(value, "?");
        return text.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String truncate(String value, int max) {
        String text = valueOrFallback(value, "");
        return text.length() <= max ? text : text.substring(0, max).trim() + "…";
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
