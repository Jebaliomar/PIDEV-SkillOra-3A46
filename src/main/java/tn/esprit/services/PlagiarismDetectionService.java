package tn.esprit.services;

import org.json.JSONObject;
import tn.esprit.tools.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PlagiarismDetectionService {

    public static final String MISSING_API_KEY_MESSAGE =
            "Clé API de plagiat manquante. Ajoutez PLAGIARISM_API_KEY dans le fichier .env puis redémarrez l'application.";

    private static final String DEFAULT_API_URL = "https://plagiarism.api.pangram.com";

    private final HttpClient client = HttpClient.newHttpClient();

    public PlagiarismResult analyze(String answerText) throws IOException, InterruptedException {
        String apiKey = AppConfig.getPlagiarismApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(MISSING_API_KEY_MESSAGE);
        }

        String body = new JSONObject()
                .put("text", answerText == null ? "" : answerText)
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.getEnv("PLAGIARISM_API_URL", DEFAULT_API_URL)))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Erreur API plagiat: HTTP " + response.statusCode());
        }

        return parseResponse(response.body());
    }

    private PlagiarismResult parseResponse(String responseBody) {
        JSONObject json = new JSONObject(responseBody == null || responseBody.isBlank() ? "{}" : responseBody);
        int percent = clamp(firstInt(json,
                "percent_plagiarized",
                "plagiarismScore",
                "plagiarism_score",
                "plagiarismPercent",
                "plagiarism_percentage",
                "score"));
        boolean detected = firstBoolean(json,
                percent >= 60,
                "plagiarism_detected",
                "plagiarismDetected",
                "detected");
        String status = firstString(json,
                detected ? "PLAGIAT DETECTE" : "AUCUN PLAGIAT MAJEUR",
                "status",
                "plagiarism_status");
        return new PlagiarismResult(percent, status, detected);
    }

    private int firstInt(JSONObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                try {
                    return (int) Math.round(json.getDouble(key));
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private boolean firstBoolean(JSONObject json, boolean fallback, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                try {
                    return json.getBoolean(key);
                } catch (Exception ignored) {
                }
            }
        }
        return fallback;
    }

    private String firstString(JSONObject json, String fallback, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.isNull(key)) {
                String value = json.optString(key, "");
                if (!value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return fallback;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record PlagiarismResult(int percent, String status, boolean detected) {
    }
}
