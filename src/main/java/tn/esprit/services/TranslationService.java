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
import java.util.regex.Pattern;

public class TranslationService {

    private static final String DEFAULT_TRANSLATION_MODEL = "facebook/mbart-large-50-many-to-many-mmt";
    private static final String HF_ROUTER_BASE = "https://router.huggingface.co/hf-inference/models/";

    private final HttpClient httpClient;
    private final String hfToken;
    private final String endpointUrl;

    public TranslationService() {
        this(resolveFromEnvironment("HF_TOKEN"), resolveFromEnvironment("HF_TRANSLATION_URL"));
    }

    public TranslationService(String hfToken, String endpointUrl) {
        this.hfToken = hfToken == null ? "" : hfToken.trim();
        this.endpointUrl = normalizeEndpointUrl(endpointUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static String normalizeEndpointUrl(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            return buildInferenceUrl(DEFAULT_TRANSLATION_MODEL);
        }

        String raw = endpointUrl.trim();

        // Accept bare model IDs in .env, e.g. facebook/mbart-large-50-many-to-many-mmt
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            return buildInferenceUrl(stripModelPrefix(raw));
        }

        URI uri = URI.create(raw);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath();

        if (host.equals("router.huggingface.co") && path.startsWith("/hf-inference/models/")) {
            return raw;
        }

        String modelId = extractModelId(path);
        if (!modelId.isBlank()) {
            return buildInferenceUrl(modelId);
        }

        return buildInferenceUrl(DEFAULT_TRANSLATION_MODEL);
    }

    private static String buildInferenceUrl(String modelId) {
        return HF_ROUTER_BASE + stripModelPrefix(modelId);
    }

    private static String extractModelId(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }

        String p = path.startsWith("/") ? path.substring(1) : path;

        if (p.startsWith("models/")) {
            return p.substring("models/".length());
        }

        if (p.startsWith("hf-inference/models/")) {
            return p.substring("hf-inference/models/".length());
        }

        if (p.contains("/")) {
            return p;
        }

        return "";
    }

    private static String stripModelPrefix(String value) {
        return value.startsWith("models/") ? value.substring("models/".length()) : value;
    }

    private static String resolveFromEnvironment(String key) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String value = dotenv.get(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }

        return value == null ? "" : value.trim();
    }

    public boolean isConfigured() {
        return !hfToken.isBlank() && !endpointUrl.isBlank();
    }

    public String translateAuto(String text, String targetLang) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String sourceLang = detectSourceLanguage(text);

        if (sourceLang.equals(targetLang)) {
            return text;
        }

        return translate(text, sourceLang, targetLang);
    }

    public String translate(String text, String sourceLang, String targetLang) {
        requireConfigured();

        try {
            JsonObject body = new JsonObject();
            body.addProperty("inputs", text);

            JsonObject parameters = new JsonObject();
            parameters.addProperty("src_lang", sourceLang);
            parameters.addProperty("tgt_lang", targetLang);
            body.add("parameters", parameters);

            JsonObject options = new JsonObject();
            options.addProperty("wait_for_model", true);
            body.add("options", options);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + hfToken)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Translation API error " + response.statusCode() + " at " + endpointUrl + ": " + response.body()
                );
            }

            return parseTranslationText(response.body());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Translation service is unavailable: " + e.getMessage(), e);
        }
    }

    private String parseTranslationText(String body) {
        JsonElement parsed = JsonParser.parseString(body);

        if (parsed.isJsonArray()) {
            JsonArray array = parsed.getAsJsonArray();
            if (!array.isEmpty() && array.get(0).isJsonObject()) {
                JsonObject first = array.get(0).getAsJsonObject();
                if (first.has("translation_text")) {
                    return first.get("translation_text").getAsString();
                }
                if (first.has("generated_text")) {
                    return first.get("generated_text").getAsString();
                }
            }
        }

        if (parsed.isJsonObject()) {
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has("translation_text")) {
                return obj.get("translation_text").getAsString();
            }
            if (obj.has("generated_text")) {
                return obj.get("generated_text").getAsString();
            }
            if (obj.has("error")) {
                throw new IllegalStateException("Translation API error: " + obj.get("error").getAsString());
            }
        }

        throw new IllegalStateException("Unexpected translation response: " + body);
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException("Missing Hugging Face translation token or endpoint. Set HF_TOKEN and HF_TRANSLATION_URL.");
        }
    }

    private String detectSourceLanguage(String text) {
        String t = text == null ? "" : text.trim();

        if (ARABIC_CHARS.matcher(t).find()) {
            return "ar_AR";
        }

        String lower = t.toLowerCase(Locale.ROOT);

        if (containsFrenchSignals(lower)) {
            return "fr_XX";
        }

        return "en_XX";
    }

    private boolean containsFrenchSignals(String lowerText) {
        return lowerText.contains("é")
                || lowerText.contains("è")
                || lowerText.contains("à")
                || lowerText.contains("ù")
                || lowerText.contains("ç")
                || lowerText.contains(" le ")
                || lowerText.contains(" la ")
                || lowerText.contains(" les ")
                || lowerText.contains(" de ")
                || lowerText.contains(" je ");
    }

    private static final Pattern ARABIC_CHARS = Pattern.compile("[\\u0600-\\u06FF]");

}