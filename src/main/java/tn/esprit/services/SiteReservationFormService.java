package tn.esprit.services;

import tn.esprit.entities.Event;
import tn.esprit.entities.Salle;
import tn.esprit.entities.SiteReservationFormData;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SiteReservationFormService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public SiteReservationFormData buildFormData(Event event, Salle salle, List<Integer> selectedSeats) {
        SiteReservationFormData data = new SiteReservationFormData();
        data.setEventId(event == null ? null : event.getId());
        data.setSalleId(salle == null ? null : salle.getId());
        data.setEventTitle(event == null ? "" : safe(event.getTitle()));
        data.setSalleName(salle == null ? "" : safe(salle.getName()));
        data.setAddress(salle == null ? "" : safe(salle.getLocation()));
        data.setMaxSeats(salle == null || salle.getMaxParticipants() == null ? 0 : Math.max(0, salle.getMaxParticipants()));
        data.setReservationDate(LocalDateTime.now());

        List<Integer> safeSeats = selectedSeats == null ? new ArrayList<>() : new ArrayList<>(selectedSeats);
        safeSeats.sort(Integer::compareTo);
        data.setSelectedSeats(safeSeats);
        data.setSeatsDisplay(safeSeats.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return data;
    }

    public String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_FORMATTER.format(dateTime);
    }

    public String buildMaxSeatsHelper(Integer maxSeats) {
        int value = maxSeats == null ? 0 : Math.max(0, maxSeats);
        return "Max seats: " + value;
    }

    public String buildAddressHelper() {
        return "Address is locked to the salle location.";
    }

    public String buildDateHelper() {
        return "Set automatically to the current time.";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
