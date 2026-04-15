package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    private final Connection connection;

    public UserService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public String getUsernameById(Integer userId) throws SQLException {
        if (userId == null) {
            return "Unknown user";
        }

        String sql = "SELECT username FROM `user` WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String username = resultSet.getString("username");
                    if (username != null && !username.isBlank()) {
                        return username;
                    }
                }
            }
        }

        return "User #" + userId;
    }

    public User getById(Integer userId) throws SQLException {
        if (userId == null) {
            return null;
        }

        String sql = "SELECT id, username FROM `user` WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }

    public User getFirstUser() throws SQLException {
        String sql = "SELECT id, username FROM `user` ORDER BY id ASC LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            if (resultSet.next()) {
                return mapUser(resultSet);
            }
        }

        return null;
    }

    public boolean hasRole(Integer userId, String... roleNames) throws SQLException {
        if (userId == null || roleNames == null || roleNames.length == 0) {
            return false;
        }

        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM user_role WHERE user_id = ? AND UPPER(role) IN (");
        for (int index = 0; index < roleNames.length; index++) {
            if (index > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            preparedStatement.setInt(1, userId);
            for (int index = 0; index < roleNames.length; index++) {
                preparedStatement.setString(index + 2, roleNames[index] == null ? "" : roleNames[index].trim().toUpperCase());
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private User mapUser(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setUsername(resultSet.getString("username"));
        return user;
    }
}
