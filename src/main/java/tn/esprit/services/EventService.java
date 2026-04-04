package tn.esprit.services;

import tn.esprit.entities.Event;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EventService {

    private final Connection connection;

    public EventService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Event event) throws SQLException {
        String sql = "INSERT INTO event (title, description, start_date, end_date, event_type, price_type, image, salle_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, event.getTitle());
            preparedStatement.setString(2, event.getDescription());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(event.getStartDate()));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(event.getEndDate()));
            preparedStatement.setString(5, event.getEventType());
            preparedStatement.setString(6, event.getPriceType());
            preparedStatement.setString(7, event.getImage());

            if (event.getSalleId() == null) {
                preparedStatement.setNull(8, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(8, event.getSalleId());
            }

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    event.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Event> getAll() throws SQLException {
        String sql = "SELECT * FROM event";
        List<Event> events = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                events.add(mapResultSetToEvent(resultSet));
            }
        }

        return events;
    }

    public Event getById(int id) throws SQLException {
        String sql = "SELECT * FROM event WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToEvent(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(Event event) throws SQLException {
        String sql = "UPDATE event SET title = ?, description = ?, start_date = ?, end_date = ?, event_type = ?, "
                + "price_type = ?, image = ?, salle_id = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, event.getTitle());
            preparedStatement.setString(2, event.getDescription());
            preparedStatement.setTimestamp(3, Timestamp.valueOf(event.getStartDate()));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(event.getEndDate()));
            preparedStatement.setString(5, event.getEventType());
            preparedStatement.setString(6, event.getPriceType());
            preparedStatement.setString(7, event.getImage());

            if (event.getSalleId() == null) {
                preparedStatement.setNull(8, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(8, event.getSalleId());
            }

            preparedStatement.setInt(9, event.getId());

            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM event WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Event mapResultSetToEvent(ResultSet resultSet) throws SQLException {
        Event event = new Event();
        event.setId(resultSet.getInt("id"));
        event.setTitle(resultSet.getString("title"));
        event.setDescription(resultSet.getString("description"));

        Timestamp startDate = resultSet.getTimestamp("start_date");
        if (startDate != null) {
            event.setStartDate(startDate.toLocalDateTime());
        }

        Timestamp endDate = resultSet.getTimestamp("end_date");
        if (endDate != null) {
            event.setEndDate(endDate.toLocalDateTime());
        }

        event.setEventType(resultSet.getString("event_type"));
        event.setPriceType(resultSet.getString("price_type"));
        event.setImage(resultSet.getString("image"));

        int salleId = resultSet.getInt("salle_id");
        if (resultSet.wasNull()) {
            event.setSalleId(null);
        } else {
            event.setSalleId(salleId);
        }

        return event;
    }
}
