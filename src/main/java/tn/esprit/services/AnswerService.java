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
                && a.getStudentId() != null
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

            if (a.getStudentId() != null) {
                ps.setInt(5, a.getStudentId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            if (a.getCreatedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }

            if (a.getWebPlagiarismPercent() != null) {
                ps.setInt(7, a.getWebPlagiarismPercent());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            if (a.getAiSuspicionPercent() != null) {
                ps.setInt(8, a.getAiSuspicionPercent());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            if (a.getWebSources() != null && !a.getWebSources().trim().isEmpty()) {
                ps.setString(9, a.getWebSources());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }

            if (a.getPasteCount() != null) {
                ps.setInt(10, a.getPasteCount());
            } else {
                ps.setNull(10, Types.INTEGER);
            }

            if (a.getTabSwitchCount() != null) {
                ps.setInt(11, a.getTabSwitchCount());
            } else {
                ps.setNull(11, Types.INTEGER);
            }

            if (a.getLastIntegrityEventAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(a.getLastIntegrityEventAt()));
            } else {
                ps.setNull(12, Types.TIMESTAMP);
            }

            if (a.getLastPlagiarismCheckAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(a.getLastPlagiarismCheckAt()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
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

            if (a.getStudentId() != null) {
                ps.setInt(5, a.getStudentId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }

            if (a.getCreatedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }

            if (a.getWebPlagiarismPercent() != null) {
                ps.setInt(7, a.getWebPlagiarismPercent());
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            if (a.getAiSuspicionPercent() != null) {
                ps.setInt(8, a.getAiSuspicionPercent());
            } else {
                ps.setNull(8, Types.INTEGER);
            }

            if (a.getWebSources() != null && !a.getWebSources().trim().isEmpty()) {
                ps.setString(9, a.getWebSources());
            } else {
                ps.setNull(9, Types.VARCHAR);
            }

            if (a.getPasteCount() != null) {
                ps.setInt(10, a.getPasteCount());
            } else {
                ps.setNull(10, Types.INTEGER);
            }

            if (a.getTabSwitchCount() != null) {
                ps.setInt(11, a.getTabSwitchCount());
            } else {
                ps.setNull(11, Types.INTEGER);
            }

            if (a.getLastIntegrityEventAt() != null) {
                ps.setTimestamp(12, Timestamp.valueOf(a.getLastIntegrityEventAt()));
            } else {
                ps.setNull(12, Types.TIMESTAMP);
            }

            if (a.getLastPlagiarismCheckAt() != null) {
                ps.setTimestamp(13, Timestamp.valueOf(a.getLastPlagiarismCheckAt()));
            } else {
                ps.setNull(13, Types.TIMESTAMP);
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

    @Override
    public boolean validerAnswer(Answer a) {
        if (a == null) return false;
        if (a.getContent() == null || a.getContent().trim().isEmpty()) return false;
        if (a.getIsCorrect() == null) return false;
        if (a.getRole() == null || a.getRole().trim().isEmpty()) return false;
        if (a.getQuestionId() == null || a.getQuestionId() <= 0) return false;

        if ("QUIZ_OPTION".equalsIgnoreCase(a.getRole())) {
            return true;
        }

        if (a.getStudentId() == null || a.getStudentId() <= 0) return false;
        if (a.getWebPlagiarismPercent() == null || a.getWebPlagiarismPercent() < 0 || a.getWebPlagiarismPercent() > 100) return false;
        if (a.getAiSuspicionPercent() == null || a.getAiSuspicionPercent() < 0 || a.getAiSuspicionPercent() > 100) return false;
        if (a.getPasteCount() == null || a.getPasteCount() < 0) return false;
        if (a.getTabSwitchCount() == null || a.getTabSwitchCount() < 0) return false;

        return true;
    }

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

    public List<Answer> recupererOptionsQuizParQuestion(int questionId) throws SQLException {
        String sql = "SELECT * FROM answer WHERE question_id = ? AND role = 'QUIZ_OPTION' ORDER BY id ASC";
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

    public void supprimerOptionsQuizParQuestion(int questionId) throws SQLException {
        String sql = "DELETE FROM answer WHERE question_id = ? AND role = 'QUIZ_OPTION'";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.executeUpdate();
        }
    }

    public void enregistrerOptionsQuiz(int questionId,
                                       String optionA,
                                       String optionB,
                                       String optionC,
                                       String optionD,
                                       String bonneReponse) throws SQLException {

        supprimerOptionsQuizParQuestion(questionId);

        ajouterOptionQuiz(questionId, optionA, "A".equalsIgnoreCase(bonneReponse));
        ajouterOptionQuiz(questionId, optionB, "B".equalsIgnoreCase(bonneReponse));
        ajouterOptionQuiz(questionId, optionC, "C".equalsIgnoreCase(bonneReponse));
        ajouterOptionQuiz(questionId, optionD, "D".equalsIgnoreCase(bonneReponse));
    }

    private void ajouterOptionQuiz(int questionId, String contenu, boolean isCorrect) throws SQLException {
        Answer a = new Answer();
        a.setContent(contenu);
        a.setIsCorrect(isCorrect);
        a.setRole("QUIZ_OPTION");
        a.setQuestionId(questionId);
        a.setStudentId(null);
        a.setCreatedAt(LocalDateTime.now());
        a.setWebPlagiarismPercent(null);
        a.setAiSuspicionPercent(null);
        a.setWebSources(null);
        a.setPasteCount(null);
        a.setTabSwitchCount(null);
        a.setLastIntegrityEventAt(null);
        a.setLastPlagiarismCheckAt(null);

        ajouter(a);
    }

    private Answer mapResultSetToAnswer(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp lastIntegrity = rs.getTimestamp("last_integrity_event_at");
        Timestamp lastPlagiarism = rs.getTimestamp("last_plagiarism_check_at");

        Integer studentId = rs.getObject("student_id") != null ? rs.getInt("student_id") : null;
        Integer webPlagiarismPercent = rs.getObject("web_plagiarism_percent") != null ? rs.getInt("web_plagiarism_percent") : null;
        Integer aiSuspicionPercent = rs.getObject("ai_suspicion_percent") != null ? rs.getInt("ai_suspicion_percent") : null;
        Integer pasteCount = rs.getObject("paste_count") != null ? rs.getInt("paste_count") : null;
        Integer tabSwitchCount = rs.getObject("tab_switch_count") != null ? rs.getInt("tab_switch_count") : null;

        return new Answer(
                rs.getInt("id"),
                rs.getString("content"),
                rs.getBoolean("is_correct"),
                rs.getString("role"),
                rs.getInt("question_id"),
                studentId,
                createdAt != null ? createdAt.toLocalDateTime() : null,
                webPlagiarismPercent,
                aiSuspicionPercent,
                rs.getString("web_sources"),
                pasteCount,
                tabSwitchCount,
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