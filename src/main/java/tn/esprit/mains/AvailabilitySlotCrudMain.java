package tn.esprit.mains;

import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.services.AvailabilitySlotService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class
AvailabilitySlotCrudMain {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        AvailabilitySlotService availabilitySlotService = new AvailabilitySlotService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addAvailabilitySlot(scanner, availabilitySlotService);
                    case "2" -> showAllAvailabilitySlots(availabilitySlotService);
                    case "3" -> showAvailabilitySlotById(scanner, availabilitySlotService);
                    case "4" -> updateAvailabilitySlot(scanner, availabilitySlotService);
                    case "5" -> deleteAvailabilitySlot(scanner, availabilitySlotService);
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

    private static void printMenu() {
        System.out.println("\n===== Gestion des Availability Slots =====");
        System.out.println("1. Ajouter un availability slot");
        System.out.println("2. Afficher tous les availability slots");
        System.out.println("3. Rechercher un availability slot par id");
        System.out.println("4. Modifier un availability slot");
        System.out.println("5. Supprimer un availability slot");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addAvailabilitySlot(Scanner scanner, AvailabilitySlotService availabilitySlotService) throws SQLException {
        AvailabilitySlot slot = readAvailabilitySlotData(scanner, new AvailabilitySlot());
        availabilitySlotService.add(slot);
        System.out.println("Availability slot ajoute avec succes. ID = " + slot.getId());
    }

    private static void showAllAvailabilitySlots(AvailabilitySlotService availabilitySlotService) throws SQLException {
        List<AvailabilitySlot> slots = availabilitySlotService.getAll();
        if (slots.isEmpty()) {
            System.out.println("Aucun availability slot trouve.");
            return;
        }

        for (AvailabilitySlot slot : slots) {
            System.out.println(slot);
        }
    }

    private static void showAvailabilitySlotById(Scanner scanner, AvailabilitySlotService availabilitySlotService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'availability slot : ");
        AvailabilitySlot slot = availabilitySlotService.getById(id);

        if (slot == null) {
            System.out.println("Availability slot introuvable.");
        } else {
            System.out.println(slot);
        }
    }

    private static void updateAvailabilitySlot(Scanner scanner, AvailabilitySlotService availabilitySlotService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'availability slot a modifier : ");
        AvailabilitySlot existingSlot = availabilitySlotService.getById(id);

        if (existingSlot == null) {
            System.out.println("Availability slot introuvable.");
            return;
        }

        AvailabilitySlot updatedSlot = readAvailabilitySlotData(scanner, existingSlot);
        updatedSlot.setId(id);

        if (availabilitySlotService.update(updatedSlot)) {
            System.out.println("Availability slot modifie avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deleteAvailabilitySlot(Scanner scanner, AvailabilitySlotService availabilitySlotService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'availability slot a supprimer : ");

        if (availabilitySlotService.delete(id)) {
            System.out.println("Availability slot supprime avec succes.");
        } else {
            System.out.println("Aucun availability slot supprime.");
        }
    }

    private static AvailabilitySlot readAvailabilitySlotData(Scanner scanner, AvailabilitySlot slot) {
        slot.setProfessorId(readRequiredInt(scanner, "Professor ID : "));

        slot.setStartAt(readDateTime(scanner, "Start at (yyyy-MM-dd HH:mm) : "));
        slot.setEndAt(readDateTime(scanner, "End at (yyyy-MM-dd HH:mm) : "));
        slot.setIsBooked(readBoolean(scanner, "Is booked (true/false) : "));

        System.out.print("Location label : ");
        slot.setLocationLabel(emptyToNull(scanner.nextLine()));

        System.out.print("Location lat (laisser vide pour null) : ");
        slot.setLocationLat(readOptionalFloat(scanner.nextLine()));

        System.out.print("Location lng (laisser vide pour null) : ");
        slot.setLocationLng(readOptionalFloat(scanner.nextLine()));

        System.out.print("Created at (yyyy-MM-dd HH:mm, laisser vide pour maintenant) : ");
        String createdAtInput = scanner.nextLine().trim();
        slot.setCreatedAt(createdAtInput.isEmpty() ? LocalDateTime.now() : parseDateTime(createdAtInput));

        return slot;
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

    private static Integer readRequiredInt(Scanner scanner, String message) {
        return readInt(scanner, message);
    }

    private static Boolean readBoolean(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim().toLowerCase();
            if ("true".equals(input) || "false".equals(input)) {
                return Boolean.parseBoolean(input);
            }
            System.out.println("Valeur booleenne invalide. Utilisez true ou false.");
        }
    }

    private static LocalDateTime readDateTime(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return parseDateTime(input);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format invalide. Utilisez yyyy-MM-dd HH:mm");
        }
    }

    private static Integer readOptionalInteger(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur numerique invalide.");
        }
    }

    private static Float readOptionalFloat(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Float.parseFloat(trimmed);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valeur decimale invalide.");
        }
    }

    private static String emptyToNull(String input) {
        String trimmed = input.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
