package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostService {

    private static final int MYSQL_FK_VIOLATION = 1452;
    private static final String SQL_INTEGRITY_VIOLATION = "23000";
    private final ModerationService moderationService = new ModerationService();
    private final SpamGuardService spamGuardService = new SpamGuardService();
    private final LinkSecurityService linkSecurityService = new LinkSecurityService();
    private final Connection connection;
    private final boolean imageUrlColumnAvailable;

    public PostService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.imageUrlColumnAvailable = ensurePostImageColumn();
    }

    public PostService(Connection connection) {
        this.connection = connection;
        this.imageUrlColumnAvailable = ensurePostImageColumn();
    }

    public void add(Post post) throws SQLException {
        validateForCreate(post);
        normalizePostForWrite(post, true);

        if (!spamGuardService.allowPost(post.getUserId())) {
            throw new IllegalStateException("You are posting too frequently. Please wait before posting again.");
        }

        if (moderationService.shouldBlock(post.getContent())) {
            throw new IllegalStateException("Your post contains content that violates the moderation policy.");
        }

        linkSecurityService.validateContent(post.getContent());

        if (existsDuplicate(post)) {
            throw new IllegalStateException("A post with the same type, title, topic, and content already exists for this user.");
        }

        String sql = imageUrlColumnAvailable
                ? "INSERT INTO post (type, title, topic, content, image_url, created_at, updated_at, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO post (type, title, topic, content, created_at, updated_at, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getType());
            ps.setString(2, post.getTitle());
            ps.setString(3, post.getTopic());
            ps.setString(4, post.getContent());
            int parameterIndex = 5;
            if (imageUrlColumnAvailable) {
                setNullableString(ps, parameterIndex++, post.getImageUrl());
            }
            setTimestamp(ps, parameterIndex++, post.getCreatedAt());
            setTimestamp(ps, parameterIndex++, post.getUpdatedAt());
            setNullableInteger(ps, parameterIndex, post.getUserId());

            try {
                ps.executeUpdate();
            } catch (SQLException e) {
                if (SQL_INTEGRITY_VIOLATION.equals(e.getSQLState()) || e.getErrorCode() == MYSQL_FK_VIOLATION) {
                    throw new SQLException(
                            "Your user account was not found in the database. "
                                    + "Please make sure a matching user record exists (user_id = " + post.getUserId() + ").",
                            e.getSQLState(), e.getErrorCode(), e);
                }
                throw e;
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    post.setId(keys.getInt(1));
                }
            }
        }
    }

    public boolean update(Post post) throws SQLException {
        validateForUpdate(post);
        normalizePostForWrite(post, false);

        if (existsDuplicateExcludingId(post)) {
            throw new IllegalStateException("A similar post already exists for this user.");
        }

        String sql = imageUrlColumnAvailable
                ? "UPDATE post SET type = ?, title = ?, topic = ?, content = ?, image_url = ?, created_at = ?, updated_at = ?, user_id = ? WHERE id = ?"
                : "UPDATE post SET type = ?, title = ?, topic = ?, content = ?, created_at = ?, updated_at = ?, user_id = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, post.getType());
            ps.setString(2, post.getTitle());
            ps.setString(3, post.getTopic());
            ps.setString(4, post.getContent());
            int parameterIndex = 5;
            if (imageUrlColumnAvailable) {
                setNullableString(ps, parameterIndex++, post.getImageUrl());
            }
            setTimestamp(ps, parameterIndex++, post.getCreatedAt());
            setTimestamp(ps, parameterIndex++, post.getUpdatedAt());
            setNullableInteger(ps, parameterIndex++, post.getUserId());
            ps.setInt(parameterIndex, post.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("A valid post ID is required.");
        }

        if (!existsById(id)) {
            return false;
        }

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            deleteRelatedRows("reaction", "post_id", id);
            deleteRelatedRows("vote", "post_id", id);
            deleteRelatedRows("report", "post_id", id);
            deleteRelatedRows("post_tag", "post_id", id);
            deleteRepliesForPost(id);

            boolean deleted;
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM post WHERE id = ?")) {
                ps.setInt(1, id);
                deleted = ps.executeUpdate() > 0;
            }

            connection.commit();
            return deleted;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public List<Post> getAll() throws SQLException {
        String sql = "SELECT * FROM post ORDER BY created_at DESC, id DESC";
        List<Post> posts = new ArrayList<>();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
        }
        return posts;
    }

    public Post getById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM post WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPost(rs);
                }
            }
        }
        return null;
    }

    public List<Post> getByUserId(int userId) throws SQLException {
        List<Post> posts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM post WHERE user_id = ? ORDER BY created_at DESC, id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapResultSetToPost(rs));
                }
            }
        }
        return posts;
    }

    public List<Post> searchAndFilter(String searchTerm, String type, String topic) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM post WHERE 1=1");
        List<Object> params = new ArrayList<>();

        String search = normalizeFilterValue(searchTerm);
        String typeVal = normalizeFilterValue(type);
        String topicVal = normalizeFilterValue(topic);

        if (search != null) {
            sql.append(" AND (LOWER(title) LIKE ? OR LOWER(topic) LIKE ? OR LOWER(content) LIKE ?)");
            String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (typeVal != null) {
            sql.append(" AND LOWER(TRIM(type)) = ?");
            params.add(typeVal.toLowerCase(Locale.ROOT));
        }
        if (topicVal != null) {
            sql.append(" AND LOWER(TRIM(topic)) = ?");
            params.add(topicVal.toLowerCase(Locale.ROOT));
        }

        sql.append(" ORDER BY created_at DESC, id DESC");

        List<Post> posts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            bindParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(mapResultSetToPost(rs));
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
        if (post.getCreatedAt() == null) {
            throw new IllegalArgumentException("Post creation date is required.");
        }
    }

    public void validateForUpdate(Post post) {
        validateForCreate(post);
        if (post.getId() == null || post.getId() <= 0) {
            throw new IllegalArgumentException("A valid post ID is required for update.");
        }
    }

    public boolean existsDuplicate(Post post) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM post
                WHERE LOWER(TRIM(COALESCE(type,''))) = ?
                  AND LOWER(TRIM(COALESCE(title,''))) = ?
                  AND LOWER(TRIM(COALESCE(topic,''))) = ?
                  AND LOWER(TRIM(COALESCE(content,''))) = ?
                  AND COALESCE(user_id,-1) = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizeComparableValue(post.getType()));
            ps.setString(2, normalizeComparableValue(post.getTitle()));
            ps.setString(3, normalizeComparableValue(post.getTopic()));
            ps.setString(4, normalizeComparableValue(post.getContent()));
            ps.setInt(5, post.getUserId() == null ? -1 : post.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean existsDuplicateExcludingId(Post post) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM post
                WHERE id <> ?
                  AND LOWER(TRIM(COALESCE(type,''))) = ?
                  AND LOWER(TRIM(COALESCE(title,''))) = ?
                  AND LOWER(TRIM(COALESCE(topic,''))) = ?
                  AND LOWER(TRIM(COALESCE(content,''))) = ?
                  AND COALESCE(user_id,-1) = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, post.getId());
            ps.setString(2, normalizeComparableValue(post.getType()));
            ps.setString(3, normalizeComparableValue(post.getTitle()));
            ps.setString(4, normalizeComparableValue(post.getTopic()));
            ps.setString(5, normalizeComparableValue(post.getContent()));
            ps.setInt(6, post.getUserId() == null ? -1 : post.getUserId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean existsById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM post WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<String> getDistinctValues(String columnName) throws SQLException {
        String sql = "SELECT DISTINCT TRIM(" + columnName + ") AS value FROM post WHERE "
                + columnName + " IS NOT NULL AND TRIM(" + columnName + ") <> '' ORDER BY LOWER(TRIM(" + columnName + ")) ASC";
        List<String> values = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString("value"));
            }
        }
        return values;
    }

    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("id"));
        post.setType(rs.getString("type"));
        post.setTitle(rs.getString("title"));
        post.setTopic(rs.getString("topic"));
        post.setContent(rs.getString("content"));
        if (imageUrlColumnAvailable) {
            post.setImageUrl(rs.getString("image_url"));
        }
        post.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        post.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));

        int userId = rs.getInt("user_id");
        post.setUserId(rs.wasNull() ? null : userId);
        return post;
    }

    private void deleteRepliesForPost(int postId) throws SQLException {
        List<Integer> replyIds = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT id FROM reply WHERE post_id = ?")) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    replyIds.add(rs.getInt("id"));
                }
            }
        }

        for (Integer replyId : replyIds) {
            deleteRelatedRows("reaction", "reply_id", replyId);
            deleteRelatedRows("vote", "reply_id", replyId);
        }

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM reply WHERE post_id = ?")) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        }
    }

    private void deleteRelatedRows(String table, String column, int fk) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM " + table + " WHERE " + column + " = ?")) {
            ps.setInt(1, fk);
            ps.executeUpdate();
        }
    }

    private void setNullableInteger(PreparedStatement ps, int idx, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, java.sql.Types.INTEGER);
        } else {
            ps.setInt(idx, value);
        }
    }

    private void setNullableString(PreparedStatement ps, int idx, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(idx, java.sql.Types.VARCHAR);
        } else {
            ps.setString(idx, value.trim());
        }
    }

    private void setTimestamp(PreparedStatement ps, int idx, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(idx, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(idx, Timestamp.valueOf(value));
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    private void bindParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof String s) {
                ps.setString(i + 1, s);
            } else if (p instanceof Integer iv) {
                ps.setInt(i + 1, iv);
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    private String normalizeFilterValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeComparableValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void normalizePostForWrite(Post post, boolean isCreate) {
        post.setType(trimToNull(post.getType()));
        post.setTitle(trimToNull(post.getTitle()));
        post.setTopic(trimToNull(post.getTopic()));
        post.setContent(trimToNull(post.getContent()));

        if (isCreate) {
            if (post.getCreatedAt() == null) {
                post.setCreatedAt(LocalDateTime.now());
            }
            if (post.getUpdatedAt() == null) {
                post.setUpdatedAt(post.getCreatedAt());
            }
        } else if (post.getUpdatedAt() == null) {
            post.setUpdatedAt(LocalDateTime.now());
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean ensurePostImageColumn() {
        try {
            if (columnExists("post", "image_url")) {
                return true;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE post ADD COLUMN image_url VARCHAR(1024) NULL");
            }
            return columnExists("post", "image_url");
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean columnExists(String tableName, String columnName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT))) {
            return columns.next();
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public int countPostsWithReplies() throws SQLException {
        String sql = """
            SELECT COUNT(DISTINCT p.id)
            FROM post p
            INNER JOIN reply r ON r.post_id = p.id
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public Map<Integer, Integer> getReplyCountsByPost() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT post_id, COUNT(*) AS cnt FROM reply GROUP BY post_id";

        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt("post_id"), rs.getInt("cnt"));
            }
        }
        return counts;
    }

    public Map<Integer, Integer> getReactionCountsByPost() throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT post_id, COUNT(*) AS cnt FROM reaction WHERE post_id IS NOT NULL GROUP BY post_id";

        try (PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt("post_id"), rs.getInt("cnt"));
            }
        }
        return counts;
    }
}
