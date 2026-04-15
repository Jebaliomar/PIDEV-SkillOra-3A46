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
import java.util.Locale;

public class PostService {

    private final Connection connection;

    public PostService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Post post) throws SQLException {
        validateForCreate(post);
        if (existsDuplicate(post)) {
            throw new IllegalStateException("A post with the same type, title, topic, and content already exists for this user.");
        }

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

    public void validateForCreate(Post post) {
        if (post == null) {
            throw new IllegalArgumentException("Post data is missing.");
        }
        if (isBlank(post.getType())) {
            throw new IllegalArgumentException("Post type is required.");
        }
        if (isBlank(post.getTitle())) {
            throw new IllegalArgumentException("Post title is required.");
        }
        if (isBlank(post.getTopic())) {
            throw new IllegalArgumentException("Post topic is required.");
        }
        if (isBlank(post.getContent())) {
            throw new IllegalArgumentException("Post content is required.");
        }
        if (post.getUserId() == null || post.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid user ID is required.");
        }
        if (post.getCreatedAt() == null || post.getUpdatedAt() == null) {
            throw new IllegalArgumentException("Post dates are invalid.");
        }
    }

    public boolean existsDuplicate(Post post) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM post
                WHERE LOWER(TRIM(COALESCE(type, ''))) = ?
                  AND LOWER(TRIM(COALESCE(title, ''))) = ?
                  AND LOWER(TRIM(COALESCE(topic, ''))) = ?
                  AND LOWER(TRIM(COALESCE(content, ''))) = ?
                  AND COALESCE(user_id, -1) = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, normalizeComparableValue(post.getType()));
            preparedStatement.setString(2, normalizeComparableValue(post.getTitle()));
            preparedStatement.setString(3, normalizeComparableValue(post.getTopic()));
            preparedStatement.setString(4, normalizeComparableValue(post.getContent()));
            preparedStatement.setInt(5, post.getUserId() == null ? -1 : post.getUserId());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
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

    public List<Post> searchAndFilter(String searchTerm, String type, String topic) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM post WHERE 1=1");
        List<Object> parameters = new ArrayList<>();

        String normalizedSearch = normalizeFilterValue(searchTerm);
        String normalizedType = normalizeFilterValue(type);
        String normalizedTopic = normalizeFilterValue(topic);

        if (normalizedSearch != null) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(topic) LIKE ? OR LOWER(content) LIKE ?)");
            String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }

        if (normalizedType != null) {
            sql.append(" AND type = ?");
            parameters.add(normalizedType);
        }

        if (normalizedTopic != null) {
            sql.append(" AND topic = ?");
            parameters.add(normalizedTopic);
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        List<Post> posts = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            bindParameters(preparedStatement, parameters);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(mapResultSetToPost(resultSet));
                }
            }
        }

        return posts;
    }

    public List<String> getDistinctTypes() throws SQLException {
        return getDistinctValues("type");
    }

    public List<String> getDistinctTopics() throws SQLException {
        return getDistinctValues("topic");
    }

    public boolean update(Post post) throws SQLException {
        validateForUpdate(post);
        if (existsDuplicateExcludingId(post)) {
            throw new IllegalStateException("A similar post already exists for this user.");
        }

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
        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            deleteRelatedRows("reaction", "post_id", id);
            deleteRelatedRows("vote", "post_id", id);
            deleteRelatedRows("report", "post_id", id);
            deleteRelatedRows("post_tag", "post_id", id);
            deleteRepliesForPost(id);

            String sql = "DELETE FROM post WHERE id = ?";
            boolean deleted;

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, id);
                deleted = preparedStatement.executeUpdate() > 0;
            }

            connection.commit();
            return deleted;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void validateForUpdate(Post post) {
        validateForCreate(post);
        if (post.getId() == null || post.getId() <= 0) {
            throw new IllegalArgumentException("A valid post ID is required for update.");
        }
    }

    public boolean existsDuplicateExcludingId(Post post) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM post
                WHERE id <> ?
                  AND LOWER(TRIM(COALESCE(type, ''))) = ?
                  AND LOWER(TRIM(COALESCE(title, ''))) = ?
                  AND LOWER(TRIM(COALESCE(topic, ''))) = ?
                  AND LOWER(TRIM(COALESCE(content, ''))) = ?
                  AND COALESCE(user_id, -1) = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, post.getId());
            preparedStatement.setString(2, normalizeComparableValue(post.getType()));
            preparedStatement.setString(3, normalizeComparableValue(post.getTitle()));
            preparedStatement.setString(4, normalizeComparableValue(post.getTopic()));
            preparedStatement.setString(5, normalizeComparableValue(post.getContent()));
            preparedStatement.setInt(6, post.getUserId() == null ? -1 : post.getUserId());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private List<String> getDistinctValues(String columnName) throws SQLException {
        String sql = "SELECT DISTINCT " + columnName + " FROM post WHERE " + columnName
                + " IS NOT NULL AND TRIM(" + columnName + ") <> '' ORDER BY " + columnName + " ASC";
        List<String> values = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
        }

        return values;
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

    private void bindParameters(PreparedStatement preparedStatement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object parameter = parameters.get(index);
            if (parameter instanceof String stringValue) {
                preparedStatement.setString(index + 1, stringValue);
            } else if (parameter instanceof Integer integerValue) {
                preparedStatement.setInt(index + 1, integerValue);
            } else {
                preparedStatement.setObject(index + 1, parameter);
            }
        }
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }

    private String normalizeComparableValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void deleteRepliesForPost(int postId) throws SQLException {
        List<Integer> replyIds = new ArrayList<>();
        String selectSql = "SELECT id FROM reply WHERE post_id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {
            preparedStatement.setInt(1, postId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    replyIds.add(resultSet.getInt("id"));
                }
            }
        }

        for (Integer replyId : replyIds) {
            deleteRelatedRows("reaction", "reply_id", replyId);
            deleteRelatedRows("vote", "reply_id", replyId);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM reply WHERE post_id = ?")) {
            preparedStatement.setInt(1, postId);
            preparedStatement.executeUpdate();
        }
    }

    private void deleteRelatedRows(String tableName, String columnName, int foreignKeyValue) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, foreignKeyValue);
            preparedStatement.executeUpdate();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
