package tn.esprit.services;

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
}
