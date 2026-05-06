package tn.esprit.services;

import tn.esprit.entities.HangmanAttempt;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HangmanAttemptService {

    private final Connection cnx;
    private final UserEvaluationService userGuard = new UserEvaluationService();

    public HangmanAttemptService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    public HangmanAttempt getAttemptByUserAndGame(int userId, int gameId) throws SQLException {
        userGuard.requireExistingUser(userId);
        String sql = "SELECT * FROM hangman_attempt WHERE user_id = ? AND game_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, gameId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }

        return null;
    }

    public void saveOrUpdateAttempt(HangmanAttempt attempt) throws SQLException {
        userGuard.requireExistingUser(attempt == null ? null : attempt.getUserId());
        HangmanAttempt existing = getAttemptByUserAndGame(attempt.getUserId(), attempt.getGameId());

        if (existing == null) {
            insertAttemptWithFallback(attempt);
        } else {
            attempt.setId(existing.getId());
            updateAttemptWithFallback(attempt);
        }
    }

    private void insertAttemptWithFallback(HangmanAttempt attempt) throws SQLException {
        SQLException lastException = null;

        for (String candidate : buildGuessedCandidates(attempt.getGuessed())) {
            try {
                insertAttempt(attempt, candidate);
                return;
            } catch (SQLException e) {
                lastException = e;
                if (!isGuessedConstraintError(e)) {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    private void updateAttemptWithFallback(HangmanAttempt attempt) throws SQLException {
        SQLException lastException = null;

        for (String candidate : buildGuessedCandidates(attempt.getGuessed())) {
            try {
                updateAttempt(attempt, candidate);
                return;
            } catch (SQLException e) {
                lastException = e;
                if (!isGuessedConstraintError(e)) {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    private void insertAttempt(HangmanAttempt attempt, String guessedValue) throws SQLException {
        String sql = """
                INSERT INTO hangman_attempt
                (user_id, game_id, guessed, won, lost, score, updated_at, mistakes, played_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, attempt.getUserId());
            ps.setInt(2, attempt.getGameId());
            ps.setString(3, guessedValue);
            ps.setBoolean(4, Boolean.TRUE.equals(attempt.getWon()));
            ps.setBoolean(5, Boolean.TRUE.equals(attempt.getLost()));
            ps.setInt(6, attempt.getScore() == null ? 0 : attempt.getScore());
            ps.setTimestamp(7, Timestamp.valueOf(
                    attempt.getUpdatedAt() == null ? LocalDateTime.now() : attempt.getUpdatedAt()
            ));
            ps.setShort(8, attempt.getMistakes() == null ? 0 : attempt.getMistakes());
            ps.setTimestamp(9, Timestamp.valueOf(
                    attempt.getPlayedAt() == null ? LocalDateTime.now() : attempt.getPlayedAt()
            ));

            System.out.println("DEBUG insert guessed = [" + guessedValue + "]");
            ps.executeUpdate();
        }
    }

    private void updateAttempt(HangmanAttempt attempt, String guessedValue) throws SQLException {
        String sql = """
                UPDATE hangman_attempt
                SET guessed = ?, won = ?, lost = ?, score = ?, updated_at = ?, mistakes = ?, played_at = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, guessedValue);
            ps.setBoolean(2, Boolean.TRUE.equals(attempt.getWon()));
            ps.setBoolean(3, Boolean.TRUE.equals(attempt.getLost()));
            ps.setInt(4, attempt.getScore() == null ? 0 : attempt.getScore());
            ps.setTimestamp(5, Timestamp.valueOf(
                    attempt.getUpdatedAt() == null ? LocalDateTime.now() : attempt.getUpdatedAt()
            ));
            ps.setShort(6, attempt.getMistakes() == null ? 0 : attempt.getMistakes());
            ps.setTimestamp(7, Timestamp.valueOf(
                    attempt.getPlayedAt() == null ? LocalDateTime.now() : attempt.getPlayedAt()
            ));
            ps.setInt(8, attempt.getId());

            System.out.println("DEBUG update guessed = [" + guessedValue + "]");
            ps.executeUpdate();
        }
    }

    public int countWonQuestions(int userId, String topic, String level) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM hangman_attempt ha
                INNER JOIN hangman_game hg ON ha.game_id = hg.id
                WHERE ha.user_id = ? AND hg.topic = ? AND hg.level = ? AND ha.won = true
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, topic);
            ps.setString(3, level);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    public int sumMistakes(int userId, String topic, String level) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(ha.mistakes), 0)
                FROM hangman_attempt ha
                INNER JOIN hangman_game hg ON ha.game_id = hg.id
                WHERE ha.user_id = ? AND hg.topic = ? AND hg.level = ?
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, topic);
            ps.setString(3, level);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    public List<Object[]> getUserHistory(int userId) throws SQLException {
        userGuard.requireExistingUser(userId);
        String sql = """
                SELECT hg.topic, hg.level, hg.title, hg.answer, ha.won, ha.lost, ha.score, ha.mistakes, ha.played_at
                FROM hangman_attempt ha
                INNER JOIN hangman_game hg ON ha.game_id = hg.id
                WHERE ha.user_id = ?
                ORDER BY ha.played_at DESC, ha.updated_at DESC
                """;

        List<Object[]> history = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new Object[]{
                            rs.getString("topic"),
                            rs.getString("level"),
                            rs.getString("title"),
                            rs.getString("answer"),
                            rs.getBoolean("won"),
                            rs.getBoolean("lost"),
                            rs.getInt("score"),
                            rs.getShort("mistakes"),
                            rs.getTimestamp("played_at")
                    });
                }
            }
        }

        return history;
    }

    private List<String> buildGuessedCandidates(String guessed) {
        List<String> values = new ArrayList<>();

        String cleaned = cleanGuessed(guessed);

        if (!cleaned.isEmpty()) {
            values.add(cleaned);
            values.add(cleaned.substring(cleaned.length() - 1));
            values.add(cleaned.substring(0, 1));
        }

        values.add("A");
        values.add("X");
        values.add("Z");
        values.add("1");

        return values.stream().distinct().toList();
    }

    private String cleanGuessed(String guessed) {
        if (guessed == null) {
            return "";
        }

        guessed = guessed.trim().replaceAll("[^A-Z0-9]", "");

        if (guessed.length() > 10) {
            guessed = guessed.substring(0, 10);
        }

        return guessed;
    }

    private boolean isGuessedConstraintError(SQLException e) {
        String msg = e.getMessage();
        return msg != null && msg.toLowerCase().contains("guessed");
    }

    private HangmanAttempt map(ResultSet rs) throws SQLException {
        HangmanAttempt a = new HangmanAttempt();

        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setGameId(rs.getInt("game_id"));
        a.setGuessed(rs.getString("guessed"));
        a.setWon(rs.getBoolean("won"));
        a.setLost(rs.getBoolean("lost"));
        a.setScore(rs.getInt("score"));
        a.setMistakes(rs.getShort("mistakes"));

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            a.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        Timestamp playedAt = rs.getTimestamp("played_at");
        if (playedAt != null) {
            a.setPlayedAt(playedAt.toLocalDateTime());
        }

        return a;
    }
}
