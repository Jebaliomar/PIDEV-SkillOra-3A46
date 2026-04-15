package tn.esprit.services;

import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.entities.User;
import tn.esprit.tools.MailService;
import tn.esprit.tools.MyConnection;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PasswordResetService {

    private static final int CODE_VALIDITY_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Connection connection;
    private final UserService userService;

    public PasswordResetService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.userService = new UserService();
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "user_id INT NOT NULL PRIMARY KEY, " +
                "code_hash VARCHAR(255) NOT NULL, " +
                "expires_at DATETIME NOT NULL, " +
                "created_at DATETIME NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a fresh 6-digit code for the given email, store its hash,
     * and email it to the user. Returns true if the email was sent.
     * Returns false if no account uses that email.
     */
    public boolean requestReset(String email) throws Exception {
        User user = userService.getByEmail(email);
        if (user == null) return false;

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        String hash = BCrypt.hashpw(code, BCrypt.gensalt(10));
        LocalDateTime expires = LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES);

        String upsert = "INSERT INTO password_reset_tokens (user_id, code_hash, expires_at, created_at) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE code_hash = VALUES(code_hash), expires_at = VALUES(expires_at), created_at = VALUES(created_at)";
        try (PreparedStatement ps = connection.prepareStatement(upsert)) {
            ps.setInt(1, user.getId());
            ps.setString(2, hash);
            ps.setTimestamp(3, Timestamp.valueOf(expires));
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }

        sendResetEmail(user, code);
        return true;
    }

    /** Verify the code for the given email without consuming it. */
    public boolean verifyCode(String email, String code) throws SQLException {
        User user = userService.getByEmail(email);
        if (user == null) return false;

        String sql = "SELECT code_hash, expires_at FROM password_reset_tokens WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, user.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                Timestamp expires = rs.getTimestamp("expires_at");
                if (expires == null || expires.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    return false;
                }
                String hash = rs.getString("code_hash");
                return BCrypt.checkpw(code, hash);
            }
        }
    }

    /** Verify code and reset password atomically. Returns true on success. */
    public boolean resetPassword(String email, String code, String newPassword) throws SQLException {
        if (!verifyCode(email, code)) return false;
        User user = userService.getByEmail(email);
        if (user == null) return false;

        userService.updatePassword(user.getId(), newPassword);

        String del = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(del)) {
            ps.setInt(1, user.getId());
            ps.executeUpdate();
        }
        return true;
    }

    private void sendResetEmail(User user, String code) throws Exception {
        String name = user.getFirstName() != null ? user.getFirstName() : "there";
        String html = "<!DOCTYPE html><html><body style=\"font-family:Segoe UI,Arial,sans-serif;background:#f5f7fb;padding:32px;\">"
                + "<div style=\"max-width:520px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;box-shadow:0 4px 20px rgba(0,0,0,0.06);\">"
                + "<div style=\"text-align:center;margin-bottom:24px;\">"
                + "<div style=\"display:inline-block;background:linear-gradient(135deg,#22d3ee,#7C3AED);color:#fff;font-weight:700;font-size:22px;width:48px;height:48px;line-height:48px;border-radius:12px;\">S</div>"
                + "<h1 style=\"color:#0f172a;font-size:22px;margin:12px 0 4px;\">SkillORA</h1>"
                + "</div>"
                + "<h2 style=\"color:#0f172a;font-size:18px;margin:0 0 12px;\">Reset your password</h2>"
                + "<p style=\"color:#475569;font-size:14px;line-height:1.6;margin:0 0 20px;\">Hi " + escape(name) + ",<br/>"
                + "We received a request to reset your SkillORA password. Use the code below to continue. It expires in " + CODE_VALIDITY_MINUTES + " minutes.</p>"
                + "<div style=\"text-align:center;margin:24px 0;\">"
                + "<div style=\"display:inline-block;background:#f1f5f9;color:#0f172a;font-size:32px;letter-spacing:10px;font-weight:700;padding:18px 28px;border-radius:12px;font-family:Consolas,Menlo,monospace;\">" + code + "</div>"
                + "</div>"
                + "<p style=\"color:#64748b;font-size:13px;line-height:1.6;margin:0;\">If you didn't request a password reset you can safely ignore this email — your password won't change.</p>"
                + "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:24px 0;\"/>"
                + "<p style=\"color:#94a3b8;font-size:12px;text-align:center;margin:0;\">SkillORA · Secure password reset</p>"
                + "</div></body></html>";
        MailService.sendHtml(user.getEmail(), "Reset your SkillORA password", html);
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
