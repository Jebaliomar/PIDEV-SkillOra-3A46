package tn.esprit.services;

import tn.esprit.entities.RendezVous;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RendezVousService {

    private final Connection connection;

    public RendezVousService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(RendezVous rendezVous) throws SQLException {
        String sql = "INSERT INTO rendez_vous (student_id, professor_id, course_id, slot_id, statut, meeting_type, "
                + "meeting_link, location, message, created_at, location_label, location_lat, location_lng, "
                + "refusal_reason, course_pdf_name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(preparedStatement, rendezVous);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    rendezVous.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<RendezVous> getAll() throws SQLException {
        String sql = "SELECT * FROM rendez_vous ORDER BY created_at DESC, id DESC";
        List<RendezVous> rendezVousList = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                rendezVousList.add(mapResultSetToRendezVous(resultSet));
            }
        }

        return rendezVousList;
    }

    public RendezVous getById(int id) throws SQLException {
        String sql = "SELECT * FROM rendez_vous WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToRendezVous(resultSet);
                }
            }
        }

        return null;
    }

    public List<RendezVous> findBySlotId(int slotId) throws SQLException {
        String sql = "SELECT * FROM rendez_vous WHERE slot_id = ? ORDER BY created_at DESC, id DESC";
        List<RendezVous> rendezVousList = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, slotId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rendezVousList.add(mapResultSetToRendezVous(resultSet));
                }
            }
        }

        return rendezVousList;
    }

    public boolean update(RendezVous rendezVous) throws SQLException {
        String sql = "UPDATE rendez_vous SET student_id = ?, professor_id = ?, course_id = ?, slot_id = ?, "
                + "statut = ?, meeting_type = ?, meeting_link = ?, location = ?, message = ?, created_at = ?, "
                + "location_label = ?, location_lat = ?, location_lng = ?, refusal_reason = ?, "
                + "course_pdf_name = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            fillStatement(preparedStatement, rendezVous);
            preparedStatement.setInt(16, rendezVous.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM rendez_vous WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private void fillStatement(PreparedStatement preparedStatement, RendezVous rendezVous) throws SQLException {
        setNullableInteger(preparedStatement, 1, rendezVous.getStudentId());
        setNullableInteger(preparedStatement, 2, rendezVous.getProfessorId());
        setNullableInteger(preparedStatement, 3, rendezVous.getCourseId());
        setNullableInteger(preparedStatement, 4, rendezVous.getSlotId());
        preparedStatement.setString(5, rendezVous.getStatut());
        preparedStatement.setString(6, rendezVous.getMeetingType());
        preparedStatement.setString(7, rendezVous.getMeetingLink());
        preparedStatement.setString(8, rendezVous.getLocation());
        preparedStatement.setString(9, rendezVous.getMessage());
        setNullableTimestamp(preparedStatement, 10, rendezVous.getCreatedAt());
        preparedStatement.setString(11, rendezVous.getLocationLabel());
        setNullableFloat(preparedStatement, 12, rendezVous.getLocationLat());
        setNullableFloat(preparedStatement, 13, rendezVous.getLocationLng());
        preparedStatement.setString(14, rendezVous.getRefusalReason());
        preparedStatement.setString(15, rendezVous.getCoursePdfName());
    }

    private RendezVous mapResultSetToRendezVous(ResultSet resultSet) throws SQLException {
        RendezVous rendezVous = new RendezVous();
        rendezVous.setId(resultSet.getInt("id"));

        int studentId = resultSet.getInt("student_id");
        rendezVous.setStudentId(resultSet.wasNull() ? null : studentId);

        int professorId = resultSet.getInt("professor_id");
        rendezVous.setProfessorId(resultSet.wasNull() ? null : professorId);

        int courseId = resultSet.getInt("course_id");
        rendezVous.setCourseId(resultSet.wasNull() ? null : courseId);

        int slotId = resultSet.getInt("slot_id");
        rendezVous.setSlotId(resultSet.wasNull() ? null : slotId);

        rendezVous.setStatut(resultSet.getString("statut"));
        rendezVous.setMeetingType(resultSet.getString("meeting_type"));
        rendezVous.setMeetingLink(resultSet.getString("meeting_link"));
        rendezVous.setLocation(resultSet.getString("location"));
        rendezVous.setMessage(resultSet.getString("message"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            rendezVous.setCreatedAt(createdAt.toLocalDateTime());
        }

        rendezVous.setLocationLabel(resultSet.getString("location_label"));
        float locationLat = resultSet.getFloat("location_lat");
        rendezVous.setLocationLat(resultSet.wasNull() ? null : locationLat);

        float locationLng = resultSet.getFloat("location_lng");
        rendezVous.setLocationLng(resultSet.wasNull() ? null : locationLng);

        rendezVous.setRefusalReason(resultSet.getString("refusal_reason"));
        rendezVous.setCoursePdfName(resultSet.getString("course_pdf_name"));

        return rendezVous;
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

    private void setNullableTimestamp(PreparedStatement preparedStatement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
