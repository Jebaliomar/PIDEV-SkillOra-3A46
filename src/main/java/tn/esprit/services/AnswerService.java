package tn.esprit.services;

import tn.esprit.entities.Answer;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnswerService implements IAnswerService<Answer> {

    private final Connection cn;

    public AnswerService() {
        cn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Answer a) throws SQLException {
        if (!validerAnswer(a)) {
            throw new IllegalArgumentException("Les données de la réponse sont invalides.");
        }

        if (!questionExiste(a.getQuestionId())) {
            throw new IllegalArgumentException("La question associée n'existe pas.");
        }

        if ("STUDENT".equalsIgnoreCase(a.getRole())
                && reponseExistePourEtudiantEtQuestion(a.getStudentId(), a.getQuestionId())) {
            throw new IllegalArgumentException("Cet étudiant a déjà répondu à cette question.");
        }

        if (a.getCreatedAt() == null) {
            a.setCreatedAt(LocalDateTime.now());
        }

        if (a.getLastIntegrityEventAt() == null &&
                ((a.getPasteCount() != null && a.getPasteCount() > 0)
                        || (a.getTabSwitchCount() != null && a.getTabSwitchCount() > 0))) {
            a.setLastIntegrityEventAt(LocalDateTime.now());
        }

        if (a.getLastPlagiarismCheckAt() == null &&
                ((a.getWebPlagiarismPercent() != null && a.getWebPlagiarismPercent() > 0)
                        || (a.getAiSuspicionPercent() != null && a.getAiSuspicionPercent() > 0))) {
            a.setLastPlagiarismCheckAt(LocalDateTime.now());
        }

        String sql = "INSERT INTO answer " +
                "(content, is_correct, role, question_id, student_id, created_at, web_plagiarism_percent, ai_suspicion_percent, web_sources, paste_count, tab_switch_count, last_integrity_event_at, last_plagiarism_check_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, a.getContent());
            ps.setBoolean(2, a.getIsCorrect());
            ps.setString(3, a.getRole());
            ps.setInt(4, a.getQuestionId());
            ps.setInt(5, a.getStudentId());
            ps.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
            ps.setInt(7, a.getWebPlagiarismPercent());
            ps.setInt(8, a.getAiSuspicionPercent());
            ps.setString(9, a.getWebSources());
            ps.setInt(10, a.getPasteCount());
            ps.setInt(11, a.getTabSwitchCount());

            if (a.getLastIntegrityEventAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(a.getLastIntegrityEventAt()));
            } else {
                ps.setTimestamp(12, null);
            }

            if (a.getLastPlagiarismCheckAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(a.getLastPlagiarismCheckAt()));
            } else {
                ps.setTimestamp(13, null);
            }

            ps.executeUpdate();
            System.out.println("Answer ajoutée avec succès.");
        }
    }

    @Override
    public void modifier(Answer a) throws SQLException {
        if (a.getId() == null) {
            throw new IllegalArgumentException("L'id de la réponse est obligatoire pour la modification.");
        }

        if (!validerAnswer(a)) {
            throw new IllegalArgumentException("Les données de la réponse sont invalides.");
        }

        if (!questionExiste(a.getQuestionId())) {
            throw new IllegalArgumentException("La question associée n'existe pas.");
        }

        if (a.getCreatedAt() == null) {
            a.setCreatedAt(LocalDateTime.now());
        }

        String sql = "UPDATE answer SET " +
                "content=?, is_correct=?, role=?, question_id=?, student_id=?, created_at=?, " +
                "web_plagiarism_percent=?, ai_suspicion_percent=?, web_sources=?, paste_count=?, " +
                "tab_switch_count=?, last_integrity_event_at=?, last_plagiarism_check_at=? " +
                "WHERE id=?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, a.getContent());
            ps.setBoolean(2, a.getIsCorrect());
            ps.setString(3, a.getRole());
            ps.setInt(4, a.getQuestionId());
            ps.setInt(5, a.getStudentId());
            ps.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
            ps.setInt(7, a.getWebPlagiarismPercent());
            ps.setInt(8, a.getAiSuspicionPercent());
            ps.setString(9, a.getWebSources());
            ps.setInt(10, a.getPasteCount());
            ps.setInt(11, a.getTabSwitchCount());

            if (a.getLastIntegrityEventAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(a.getLastIntegrityEventAt()));
            } else {
                ps.setTimestamp(12, null);
            }

            if (a.getLastPlagiarismCheckAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(a.getLastPlagiarismCheckAt()));
            } else {
                ps.setTimestamp(13, null);
            }

            ps.setInt(14, a.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Answer modifiée avec succès.");
            } else {
                System.out.println("Answer non trouvée.");
            }
        }
    }

    @Override
    public void supprimer(Answer a) throws SQLException {
        if (a == null || a.getId() == null) {
            throw new IllegalArgumentException("L'id est obligatoire pour supprimer une réponse.");
        }

        String sql = "DELETE FROM answer WHERE id=?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, a.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Answer supprimée avec succès.");
            } else {
                System.out.println("Answer non trouvée.");
            }
        }
    }

    @Override
    public Answer chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM answer WHERE id=?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAnswer(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<Answer> recuperer() throws SQLException {
        String sql = "SELECT * FROM answer";
        return executeAndMapList(sql);
    }

    // =========================
    // TRI
    // =========================

    @Override
    public List<Answer> trierParDateAsc() throws SQLException {
        String sql = "SELECT * FROM answer ORDER BY created_at ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Answer> trierParDateDesc() throws SQLException {
        String sql = "SELECT * FROM answer ORDER BY created_at DESC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Answer> trierParPlagiatDesc() throws SQLException {
        String sql = "SELECT * FROM answer ORDER BY web_plagiarism_percent DESC, id ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Answer> trierParAiSuspicionDesc() throws SQLException {
        String sql = "SELECT * FROM answer ORDER BY ai_suspicion_percent DESC, id ASC";
        return executeAndMapList(sql);
    }

    // =========================
    // RECHERCHE / FILTRE
    // =========================

    @Override
    public List<Answer> rechercherParMotCle(String motCle) throws SQLException {
        String sql = "SELECT * FROM answer " +
                "WHERE LOWER(content) LIKE LOWER(?) " +
                "OR LOWER(web_sources) LIKE LOWER(?) " +
                "OR LOWER(role) LIKE LOWER(?)";

        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            String valeur = "%" + motCle + "%";
            ps.setString(1, valeur);
            ps.setString(2, valeur);
            ps.setString(3, valeur);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public List<Answer> filtrerParQuestionId(int questionId) throws SQLException {
        String sql = "SELECT * FROM answer WHERE question_id = ?";
        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, questionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public List<Answer> filtrerParStudentId(int studentId) throws SQLException {
        String sql = "SELECT * FROM answer WHERE student_id = ?";
        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public List<Answer> filtrerParRole(String role) throws SQLException {
        String sql = "SELECT * FROM answer WHERE LOWER(role) = LOWER(?)";
        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, role);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public List<Answer> filtrerParCorrect(boolean isCorrect) throws SQLException {
        String sql = "SELECT * FROM answer WHERE is_correct = ?";
        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setBoolean(1, isCorrect);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    // =========================
    // PAGINATION
    // =========================

    @Override
    public List<Answer> recupererParPage(int page, int taillePage) throws SQLException {
        if (page <= 0) {
            throw new IllegalArgumentException("Le numéro de page doit être supérieur à 0.");
        }

        if (taillePage <= 0) {
            throw new IllegalArgumentException("La taille de page doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM answer ORDER BY id ASC LIMIT ? OFFSET ?";
        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, taillePage);
            ps.setInt(2, (page - 1) * taillePage);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    // =========================
    // STATISTIQUES
    // =========================

    @Override
    public int countAnswers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM answer";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    @Override
    public double moyennePlagiatWeb() throws SQLException {
        String sql = "SELECT AVG(web_plagiarism_percent) FROM answer";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }

        return 0;
    }

    @Override
    public double moyenneAiSuspicion() throws SQLException {
        String sql = "SELECT AVG(ai_suspicion_percent) FROM answer";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }

        return 0;
    }

    @Override
    public int countCorrectes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM answer WHERE is_correct = true";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    @Override
    public int countIncorrectes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM answer WHERE is_correct = false";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    // =========================
    // VALIDATION
    // =========================

    @Override
    public boolean validerAnswer(Answer a) {
        if (a == null) return false;
        if (a.getContent() == null || a.getContent().trim().isEmpty()) return false;
        if (a.getIsCorrect() == null) return false;
        if (a.getRole() == null || a.getRole().trim().isEmpty()) return false;
        if (a.getQuestionId() == null || a.getQuestionId() <= 0) return false;
        if (a.getStudentId() == null || a.getStudentId() <= 0) return false;

        if (a.getWebPlagiarismPercent() == null || a.getWebPlagiarismPercent() < 0 || a.getWebPlagiarismPercent() > 100) return false;
        if (a.getAiSuspicionPercent() == null || a.getAiSuspicionPercent() < 0 || a.getAiSuspicionPercent() > 100) return false;
        if (a.getPasteCount() == null || a.getPasteCount() < 0) return false;
        if (a.getTabSwitchCount() == null || a.getTabSwitchCount() < 0) return false;

        return true;
    }

    // =========================
    // CONTRÔLE MÉTIER
    // =========================

    @Override
    public boolean questionExiste(int questionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM question WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, questionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    @Override
    public boolean reponseExistePourEtudiantEtQuestion(int studentId, int questionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM answer WHERE student_id = ? AND question_id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, questionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // =========================
    // FONCTIONNALITÉS AVANCÉES
    // =========================

    @Override
    public List<Answer> recupererRecentes(int limite) throws SQLException {
        if (limite <= 0) {
            throw new IllegalArgumentException("La limite doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM answer ORDER BY created_at DESC LIMIT ?";
        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public List<Answer> reponsesSuspectes(int seuilPlagiat, int seuilAi, int seuilPaste, int seuilTabSwitch) throws SQLException {
        String sql = "SELECT * FROM answer " +
                "WHERE web_plagiarism_percent >= ? " +
                "OR ai_suspicion_percent >= ? " +
                "OR paste_count >= ? " +
                "OR tab_switch_count >= ? " +
                "ORDER BY web_plagiarism_percent DESC, ai_suspicion_percent DESC";

        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, seuilPlagiat);
            ps.setInt(2, seuilAi);
            ps.setInt(3, seuilPaste);
            ps.setInt(4, seuilTabSwitch);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public List<Answer> topAnswersSuspectes(int limite) throws SQLException {
        if (limite <= 0) {
            throw new IllegalArgumentException("La limite doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM answer " +
                "ORDER BY (web_plagiarism_percent + ai_suspicion_percent + (paste_count * 5) + (tab_switch_count * 3)) DESC, id ASC " +
                "LIMIT ?";

        List<Answer> answers = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    answers.add(mapResultSetToAnswer(rs));
                }
            }
        }

        return answers;
    }

    @Override
    public Map<String, Integer> repartitionParRole() throws SQLException {
        String sql = "SELECT role, COUNT(*) AS total FROM answer GROUP BY role ORDER BY total DESC, role ASC";
        Map<String, Integer> repartition = new LinkedHashMap<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                repartition.put(rs.getString("role"), rs.getInt("total"));
            }
        }

        return repartition;
    }

    @Override
    public Map<String, Integer> repartitionCorrectIncorrect() throws SQLException {
        String sql = "SELECT is_correct, COUNT(*) AS total FROM answer GROUP BY is_correct ORDER BY is_correct DESC";
        Map<String, Integer> repartition = new LinkedHashMap<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                boolean correct = rs.getBoolean("is_correct");
                repartition.put(correct ? "Correctes" : "Incorrectes", rs.getInt("total"));
            }
        }

        return repartition;
    }

    @Override
    public boolean verifierCoherenceIntegrite(int answerId) throws SQLException {
        Answer a = chercherParId(answerId);

        if (a == null) {
            throw new IllegalArgumentException("Réponse introuvable.");
        }

        boolean coherencePlagiat =
                ((a.getWebPlagiarismPercent() != null && a.getWebPlagiarismPercent() > 0)
                        || (a.getAiSuspicionPercent() != null && a.getAiSuspicionPercent() > 0))
                        ? a.getLastPlagiarismCheckAt() != null
                        : true;

        boolean coherenceIntegrite =
                ((a.getPasteCount() != null && a.getPasteCount() > 0)
                        || (a.getTabSwitchCount() != null && a.getTabSwitchCount() > 0))
                        ? a.getLastIntegrityEventAt() != null
                        : true;

        return coherencePlagiat && coherenceIntegrite;
    }

    // =========================
    // MÉTHODES UTILITAIRES
    // =========================

    private Answer mapResultSetToAnswer(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp lastIntegrity = rs.getTimestamp("last_integrity_event_at");
        Timestamp lastPlagiarism = rs.getTimestamp("last_plagiarism_check_at");

        return new Answer(
                rs.getInt("id"),
                rs.getString("content"),
                rs.getBoolean("is_correct"),
                rs.getString("role"),
                rs.getInt("question_id"),
                rs.getInt("student_id"),
                createdAt != null ? createdAt.toLocalDateTime() : null,
                rs.getInt("web_plagiarism_percent"),
                rs.getInt("ai_suspicion_percent"),
                rs.getString("web_sources"),
                rs.getInt("paste_count"),
                rs.getInt("tab_switch_count"),
                lastIntegrity != null ? lastIntegrity.toLocalDateTime() : null,
                lastPlagiarism != null ? lastPlagiarism.toLocalDateTime() : null
        );
    }

    private List<Answer> executeAndMapList(String sql) throws SQLException {
        List<Answer> answers = new ArrayList<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                answers.add(mapResultSetToAnswer(rs));
            }
        }

        return answers;
    }
}