package tn.esprit.services;

import tn.esprit.entities.HangmanGame;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class HangmanGameService {

    private final Connection cnx;

    public HangmanGameService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    public List<String> getAllTopics() throws SQLException {
        String sql = "SELECT DISTINCT topic FROM hangman_game WHERE topic IS NOT NULL AND TRIM(topic) <> '' ORDER BY topic";
        List<String> topics = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                topics.add(rs.getString("topic"));
            }
        }

        return topics;
    }

    public List<HangmanGame> getGamesByTopicAndLevel(String topic, String level) throws SQLException {
        String sql = "SELECT * FROM hangman_game WHERE topic = ? AND level = ? ORDER BY id";
        List<HangmanGame> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, topic);
            ps.setString(2, level);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }

        Collections.shuffle(list);
        return list;
    }

    /**
     * Retourne uniquement des questions uniques dans un topic/level,
     * en supprimant les doublons par answer + title.
     */
    public List<HangmanGame> getUniqueGamesByTopicAndLevel(String topic, String level) throws SQLException {
        List<HangmanGame> allGames = getGamesByTopicAndLevel(topic, level);
        List<HangmanGame> uniqueGames = new ArrayList<>();

        Set<String> seenAnswers = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();

        for (HangmanGame game : allGames) {
            String answerKey = normalize(game.getAnswer());
            String titleKey = normalize(game.getTitle());

            boolean sameAnswer = !answerKey.isBlank() && seenAnswers.contains(answerKey);
            boolean sameTitle = !titleKey.isBlank() && seenTitles.contains(titleKey);

            if (sameAnswer || sameTitle) {
                continue;
            }

            uniqueGames.add(game);

            if (!answerKey.isBlank()) {
                seenAnswers.add(answerKey);
            }
            if (!titleKey.isBlank()) {
                seenTitles.add(titleKey);
            }
        }

        return uniqueGames;
    }

    public int countGamesByTopicAndLevel(String topic, String level) throws SQLException {
        String sql = "SELECT COUNT(*) FROM hangman_game WHERE topic = ? AND level = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, topic);
            ps.setString(2, level);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    public HangmanGame saveGeneratedGame(HangmanGame game) throws SQLException {
        String sql = """
                INSERT INTO hangman_game (title, topic, level, hint, answer, max_mistakes, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, game.getTitle());
            ps.setString(2, game.getTopic());
            ps.setString(3, game.getLevel());
            ps.setString(4, game.getHint());
            ps.setString(5, game.getAnswer());
            ps.setShort(6, game.getMaxMistakes() == null ? 6 : game.getMaxMistakes());
            ps.setTimestamp(7, Timestamp.valueOf(game.getCreatedAt() == null ? LocalDateTime.now() : game.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    game.setId(rs.getInt(1));
                }
            }
        }

        return game;
    }

    private HangmanGame map(ResultSet rs) throws SQLException {
        HangmanGame game = new HangmanGame();
        game.setId(rs.getInt("id"));
        game.setTitle(rs.getString("title"));
        game.setTopic(rs.getString("topic"));
        game.setLevel(rs.getString("level"));
        game.setHint(rs.getString("hint"));
        game.setAnswer(rs.getString("answer"));
        game.setMaxMistakes(rs.getShort("max_mistakes"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        game.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime() : LocalDateTime.now());

        return game;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }
}