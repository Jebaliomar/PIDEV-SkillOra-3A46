package tn.esprit.services;

import tn.esprit.entities.Reply;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ReplyService {

    private final Connection connection;
    private final PostService postService;
    private final ModerationService moderationService = new ModerationService();

    public ReplyService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.postService = new PostService(this.connection);
    }

    public ReplyService(Connection connection) {
        this.connection = connection;
        this.postService = new PostService(connection);
    }

    public void add(Reply reply) throws SQLException {
        validateForCreate(reply);
        normalizeReplyForWrite(reply, true);
        validateParentReply(reply);

        if (moderationService.shouldBlock(reply.getContent())) {
            throw new IllegalStateException("Your reply contains content that violates the moderation policy.");
        }

        if (existsDuplicate(reply)) {
            throw new IllegalStateException("You already posted the same reply on this post.");
        }

        String sql = "INSERT INTO reply (post_id, parent_id, content, author_name, upvotes, created_at, updated_at, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, reply.getPostId());
            setNullableInteger(preparedStatement, 2, reply.getParentId());
            preparedStatement.setString(3, reply.getContent());
            preparedStatement.setString(4, reply.getAuthorName());
            setNullableInteger(preparedStatement, 5, reply.getUpvotes());
            setTimestamp(preparedStatement, 6, reply.getCreatedAt());
            setTimestamp(preparedStatement, 7, reply.getUpdatedAt());
            setNullableInteger(preparedStatement, 8, reply.getUserId());

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reply.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Reply> getAll() throws SQLException {
        String sql = "SELECT * FROM reply ORDER BY created_at DESC, id DESC";
        List<Reply> replies = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                replies.add(mapResultSetToReply(resultSet));
            }
        }

        return replies;
    }

    public Reply getById(int id) throws SQLException {
        String sql = "SELECT * FROM reply WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToReply(resultSet);
                }
            }
        }

        return null;
    }

    public List<Reply> getByPostId(int postId) throws SQLException {
        String sql = "SELECT * FROM reply WHERE post_id = ? ORDER BY created_at ASC, id ASC";
        List<Reply> replies = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, postId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    replies.add(mapResultSetToReply(resultSet));
                }
            }
        }

        return replies;
    }

    public boolean update(Reply reply) throws SQLException {
        validateForUpdate(reply);
        normalizeReplyForWrite(reply, false);
        validateParentReply(reply);

        if (existsDuplicateExcludingId(reply)) {
            throw new IllegalStateException("You already have the same reply on this post.");
        }

        String sql = "UPDATE reply SET post_id = ?, parent_id = ?, content = ?, author_name = ?, upvotes = ?, "
                + "created_at = ?, updated_at = ?, user_id = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, reply.getPostId());
            setNullableInteger(preparedStatement, 2, reply.getParentId());
            preparedStatement.setString(3, reply.getContent());
            preparedStatement.setString(4, reply.getAuthorName());
            setNullableInteger(preparedStatement, 5, reply.getUpvotes());
            setTimestamp(preparedStatement, 6, reply.getCreatedAt());
            setTimestamp(preparedStatement, 7, reply.getUpdatedAt());
            setNullableInteger(preparedStatement, 8, reply.getUserId());
            preparedStatement.setInt(9, reply.getId());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        if (id <= 0) {
            throw new IllegalArgumentException("A valid reply ID is required.");
        }

        if (getById(id) == null) {
            return false;
        }

        boolean initialAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            List<Integer> replyIds = collectReplyIdsForDeletion(id);

            for (int index = replyIds.size() - 1; index >= 0; index--) {
                Integer replyId = replyIds.get(index);
                deleteRelatedRows("reaction", "reply_id", replyId);
                deleteRelatedRows("vote", "reply_id", replyId);
                deleteRelatedRows("report", "reply_id", replyId);

                try (PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM reply WHERE id = ?")) {
                    preparedStatement.setInt(1, replyId);
                    preparedStatement.executeUpdate();
                }
            }

            connection.commit();
            return true;
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(initialAutoCommit);
        }
    }

    public void validateForCreate(Reply reply) {
        if (reply == null) {
            throw new IllegalArgumentException("Reply data is missing.");
        }
        if (reply.getPostId() == null || reply.getPostId() <= 0) {
            throw new IllegalArgumentException("A valid post ID is required.");
        }
        if (reply.getParentId() != null && reply.getParentId() <= 0) {
            throw new IllegalArgumentException("A valid parent reply ID is required.");
        }
        if (isBlank(reply.getAuthorName())) {
            throw new IllegalArgumentException("Reply author is required.");
        }
        if (isBlank(reply.getContent())) {
            throw new IllegalArgumentException("Reply content is required.");
        }
        if (reply.getUserId() == null || reply.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid user ID is required.");
        }
        if (reply.getUpvotes() != null && reply.getUpvotes() < 0) {
            throw new IllegalArgumentException("Reply upvotes must be a valid number.");
        }
        if (reply.getCreatedAt() == null || reply.getUpdatedAt() == null) {
            throw new IllegalArgumentException("Reply dates are invalid.");
        }
    }

    public void validateForUpdate(Reply reply) {
        validateForCreate(reply);
        if (reply.getId() == null || reply.getId() <= 0) {
            throw new IllegalArgumentException("A valid reply ID is required for update.");
        }
    }

    public boolean existsDuplicate(Reply reply) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM reply
                WHERE post_id = ?
                  AND LOWER(TRIM(COALESCE(author_name, ''))) = ?
                  AND LOWER(TRIM(COALESCE(content, ''))) = ?
                  AND COALESCE(user_id, -1) = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, reply.getPostId());
            preparedStatement.setString(2, normalizeComparableValue(reply.getAuthorName()));
            preparedStatement.setString(3, normalizeComparableValue(reply.getContent()));
            preparedStatement.setInt(4, reply.getUserId() == null ? -1 : reply.getUserId());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    public boolean existsDuplicateExcludingId(Reply reply) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM reply
                WHERE id <> ?
                  AND post_id = ?
                  AND LOWER(TRIM(COALESCE(author_name, ''))) = ?
                  AND LOWER(TRIM(COALESCE(content, ''))) = ?
                  AND COALESCE(user_id, -1) = ?
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, reply.getId());
            preparedStatement.setInt(2, reply.getPostId());
            preparedStatement.setString(3, normalizeComparableValue(reply.getAuthorName()));
            preparedStatement.setString(4, normalizeComparableValue(reply.getContent()));
            preparedStatement.setInt(5, reply.getUserId() == null ? -1 : reply.getUserId());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }

    private Reply mapResultSetToReply(ResultSet resultSet) throws SQLException {
        Reply reply = new Reply();
        reply.setId(resultSet.getInt("id"));
        reply.setPostId(resultSet.getInt("post_id"));

        int parentId = resultSet.getInt("parent_id");
        reply.setParentId(resultSet.wasNull() ? null : parentId);

        reply.setContent(resultSet.getString("content"));
        reply.setAuthorName(resultSet.getString("author_name"));

        int upvotes = resultSet.getInt("upvotes");
        reply.setUpvotes(resultSet.wasNull() ? null : upvotes);

        reply.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        reply.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));

        int userId = resultSet.getInt("user_id");
        reply.setUserId(resultSet.wasNull() ? null : userId);

        return reply;
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

    private String normalizeComparableValue(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private List<Integer> collectReplyIdsForDeletion(int rootReplyId) throws SQLException {
        List<Integer> replyIds = new ArrayList<>();
        Set<Integer> visited = new LinkedHashSet<>();
        Deque<Integer> pendingReplyIds = new ArrayDeque<>();
        pendingReplyIds.add(rootReplyId);

        while (!pendingReplyIds.isEmpty()) {
            Integer currentReplyId = pendingReplyIds.removeFirst();
            if (!visited.add(currentReplyId)) {
                continue;
            }

            replyIds.add(currentReplyId);

            String sql = "SELECT id FROM reply WHERE parent_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, currentReplyId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        pendingReplyIds.add(resultSet.getInt("id"));
                    }
                }
            }
        }

        return replyIds;
    }

    private void deleteRelatedRows(String tableName, String columnName, int foreignKeyValue) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, foreignKeyValue);
            preparedStatement.executeUpdate();
        }
    }

    private void validateParentReply(Reply reply) throws SQLException {
        if (reply.getParentId() == null) {
            return;
        }

        Reply parent = getById(reply.getParentId());
        if (parent == null) {
            throw new IllegalArgumentException("The selected parent reply does not exist.");
        }
        if (!reply.getPostId().equals(parent.getPostId())) {
            throw new IllegalArgumentException("A reply can only reference a parent from the same post.");
        }
    }

    private void normalizeReplyForWrite(Reply reply, boolean isCreate) {
        reply.setAuthorName(trimToNull(reply.getAuthorName()));
        reply.setContent(trimToNull(reply.getContent()));

        if (reply.getUpvotes() == null) {
            reply.setUpvotes(0);
        }

        if (isCreate) {
            if (reply.getCreatedAt() == null) {
                reply.setCreatedAt(LocalDateTime.now());
            }
            if (reply.getUpdatedAt() == null) {
                reply.setUpdatedAt(reply.getCreatedAt());
            }
        } else if (reply.getUpdatedAt() == null) {
            reply.setUpdatedAt(LocalDateTime.now());
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
