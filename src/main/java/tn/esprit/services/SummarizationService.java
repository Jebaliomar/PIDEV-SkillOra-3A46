package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;

public class SummarizationService {

    private final HttpClient httpClient;
    private final String hfToken;
    private final String endpointUrl;
    private final String groqApiKey;
    private final String groqModelId;

    public SummarizationService() {
        this(
                readEnv("HF_TOKEN"),
                readEnv("HF_SUMMARY_URL"),
                readFirstNonBlankEnv("GROQ_API_KEY", "GROQ_API_KEY1"),
                readEnv("GROQ_MODEL_ID")
        );
    }

    public SummarizationService(String hfToken, String endpointUrl, String groqApiKey, String groqModelId) {
        this.hfToken = hfToken == null ? "" : hfToken.trim();
        this.endpointUrl = endpointUrl == null ? "" : endpointUrl.trim();
        this.groqApiKey = groqApiKey == null ? "" : groqApiKey.trim();
        this.groqModelId = groqModelId == null ? "" : groqModelId.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private static String readEnv(String key) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null ? "" : value.trim();
    }

    private static String readFirstNonBlankEnv(String... keys) {
        if (keys == null) return "";
        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            String value = readEnv(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    public boolean isConfigured() {
        // configured if either Hugging Face or Groq is available
        boolean hf = !hfToken.isBlank() && !endpointUrl.isBlank();
        boolean groq = !groqApiKey.isBlank() && !groqModelId.isBlank();
        return hf || groq;
    }

    public boolean shouldSummarize(String text) {
        return text != null && text.trim().length() > 1000;
    }

    public String summarize(String text) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Missing summarization configuration. Set GROQ_API_KEY and GROQ_MODEL_ID (recommended), "
                            + "or set HF_TOKEN and HF_SUMMARY_URL."
            );
        }

        if (text == null || text.isBlank()) {
            return "";
        }

        JsonObject body = new JsonObject();
        body.addProperty("inputs", text);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("max_length", 180);
        parameters.addProperty("min_length", 40);
        parameters.addProperty("do_sample", false);
        body.add("parameters", parameters);

        JsonObject options = new JsonObject();
        options.addProperty("wait_for_model", true);
        body.add("options", options);

        Exception groqFailure = null;
        Exception hfFailure = null;

        // 1) Try Groq if configured
        if (!groqApiKey.isBlank() && !groqModelId.isBlank()) {
            try {
                String groqResult = summarizeWithGroq(text);
                if (groqResult != null && !groqResult.isBlank()) return groqResult;
            } catch (Exception e) {
                groqFailure = e;
            }
        }

        // 2) Candidate HF endpoints: only if HF is configured.
        if (!hfToken.isBlank() && !endpointUrl.isBlank()) {
            List<String> candidates = List.of(
                endpointUrl,
                "https://api-inference.huggingface.co/models/facebook/bart-large-cnn",
                "https://api-inference.huggingface.co/models/sshleifer/distilbart-cnn-12-6"
            );

            for (String url : candidates) {
            if (url == null || url.isBlank()) continue;
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(120))
                        .header("Authorization", "Bearer " + hfToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
                );

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return parseSummary(response.body());
                }

                // Treat not-found / HTML responses as unsupported model — try next fallback.
                String rb = response.body() == null ? "" : response.body();
                if (response.statusCode() == 404 || rb.startsWith("<!DOCTYPE") || rb.contains("Cannot POST")) {
                    hfFailure = new IllegalStateException("Summarization API returned 404 or unsupported model at " + url + ": " + rb);
                    continue;
                }

                // Other non-success codes — surface immediately.
                throw new IllegalStateException("Summarization API error " + response.statusCode() + ": " + rb);
            } catch (Exception e) {
                hfFailure = e;
                // try next candidate
            }
            }
        }

        // If remote summarization fails for all candidates, fall back to a local extractive summary
        String local = localFallbackSummary(text);
        Exception reason = groqFailure != null ? groqFailure : hfFailure;
        if (reason != null) {
            return local + "\n\n(Note: summary generated locally because remote summarization failed: " + reason.getMessage() + ")";
        }

        return local + "\n\n(Note: summary generated locally because no summarization endpoint was configured.)";
    }

    private String summarizeWithGroq(String text) throws Exception {
        String groqUrl = "https://api.groq.com/openai/v1/chat/completions";

        JsonObject payload = new JsonObject();
        payload.addProperty("model", groqModelId);

        // messages: single user message with summarization instruction
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        String prompt = "Summarize the following post in 3-5 clear sentences:\n\n" + text;
        userMsg.addProperty("content", prompt);
        messages.add(userMsg);
        payload.add("messages", messages);

        payload.addProperty("temperature", 0.3);
        payload.addProperty("max_tokens", 400);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(groqUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String rb = response.body() == null ? "" : response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Groq summarization error " + response.statusCode() + ": " + rb);
        }

        JsonElement parsed = JsonParser.parseString(rb);
        if (parsed.isJsonObject()) {
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("choices") && obj.get("choices").isJsonArray()) {
                JsonArray choices = obj.getAsJsonArray("choices");
                if (!choices.isEmpty() && choices.get(0).isJsonObject()) {
                    JsonObject first = choices.get(0).getAsJsonObject();
                    if (first.has("message") && first.get("message").isJsonObject()) {
                        JsonObject msg = first.getAsJsonObject("message");
                        if (msg.has("content")) return msg.get("content").getAsString();
                    }
                    if (first.has("text")) return first.get("text").getAsString();
                }
            }
        }

        throw new IllegalStateException("Unexpected Groq summarization response: " + rb);
    }

    private String localFallbackSummary(String text) {
        if (text == null || text.isBlank()) return "";

        // Split heuristically into sentences. If none found, fall back to a character-based trim.
        String[] sentences = Pattern.compile("(?<=[.!?])\\s+").split(text.trim());
        int max = 3;
        StringBuilder sb = new StringBuilder();
        if (sentences.length == 0) {
            String t = text.trim();
            return t.length() <= 400 ? t : t.substring(0, 400).trim() + "…";
        }

        for (int i = 0; i < Math.min(max, sentences.length); i++) {
            if (i > 0) sb.append(" ");
            sb.append(sentences[i].trim());
        }

        String result = sb.toString();
        if (result.length() > 600) {
            result = result.substring(0, 600).trim() + "…";
        }
        return result;
    }

    private String parseSummary(String body) {
        JsonElement parsed = JsonParser.parseString(body);

        // If the API returned an explicit error message, surface it.
        if (parsed.isJsonObject()) {
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("error")) {
                throw new IllegalStateException("Summarization API error: " + obj.get("error").getAsString());
            }
        }

        if (parsed.isJsonArray()) {
            JsonArray array = parsed.getAsJsonArray();
            if (!array.isEmpty() && array.get(0).isJsonObject()) {
                JsonObject first = array.get(0).getAsJsonObject();
                if (first.has("summary_text")) {
                    return first.get("summary_text").getAsString();
                }
                if (first.has("generated_text")) {
                    return first.get("generated_text").getAsString();
                }
                // Some models return choices or text in different fields
                if (first.has("choices") && first.get("choices").isJsonArray()) {
                    JsonArray choices = first.getAsJsonArray("choices");
                    if (!choices.isEmpty() && choices.get(0).isJsonObject()) {
                        JsonObject c = choices.get(0).getAsJsonObject();
                        if (c.has("text")) return c.get("text").getAsString();
                        if (c.has("generated_text")) return c.get("generated_text").getAsString();
                    }
                }
            }
        }

        if (parsed.isJsonObject()) {
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("summary_text")) {
                return obj.get("summary_text").getAsString();
            }
            if (obj.has("generated_text")) {
                return obj.get("generated_text").getAsString();
            }
            if (obj.has("choices") && obj.get("choices").isJsonArray()) {
                JsonArray choices = obj.getAsJsonArray("choices");
                if (!choices.isEmpty() && choices.get(0).isJsonObject()) {
                    JsonObject c = choices.get(0).getAsJsonObject();
                    if (c.has("text")) return c.get("text").getAsString();
                    if (c.has("generated_text")) return c.get("generated_text").getAsString();
                }
            }
        }

        throw new IllegalStateException("Unexpected summarization response: " + body);
    }
}