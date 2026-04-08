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
import java.util.List;

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
        String sql = "DELETE FROM `course` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
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
