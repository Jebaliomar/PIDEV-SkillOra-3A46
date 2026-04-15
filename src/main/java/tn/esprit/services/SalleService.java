package tn.esprit.services;

import tn.esprit.entities.Salle;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SalleService {

    private final Connection connection;

    public SalleService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Salle salle) throws SQLException {
        String sql = "INSERT INTO salle (name, image_3d, max_participants, duration, equipment, location, event_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, salle.getName());
            preparedStatement.setString(2, salle.getImage3d());
            preparedStatement.setInt(3, salle.getMaxParticipants());
            preparedStatement.setInt(4, salle.getDuration());
            preparedStatement.setString(5, salle.getEquipment());
            preparedStatement.setString(6, salle.getLocation());

            if (salle.getEventId() == null) {
                preparedStatement.setNull(7, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(7, salle.getEventId());
            }

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    salle.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Salle> getAll() throws SQLException {
        String sql = "SELECT * FROM salle ORDER BY id DESC";
        List<Salle> salles = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                salles.add(mapResultSetToSalle(resultSet));
            }
        }

        return salles;
    }

    public Salle getById(int id) throws SQLException {
        String sql = "SELECT * FROM salle WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToSalle(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(Salle salle) throws SQLException {
        String sql = "UPDATE salle SET name = ?, image_3d = ?, max_participants = ?, duration = ?, equipment = ?, "
                + "location = ?, event_id = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, salle.getName());
            preparedStatement.setString(2, salle.getImage3d());
            preparedStatement.setInt(3, salle.getMaxParticipants());
            preparedStatement.setInt(4, salle.getDuration());
            preparedStatement.setString(5, salle.getEquipment());
            preparedStatement.setString(6, salle.getLocation());

            if (salle.getEventId() == null) {
                preparedStatement.setNull(7, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(7, salle.getEventId());
            }

            preparedStatement.setInt(8, salle.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM salle WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Salle mapResultSetToSalle(ResultSet resultSet) throws SQLException {
        Salle salle = new Salle();
        salle.setId(resultSet.getInt("id"));
        salle.setName(resultSet.getString("name"));
        salle.setImage3d(resultSet.getString("image_3d"));
        salle.setMaxParticipants(resultSet.getInt("max_participants"));
        salle.setDuration(resultSet.getInt("duration"));
        salle.setEquipment(resultSet.getString("equipment"));
        salle.setLocation(resultSet.getString("location"));

        int eventId = resultSet.getInt("event_id");
        if (resultSet.wasNull()) {
            salle.setEventId(null);
        } else {
            salle.setEventId(eventId);
        }

        return salle;
    }
}
