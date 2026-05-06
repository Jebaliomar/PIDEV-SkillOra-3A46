package tn.esprit.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tn.esprit.tools.AppConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GroqQuizService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient;
    private final Gson gson;

    public GroqQuizService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public JsonObject genererQuestionQuiz(String sujet) throws IOException, InterruptedException {
        String apiKey = AppConfig.getGroqApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Configuration Groq manquante. Ajoutez GROQ_API_KEY dans le fichier .env puis redémarrez l'application.");
        }

        if (sujet == null || sujet.trim().isEmpty()) {
            throw new IllegalArgumentException("Le sujet de génération est vide.");
        }

        String prompt = """
                Génère UNE seule question de quiz en français sur le sujet suivant : %s

                Réponds uniquement en JSON valide, sans markdown, sans texte supplémentaire.

                Structure obligatoire :
                {
                  "question": "texte de la question",
                  "optionA": "texte option A",
                  "optionB": "texte option B",
                  "optionC": "texte option C",
                  "optionD": "texte option D",
                  "correctAnswer": "A",
                  "explanation": "explication courte"
                }

                Contraintes :
                - 4 options obligatoires
                - Une seule bonne réponse parmi A, B, C, D
                - La bonne réponse doit être cohérente
                - Question claire et professionnelle
                - Pas de ```json
                - JSON brut uniquement
                """.formatted(sujet.trim());

        JsonObject payload = new JsonObject();
        payload.addProperty("model", MODEL);
        payload.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "Tu es un générateur strict de QCM. Tu réponds uniquement en JSON valide.");
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Erreur API Groq : " + response.statusCode() + " - " + response.body());
        }

        JsonObject root = gson.fromJson(response.body(), JsonObject.class);

        if (root == null || !root.has("choices")) {
            throw new RuntimeException("Réponse Groq invalide : " + response.body());
        }

        String content = root
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content")
                .getAsString();

        content = nettoyerJson(content);

        JsonObject result = gson.fromJson(content, JsonObject.class);

        if (result == null) {
            throw new RuntimeException("Impossible de parser le JSON retourné par Groq.");
        }

        verifierChamp(result, "question");
        verifierChamp(result, "optionA");
        verifierChamp(result, "optionB");
        verifierChamp(result, "optionC");
        verifierChamp(result, "optionD");
        verifierChamp(result, "correctAnswer");
        verifierChamp(result, "explanation");

        return result;
    }

    private void verifierChamp(JsonObject json, String champ) {
        if (!json.has(champ) || json.get(champ).isJsonNull() || json.get(champ).getAsString().trim().isEmpty()) {
            throw new RuntimeException("Champ manquant dans la réponse IA : " + champ);
        }
    }

    private String nettoyerJson(String content) {
        if (content == null) {
            return "";
        }

        content = content.trim();

        if (content.startsWith("```json")) {
            content = content.substring(7).trim();
        } else if (content.startsWith("```")) {
            content = content.substring(3).trim();
        }

        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3).trim();
        }

        return content;
    }
}
