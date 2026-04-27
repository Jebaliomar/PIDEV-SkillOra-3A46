package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.esprit.entities.Course;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CourseInsightsService {

    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final String STACKOVERFLOW_SITE = "stackoverflow";
    private static final int RECENT_MONTHS = 12;
    private static final Pattern WORD_SPLIT = Pattern.compile("\\s+");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CourseInsightsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CourseInsights getInsightsForCourse(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required.");
        }

        String keyword = resolveKeyword(course);
        WikipediaData wikipedia = getWikipediaData(keyword);
        GithubData github = getGithubData(keyword);
        StackOverflowData stackOverflow = getStackOverflowData(keyword);

        int repoCount = github.totalRepositories();
        int topRepoStars = github.topRepository() == null ? 0 : github.topRepository().stars();
        int soTotal = stackOverflow.totalQuestions();
        int soRecent = stackOverflow.recentQuestions();

        List<InsightReason> reasons = List.of(
                new InsightReason(
                        "What You Will Learn",
                        wikipedia.ok() ? wikipedia.title() : "Unavailable",
                        wikipedia.ok() ? wikipedia.extract() : "Wikipedia summary is currently unavailable for this keyword.",
                        wikipedia.ok() ? wikipedia.url() : null
                ),
                new InsightReason(
                        "Real-World Usage",
                        repoCount > 0 ? signalLevel(repoCount, 20_000, 3_000) : "Unavailable",
                        repoCount > 0
                                ? "Around " + formatNumber(repoCount) + " public projects were found, which suggests practical industry usage."
                                : "GitHub repository volume is currently unavailable.",
                        "https://github.com/search?q=" + encode(keyword)
                ),
                new InsightReason(
                        "Trust by Developers",
                        topRepoStars > 0 ? signalLevel(topRepoStars, 50_000, 5_000) : "Unavailable",
                        topRepoStars > 0
                                ? safe(github.topRepository().name(), "A top project") + " has about " + formatNumber(topRepoStars) + " stars, a strong trust signal from developers."
                                : "Top repository stars are currently unavailable.",
                        github.topRepository() == null ? "" : github.topRepository().url()
                ),
                new InsightReason(
                        "Help Availability",
                        soTotal > 0 ? signalLevel(soTotal, 50_000, 8_000) : "Unavailable",
                        soTotal > 0
                                ? "There are roughly " + formatNumber(soTotal) + " community Q&A threads to support troubleshooting."
                                : "StackOverflow total activity is currently unavailable.",
                        "https://stackoverflow.com/search?q=" + encode(keyword)
                ),
                new InsightReason(
                        "Current Momentum",
                        soRecent > 0 ? signalLevel(soRecent, 3_000, 500) : "Unavailable",
                        soRecent > 0
                                ? "About " + formatNumber(soRecent) + " new discussions were posted in the last " + RECENT_MONTHS + " months."
                                : "Recent StackOverflow activity is currently unavailable.",
                        "https://stackoverflow.com/search?q=" + encode(keyword)
                )
        );

        List<String> labels = List.of("Project Ecosystem", "Top Project Trust", "Learning Support", "Current Momentum");
        List<Integer> values = List.of(repoCount, topRepoStars, soTotal, soRecent);
        return new CourseInsights(keyword, reasons, new InsightChart(labels, values, normalizeForChart(values)));
    }

    private String resolveKeyword(Course course) {
        String title = safe(course.getTitle(), "");
        String description = stripTags(safe(course.getDescription(), ""));
        String category = safe(course.getCategory(), "");
        String text = (title + " " + description).toLowerCase(Locale.ROOT).trim();

        String detected = detectKnownTechnology(text);
        if (detected != null) {
            return detected;
        }

        for (String candidate : extractCandidates(title + " " + description)) {
            if (isValidStackOverflowTag(candidate)) {
                return candidate;
            }
        }

        String fallback = !category.isBlank() ? category.trim() : title.trim();
        return fallback.isBlank() ? "technology" : fallback;
    }

    private String detectKnownTechnology(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("symfony", "symfony");
        map.put("laravel", "laravel");
        map.put("react", "reactjs");
        map.put("angular", "angular");
        map.put("vue", "vue.js");
        map.put("nodejs", "node.js");
        map.put("node", "node.js");
        map.put("typescript", "typescript");
        map.put("javascript", "javascript");
        map.put("python", "python");
        map.put("django", "django");
        map.put("flask", "flask");
        map.put("java", "java");
        map.put("spring", "spring-boot");
        map.put("php", "php");
        map.put("mysql", "mysql");
        map.put("postgresql", "postgresql");
        map.put("postgres", "postgresql");
        map.put("mongodb", "mongodb");
        map.put("docker", "docker");
        map.put("kubernetes", "kubernetes");
        map.put("machine learning", "machine-learning");
        map.put("data science", "data-science");
        map.put("deep learning", "deep-learning");
        map.put("devops", "devops");

        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private List<String> extractCandidates(String text) {
        String normalized = stripTags(text).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9+#.\\-\\s]", " ");
        List<String> stopwords = List.of(
                "course", "courses", "learn", "learning", "complete", "guide", "bootcamp",
                "masterclass", "introduction", "intro", "advanced", "beginner", "intermediate",
                "practical", "ultimate", "from", "zero", "project", "projects", "with", "and",
                "for", "the", "using", "build", "building", "development"
        );

        List<String> words = Arrays.stream(WORD_SPLIT.split(normalized))
                .map(String::trim)
                .filter(word -> word.length() >= 3)
                .filter(word -> !stopwords.contains(word))
                .toList();

        List<String> candidates = new ArrayList<>(words);
        for (int i = 0; i < words.size() - 1; i++) {
            candidates.add(words.get(i) + "-" + words.get(i + 1));
        }
        return candidates.stream().distinct().limit(12).toList();
    }

    private boolean isValidStackOverflowTag(String candidate) {
        String cacheKey = "tag:" + candidate;
        Boolean cached = getCached(cacheKey, Boolean.class);
        if (cached != null) {
            return cached;
        }

        String url = "https://api.stackexchange.com/2.3/tags/" + encode(candidate) + "/info?site=" + STACKOVERFLOW_SITE;
        JsonNode data = requestJson(url, Map.of());
        boolean valid = data != null && data.path("items").path(0).path("count").asInt(0) > 100;
        putCached(cacheKey, valid);
        return valid;
    }

    private WikipediaData getWikipediaData(String keyword) {
        String cacheKey = "wikipedia:" + keyword;
        WikipediaData cached = getCached(cacheKey, WikipediaData.class);
        if (cached != null) {
            return cached;
        }

        JsonNode summary = requestJson("https://en.wikipedia.org/api/rest_v1/page/summary/" + encode(keyword), Map.of());
        WikipediaData data = parseWikipediaSummary(summary, keyword);
        if (!data.ok()) {
            JsonNode search = requestJson("https://en.wikipedia.org/w/api.php?action=query&list=search&format=json&srlimit=1&utf8=1&srsearch=" + encode(keyword), Map.of());
            String firstTitle = search == null ? "" : search.path("query").path("search").path(0).path("title").asText("");
            if (!firstTitle.isBlank()) {
                data = parseWikipediaSummary(requestJson("https://en.wikipedia.org/api/rest_v1/page/summary/" + encode(firstTitle), Map.of()), firstTitle);
            }
        }

        putCached(cacheKey, data);
        return data;
    }

    private WikipediaData parseWikipediaSummary(JsonNode node, String keyword) {
        if (node == null || node.path("extract").asText("").isBlank()) {
            return WikipediaData.unavailable();
        }
        return new WikipediaData(
                true,
                node.path("title").asText(keyword),
                node.path("extract").asText("").trim(),
                node.path("content_urls").path("desktop").path("page").asText("")
        );
    }

    private GithubData getGithubData(String keyword) {
        String cacheKey = "github:" + keyword;
        GithubData cached = getCached(cacheKey, GithubData.class);
        if (cached != null) {
            return cached;
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/vnd.github+json");
        headers.put("User-Agent", "SkillORA-Course-Insights");
        String token = loadGithubToken();
        if (!token.isBlank()) {
            headers.put("Authorization", "Bearer " + token);
        }

        String url = "https://api.github.com/search/repositories?q=" + encode(keyword + " in:name,description") + "&sort=stars&order=desc&per_page=5";
        JsonNode data = requestJson(url, headers);
        if (data == null) {
            GithubData unavailable = new GithubData(false, 0, null);
            putCached(cacheKey, unavailable);
            return unavailable;
        }

        JsonNode top = data.path("items").path(0);
        GithubRepository topRepository = top.isMissingNode()
                ? null
                : new GithubRepository(
                top.path("full_name").asText(""),
                top.path("stargazers_count").asInt(0),
                top.path("html_url").asText(""),
                top.path("description").asText("")
        );
        GithubData githubData = new GithubData(true, data.path("total_count").asInt(0), topRepository);
        putCached(cacheKey, githubData);
        return githubData;
    }

    private StackOverflowData getStackOverflowData(String keyword) {
        String cacheKey = "stackoverflow:" + keyword;
        StackOverflowData cached = getCached(cacheKey, StackOverflowData.class);
        if (cached != null) {
            return cached;
        }

        String base = "https://api.stackexchange.com/2.3/search/advanced?site=" + STACKOVERFLOW_SITE + "&pagesize=1&filter=total";
        JsonNode totalData = requestJson(base + "&order=desc&sort=relevance&title=" + encode(keyword), Map.of());

        long fromDate = LocalDate.now().minusMonths(RECENT_MONTHS).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        JsonNode recentData = requestJson(base + "&order=desc&sort=activity&fromdate=" + fromDate + "&title=" + encode(keyword), Map.of());

        StackOverflowData data = new StackOverflowData(
                totalData != null || recentData != null,
                totalData == null ? 0 : totalData.path("total").asInt(0),
                recentData == null ? 0 : recentData.path("total").asInt(0)
        );
        putCached(cacheKey, data);
        return data;
    }

    private JsonNode requestJson(String url, Map<String, String> headers) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(REQUEST_TIMEOUT)
                    .GET();
            headers.forEach(builder::header);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                return null;
            }
            return objectMapper.readTree(response.body());
        } catch (Exception e) {
            return null;
        }
    }

    private List<Integer> normalizeForChart(List<Integer> values) {
        List<Double> logs = values.stream()
                .map(value -> Math.log10(Math.max(0, value) + 1))
                .toList();
        double max = logs.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (max <= 0) {
            return values.stream().map(value -> 0).toList();
        }
        return logs.stream().map(value -> (int) Math.round((value / max) * 100)).toList();
    }

    private String signalLevel(int value, int highThreshold, int mediumThreshold) {
        if (value >= highThreshold) {
            return "Very Strong";
        }
        if (value >= mediumThreshold) {
            return "Strong";
        }
        return value > 0 ? "Growing" : "Unavailable";
    }

    private String loadGithubToken() {
        String env = System.getenv("GITHUB_TOKEN");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }

        Properties properties = new Properties();
        loadPropertiesFromClasspath(properties, "/app.properties");
        loadPropertiesFromFile(properties, Path.of("app.properties"));
        String configured = properties.getProperty("GITHUB_TOKEN", properties.getProperty("github.token", ""));
        if (configured != null && !configured.isBlank()) {
            return unquote(configured.trim());
        }
        return readSymfonyEnvValue(Path.of("/Users/nidhal/Desktop/SkillORALatest/.env.local"), "GITHUB_TOKEN");
    }

    private void loadPropertiesFromClasspath(Properties properties, String resourcePath) {
        try (InputStream inputStream = CourseInsightsService.class.getResourceAsStream(resourcePath)) {
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

    private <T> T getCached(String key, Class<T> type) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(key);
            return null;
        }
        return type.cast(entry.value());
    }

    private void putCached(String key, Object value) {
        cache.put(key, new CacheEntry(value, Instant.now().plus(CACHE_TTL)));
    }

    private String stripTags(String value) {
        return value == null ? "" : value.replaceAll("<[^>]*>", " ");
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(Objects.toString(value, ""), StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String unquote(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String formatNumber(int value) {
        return String.format(Locale.US, "%,d", value);
    }

    private record CacheEntry(Object value, Instant expiresAt) {
    }

    public record CourseInsights(String keyword, List<InsightReason> reasons, InsightChart chart) {
    }

    public record InsightReason(String title, String value, String detail, String link) {
    }

    public record InsightChart(List<String> labels, List<Integer> values, List<Integer> normalized) {
    }

    private record WikipediaData(boolean ok, String title, String extract, String url) {
        static WikipediaData unavailable() {
            return new WikipediaData(false, "", "", "");
        }
    }

    private record GithubData(boolean ok, int totalRepositories, GithubRepository topRepository) {
    }

    private record GithubRepository(String name, int stars, String url, String description) {
    }

    private record StackOverflowData(boolean ok, int totalQuestions, int recentQuestions) {
    }
}
