package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostService {

    private final Connection connection;

    public PostService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Post post) throws SQLException {
        String sql = "INSERT INTO post (type, title, topic, content, created_at, updated_at, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, post.getType());
            preparedStatement.setString(2, post.getTitle());
            preparedStatement.setString(3, post.getTopic());
            preparedStatement.setString(4, post.getContent());
            setTimestamp(preparedStatement, 5, post.getCreatedAt());
            setTimestamp(preparedStatement, 6, post.getUpdatedAt());
            setNullableInteger(preparedStatement, 7, post.getUserId());

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    post.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Post> getAll() throws SQLException {
        String sql = "SELECT * FROM post ORDER BY created_at DESC, id DESC";
        List<Post> posts = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                posts.add(mapResultSetToPost(resultSet));
            }
        }

        return posts;
    }

    public Post getById(int id) throws SQLException {
        String sql = "SELECT * FROM post WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToPost(resultSet);
                }
            }
        }

        return null;
    }

    public List<Post> getByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM post WHERE user_id = ? ORDER BY created_at DESC, id DESC";
        List<Post> posts = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(mapResultSetToPost(resultSet));
                }
            }
        }

        return posts;
    }

    public boolean update(Post post) throws SQLException {
        String sql = "UPDATE post SET type = ?, title = ?, topic = ?, content = ?, created_at = ?, updated_at = ?, user_id = ? "
                + "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, post.getType());
            preparedStatement.setString(2, post.getTitle());
            preparedStatement.setString(3, post.getTopic());
            preparedStatement.setString(4, post.getContent());
            setTimestamp(preparedStatement, 5, post.getCreatedAt());
            setTimestamp(preparedStatement, 6, post.getUpdatedAt());
            setNullableInteger(preparedStatement, 7, post.getUserId());
            preparedStatement.setInt(8, post.getId());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM post WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Post mapResultSetToPost(ResultSet resultSet) throws SQLException {
        Post post = new Post();
        post.setId(resultSet.getInt("id"));
        post.setType(resultSet.getString("type"));
        post.setTitle(resultSet.getString("title"));
        post.setTopic(resultSet.getString("topic"));
        post.setContent(resultSet.getString("content"));
        post.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        post.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));

        int userId = resultSet.getInt("user_id");
        if (resultSet.wasNull()) {
            post.setUserId(null);
        } else {
            post.setUserId(userId);
        }

        return post;
    }

    private void setNullableInteger(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private void setTimestamp(PreparedStatement preparedStatement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private java.time.LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
