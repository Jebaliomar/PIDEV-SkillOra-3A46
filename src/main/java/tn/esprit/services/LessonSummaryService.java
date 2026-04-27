package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.entities.Lesson;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LessonSummaryService {

    private static final String API_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String FALLBACK_MODEL = "llama-3.1-8b-instant";
    private static final int PRIMARY_TIMEOUT_SECONDS = 45;
    private static final int FALLBACK_TIMEOUT_SECONDS = 25;
    private static final int FALLBACK_INPUT_CHARS = 9_000;
    private static final int PDF_PRIMARY_INPUT_CHARS = 6_000;
    private static final int PDF_FALLBACK_INPUT_CHARS = 3_500;
    private static final int LONG_DOC_THRESHOLD_CHARS = 8_000;
    private static final int LONG_DOC_CHUNK_CHARS = 3_200;
    private static final int LONG_DOC_MAX_CHUNKS = 6;
    private static final long LONG_DOC_STEP_DELAY_MS = 350L;

    private final LessonContentExtractorService contentExtractor;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LessonSummaryService() {
        this(new LessonContentExtractorService());
    }

    public LessonSummaryService(LessonContentExtractorService contentExtractor) {
        this.contentExtractor = contentExtractor;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String summarizeLesson(Lesson lesson) throws Exception {
        String apiKey = loadGroqApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY configuration.");
        }

        String sourceText = sanitizeText(contentExtractor.extractForSummary(lesson));
        if (sourceText.isBlank()) {
            throw new IllegalStateException("No readable content found in this lesson.");
        }

        if (sourceText.length() > LONG_DOC_THRESHOLD_CHARS) {
            return summarizeLongDocument(lesson, sourceText, apiKey);
        }

        return requestSummaryWithFallback(lesson, sourceText, apiKey);
    }

    private String summarizeLongDocument(Lesson lesson, String sourceText, String apiKey) throws Exception {
        List<String> chunks = selectRepresentativeChunks(splitTextIntoChunks(sourceText, LONG_DOC_CHUNK_CHARS), LONG_DOC_MAX_CHUNKS);
        if (chunks.isEmpty()) {
            throw new IllegalStateException("No readable content found for summarization.");
        }

        List<String> partials = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            try {
                partials.add(requestSummaryWithFallback(lesson, chunks.get(index), apiKey));
            } catch (RuntimeException e) {
                if (!shouldFallback(e)) {
                    throw e;
                }
            }

            if (index < chunks.size() - 1) {
                Thread.sleep(LONG_DOC_STEP_DELAY_MS);
            }
        }

        if (partials.isEmpty()) {
            throw new IllegalStateException("AI service is busy right now. Please wait 10-20 seconds and try again.");
        }
        if (partials.size() == 1) {
            return partials.get(0);
        }

        StringBuilder synthesisInput = new StringBuilder("Combine these partial summaries into one coherent final summary.\n\n");
        for (int i = 0; i < partials.size(); i++) {
            synthesisInput.append("Partial summary ").append(i + 1).append(":\n").append(partials.get(i)).append("\n\n");
        }
        return requestSummaryWithFallback(lesson, synthesisInput.toString(), apiKey);
    }

    private String requestSummaryWithFallback(Lesson lesson, String sourceText, String apiKey) throws Exception {
        boolean isPdf = "pdf".equalsIgnoreCase(lesson.getType());
        String primaryInput = truncate(sourceText, isPdf ? PDF_PRIMARY_INPUT_CHARS : sourceText.length());

        try {
            return requestSummary(lesson, primaryInput, FALLBACK_MODEL, isPdf ? 22 : PRIMARY_TIMEOUT_SECONDS, isPdf ? 480 : 700, apiKey);
        } catch (RuntimeException e) {
            if (!shouldFallback(e)) {
                throw e;
            }
        }

        Thread.sleep(700L);
        String fallbackInput = truncate(sourceText, isPdf ? PDF_FALLBACK_INPUT_CHARS : FALLBACK_INPUT_CHARS);
        try {
            return requestSummary(lesson, fallbackInput, FALLBACK_MODEL, FALLBACK_TIMEOUT_SECONDS, isPdf ? 420 : 520, apiKey);
        } catch (RuntimeException e) {
            throw new IllegalStateException("AI service is busy right now. Please wait 10-20 seconds and try again.");
        }
    }

    private String requestSummary(Lesson lesson, String sourceText, String model, int timeoutSeconds, int maxTokens, String apiKey) throws Exception {
        String prompt = "Lesson title: " + (lesson.getTitle() == null ? "Untitled lesson" : lesson.getTitle())
                + "\n\nLesson source content:\n" + sourceText;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("max_tokens", maxTokens);
        body.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "You are an academic learning assistant. Write precise, factual, student-friendly summaries. Preserve key technical terms, avoid speculation, and keep language clear and concise."
                ),
                Map.of(
                        "role", "user",
                        "content", prompt + "\n\nRespond in plain text using this exact format:\nSummary:\n(4-6 concise sentences)\n\nKey Points:\n- point 1\n- point 2\n- point 3\n- point 4\n\nImportant Terms:\n- term: short explanation\n- term: short explanation\n\nRules:\n- Keep output under 220 words.\n- Include only information supported by the lesson content.\n- Do not add extra sections."
                )
        ));

        HttpRequest request = HttpRequest.newBuilder(URI.create(API_ENDPOINT))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("AI service timeout or network issue. Please try again.");
        }

        JsonNode data = objectMapper.readTree(response.body());
        if (response.statusCode() >= 400) {
            String message = data.path("error").path("message").asText("Groq request failed.");
            throw new IllegalStateException(message);
        }

        String content = data.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("AI service returned an empty summary.");
        }
        return content.trim();
    }

    private String loadGroqApiKey() {
        String env = System.getenv("GROQ_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        Properties properties = new Properties();
        loadPropertiesFromClasspath(properties, "/app.properties");
        loadPropertiesFromFile(properties, Path.of("groq.properties"));
        loadPropertiesFromFile(properties, Path.of("app.properties"));

        String configured = properties.getProperty("GROQ_API_KEY", properties.getProperty("groq.apiKey", ""));
        if (configured != null && !configured.isBlank()) {
            return unquote(configured.trim());
        }

        return readSymfonyEnvValue(Path.of("/Users/nidhal/Desktop/SkillORALatest/.env.local"), "GROQ_API_KEY");
    }

    private void loadPropertiesFromClasspath(Properties properties, String resourcePath) {
        try (InputStream inputStream = LessonSummaryService.class.getResourceAsStream(resourcePath)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (Exception ignored) {
            // Optional configuration source.
        }
    }

    private void loadPropertiesFromFile(Properties properties, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            properties.load(inputStream);
        } catch (Exception ignored) {
            // Optional configuration source.
        }
    }

    private String readSymfonyEnvValue(Path envPath, String key) {
        if (!Files.exists(envPath)) {
            return "";
        }
        try {
            for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith(key + "=")) {
                    return unquote(trimmed.substring((key + "=").length()).trim());
                }
            }
        } catch (Exception ignored) {
            // Optional development fallback.
        }
        return "";
    }

    private boolean shouldFallback(RuntimeException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return message.contains("timeout")
                || message.contains("network")
                || message.contains("rate limit")
                || message.contains("too many requests")
                || message.contains("429")
                || message.contains("503")
                || message.contains("busy")
                || message.contains("overloaded")
                || message.contains("temporarily unavailable")
                || message.contains("try again");
    }

    private String sanitizeText(String text) {
        return text == null ? "" : text.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").replaceAll("\\s+", " ").trim();
    }

    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private List<String> splitTextIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int offset = 0; offset < text.length(); offset += chunkSize) {
            String piece = text.substring(offset, Math.min(text.length(), offset + chunkSize)).trim();
            if (!piece.isBlank()) {
                chunks.add(piece);
            }
        }
        return chunks;
    }

    private List<String> selectRepresentativeChunks(List<String> chunks, int maxChunks) {
        if (chunks.size() <= maxChunks) {
            return chunks;
        }

        List<String> selected = new ArrayList<>();
        int previousIndex = -1;
        for (int i = 0; i < maxChunks; i++) {
            int index = Math.round(i * (chunks.size() - 1) / (float) Math.max(1, maxChunks - 1));
            if (index != previousIndex) {
                selected.add(chunks.get(index));
                previousIndex = index;
            }
        }
        return selected;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
