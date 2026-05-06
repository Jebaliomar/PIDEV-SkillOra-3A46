package tn.esprit.tools;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public final class BotpressSupportWidget {

    private static final String WIDGET_ID = "botpressSupportWidget";
    private static final double LAUNCHER_WIDTH = 96;
    private static final double LAUNCHER_HEIGHT = 96;
    private static final double EXPANDED_WIDTH = 430;
    private static final double EXPANDED_HEIGHT = 640;
    private static final double GAP = 16;

    private BotpressSupportWidget() {
    }

    public static void installForFrontendPage(Node anchor) {
        if (anchor == null) {
            return;
        }

        anchor.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> install(anchor, newScene));
            }
        });

        Scene scene = anchor.getScene();
        if (scene != null) {
            Platform.runLater(() -> install(anchor, scene));
        }
    }

    private static void install(Node anchor, Scene scene) {
        StackPane studentContentArea = findStudentContentArea(anchor);
        if (studentContentArea != null) {
            installInStackPane(studentContentArea);
            return;
        }

        Parent root = scene.getRoot();
        if (root == null || root.lookup("#" + WIDGET_ID) != null) {
            return;
        }

        if (root instanceof StackPane stackPane) {
            installInStackPane(stackPane);
            return;
        }

        StackPane wrappedRoot = new StackPane();
        wrappedRoot.getStyleClass().addAll(root.getStyleClass());
        scene.setRoot(wrappedRoot);
        wrappedRoot.getChildren().add(root);
        installInStackPane(wrappedRoot);
    }

    private static StackPane findStudentContentArea(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof StackPane stackPane && stackPane.getStyleClass().contains("content-area")) {
                return stackPane;
            }
            current = current.getParent();
        }
        return null;
    }

    private static void installInStackPane(StackPane host) {
        if (host.lookup("#" + WIDGET_ID) != null) {
            return;
        }

        StackPane widget = createWidget();
        StackPane.setAlignment(widget, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(widget, new Insets(0, GAP, GAP, 0));
        host.getChildren().add(widget);
    }

    private static StackPane createWidget() {
        WebView webView = new WebView();
        webView.setContextMenuEnabled(false);
        webView.setPageFill(Color.TRANSPARENT);
        webView.setStyle("-fx-background-color: transparent;");
        webView.getEngine().loadContent(botpressHtml(), "text/html");

        StackPane widget = new StackPane(webView);
        widget.setId(WIDGET_ID);
        widget.setPickOnBounds(false);
        widget.setStyle("-fx-background-color: transparent;");
        setWidgetSize(widget, webView, LAUNCHER_WIDTH, LAUNCHER_HEIGHT);
        webView.setOnMousePressed(event -> setWidgetSize(widget, webView, EXPANDED_WIDTH, EXPANDED_HEIGHT));

        return widget;
    }

    private static void setWidgetSize(StackPane widget, WebView webView, double width, double height) {
        widget.setMinSize(width, height);
        widget.setPrefSize(width, height);
        widget.setMaxSize(width, height);
        webView.setMinSize(width, height);
        webView.setPrefSize(width, height);
        webView.setMaxSize(width, height);
    }

    private static String botpressHtml() {
        return """
                <!doctype html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <style>
                        html, body {
                            width: 100%;
                            height: 100%;
                            margin: 0;
                            padding: 0;
                            overflow: hidden;
                            background: transparent !important;
                        }
                        body > div:not([id]),
                        iframe {
                            background: transparent !important;
                        }
                    </style>
                </head>
                <body>
                    <script src="https://cdn.botpress.cloud/webchat/v3.6/inject.js"></script>
                    <script src="https://files.bpcontent.cloud/2025/04/23/16/20250423165100-YXPJF3IQ.js" defer></script>
                </body>
                </html>
                """;
    }
}
