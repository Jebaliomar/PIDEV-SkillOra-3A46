package tn.esprit.services;

import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserEvaluationService implements IUserEvaluationService {

    private final Connection cnx;

    public UserEvaluationService() {
        cnx = MyConnection.getInstance().getConnection();
    }

    @Override
    public List<Object[]> getUserEvaluationsWithEvaluation(int userId) throws SQLException {
        List<Object[]> list = new ArrayList<>();

        String sql = "SELECT " +
                "e.id AS e_id, e.title, e.description, e.type, e.duration, e.total_score, e.created_at, e.docx_path, e.pdf_path, " +
                "ue.id AS ue_id, ue.user_id, ue.evaluation_id, ue.started_at, ue.submitted_at, ue.score, ue.ai_feedback, ue.ai_corrected_at, ue.is_corrected " +
                "FROM evaluation e " +
                "LEFT JOIN user_evaluation ue ON e.id = ue.evaluation_id AND ue.user_id = ? " +
                "WHERE UPPER(e.type) IN ('EXAM', 'QUIZ') " +
                "ORDER BY e.id DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Evaluation evaluation = new Evaluation();
                    evaluation.setId(rs.getInt("e_id"));
                    evaluation.setTitle(rs.getString("title"));
                    evaluation.setDescription(rs.getString("description"));
                    evaluation.setType(rs.getString("type"));

                    int duration = rs.getInt("duration");
                    if (!rs.wasNull()) {
                        evaluation.setDuration(duration);
                    }

                    int totalScore = rs.getInt("total_score");
                    if (!rs.wasNull()) {
                        evaluation.setTotalScore(totalScore);
                    }

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        evaluation.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    evaluation.setDocxPath(rs.getString("docx_path"));
                    evaluation.setPdfPath(rs.getString("pdf_path"));

                    UserEvaluation userEvaluation = null;

                    int ueId = rs.getInt("ue_id");
                    if (!rs.wasNull()) {
                        userEvaluation = mapUserEvaluationFromJoinedResult(rs);
                    }

                    list.add(new Object[]{evaluation, userEvaluation});
                }
            }
        }

        return list;
    }

    @Override
    public Integer findUserEvaluationId(int userId, int evaluationId) throws SQLException {
        String sql = "SELECT id FROM user_evaluation WHERE user_id = ? AND evaluation_id = ? LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }

        return null;
    }

    @Override
    public void createStartedUserEvaluation(int userId, int evaluationId) throws SQLException {
        Integer existingId = findUserEvaluationId(userId, evaluationId);

        if (existingId != null) {
            return;
        }

        String sql = "INSERT INTO user_evaluation " +
                "(user_id, evaluation_id, started_at, submitted_at, score, ai_feedback, ai_corrected_at, is_corrected) " +
                "VALUES (?, ?, NOW(), NULL, NULL, NULL, NULL, FALSE)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, evaluationId);
            ps.executeUpdate();
        }
    }

    @Override
    public void submitExamResponse(int userId, int evaluationId, String responseText) throws SQLException {
        if (responseText == null) {
            responseText = "";
        }

        responseText = responseText.trim();

        Integer existingId = findUserEvaluationId(userId, evaluationId);

        int aiPercent = estimateAiUsagePercent(responseText);
        int plagiarismPercent = computeMaxInternalPlagiarism(userId, evaluationId, responseText);
        boolean fraudAttempt = aiPercent >= 70 || plagiarismPercent >= 60;
        String fraudReason = buildFraudReason(aiPercent, plagiarismPercent);
        int score = calculateScore(responseText, aiPercent, plagiarismPercent);
        String feedback = buildFeedback(score, aiPercent, plagiarismPercent, fraudAttempt, fraudReason);

        String payload = buildPayload(
                responseText,
                aiPercent,
                plagiarismPercent,
                fraudAttempt,
                fraudReason,
                feedback
        );

        if (existingId == null) {
            String insertSql = "INSERT INTO user_evaluation " +
                    "(user_id, evaluation_id, started_at, submitted_at, score, ai_feedback, ai_corrected_at, is_corrected) " +
                    "VALUES (?, ?, NOW(), NOW(), ?, ?, NOW(), TRUE)";

            try (PreparedStatement ps = cnx.prepareStatement(insertSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, evaluationId);
                ps.setInt(3, score);
                ps.setString(4, payload);
                ps.executeUpdate();
            }
        } else {
            String updateSql = "UPDATE user_evaluation " +
                    "SET submitted_at = NOW(), score = ?, ai_feedback = ?, is_corrected = TRUE, ai_corrected_at = NOW() " +
                    "WHERE user_id = ? AND evaluation_id = ?";

            try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
                ps.setInt(1, score);
                ps.setString(2, payload);
                ps.setInt(3, userId);
                ps.setInt(4, evaluationId);
                ps.executeUpdate();
            }
        }
    }

    @Override
    public UserEvaluation getByUserIdAndEvaluationId(int userId, int evaluationId) throws SQLException {
        String sql = "SELECT * FROM user_evaluation WHERE user_id = ? AND evaluation_id = ? LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserEvaluation(rs);
                }
            }
        }

        return null;
    }

    @Override
    public void saveOrUpdate(UserEvaluation ue) throws SQLException {
        if (ue == null) {
            return;
        }

        if (ue.getId() != null && ue.getId() > 0) {
            String sql = "UPDATE user_evaluation " +
                    "SET user_id = ?, evaluation_id = ?, started_at = ?, submitted_at = ?, score = ?, ai_feedback = ?, ai_corrected_at = ?, is_corrected = ? " +
                    "WHERE id = ?";

            try (PreparedStatement ps = cnx.prepareStatement(sql)) {
                ps.setInt(1, ue.getUserId());
                ps.setInt(2, ue.getEvaluationId());

                if (ue.getStartedAt() != null) {
                    ps.setTimestamp(3, Timestamp.valueOf(ue.getStartedAt()));
                } else {
                    ps.setNull(3, Types.TIMESTAMP);
                }

                if (ue.getSubmittedAt() != null) {
                    ps.setTimestamp(4, Timestamp.valueOf(ue.getSubmittedAt()));
                } else {
                    ps.setNull(4, Types.TIMESTAMP);
                }

                if (ue.getScore() != null) {
                    ps.setInt(5, ue.getScore());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }

                ps.setString(6, ue.getAiFeedback());

                if (ue.getAiCorrectedAt() != null) {
                    ps.setTimestamp(7, Timestamp.valueOf(ue.getAiCorrectedAt()));
                } else {
                    ps.setNull(7, Types.TIMESTAMP);
                }

                if (ue.getIsCorrected() != null) {
                    ps.setBoolean(8, ue.getIsCorrected());
                } else {
                    ps.setNull(8, Types.BOOLEAN);
                }

                ps.setInt(9, ue.getId());
                ps.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO user_evaluation " +
                    "(user_id, evaluation_id, started_at, submitted_at, score, ai_feedback, ai_corrected_at, is_corrected) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, ue.getUserId());
                ps.setInt(2, ue.getEvaluationId());

                if (ue.getStartedAt() != null) {
                    ps.setTimestamp(3, Timestamp.valueOf(ue.getStartedAt()));
                } else {
                    ps.setNull(3, Types.TIMESTAMP);
                }

                if (ue.getSubmittedAt() != null) {
                    ps.setTimestamp(4, Timestamp.valueOf(ue.getSubmittedAt()));
                } else {
                    ps.setNull(4, Types.TIMESTAMP);
                }

                if (ue.getScore() != null) {
                    ps.setInt(5, ue.getScore());
                } else {
                    ps.setNull(5, Types.INTEGER);
                }

                ps.setString(6, ue.getAiFeedback());

                if (ue.getAiCorrectedAt() != null) {
                    ps.setTimestamp(7, Timestamp.valueOf(ue.getAiCorrectedAt()));
                } else {
                    ps.setNull(7, Types.TIMESTAMP);
                }

                if (ue.getIsCorrected() != null) {
                    ps.setBoolean(8, ue.getIsCorrected());
                } else {
                    ps.setNull(8, Types.BOOLEAN);
                }

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        ue.setId(rs.getInt(1));
                    }
                }
            }
        }
    }

    @Override
    public List<UserEvaluation> getSubmittedUserEvaluationsByEvaluationId(int evaluationId) throws SQLException {
        List<UserEvaluation> list = new ArrayList<>();

        String sql = "SELECT * FROM user_evaluation " +
                "WHERE evaluation_id = ? AND submitted_at IS NOT NULL " +
                "ORDER BY submitted_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUserEvaluation(rs));
                }
            }
        }

        return list;
    }

    @Override
    public String getUserFullNameById(int userId) throws SQLException {
        String sql = "SELECT first_name, last_name FROM users WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");

                    String fullName = ((firstName != null ? firstName : "") + " " +
                            (lastName != null ? lastName : "")).trim();

                    if (!fullName.isEmpty()) {
                        return fullName;
                    }
                }
            }
        }

        return "Utilisateur #" + userId;
    }

    @Override
    public boolean hasSubmittedResponses(int evaluationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_evaluation WHERE evaluation_id = ? AND submitted_at IS NOT NULL";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    private UserEvaluation mapUserEvaluation(ResultSet rs) throws SQLException {
        UserEvaluation ue = new UserEvaluation();

        ue.setId(rs.getInt("id"));
        ue.setUserId(rs.getInt("user_id"));
        ue.setEvaluationId(rs.getInt("evaluation_id"));

        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            ue.setStartedAt(startedAt.toLocalDateTime());
        }

        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            ue.setSubmittedAt(submittedAt.toLocalDateTime());
        }

        int score = rs.getInt("score");
        if (!rs.wasNull()) {
            ue.setScore(score);
        }

        ue.setAiFeedback(rs.getString("ai_feedback"));

        Timestamp aiCorrectedAt = rs.getTimestamp("ai_corrected_at");
        if (aiCorrectedAt != null) {
            ue.setAiCorrectedAt(aiCorrectedAt.toLocalDateTime());
        }

        boolean corrected = rs.getBoolean("is_corrected");
        if (!rs.wasNull()) {
            ue.setIsCorrected(corrected);
        }

        return ue;
    }

    private UserEvaluation mapUserEvaluationFromJoinedResult(ResultSet rs) throws SQLException {
        UserEvaluation ue = new UserEvaluation();

        ue.setId(rs.getInt("ue_id"));
        ue.setUserId(rs.getInt("user_id"));
        ue.setEvaluationId(rs.getInt("evaluation_id"));

        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            ue.setStartedAt(startedAt.toLocalDateTime());
        }

        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        if (submittedAt != null) {
            ue.setSubmittedAt(submittedAt.toLocalDateTime());
        }

        int score = rs.getInt("score");
        if (!rs.wasNull()) {
            ue.setScore(score);
        }

        ue.setAiFeedback(rs.getString("ai_feedback"));

        Timestamp aiCorrectedAt = rs.getTimestamp("ai_corrected_at");
        if (aiCorrectedAt != null) {
            ue.setAiCorrectedAt(aiCorrectedAt.toLocalDateTime());
        }

        boolean corrected = rs.getBoolean("is_corrected");
        if (!rs.wasNull()) {
            ue.setIsCorrected(corrected);
        }

        return ue;
    }

    private int estimateAiUsagePercent(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        String normalized = text.toLowerCase().replaceAll("\\s+", " ").trim();
        int score = 0;

        if (normalized.length() > 1200) score += 10;
        if (normalized.length() > 1800) score += 10;

        if (normalized.contains("en conclusion")) score += 8;
        if (normalized.contains("il est important de noter")) score += 12;
        if (normalized.contains("dans le cadre de")) score += 8;
        if (normalized.contains("cependant")) score += 4;
        if (normalized.contains("de plus")) score += 4;
        if (normalized.contains("en effet")) score += 4;
        if (normalized.contains("il convient de")) score += 10;
        if (normalized.contains("par ailleurs")) score += 5;
        if (normalized.contains("il ressort que")) score += 8;

        int sentenceCount = text.split("[.!?]+").length;
        if (sentenceCount > 0) {
            double avgLen = (double) text.length() / sentenceCount;
            if (avgLen > 120) score += 20;
            else if (avgLen > 90) score += 10;
        }

        if (normalized.matches(".*\\b(chatgpt|openai|intelligence artificielle|ia)\\b.*")) {
            score += 25;
        }

        if (!text.contains("...") && !text.contains("??") && text.length() > 600) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private int computeMaxInternalPlagiarism(int currentUserId, int evaluationId, String responseText) throws SQLException {
        List<UserEvaluation> existingResponses = getSubmittedUserEvaluationsByEvaluationId(evaluationId);
        int max = 0;

        for (UserEvaluation ue : existingResponses) {
            if (ue.getUserId() != null && ue.getUserId() == currentUserId) {
                continue;
            }

            String otherPayload = ue.getAiFeedback();
            String otherAnswer = extractAnswer(otherPayload);

            if (otherAnswer.isBlank()) {
                otherAnswer = otherPayload != null ? otherPayload : "";
            }

            int similarity = compareTexts(responseText, otherAnswer);
            if (similarity > max) {
                max = similarity;
            }
        }

        return max;
    }

    private int compareTexts(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) {
            return 0;
        }

        Set<String> setA = tokenize(a);
        Set<String> setB = tokenize(b);

        if (setA.isEmpty() || setB.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        double jaccard = (double) intersection.size() / (double) union.size();
        return (int) Math.round(jaccard * 100);
    }

    private Set<String> tokenize(String text) {
        Set<String> result = new HashSet<>();

        String normalized = text.toLowerCase()
                .replaceAll("[^a-zA-Z0-9àâçéèêëîïôûùüÿñæœ\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (normalized.isBlank()) {
            return result;
        }

        String[] tokens = normalized.split(" ");
        for (String token : tokens) {
            if (token.length() >= 4) {
                result.add(token);
            }
        }

        return result;
    }

    private String buildFraudReason(int aiPercent, int plagiarismPercent) {
        if (aiPercent >= 70 && plagiarismPercent >= 60) {
            return "Usage IA élevé et plagiat détecté";
        }
        if (aiPercent >= 70) {
            return "Usage IA suspect élevé";
        }
        if (plagiarismPercent >= 60) {
            return "Plagiat détecté";
        }
        return "Aucune fraude majeure détectée";
    }

    private int calculateScore(String responseText, int aiPercent, int plagiarismPercent) {
        if (responseText == null || responseText.isBlank()) {
            return 0;
        }

        int score = 100;

        if (responseText.length() < 80) {
            score -= 25;
        } else if (responseText.length() < 150) {
            score -= 10;
        }

        score -= aiPercent / 10;
        score -= plagiarismPercent / 8;

        if (score < 0) score = 0;
        if (score > 100) score = 100;

        return score;
    }

    private String buildFeedback(int score, int aiPercent, int plagiarismPercent, boolean fraudAttempt, String fraudReason) {
        StringBuilder sb = new StringBuilder();

        sb.append("Score calculé : ").append(score).append("/100\n");
        sb.append("Usage IA estimé : ").append(aiPercent).append("%\n");
        sb.append("Plagiat détecté : ").append(plagiarismPercent).append("%\n");
        sb.append("Fraude : ").append(fraudAttempt ? "Oui" : "Non").append("\n");
        sb.append("Raison : ").append(fraudReason).append("\n\n");

        if (fraudAttempt) {
            sb.append("Votre soumission présente une suspicion de fraude. ");
            sb.append("Elle peut être revue par l'administration.");
        } else {
            sb.append("Votre réponse a été enregistrée et analysée avec succès.");
        }

        return sb.toString();
    }

    private String buildPayload(String answer,
                                int aiPercent,
                                int plagiarismPercent,
                                boolean fraudAttempt,
                                String fraudReason,
                                String feedback) {
        return "ANSWER:::\n" +
                safe(answer) + "\n" +
                ":::END_ANSWER\n" +
                "AI_PERCENT:::" + aiPercent + "\n" +
                "PLAGIARISM_PERCENT:::" + plagiarismPercent + "\n" +
                "FRAUD_ATTEMPT:::" + fraudAttempt + "\n" +
                "FRAUD_REASON:::" + safe(fraudReason) + "\n" +
                "FEEDBACK:::\n" +
                safe(feedback) + "\n" +
                ":::END_FEEDBACK";
    }

    private String extractAnswer(String payload) {
        return extractBlock(payload, "ANSWER:::", ":::END_ANSWER");
    }

    private String extractBlock(String payload, String startToken, String endToken) {
        if (payload == null || payload.isBlank()) {
            return "";
        }

        int start = payload.indexOf(startToken);
        int end = payload.indexOf(endToken);

        if (start == -1 || end == -1 || end <= start) {
            return "";
        }

        start += startToken.length();
        return payload.substring(start, end).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}