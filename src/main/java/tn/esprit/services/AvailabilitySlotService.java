package tn.esprit.services;

import tn.esprit.entities.AvailabilitySlot;
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

public class AvailabilitySlotService {

    private final Connection connection;

    public AvailabilitySlotService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(AvailabilitySlot slot) throws SQLException {
        String professorColumn = findProfessorColumn();
        if (professorColumn == null) {
            throw new SQLException("No professor/user column found in availability_slots (expected professor_id/prof_id/teacher_id/user_id/id_professor/id_professeur/id_prof).");
        }
        String startColumn = requireExistingColumn("availability_slots", "start_at");
        String endColumn = requireExistingColumn("availability_slots", "end_at");
        String bookedColumn = findExistingColumn("availability_slots", "is_booked", "booked", "is_reserved", "reserved");
        String locationLabelColumn = findExistingColumn("availability_slots", "location_label", "location", "address");
        String locationLatColumn = findExistingColumn("availability_slots", "location_lat", "lat", "latitude");
        String locationLngColumn = findExistingColumn("availability_slots", "location_lng", "lng", "lon", "longitude");
        String createdAtColumn = findExistingColumn("availability_slots", "created_at", "createdon");

        StringBuilder columns = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();

        columns.append("`").append(professorColumn).append("`, `").append(startColumn).append("`, `").append(endColumn).append("`");
        placeholders.append("?, ?, ?");
        if (bookedColumn != null) {
            columns.append(", `").append(bookedColumn).append("`");
            placeholders.append(", ?");
        }
        if (locationLabelColumn != null) {
            columns.append(", `").append(locationLabelColumn).append("`");
            placeholders.append(", ?");
        }
        if (locationLatColumn != null) {
            columns.append(", `").append(locationLatColumn).append("`");
            placeholders.append(", ?");
        }
        if (locationLngColumn != null) {
            columns.append(", `").append(locationLngColumn).append("`");
            placeholders.append(", ?");
        }
        if (createdAtColumn != null) {
            columns.append(", `").append(createdAtColumn).append("`");
            placeholders.append(", ?");
        }

        String sql = "INSERT INTO availability_slots (" + columns + ") VALUES (" + placeholders + ")";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int index = 1;
            setNullableInteger(preparedStatement, index++, slot.getProfessorId());
            preparedStatement.setTimestamp(index++, Timestamp.valueOf(slot.getStartAt()));
            preparedStatement.setTimestamp(index++, Timestamp.valueOf(slot.getEndAt()));
            if (bookedColumn != null) {
                setNullableBoolean(preparedStatement, index++, slot.getIsBooked());
            }
            if (locationLabelColumn != null) {
                preparedStatement.setString(index++, slot.getLocationLabel());
            }
            if (locationLatColumn != null) {
                setNullableFloat(preparedStatement, index++, slot.getLocationLat());
            }
            if (locationLngColumn != null) {
                setNullableFloat(preparedStatement, index++, slot.getLocationLng());
            }
            if (createdAtColumn != null) {
                setNullableTimestamp(preparedStatement, index, slot.getCreatedAt());
            }

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    slot.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<AvailabilitySlot> getAll() throws SQLException {
        String startColumn = requireExistingColumn("availability_slots", "start_at");
        String sql = "SELECT * FROM availability_slots ORDER BY `" + startColumn + "` DESC";
        List<AvailabilitySlot> slots = new ArrayList<>();
        String professorColumn = findProfessorColumn();
        String endColumn = findExistingColumn("availability_slots", "end_at");
        String bookedColumn = findExistingColumn("availability_slots", "is_booked", "booked", "is_reserved", "reserved");
        String locationLabelColumn = findExistingColumn("availability_slots", "location_label", "location", "address");
        String locationLatColumn = findExistingColumn("availability_slots", "location_lat", "lat", "latitude");
        String locationLngColumn = findExistingColumn("availability_slots", "location_lng", "lng", "lon", "longitude");
        String createdAtColumn = findExistingColumn("availability_slots", "created_at", "createdon");

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                slots.add(mapResultSetToSlot(
                        resultSet,
                        professorColumn,
                        startColumn,
                        endColumn,
                        bookedColumn,
                        locationLabelColumn,
                        locationLatColumn,
                        locationLngColumn,
                        createdAtColumn
                ));
            }
        }

        return slots;
    }

    public List<AvailabilitySlot> getAllCreatedByProfessors() throws SQLException {
        String professorColumn = findProfessorColumn();
        String startColumn = requireExistingColumn("availability_slots", "start_at");
        String userRoleTable = findExistingTable("user_roles", "user_role", "users_roles");
        if (professorColumn == null || userRoleTable == null) {
            return getAll();
        }

        String userIdColumn = findExistingColumn(userRoleTable, "user_id", "userid", "id_user");
        String roleColumn = findExistingColumn(userRoleTable, "role", "name");
        if (userIdColumn == null || roleColumn == null) {
            return getAll();
        }

        String sql = "SELECT s.* FROM availability_slots s "
                + "WHERE EXISTS ("
                + "SELECT 1 FROM `" + userRoleTable + "` ur "
                + "WHERE ur.`" + userIdColumn + "` = s.`" + professorColumn + "` "
                + "AND ("
                + "LOWER(ur.`" + roleColumn + "`) = 'professor' "
                + "OR LOWER(ur.`" + roleColumn + "`) = 'teacher' "
                + "OR LOWER(ur.`" + roleColumn + "`) = 'instructor' "
                + "OR LOWER(ur.`" + roleColumn + "`) = 'prof' "
                + "OR LOWER(ur.`" + roleColumn + "`) LIKE 'role_prof%' "
                + "OR LOWER(ur.`" + roleColumn + "`) LIKE '%professor%' "
                + "OR LOWER(ur.`" + roleColumn + "`) LIKE '%teacher%' "
                + "OR LOWER(ur.`" + roleColumn + "`) LIKE '%instructor%'"
                + ")"
                + ") ORDER BY s.`" + startColumn + "` DESC";

        List<AvailabilitySlot> slots = new ArrayList<>();
        String endColumn = findExistingColumn("availability_slots", "end_at");
        String bookedColumn = findExistingColumn("availability_slots", "is_booked", "booked", "is_reserved", "reserved");
        String locationLabelColumn = findExistingColumn("availability_slots", "location_label", "location", "address");
        String locationLatColumn = findExistingColumn("availability_slots", "location_lat", "lat", "latitude");
        String locationLngColumn = findExistingColumn("availability_slots", "location_lng", "lng", "lon", "longitude");
        String createdAtColumn = findExistingColumn("availability_slots", "created_at", "createdon");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                slots.add(mapResultSetToSlot(
                        resultSet,
                        professorColumn,
                        startColumn,
                        endColumn,
                        bookedColumn,
                        locationLabelColumn,
                        locationLatColumn,
                        locationLngColumn,
                        createdAtColumn
                ));
            }
        }
        return slots;
    }

    public AvailabilitySlot getById(int id) throws SQLException {
        String sql = "SELECT * FROM availability_slots WHERE id = ?";
        String professorColumn = findProfessorColumn();
        String startColumn = requireExistingColumn("availability_slots", "start_at");
        String endColumn = findExistingColumn("availability_slots", "end_at");
        String bookedColumn = findExistingColumn("availability_slots", "is_booked", "booked", "is_reserved", "reserved");
        String locationLabelColumn = findExistingColumn("availability_slots", "location_label", "location", "address");
        String locationLatColumn = findExistingColumn("availability_slots", "location_lat", "lat", "latitude");
        String locationLngColumn = findExistingColumn("availability_slots", "location_lng", "lng", "lon", "longitude");
        String createdAtColumn = findExistingColumn("availability_slots", "created_at", "createdon");

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToSlot(
                            resultSet,
                            professorColumn,
                            startColumn,
                            endColumn,
                            bookedColumn,
                            locationLabelColumn,
                            locationLatColumn,
                            locationLngColumn,
                            createdAtColumn
                    );
                }
            }
        }

        return null;
    }

    public boolean update(AvailabilitySlot slot) throws SQLException {
        String startColumn = requireExistingColumn("availability_slots", "start_at");
        String endColumn = requireExistingColumn("availability_slots", "end_at");
        String bookedColumn = findExistingColumn("availability_slots", "is_booked", "booked", "is_reserved", "reserved");
        String locationLabelColumn = findExistingColumn("availability_slots", "location_label", "location", "address");
        String locationLatColumn = findExistingColumn("availability_slots", "location_lat", "lat", "latitude");
        String locationLngColumn = findExistingColumn("availability_slots", "location_lng", "lng", "lon", "longitude");
        String createdAtColumn = findExistingColumn("availability_slots", "created_at", "createdon");
        StringBuilder sql = new StringBuilder("UPDATE availability_slots SET ");
        List<String> assignments = new ArrayList<>();
        assignments.add("`" + startColumn + "` = ?");
        assignments.add("`" + endColumn + "` = ?");
        if (bookedColumn != null) {
            assignments.add("`" + bookedColumn + "` = ?");
        }
        if (locationLabelColumn != null) {
            assignments.add("`" + locationLabelColumn + "` = ?");
        }
        if (locationLatColumn != null) {
            assignments.add("`" + locationLatColumn + "` = ?");
        }
        if (locationLngColumn != null) {
            assignments.add("`" + locationLngColumn + "` = ?");
        }
        if (createdAtColumn != null) {
            assignments.add("`" + createdAtColumn + "` = ?");
        }
        sql.append(String.join(", ", assignments)).append(" WHERE id = ?");

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            preparedStatement.setTimestamp(index++, Timestamp.valueOf(slot.getStartAt()));
            preparedStatement.setTimestamp(index++, Timestamp.valueOf(slot.getEndAt()));
            if (bookedColumn != null) {
                setNullableBoolean(preparedStatement, index++, slot.getIsBooked());
            }
            if (locationLabelColumn != null) {
                preparedStatement.setString(index++, slot.getLocationLabel());
            }
            if (locationLatColumn != null) {
                setNullableFloat(preparedStatement, index++, slot.getLocationLat());
            }
            if (locationLngColumn != null) {
                setNullableFloat(preparedStatement, index++, slot.getLocationLng());
            }
            if (createdAtColumn != null) {
                setNullableTimestamp(preparedStatement, index++, slot.getCreatedAt());
            }
            preparedStatement.setInt(index, slot.getId());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM availability_slots WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean updateBookedStatus(int id, boolean isBooked) throws SQLException {
        String bookedColumn = findExistingColumn("availability_slots", "is_booked", "booked", "is_reserved", "reserved");
        if (bookedColumn == null) {
            return false;
        }
        String sql = "UPDATE availability_slots SET `" + bookedColumn + "` = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, isBooked);
            preparedStatement.setInt(2, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private AvailabilitySlot mapResultSetToSlot(
            ResultSet resultSet,
            String professorColumn,
            String startColumn,
            String endColumn,
            String bookedColumn,
            String locationLabelColumn,
            String locationLatColumn,
            String locationLngColumn,
            String createdAtColumn) throws SQLException {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(resultSet.getInt("id"));

        slot.setProfessorId(getNullableInteger(
                resultSet,
                professorColumn,
                "professor_id",
                "prof_id",
                "teacher_id",
                "user_id",
                "id_professor",
                "id_professeur",
                "id_prof"
        ));

        Timestamp startAt = getNullableTimestamp(resultSet, startColumn, "start_at");
        if (startAt != null) {
            slot.setStartAt(startAt.toLocalDateTime());
        }

        Timestamp endAt = getNullableTimestamp(resultSet, endColumn, "end_at");
        if (endAt != null) {
            slot.setEndAt(endAt.toLocalDateTime());
        }

        slot.setIsBooked(getNullableBoolean(resultSet, bookedColumn, "is_booked", "booked", "is_reserved", "reserved"));
        slot.setLocationLabel(getNullableString(resultSet, locationLabelColumn, "location_label", "location", "address"));
        slot.setLocationLat(getNullableFloat(resultSet, locationLatColumn, "location_lat", "lat", "latitude"));
        slot.setLocationLng(getNullableFloat(resultSet, locationLngColumn, "location_lng", "lng", "lon", "longitude"));

        Timestamp createdAt = getNullableTimestamp(resultSet, createdAtColumn, "created_at", "createdon");
        if (createdAt != null) {
            slot.setCreatedAt(createdAt.toLocalDateTime());
        }

        return slot;
    }

    private Integer getNullableInteger(ResultSet resultSet, String primaryColumn, String... fallbackColumns) throws SQLException {
        if (primaryColumn != null && !primaryColumn.isBlank()) {
            try {
                return readNullableInteger(resultSet, primaryColumn);
            } catch (SQLException ignored) {
                // Fallback to alternative names.
            }
        }
        for (String fallbackColumn : fallbackColumns) {
            if (fallbackColumn == null || fallbackColumn.isBlank() || fallbackColumn.equalsIgnoreCase(primaryColumn)) {
                continue;
            }
            try {
                return readNullableInteger(resultSet, fallbackColumn);
            } catch (SQLException ignored) {
                // Try the next candidate.
            }
        }
        return null;
    }

    private Integer readNullableInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private Boolean getNullableBoolean(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        List<String> candidates = new ArrayList<>();
        if (primaryColumn != null && !primaryColumn.isBlank()) {
            candidates.add(primaryColumn);
        }
        for (String fallbackColumn : fallbackColumns) {
            if (fallbackColumn != null && !fallbackColumn.isBlank() && !candidates.contains(fallbackColumn)) {
                candidates.add(fallbackColumn);
            }
        }
        for (String candidate : candidates) {
            try {
                boolean value = resultSet.getBoolean(candidate);
                return resultSet.wasNull() ? null : value;
            } catch (SQLException ignored) {
                // Try next candidate.
            }
        }
        return null;
    }

    private Float getNullableFloat(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        List<String> candidates = new ArrayList<>();
        if (primaryColumn != null && !primaryColumn.isBlank()) {
            candidates.add(primaryColumn);
        }
        for (String fallbackColumn : fallbackColumns) {
            if (fallbackColumn != null && !fallbackColumn.isBlank() && !candidates.contains(fallbackColumn)) {
                candidates.add(fallbackColumn);
            }
        }
        for (String candidate : candidates) {
            try {
                float value = resultSet.getFloat(candidate);
                return resultSet.wasNull() ? null : value;
            } catch (SQLException ignored) {
                // Try next candidate.
            }
        }
        return null;
    }

    private String getNullableString(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        List<String> candidates = new ArrayList<>();
        if (primaryColumn != null && !primaryColumn.isBlank()) {
            candidates.add(primaryColumn);
        }
        for (String fallbackColumn : fallbackColumns) {
            if (fallbackColumn != null && !fallbackColumn.isBlank() && !candidates.contains(fallbackColumn)) {
                candidates.add(fallbackColumn);
            }
        }
        for (String candidate : candidates) {
            try {
                return resultSet.getString(candidate);
            } catch (SQLException ignored) {
                // Try next candidate.
            }
        }
        return null;
    }

    private Timestamp getNullableTimestamp(ResultSet resultSet, String primaryColumn, String... fallbackColumns) {
        List<String> candidates = new ArrayList<>();
        if (primaryColumn != null && !primaryColumn.isBlank()) {
            candidates.add(primaryColumn);
        }
        for (String fallbackColumn : fallbackColumns) {
            if (fallbackColumn != null && !fallbackColumn.isBlank() && !candidates.contains(fallbackColumn)) {
                candidates.add(fallbackColumn);
            }
        }
        for (String candidate : candidates) {
            try {
                return resultSet.getTimestamp(candidate);
            } catch (SQLException ignored) {
                // Try next candidate.
            }
        }
        return null;
    }

    private String findProfessorColumn() throws SQLException {
        return findExistingColumn(
                "availability_slots",
                "professor_id",
                "prof_id",
                "teacher_id",
                "user_id",
                "id_professor",
                "id_professeur",
                "id_prof"
        );
    }

    private String requireExistingColumn(String tableName, String... candidates) throws SQLException {
        String column = findExistingColumn(tableName, candidates);
        if (column == null) {
            throw new SQLException("No compatible column found in " + tableName + " for " + String.join("/", candidates));
        }
        return column;
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

    private void setNullableInteger(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private void setNullableFloat(PreparedStatement preparedStatement, int index, Float value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.FLOAT);
        } else {
            preparedStatement.setFloat(index, value);
        }
    }

    private void setNullableBoolean(PreparedStatement preparedStatement, int index, Boolean value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.BOOLEAN);
        } else {
            preparedStatement.setBoolean(index, value);
        }
    }

    private void setNullableTimestamp(PreparedStatement preparedStatement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
