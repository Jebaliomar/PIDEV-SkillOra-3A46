package tn.esprit.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Saves / loads the "remember me" session token to a per-user file on disk so
 * the app can auto-sign-in on the next launch.
 */
public final class SessionStore {

    private static final Path TOKEN_PATH = Paths.get(
            System.getProperty("user.home"), ".skillora", "session.token");

    private SessionStore() {}

    public static void save(String token) {
        try {
            Files.createDirectories(TOKEN_PATH.getParent());
            Files.writeString(TOKEN_PATH, token, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String load() {
        try {
            if (!Files.exists(TOKEN_PATH)) return null;
            String v = Files.readString(TOKEN_PATH, StandardCharsets.UTF_8).trim();
            return v.isEmpty() ? null : v;
        } catch (IOException e) {
            return null;
        }
    }

    public static void clear() {
        try {
            Files.deleteIfExists(TOKEN_PATH);
        } catch (IOException e) {
            // ignore
        }
    }
}
