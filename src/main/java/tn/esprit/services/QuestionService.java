package tn.esprit.services;

import tn.esprit.entities.Question;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionService implements IQuestionService<Question> {

    private final Connection cn;

    public QuestionService() {
        cn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Question q) throws SQLException {
        if (!validerQuestion(q)) {
            throw new IllegalArgumentException("Les données de la question sont invalides.");
        }

        if (!evaluationExiste(q.getEvaluationId())) {
            throw new IllegalArgumentException("L'evaluation_id n'existe pas dans la table evaluation.");
        }

        String sql = "INSERT INTO question (content, explanation, type, score, evaluation_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, q.getContent());
            ps.setString(2, q.getExplanation());
            ps.setString(3, q.getType());
            ps.setInt(4, q.getScore());
            ps.setInt(5, q.getEvaluationId());

            ps.executeUpdate();
            System.out.println("Question ajoutée avec succès.");
        }
    }

    @Override
    public int ajouterEtRetournerId(Question q) throws SQLException {
        if (!validerQuestion(q)) {
            throw new IllegalArgumentException("Les données de la question sont invalides.");
        }

        if (!evaluationExiste(q.getEvaluationId())) {
            throw new IllegalArgumentException("L'evaluation_id n'existe pas dans la table evaluation.");
        }

        String sql = "INSERT INTO question (content, explanation, type, score, evaluation_id) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getContent());
            ps.setString(2, q.getExplanation());
            ps.setString(3, q.getType());
            ps.setInt(4, q.getScore());
            ps.setInt(5, q.getEvaluationId());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Impossible de récupérer l'id de la question ajoutée.");
    }

    @Override
    public void modifier(Question q) throws SQLException {
        if (q.getId() == null) {
            throw new IllegalArgumentException("L'id de la question est obligatoire pour la modification.");
        }

        if (!validerQuestion(q)) {
            throw new IllegalArgumentException("Les données de la question sont invalides.");
        }

        if (!evaluationExiste(q.getEvaluationId())) {
            throw new IllegalArgumentException("L'evaluation_id n'existe pas dans la table evaluation.");
        }

        String sql = "UPDATE question SET content = ?, explanation = ?, type = ?, score = ?, evaluation_id = ? WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, q.getContent());
            ps.setString(2, q.getExplanation());
            ps.setString(3, q.getType());
            ps.setInt(4, q.getScore());
            ps.setInt(5, q.getEvaluationId());
            ps.setInt(6, q.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Question modifiée avec succès.");
            } else {
                System.out.println("Question non trouvée.");
            }
        }
    }

    @Override
    public void supprimer(Question q) throws SQLException {
        if (q == null || q.getId() == null) {
            throw new IllegalArgumentException("L'id est obligatoire pour supprimer une question.");
        }

        String sql = "DELETE FROM question WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, q.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Question supprimée avec succès.");
            } else {
                System.out.println("Question non trouvée.");
            }
        }
    }

    @Override
    public Question chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM question WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToQuestion(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<Question> recuperer() throws SQLException {
        String sql = "SELECT * FROM question";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> trierParContenuAsc() throws SQLException {
        String sql = "SELECT * FROM question ORDER BY content ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> trierParContenuDesc() throws SQLException {
        String sql = "SELECT * FROM question ORDER BY content DESC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> trierParScoreAsc() throws SQLException {
        String sql = "SELECT * FROM question ORDER BY score ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> trierParScoreDesc() throws SQLException {
        String sql = "SELECT * FROM question ORDER BY score DESC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> trierParTypeAsc() throws SQLException {
        String sql = "SELECT * FROM question ORDER BY type ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> trierParTypeDesc() throws SQLException {
        String sql = "SELECT * FROM question ORDER BY type DESC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Question> rechercherParMotCle(String motCle) throws SQLException {
        String sql = "SELECT * FROM question " +
                "WHERE LOWER(content) LIKE LOWER(?) " +
                "OR LOWER(explanation) LIKE LOWER(?) " +
                "OR LOWER(type) LIKE LOWER(?)";

        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            String valeur = "%" + motCle + "%";
            ps.setString(1, valeur);
            ps.setString(2, valeur);
            ps.setString(3, valeur);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }

        return questions;
    }

    @Override
    public List<Question> filtrerParType(String type) throws SQLException {
        String sql = "SELECT * FROM question WHERE LOWER(type) = LOWER(?)";

        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, type);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }

        return questions;
    }

    @Override
    public List<Question> filtrerParEvaluationId(int evaluationId) throws SQLException {
        String sql = "SELECT * FROM question WHERE evaluation_id = ?";

        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }

        return questions;
    }

    @Override
    public List<Question> filtrerParScoreMin(int scoreMin) throws SQLException {
        String sql = "SELECT * FROM question WHERE score >= ?";

        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, scoreMin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }

        return questions;
    }

    @Override
    public List<Question> recupererParPage(int page, int taillePage) throws SQLException {
        if (page <= 0) {
            throw new IllegalArgumentException("Le numéro de page doit être supérieur à 0.");
        }

        if (taillePage <= 0) {
            throw new IllegalArgumentException("La taille de page doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM question ORDER BY id ASC LIMIT ? OFFSET ?";
        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, taillePage);
            ps.setInt(2, (page - 1) * taillePage);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }

        return questions;
    }

    @Override
    public int countQuestions() throws SQLException {
        String sql = "SELECT COUNT(*) FROM question";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    @Override
    public double moyenneScores() throws SQLException {
        String sql = "SELECT AVG(score) FROM question";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }

        return 0;
    }

    @Override
    public boolean validerQuestion(Question q) {
        if (q == null) {
            return false;
        }

        if (q.getContent() == null || q.getContent().trim().isEmpty()) {
            return false;
        }

        if (q.getExplanation() == null || q.getExplanation().trim().isEmpty()) {
            return false;
        }

        if (q.getType() == null || q.getType().trim().isEmpty()) {
            return false;
        }

        if (q.getScore() == null || q.getScore() < 0) {
            return false;
        }

        if (q.getEvaluationId() == null || q.getEvaluationId() <= 0) {
            return false;
        }

        return true;
    }

    @Override
    public boolean evaluationExiste(int evaluationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM evaluation WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    @Override
    public List<Question> topQuestions(int limite) throws SQLException {
        if (limite <= 0) {
            throw new IllegalArgumentException("La limite doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM question ORDER BY score DESC, id ASC LIMIT ?";
        List<Question> questions = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
        }

        return questions;
    }

    @Override
    public Map<String, Integer> repartitionParType() throws SQLException {
        String sql = "SELECT type, COUNT(*) AS total FROM question GROUP BY type ORDER BY total DESC, type ASC";
        Map<String, Integer> repartition = new LinkedHashMap<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                repartition.put(rs.getString("type"), rs.getInt("total"));
            }
        }

        return repartition;
    }

    @Override
    public int sommeScoresQuestionsParEvaluation(int evaluationId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(score), 0) AS somme FROM question WHERE evaluation_id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("somme");
                }
            }
        }

        return 0;
    }

    @Override
    public Integer totalScoreEvaluation(int evaluationId) throws SQLException {
        String sql = "SELECT total_score FROM evaluation WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, evaluationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_score");
                }
            }
        }

        return null;
    }

    @Override
    public boolean verifierCoherenceEvaluationQuestions(int evaluationId) throws SQLException {
        if (!evaluationExiste(evaluationId)) {
            throw new IllegalArgumentException("L'évaluation demandée n'existe pas.");
        }

        int sommeQuestions = sommeScoresQuestionsParEvaluation(evaluationId);
        Integer totalEvaluation = totalScoreEvaluation(evaluationId);

        if (totalEvaluation == null) {
            return false;
        }

        return sommeQuestions == totalEvaluation;
    }

    private Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("id"),
                rs.getString("content"),
                rs.getString("explanation"),
                rs.getString("type"),
                rs.getInt("score"),
                rs.getInt("evaluation_id")
        );
    }

    private List<Question> executeAndMapList(String sql) throws SQLException {
        List<Question> questions = new ArrayList<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                questions.add(mapResultSetToQuestion(rs));
            }
        }

        return questions;
    }
}