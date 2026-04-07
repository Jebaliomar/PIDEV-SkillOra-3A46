package tn.esprit.services;

import tn.esprit.entities.Reply;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ReplyService {

    private final Connection connection;

    public ReplyService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Reply reply) throws SQLException {
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
        String sql = "DELETE FROM reply WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Reply mapResultSetToReply(ResultSet resultSet) throws SQLException {
        Reply reply = new Reply();
        reply.setId(resultSet.getInt("id"));
        reply.setPostId(resultSet.getInt("post_id"));

        int parentId = resultSet.getInt("parent_id");
        if (resultSet.wasNull()) {
            reply.setParentId(null);
        } else {
            reply.setParentId(parentId);
        }

        reply.setContent(resultSet.getString("content"));
        reply.setAuthorName(resultSet.getString("author_name"));

        int upvotes = resultSet.getInt("upvotes");
        if (resultSet.wasNull()) {
            reply.setUpvotes(null);
        } else {
            reply.setUpvotes(upvotes);
        }

        reply.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        reply.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));

        int userId = resultSet.getInt("user_id");
        if (resultSet.wasNull()) {
            reply.setUserId(null);
        } else {
            reply.setUserId(userId);
        }

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
}
