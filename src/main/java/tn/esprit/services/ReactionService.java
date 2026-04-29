package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReactionService {

    private final Connection connection = MyConnection.getInstance().getConnection();

    public void reactToPost(int postId, int userId, String type) throws SQLException {
        upsertReaction(postId, null, userId, type);
    }

    public void reactToReply(int replyId, int userId, String type) throws SQLException {
        upsertReaction(null, replyId, userId, type);
    }

    public void removeReactionFromPost(int postId, int userId) throws SQLException {
        String sql = "DELETE FROM reaction WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void removeReactionFromReply(int replyId, int userId) throws SQLException {
        String sql = "DELETE FROM reaction WHERE reply_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, replyId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public int countReactionsForPost(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reaction WHERE post_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countReactionsForReply(int replyId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reaction WHERE reply_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, replyId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public String getUserReactionForPost(int postId, int userId) throws SQLException {
        String sql = "SELECT type FROM reaction WHERE post_id = ? AND user_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("type") : null;
            }
        }
    }

    public Map<String, Integer> getReactionCountsForPost(int postId) throws SQLException {
        Map<String, Integer> counts = new HashMap<>();
        String sql = "SELECT type, COUNT(*) AS cnt FROM reaction WHERE post_id = ? GROUP BY type";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String type = normalizeReactionTypeForRead(rs.getString("type"));
                    if (type != null) {
                        counts.put(type, rs.getInt("cnt"));
                    }
                }
            }
        }
        return counts;
    }

    public String getUserReactionForReply(int replyId, int userId) throws SQLException {
        String sql = "SELECT type FROM reaction WHERE reply_id = ? AND user_id = ? ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, replyId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("type") : null;
            }
        }
    }

    private void upsertReaction(Integer postId, Integer replyId, int userId, String type) throws SQLException {
        if ((postId == null && replyId == null) || (postId != null && replyId != null)) {
            throw new IllegalArgumentException("A reaction must target exactly one post or one reply.");
        }
        if (userId <= 0) {
            throw new IllegalArgumentException("A valid user ID is required for reactions.");
        }

        String normalizedType = normalizeReactionType(type);

        String existingSql = postId != null
                ? "SELECT id FROM reaction WHERE post_id = ? AND user_id = ? ORDER BY id DESC"
                : "SELECT id FROM reaction WHERE reply_id = ? AND user_id = ? ORDER BY id DESC";

        Integer keepId = null;

        try (PreparedStatement ps = connection.prepareStatement(existingSql)) {
            if (postId != null) {
                ps.setInt(1, postId);
                ps.setInt(2, userId);
            } else {
                ps.setInt(1, replyId);
                ps.setInt(2, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    keepId = rs.getInt("id");
                }

                while (rs.next()) {
                    int duplicateId = rs.getInt("id");
                    try (PreparedStatement deleteDuplicate = connection.prepareStatement("DELETE FROM reaction WHERE id = ?")) {
                        deleteDuplicate.setInt(1, duplicateId);
                        deleteDuplicate.executeUpdate();
                    }
                }
            }
        }

        if (keepId == null) {
            String insertSql = "INSERT INTO reaction(type, created_at, post_id, reply_id, user_id) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, normalizedType);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

                if (postId != null) {
                    ps.setInt(3, postId);
                    ps.setNull(4, Types.INTEGER);
                } else {
                    ps.setNull(3, Types.INTEGER);
                    ps.setInt(4, replyId);
                }

                ps.setInt(5, userId);
                ps.executeUpdate();
            }
        } else {
            String updateSql = "UPDATE reaction SET type = ?, created_at = ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setString(1, normalizedType);
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(3, keepId);
                ps.executeUpdate();
            }
        }
    }

    private String normalizeReactionType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Reaction type is required.");
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeReactionTypeForRead(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }
}