package tn.esprit.services;

import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.tools.LocalModelServer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteSalleDetailService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");

    private final ReservationService reservationService;

    public SiteSalleDetailService() {
        this.reservationService = new ReservationService();
    }

    public List<Reservation> loadReservationsForSalle(Integer salleId) {
        if (salleId == null) {
            return List.of();
        }

        try {
            List<Reservation> matches = new ArrayList<>();
            for (Reservation reservation : reservationService.getAll()) {
                if (salleId.equals(reservation.getSalleId())) {
                    matches.add(reservation);
                }
            }
            return matches;
        } catch (SQLException exception) {
            return List.of();
        }
    }

    public Set<Integer> buildReservedSeatIndexes(Salle salle, List<Reservation> reservations) {
        int capacity = salle == null || salle.getMaxParticipants() == null ? 0 : Math.max(0, salle.getMaxParticipants());
        Set<Integer> reserved = new LinkedHashSet<>();

        for (Reservation reservation : reservations) {
            reserved.addAll(extractReservedSeatNumbers(reservation, capacity));
        }
        return reserved;
    }

    public SeatMatrixSpec buildSeatMatrix(int capacity) {
        int normalizedCapacity = Math.max(0, capacity);
        if (normalizedCapacity == 0) {
            return new SeatMatrixSpec(1, 1, 0);
        }

        int columns = (int) Math.ceil(Math.sqrt(normalizedCapacity));
        int rows = (int) Math.ceil((double) normalizedCapacity / columns);
        return new SeatMatrixSpec(rows, columns, normalizedCapacity);
    }

    public String resolveModelViewerUrl(String modelPath) throws IOException {
        if (modelPath == null || modelPath.isBlank()) {
            return null;
        }

        File file = resolveFile(modelPath.trim());
        if (file == null || !file.exists()) {
            return null;
        }

        return LocalModelServer.registerModel(file.toPath());
    }

    private Set<Integer> extractReservedSeatNumbers(Reservation reservation, int capacity) {
        Set<Integer> seats = new LinkedHashSet<>();
        if (reservation == null || capacity <= 0) {
            return seats;
        }

        String raw = reservation.getNombrePlaces();
        if (raw == null || raw.isBlank()) {
            return seats;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        while (matcher.find()) {
            try {
                int seatNumber = Integer.parseInt(matcher.group(1));
                if (seatNumber >= 1 && seatNumber <= capacity) {
                    seats.add(seatNumber);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return seats;
    }

    private File resolveFile(String path) {
        File direct = new File(path);
        if (direct.exists()) {
            return direct;
        }

        File fromHome = new File(System.getProperty("user.home"), path);
        if (fromHome.exists()) {
            return fromHome;
        }

        File fromDownloads = new File(new File(System.getProperty("user.home"), "Downloads"), new File(path).getName());
        if (fromDownloads.exists()) {
            return fromDownloads;
        }

        return null;
    }

    public record SeatMatrixSpec(int rows, int columns, int capacity) {
    }
}
