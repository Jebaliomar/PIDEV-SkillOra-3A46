package tn.esprit.services;

import tn.esprit.entities.AdminReservationRow;
import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReservationDashboardService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final ReservationService reservationService = new ReservationService();
    private final EventService eventService = new EventService();
    private final SalleService salleService = new SalleService();

    public List<AdminReservationRow> loadRows() throws SQLException {
        Map<Integer, Event> eventMap = new HashMap<>();
        for (Event event : eventService.getAll()) {
            if (event.getId() != null) {
                eventMap.put(event.getId(), event);
            }
        }

        Map<Integer, Salle> salleMap = new HashMap<>();
        for (Salle salle : salleService.getAll()) {
            if (salle.getId() != null) {
                salleMap.put(salle.getId(), salle);
            }
        }

        List<AdminReservationRow> rows = new ArrayList<>();
        for (Reservation reservation : reservationService.getAll()) {
            AdminReservationRow row = new AdminReservationRow();
            row.setReservationId(reservation.getId());
            row.setEventId(reservation.getEventId());
            row.setSalleId(reservation.getSalleId());
            row.setFirstName(safe(reservation.getPrenom()));
            row.setLastName(safe(reservation.getNom()));
            row.setPhone(safe(reservation.getTelephone()));
            row.setSeats(safe(reservation.getNombrePlaces()));
            row.setSeatsCount(extractSeatsCount(reservation.getNombrePlaces()));
            row.setAddress(safe(reservation.getAdresse()));
            row.setReservationDate(reservation.getDateReservation());
            row.setStatus(buildStatus(reservation.getDateReservation()));

            Event event = reservation.getEventId() == null ? null : eventMap.get(reservation.getEventId());
            Salle salle = reservation.getSalleId() == null ? null : salleMap.get(reservation.getSalleId());
            row.setEventTitle(event == null ? "Unknown Event" : safe(event.getTitle()));
            row.setSalleName(salle == null ? "Unknown Salle" : safe(salle.getName()));
            rows.add(row);
        }

        rows.sort(Comparator.comparing(AdminReservationRow::getReservationDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    public int totalPlaces(List<AdminReservationRow> rows) {
        return rows.stream().mapToInt(AdminReservationRow::getSeatsCount).sum();
    }

    public Map<String, Integer> placesByEvent(List<AdminReservationRow> rows) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        for (AdminReservationRow row : rows) {
            stats.merge(row.getEventTitle(), row.getSeatsCount(), Integer::sum);
        }
        return stats;
    }

    public int extractSeatsCount(String raw) {
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

    private String buildStatus(LocalDateTime dateTime) {
        return dateTime == null ? "No date" : "Linked";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
