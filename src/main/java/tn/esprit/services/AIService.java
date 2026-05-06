package tn.esprit.services;

import javafx.application.Platform;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_AUDIO_API_URL = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String MODEL = "llama3-8b-8192";
    private static final String WHISPER_MODEL = "whisper-large-v3";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(25);
    private static final Pattern SLOT_DATE_TIME_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})");
    private static final Pattern SLOT_TIME_PATTERN = Pattern.compile("\\b(\\d{2}:\\d{2})\\b");
    private static final Pattern SUGGESTION_FORMAT_PATTERN =
            Pattern.compile("^Best slot for you is .+ at .+ — .+$");
    private static final ExecutorService AI_EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "skillora-ai-worker");
        thread.setDaemon(true);
        return thread;
    });

    private final HttpClient httpClient;
    private final String apiKey;

    public AIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.apiKey = loadApiKey();
    }

    public String suggestBestSlot(List<String> slots, String studentName, String subject) {
        String safeStudent = normalize(studentName, "Student");
        String safeSubject = normalize(subject, "the requested subject");
        DayTime dayTime = extractDayAndTime(slots);
        String fallback = "Best slot for you is " + dayTime.day() + " at " + dayTime.time()
                + " — it gives you a focused session for " + safeSubject + ".";

        String slotsText;
        if (slots == null || slots.isEmpty()) {
            slotsText = "No available slots were provided.";
        } else {
            StringBuilder builder = new StringBuilder();
            for (String slot : slots) {
                builder.append("- ").append(normalize(slot, "Unknown slot")).append('\n');
            }
            slotsText = builder.toString().trim();
        }

        String prompt = "Student name: " + safeStudent + "\n"
                + "Subject: " + safeSubject + "\n"
                + "Available slots:\n" + slotsText + "\n\n"
                + "Respond in English only. Use exactly this format and nothing else:\n"
                + "Best slot for you is [DAY] at [TIME] — [short reason]";

        String aiResponse = executeWithFallback(prompt, fallback);
        return normalizeSuggestResponse(aiResponse, fallback);
    }

    public String generateReminder(String studentName, String profName, String time, String subject) {
        String safeStudent = normalize(studentName, "Student");
        String safeProfessor = normalize(profName, "Professor");
        String safeTime = normalize(time, "the planned time");
        String safeSubject = normalize(subject, "the requested subject");
        String strictTemplate = buildReminderTemplate(safeStudent, safeProfessor, safeTime, safeSubject);

        String prompt = "Write a short personalized reminder for this rendez-vous:\n"
                + "Student: " + safeStudent + "\n"
                + "Professor: " + safeProfessor + "\n"
                + "Time: " + safeTime + "\n"
                + "Subject: " + safeSubject + "\n\n"
                + "Respond in English only. Use exactly this format and nothing else:\n"
                + "Hi [studentName], reminder: you have a meeting with [profName] tomorrow at [time]. Don't forget to prepare [subject]!";

        // Keep the API call for consistency, then enforce the exact output contract.
        executeWithFallback(prompt, strictTemplate);
        return strictTemplate;
    }

    public String transcribeAudio(File audioFile) {
        if (audioFile == null || !audioFile.exists() || !audioFile.isFile()) {
            return "";
        }
        if (apiKey == null || apiKey.isBlank() || "YOUR_GROQ_KEY_HERE".equals(apiKey.trim())) {
            return "";
        }

        try {
            String boundary = "----SkillOraBoundary" + System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_AUDIO_API_URL))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(buildMultipartTranscriptionBodyPublisher(audioFile.toPath(), boundary))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }

            JSONObject root = new JSONObject(response.body());
            return sanitizeSingleLine(root.optString("text", ""));
        } catch (Exception exception) {
            return "";
        }
    }

    public VoiceBookingIntent extractVoiceBookingIntent(String transcribedText) {
        String transcript = sanitizeSingleLine(transcribedText);
        if (transcript.isBlank()) {
            return new VoiceBookingIntent(null, null, null);
        }

        String prompt = "You are a voice assistant for a university appointment booking system. "
                + "Extract from the user's message:\n"
                + "- professor_name: the professor they want to meet\n"
                + "- preferred_day: the day they prefer (if mentioned)\n"
                + "- subject: the meeting subject (if mentioned)\n"
                + "Respond ONLY in JSON format with double quotes like:\n"
                + "{\"professor_name\":\"Ben Ali\",\"preferred_day\":\"Friday\",\"subject\":\"PFE\"}\n\n"
                + "User message: " + transcript;

        String fallbackJson = "{\"professor_name\":\"\",\"preferred_day\":\"\",\"subject\":\"\"}";
        String rawResponse = executeWithFallback(prompt, fallbackJson);
        return parseVoiceBookingIntent(rawResponse);
    }

    private VoiceBookingIntent parseVoiceBookingIntent(String rawResponse) {
        String jsonCandidate = extractJsonObject(rawResponse);
        if (jsonCandidate.isBlank()) {
            return new VoiceBookingIntent(null, null, null);
        }

        try {
            JSONObject object = new JSONObject(jsonCandidate);
            return new VoiceBookingIntent(
                    normalizeToNull(object.optString("professor_name", null)),
                    normalizeToNull(object.optString("preferred_day", null)),
                    normalizeToNull(object.optString("subject", null))
            );
        } catch (Exception firstException) {
            try {
                String repaired = jsonCandidate.replace('\'', '"');
                JSONObject object = new JSONObject(repaired);
                return new VoiceBookingIntent(
                        normalizeToNull(object.optString("professor_name", null)),
                        normalizeToNull(object.optString("preferred_day", null)),
                        normalizeToNull(object.optString("subject", null))
                );
            } catch (Exception secondException) {
                return new VoiceBookingIntent(null, null, null);
            }
        }
    }

    private String extractJsonObject(String raw) {
        String value = sanitizeSingleLine(raw);
        if (value.isBlank()) {
            return "";
        }
        int firstBrace = value.indexOf('{');
        int lastBrace = value.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            return "";
        }
        return value.substring(firstBrace, lastBrace + 1);
    }

    private HttpRequest.BodyPublisher buildMultipartTranscriptionBodyPublisher(Path audioPath, String boundary) throws IOException {
        String lineBreak = "\r\n";
        List<byte[]> parts = new ArrayList<>();

        parts.add(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"model\"" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        parts.add((WHISPER_MODEL + lineBreak).getBytes(StandardCharsets.UTF_8));

        String fileName = audioPath.getFileName() == null ? "audio.wav" : audioPath.getFileName().toString();
        String mimeType = detectAudioMimeType(audioPath);
        parts.add(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
        parts.add(("Content-Type: " + mimeType + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        parts.add(Files.readAllBytes(audioPath));
        parts.add(lineBreak.getBytes(StandardCharsets.UTF_8));

        parts.add(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(parts);
    }

    private String detectAudioMimeType(Path audioPath) {
        try {
            String detected = Files.probeContentType(audioPath);
            if (detected != null && !detected.isBlank()) {
                return detected;
            }
        } catch (IOException ignored) {
            // Fallback below.
        }
        return "audio/wav";
    }

    private String executeWithFallback(String prompt, String fallback) {
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> callGroq(prompt, fallback), AI_EXECUTOR);

        // Do not block JavaFX UI thread.
        if (isJavaFxThread()) {
            return fallback;
        }

        try {
            return task.get(30, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String callGroq(String prompt, String fallback) {
        if (apiKey == null || apiKey.isBlank() || "YOUR_GROQ_KEY_HERE".equals(apiKey.trim())) {
            return fallback;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", MODEL);
            payload.put("temperature", 0.4);
            payload.put("max_tokens", 180);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are an assistant for student-professor rendez-vous planning. Keep responses short."));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", prompt));
            payload.put("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return fallback;
            }

            JSONObject root = new JSONObject(response.body());
            JSONArray choices = root.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return fallback;
            }

            JSONObject firstChoice = choices.optJSONObject(0);
            if (firstChoice == null) {
                return fallback;
            }

            JSONObject message = firstChoice.optJSONObject("message");
            if (message == null) {
                return fallback;
            }

            String content = message.optString("content", "").trim();
            return content.isEmpty() ? fallback : content;
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String loadApiKey() {
        Properties properties = new Properties();
        try (InputStream inputStream = AIService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (inputStream == null) {
                return "";
            }
            properties.load(inputStream);
            return properties.getProperty("groq.api.key", "").trim();
        } catch (IOException exception) {
            return "";
        }
    }

    private String normalizeSuggestResponse(String response, String fallback) {
        String cleaned = sanitizeSingleLine(response);
        if (cleaned.isBlank()) {
            return fallback;
        }
        if (!SUGGESTION_FORMAT_PATTERN.matcher(cleaned).matches()) {
            return fallback;
        }
        return cleaned.endsWith(".") ? cleaned : cleaned + ".";
    }

    private DayTime extractDayAndTime(List<String> slots) {
        if (slots == null || slots.isEmpty()) {
            return new DayTime("the next available day", "the best available time");
        }

        for (String slot : slots) {
            String value = normalize(slot, "");
            if (value.isBlank()) {
                continue;
            }

            Matcher dateTimeMatcher = SLOT_DATE_TIME_PATTERN.matcher(value);
            if (dateTimeMatcher.find()) {
                String day = toEnglishDay(dateTimeMatcher.group(1));
                String time = normalize(dateTimeMatcher.group(2), "the available time");
                return new DayTime(day, time);
            }

            Matcher timeMatcher = SLOT_TIME_PATTERN.matcher(value);
            if (timeMatcher.find()) {
                return new DayTime("the next available day", normalize(timeMatcher.group(1), "the available time"));
            }
        }

        return new DayTime("the next available day", "the best available time");
    }

    private String toEnglishDay(String isoDate) {
        String safeDate = normalize(isoDate, "");
        if (safeDate.isBlank()) {
            return "the next available day";
        }
        try {
            LocalDate date = LocalDate.parse(safeDate);
            return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } catch (DateTimeParseException exception) {
            return "the next available day";
        }
    }

    private String buildReminderTemplate(String studentName, String profName, String time, String subject) {
        return "Hi " + studentName + ", reminder: you have a meeting with "
                + profName + " tomorrow at " + time
                + ". Don't forget to prepare " + subject + "!";
    }

    private String sanitizeSingleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String normalizeToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isJavaFxThread() {
        try {
            return Platform.isFxApplicationThread();
        } catch (Exception exception) {
            return false;
        }
    }

    private record DayTime(String day, String time) {
    }

    public record VoiceBookingIntent(String professorName, String preferredDay, String subject) {
    }
}
