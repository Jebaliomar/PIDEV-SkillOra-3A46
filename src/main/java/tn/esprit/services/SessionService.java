package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Persists "remember me" session tokens. The raw token is given to the client
 * (and stored in a local file); the DB only keeps a SHA-256 hash so a DB leak
 * cannot be used to impersonate users.
 */
public class SessionService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Connection connection;
    private final UserService userService;

    public SessionService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.userService = new UserService();
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_sessions (" +
                "token_hash VARCHAR(128) NOT NULL PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "expires_at DATETIME NOT NULL, " +
                "created_at DATETIME NOT NULL, " +
                "INDEX idx_user (user_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Create a new session and return the raw token to hand to the client. */
    public String createSession(int userId, int validityDays) throws SQLException {
        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expires = now.plusDays(validityDays);

        String sql = "INSERT INTO user_sessions (token_hash, user_id, expires_at, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sha256(token));
            ps.setInt(2, userId);
            ps.setTimestamp(3, Timestamp.valueOf(expires));
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.executeUpdate();
        }
        return token;
    }

    /** Return the user matching a valid, non-expired token, or null otherwise. */
    public User validate(String token) {
        if (token == null || token.isEmpty()) return null;
        String hash = sha256(token);
        String sql = "SELECT user_id, expires_at FROM user_sessions WHERE token_hash = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Timestamp expires = rs.getTimestamp("expires_at");
                if (expires == null || expires.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    deleteByHash(hash);
                    return null;
                }
                int userId = rs.getInt("user_id");
                User user = userService.getById(userId);
                if (user == null || (user.getIsActive() != null && user.getIsActive() == 0)) {
                    deleteByHash(hash);
                    return null;
                }
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Remove a single session (logout on this device). */
    public void deleteSession(String token) {
        if (token == null || token.isEmpty()) return;
        deleteByHash(sha256(token));
    }

    private void deleteByHash(String hash) {
        String sql = "DELETE FROM user_sessions WHERE token_hash = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
