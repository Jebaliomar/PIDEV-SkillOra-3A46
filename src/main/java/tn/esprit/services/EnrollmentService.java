package tn.esprit.services;

import tn.esprit.entities.Enrollment;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentService {

    private final Connection connection;

    public EnrollmentService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Enrollment enrollment) throws SQLException {
        String sql = "INSERT INTO `enrollment` (`enrolled_at`, `completed_at`, `progress_percent`, `status`, `user_id`, `course_id`) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setTimestamp(preparedStatement, 1, enrollment.getEnrolledAt());
            setTimestamp(preparedStatement, 2, enrollment.getCompletedAt());
            setShort(preparedStatement, 3, enrollment.getProgressPercent());
            preparedStatement.setString(4, enrollment.getStatus());
            setInteger(preparedStatement, 5, enrollment.getUserId());
            setInteger(preparedStatement, 6, enrollment.getCourseId());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    enrollment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Enrollment> getAll() throws SQLException {
        String sql = "SELECT * FROM `enrollment`";
        List<Enrollment> enrollments = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                enrollments.add(mapResultSetToEnrollment(resultSet));
            }
        }

        return enrollments;
    }

    public Enrollment getById(int id) throws SQLException {
        String sql = "SELECT * FROM `enrollment` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToEnrollment(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(Enrollment enrollment) throws SQLException {
        String sql = "UPDATE `enrollment` SET `enrolled_at` = ?, `completed_at` = ?, `progress_percent` = ?, "
                + "`status` = ?, `user_id` = ?, `course_id` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            setTimestamp(preparedStatement, 1, enrollment.getEnrolledAt());
            setTimestamp(preparedStatement, 2, enrollment.getCompletedAt());
            setShort(preparedStatement, 3, enrollment.getProgressPercent());
            preparedStatement.setString(4, enrollment.getStatus());
            setInteger(preparedStatement, 5, enrollment.getUserId());
            setInteger(preparedStatement, 6, enrollment.getCourseId());
            preparedStatement.setInt(7, enrollment.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM `enrollment` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Enrollment mapResultSetToEnrollment(ResultSet resultSet) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(resultSet.getInt("id"));
        enrollment.setEnrolledAt(getTimestamp(resultSet, "enrolled_at"));
        enrollment.setCompletedAt(getTimestamp(resultSet, "completed_at"));
        enrollment.setProgressPercent(getShort(resultSet, "progress_percent"));
        enrollment.setStatus(resultSet.getString("status"));
        enrollment.setUserId(getInteger(resultSet, "user_id"));
        enrollment.setCourseId(getInteger(resultSet, "course_id"));
        return enrollment;
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

    private void setShort(PreparedStatement preparedStatement, int index, Short value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.SMALLINT);
        } else {
            preparedStatement.setShort(index, value);
        }
    }

    private Short getShort(ResultSet resultSet, String column) throws SQLException {
        short value = resultSet.getShort(column);
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
