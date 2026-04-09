package tn.esprit.services;

import tn.esprit.entities.Evaluation;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService implements IEvaluationService<Evaluation> {

    private final Connection cn;

    public EvaluationService() {
        cn = MyConnection.getInstance().getConnection();
    }

    @Override
    public void ajouter(Evaluation e) throws SQLException {
        if (!validerEvaluation(e)) {
            throw new IllegalArgumentException("Les données de l'évaluation sont invalides.");
        }

        if (e.getCreatedAt() == null) {
            e.setCreatedAt(LocalDateTime.now());
        }

        String sql = "INSERT INTO evaluation " +
                "(title, description, type, duration, total_score, created_at, docx_path, pdf_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());
            ps.setInt(4, e.getDuration());
            ps.setInt(5, e.getTotalScore());
            ps.setTimestamp(6, Timestamp.valueOf(e.getCreatedAt()));
            ps.setString(7, e.getDocxPath());
            ps.setString(8, e.getPdfPath());

            ps.executeUpdate();
            System.out.println("Evaluation ajoutée avec succès.");
        }
    }

    @Override
    public void modifier(Evaluation e) throws SQLException {
        if (e.getId() == null) {
            throw new IllegalArgumentException("L'id de l'évaluation est obligatoire pour la modification.");
        }

        if (!validerEvaluation(e)) {
            throw new IllegalArgumentException("Les données de l'évaluation sont invalides.");
        }

        if (e.getCreatedAt() == null) {
            e.setCreatedAt(LocalDateTime.now());
        }

        String sql = "UPDATE evaluation SET " +
                "title = ?, description = ?, type = ?, duration = ?, total_score = ?, " +
                "created_at = ?, docx_path = ?, pdf_path = ? " +
                "WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getType());
            ps.setInt(4, e.getDuration());
            ps.setInt(5, e.getTotalScore());
            ps.setTimestamp(6, Timestamp.valueOf(e.getCreatedAt()));
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
    }

    @Override
    public void supprimer(Evaluation e) throws SQLException {
        if (e == null || e.getId() == null) {
            throw new IllegalArgumentException("L'id est obligatoire pour supprimer une évaluation.");
        }

        String sql = "DELETE FROM evaluation WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, e.getId());

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Evaluation supprimée avec succès.");
            } else {
                System.out.println("Aucune évaluation trouvée avec cet ID.");
            }
        }
    }

    @Override
    public Evaluation chercherParId(int id) throws SQLException {
        String sql = "SELECT * FROM evaluation WHERE id = ?";

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvaluation(rs);
                }
            }
        }

        return null;
    }

    @Override
    public List<Evaluation> recuperer() throws SQLException {
        String sql = "SELECT * FROM evaluation";
        return executeAndMapList(sql);
    }

    // =========================
    // TRI
    // =========================

    @Override
    public List<Evaluation> trierParTitreAsc() throws SQLException {
        String sql = "SELECT * FROM evaluation ORDER BY title ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Evaluation> trierParTitreDesc() throws SQLException {
        String sql = "SELECT * FROM evaluation ORDER BY title DESC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Evaluation> trierParScoreAsc() throws SQLException {
        String sql = "SELECT * FROM evaluation ORDER BY total_score ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Evaluation> trierParScoreDesc() throws SQLException {
        String sql = "SELECT * FROM evaluation ORDER BY total_score DESC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Evaluation> trierParDateAsc() throws SQLException {
        String sql = "SELECT * FROM evaluation ORDER BY created_at ASC";
        return executeAndMapList(sql);
    }

    @Override
    public List<Evaluation> trierParDateDesc() throws SQLException {
        String sql = "SELECT * FROM evaluation ORDER BY created_at DESC";
        return executeAndMapList(sql);
    }

    // =========================
    // RECHERCHE / FILTRE
    // =========================

    @Override
    public List<Evaluation> rechercherParMotCle(String motCle) throws SQLException {
        String sql = "SELECT * FROM evaluation " +
                "WHERE LOWER(title) LIKE LOWER(?) " +
                "OR LOWER(description) LIKE LOWER(?) " +
                "OR LOWER(type) LIKE LOWER(?)";

        List<Evaluation> evaluations = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            String valeur = "%" + motCle + "%";
            ps.setString(1, valeur);
            ps.setString(2, valeur);
            ps.setString(3, valeur);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(mapResultSetToEvaluation(rs));
                }
            }
        }

        return evaluations;
    }

    @Override
    public List<Evaluation> filtrerParType(String type) throws SQLException {
        String sql = "SELECT * FROM evaluation WHERE LOWER(type) = LOWER(?)";

        List<Evaluation> evaluations = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, type);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(mapResultSetToEvaluation(rs));
                }
            }
        }

        return evaluations;
    }

    // =========================
    // PAGINATION
    // =========================

    @Override
    public List<Evaluation> recupererParPage(int page, int taillePage) throws SQLException {
        if (page <= 0) {
            throw new IllegalArgumentException("Le numéro de page doit être supérieur à 0.");
        }

        if (taillePage <= 0) {
            throw new IllegalArgumentException("La taille de page doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM evaluation ORDER BY id ASC LIMIT ? OFFSET ?";
        List<Evaluation> evaluations = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, taillePage);
            ps.setInt(2, (page - 1) * taillePage);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(mapResultSetToEvaluation(rs));
                }
            }
        }

        return evaluations;
    }

    // =========================
    // STATISTIQUES
    // =========================

    @Override
    public int countEvaluations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM evaluation";

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
        String sql = "SELECT AVG(total_score) FROM evaluation";

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble(1);
            }
        }

        return 0;
    }

    // =========================
    // RECENTES
    // =========================

    @Override
    public List<Evaluation> recupererRecentes(int limite) throws SQLException {
        if (limite <= 0) {
            throw new IllegalArgumentException("La limite doit être supérieure à 0.");
        }

        String sql = "SELECT * FROM evaluation ORDER BY created_at DESC LIMIT ?";
        List<Evaluation> evaluations = new ArrayList<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    evaluations.add(mapResultSetToEvaluation(rs));
                }
            }
        }

        return evaluations;
    }

    // =========================
    // VALIDATION
    // =========================

    @Override
    public boolean validerEvaluation(Evaluation e) {
        if (e == null) {
            return false;
        }

        if (e.getTitle() == null || e.getTitle().trim().isEmpty()) {
            return false;
        }

        if (e.getDescription() == null || e.getDescription().trim().isEmpty()) {
            return false;
        }

        if (e.getType() == null || e.getType().trim().isEmpty()) {
            return false;
        }

        if (e.getDuration() == null || e.getDuration() <= 0) {
            return false;
        }

        if (e.getTotalScore() == null || e.getTotalScore() < 0) {
            return false;
        }

        return true;
    }

    // =========================
    // METHODES UTILITAIRES
    // =========================

    private Evaluation mapResultSetToEvaluation(ResultSet rs) throws SQLException {
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

    private List<Evaluation> executeAndMapList(String sql) throws SQLException {
        List<Evaluation> evaluations = new ArrayList<>();

        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                evaluations.add(mapResultSetToEvaluation(rs));
            }
        }

        return evaluations;
    }
}