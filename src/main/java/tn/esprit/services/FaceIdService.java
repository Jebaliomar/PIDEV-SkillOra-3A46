package tn.esprit.services;

import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class FaceIdService {

    private static final int DESCRIPTOR_LEN = 128;
    public static final double MATCH_THRESHOLD = 0.55;

    private final Connection connection;

    public FaceIdService() {
        this.connection = MyConnection.getInstance().getConnection();
        ensureTable();
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_face_descriptors (" +
                     "user_id INT PRIMARY KEY," +
                     "descriptor TEXT NOT NULL," +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveDescriptor(int userId, double[] descriptor) throws SQLException {
        String encoded = encode(descriptor);
        String sql = "INSERT INTO user_face_descriptors (user_id, descriptor) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE descriptor = VALUES(descriptor), created_at = CURRENT_TIMESTAMP";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, encoded);
            ps.executeUpdate();
        }
        setFaceIdEnabled(userId, true);
    }

    public double[] getDescriptor(int userId) throws SQLException {
        String sql = "SELECT descriptor FROM user_face_descriptors WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return decode(rs.getString("descriptor"));
            }
        }
        return null;
    }

    public void setFaceIdEnabled(int userId, boolean enabled) throws SQLException {
        String sql = "UPDATE users SET face_id_enabled = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public static double distance(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) return Double.MAX_VALUE;
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    public static boolean matches(double[] a, double[] b) {
        return distance(a, b) < MATCH_THRESHOLD;
    }

    public static String encode(double[] d) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < d.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(d[i]);
        }
        return sb.toString();
    }

    public static double[] decode(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] parts = s.split(",");
        double[] out = new double[parts.length];
        for (int i = 0; i < parts.length; i++) out[i] = Double.parseDouble(parts[i].trim());
        return out;
    }

    public static boolean isValidDescriptor(double[] d) {
        return d != null && d.length == DESCRIPTOR_LEN;
    }
}
