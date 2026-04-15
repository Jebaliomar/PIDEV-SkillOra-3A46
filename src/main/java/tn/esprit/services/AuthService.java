package tn.esprit.services;

import org.mindrot.jbcrypt.BCrypt;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;

public class AuthService {

    private final Connection connection;

    public AuthService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public Optional<AuthResult> authenticate(String identifier, String rawPassword) throws SQLException {
        String login = toNull(identifier);
        String password = toNull(rawPassword);
        if (login == null || password == null) {
            return Optional.empty();
        }

        String userTable = findExistingTable("user", "users");
        if (userTable == null) {
            return Optional.empty();
        }

        String usernameColumn = findExistingColumn(userTable, "username", "user_name", "login");
        String emailColumn = findExistingColumn(userTable, "email", "mail");
        String passwordColumn = findExistingColumn(userTable, "password", "hashed_password");
        String activeColumn = findExistingColumn(userTable, "is_active", "active", "enabled");
        String firstNameColumn = findExistingColumn(userTable, "first_name", "firstname");
        String lastNameColumn = findExistingColumn(userTable, "last_name", "lastname");
        if (passwordColumn == null || (usernameColumn == null && emailColumn == null)) {
            return Optional.empty();
        }

        StringBuilder sql = new StringBuilder("SELECT `id`, `").append(passwordColumn).append("` AS pwd");
        if (activeColumn != null) {
            sql.append(", `").append(activeColumn).append("` AS is_active");
        }
        if (firstNameColumn != null) {
            sql.append(", `").append(firstNameColumn).append("` AS first_name");
        }
        if (lastNameColumn != null) {
            sql.append(", `").append(lastNameColumn).append("` AS last_name");
        }
        if (usernameColumn != null) {
            sql.append(", `").append(usernameColumn).append("` AS username");
        }
        if (emailColumn != null) {
            sql.append(", `").append(emailColumn).append("` AS email");
        }
        sql.append(" FROM `").append(userTable).append("` WHERE ");

        if (usernameColumn != null && emailColumn != null) {
            sql.append("LOWER(`").append(usernameColumn).append("`) = LOWER(?) OR LOWER(`")
                    .append(emailColumn).append("`) = LOWER(?)");
        } else if (usernameColumn != null) {
            sql.append("LOWER(`").append(usernameColumn).append("`) = LOWER(?)");
        } else {
            sql.append("LOWER(`").append(emailColumn).append("`) = LOWER(?)");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, login);
            if (usernameColumn != null && emailColumn != null) {
                statement.setString(2, login);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                String storedPassword = resultSet.getString("pwd");
                if (!passwordMatches(password, storedPassword)) {
                    return Optional.empty();
                }

                if (activeColumn != null) {
                    int activeRaw = resultSet.getInt("is_active");
                    if (!resultSet.wasNull() && activeRaw == 0) {
                        return Optional.empty();
                    }
                }

                int userId = resultSet.getInt("id");
                String displayName = buildDisplayName(resultSet);
                String role = resolveRoleForUser(userId, userTable);
                return Optional.of(new AuthResult(userId, role, displayName));
            }
        }
    }

    private String buildDisplayName(ResultSet resultSet) throws SQLException {
        String firstName = toNull(safeGet(resultSet, "first_name"));
        String lastName = toNull(safeGet(resultSet, "last_name"));
        String username = toNull(safeGet(resultSet, "username"));
        String email = toNull(safeGet(resultSet, "email"));

        String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        if (!fullName.isEmpty()) {
            return fullName;
        }
        if (username != null) {
            return username;
        }
        return email == null ? "User" : email;
    }

    private String safeGet(ResultSet resultSet, String column) {
        try {
            return resultSet.getString(column);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        String hash = storedPassword.trim();
        if (isBcryptHash(hash)) {
            String normalizedHash = normalizeBcryptPrefix(hash);
            try {
                return BCrypt.checkpw(rawPassword, normalizedHash);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }
        return rawPassword.equals(hash);
    }

    private boolean isBcryptHash(String hash) {
        return hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$");
    }

    private String normalizeBcryptPrefix(String hash) {
        if (hash.startsWith("$2y$")) {
            return "$2a$" + hash.substring(4);
        }
        return hash;
    }

    private String resolveRoleForUser(int userId, String userTable) throws SQLException {
        String role = resolveRoleFromUserRoleTable(userId);
        if (role != null) {
            return normalizeRole(role);
        }

        String rolesColumn = findExistingColumn(userTable, "roles", "role");
        if (rolesColumn != null) {
            String sql = "SELECT `" + rolesColumn + "` FROM `" + userTable + "` WHERE `id` = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String raw = resultSet.getString(1);
                        if (toNull(raw) != null) {
                            return normalizeRole(raw);
                        }
                    }
                }
            }
        }

        return "student";
    }

    private String resolveRoleFromUserRoleTable(int userId) throws SQLException {
        String roleTable = findExistingTable("user_role", "user_roles", "users_roles");
        if (roleTable == null) {
            return null;
        }

        String userIdColumn = findExistingColumn(roleTable, "user_id", "userid", "id_user");
        String roleColumn = findExistingColumn(roleTable, "role", "name");
        if (userIdColumn == null || roleColumn == null) {
            return null;
        }

        String createdAtColumn = findExistingColumn(roleTable, "created_at", "createdat");
        StringBuilder sql = new StringBuilder("SELECT `").append(roleColumn).append("` FROM `")
                .append(roleTable).append("` WHERE `").append(userIdColumn).append("` = ?");
        if (createdAtColumn != null) {
            sql.append(" ORDER BY `").append(createdAtColumn).append("` DESC");
        }
        sql.append(" LIMIT 1");

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setInt(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private String normalizeRole(String rawRole) {
        String value = rawRole == null ? "" : rawRole.toLowerCase(Locale.ROOT);
        if (value.contains("admin")) {
            return "admin";
        }
        if (value.contains("prof") || value.contains("teacher") || value.contains("instructor")) {
            return "professor";
        }
        return "student";
    }

    private String findExistingTable(String... candidates) throws SQLException {
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (tableExists(catalog, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tableExists(String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private String findExistingColumn(String tableName, String... candidates) throws SQLException {
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (columnExists(catalog, tableName, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean columnExists(String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName, columnName)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                catalog,
                null,
                tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return resultSet.next();
        }
    }

    private String toNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record AuthResult(int userId, String role, String displayName) {
    }
}
