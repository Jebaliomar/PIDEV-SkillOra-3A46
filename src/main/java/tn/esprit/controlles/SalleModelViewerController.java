package tn.esprit.controlles;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.tools.LocalModelServer;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class SalleModelViewerController {

    @FXML
    private Label viewerTitleLabel;
    @FXML
    private Label viewerSubtitleLabel;
    @FXML
    private WebView modelWebView;

    private WebEngine engine;
    private String pendingModelPath;

    public static void openViewerWindow(String modelPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(SalleModelViewerController.class.getResource("/viewsadmin/event/SalleModelViewer.fxml"));
        Parent root = loader.load();
        SalleModelViewerController controller = loader.getController();
        controller.setModelPath(modelPath);

        Scene scene = new Scene(root, 1440, 900);
        URL eventCss = SalleModelViewerController.class.getResource("/styles/event.css");
        URL salleCss = SalleModelViewerController.class.getResource("/styles/salle.css");
        if (eventCss != null) {
            scene.getStylesheets().add(eventCss.toExternalForm());
        }
        if (salleCss != null) {
            scene.getStylesheets().add(salleCss.toExternalForm());
        }
        ThemeManager.applyTheme(scene);

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("Salle 3D Viewer");
        stage.setMinWidth(1440);
        stage.setMinHeight(900);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void initialize() {
        engine = modelWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                pushModelToViewer();
            }
        });

        URL viewerHtml = getClass().getResource("/viewsadmin/event/salle-model-viewer.html");
        if (viewerHtml == null) {
            showLoadError("Le fichier salle-model-viewer.html est introuvable.");
            return;
        }
        engine.load(viewerHtml.toExternalForm());
    }

    public void setModelPath(String modelPath) {
        this.pendingModelPath = modelPath;
        if (engine != null && engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
            pushModelToViewer();
        }
    }

    private void pushModelToViewer() {
        try {
            ResolvedModel resolved = resolveModel(pendingModelPath);
            viewerTitleLabel.setText("Salle 3D Viewer");
            viewerSubtitleLabel.setText(resolved.fileName);

            String script = "window.setModelSource(" + toJsString(resolved.sourceUrl) + ","
                    + toJsString(resolved.extension) + ","
                    + toJsString(resolved.fileName) + ");";
            engine.executeScript(script);
        } catch (Exception e) {
            viewerSubtitleLabel.setText("Apercu indisponible");
            engine.executeScript("window.showEmpty(" + toJsString(e.getMessage()) + ");");
        }
    }

    private ResolvedModel resolveModel(String modelPath) throws IOException {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Choisissez d'abord un modele .glb ou .gltf.");
        }

        String trimmed = modelPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return new ResolvedModel(trimmed, extensionFromName(trimmed), nameFromPath(trimmed));
        }

        File file = resolveLocalFile(trimmed);
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Le fichier 3D selectionne est introuvable.");
        }

        String ext = extensionFromName(file.getName());
        if (!"glb".equals(ext) && !"gltf".equals(ext)) {
            throw new IllegalArgumentException("Format non supporte. Utilisez .glb ou .gltf.");
        }

        Path absolutePath = file.toPath().toAbsolutePath().normalize();
        String source = LocalModelServer.registerModel(absolutePath);
        return new ResolvedModel(source, ext, file.getName());
    }

    private File resolveLocalFile(String modelPath) {
        File direct = new File(modelPath);
        if (direct.exists()) {
            return direct;
        }
        File fromHome = new File(System.getProperty("user.home"), modelPath);
        if (fromHome.exists()) {
            return fromHome;
        }
        File fromDownloads = new File(new File(System.getProperty("user.home"), "Downloads"), new File(modelPath).getName());
        if (fromDownloads.exists()) {
            return fromDownloads;
        }
        return null;
    }

    private String extensionFromName(String path) {
        int idx = path.lastIndexOf('.');
        if (idx < 0 || idx == path.length() - 1) {
            return "";
        }
        return path.substring(idx + 1).toLowerCase();
    }

    private String nameFromPath(String path) {
        String normalized = path.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    private String toJsString(String value) {
        if (value == null) {
            return "null";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    private void showLoadError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Initialisation 3D");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record ResolvedModel(String sourceUrl, String extension, String fileName) {
    }
}

