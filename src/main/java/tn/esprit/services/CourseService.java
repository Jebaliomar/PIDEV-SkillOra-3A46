package tn.esprit.services;

import tn.esprit.entities.Course;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CourseService {

    private final Connection connection;

    public CourseService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Course course) throws SQLException {
        String sql = "INSERT INTO `course` (`title`, `category`, `description`, `thumbnail`, `status`, `created_at`, `updated_at`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, course.getTitle());
            preparedStatement.setString(2, course.getCategory());
            preparedStatement.setString(3, course.getDescription());
            preparedStatement.setString(4, course.getThumbnail());
            preparedStatement.setString(5, course.getStatus());
            setTimestamp(preparedStatement, 6, course.getCreatedAt());
            setTimestamp(preparedStatement, 7, course.getUpdatedAt());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    course.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Course> getAll() throws SQLException {
        String sql = "SELECT * FROM `course`";
        List<Course> courses = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                courses.add(mapResultSetToCourse(resultSet));
            }
        }

        return courses;
    }

    public Course getById(int id) throws SQLException {
        String sql = "SELECT * FROM `course` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToCourse(resultSet);
                }
            }
        }

        return null;
    }

    public List<Course> getByIds(List<Integer> courseIds) throws SQLException {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }

        String placeholders = courseIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        String sql = "SELECT * FROM `course` WHERE `id` IN (" + placeholders + ")";
        List<Course> courses = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int index = 0; index < courseIds.size(); index++) {
                preparedStatement.setInt(index + 1, courseIds.get(index));
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(mapResultSetToCourse(resultSet));
                }
            }
        }

        Map<Integer, Integer> ranks = new HashMap<>();
        for (int index = 0; index < courseIds.size(); index++) {
            ranks.put(courseIds.get(index), index);
        }
        return courses.stream()
                .sorted(java.util.Comparator.comparing(course -> ranks.getOrDefault(course.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    public boolean update(Course course) throws SQLException {
        String sql = "UPDATE `course` SET `title` = ?, `category` = ?, `description` = ?, `thumbnail` = ?, "
                + "`status` = ?, `created_at` = ?, `updated_at` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, course.getTitle());
            preparedStatement.setString(2, course.getCategory());
            preparedStatement.setString(3, course.getDescription());
            preparedStatement.setString(4, course.getThumbnail());
            preparedStatement.setString(5, course.getStatus());
            setTimestamp(preparedStatement, 6, course.getCreatedAt());
            setTimestamp(preparedStatement, 7, course.getUpdatedAt());
            preparedStatement.setInt(8, course.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            executeDelete(
                    "DELETE FROM `lesson_completion` WHERE `lesson_id` IN (" +
                            "SELECT `lesson`.`id` FROM `lesson` " +
                            "INNER JOIN `course_section` ON `lesson`.`section_id` = `course_section`.`id` " +
                            "WHERE `course_section`.`course_id` = ?)",
                    id
            );
            executeDelete(
                    "DELETE FROM `lesson_completion` WHERE `enrollment_id` IN (" +
                            "SELECT `id` FROM `enrollment` WHERE `course_id` = ?)",
                    id
            );
            executeDelete("DELETE FROM `enrollment` WHERE `course_id` = ?", id);
            executeDelete(
                    "DELETE FROM `lesson` WHERE `section_id` IN (" +
                            "SELECT `id` FROM `course_section` WHERE `course_id` = ?)",
                    id
            );
            executeDelete("DELETE FROM `course_section` WHERE `course_id` = ?", id);
            boolean deleted = executeDelete("DELETE FROM `course` WHERE `id` = ?", id) > 0;
            connection.commit();
            return deleted;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }
    }

    private int executeDelete(String sql, int id) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate();
        }
    }

    private Course mapResultSetToCourse(ResultSet resultSet) throws SQLException {
        Course course = new Course();
        course.setId(resultSet.getInt("id"));
        course.setTitle(resultSet.getString("title"));
        course.setCategory(resultSet.getString("category"));
        course.setDescription(resultSet.getString("description"));
        course.setThumbnail(resultSet.getString("thumbnail"));
        course.setStatus(resultSet.getString("status"));
        course.setCreatedAt(getTimestamp(resultSet, "created_at"));
        course.setUpdatedAt(getTimestamp(resultSet, "updated_at"));
        return course;
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
