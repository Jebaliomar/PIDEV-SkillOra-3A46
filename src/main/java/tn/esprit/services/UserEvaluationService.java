package tn.esprit.services;

import tn.esprit.entities.Evaluation;
import tn.esprit.entities.UserEvaluation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                    evaluation.setDuration(rs.getInt("duration"));
                    evaluation.setTotalScore(rs.getInt("total_score"));

                    Timestamp createdAt = rs.getTimestamp("created_at");
                    if (createdAt != null) {
                        evaluation.setCreatedAt(createdAt.toLocalDateTime());
                    }

                    evaluation.setDocxPath(rs.getString("docx_path"));
                    evaluation.setPdfPath(rs.getString("pdf_path"));

                    UserEvaluation userEvaluation = null;

                    int ueId = rs.getInt("ue_id");
                    if (!rs.wasNull()) {
                        userEvaluation = new UserEvaluation();
                        userEvaluation.setId(ueId);
                        userEvaluation.setUserId(rs.getInt("user_id"));
                        userEvaluation.setEvaluationId(rs.getInt("evaluation_id"));

                        Timestamp startedAt = rs.getTimestamp("started_at");
                        if (startedAt != null) {
                            userEvaluation.setStartedAt(startedAt.toLocalDateTime());
                        }

                        Timestamp submittedAt = rs.getTimestamp("submitted_at");
                        if (submittedAt != null) {
                            userEvaluation.setSubmittedAt(submittedAt.toLocalDateTime());
                        }

                        int score = rs.getInt("score");
                        if (!rs.wasNull()) {
                            userEvaluation.setScore(score);
                        }

                        userEvaluation.setAiFeedback(rs.getString("ai_feedback"));

                        Timestamp aiCorrectedAt = rs.getTimestamp("ai_corrected_at");
                        if (aiCorrectedAt != null) {
                            userEvaluation.setAiCorrectedAt(aiCorrectedAt.toLocalDateTime());
                        }

                        boolean corrected = rs.getBoolean("is_corrected");
                        if (!rs.wasNull()) {
                            userEvaluation.setIsCorrected(corrected);
                        }
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
        Integer existingId = findUserEvaluationId(userId, evaluationId);

        if (existingId == null) {
            String insertSql = "INSERT INTO user_evaluation " +
                    "(user_id, evaluation_id, started_at, submitted_at, score, ai_feedback, ai_corrected_at, is_corrected) " +
                    "VALUES (?, ?, NOW(), NOW(), NULL, ?, NULL, FALSE)";

            try (PreparedStatement ps = cnx.prepareStatement(insertSql)) {
                ps.setInt(1, userId);
                ps.setInt(2, evaluationId);
                ps.setString(3, responseText);
                ps.executeUpdate();
            }
        } else {
            String updateSql = "UPDATE user_evaluation " +
                    "SET submitted_at = NOW(), ai_feedback = ?, is_corrected = FALSE, ai_corrected_at = NULL " +
                    "WHERE user_id = ? AND evaluation_id = ?";

            try (PreparedStatement ps = cnx.prepareStatement(updateSql)) {
                ps.setString(1, responseText);
                ps.setInt(2, userId);
                ps.setInt(3, evaluationId);
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
            }
        }

        return null;
    }

    @Override
    public void saveOrUpdate(UserEvaluation ue) throws SQLException {
        if (ue == null) {
            return;
        }

        if (ue.getId() > 0) {
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

                    list.add(ue);
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

    private String getNullableColumn(ResultSet rs, String... columnNames) {
        for (String columnName : columnNames) {
            try {
                return rs.getString(columnName);
            } catch (SQLException ignored) {
            }
        }
        return null;
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
}