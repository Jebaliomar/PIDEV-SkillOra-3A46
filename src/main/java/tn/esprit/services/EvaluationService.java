package tn.esprit.services;

import tn.esprit.entities.Evaluation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService implements IEvaluationService<Evaluation> {

    private Connection cn;

    public EvaluationService() {
        cn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Evaluation e) throws SQLException {
        String sql = "INSERT INTO evaluation " +
                "(title, description, type, duration, total_score, created_at, docx_path, pdf_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setString(1, e.getTitle());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getType());
        ps.setInt(4, e.getDuration());
        ps.setInt(5, e.getTotalScore());

        if (e.getCreatedAt() != null) {
            ps.setTimestamp(6, Timestamp.valueOf(e.getCreatedAt()));
        } else {
            ps.setTimestamp(6, null);
        }

        ps.setString(7, e.getDocxPath());
        ps.setString(8, e.getPdfPath());

        ps.executeUpdate();
        System.out.println("Evaluation ajoutée avec succès.");
    }

    @Override
    public void modifier(Evaluation e) throws SQLException {
        String sql = "UPDATE evaluation SET " +
                "title = ?, " +
                "description = ?, " +
                "type = ?, " +
                "duration = ?, " +
                "total_score = ?, " +
                "created_at = ?, " +
                "docx_path = ?, " +
                "pdf_path = ? " +
                "WHERE id = ?";

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setString(1, e.getTitle());
        ps.setString(2, e.getDescription());
        ps.setString(3, e.getType());
        ps.setInt(4, e.getDuration());
        ps.setInt(5, e.getTotalScore());

        if (e.getCreatedAt() != null) {
            ps.setTimestamp(6, Timestamp.valueOf(e.getCreatedAt()));
        } else {
            ps.setTimestamp(6, null);
        }

        ps.setString(7, e.getDocxPath());
        ps.setString(8, e.getPdfPath());
        ps.setInt(9, e.getId());

        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("Evaluation modifiée avec succès.");
        } else {
            System.out.println("Aucune évaluation trouvée avec cet ID.");
        }
    }

    @Override
    public void supprimer(Evaluation e) throws SQLException {
        String sql = "DELETE FROM evaluation WHERE id = ?";

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setInt(1, e.getId());

        int rows = ps.executeUpdate();

        if (rows > 0) {
            System.out.println("Evaluation supprimée avec succès.");
        } else {
            System.out.println("Aucune évaluation trouvée avec cet ID.");
        }
    }

    @Override
    public Evaluation chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM evaluation WHERE id = ?";

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setInt(1, id);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");

            return new Evaluation(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getInt("duration"),
                    rs.getInt("total_score"),
                    createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null,
                    rs.getString("docx_path"),
                    rs.getString("pdf_path")
            );
        }

        return null;
    }

    @Override
    public List<Evaluation> recuperer() throws SQLException {
        String sql = "SELECT * FROM evaluation";
        Statement st = cn.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Evaluation> evaluations = new ArrayList<>();

        while (rs.next()) {
            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");

            Evaluation e = new Evaluation(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getInt("duration"),
                    rs.getInt("total_score"),
                    createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null,
                    rs.getString("docx_path"),
                    rs.getString("pdf_path")
            );

            evaluations.add(e);
        }

        return evaluations;
    }
}