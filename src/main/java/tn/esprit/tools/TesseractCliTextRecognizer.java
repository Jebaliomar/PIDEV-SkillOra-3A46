package tn.esprit.tools;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TesseractCliTextRecognizer implements OcrTextRecognizer {

    private static final long COMMAND_TIMEOUT_SECONDS = 8;
    private static final long FALLBACK_TIMEOUT_SECONDS = 12;
    private static final String OCR_SPACE_ENDPOINT = "https://api.ocr.space/parse/image";
    private static final String OCR_SPACE_API_KEY = "helloworld";
    private final CameraReservationConfig config = CameraReservationConfig.getInstance();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private Boolean available;
    private String availabilityMessage;
    private String resolvedCommand;

    @Override
    public String recognize(WritableImage image, CameraReservationField field) throws Exception {
        ensureAvailabilityChecked();

        Path imageFile = Files.createTempFile("skillora-camera-", ".png");
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imageFile.toFile());

            String normalized = "";
            if (Boolean.TRUE.equals(available)) {
                try {
                    normalized = runBestPass(imageFile, field);
                } catch (Exception localOcrError) {
                    normalized = "";
                }
            }

            if (normalized.isBlank()) {
                normalized = runOcrSpaceFallback(imageFile, field);
            }

            if (normalized.isBlank()) {
                throw new IOException("Aucun texte fiable detecte. Tracez un peu plus grand ou corrigez manuellement.");
            }
            return normalized;
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    @Override
    public String describeAvailability() {
        ensureAvailabilityChecked();
        if (Boolean.TRUE.equals(available)) {
            return availabilityMessage;
        }
        return "OCR local indisponible : reconnaissance de secours en ligne disponible si Internet l'autorise.";
    }

    @Override
    public boolean isAvailable() {
        ensureAvailabilityChecked();
        return Boolean.TRUE.equals(available);
    }

    private synchronized void ensureAvailabilityChecked() {
        if (availabilityMessage != null) {
            return;
        }

        try {
            Process process = new ProcessBuilder(resolveTesseractCommand(), "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            available = finished && process.exitValue() == 0;
            availabilityMessage = available
                    ? "OCR pret : Tesseract detecte."
                    : buildMissingMessage();
            if (!available && !output.isBlank()) {
                availabilityMessage = buildMissingMessage();
            }
        } catch (Exception exception) {
            available = false;
            availabilityMessage = buildMissingMessage();
        }
    }

    private String resolveTesseractCommand() {
        if (resolvedCommand != null && !resolvedCommand.isBlank()) {
            return resolvedCommand;
        }

        String configured = sanitizeConfiguredCommand(config.getOcrCommand());
        Path configuredPath = resolveConfiguredCommandPath(configured);
        if (configuredPath != null) {
            resolvedCommand = configuredPath.toString();
            return resolvedCommand;
        }

        List<String> candidates = new ArrayList<>();
        candidates.add("tesseract");
        if (!configured.isBlank()) {
            candidates.add(configured);
            addConfiguredCandidates(candidates, configured);
        }
        candidates.addAll(buildWindowsCandidates());

        for (String candidate : candidates) {
            if (commandLooksAvailable(candidate)) {
                resolvedCommand = candidate;
                return resolvedCommand;
            }
        }

        resolvedCommand = "tesseract";
        return resolvedCommand;
    }

    private String buildMissingMessage() {
        String configured = sanitizeConfiguredCommand(config.getOcrCommand());
        if (!configured.isBlank()) {
            Path configuredPath = resolvePathQuietly(configured);
            if (configuredPath != null && Files.isDirectory(configuredPath)) {
                return "OCR indisponible : ocr.command pointe vers un dossier, pas vers tesseract.exe.";
            }
            if (configuredPath != null && Files.exists(configuredPath) && !Files.isRegularFile(configuredPath)) {
                return "OCR indisponible : ocr.command ne pointe pas vers un fichier executable valide.";
            }
            return "OCR indisponible : le chemin Tesseract configure est introuvable ou invalide. Verifiez ocr.command dans camera-reservation.properties.";
        }
        return "OCR indisponible : installez Tesseract ou configurez ocr.command dans camera-reservation.properties.";
    }

    private String sanitizeConfiguredCommand(String configured) {
        if (configured == null) {
            return "";
        }
        String sanitized = configured.trim();
        if (sanitized.length() >= 2) {
            boolean doubleQuoted = sanitized.startsWith("\"") && sanitized.endsWith("\"");
            boolean singleQuoted = sanitized.startsWith("'") && sanitized.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                sanitized = sanitized.substring(1, sanitized.length() - 1).trim();
            }
        }
        return sanitized;
    }

    private Path resolveConfiguredCommandPath(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }

        Path path = resolvePathQuietly(configured);
        if (path == null) {
            return null;
        }

        if (Files.isRegularFile(path)) {
            return path;
        }
        if (Files.isDirectory(path)) {
            Path bundledExe = path.resolve("tesseract.exe");
            if (Files.isRegularFile(bundledExe)) {
                return bundledExe;
            }
        }

        Path parent = path.getParent();
        if (parent != null) {
            Path siblingExe = parent.resolve("tesseract.exe");
            if (Files.isRegularFile(siblingExe)) {
                return siblingExe;
            }
        }

        return null;
    }

    private Path resolvePathQuietly(String configured) {
        try {
            return Paths.get(configured);
        } catch (InvalidPathException exception) {
            return null;
        }
    }

    private void addConfiguredCandidates(List<String> candidates, String configured) {
        Path configuredPath = resolvePathQuietly(configured);
        if (configuredPath == null) {
            return;
        }

        if (Files.isDirectory(configuredPath)) {
            Path exe = configuredPath.resolve("tesseract.exe");
            candidates.add(exe.toString());
        }

        Path parent = configuredPath.getParent();
        if (parent != null) {
            candidates.add(parent.resolve("tesseract.exe").toString());
        }
    }

    private List<String> buildWindowsCandidates() {
        String programFiles = System.getenv("ProgramFiles");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String localAppData = System.getenv("LocalAppData");

        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, programFiles, "Tesseract-OCR\\tesseract.exe");
        addCandidate(candidates, programFilesX86, "Tesseract-OCR\\tesseract.exe");
        addCandidate(candidates, localAppData, "Programs\\Tesseract-OCR\\tesseract.exe");
        addCandidate(candidates, "C:\\Tesseract-OCR", "tesseract.exe");
        return candidates;
    }

    private void addCandidate(List<String> candidates, String base, String suffix) {
        if (base == null || base.isBlank()) {
            return;
        }
        candidates.add(Paths.get(base, suffix).toString());
    }

    private boolean commandLooksAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private String runBestPass(Path imageFile, CameraReservationField field) throws Exception {
        List<OcrPass> passes = field == CameraReservationField.TELEPHONE
                ? List.of(
                new OcrPass("7", List.of("tessedit_char_whitelist=+0123456789")),
                new OcrPass("13", List.of("tessedit_char_whitelist=+0123456789"))
        )
                : List.of(
                new OcrPass("6", List.of("preserve_interword_spaces=1")),
                new OcrPass("8", List.of("preserve_interword_spaces=1")),
                new OcrPass("7", List.of("preserve_interword_spaces=1")),
                new OcrPass("13", List.of("preserve_interword_spaces=1"))
        );

        String best = "";
        int bestScore = Integer.MIN_VALUE;
        String lastError = null;
        for (OcrPass pass : passes) {
            try {
                String raw = runTesseract(imageFile, field, pass);
                String normalized = normalizeOutput(raw, field);
                if (isLikelyFalsePositive(normalized, field)) {
                    continue;
                }
                int score = scoreOutput(normalized, field);
                if (!normalized.isBlank() && score > bestScore) {
                    best = normalized;
                    bestScore = score;
                }
            } catch (IOException exception) {
                lastError = exception.getMessage();
            }
        }

        if (!best.isBlank()) {
            return best;
        }
        if (lastError != null && !lastError.isBlank()) {
            throw new IOException(lastError);
        }
        return "";
    }

    private String runOcrSpaceFallback(Path imageFile, CameraReservationField field) throws Exception {
        byte[] fileBytes = Files.readAllBytes(imageFile);
        String boundary = "----SkillOraOCR" + System.nanoTime();
        byte[] requestBody = buildMultipartBody(boundary, imageFile.getFileName().toString(), fileBytes, field);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OCR_SPACE_ENDPOINT))
                .timeout(Duration.ofSeconds(FALLBACK_TIMEOUT_SECONDS))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OCR de secours indisponible (HTTP " + response.statusCode() + ").");
        }

        String body = response.body() == null ? "" : response.body();
        String error = extractJsonValue(body, "ErrorMessage");
        if (!error.isBlank() && !"[]".equals(error.trim())) {
            String parsedError = error.replace("[", "").replace("]", "").replace("\"", "").trim();
            if (!parsedError.isBlank()) {
                throw new IOException(parsedError);
            }
        }

        String parsedText = extractJsonValue(body, "ParsedText");
        if (parsedText.isBlank()) {
            return "";
        }
        String normalized = normalizeOutput(parsedText, field);
        if (isLikelyFalsePositive(normalized, field)) {
            return "";
        }
        return normalized;
    }

    private byte[] buildMultipartBody(String boundary, String fileName, byte[] fileBytes, CameraReservationField field) throws IOException {
        try (java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            writeFormField(outputStream, boundary, "apikey", OCR_SPACE_API_KEY);
            writeFormField(outputStream, boundary, "language", config.getOcrLanguage());
            writeFormField(outputStream, boundary, "OCREngine", "2");
            writeFormField(outputStream, boundary, "isTable", "false");
            writeFormField(outputStream, boundary, "scale", "true");
            writeFormField(outputStream, boundary, "detectOrientation", "true");
            if (field == CameraReservationField.TELEPHONE) {
                writeFormField(outputStream, boundary, "isOverlayRequired", "false");
            }

            outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(fileBytes);
            outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));

            outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            return outputStream.toByteArray();
        }
    }

    private void writeFormField(OutputStream outputStream, String boundary, String name, String value) throws IOException {
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private String extractJsonValue(String body, String key) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String needle = "\"" + key + "\"";
        int keyIndex = body.indexOf(needle);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = body.indexOf(':', keyIndex + needle.length());
        if (colonIndex < 0) {
            return "";
        }

        int cursor = colonIndex + 1;
        while (cursor < body.length() && Character.isWhitespace(body.charAt(cursor))) {
            cursor++;
        }

        if (cursor >= body.length()) {
            return "";
        }

        char first = body.charAt(cursor);
        if (first == '"') {
            StringBuilder builder = new StringBuilder();
            boolean escaped = false;
            for (int i = cursor + 1; i < body.length(); i++) {
                char ch = body.charAt(i);
                if (escaped) {
                    builder.append(ch);
                    escaped = false;
                    continue;
                }
                if (ch == '\\') {
                    escaped = true;
                    continue;
                }
                if (ch == '"') {
                    return unescapeJson(builder.toString());
                }
                builder.append(ch);
            }
            return unescapeJson(builder.toString());
        }

        if (first == '[') {
            int depth = 0;
            for (int i = cursor; i < body.length(); i++) {
                char ch = body.charAt(i);
                if (ch == '[') {
                    depth++;
                } else if (ch == ']') {
                    depth--;
                    if (depth == 0) {
                        return body.substring(cursor, i + 1);
                    }
                }
            }
            return body.substring(cursor).trim();
        }

        int end = cursor;
        while (end < body.length()) {
            char ch = body.charAt(end);
            if (ch == ',' || ch == '}' || Character.isWhitespace(ch)) {
                break;
            }
            end++;
        }
        return body.substring(cursor, end).trim();
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String runTesseract(Path imageFile, CameraReservationField field, OcrPass pass) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(resolveTesseractCommand());
        command.add(imageFile.toAbsolutePath().toString());
        command.add("stdout");
        command.add("-l");
        command.add(config.getOcrLanguage());
        command.add("--dpi");
        command.add("300");
        command.add("--oem");
        command.add("1");
        command.add("--psm");
        command.add(pass.psm());
        for (String variable : pass.variables()) {
            command.add("-c");
            command.add(variable);
        }
        if (field != CameraReservationField.TELEPHONE) {
            command.add("-c");
            command.add("load_system_dawg=0");
            command.add("-c");
            command.add("load_freq_dawg=0");
        }

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("OCR timed out.");
        }
        if (process.exitValue() != 0) {
            throw new IOException(output.isBlank() ? "OCR failed." : output);
        }
        return output;
    }

    private int scoreOutput(String output, CameraReservationField field) {
        if (output == null || output.isBlank()) {
            return Integer.MIN_VALUE;
        }
        if (field == CameraReservationField.TELEPHONE) {
            return output.replaceAll("[^0-9]", "").length() * 8 - Math.abs(output.length() - output.replaceAll("[^0-9+]", "").length()) * 5;
        }
        int letters = output.replaceAll("[^A-Za-z]", "").length();
        int spacesAndHyphens = output.replaceAll("[A-Za-z\\s'-]", "").length();
        return letters * 5 - spacesAndHyphens * 6 - Math.abs(output.length() - letters) * 2;
    }

    private boolean isLikelyFalsePositive(String output, CameraReservationField field) {
        if (output == null) {
            return true;
        }

        String normalized = output.trim();
        if (normalized.isBlank()) {
            return true;
        }

        if (field == CameraReservationField.TELEPHONE) {
            return normalized.replaceAll("[^0-9+]", "").isBlank();
        }

        String lower = normalized.toLowerCase();
        if (lower.equals("lines") || lower.equals("line") || lower.equals("ocr") || lower.equals("text") || lower.equals("image")) {
            return true;
        }

        String compactLetters = lower.replaceAll("[^a-zà-ÿ'-]", "");
        return compactLetters.length() < 2;
    }

    private String normalizeOutput(String output, CameraReservationField field) {
        String compact = output == null ? "" : output.replaceAll("\\s+", " ").trim();
        if (field == CameraReservationField.TELEPHONE) {
            return compact.replaceAll("[^+0-9]", "");
        }
        return compact
                .replace('|', 'I')
                .replaceAll("[^\\p{L}\\s'-]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record OcrPass(String psm, List<String> variables) {
    }
}
