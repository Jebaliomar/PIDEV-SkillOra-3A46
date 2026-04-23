package tn.esprit.services;

import tn.esprit.entities.HangmanGame;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GroqHangmanGeneratorService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient client = HttpClient.newHttpClient();

    public HangmanGame generateQuestion(String topic, String level) throws IOException, InterruptedException {
        return generateQuestion(topic, level, null);
    }

    public HangmanGame generateQuestion(String topic, String level, Collection<String> forbiddenAnswers)
            throws IOException, InterruptedException {

        String apiKey = System.getenv("GROQ_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GROQ_API_KEY n'est pas défini.");
        }

        String prompt = buildPrompt(topic, level, forbiddenAnswers);
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
            throw new IllegalStateException("JSON généré invalide : " + content);
        }

        HangmanGame game = parseGeneratedGame(json, topic, level);

        if (forbiddenAnswers != null) {
            String generatedAnswer = normalize(game.getAnswer());
            boolean alreadyUsed = forbiddenAnswers.stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(this::normalize)
                    .anyMatch(a -> a.equals(generatedAnswer));

            if (alreadyUsed) {
                throw new IllegalStateException("Groq a généré une réponse déjà utilisée dans la session : " + game.getAnswer());
            }
        }

        return game;
    }

    private String buildPrompt(String topic, String level, Collection<String> forbiddenAnswers) {
        int maxMistakes = switch (level.toUpperCase()) {
            case "HARD" -> 4;
            case "MEDIUM" -> 5;
            default -> 6;
        };

        String forbiddenBlock = "";
        if (forbiddenAnswers != null && !forbiddenAnswers.isEmpty()) {
            String forbidden = forbiddenAnswers.stream()
                    .filter(a -> a != null && !a.isBlank())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.joining(", "));

            if (!forbidden.isBlank()) {
                forbiddenBlock = """
                        
                        Additional rule:
                        - answer must be different from these existing answers: [%s]
                        - do not generate a duplicate concept already covered by these answers
                        """.formatted(forbidden);
            }
        }

        return """
                Generate exactly one hangman question for a technical learning app.

                Topic: %s
                Difficulty: %s

                Rules:
                - title must describe the concept to guess
                - hint must be short and useful
                - answer must be one single word only
                - answer must contain only letters or digits
                - no spaces in answer
                - no symbols in answer
                - maxMistakes must be %d%s

                Return ONLY raw JSON.
                Example:
                {
                  "title": "Keyword used to inherit a class in Java",
                  "hint": "Object-oriented programming",
                  "answer": "extends",
                  "maxMistakes": 6
                }
                """.formatted(topic, level, maxMistakes, forbiddenBlock);
    }

    private String buildRequestBody(String prompt) {
        return """
                {
                  "model": "%s",
                  "temperature": 0.7,
                  "messages": [
                    {
                      "role": "system",
                      "content": "You generate strict valid JSON only. No markdown. No explanations."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(MODEL, escapeJson(prompt));
    }

    private String extractContentFromResponse(String responseBody) {
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseBody);

        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }

        throw new IllegalStateException("Impossible d'extraire le contenu Groq.");
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

    private HangmanGame parseGeneratedGame(String json, String topic, String level) {
        String title = extractJsonValue(json, "title");
        String hint = extractJsonValue(json, "hint");
        String answer = extractJsonValue(json, "answer");
        String maxMistakesValue = extractJsonNumber(json, "maxMistakes");

        if (title == null || hint == null || answer == null || maxMistakesValue == null) {
            throw new IllegalStateException("JSON généré invalide : " + json);
        }

        String normalizedAnswer = answer.trim().replaceAll("[^A-Za-z0-9]", "");

        if (normalizedAnswer.isBlank()) {
            throw new IllegalStateException("La réponse générée est vide ou invalide.");
        }

        HangmanGame game = new HangmanGame();
        game.setId(-1);
        game.setTitle(title.trim());
        game.setTopic(topic);
        game.setLevel(level);
        game.setHint(hint.trim());
        game.setAnswer(normalizedAnswer);
        game.setMaxMistakes(Short.parseShort(maxMistakesValue));
        game.setCreatedAt(LocalDateTime.now());

        return game;
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }

        return null;
    }

    private String extractJsonNumber(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
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

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}