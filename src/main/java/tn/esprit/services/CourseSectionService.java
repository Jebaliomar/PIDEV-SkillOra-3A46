package tn.esprit.services;

import tn.esprit.entities.CourseSection;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CourseSectionService {

    private final Connection connection;

    public CourseSectionService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(CourseSection courseSection) throws SQLException {
        String sql = "INSERT INTO `course_section` (`title`, `position`, `created_at`, `updated_at`, `course_id`) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, courseSection.getTitle());
            setInteger(preparedStatement, 2, courseSection.getPosition());
            setTimestamp(preparedStatement, 3, courseSection.getCreatedAt());
            setTimestamp(preparedStatement, 4, courseSection.getUpdatedAt());
            setInteger(preparedStatement, 5, courseSection.getCourseId());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    courseSection.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<CourseSection> getAll() throws SQLException {
        String sql = "SELECT * FROM `course_section`";
        List<CourseSection> courseSections = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                courseSections.add(mapResultSetToCourseSection(resultSet));
            }
        }

        return courseSections;
    }

    public CourseSection getById(int id) throws SQLException {
        String sql = "SELECT * FROM `course_section` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToCourseSection(resultSet);
                }
            }
        }

        return null;
    }

    public List<CourseSection> findByCourse(int courseId) throws SQLException {
        String sql = "SELECT * FROM `course_section` WHERE `course_id` = ? ORDER BY `position` ASC, `id` ASC";
        List<CourseSection> courseSections = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, courseId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    courseSections.add(mapResultSetToCourseSection(resultSet));
                }
            }
        }

        return courseSections;
    }

    public boolean update(CourseSection courseSection) throws SQLException {
        String sql = "UPDATE `course_section` SET `title` = ?, `position` = ?, `created_at` = ?, `updated_at` = ?, "
                + "`course_id` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, courseSection.getTitle());
            setInteger(preparedStatement, 2, courseSection.getPosition());
            setTimestamp(preparedStatement, 3, courseSection.getCreatedAt());
            setTimestamp(preparedStatement, 4, courseSection.getUpdatedAt());
            setInteger(preparedStatement, 5, courseSection.getCourseId());
            preparedStatement.setInt(6, courseSection.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();

        try {
            connection.setAutoCommit(false);
            deleteLessonsBySection(id);

            boolean deleted;
            try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `course_section` WHERE `id` = ?")) {
                preparedStatement.setInt(1, id);
                deleted = preparedStatement.executeUpdate() > 0;
            }

            connection.commit();
            return deleted;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private void deleteLessonsBySection(int sectionId) throws SQLException {
        String sql = "DELETE FROM `lesson` WHERE `section_id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, sectionId);
            preparedStatement.executeUpdate();
        }
    }

    private CourseSection mapResultSetToCourseSection(ResultSet resultSet) throws SQLException {
        CourseSection courseSection = new CourseSection();
        courseSection.setId(resultSet.getInt("id"));
        courseSection.setTitle(resultSet.getString("title"));
        courseSection.setPosition(getInteger(resultSet, "position"));
        courseSection.setCreatedAt(getTimestamp(resultSet, "created_at"));
        courseSection.setUpdatedAt(getTimestamp(resultSet, "updated_at"));
        courseSection.setCourseId(getInteger(resultSet, "course_id"));
        return courseSection;
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
