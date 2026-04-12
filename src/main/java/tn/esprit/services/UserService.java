package tn.esprit.services;

import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.entities.User;
import tn.esprit.entities.UserRole;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UserService {

    private final Connection connection;

    public UserService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    // ==================== CREATE ====================

    public void register(User user, String role) throws SQLException {
        // Generate unique username from first + last name
        String baseUsername = (user.getFirstName() + user.getLastName()).toLowerCase().replaceAll("[^a-z0-9]", "");
        String username = baseUsername + new Random().nextInt(9999);

        // Hash password with BCrypt (cost 12, compatible with Symfony website)
        // Generate $2a$ hash then convert to $2y$ for PHP/Symfony compatibility
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12))
                .replaceFirst("^\\$2a\\$", "\\$2y\\$");

        String sql = "INSERT INTO users (email, username, password, first_name, last_name, is_active, is_verified, created_at, profile_completed, face_id_enabled) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, user.getEmail());
        ps.setString(2, username);
        ps.setString(3, hashedPassword);
        ps.setString(4, user.getFirstName());
        ps.setString(5, user.getLastName());
        ps.setInt(6, 1);     // is_active = true
        ps.setInt(7, 1);     // is_verified = true (desktop app skips email verification)
        ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
        ps.setInt(9, 0);     // profile_completed = false
        ps.setBoolean(10, false);
        ps.executeUpdate();

        // Get the generated user ID
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            int userId = rs.getInt(1);
            // Insert role into user_roles table
            String roleSql = "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, ?, ?)";
            PreparedStatement rolePs = connection.prepareStatement(roleSql);
            rolePs.setInt(1, userId);
            rolePs.setString(2, role.toLowerCase());
            rolePs.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            rolePs.executeUpdate();
        }
    }

    // ==================== READ ====================

    public User authenticate(String email, String rawPassword) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            String storedHash = rs.getString("password");

            // Convert PHP's $2y$ to Java's $2a$ for BCrypt compatibility (same algorithm)
            String javaCompatibleHash = storedHash.replaceFirst("^\\$2y\\$", "\\$2a\\$");

            // Verify password with BCrypt
            if (BCrypt.checkpw(rawPassword, javaCompatibleHash)) {
                User user = mapResultSetToUser(rs);

                // Check if user is active and verified
                if (user.getIsActive() == 0) {
                    throw new SQLException("Your account has been deactivated.");
                }
                if (user.getIsVerified() == 0) {
                    throw new SQLException("Please verify your email before logging in.");
                }

                // Update last login time
                updateLastLogin(user.getId());
                return user;
            }
        }
        return null; // Invalid credentials
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSetToUser(rs);
        }
        return null;
    }

    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return mapResultSetToUser(rs);
        }
        return null;
    }

    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        return users;
    }

    public String getUserRole(int userId) throws SQLException {
        String sql = "SELECT role FROM user_roles WHERE user_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("role");
        }
        return "ROLE_STUDENT";
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1) > 0;
    }

    // ==================== UPDATE ====================

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, phone = ?, gender = ?, " +
                     "bio = ?, date_of_birth = ?, field_of_study = ?, university = ?, country = ? " +
                     "WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, user.getFirstName());
        ps.setString(2, user.getLastName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getGender());
        ps.setString(6, user.getBio());
        ps.setDate(7, user.getDateOfBirth() != null ? java.sql.Date.valueOf(user.getDateOfBirth()) : null);
        ps.setString(8, user.getFieldOfStudy());
        ps.setString(9, user.getUniversity());
        ps.setString(10, user.getCountry());
        ps.setInt(11, user.getId());
        ps.executeUpdate();
    }

    public void updatePassword(int userId, String newPassword) throws SQLException {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12))
                .replaceFirst("^\\$2a\\$", "\\$2y\\$");
        String sql = "UPDATE users SET password = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, hashedPassword);
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    private void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users SET last_login_at = ? WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    // ==================== DELETE ====================

    public void delete(int userId) throws SQLException {
        // Delete user role first (foreign key)
        String deleteRole = "DELETE FROM user_roles WHERE user_id = ?";
        PreparedStatement rolePs = connection.prepareStatement(deleteRole);
        rolePs.setInt(1, userId);
        rolePs.executeUpdate();

        // Delete user
        String deleteUser = "DELETE FROM users WHERE id = ?";
        PreparedStatement userPs = connection.prepareStatement(deleteUser);
        userPs.setInt(1, userId);
        userPs.executeUpdate();
    }

    // ==================== STATS ====================

    public Map<String, Integer> getUserStats() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();

        // Total users
        PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM users");
        ResultSet rs = ps.executeQuery();
        rs.next();
        int total = rs.getInt(1);
        stats.put("total", total);

        // Banned users (is_active = 0)
        ps = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE is_active = 0");
        rs = ps.executeQuery();
        rs.next();
        int banned = rs.getInt(1);
        stats.put("banned", banned);

        // Active users
        stats.put("active", total - banned);

        // New this week
        ps = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)");
        rs = ps.executeQuery();
        rs.next();
        stats.put("new_this_week", rs.getInt(1));

        return stats;
    }

    public Map<String, Integer> getUserDistribution() throws SQLException {
        Map<String, Integer> distribution = new LinkedHashMap<>();
        String sql = "SELECT ur.role, COUNT(*) as cnt FROM user_roles ur GROUP BY ur.role ORDER BY cnt DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            String role = rs.getString("role");
            distribution.put(role.substring(0, 1).toUpperCase() + role.substring(1), rs.getInt("cnt"));
        }
        return distribution;
    }

    public List<Map<String, Object>> getUserGrowth(int days) throws SQLException {
        List<Map<String, Object>> growth = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String sql = "SELECT COUNT(*) FROM users WHERE DATE(created_at) = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setDate(1, java.sql.Date.valueOf(date));
            ResultSet rs = ps.executeQuery();
            rs.next();

            Map<String, Object> day = new HashMap<>();
            day.put("date", date.format(fmt));
            day.put("count", rs.getInt(1));
            growth.add(day);
        }
        return growth;
    }

    public List<Map<String, Object>> getAllWithRoles() throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT u.*, COALESCE(ur.role, 'student') as user_role FROM users u " +
                     "LEFT JOIN user_roles ur ON u.id = ur.user_id ORDER BY u.created_at DESC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("user", mapResultSetToUser(rs));
            row.put("role", rs.getString("user_role"));
            result.add(row);
        }
        return result;
    }

    // ==================== USER MANAGEMENT ====================

    public void banUser(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = 0 WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    public void unbanUser(int userId) throws SQLException {
        String sql = "UPDATE users SET is_active = 1 WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, userId);
        ps.executeUpdate();
    }

    public void changeRole(int userId, String newRole) throws SQLException {
        // Check if user already has a role
        String checkSql = "SELECT COUNT(*) FROM user_roles WHERE user_id = ?";
        PreparedStatement checkPs = connection.prepareStatement(checkSql);
        checkPs.setInt(1, userId);
        ResultSet rs = checkPs.executeQuery();
        rs.next();

        if (rs.getInt(1) > 0) {
            String sql = "UPDATE user_roles SET role = ?, created_at = NOW() WHERE user_id = ?";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, newRole.toLowerCase());
            ps.setInt(2, userId);
            ps.executeUpdate();
        } else {
            String sql = "INSERT INTO user_roles (user_id, role, created_at) VALUES (?, ?, NOW())";
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, newRole.toLowerCase());
            ps.executeUpdate();
        }
    }

    // ==================== HELPER ====================

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setIsActive(rs.getInt("is_active"));
        user.setIsVerified(rs.getInt("is_verified"));
        user.setPhone(rs.getString("phone"));
        user.setGender(rs.getString("gender"));
        user.setBio(rs.getString("bio"));
        user.setAvatar(rs.getString("avatar"));
        user.setAvatarType(rs.getString("avatar_type"));
        user.setFaceIdEnabled(rs.getBoolean("face_id_enabled"));
        user.setProfileCompleted(rs.getInt("profile_completed"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) user.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        if (lastLogin != null) user.setLastLoginAt(lastLogin.toLocalDateTime());

        java.sql.Date dob = rs.getDate("date_of_birth");
        if (dob != null) user.setDateOfBirth(dob.toLocalDate());

        user.setFieldOfStudy(rs.getString("field_of_study"));
        user.setUniversity(rs.getString("university"));
        user.setCountry(rs.getString("country"));

        return user;
    }
}
