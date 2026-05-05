package tn.esprit.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinkSecurityService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)"
    );
    private static final String URLHAUS_ENDPOINT = "https://urlhaus-api.abuse.ch/v1/url/";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String apiKey;
    private final boolean enabled;
    private volatile boolean lookupEnabled;

    public LinkSecurityService() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.apiKey = dotenv.get("URLHAUS_AUTH_KEY");
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.lookupEnabled = enabled;
    }

    /**
     * Check if content contains any unsafe URLs
     * @param content The text content to check (post or reply)
     * @return true if all URLs are safe or no API key configured, false if unsafe URL found
     * @throws IllegalStateException if an unsafe URL is detected
     */
    public void validateContent(String content) {
        if (!enabled || content == null || content.isBlank()) {
            return;
        }

        Set<String> urls = extractUrls(content);
        if (urls.isEmpty()) {
            return;
        }

        for (String url : urls) {
            if (!isSafeUrl(url)) {
                throw new IllegalStateException(
                        "Your content contains a link that has been flagged as potentially unsafe: " + url
                );
            }
        }
    }

    /**
     * Extract all URLs from text
     */
    private Set<String> extractUrls(String text) {
        Set<String> urls = new HashSet<>();
        Matcher matcher = URL_PATTERN.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    /**
     * Check if a URL is safe using Google Web Risk API
     */
    private boolean isSafeUrl(String urlString) {
        if (!lookupEnabled) {
            return true;
        }

        try {
            URI.create(urlString);

            HttpRequest request = HttpRequest.newBuilder(URI.create(URLHAUS_ENDPOINT))
                    .header("Auth-Key", apiKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("url=" + URLEncoder.encode(urlString, StandardCharsets.UTF_8)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                System.err.println("URLhaus API returned HTTP " + response.statusCode() + ": " + response.body());
                if (response.statusCode() == 403) {
                    lookupEnabled = false;
                }
                return true;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String queryStatus = json.has("query_status") ? json.get("query_status").getAsString() : "";

            if ("no_results".equalsIgnoreCase(queryStatus)) {
                return true;
            }

            if ("ok".equalsIgnoreCase(queryStatus)) {
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (IOException e) {
            System.err.println("URLhaus API check failed: " + e.getMessage());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("URLhaus API check failed: " + e.getMessage());
            return true;
        }
    }

    /**
     * Check if URL checking is enabled (API key configured)
     */
    public boolean isEnabled() {
        return enabled;
    }
}
