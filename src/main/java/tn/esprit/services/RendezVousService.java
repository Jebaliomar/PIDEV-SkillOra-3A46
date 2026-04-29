package tn.esprit.services;

import tn.esprit.entities.RendezVous;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RendezVousService {

    private final Connection connection;

    public RendezVousService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(RendezVous rendezVous) throws SQLException {
        RendezVousSchema schema = resolveSchema();

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        appendColumn(columns, placeholders, schema.studentIdColumn);
        appendColumn(columns, placeholders, schema.professorIdColumn);
        appendColumn(columns, placeholders, schema.courseIdColumn);
        appendColumn(columns, placeholders, schema.slotIdColumn);
        appendColumn(columns, placeholders, schema.statutColumn);
        appendColumn(columns, placeholders, schema.meetingTypeColumn);
        appendColumn(columns, placeholders, schema.meetingLinkColumn);
        appendColumn(columns, placeholders, schema.locationColumn);
        appendColumn(columns, placeholders, schema.messageColumn);
        appendColumn(columns, placeholders, schema.createdAtColumn);
        appendColumn(columns, placeholders, schema.locationLabelColumn);
        appendColumn(columns, placeholders, schema.locationLatColumn);
        appendColumn(columns, placeholders, schema.locationLngColumn);
        appendColumn(columns, placeholders, schema.refusalReasonColumn);
        appendColumn(columns, placeholders, schema.coursePdfNameColumn);

        if (columns.length() == 0) {
            throw new SQLException("No writable columns found in " + schema.tableName);
        }

        String sql = "INSERT INTO `" + schema.tableName + "` (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;

            if (schema.studentIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getStudentId());
            }
            if (schema.professorIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getProfessorId());
            }
            if (schema.courseIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getCourseId());
            }
            if (schema.slotIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getSlotId());
            }
            if (schema.statutColumn != null) {
                preparedStatement.setString(index++, rendezVous.getStatut());
            }
            if (schema.meetingTypeColumn != null) {
                preparedStatement.setString(index++, rendezVous.getMeetingType());
            }
            if (schema.meetingLinkColumn != null) {
                preparedStatement.setString(index++, rendezVous.getMeetingLink());
            }
            if (schema.locationColumn != null) {
                preparedStatement.setString(index++, rendezVous.getLocation());
            }
            if (schema.messageColumn != null) {
                preparedStatement.setString(index++, rendezVous.getMessage());
            }
            if (schema.createdAtColumn != null) {
                setNullableTimestamp(preparedStatement, index++, rendezVous.getCreatedAt());
            }
            if (schema.locationLabelColumn != null) {
                preparedStatement.setString(index++, rendezVous.getLocationLabel());
            }
            if (schema.locationLatColumn != null) {
                setNullableFloat(preparedStatement, index++, rendezVous.getLocationLat());
            }
            if (schema.locationLngColumn != null) {
                setNullableFloat(preparedStatement, index++, rendezVous.getLocationLng());
            }
            if (schema.refusalReasonColumn != null) {
                preparedStatement.setString(index++, rendezVous.getRefusalReason());
            }
            if (schema.coursePdfNameColumn != null) {
                preparedStatement.setString(index, rendezVous.getCoursePdfName());
            }

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rendezVous.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<RendezVous> getAll() throws SQLException {
        RendezVousSchema schema = resolveSchema();
        StringBuilder sql = new StringBuilder("SELECT * FROM `").append(schema.tableName).append("`");
        if (schema.createdAtColumn != null) {
            sql.append(" ORDER BY `").append(schema.createdAtColumn).append("` DESC, `").append(schema.idColumn).append("` DESC");
        } else {
            sql.append(" ORDER BY `").append(schema.idColumn).append("` DESC");
        }

        List<RendezVous> rendezVousList = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql.toString())) {
            while (resultSet.next()) {
                rendezVousList.add(mapResultSetToRendezVous(resultSet, schema));
            }
        }
        return rendezVousList;
    }

    public RendezVous getById(int id) throws SQLException {
        RendezVousSchema schema = resolveSchema();
        String sql = "SELECT * FROM `" + schema.tableName + "` WHERE `" + schema.idColumn + "` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToRendezVous(resultSet, schema);
                }
            }
        }
        return null;
    }

    public List<RendezVous> findBySlotId(int slotId) throws SQLException {
        RendezVousSchema schema = resolveSchema();
        if (schema.slotIdColumn == null) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM `").append(schema.tableName).append("` WHERE `")
                .append(schema.slotIdColumn).append("` = ?");
        if (schema.createdAtColumn != null) {
            sql.append(" ORDER BY `").append(schema.createdAtColumn).append("` DESC, `").append(schema.idColumn).append("` DESC");
        } else {
            sql.append(" ORDER BY `").append(schema.idColumn).append("` DESC");
        }

        List<RendezVous> rendezVousList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            preparedStatement.setInt(1, slotId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rendezVousList.add(mapResultSetToRendezVous(resultSet, schema));
                }
            }
        }
        return rendezVousList;
    }

    public boolean update(RendezVous rendezVous) throws SQLException {
        RendezVousSchema schema = resolveSchema();

        List<String> assignments = new ArrayList<>();
        appendAssignment(assignments, schema.studentIdColumn);
        appendAssignment(assignments, schema.professorIdColumn);
        appendAssignment(assignments, schema.courseIdColumn);
        appendAssignment(assignments, schema.slotIdColumn);
        appendAssignment(assignments, schema.statutColumn);
        appendAssignment(assignments, schema.meetingTypeColumn);
        appendAssignment(assignments, schema.meetingLinkColumn);
        appendAssignment(assignments, schema.locationColumn);
        appendAssignment(assignments, schema.messageColumn);
        appendAssignment(assignments, schema.createdAtColumn);
        appendAssignment(assignments, schema.locationLabelColumn);
        appendAssignment(assignments, schema.locationLatColumn);
        appendAssignment(assignments, schema.locationLngColumn);
        appendAssignment(assignments, schema.refusalReasonColumn);
        appendAssignment(assignments, schema.coursePdfNameColumn);

        if (assignments.isEmpty()) {
            return false;
        }

        String sql = "UPDATE `" + schema.tableName + "` SET " + String.join(", ", assignments)
                + " WHERE `" + schema.idColumn + "` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            int index = 1;

            if (schema.studentIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getStudentId());
            }
            if (schema.professorIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getProfessorId());
            }
            if (schema.courseIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getCourseId());
            }
            if (schema.slotIdColumn != null) {
                setNullableInteger(preparedStatement, index++, rendezVous.getSlotId());
            }
            if (schema.statutColumn != null) {
                preparedStatement.setString(index++, rendezVous.getStatut());
            }
            if (schema.meetingTypeColumn != null) {
                preparedStatement.setString(index++, rendezVous.getMeetingType());
            }
            if (schema.meetingLinkColumn != null) {
                preparedStatement.setString(index++, rendezVous.getMeetingLink());
            }
            if (schema.locationColumn != null) {
                preparedStatement.setString(index++, rendezVous.getLocation());
            }
            if (schema.messageColumn != null) {
                preparedStatement.setString(index++, rendezVous.getMessage());
            }
            if (schema.createdAtColumn != null) {
                setNullableTimestamp(preparedStatement, index++, rendezVous.getCreatedAt());
            }
            if (schema.locationLabelColumn != null) {
                preparedStatement.setString(index++, rendezVous.getLocationLabel());
            }
            if (schema.locationLatColumn != null) {
                setNullableFloat(preparedStatement, index++, rendezVous.getLocationLat());
            }
            if (schema.locationLngColumn != null) {
                setNullableFloat(preparedStatement, index++, rendezVous.getLocationLng());
            }
            if (schema.refusalReasonColumn != null) {
                preparedStatement.setString(index++, rendezVous.getRefusalReason());
            }
            if (schema.coursePdfNameColumn != null) {
                preparedStatement.setString(index++, rendezVous.getCoursePdfName());
            }

            preparedStatement.setInt(index, rendezVous.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        RendezVousSchema schema = resolveSchema();
        String sql = "DELETE FROM `" + schema.tableName + "` WHERE `" + schema.idColumn + "` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private RendezVous mapResultSetToRendezVous(ResultSet resultSet, RendezVousSchema schema) {
        RendezVous rendezVous = new RendezVous();

        rendezVous.setId(getNullableInteger(resultSet, schema.idColumn, "id", "rdv_id"));
        rendezVous.setStudentId(getNullableInteger(resultSet, schema.studentIdColumn, "student_id", "etudiant_id", "eleve_id", "user_id"));
        rendezVous.setProfessorId(getNullableInteger(resultSet, schema.professorIdColumn, "professor_id", "prof_id", "professeur_id", "teacher_id", "instructor_id"));
        rendezVous.setCourseId(getNullableInteger(resultSet, schema.courseIdColumn, "course_id", "cours_id", "module_id"));
        rendezVous.setSlotId(getNullableInteger(resultSet, schema.slotIdColumn, "slot_id", "availability_slot_id", "availability_id", "creneau_id"));

        rendezVous.setStatut(getNullableString(resultSet, schema.statutColumn, "statut", "status", "etat"));
        rendezVous.setMeetingType(getNullableString(resultSet, schema.meetingTypeColumn, "meeting_type", "type_reunion", "type_meeting", "meeting_mode"));
        rendezVous.setMeetingLink(getNullableString(resultSet, schema.meetingLinkColumn, "meeting_link", "lien_reunion", "meet_link", "meeting_url", "link"));
        rendezVous.setLocation(getNullableString(resultSet, schema.locationColumn, "location", "lieu", "place"));
        rendezVous.setMessage(getNullableString(resultSet, schema.messageColumn, "message", "note", "notes", "description"));
        rendezVous.setLocationLabel(getNullableString(resultSet, schema.locationLabelColumn, "location_label", "location_name", "address_label", "adresse", "address"));
        rendezVous.setLocationLat(getNullableFloat(resultSet, schema.locationLatColumn, "location_lat", "lat", "latitude"));
        rendezVous.setLocationLng(getNullableFloat(resultSet, schema.locationLngColumn, "location_lng", "lng", "lon", "longitude"));
        rendezVous.setRefusalReason(getNullableString(resultSet, schema.refusalReasonColumn, "refusal_reason", "motif_refus", "reject_reason", "reason"));
        rendezVous.setCoursePdfName(getNullableString(resultSet, schema.coursePdfNameColumn, "course_pdf_name", "pdf_name", "course_file", "document_name"));

        Timestamp createdAt = getNullableTimestamp(resultSet, schema.createdAtColumn, "created_at", "createdon", "date_creation");
        if (createdAt != null) {
            rendezVous.setCreatedAt(createdAt.toLocalDateTime());
        }

        return rendezVous;
    }

    private void appendColumn(StringBuilder columns, StringBuilder placeholders, String column) {
        if (column == null || column.isBlank()) {
            return;
        }
        if (columns.length() > 0) {
            columns.append(", ");
            placeholders.append(", ");
        }
        columns.append("`").append(column).append("`");
        placeholders.append("?");
    }

    private void appendAssignment(List<String> assignments, String column) {
        if (column != null && !column.isBlank()) {
            assignments.add("`" + column + "` = ?");
        }
    }

    private Integer getNullableInteger(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        for (String candidate : buildCandidates(primaryColumn, fallbackColumns)) {
            try {
                int value = resultSet.getInt(candidate);
                return resultSet.wasNull() ? null : value;
            } catch (SQLException ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }

    private Float getNullableFloat(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        for (String candidate : buildCandidates(primaryColumn, fallbackColumns)) {
            try {
                float value = resultSet.getFloat(candidate);
                return resultSet.wasNull() ? null : value;
            } catch (SQLException ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }

    private String getNullableString(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        for (String candidate : buildCandidates(primaryColumn, fallbackColumns)) {
            try {
                return resultSet.getString(candidate);
            } catch (SQLException ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }

    private Timestamp getNullableTimestamp(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        for (String candidate : buildCandidates(primaryColumn, fallbackColumns)) {
            try {
                return resultSet.getTimestamp(candidate);
            } catch (SQLException ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }

    private List<String> buildCandidates(String primaryColumn, String... fallbackColumns) {
        List<String> candidates = new ArrayList<>();
        if (primaryColumn != null && !primaryColumn.isBlank()) {
            candidates.add(primaryColumn);
        }
        if (fallbackColumns == null) {
            return candidates;
        }
        for (String fallbackColumn : fallbackColumns) {
            if (fallbackColumn == null || fallbackColumn.isBlank() || containsIgnoreCase(candidates, fallbackColumn)) {
                continue;
            }
            candidates.add(fallbackColumn);
        }
        return candidates;
    }

    private boolean containsIgnoreCase(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private void setNullableInteger(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private void setNullableFloat(PreparedStatement preparedStatement, int index, Float value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.FLOAT);
        } else {
            preparedStatement.setFloat(index, value);
        }
    }

    private void setNullableTimestamp(PreparedStatement preparedStatement, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private RendezVousSchema resolveSchema() throws SQLException {
        String tableName = findExistingTable("rendez_vous", "rendezvous", "rdv");
        if (tableName == null) {
            throw new SQLException("Rendez-vous table not found (expected rendez_vous/rendezvous/rdv).");
        }

        String idColumn = findExistingColumn(tableName, "id", "rdv_id");
        if (idColumn == null) {
            throw new SQLException("Primary key column not found for table " + tableName + " (expected id/rdv_id).");
        }

        String studentIdColumn = findExistingColumn(
                tableName,
                "student_id",
                "etudiant_id",
                "eleve_id",
                "user_id",
                "id_student",
                "id_etudiant"
        );
        String professorIdColumn = findExistingColumn(
                tableName,
                "professor_id",
                "prof_id",
                "professeur_id",
                "teacher_id",
                "instructor_id",
                "id_professor",
                "id_professeur"
        );
        String courseIdColumn = findExistingColumn(
                tableName,
                "course_id",
                "cours_id",
                "module_id",
                "id_course",
                "id_cours"
        );
        String slotIdColumn = findExistingColumn(
                tableName,
                "slot_id",
                "availability_slot_id",
                "availability_id",
                "creneau_id",
                "id_slot"
        );
        String statutColumn = findExistingColumn(tableName, "statut", "status", "etat", "state");
        String meetingTypeColumn = findExistingColumn(tableName, "meeting_type", "type_reunion", "type_meeting", "meeting_mode", "mode");
        String meetingLinkColumn = findExistingColumn(tableName, "meeting_link", "lien_reunion", "meet_link", "meeting_url", "link");
        String locationColumn = findExistingColumn(tableName, "location", "lieu", "place", "adresse", "address");
        String messageColumn = findExistingColumn(tableName, "message", "note", "notes", "description");
        String createdAtColumn = findExistingColumn(tableName, "created_at", "createdon", "date_creation");
        String locationLabelColumn = findExistingColumn(tableName, "location_label", "location_name", "address_label", "adresse_label");
        String locationLatColumn = findExistingColumn(tableName, "location_lat", "lat", "latitude");
        String locationLngColumn = findExistingColumn(tableName, "location_lng", "lng", "lon", "longitude");
        String refusalReasonColumn = findExistingColumn(tableName, "refusal_reason", "motif_refus", "reject_reason", "reason");
        String coursePdfNameColumn = findExistingColumn(tableName, "course_pdf_name", "pdf_name", "course_file", "document_name");

        return new RendezVousSchema(
                tableName,
                idColumn,
                studentIdColumn,
                professorIdColumn,
                courseIdColumn,
                slotIdColumn,
                statutColumn,
                meetingTypeColumn,
                meetingLinkColumn,
                locationColumn,
                messageColumn,
                createdAtColumn,
                locationLabelColumn,
                locationLatColumn,
                locationLngColumn,
                refusalReasonColumn,
                coursePdfNameColumn
        );
    }

    private String findExistingTable(String... candidates) throws SQLException {
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (tableExists(catalog, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tableExists(String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private String findExistingColumn(String tableName, String... candidates) throws SQLException {
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (columnExists(catalog, tableName, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean columnExists(String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName, columnName)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                catalog,
                null,
                tableName.toUpperCase(Locale.ROOT),
                columnName.toUpperCase(Locale.ROOT))) {
            return resultSet.next();
        }
    }

    private static class RendezVousSchema {
        private final String tableName;
        private final String idColumn;
        private final String studentIdColumn;
        private final String professorIdColumn;
        private final String courseIdColumn;
        private final String slotIdColumn;
        private final String statutColumn;
        private final String meetingTypeColumn;
        private final String meetingLinkColumn;
        private final String locationColumn;
        private final String messageColumn;
        private final String createdAtColumn;
        private final String locationLabelColumn;
        private final String locationLatColumn;
        private final String locationLngColumn;
        private final String refusalReasonColumn;
        private final String coursePdfNameColumn;

        private RendezVousSchema(
                String tableName,
                String idColumn,
                String studentIdColumn,
                String professorIdColumn,
                String courseIdColumn,
                String slotIdColumn,
                String statutColumn,
                String meetingTypeColumn,
                String meetingLinkColumn,
                String locationColumn,
                String messageColumn,
                String createdAtColumn,
                String locationLabelColumn,
                String locationLatColumn,
                String locationLngColumn,
                String refusalReasonColumn,
                String coursePdfNameColumn) {
            this.tableName = tableName;
            this.idColumn = idColumn;
            this.studentIdColumn = studentIdColumn;
            this.professorIdColumn = professorIdColumn;
            this.courseIdColumn = courseIdColumn;
            this.slotIdColumn = slotIdColumn;
            this.statutColumn = statutColumn;
            this.meetingTypeColumn = meetingTypeColumn;
            this.meetingLinkColumn = meetingLinkColumn;
            this.locationColumn = locationColumn;
            this.messageColumn = messageColumn;
            this.createdAtColumn = createdAtColumn;
            this.locationLabelColumn = locationLabelColumn;
            this.locationLatColumn = locationLatColumn;
            this.locationLngColumn = locationLngColumn;
            this.refusalReasonColumn = refusalReasonColumn;
            this.coursePdfNameColumn = coursePdfNameColumn;
        }
    }
}
