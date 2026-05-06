package tn.esprit.tools;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {

    private static Properties properties;
    private static Properties environment;
    private static boolean groqLoadMessagePrinted;

    private AppConfig() {
    }

    public static synchronized String get(String key, String fallback) {
        String value = load().getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static synchronized void loadEnvironment() {
        Properties env = environment();
        String groqApiKey = firstNonBlank(
                System.getenv("GROQ_API_KEY"),
                System.getProperty("GROQ_API_KEY"),
                env.getProperty("GROQ_API_KEY")
        );
        if (!groqApiKey.isBlank() && !groqLoadMessagePrinted) {
            System.out.println("Groq API key loaded successfully");
            groqLoadMessagePrinted = true;
        }
    }

    public static synchronized String getEnv(String key) {
        return getEnv(key, "");
    }

    public static synchronized String getEnv(String key, String fallback) {
        loadEnvironment();
        String value = firstNonBlank(
                System.getenv(key),
                System.getProperty(key),
                environment().getProperty(key),
                load().getProperty(key)
        );
        return value.isBlank() ? fallback : value;
    }

    public static synchronized String getFirstEnv(String fallback, String... keys) {
        loadEnvironment();
        if (keys != null) {
            for (String key : keys) {
                String value = getEnv(key, "");
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return fallback == null ? "" : fallback.trim();
    }

    public static synchronized String getGroqApiKey() {
        return getFirstEnv("", "GROQ_API_KEY", "GROQ_API_KEY1");
    }

    public static synchronized String getPlagiarismApiKey() {
        return getFirstEnv("", "PLAGIARISM_API_KEY");
    }

    public static synchronized boolean hasGroqApiKey() {
        return !getGroqApiKey().isBlank();
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

    private static Properties environment() {
        if (environment != null) {
            return environment;
        }

        environment = new Properties();
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();
            for (io.github.cdimascio.dotenv.DotenvEntry entry : dotenv.entries()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    environment.setProperty(entry.getKey(), entry.getValue().trim());
                    System.setProperty(entry.getKey(), entry.getValue().trim());
                }
            }
        } catch (Exception ignored) {
            // Environment variables are optional; callers validate required keys.
        }
        return environment;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
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
