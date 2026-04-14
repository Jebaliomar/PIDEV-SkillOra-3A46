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

public class AvailabilitySlotService {

    private final Connection connection;

    public AvailabilitySlotService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(AvailabilitySlot slot) throws SQLException {
        String sql = "INSERT INTO availability_slots (professor_id, start_at, end_at, is_booked, location_label, "
                + "location_lat, location_lng, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setNullableInteger(preparedStatement, 1, slot.getProfessorId());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(slot.getStartAt()));
            preparedStatement.setTimestamp(3, Timestamp.valueOf(slot.getEndAt()));
            setNullableBoolean(preparedStatement, 4, slot.getIsBooked());
            preparedStatement.setString(5, slot.getLocationLabel());
            setNullableFloat(preparedStatement, 6, slot.getLocationLat());
            setNullableFloat(preparedStatement, 7, slot.getLocationLng());
            setNullableTimestamp(preparedStatement, 8, slot.getCreatedAt());

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    slot.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<AvailabilitySlot> getAll() throws SQLException {
        String sql = "SELECT * FROM availability_slots ORDER BY start_at DESC";
        List<AvailabilitySlot> slots = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                slots.add(mapResultSetToSlot(resultSet));
            }
        }

        return slots;
    }

    public AvailabilitySlot getById(int id) throws SQLException {
        String sql = "SELECT * FROM availability_slots WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToSlot(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(AvailabilitySlot slot) throws SQLException {
        String sql = "UPDATE availability_slots SET professor_id = ?, start_at = ?, end_at = ?, is_booked = ?, "
                + "location_label = ?, location_lat = ?, location_lng = ?, created_at = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            setNullableInteger(preparedStatement, 1, slot.getProfessorId());
            preparedStatement.setTimestamp(2, Timestamp.valueOf(slot.getStartAt()));
            preparedStatement.setTimestamp(3, Timestamp.valueOf(slot.getEndAt()));
            setNullableBoolean(preparedStatement, 4, slot.getIsBooked());
            preparedStatement.setString(5, slot.getLocationLabel());
            setNullableFloat(preparedStatement, 6, slot.getLocationLat());
            setNullableFloat(preparedStatement, 7, slot.getLocationLng());
            setNullableTimestamp(preparedStatement, 8, slot.getCreatedAt());
            preparedStatement.setInt(9, slot.getId());

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
        String sql = "UPDATE availability_slots SET is_booked = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setBoolean(1, isBooked);
            preparedStatement.setInt(2, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private AvailabilitySlot mapResultSetToSlot(ResultSet resultSet) throws SQLException {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setId(resultSet.getInt("id"));

        int professorId = resultSet.getInt("professor_id");
        slot.setProfessorId(resultSet.wasNull() ? null : professorId);

        Timestamp startAt = resultSet.getTimestamp("start_at");
        if (startAt != null) {
            slot.setStartAt(startAt.toLocalDateTime());
        }

        Timestamp endAt = resultSet.getTimestamp("end_at");
        if (endAt != null) {
            slot.setEndAt(endAt.toLocalDateTime());
        }

        boolean isBooked = resultSet.getBoolean("is_booked");
        slot.setIsBooked(resultSet.wasNull() ? null : isBooked);
        slot.setLocationLabel(resultSet.getString("location_label"));

        float locationLat = resultSet.getFloat("location_lat");
        slot.setLocationLat(resultSet.wasNull() ? null : locationLat);

        float locationLng = resultSet.getFloat("location_lng");
        slot.setLocationLng(resultSet.wasNull() ? null : locationLng);

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            slot.setCreatedAt(createdAt.toLocalDateTime());
        }

        return slot;
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
