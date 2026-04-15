package tn.esprit.mains;

import tn.esprit.entities.Event;
import tn.esprit.entities.Reservation;
import tn.esprit.entities.Salle;
import tn.esprit.services.EventService;
import tn.esprit.services.ReservationService;
import tn.esprit.services.SalleService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class EventMain {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        EventService eventService = new EventService();
        SalleService salleService = new SalleService();
        ReservationService reservationService = new ReservationService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleEventMenu(scanner, eventService);
                    case "2" -> handleSalleMenu(scanner, salleService);
                    case "3" -> handleReservationMenu(scanner, reservationService);
                    case "0" -> {
                        running = false;
                        System.out.println("Application fermee.");
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur SQL: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur de saisie: " + e.getMessage());
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n===== Gestion Console =====");
        System.out.println("1. CRUD Event");
        System.out.println("2. CRUD Salle");
        System.out.println("3. CRUD Reservation");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void handleEventMenu(Scanner scanner, EventService eventService) throws SQLException {
        boolean back = false;

        while (!back) {
            printEventMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addEvent(scanner, eventService);
                case "2" -> showAllEvents(eventService);
                case "3" -> showEventById(scanner, eventService);
                case "4" -> updateEvent(scanner, eventService);
                case "5" -> deleteEvent(scanner, eventService);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleSalleMenu(Scanner scanner, SalleService salleService) throws SQLException {
        boolean back = false;

        while (!back) {
            printSalleMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addSalle(scanner, salleService);
                case "2" -> showAllSalles(salleService);
                case "3" -> showSalleById(scanner, salleService);
                case "4" -> updateSalle(scanner, salleService);
                case "5" -> deleteSalle(scanner, salleService);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleReservationMenu(Scanner scanner, ReservationService reservationService) throws SQLException {
        boolean back = false;

        while (!back) {
            printReservationMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addReservation(scanner, reservationService);
                case "2" -> showAllReservations(reservationService);
                case "3" -> showReservationById(scanner, reservationService);
                case "4" -> updateReservation(scanner, reservationService);
                case "5" -> deleteReservation(scanner, reservationService);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void printEventMenu() {
        System.out.println("\n===== Gestion des Events =====");
        System.out.println("1. Ajouter un event");
        System.out.println("2. Afficher tous les events");
        System.out.println("3. Rechercher un event par id");
        System.out.println("4. Modifier un event");
        System.out.println("5. Supprimer un event");
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
    }

    private static void printSalleMenu() {
        System.out.println("\n===== Gestion des Salles =====");
        System.out.println("1. Ajouter une salle");
        System.out.println("2. Afficher toutes les salles");
        System.out.println("3. Rechercher une salle par id");
        System.out.println("4. Modifier une salle");
        System.out.println("5. Supprimer une salle");
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
    }

    private static void printReservationMenu() {
        System.out.println("\n===== Gestion des Reservations =====");
        System.out.println("1. Ajouter une reservation");
        System.out.println("2. Afficher toutes les reservations");
        System.out.println("3. Rechercher une reservation par id");
        System.out.println("4. Modifier une reservation");
        System.out.println("5. Supprimer une reservation");
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
    }

    private static void addEvent(Scanner scanner, EventService eventService) throws SQLException {
        Event event = readEventData(scanner, new Event());
        eventService.add(event);
        System.out.println("Event ajoute avec succes. ID = " + event.getId());
    }

    private static void showAllEvents(EventService eventService) throws SQLException {
        List<Event> events = eventService.getAll();
        if (events.isEmpty()) {
            System.out.println("Aucun event trouve.");
            return;
        }

        for (Event event : events) {
            System.out.println(event);
        }
    }

    private static void showEventById(Scanner scanner, EventService eventService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'event : ");
        Event event = eventService.getById(id);

        if (event == null) {
            System.out.println("Event introuvable.");
        } else {
            System.out.println(event);
        }
    }

    private static void updateEvent(Scanner scanner, EventService eventService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'event a modifier : ");
        Event existingEvent = eventService.getById(id);

        if (existingEvent == null) {
            System.out.println("Event introuvable.");
            return;
        }

        Event updatedEvent = readEventData(scanner, existingEvent);
        updatedEvent.setId(id);

        if (eventService.update(updatedEvent)) {
            System.out.println("Event modifie avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deleteEvent(Scanner scanner, EventService eventService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'event a supprimer : ");

        if (eventService.delete(id)) {
            System.out.println("Event supprime avec succes.");
        } else {
            System.out.println("Aucun event supprime.");
        }
    }

    private static Event readEventData(Scanner scanner, Event event) {
        System.out.print("Titre : ");
        event.setTitle(scanner.nextLine().trim());

        System.out.print("Description : ");
        event.setDescription(scanner.nextLine().trim());

        event.setStartDate(readDateTime(scanner, "Date debut (yyyy-MM-dd HH:mm) : "));
        event.setEndDate(readDateTime(scanner, "Date fin (yyyy-MM-dd HH:mm) : "));

        System.out.print("Type d'event : ");
        event.setEventType(scanner.nextLine().trim());

        System.out.print("Type de prix : ");
        event.setPriceType(scanner.nextLine().trim());

        System.out.print("Image : ");
        event.setImage(scanner.nextLine().trim());

        System.out.print("Salle ID (laisser vide pour null) : ");
        String salleInput = scanner.nextLine().trim();
        event.setSalleId(salleInput.isEmpty() ? null : Integer.parseInt(salleInput));

        return event;
    }

    private static void addSalle(Scanner scanner, SalleService salleService) throws SQLException {
        Salle salle = readSalleData(scanner, new Salle());
        salleService.add(salle);
        System.out.println("Salle ajoutee avec succes. ID = " + salle.getId());
    }

    private static void showAllSalles(SalleService salleService) throws SQLException {
        List<Salle> salles = salleService.getAll();
        if (salles.isEmpty()) {
            System.out.println("Aucune salle trouvee.");
            return;
        }

        for (Salle salle : salles) {
            System.out.println(salle);
        }
    }

    private static void showSalleById(Scanner scanner, SalleService salleService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la salle : ");
        Salle salle = salleService.getById(id);

        if (salle == null) {
            System.out.println("Salle introuvable.");
        } else {
            System.out.println(salle);
        }
    }

    private static void updateSalle(Scanner scanner, SalleService salleService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la salle a modifier : ");
        Salle existingSalle = salleService.getById(id);

        if (existingSalle == null) {
            System.out.println("Salle introuvable.");
            return;
        }

        Salle updatedSalle = readSalleData(scanner, existingSalle);
        updatedSalle.setId(id);

        if (salleService.update(updatedSalle)) {
            System.out.println("Salle modifiee avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deleteSalle(Scanner scanner, SalleService salleService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la salle a supprimer : ");

        if (salleService.delete(id)) {
            System.out.println("Salle supprimee avec succes.");
        } else {
            System.out.println("Aucune salle supprimee.");
        }
    }

    private static Salle readSalleData(Scanner scanner, Salle salle) {
        System.out.print("Nom : ");
        salle.setName(scanner.nextLine().trim());

        System.out.print("Image 3D : ");
        salle.setImage3d(scanner.nextLine().trim());

        salle.setMaxParticipants(readInt(scanner, "Nombre max participants : "));
        salle.setDuration(readInt(scanner, "Duree : "));

        System.out.print("Equipement : ");
        salle.setEquipment(scanner.nextLine().trim());

        System.out.print("Location : ");
        salle.setLocation(scanner.nextLine().trim());

        System.out.print("Event ID (laisser vide pour null) : ");
        String eventInput = scanner.nextLine().trim();
        salle.setEventId(eventInput.isEmpty() ? null : Integer.parseInt(eventInput));

        return salle;
    }

    private static void addReservation(Scanner scanner, ReservationService reservationService) throws SQLException {
        Reservation reservation = readReservationData(scanner, new Reservation());
        reservationService.add(reservation);
        System.out.println("Reservation ajoutee avec succes. ID = " + reservation.getId());
    }

    private static void showAllReservations(ReservationService reservationService) throws SQLException {
        List<Reservation> reservations = reservationService.getAll();
        if (reservations.isEmpty()) {
            System.out.println("Aucune reservation trouvee.");
            return;
        }

        for (Reservation reservation : reservations) {
            System.out.println(reservation);
        }
    }

    private static void showReservationById(Scanner scanner, ReservationService reservationService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la reservation : ");
        Reservation reservation = reservationService.getById(id);

        if (reservation == null) {
            System.out.println("Reservation introuvable.");
        } else {
            System.out.println(reservation);
        }
    }

    private static void updateReservation(Scanner scanner, ReservationService reservationService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la reservation a modifier : ");
        Reservation existingReservation = reservationService.getById(id);

        if (existingReservation == null) {
            System.out.println("Reservation introuvable.");
            return;
        }

        Reservation updatedReservation = readReservationData(scanner, existingReservation);
        updatedReservation.setId(id);

        if (reservationService.update(updatedReservation)) {
            System.out.println("Reservation modifiee avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deleteReservation(Scanner scanner, ReservationService reservationService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la reservation a supprimer : ");

        if (reservationService.delete(id)) {
            System.out.println("Reservation supprimee avec succes.");
        } else {
            System.out.println("Aucune reservation supprimee.");
        }
    }

    private static Reservation readReservationData(Scanner scanner, Reservation reservation) {
        System.out.print("Nom : ");
        reservation.setNom(scanner.nextLine().trim());

        System.out.print("Prenom : ");
        reservation.setPrenom(scanner.nextLine().trim());

        System.out.print("Telephone : ");
        reservation.setTelephone(scanner.nextLine().trim());

        System.out.print("Adresse : ");
        reservation.setAdresse(scanner.nextLine().trim());

        System.out.print("Nombre places : ");
        reservation.setNombrePlaces(scanner.nextLine().trim());

        reservation.setDateReservation(readDateTime(scanner, "Date reservation (yyyy-MM-dd HH:mm) : "));
        reservation.setEventId(readInt(scanner, "Event ID : "));
        reservation.setSalleId(readInt(scanner, "Salle ID : "));

        System.out.print("User ID (laisser vide pour null) : ");
        String userInput = scanner.nextLine().trim();
        reservation.setUserId(userInput.isEmpty() ? null : Integer.parseInt(userInput));

        return reservation;
    }

    private static int readInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Valeur numerique invalide.");
            }
        }
    }

    private static LocalDateTime readDateTime(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return LocalDateTime.parse(input, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Format invalide. Utilisez yyyy-MM-dd HH:mm");
            }
        }
    }
}
