package tn.esprit.services;

import tn.esprit.entities.Answer;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnswerService implements IAnswerService<Answer> {

    private Connection cn;

    public AnswerService() {
        cn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Answer a) throws SQLException {

        String sql = "INSERT INTO answer " +
                "(content, is_correct, role, question_id, student_id, created_at, web_plagiarism_percent, ai_suspicion_percent, web_sources, paste_count, tab_switch_count, last_integrity_event_at, last_plagiarism_check_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setString(1, a.getContent());
        ps.setBoolean(2, a.getIsCorrect());
        ps.setString(3, a.getRole());
        ps.setInt(4, a.getQuestionId());
        ps.setInt(5, a.getStudentId());

        if (a.getCreatedAt() != null) {
            ps.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
        } else {
            ps.setTimestamp(6, null);
        }

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

    @Override
    public void modifier(Answer a) throws SQLException {

        String sql = "UPDATE answer SET " +
                "content=?, " +
                "is_correct=?, " +
                "role=?, " +
                "question_id=?, " +
                "student_id=?, " +
                "created_at=?, " +
                "web_plagiarism_percent=?, " +
                "ai_suspicion_percent=?, " +
                "web_sources=?, " +
                "paste_count=?, " +
                "tab_switch_count=?, " +
                "last_integrity_event_at=?, " +
                "last_plagiarism_check_at=? " +
                "WHERE id=?";

        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setString(1, a.getContent());
        ps.setBoolean(2, a.getIsCorrect());
        ps.setString(3, a.getRole());
        ps.setInt(4, a.getQuestionId());
        ps.setInt(5, a.getStudentId());

        if (a.getCreatedAt() != null)
            ps.setTimestamp(6, Timestamp.valueOf(a.getCreatedAt()));
        else
            ps.setTimestamp(6, null);

        ps.setInt(7, a.getWebPlagiarismPercent());
        ps.setInt(8, a.getAiSuspicionPercent());
        ps.setString(9, a.getWebSources());
        ps.setInt(10, a.getPasteCount());
        ps.setInt(11, a.getTabSwitchCount());

        if (a.getLastIntegrityEventAt() != null)
            ps.setTimestamp(12, Timestamp.valueOf(a.getLastIntegrityEventAt()));
        else
            ps.setTimestamp(12, null);

        if (a.getLastPlagiarismCheckAt() != null)
            ps.setTimestamp(13, Timestamp.valueOf(a.getLastPlagiarismCheckAt()));
        else
            ps.setTimestamp(13, null);

        ps.setInt(14, a.getId());

        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("Answer modifiée avec succès.");
        else
            System.out.println("Answer non trouvée.");
    }

    @Override
    public void supprimer(Answer a) throws SQLException {

        String sql = "DELETE FROM answer WHERE id=?";

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setInt(1, a.getId());

        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("Answer supprimée avec succès.");
        else
            System.out.println("Answer non trouvée.");
    }

    @Override
    public Answer chercherParId(int id) throws SQLException {

        String sql = "SELECT * FROM answer WHERE id=?";

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {

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

        return null;
    }

    @Override
    public List<Answer> recuperer() throws SQLException {

        String sql = "SELECT * FROM answer";

        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Answer> answers = new ArrayList<>();

        while (rs.next()) {

            Timestamp createdAt = rs.getTimestamp("created_at");
            Timestamp lastIntegrity = rs.getTimestamp("last_integrity_event_at");
            Timestamp lastPlagiarism = rs.getTimestamp("last_plagiarism_check_at");

            Answer a = new Answer(
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

            answers.add(a);
        }

        return answers;
    }
}