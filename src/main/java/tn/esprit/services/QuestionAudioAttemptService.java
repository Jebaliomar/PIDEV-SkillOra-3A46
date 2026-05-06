package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class QuestionAudioAttemptService {

    private final Connection cnx;

    public QuestionAudioAttemptService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    public void incrementPlayCount(int userId, int questionId) {
        try {
            Integer id = findAttemptId(userId, questionId);

            if (id == null) {
                String insertSql = """
                        INSERT INTO question_audio_attempt (user_id, question_id, play_count, updated_at)
                        VALUES (?, ?, ?, ?)
                        """;

                try (PreparedStatement ps = cnx.prepareStatement(insertSql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, questionId);
                    ps.setInt(3, 1);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    ps.executeUpdate();
                }
            } else {
                String updateSql = """
                        UPDATE question_audio_attempt
                        SET play_count = COALESCE(play_count, 0) + 1,
                            updated_at = ?
                        WHERE id = ?
                        """;

                try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
                    ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
            }

        } catch (Exception ignored) {
            // ne pas casser le quiz si la table n'existe pas ou si un souci survient
        }
    }

    private Integer findAttemptId(int userId, int questionId) throws SQLException {
        String sql = """
                SELECT id
                FROM question_audio_attempt
                WHERE user_id = ? AND question_id = ?
                LIMIT 1
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, questionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        return null;
    }
}