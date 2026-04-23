package tn.esprit.services;

import tn.esprit.entities.LessonCompletion;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LessonCompletionService {

    private final Connection connection;

    public LessonCompletionService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(LessonCompletion lessonCompletion) throws SQLException {
        String sql = "INSERT INTO `lesson_completion` (`completed_at`, `enrollment_id`, `lesson_id`) VALUES (?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setTimestamp(preparedStatement, 1, lessonCompletion.getCompletedAt());
            setInteger(preparedStatement, 2, lessonCompletion.getEnrollmentId());
            setInteger(preparedStatement, 3, lessonCompletion.getLessonId());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    lessonCompletion.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<LessonCompletion> getAll() throws SQLException {
        String sql = "SELECT * FROM `lesson_completion`";
        List<LessonCompletion> lessonCompletions = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                lessonCompletions.add(mapResultSetToLessonCompletion(resultSet));
            }
        }

        return lessonCompletions;
    }

    public LessonCompletion getById(int id) throws SQLException {
        String sql = "SELECT * FROM `lesson_completion` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToLessonCompletion(resultSet);
                }
            }
        }

        return null;
    }

    public List<LessonCompletion> getByEnrollmentId(int enrollmentId) throws SQLException {
        String sql = "SELECT * FROM `lesson_completion` WHERE `enrollment_id` = ?";
        List<LessonCompletion> lessonCompletions = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, enrollmentId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lessonCompletions.add(mapResultSetToLessonCompletion(resultSet));
                }
            }
        }

        return lessonCompletions;
    }

    public LessonCompletion getByEnrollmentAndLesson(int enrollmentId, int lessonId) throws SQLException {
        String sql = "SELECT * FROM `lesson_completion` WHERE `enrollment_id` = ? AND `lesson_id` = ? ORDER BY `id` DESC LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, enrollmentId);
            preparedStatement.setInt(2, lessonId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToLessonCompletion(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(LessonCompletion lessonCompletion) throws SQLException {
        String sql = "UPDATE `lesson_completion` SET `completed_at` = ?, `enrollment_id` = ?, `lesson_id` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            setTimestamp(preparedStatement, 1, lessonCompletion.getCompletedAt());
            setInteger(preparedStatement, 2, lessonCompletion.getEnrollmentId());
            setInteger(preparedStatement, 3, lessonCompletion.getLessonId());
            preparedStatement.setInt(4, lessonCompletion.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM `lesson_completion` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private LessonCompletion mapResultSetToLessonCompletion(ResultSet resultSet) throws SQLException {
        LessonCompletion lessonCompletion = new LessonCompletion();
        lessonCompletion.setId(resultSet.getInt("id"));
        lessonCompletion.setCompletedAt(getTimestamp(resultSet, "completed_at"));
        lessonCompletion.setEnrollmentId(getInteger(resultSet, "enrollment_id"));
        lessonCompletion.setLessonId(getInteger(resultSet, "lesson_id"));
        return lessonCompletion;
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
