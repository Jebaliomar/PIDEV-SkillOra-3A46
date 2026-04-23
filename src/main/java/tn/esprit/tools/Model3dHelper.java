package tn.esprit.tools;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Model3dHelper {

    private Model3dHelper() {
    }

    public static File chooseModelFile(Window owner) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un modele 3D");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("3D Models", "*.glb", "*.gltf")
        );
        return chooser.showOpenDialog(owner);
    }

    public static void openPreview(String modelPath) throws IOException {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Choisissez d'abord un modele 3D.");
        }

        String trimmedPath = modelPath.trim();
        File localFile = resolveFile(trimmedPath);

        // Fallback 1: on Windows, open local model directly in native 3D viewer if available.
        if (localFile != null && localFile.exists() && tryOpenWithNativeViewer(localFile)) {
            return;
        }

        String source = resolveSource(trimmedPath);
        String html = buildHtml(source);

        Path previewFile = Files.createTempFile("skillora-model-preview-", ".html");
        Files.writeString(previewFile, html, StandardCharsets.UTF_8);

        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Preview is not supported on this machine.");
        }
        Desktop.getDesktop().browse(previewFile.toUri());
    }

    private static boolean tryOpenWithNativeViewer(File modelFile) {
        String name = modelFile.getName().toLowerCase();
        boolean is3dModel = name.endsWith(".glb") || name.endsWith(".gltf");
        if (!is3dModel || !Desktop.isDesktopSupported()) {
            return false;
        }

        try {
            Desktop.getDesktop().open(modelFile);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static String resolveSource(String modelPath) {
        if (modelPath.startsWith("http://") || modelPath.startsWith("https://")) {
            return modelPath;
        }

        File file = resolveFile(modelPath);
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Le fichier 3D selectionne est introuvable.");
        }
        try {
            return LocalModelServer.registerModel(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de preparer le modele 3D pour l'apercu.");
        }
    }

    private static File resolveFile(String modelPath) {
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

    private static String buildHtml(String source) {
        String escapedSource = source
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        return """
                <!DOCTYPE html>
                <html lang=\"fr\">
                <head>
                    <meta charset=\"UTF-8\" />
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                    <title>Apercu modele 3D</title>
                    <script src=\"https://cdn.babylonjs.com/babylon.js\"></script>
                    <script src=\"https://cdn.babylonjs.com/loaders/babylonjs.loaders.min.js\"></script>
                    <style>
                        html, body {
                            margin: 0;
                            width: 100%%;
                            height: 100%%;
                            overflow: hidden;
                            font-family: Arial, sans-serif;
                            background: #0b1220;
                            color: #e2e8f0;
                        }
                        .viewer-box {
                            width: calc(100%% - 24px);
                            height: calc(100%% - 24px);
                            margin: 12px;
                            background: #111a2e;
                            border-radius: 16px;
                            box-sizing: border-box;
                            padding: 14px;
                        }
                        h1 {
                            margin: 0 0 8px;
                            font-size: 22px;
                        }
                        p {
                            margin: 0 0 10px;
                            color: #94a3b8;
                        }
                        #renderCanvas {
                            width: 100%%;
                            height: calc(100%% - 60px);
                            border-radius: 10px;
                            background: linear-gradient(180deg, #1e293b 0%%, #0f172a 100%%);
                            touch-action: none;
                            outline: none;
                        }
                        #errorBox {
                            position: absolute;
                            left: 24px;
                            right: 24px;
                            bottom: 18px;
                            background: rgba(127, 29, 29, 0.9);
                            color: #fee2e2;
                            padding: 8px 10px;
                            border-radius: 8px;
                            font-size: 13px;
                            display: none;
                        }
                    </style>
                </head>
                <body>
                    <div class=\"viewer-box\">
                        <h1>Modele 3D</h1>
                        <p>Rotation, zoom et deplacement disponibles.</p>
                        <canvas id=\"renderCanvas\"></canvas>
                    </div>
                    <div id=\"errorBox\"></div>

                    <script>
                        (function () {
                            const modelUrl = "%s";
                            const canvas = document.getElementById('renderCanvas');
                            const errorBox = document.getElementById('errorBox');

                            function showError(message) {
                                errorBox.style.display = 'block';
                                errorBox.textContent = message;
                                console.error(message);
                            }

                            try {
                                const engine = new BABYLON.Engine(canvas, true, { preserveDrawingBuffer: true, stencil: true });
                                const scene = new BABYLON.Scene(engine);
                                scene.clearColor = new BABYLON.Color4(0.06, 0.09, 0.16, 1);

                                const camera = new BABYLON.ArcRotateCamera('camera', Math.PI / 2, Math.PI / 2.5, 4, BABYLON.Vector3.Zero(), scene);
                                camera.attachControl(canvas, true);
                                camera.wheelDeltaPercentage = 0.01;

                                const light1 = new BABYLON.HemisphericLight('light1', new BABYLON.Vector3(1, 1, 0), scene);
                                light1.intensity = 0.9;
                                const light2 = new BABYLON.DirectionalLight('light2', new BABYLON.Vector3(-1, -2, -1), scene);
                                light2.intensity = 0.6;

                                const rootUrl = modelUrl.substring(0, modelUrl.lastIndexOf('/') + 1);
                                const fileName = modelUrl.substring(modelUrl.lastIndexOf('/') + 1);

                                BABYLON.SceneLoader.Append(rootUrl, fileName, scene,
                                    function () {
                                        try {
                                            const meshes = scene.meshes.filter(m => m.getTotalVertices && m.getTotalVertices() > 0);
                                            if (meshes.length > 0) {
                                                const vectors = [];
                                                meshes.forEach(m => {
                                                    const info = m.getBoundingInfo();
                                                    if (info && info.boundingBox) {
                                                        vectors.push(info.boundingBox.minimumWorld);
                                                        vectors.push(info.boundingBox.maximumWorld);
                                                    }
                                                });
                                                if (vectors.length > 1) {
                                                    const min = BABYLON.Vector3.Minimize.apply(null, vectors);
                                                    const max = BABYLON.Vector3.Maximize.apply(null, vectors);
                                                    const center = min.add(max).scale(0.5);
                                                    camera.setTarget(center);

                                                    const size = max.subtract(min).length();
                                                    camera.radius = Math.max(1.5, size * 1.2);
                                                    camera.lowerRadiusLimit = Math.max(0.2, camera.radius * 0.2);
                                                    camera.upperRadiusLimit = camera.radius * 8;
                                                }
                                            }
                                        } catch (fitErr) {
                                            console.warn('Fit camera warning:', fitErr);
                                        }
                                    },
                                    null,
                                    function (_, msg, exception) {
                                        showError('Erreur chargement modele: ' + (msg || exception || 'inconnue'));
                                    }
                                );

                                engine.runRenderLoop(function () {
                                    scene.render();
                                });

                                window.addEventListener('resize', function () {
                                    engine.resize();
                                });
                            } catch (e) {
                                showError('Erreur initialisation 3D: ' + (e && e.message ? e.message : e));
                            }
                        })();
                    </script>
                </body>
                </html>
                """.formatted(escapedSource);
    }
}
