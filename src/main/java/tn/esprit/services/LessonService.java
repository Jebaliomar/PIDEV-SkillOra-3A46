package tn.esprit.services;

import tn.esprit.entities.Lesson;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LessonService {

    private final Connection connection;

    public LessonService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Lesson lesson) throws SQLException {
        String sql = "INSERT INTO `lesson` (`title`, `type`, `content`, `file_path`, `position`, `created_at`, `updated_at`, `section_id`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, lesson.getTitle());
            preparedStatement.setString(2, lesson.getType());
            preparedStatement.setString(3, lesson.getContent());
            preparedStatement.setString(4, lesson.getFilePath());
            setInteger(preparedStatement, 5, lesson.getPosition());
            setTimestamp(preparedStatement, 6, lesson.getCreatedAt());
            setTimestamp(preparedStatement, 7, lesson.getUpdatedAt());
            setInteger(preparedStatement, 8, lesson.getSectionId());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    lesson.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Lesson> getAll() throws SQLException {
        String sql = "SELECT * FROM `lesson`";
        List<Lesson> lessons = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                lessons.add(mapResultSetToLesson(resultSet));
            }
        }

        return lessons;
    }

    public Lesson getById(int id) throws SQLException {
        String sql = "SELECT * FROM `lesson` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToLesson(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(Lesson lesson) throws SQLException {
        String sql = "UPDATE `lesson` SET `title` = ?, `type` = ?, `content` = ?, `file_path` = ?, `position` = ?, "
                + "`created_at` = ?, `updated_at` = ?, `section_id` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, lesson.getTitle());
            preparedStatement.setString(2, lesson.getType());
            preparedStatement.setString(3, lesson.getContent());
            preparedStatement.setString(4, lesson.getFilePath());
            setInteger(preparedStatement, 5, lesson.getPosition());
            setTimestamp(preparedStatement, 6, lesson.getCreatedAt());
            setTimestamp(preparedStatement, 7, lesson.getUpdatedAt());
            setInteger(preparedStatement, 8, lesson.getSectionId());
            preparedStatement.setInt(9, lesson.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM `lesson` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Lesson mapResultSetToLesson(ResultSet resultSet) throws SQLException {
        Lesson lesson = new Lesson();
        lesson.setId(resultSet.getInt("id"));
        lesson.setTitle(resultSet.getString("title"));
        lesson.setType(resultSet.getString("type"));
        lesson.setContent(resultSet.getString("content"));
        lesson.setFilePath(resultSet.getString("file_path"));
        lesson.setPosition(getInteger(resultSet, "position"));
        lesson.setCreatedAt(getTimestamp(resultSet, "created_at"));
        lesson.setUpdatedAt(getTimestamp(resultSet, "updated_at"));
        lesson.setSectionId(getInteger(resultSet, "section_id"));
        return lesson;
    }

    private void setInteger(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private Integer getInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private void setTimestamp(PreparedStatement preparedStatement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private java.time.LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
