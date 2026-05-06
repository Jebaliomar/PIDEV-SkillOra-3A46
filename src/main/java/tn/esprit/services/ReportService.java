package tn.esprit.services;

import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.entities.Report;
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
import java.util.List;
import java.util.Locale;

public class ReportService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REVIEWED = "reviewed";
    private static final String STATUS_DISMISSED = "dismissed";

    private final Connection connection;
    private final PostService postService;
    private final ReplyService replyService;
    private final SpamGuardService spamGuardService = new SpamGuardService();
    private final boolean replyIdColumnAvailable;

    public ReportService() {
        this.connection = MyConnection.getInstance().getConnection();
        this.replyIdColumnAvailable = ensureReportReplyColumn();
        this.postService = new PostService(this.connection);
        this.replyService = new ReplyService(this.connection);
    }

    public ReportService(Connection connection) {
        this.connection = connection;
        this.replyIdColumnAvailable = ensureReportReplyColumn();
        this.postService = new PostService(connection);
        this.replyService = new ReplyService(connection);
    }

    public void add(Report report) throws SQLException {
        validateForCreate(report);
        normalizeForCreate(report);

        if (!spamGuardService.allowReport(report.getUserId())) {
            throw new IllegalStateException("You are reporting too frequently. Please wait before reporting again.");
        }

        validateTargetExists(report);

        String sql = replyIdColumnAvailable
                ? """
                        INSERT INTO report
                        (post_id, reply_id, user_id, reason, description, status, created_at, reviewed_at, reviewed_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """
                : """
                        INSERT INTO report
                        (post_id, user_id, reason, description, status, created_at, reviewed_at, reviewed_by)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, report.getPostId());
            int parameterIndex = 2;
            if (replyIdColumnAvailable) {
                setNullableInteger(ps, parameterIndex++, report.getReplyId());
            }
            ps.setInt(parameterIndex++, report.getUserId());
            ps.setString(parameterIndex++, report.getReason());
            setNullableString(ps, parameterIndex++, report.getDescription());
            ps.setString(parameterIndex++, report.getStatus());
            setTimestamp(ps, parameterIndex++, report.getCreatedAt());
            setTimestamp(ps, parameterIndex++, report.getReviewedAt());
            setNullableInteger(ps, parameterIndex, report.getReviewedBy());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    report.setId(keys.getInt(1));
                }
            }
        }
    }

    public Report getById(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM report WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Report> getAll() throws SQLException {
        String sql = """
                SELECT * FROM report
                ORDER BY
                    CASE LOWER(status)
                        WHEN 'pending' THEN 0
                        WHEN 'reviewed' THEN 1
                        WHEN 'dismissed' THEN 2
                        ELSE 3
                    END,
                    created_at DESC,
                    id DESC
                """;
        return queryReports(sql);
    }

    public List<Report> getByStatus(String status) throws SQLException {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            return getAll();
        }

        String sql = """
                SELECT * FROM report
                WHERE LOWER(TRIM(status)) = ?
                ORDER BY created_at DESC, id DESC
                """;
        return queryReports(sql, normalized);
    }

    public List<Report> getByPostId(int postId) throws SQLException {
        return queryReports("SELECT * FROM report WHERE post_id = ? ORDER BY created_at DESC, id DESC", postId);
    }

    public List<Report> getByReplyId(int replyId) throws SQLException {
        if (!replyIdColumnAvailable) {
            return List.of();
        }
        return queryReports("SELECT * FROM report WHERE reply_id = ? ORDER BY created_at DESC, id DESC", replyId);
    }

    public int countAll() throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM report");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByStatus(String status) throws SQLException {
        String normalized = normalizeStatus(status);
        if (normalized == null) {
            return 0;
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT COUNT(*) FROM report WHERE LOWER(TRIM(status)) = ?")) {
            ps.setString(1, normalized);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean markReviewed(int reportId, Integer reviewerId) throws SQLException {
        return updateStatus(reportId, STATUS_REVIEWED, reviewerId);
    }

    public boolean dismiss(int reportId, Integer reviewerId) throws SQLException {
        return updateStatus(reportId, STATUS_DISMISSED, reviewerId);
    }

    public boolean updateStatus(int reportId, String newStatus, Integer reviewerId) throws SQLException {
        String normalized = normalizeStatus(newStatus);
        validateStatus(normalized);

        String sql = """
                UPDATE report
                SET status = ?, reviewed_at = ?, reviewed_by = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalized);
            setTimestamp(ps, 2, LocalDateTime.now());
            setNullableInteger(ps, 3, reviewerId);
            ps.setInt(4, reportId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int reportId) throws SQLException {
        if (reportId <= 0) {
            throw new IllegalArgumentException("A valid report ID is required.");
        }

        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM report WHERE id = ?")) {
            ps.setInt(1, reportId);
            return ps.executeUpdate() > 0;
        }
    }

    public void validateForCreate(Report report) {
        if (report == null) {
            throw new IllegalArgumentException("Report data is missing.");
        }
        if (report.getPostId() == null || report.getPostId() <= 0) {
            throw new IllegalArgumentException("A valid post ID is required.");
        }
        if (report.getReplyId() != null && report.getReplyId() <= 0) {
            throw new IllegalArgumentException("A valid reply ID is required.");
        }
        if (report.getReplyId() != null && !replyIdColumnAvailable) {
            throw new IllegalStateException("Reply reports require the report.reply_id database column.");
        }
        if (report.getUserId() == null || report.getUserId() <= 0) {
            throw new IllegalArgumentException("A valid reporting user ID is required.");
        }
        if (isBlank(report.getReason())) {
            throw new IllegalArgumentException("A report reason is required.");
        }
        if (report.getDescription() != null && report.getDescription().length() > 10000) {
            throw new IllegalArgumentException("Report description is too long.");
        }
    }

    private void normalizeForCreate(Report report) {
        report.setReason(trimToNull(report.getReason()));
        report.setDescription(trimToNull(report.getDescription()));
        report.setStatus(STATUS_PENDING);
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(LocalDateTime.now());
        }
        report.setReviewedAt(null);
        report.setReviewedBy(null);
    }

    private void validateTargetExists(Report report) throws SQLException {
        Post post = postService.getById(report.getPostId());
        if (post == null) {
            throw new IllegalArgumentException("The selected post no longer exists.");
        }

        Integer replyId = report.getReplyId();
        if (replyId == null) {
            return;
        }

        Reply reply = replyService.getById(replyId);
        if (reply == null) {
            throw new IllegalArgumentException("The selected reply no longer exists.");
        }
        if (!report.getPostId().equals(reply.getPostId())) {
            throw new IllegalArgumentException("The reply does not belong to the selected post.");
        }
    }

    private List<Report> queryReports(String sql, Object... params) throws SQLException {
        List<Report> reports = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParameters(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapRow(rs));
                }
            }
        }
        return reports;
    }

    private Report mapRow(ResultSet rs) throws SQLException {
        Report report = new Report();
        report.setId(rs.getInt("id"));
        report.setPostId(rs.getInt("post_id"));

        if (replyIdColumnAvailable) {
            int replyId = rs.getInt("reply_id");
            report.setReplyId(rs.wasNull() ? null : replyId);
        }

        int userId = rs.getInt("user_id");
        report.setUserId(rs.wasNull() ? null : userId);

        report.setReason(rs.getString("reason"));
        report.setDescription(rs.getString("description"));
        report.setStatus(rs.getString("status"));
        report.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        report.setReviewedAt(toLocalDateTime(rs.getTimestamp("reviewed_at")));

        int reviewedBy = rs.getInt("reviewed_by");
        report.setReviewedBy(rs.wasNull() ? null : reviewedBy);

        return report;
    }

    private void bindParameters(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;
            if (value instanceof Integer integer) {
                ps.setInt(index, integer);
            } else if (value instanceof String string) {
                ps.setString(index, string);
            } else {
                ps.setObject(index, value);
            }
        }
    }

    private void setNullableInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.LONGVARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private void setTimestamp(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateStatus(String status) {
        if (!STATUS_PENDING.equals(status) && !STATUS_REVIEWED.equals(status) && !STATUS_DISMISSED.equals(status)) {
            throw new IllegalArgumentException("Unsupported report status: " + status);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean ensureReportReplyColumn() {
        try {
            if (columnExists("report", "reply_id")) {
                return true;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE report ADD COLUMN reply_id INT NULL");
            }
            return columnExists("report", "reply_id");
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
}
