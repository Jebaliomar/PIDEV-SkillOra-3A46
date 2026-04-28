package tn.esprit.tools;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {

    private static Properties properties;

    private AppConfig() {
    }

    public static synchronized String get(String key, String fallback) {
        String value = load().getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static synchronized int getInt(String key, int fallback) {
        String value = get(key, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static synchronized long getLong(String key, long fallback) {
        String value = get(key, "");
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Properties load() {
        if (properties != null) {
            return properties;
        }
        properties = new Properties();
        loadFromClasspath(properties, "app.properties");
        loadFromFile(properties, Path.of("app.properties"));
        return properties;
    }

    private static void loadFromClasspath(Properties target, String resourceName) {
        try (InputStream inputStream = AppConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream != null) {
                target.load(inputStream);
            }
        } catch (Exception ignored) {
            // Defaults keep the desktop app usable if optional config is missing.
        }
    }

    private static void loadFromFile(Properties target, Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            target.load(inputStream);
        } catch (Exception ignored) {
            // Local config is optional.
        }
    }
}
