package tn.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExamAiCorrectionService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient client = HttpClient.newHttpClient();

    public Map<String, Object> analyzeExam(String examText, String answerText)
            throws IOException, InterruptedException {

        String apiKey = System.getenv("GROQ_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY n'est pas défini.");
        }

        String prompt = buildPrompt(examText, answerText);
        String requestBody = buildRequestBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Erreur Groq API: HTTP " + response.statusCode() + " -> " + response.body());
        }

        String content = extractContentFromResponse(response.body());
        String json = extractJsonObject(content);

        if (json == null || json.isBlank()) {
            throw new IllegalStateException("Réponse IA invalide : " + content);
        }

        return parseJsonResult(json);
    }

    private String buildPrompt(String examText, String answerText) {
        return """
                Tu es un correcteur d'examen universitaire.

                Analyse la réponse d'un étudiant à partir de l'énoncé d'examen.

                Donne uniquement un JSON brut au format exact suivant :
                {
                  "score": 0,
                  "feedback": "",
                  "aiPercent": 0,
                  "plagiarismPercent": 0,
                  "fraudAttempt": false,
                  "fraudReason": ""
                }

                Règles :
                - score entre 0 et 100
                - feedback en français, utile, clair, court à moyen
                - aiPercent entre 0 et 100
                - plagiarismPercent entre 0 et 100
                - fraudAttempt = true seulement si suspicion forte
                - fraudReason en français
                - pas de markdown
                - pas d'explication hors JSON

                ENONCE EXAMEN:
                %s

                REPONSE ETUDIANT:
                %s
                """.formatted(limit(examText), limit(answerText));
    }

    private String buildRequestBody(String prompt) {
        return """
                {
                  "model": "%s",
                  "temperature": 0.2,
                  "messages": [
                    {
                      "role": "system",
                      "content": "Return strict valid JSON only."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(MODEL, escapeJson(prompt));
    }

    private Map<String, Object> parseJsonResult(String json) {
        Map<String, Object> result = new HashMap<>();

        int score = clamp(extractJsonInt(json, "score", 0));
        String feedback = extractJsonString(json, "feedback", "Aucun feedback généré.");
        int aiPercent = clamp(extractJsonInt(json, "aiPercent", 0));
        int plagiarismPercent = clamp(extractJsonInt(json, "plagiarismPercent", 0));
        boolean fraudAttempt = extractJsonBoolean(json, "fraudAttempt", false);
        String fraudReason = extractJsonString(json, "fraudReason", fraudAttempt ? "Suspicion détectée" : "Aucune fraude majeure détectée");

        result.put("score", score);
        result.put("feedback", feedback);
        result.put("aiPercent", aiPercent);
        result.put("plagiarismPercent", plagiarismPercent);
        result.put("fraudAttempt", fraudAttempt);
        result.put("fraudReason", fraudReason);

        return result;
    }

    private String extractContentFromResponse(String responseBody) {
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseBody);

        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }

        throw new IllegalStateException("Impossible d'extraire le contenu IA.");
    }

    private String extractJsonObject(String rawContent) {
        if (rawContent == null) {
            return null;
        }

        String cleaned = rawContent.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7).trim();
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1);
        }

        return null;
    }

    private int extractJsonInt(String json, String key, int defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (Exception ignored) {
            }
        }
        return defaultValue;
    }

    private boolean extractJsonBoolean(String json, String key, boolean defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Boolean.parseBoolean(matcher.group(1));
        }
        return defaultValue;
    }

    private String extractJsonString(String json, String key, String defaultValue) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return defaultValue;
    }

    private String escapeJson(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    private String unescapeJson(String input) {
        return input
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private String limit(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        return cleaned.length() > 12000 ? cleaned.substring(0, 12000) : cleaned;
    }
}