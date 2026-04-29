package tn.esprit.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.github.cdimascio.dotenv.Dotenv;

public class ModerationService {

    private static final URI HF_ENDPOINT =
            URI.create("https://router.huggingface.co/hf-inference/models/unitary/toxic-bert");

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final String hfToken;

    public ModerationService() {
        this(resolveTokenFromEnvironment());
    }

    public ModerationService(String hfToken) {
        if (hfToken == null || hfToken.isBlank()) {
            throw new IllegalStateException("Missing Hugging Face token. Set HF_TOKEN in your environment.");
        }
        this.hfToken = hfToken.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static String resolveTokenFromEnvironment() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String token = dotenv.get("HF_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getenv("HF_TOKEN");
        }

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Missing Hugging Face token. Add HF_TOKEN to .env or environment variables.");
        }

        return token.trim();
    }

    public ModerationResult moderate(String text) {
        try {
            String input = text == null ? "" : text.trim();

            // If input is short, send a single request. Otherwise split into chunks
            // and aggregate the results (take maximum per label) to preserve detection.
            String[] words = input.isBlank() ? new String[0] : input.split("\\s+");
            int chunkSize = 220; // words per chunk (heuristic to stay well under 512 tokens)

            if (words.length <= chunkSize) {
                return singleModerate(input);
            }

            ModerationResult aggregate = new ModerationResult(0,0,0,0,0,0);
            for (int i = 0; i < words.length; i += chunkSize) {
                int end = Math.min(words.length, i + chunkSize);
                String chunk = String.join(" ", Arrays.copyOfRange(words, i, end));
                ModerationResult part = singleModerate(chunk);
                aggregate = new ModerationResult(
                        Math.max(aggregate.toxicity(), part.toxicity()),
                        Math.max(aggregate.severeToxicity(), part.severeToxicity()),
                        Math.max(aggregate.obscene(), part.obscene()),
                        Math.max(aggregate.threat(), part.threat()),
                        Math.max(aggregate.insult(), part.insult()),
                        Math.max(aggregate.identityHate(), part.identityHate())
                );
            }

            return aggregate;
        } catch (Exception e) {
            throw new RuntimeException("Moderation service is unavailable.", e);
        }
    }

    private ModerationResult singleModerate(String textChunk) throws Exception {
        String payload = "{\"inputs\":" + jsonString(textChunk == null ? "" : textChunk) + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(HF_ENDPOINT)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + hfToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Hugging Face moderation API error: " + response.body());
        }

        return parseScores(response.body());
    }

    private String truncateForModel(String text, int maxWords) {
        if (text == null || text.isBlank()) return "";
        String trimmed = text.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length <= maxWords) return trimmed;
        String[] first = Arrays.copyOf(parts, maxWords);
        return String.join(" ", first);
    }

    public boolean shouldBlock(String text) {
        ModerationResult r = moderate(text);

        return r.toxicity() >= 0.80
                || r.severeToxicity() >= 0.65
                || r.threat() >= 0.60
                || r.insult() >= 0.75
                || r.obscene() >= 0.75
                || r.identityHate() >= 0.70;
    }

    private ModerationResult parseScores(String body) {
        Map<String, Double> scores = new HashMap<>();

        Pattern pattern = Pattern.compile(
                "\\\"label\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"\\s*,\\s*\\\"score\\\"\\s*:\\s*([0-9.Ee+-]+)"
        );
        Matcher matcher = pattern.matcher(body);

        while (matcher.find()) {
            String label = normalizeLabel(matcher.group(1));
            double score = Double.parseDouble(matcher.group(2));
            scores.put(label, score);
        }

        return new ModerationResult(
                scores.getOrDefault("toxicity", 0.0),
                scores.getOrDefault("severe_toxicity", 0.0),
                scores.getOrDefault("obscene", 0.0),
                scores.getOrDefault("threat", 0.0),
                scores.getOrDefault("insult", 0.0),
                scores.getOrDefault("identity_hate", 0.0)
        );
    }

    private String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }

        String normalized = label.trim().toLowerCase(Locale.ROOT);

        return switch (normalized) {
            case "toxic" -> "toxicity";
            case "severe_toxic", "severe_toxicity" -> "severe_toxicity";
            case "identity_attack", "identity_hate" -> "identity_hate";
            default -> normalized;
        };
    }

    private String jsonString(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + escaped + "\"";
    }

    public record ModerationResult(
            double toxicity,
            double severeToxicity,
            double obscene,
            double threat,
            double insult,
            double identityHate
    ) {}
}