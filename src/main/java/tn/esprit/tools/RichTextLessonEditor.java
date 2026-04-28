package tn.esprit.tools;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import netscape.javascript.JSObject;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

public class RichTextLessonEditor extends StackPane {

    private final WebView webView;
    private final WebEngine engine;
    private final EditorBridge bridge;
    private String pendingHtml = "";
    private boolean ready;

    public RichTextLessonEditor() {
        getStyleClass().add("rich-editor-shell");
        setMinHeight(360);
        setPrefHeight(460);

        webView = new WebView();
        webView.getStyleClass().add("rich-editor-web-view");
        webView.setContextMenuEnabled(false);
        webView.setMinHeight(360);
        webView.setPrefHeight(460);

        engine = webView.getEngine();
        bridge = new EditorBridge();
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("skilloraBridge", bridge);
                ready = true;
                setHtml(pendingHtml);
            }
        });

        getChildren().add(webView);
        engine.loadContent(buildEditorHtml(), "text/html");
    }

    public void setHtml(String html) {
        pendingHtml = html == null ? "" : html;
        if (!ready) {
            return;
        }
        engine.executeScript("window.skilloraEditor.setHtml(" + toJsString(pendingHtml) + ")");
    }

    public String getHtml() {
        if (!ready) {
            return pendingHtml == null ? "" : pendingHtml;
        }
        Object value = engine.executeScript("window.skilloraEditor.getHtml()");
        return value == null ? "" : value.toString().trim();
    }

    public String getPlainText() {
        return Jsoup.parse(getHtml()).text().trim();
    }

    public void focusEditor() {
        if (ready) {
            engine.executeScript("window.skilloraEditor.focus()");
        }
    }

    public boolean hasMeaningfulContent() {
        String html = getHtml();
        if (!getPlainText().isBlank()) {
            return true;
        }
        String lower = html.toLowerCase(Locale.ROOT);
        return lower.contains("<img") || lower.contains("<video") || lower.contains("<iframe");
    }

    private String copyPickedFile(File file, String subDirectory) throws IOException {
        Path uploadDir = Path.of("uploads", subDirectory);
        Files.createDirectories(uploadDir);
        String extension = extensionOf(file.getName());
        String fileName = UUID.randomUUID() + extension;
        Path destination = uploadDir.resolve(fileName);
        Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
        return destination.toAbsolutePath().normalize().toUri().toString();
    }

    private String saveDataUrl(String dataUrl, String preferredName) throws IOException {
        if (dataUrl == null || !dataUrl.startsWith("data:")) {
            throw new IOException("Unsupported dropped image.");
        }

        int commaIndex = dataUrl.indexOf(',');
        if (commaIndex < 0) {
            throw new IOException("Invalid dropped image.");
        }

        String metadata = dataUrl.substring(0, commaIndex).toLowerCase(Locale.ROOT);
        String extension = extensionFromMime(metadata);
        String safeName = preferredName == null || preferredName.isBlank()
                ? UUID.randomUUID() + extension
                : stripExtension(preferredName).replaceAll("[^a-zA-Z0-9._-]", "-") + "-" + UUID.randomUUID() + extension;

        byte[] bytes = Base64.getDecoder().decode(dataUrl.substring(commaIndex + 1));
        Path uploadDir = Path.of("uploads", "lesson-images");
        Files.createDirectories(uploadDir);
        Path destination = uploadDir.resolve(safeName);
        Files.write(destination, bytes);
        return destination.toAbsolutePath().normalize().toUri().toString();
    }

    private String extensionFromMime(String metadata) {
        if (metadata.contains("image/jpeg") || metadata.contains("image/jpg")) {
            return ".jpg";
        }
        if (metadata.contains("image/gif")) {
            return ".gif";
        }
        if (metadata.contains("image/webp")) {
            return ".webp";
        }
        return ".png";
    }

    private String stripExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    private String extensionOf(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex >= 0 ? name.substring(dotIndex) : "";
    }

    private String toJsString(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "\\n")
                .replace("</script>", "<\\/script>") + "'";
    }

    public final class EditorBridge {
        public String saveDroppedImage(String dataUrl, String preferredName) {
            try {
                return saveDataUrl(dataUrl, preferredName);
            } catch (IOException e) {
                return "";
            }
        }

        public String pickImage() {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Insert image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
            File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
            if (file == null) {
                return "";
            }
            try {
                return copyPickedFile(file, "lesson-images");
            } catch (IOException e) {
                return "";
            }
        }

        public String pickVideo() {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Insert video");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.mov", "*.m4v", "*.webm"));
            File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
            if (file == null) {
                return "";
            }
            try {
                return copyPickedFile(file, "lesson-embeds");
            } catch (IOException e) {
                return "";
            }
        }
    }

    private String buildEditorHtml() {
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <style>
                        :root {
                            color-scheme: light;
                            --ink: #0f172a;
                            --muted: #64748b;
                            --blue: #0b6ff3;
                            --border: #dbe5f1;
                            --soft: #f8fbff;
                            --panel: #ffffff;
                        }
                        * { box-sizing: border-box; }
                        button, select, textarea, article {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, Arial, sans-serif;
                        }
                        html, body {
                            margin: 0;
                            width: 100%;
                            height: 100%;
                            background: transparent;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, Arial, sans-serif;
                            color: var(--ink);
                            overflow: hidden;
                        }
                        .editor-shell {
                            height: 100vh;
                            display: flex;
                            flex-direction: column;
                            background: linear-gradient(160deg, rgba(239,248,255,.96), rgba(255,255,255,.98));
                            border: 1px solid var(--border);
                            border-radius: 20px;
                            overflow: hidden;
                        }
                        .toolbar {
                            display: flex;
                            align-items: center;
                            align-content: flex-start;
                            gap: 6px;
                            flex-wrap: wrap;
                            padding: 10px;
                            border-bottom: 1px solid var(--border);
                            background: rgba(255,255,255,.86);
                        }
                        .tool, select {
                            height: 30px;
                            border: 1px solid #d7e1ed;
                            border-radius: 10px;
                            background: #f8fafc;
                            color: #1f2a3d;
                            font-weight: 750;
                            font-size: 12px;
                            line-height: 28px;
                            padding: 0 9px;
                            outline: none;
                            white-space: nowrap;
                        }
                        .tool {
                            min-width: 32px;
                            cursor: pointer;
                        }
                        select {
                            width: 120px;
                            min-width: 120px;
                        }
                        .tool.compact {
                            min-width: 32px;
                            padding: 0 8px;
                        }
                        .tool.wide {
                            min-width: 52px;
                        }
                        .tool:hover, select:hover {
                            border-color: #93c5fd;
                            background: #eff6ff;
                            color: #0b5bd3;
                        }
                        .tool.primary {
                            background: #0b6ff3;
                            color: #fff;
                            border-color: #0b6ff3;
                        }
                        .divider {
                            width: 1px;
                            height: 24px;
                            background: #dbe5f1;
                            margin: 0 1px;
                        }
                        .status {
                            margin-left: 4px;
                            color: var(--muted);
                            font-size: 11px;
                            font-weight: 700;
                            line-height: 30px;
                        }
                        .editor-wrap {
                            flex: 1;
                            padding: 14px;
                            min-height: 0;
                            overflow: auto;
                        }
                        #editor {
                            min-height: 100%;
                            background: #fff;
                            border: 1px solid #dbe5f1;
                            border-radius: 18px;
                            padding: 20px 24px;
                            outline: none;
                            line-height: 1.7;
                            font-size: 15px;
                            box-shadow: 0 14px 34px rgba(15,23,42,.06);
                        }
                        #editor:empty::before {
                            content: "Write the lesson content here. Drop images, paste screenshots, format text, or insert a video embed.";
                            color: #94a3b8;
                        }
                        #editor.dragging {
                            border-color: #60a5fa;
                            box-shadow: 0 0 0 4px rgba(96,165,250,.18), 0 14px 34px rgba(15,23,42,.06);
                        }
                        #editor h1, #editor h2, #editor h3 {
                            line-height: 1.18;
                            margin: 1em 0 .45em;
                            color: #0f172a;
                        }
                        #editor h1 { font-size: 30px; }
                        #editor h2 { font-size: 24px; }
                        #editor h3 { font-size: 19px; }
                        #editor p { margin: 0 0 .9em; }
                        #editor blockquote {
                            margin: 1em 0;
                            padding: .35em 0 .35em 1em;
                            border-left: 4px solid #93c5fd;
                            color: #334155;
                            background: #f8fbff;
                            border-radius: 0 12px 12px 0;
                        }
                        #editor ul, #editor ol { padding-left: 1.5em; }
                        #editor img, #editor video, #editor iframe {
                            max-width: 100%;
                            max-height: 360px;
                            object-fit: contain;
                            display: block;
                            border-radius: 14px;
                            margin: .85rem auto;
                            border: 1px solid #e2e8f0;
                        }
                        #editor video { background: #0f172a; }
                        #editor table {
                            width: 100%;
                            border-collapse: collapse;
                            margin: 1em 0;
                        }
                        #editor th, #editor td {
                            border: 1px solid #dbe5f1;
                            padding: 9px 10px;
                        }
                        #editor pre {
                            background: #0f172a;
                            color: #e2e8f0;
                            padding: 16px;
                            border-radius: 14px;
                            overflow: auto;
                        }
                        #source {
                            display: none;
                            flex: 1;
                            width: 100%;
                            border: 0;
                            outline: none;
                            resize: none;
                            padding: 20px;
                            font: 13px/1.55 "SFMono-Regular", Consolas, monospace;
                            color: #0f172a;
                            background: #f8fafc;
                        }
                        .source-mode #editor { display: none; }
                        .source-mode #source { display: block; }
                    </style>
                </head>
                <body>
                    <main class="editor-shell">
                        <div class="toolbar">
                            <select id="block">
                                <option value="p">Paragraph</option>
                                <option value="h1">Heading 1</option>
                                <option value="h2">Heading 2</option>
                                <option value="h3">Heading 3</option>
                                <option value="blockquote">Quote</option>
                                <option value="pre">Code Block</option>
                            </select>
                            <button class="tool compact" data-cmd="bold" title="Bold"><b>B</b></button>
                            <button class="tool compact" data-cmd="italic" title="Italic"><i>I</i></button>
                            <button class="tool compact" data-cmd="underline" title="Underline"><u>U</u></button>
                            <button class="tool compact" data-cmd="strikeThrough" title="Strikethrough">S</button>
                            <div class="divider"></div>
                            <button class="tool compact" data-cmd="insertUnorderedList" title="Bulleted list">•</button>
                            <button class="tool compact" data-cmd="insertOrderedList" title="Numbered list">1.</button>
                            <button class="tool compact" data-cmd="outdent" title="Outdent">⇤</button>
                            <button class="tool compact" data-cmd="indent" title="Indent">⇥</button>
                            <div class="divider"></div>
                            <button class="tool compact" data-cmd="justifyLeft" title="Align left">L</button>
                            <button class="tool compact" data-cmd="justifyCenter" title="Align center">C</button>
                            <button class="tool compact" data-cmd="justifyRight" title="Align right">R</button>
                            <div class="divider"></div>
                            <button class="tool wide" id="linkBtn" title="Insert link">Link</button>
                            <button class="tool wide primary" id="imageBtn" title="Insert image">Image</button>
                            <button class="tool wide" id="videoBtn" title="Insert video">Video</button>
                            <button class="tool wide" id="tableBtn" title="Insert table">Table</button>
                            <button class="tool wide" id="codeBtn" title="HTML source">HTML</button>
                            <span class="status" id="status">Ready</span>
                        </div>
                        <div class="editor-wrap" id="wrap">
                            <article id="editor" contenteditable="true"></article>
                            <textarea id="source" spellcheck="false"></textarea>
                        </div>
                    </main>
                    <script>
                        const editor = document.getElementById('editor');
                        const source = document.getElementById('source');
                        const shell = document.querySelector('.editor-shell');
                        const block = document.getElementById('block');
                        const status = document.getElementById('status');
                        let sourceMode = false;

                        function exec(cmd, value = null) {
                            document.execCommand(cmd, false, value);
                            editor.focus();
                        }
                        document.querySelectorAll('[data-cmd]').forEach(button => {
                            button.addEventListener('click', () => exec(button.dataset.cmd));
                        });
                        block.addEventListener('change', () => {
                            exec('formatBlock', '<' + block.value + '>');
                            block.value = 'p';
                        });
                        document.getElementById('linkBtn').addEventListener('click', () => {
                            const url = prompt('Paste the link URL');
                            if (!url) return;
                            exec('createLink', url.match(/^https?:\\/\\//) ? url : 'https://' + url);
                        });
                        document.getElementById('tableBtn').addEventListener('click', () => {
                            exec('insertHTML', '<table><tbody><tr><th>Topic</th><th>Notes</th></tr><tr><td></td><td></td></tr><tr><td></td><td></td></tr></tbody></table><p></p>');
                        });
                        document.getElementById('imageBtn').addEventListener('click', () => {
                            if (!window.skilloraBridge) return;
                            const url = window.skilloraBridge.pickImage();
                            if (url) insertImage(url);
                        });
                        document.getElementById('videoBtn').addEventListener('click', () => {
                            if (!window.skilloraBridge) return;
                            const url = window.skilloraBridge.pickVideo();
                            if (url) insertVideo(url);
                        });
                        document.getElementById('codeBtn').addEventListener('click', () => {
                            sourceMode = !sourceMode;
                            if (sourceMode) {
                                source.value = editor.innerHTML;
                                shell.classList.add('source-mode');
                                status.textContent = 'HTML Source';
                            } else {
                                editor.innerHTML = source.value;
                                shell.classList.remove('source-mode');
                                status.textContent = 'Ready';
                            }
                        });
                        function insertImage(url) {
                            exec('insertHTML', '<figure><img src="' + escapeAttr(url) + '" alt="Lesson image"><figcaption>Image caption</figcaption></figure><p></p>');
                        }
                        function insertVideo(url) {
                            exec('insertHTML', '<video controls src="' + escapeAttr(url) + '"></video><p></p>');
                        }
                        function escapeAttr(value) {
                            return String(value || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
                        }
                        function readImageFile(file) {
                            return new Promise((resolve, reject) => {
                                const reader = new FileReader();
                                reader.onload = () => resolve(reader.result);
                                reader.onerror = reject;
                                reader.readAsDataURL(file);
                            });
                        }
                        async function handleFiles(files) {
                            if (!files || !files.length || !window.skilloraBridge) return;
                            for (const file of files) {
                                if (file.type && file.type.startsWith('image/')) {
                                    const dataUrl = await readImageFile(file);
                                    const url = window.skilloraBridge.saveDroppedImage(dataUrl, file.name || 'image');
                                    if (url) insertImage(url);
                                }
                            }
                        }
                        editor.addEventListener('dragover', event => {
                            event.preventDefault();
                            editor.classList.add('dragging');
                        });
                        editor.addEventListener('dragleave', () => editor.classList.remove('dragging'));
                        editor.addEventListener('drop', async event => {
                            event.preventDefault();
                            editor.classList.remove('dragging');
                            await handleFiles(event.dataTransfer.files);
                        });
                        editor.addEventListener('paste', async event => {
                            const files = event.clipboardData && event.clipboardData.files;
                            if (files && files.length) {
                                event.preventDefault();
                                await handleFiles(files);
                            }
                        });
                        window.skilloraEditor = {
                            setHtml(html) {
                                editor.innerHTML = html || '';
                                source.value = editor.innerHTML;
                            },
                            getHtml() {
                                return sourceMode ? source.value : editor.innerHTML;
                            },
                            focus() {
                                editor.focus();
                            }
                        };
                    </script>
                </body>
                </html>
                """;
    }
}
