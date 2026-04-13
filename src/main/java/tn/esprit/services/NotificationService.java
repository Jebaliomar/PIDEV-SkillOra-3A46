package tn.esprit.services;

import tn.esprit.entities.Notification;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {

    private final Connection connection;

    public NotificationService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Notification notification) throws SQLException {
        String sql = "INSERT INTO notification (user_id, title, message, link, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, notification.getUserId());
            preparedStatement.setString(2, notification.getTitle());
            preparedStatement.setString(3, notification.getMessage());
            preparedStatement.setString(4, notification.getLink());
            preparedStatement.setBoolean(5, Boolean.TRUE.equals(notification.getIsRead()));
            if (notification.getCreatedAt() == null) {
                preparedStatement.setTimestamp(6, Timestamp.valueOf(java.time.LocalDateTime.now()));
            } else {
                preparedStatement.setTimestamp(6, Timestamp.valueOf(notification.getCreatedAt()));
            }
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Notification> findUnreadByUser(int userId, int limit) throws SQLException {
        String sql = "SELECT * FROM notification WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC LIMIT ?";
        List<Notification> notifications = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            preparedStatement.setInt(2, limit);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    notifications.add(mapResultSetToNotification(resultSet));
                }
            }
        }
        return notifications;
    }

    public int countUnreadByUser(int userId) throws SQLException {
        String sql = "SELECT COUNT(id) FROM notification WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return 0;
    }

    public int markAllAsReadForUser(int userId) throws SQLException {
        String sql = "UPDATE notification SET is_read = 1 WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);
            return preparedStatement.executeUpdate();
        }
    }

    private Notification mapResultSetToNotification(ResultSet resultSet) throws SQLException {
        Notification notification = new Notification();
        notification.setId(resultSet.getInt("id"));
        notification.setUserId(resultSet.getInt("user_id"));
        notification.setTitle(resultSet.getString("title"));
        notification.setMessage(resultSet.getString("message"));
        notification.setLink(resultSet.getString("link"));
        notification.setIsRead(resultSet.getBoolean("is_read"));
        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            notification.setCreatedAt(createdAt.toLocalDateTime());
        }
        return notification;
    }
}
