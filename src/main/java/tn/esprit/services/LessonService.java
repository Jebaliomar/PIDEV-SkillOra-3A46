package tn.esprit.services;

import tn.esprit.entities.Lesson;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

public class LessonService {

    private final Connection connection;

    public LessonService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Lesson lesson) throws SQLException {
        String sql = "INSERT INTO `lesson` (`title`, `type`, `content`, `file_path`, `position`, `created_at`, `updated_at`, `section_id`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, lesson.getTitle());
            preparedStatement.setString(2, lesson.getType());
            preparedStatement.setString(3, lesson.getContent());
            preparedStatement.setString(4, lesson.getFilePath());
            setInteger(preparedStatement, 5, lesson.getPosition());
            setTimestamp(preparedStatement, 6, lesson.getCreatedAt());
            setTimestamp(preparedStatement, 7, lesson.getUpdatedAt());
            setInteger(preparedStatement, 8, lesson.getSectionId());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    lesson.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Lesson> getAll() throws SQLException {
        String sql = "SELECT * FROM `lesson`";
        List<Lesson> lessons = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                lessons.add(mapResultSetToLesson(resultSet));
            }
        }

        return lessons;
    }

    public Lesson getById(int id) throws SQLException {
        String sql = "SELECT * FROM `lesson` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToLesson(resultSet);
                }
            }
        }

        return null;
    }

    public List<Lesson> getBySectionIds(List<Integer> sectionIds) throws SQLException {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return List.of();
        }

        String placeholders = sectionIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        String sql = "SELECT * FROM `lesson` WHERE `section_id` IN (" + placeholders + ")";
        List<Lesson> lessons = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int index = 0; index < sectionIds.size(); index++) {
                preparedStatement.setInt(index + 1, sectionIds.get(index));
            }

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lessons.add(mapResultSetToLesson(resultSet));
                }
            }
        }

        Set<Integer> sectionOrder = Set.copyOf(sectionIds);
        Map<Integer, Integer> sectionRanks = new HashMap<>();
        for (int index = 0; index < sectionIds.size(); index++) {
            sectionRanks.put(sectionIds.get(index), index);
        }
        return lessons.stream()
                .filter(lesson -> lesson.getSectionId() != null && sectionOrder.contains(lesson.getSectionId()))
                .sorted(Comparator.comparing((Lesson lesson) -> sectionRanks.getOrDefault(lesson.getSectionId(), Integer.MAX_VALUE))
                        .thenComparing(Lesson::getPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Lesson::getId, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    public boolean update(Lesson lesson) throws SQLException {
        String sql = "UPDATE `lesson` SET `title` = ?, `type` = ?, `content` = ?, `file_path` = ?, `position` = ?, "
                + "`created_at` = ?, `updated_at` = ?, `section_id` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, lesson.getTitle());
            preparedStatement.setString(2, lesson.getType());
            preparedStatement.setString(3, lesson.getContent());
            preparedStatement.setString(4, lesson.getFilePath());
            setInteger(preparedStatement, 5, lesson.getPosition());
            setTimestamp(preparedStatement, 6, lesson.getCreatedAt());
            setTimestamp(preparedStatement, 7, lesson.getUpdatedAt());
            setInteger(preparedStatement, 8, lesson.getSectionId());
            preparedStatement.setInt(9, lesson.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            executeDelete("DELETE FROM `lesson_completion` WHERE `lesson_id` = ?", id);
            boolean deleted = executeDelete("DELETE FROM `lesson` WHERE `id` = ?", id) > 0;
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

    private Lesson mapResultSetToLesson(ResultSet resultSet) throws SQLException {
        Lesson lesson = new Lesson();
        lesson.setId(resultSet.getInt("id"));
        lesson.setTitle(resultSet.getString("title"));
        lesson.setType(resultSet.getString("type"));
        lesson.setContent(resultSet.getString("content"));
        lesson.setFilePath(resultSet.getString("file_path"));
        lesson.setPosition(getInteger(resultSet, "position"));
        lesson.setCreatedAt(getTimestamp(resultSet, "created_at"));
        lesson.setUpdatedAt(getTimestamp(resultSet, "updated_at"));
        lesson.setSectionId(getInteger(resultSet, "section_id"));
        return lesson;
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
