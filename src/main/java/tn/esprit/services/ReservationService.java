package tn.esprit.services;

import tn.esprit.entities.Reservation;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReservationService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final Connection connection;

    public ReservationService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Reservation reservation) throws SQLException {
        String sql = "INSERT INTO reservation (nom, prenom, telephone, adresse, nombre_places, date_reservation, event_id, salle_id, user_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, reservation.getNom());
            preparedStatement.setString(2, reservation.getPrenom());
            preparedStatement.setString(3, reservation.getTelephone());
            preparedStatement.setString(4, reservation.getAdresse());
            preparedStatement.setString(5, reservation.getNombrePlaces());
            preparedStatement.setTimestamp(6, Timestamp.valueOf(reservation.getDateReservation()));
            preparedStatement.setInt(7, reservation.getEventId());
            preparedStatement.setInt(8, reservation.getSalleId());

            if (reservation.getUserId() == null) {
                preparedStatement.setNull(9, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(9, reservation.getUserId());
            }

            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reservation.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Reservation> getAll() throws SQLException {
        String sql = "SELECT * FROM reservation";
        List<Reservation> reservations = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                reservations.add(mapResultSetToReservation(resultSet));
            }
        }

        return reservations;
    }

    public Reservation getById(int id) throws SQLException {
        String sql = "SELECT * FROM reservation WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToReservation(resultSet);
                }
            }
        }

        return null;
    }

    public boolean update(Reservation reservation) throws SQLException {
        String sql = "UPDATE reservation SET nom = ?, prenom = ?, telephone = ?, adresse = ?, nombre_places = ?, "
                + "date_reservation = ?, event_id = ?, salle_id = ?, user_id = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, reservation.getNom());
            preparedStatement.setString(2, reservation.getPrenom());
            preparedStatement.setString(3, reservation.getTelephone());
            preparedStatement.setString(4, reservation.getAdresse());
            preparedStatement.setString(5, reservation.getNombrePlaces());
            preparedStatement.setTimestamp(6, Timestamp.valueOf(reservation.getDateReservation()));
            preparedStatement.setInt(7, reservation.getEventId());
            preparedStatement.setInt(8, reservation.getSalleId());

            if (reservation.getUserId() == null) {
                preparedStatement.setNull(9, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(9, reservation.getUserId());
            }

            preparedStatement.setInt(10, reservation.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM reservation WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public Map<String, Integer> getReservedSeatsByEventAndSalle() throws SQLException {
        String sql = "SELECT event_id, salle_id, nombre_places FROM reservation";
        Map<String, Integer> totals = new HashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int eventId = resultSet.getInt("event_id");
                if (resultSet.wasNull()) {
                    continue;
                }

                int salleId = resultSet.getInt("salle_id");
                if (resultSet.wasNull()) {
                    continue;
                }

                int seatsCount = extractSeatsCount(resultSet.getString("nombre_places"));
                totals.merge(buildEventSalleKey(eventId, salleId), seatsCount, Integer::sum);
            }
        }

        return totals;
    }

    public String buildEventSalleKey(Integer eventId, Integer salleId) {
        return eventId + ":" + salleId;
    }

    private int extractSeatsCount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }

        if (raw.contains(",")) {
            return (int) java.util.Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .count();
        }

        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private Reservation mapResultSetToReservation(ResultSet resultSet) throws SQLException {
        Reservation reservation = new Reservation();
        reservation.setId(resultSet.getInt("id"));
        reservation.setNom(resultSet.getString("nom"));
        reservation.setPrenom(resultSet.getString("prenom"));
        reservation.setTelephone(resultSet.getString("telephone"));
        reservation.setAdresse(resultSet.getString("adresse"));
        reservation.setNombrePlaces(resultSet.getString("nombre_places"));

        Timestamp dateReservation = resultSet.getTimestamp("date_reservation");
        if (dateReservation != null) {
            reservation.setDateReservation(dateReservation.toLocalDateTime());
        }

        reservation.setEventId(resultSet.getInt("event_id"));
        reservation.setSalleId(resultSet.getInt("salle_id"));

        int userId = resultSet.getInt("user_id");
        if (resultSet.wasNull()) {
            reservation.setUserId(null);
        } else {
            reservation.setUserId(userId);
        }

        return reservation;
    }
}
