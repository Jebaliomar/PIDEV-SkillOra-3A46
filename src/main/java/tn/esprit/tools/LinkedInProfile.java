package tn.esprit.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * Local store for the LinkedIn URL + the raw imported JSON, keyed by user id.
 * URL lives in linkedin.properties; rich data lives in linkedin_data/{userId}.json.
 */
public final class LinkedInProfile {

    private static final Path BASE = Paths.get(System.getProperty("user.home"), ".skillora");
    private static final Path URL_FILE = BASE.resolve("linkedin.properties");
    private static final Path DATA_DIR = BASE.resolve("linkedin_data");
    private static final Path SNAP_DIR = BASE.resolve("linkedin_snaps");

    private LinkedInProfile() {}

    public static synchronized String getUrl(Integer userId) {
        if (userId == null) return null;
        Properties p = load();
        return p.getProperty(String.valueOf(userId));
    }

    public static synchronized void setUrl(Integer userId, String url) {
        if (userId == null) return;
        Properties p = load();
        if (url == null || url.isBlank()) {
            p.remove(String.valueOf(userId));
        } else {
            p.setProperty(String.valueOf(userId), url.trim());
        }
        save(p);
    }

    public static synchronized void saveData(Integer userId, String json) {
        if (userId == null || json == null) return;
        try {
            Files.createDirectories(DATA_DIR);
            Files.writeString(DATA_DIR.resolve(userId + ".json"), json, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns the local file path of a custom-uploaded snapshot for this user, or null.
     */
    public static synchronized String getSnapshotPath(Integer userId) {
        if (userId == null) return null;
        Path file = SNAP_DIR.resolve(userId + ".png");
        return Files.exists(file) ? file.toString() : null;
    }

    public static synchronized String saveSnapshotFile(Integer userId, Path source) throws IOException {
        if (userId == null || source == null) return null;
        Files.createDirectories(SNAP_DIR);
        Path target = SNAP_DIR.resolve(userId + ".png");
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public static synchronized void clearSnapshotFile(Integer userId) {
        if (userId == null) return;
        try {
            Files.deleteIfExists(SNAP_DIR.resolve(userId + ".png"));
        } catch (IOException ignored) {
        }
    }

    /**
     * Builds a public screenshot URL for the given LinkedIn profile via thum.io.
     * No API key needed; free tier renders a watermark.
     */
    public static String snapshotUrlFor(String profileUrl) {
        if (profileUrl == null || profileUrl.isBlank()) return null;
        return "https://image.thum.io/get/width/1200/crop/1500/noanimate/" + profileUrl.trim();
    }

    public static synchronized String loadData(Integer userId) {
        if (userId == null) return null;
        Path file = DATA_DIR.resolve(userId + ".json");
        try {
            if (Files.exists(file)) return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        return null;
    }

    private static Properties load() {
        Properties p = new Properties();
        try {
            if (Files.exists(URL_FILE)) {
                try (var in = Files.newInputStream(URL_FILE)) {
                    p.load(in);
                }
            }
        } catch (IOException ignored) {
        }
        return p;
    }

    private static void save(Properties p) {
        try {
            Files.createDirectories(URL_FILE.getParent());
            try (var out = Files.newOutputStream(URL_FILE)) {
                p.store(out, "SkillORA LinkedIn URLs");
            }
        } catch (IOException ignored) {
        }
    }
}
