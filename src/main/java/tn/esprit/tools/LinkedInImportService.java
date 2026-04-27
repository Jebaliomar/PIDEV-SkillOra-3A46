package tn.esprit.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports a LinkedIn profile via RapidAPI's Fresh LinkedIn Profile Data API.
 * <p>
 * Setup: drop your RapidAPI key into ~/.skillora/api.properties as
 *   rapidapi.key=YOUR_KEY
 * (or set the env var RAPIDAPI_KEY).
 */
public class LinkedInImportService {

    private static final String DEFAULT_HOST = "fresh-linkedin-profile-data-api.p.rapidapi.com";
    private static final String DEFAULT_PATH = "/api/scraper/profile";
    private static final Pattern PROFILE_URL = Pattern.compile(
            "^https?://([a-z]{2,3}\\.)?linkedin\\.com/in/([^/?#]+)/?", Pattern.CASE_INSENSITIVE);
    private static final Path KEY_FILE =
            Paths.get(System.getProperty("user.home"), ".skillora", "api.properties");

    public static class Experience {
        public String title;
        public String company;
        public String startsAt;
        public String endsAt;
        public String description;
    }

    public static class Education {
        public String school;
        public String degree;
        public String field;
        public String startsAt;
        public String endsAt;
    }

    public static class Result {
        public String firstName;
        public String lastName;
        public String headline;
        public String summary;
        public String country;
        public String city;
        public String profilePicUrl;
        public String currentCompany;
        public String currentTitle;
        public List<Experience> experiences = new ArrayList<>();
        public List<Education> education = new ArrayList<>();
        public List<String> skills = new ArrayList<>();
        public List<String> certifications = new ArrayList<>();
        public List<String> languages = new ArrayList<>();
        public String rawJson;
        public String warning;
    }

    public Result fetch(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Please paste your LinkedIn profile URL.");
        }
        Matcher m = PROFILE_URL.matcher(url.trim());
        if (!m.find()) {
            throw new IllegalArgumentException("URL must look like https://www.linkedin.com/in/your-handle/");
        }

        Properties cfg = readConfig();
        String apiKey = firstNonBlank(System.getenv("RAPIDAPI_KEY"), cfg.getProperty("rapidapi.key"));
        if (apiKey == null) {
            throw new IllegalStateException(
                    "No RapidAPI key found. Add rapidapi.key=YOUR_KEY to "
                            + KEY_FILE + " (or set env RAPIDAPI_KEY) and try again.");
        }
        String host = firstNonBlank(cfg.getProperty("rapidapi.host"), DEFAULT_HOST);
        String path = firstNonBlank(cfg.getProperty("rapidapi.path"), DEFAULT_PATH);

        String endpoint = "https://" + host + path
                + "?linkedin_url=" + URLEncoder.encode(url.trim(), StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("x-rapidapi-key", apiKey)
                .header("x-rapidapi-host", host)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        int code = resp.statusCode();
        if (code == 401 || code == 403) {
            throw new IllegalStateException("RapidAPI rejected the key (HTTP " + code
                    + "). Check rapidapi.key in " + KEY_FILE + " and that you subscribed to the API.");
        }
        if (code == 404) {
            // Fallback: some variants of this API expose /api/profile or /get-linkedin-profile
            return tryFallback(client, host, apiKey, url.trim());
        }
        if (code == 429) {
            throw new IllegalStateException("RapidAPI rate limit / quota exhausted (HTTP 429).");
        }
        if (code < 200 || code >= 300) {
            throw new IllegalStateException("RapidAPI HTTP " + code + ": " + truncate(resp.body(), 240));
        }

        return parse(resp.body());
    }

    private Result tryFallback(HttpClient client, String host, String apiKey, String profileUrl) throws Exception {
        String[] candidates = { "/api/profile", "/get-linkedin-profile", "/profile" };
        for (String path : candidates) {
            String endpoint = "https://" + host + path
                    + "?linkedin_url=" + URLEncoder.encode(profileUrl, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("x-rapidapi-key", apiKey)
                    .header("x-rapidapi-host", host)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() >= 200 && r.statusCode() < 300) {
                return parse(r.body());
            }
        }
        throw new IllegalStateException("This RapidAPI endpoint did not return profile data. "
                + "Open the API docs on RapidAPI, copy the exact profile-lookup path, "
                + "and add it to " + KEY_FILE + " as rapidapi.path=/your/path");
    }

    private Result parse(String body) {
        Result r = new Result();
        r.rawJson = body;
        JSONObject root = new JSONObject(body);

        // Some APIs wrap the profile under "data" or "response"
        JSONObject o = root;
        if (root.has("data") && root.opt("data") instanceof JSONObject) o = root.getJSONObject("data");
        else if (root.has("response") && root.opt("response") instanceof JSONObject) o = root.getJSONObject("response");

        r.firstName = firstString(o, "first_name", "firstName", "given_name");
        r.lastName = firstString(o, "last_name", "lastName", "family_name");
        if (r.firstName == null && r.lastName == null) {
            String full = firstString(o, "full_name", "fullName", "name");
            if (full != null) {
                String[] parts = full.trim().split("\\s+", 2);
                if (parts.length >= 1) r.firstName = parts[0];
                if (parts.length >= 2) r.lastName = parts[1];
            }
        }
        r.headline = firstString(o, "headline", "occupation", "sub_title", "subTitle");
        r.summary = firstString(o, "summary", "about", "description", "bio");
        r.country = firstString(o, "country_full_name", "country", "location_country", "geoCountry");
        r.city = firstString(o, "city", "location_city", "location");
        r.profilePicUrl = firstString(o, "profile_pic_url", "profilePicUrl", "profile_image_url",
                "profile_picture", "profile_image", "image_url", "avatar");

        JSONArray experiences = firstArray(o, "experiences", "experience", "positions", "work_experience");
        if (experiences != null) {
            for (int i = 0; i < experiences.length(); i++) {
                JSONObject e = experiences.optJSONObject(i);
                if (e == null) continue;
                Experience exp = new Experience();
                exp.title = firstString(e, "title", "position", "role");
                exp.company = firstString(e, "company", "company_name", "companyName", "organization");
                exp.startsAt = formatDate(e, "starts_at", "start_date", "startDate", "from", "date_from");
                exp.endsAt = formatDate(e, "ends_at", "end_date", "endDate", "to", "date_to");
                exp.description = firstString(e, "description", "summary");
                r.experiences.add(exp);
                if (i == 0) {
                    if (r.currentTitle == null) r.currentTitle = exp.title;
                    if (exp.endsAt == null && exp.company != null) r.currentCompany = exp.company;
                }
            }
        }

        JSONArray edu = firstArray(o, "education", "educations", "schools");
        if (edu != null) {
            for (int i = 0; i < edu.length(); i++) {
                JSONObject e = edu.optJSONObject(i);
                if (e == null) continue;
                Education ed = new Education();
                ed.school = firstString(e, "school", "school_name", "institution", "name");
                ed.degree = firstString(e, "degree_name", "degree");
                ed.field = firstString(e, "field_of_study", "fieldOfStudy", "field");
                ed.startsAt = formatDate(e, "starts_at", "start_date", "startDate", "from", "date_from");
                ed.endsAt = formatDate(e, "ends_at", "end_date", "endDate", "to", "date_to");
                r.education.add(ed);
            }
        }

        JSONArray skills = firstArray(o, "skills", "skill_list", "skillList");
        if (skills != null) {
            for (int i = 0; i < skills.length(); i++) {
                Object item = skills.opt(i);
                if (item instanceof String s && !s.isBlank()) r.skills.add(s);
                else if (item instanceof JSONObject jo) {
                    String name = firstString(jo, "name", "skill", "title");
                    if (name != null && !name.isBlank()) r.skills.add(name);
                }
            }
        }

        JSONArray certs = firstArray(o, "certifications", "certificates");
        if (certs != null) {
            for (int i = 0; i < certs.length(); i++) {
                JSONObject c = certs.optJSONObject(i);
                String name = c != null ? firstString(c, "name", "title") : certs.optString(i, null);
                if (name != null && !name.isBlank()) r.certifications.add(name);
            }
        }

        JSONArray langs = firstArray(o, "languages_and_proficiencies", "languages");
        if (langs != null) {
            for (int i = 0; i < langs.length(); i++) {
                Object item = langs.opt(i);
                if (item instanceof String s && !s.isBlank()) r.languages.add(s);
                else if (item instanceof JSONObject jo) {
                    String name = firstString(jo, "name", "language");
                    if (name != null && !name.isBlank()) r.languages.add(name);
                }
            }
        }

        return r;
    }

    private String firstString(JSONObject o, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.isNull(k)) {
                String v = o.optString(k, null);
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    private JSONArray firstArray(JSONObject o, String... keys) {
        for (String k : keys) {
            JSONArray a = o.optJSONArray(k);
            if (a != null) return a;
        }
        return null;
    }

    private String formatDate(JSONObject parent, String... keys) {
        for (String k : keys) {
            if (!parent.has(k) || parent.isNull(k)) continue;
            Object v = parent.get(k);
            if (v instanceof String s) return s;
            if (v instanceof JSONObject d) {
                Integer year = d.has("year") && !d.isNull("year") ? d.optInt("year") : null;
                Integer month = d.has("month") && !d.isNull("month") ? d.optInt("month") : null;
                if (year == null) return null;
                if (month == null) return String.valueOf(year);
                String[] mn = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                int mi = Math.max(1, Math.min(12, month)) - 1;
                return mn[mi] + " " + year;
            }
        }
        return null;
    }

    private Properties readConfig() {
        Properties p = new Properties();
        try {
            if (Files.exists(KEY_FILE)) {
                try (var in = Files.newInputStream(KEY_FILE)) {
                    p.load(in);
                }
            }
        } catch (IOException ignored) {
        }
        return p;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v.trim();
        return null;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
