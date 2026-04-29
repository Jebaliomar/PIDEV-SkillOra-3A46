package tn.esprit.services;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import tn.esprit.entities.Post;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ShareService {

    public void showShareDialog(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("Post is null.");
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Share Post");
        dialog.setHeaderText("Share this post across platforms");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setPrefWidth(520);

        String shareText = buildShareText(post);
        String shareUrl = buildShareUrl(post);
        String whatsappMessage = buildWhatsAppMessage(shareText, shareUrl, post.getImageUrl());
        String facebookQuote = buildFacebookQuote(shareText, post.getImageUrl());

        Label title = new Label("Choose where to share");
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 16px; -fx-font-weight: 900;");

        Label subtitle = new Label("Share text, link, and image context in one click.");
        subtitle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label note = new Label("Note: Facebook card preview comes from the shared URL metadata.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        Button whatsapp = new Button("WhatsApp");
        whatsapp.setMaxWidth(Double.MAX_VALUE);
        whatsapp.setOnAction(e -> openUri("https://api.whatsapp.com/send?text=" + encode(whatsappMessage)));

        Button telegram = new Button("Telegram");
        telegram.setMaxWidth(Double.MAX_VALUE);
        telegram.setOnAction(e -> openUri("https://t.me/share/url?url=" + encode(shareUrlOrFallback(shareUrl)) + "&text=" + encode(shareText)));

        Button x = new Button("X");
        x.setMaxWidth(Double.MAX_VALUE);
        x.setOnAction(e -> openUri("https://twitter.com/intent/tweet?text=" + encode(shareText) + "&url=" + encode(shareUrlOrFallback(shareUrl))));

        Button facebook = new Button("Facebook");
        facebook.setMaxWidth(Double.MAX_VALUE);
        facebook.setDisable(shareUrl == null || shareUrl.isBlank());
        facebook.setOnAction(e -> openUri(
            "https://www.facebook.com/sharer/sharer.php?u="
                + encode(shareUrl)
                + "&quote="
                + encode(facebookQuote)
        ));

        Button copy = new Button("Copy text + link");
        copy.setMaxWidth(Double.MAX_VALUE);
        copy.setOnAction(e -> copyToClipboard(whatsappMessage));

        styleShareButton(whatsapp, "#25D366", "white");
        styleShareButton(telegram, "#229ED9", "white");
        styleShareButton(x, "#0f172a", "white");
        styleShareButton(facebook, "#1877F2", "white");
        styleShareButton(copy, "#f8fafc", "#334155");

        VBox content = new VBox(10, title, subtitle, note, whatsapp, telegram, x, facebook, copy);
        content.setStyle("-fx-background-color: white; -fx-padding: 8 2 2 2;");
        pane.setContent(content);

        dialog.showAndWait();
    }

    private String buildShareText(Post post) {
        String title = valueOrFallback(post.getTitle(), "Untitled post");
        String topic = valueOrFallback(post.getTopic(), "");
        String preview = truncate(valueOrFallback(post.getContent(), ""), 160);

        StringBuilder sb = new StringBuilder();
        sb.append(title);
        if (!topic.isBlank()) {
            sb.append(" #").append(topic);
        }
        if (!preview.isBlank()) {
            sb.append("\n\n").append(preview);
        }
        sb.append("\n\nShared from SkillOra Forum");

        return sb.toString();
    }

    private String buildShareUrl(Post post) {
        String imageUrl = post.getImageUrl();
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl.trim();
        }
        return "";
    }

    private String buildWhatsAppMessage(String shareText, String shareUrl, String imageUrl) {
        StringBuilder sb = new StringBuilder(valueOrFallback(shareText, ""));

        String primaryUrl = valueOrFallback(shareUrl, "");
        if (!primaryUrl.isBlank()) {
            sb.append("\n\n").append(primaryUrl);
        }

        String mediaUrl = valueOrFallback(imageUrl, "");
        if (!mediaUrl.isBlank() && !mediaUrl.equals(primaryUrl)) {
            sb.append("\nImage: ").append(mediaUrl);
        }

        return sb.toString().trim();
    }

    private String buildFacebookQuote(String shareText, String imageUrl) {
        StringBuilder sb = new StringBuilder(valueOrFallback(shareText, ""));
        String mediaUrl = valueOrFallback(imageUrl, "");
        if (!mediaUrl.isBlank()) {
            sb.append("\nImage: ").append(mediaUrl);
        }
        return truncate(sb.toString().trim(), 350);
    }

    private String shareUrlOrFallback(String shareUrl) {
        return (shareUrl == null || shareUrl.isBlank())
                ? "https://example.com"
                : shareUrl;
    }

    private void copyToClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void openUri(String uri) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(uri));
            } else {
                throw new IllegalStateException("Your system does not support opening the browser.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to open share link.", e);
        }
    }

    private void styleShareButton(Button button, String bg, String fg) {
        button.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-font-weight: 800;" +
                "-fx-font-size: 12px;" +
                "-fx-background-radius: 12;" +
                "-fx-border-color: transparent;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 14 10 14;"
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max).trim() + "…";
    }

    private String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }
}