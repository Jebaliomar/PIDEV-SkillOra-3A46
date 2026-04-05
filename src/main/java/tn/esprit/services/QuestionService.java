package tn.esprit.services;

import tn.esprit.entities.Question;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IQuestionService<Question> {

    private Connection cn;

    public QuestionService() {
        cn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Question q) throws SQLException {

        String sql = "INSERT INTO question (content, explanation, type, score, evaluation_id) VALUES (?, ?, ?, ?, ?)";

        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setString(1, q.getContent());
        ps.setString(2, q.getExplanation());
        ps.setString(3, q.getType());
        ps.setInt(4, q.getScore());
        ps.setInt(5, q.getEvaluationId());

        ps.executeUpdate();

        System.out.println("Question ajoutée avec succès.");
    }

    @Override
    public void modifier(Question q) throws SQLException {

        String sql = "UPDATE question SET content=?, explanation=?, type=?, score=?, evaluation_id=? WHERE id=?";

        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setString(1, q.getContent());
        ps.setString(2, q.getExplanation());
        ps.setString(3, q.getType());
        ps.setInt(4, q.getScore());
        ps.setInt(5, q.getEvaluationId());
        ps.setInt(6, q.getId());

        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("Question modifiée avec succès.");
        else
            System.out.println("Question non trouvée.");
    }

    @Override
    public void supprimer(Question q) throws SQLException {

        String sql = "DELETE FROM question WHERE id=?";

        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setInt(1, q.getId());

        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("Question supprimée avec succès.");
        else
            System.out.println("Question non trouvée.");
    }

    @Override
    public Question chercherParId(int id) throws SQLException {

        String sql = "SELECT * FROM question WHERE id=?";

        PreparedStatement ps = cn.prepareStatement(sql);

        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {

            return new Question(
                    rs.getInt("id"),
                    rs.getString("content"),
                    rs.getString("explanation"),
                    rs.getString("type"),
                    rs.getInt("score"),
                    rs.getInt("evaluation_id")
            );
        }

        return null;
    }

    @Override
    public List<Question> recuperer() throws SQLException {

        String sql = "SELECT * FROM question";

        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Question> questions = new ArrayList<>();

        while (rs.next()) {

            Question q = new Question(
                    rs.getInt("id"),
                    rs.getString("content"),
                    rs.getString("explanation"),
                    rs.getString("type"),
                    rs.getInt("score"),
                    rs.getInt("evaluation_id")
            );

            questions.add(q);
        }

        return questions;
    }
}