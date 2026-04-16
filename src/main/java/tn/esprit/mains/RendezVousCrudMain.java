package tn.esprit.mains;

import tn.esprit.entities.RendezVous;
import tn.esprit.services.RendezVousService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class RendezVousCrudMain {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        RendezVousService rendezVousService = new RendezVousService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printRendezVousMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addRendezVous(scanner, rendezVousService);
                    case "2" -> showAllRendezVous(rendezVousService);
                    case "3" -> showRendezVousById(scanner, rendezVousService);
                    case "4" -> updateRendezVous(scanner, rendezVousService);
                    case "5" -> deleteRendezVous(scanner, rendezVousService);
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

    private static void printRendezVousMenu() {
        System.out.println("\n===== Gestion des RendezVous =====");
        System.out.println("1. Ajouter un rendez-vous");
        System.out.println("2. Afficher tous les rendez-vous");
        System.out.println("3. Rechercher un rendez-vous par id");
        System.out.println("4. Modifier un rendez-vous");
        System.out.println("5. Supprimer un rendez-vous");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addRendezVous(Scanner scanner, RendezVousService rendezVousService) throws SQLException {
        RendezVous rendezVous = readRendezVousData(scanner, new RendezVous());
        rendezVousService.add(rendezVous);
        System.out.println("Rendez-vous ajoute avec succes. ID = " + rendezVous.getId());
    }

    private static void showAllRendezVous(RendezVousService rendezVousService) throws SQLException {
        List<RendezVous> rendezVousList = rendezVousService.getAll();
        if (rendezVousList.isEmpty()) {
            System.out.println("Aucun rendez-vous trouve.");
            return;
        }

        for (RendezVous rendezVous : rendezVousList) {
            System.out.println(rendezVous);
        }
    }

    private static void showRendezVousById(Scanner scanner, RendezVousService rendezVousService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du rendez-vous : ");
        RendezVous rendezVous = rendezVousService.getById(id);

        if (rendezVous == null) {
            System.out.println("Rendez-vous introuvable.");
        } else {
            System.out.println(rendezVous);
        }
    }

    private static void updateRendezVous(Scanner scanner, RendezVousService rendezVousService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du rendez-vous a modifier : ");
        RendezVous existingRendezVous = rendezVousService.getById(id);

        if (existingRendezVous == null) {
            System.out.println("Rendez-vous introuvable.");
            return;
        }

        RendezVous updatedRendezVous = readRendezVousData(scanner, existingRendezVous);
        updatedRendezVous.setId(id);

        if (rendezVousService.update(updatedRendezVous)) {
            System.out.println("Rendez-vous modifie avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deleteRendezVous(Scanner scanner, RendezVousService rendezVousService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du rendez-vous a supprimer : ");

        if (rendezVousService.delete(id)) {
            System.out.println("Rendez-vous supprime avec succes.");
        } else {
            System.out.println("Aucun rendez-vous supprime.");
        }
    }

    private static RendezVous readRendezVousData(Scanner scanner, RendezVous rendezVous) {
        rendezVous.setStudentId(readRequiredInt(scanner, "Student ID : "));
        rendezVous.setProfessorId(readRequiredInt(scanner, "Professor ID : "));

        System.out.print("Course ID (laisser vide pour null) : ");
        rendezVous.setCourseId(readOptionalInteger(scanner.nextLine()));

        rendezVous.setSlotId(readRequiredInt(scanner, "Slot ID : "));

        System.out.print("Statut (laisser vide pour en_attente) : ");
        String statut = scanner.nextLine().trim();
        rendezVous.setStatut(statut.isEmpty() ? "en_attente" : statut);

        System.out.print("Meeting type (laisser vide pour en_ligne) : ");
        String meetingType = scanner.nextLine().trim();
        rendezVous.setMeetingType(meetingType.isEmpty() ? "en_ligne" : meetingType);

        System.out.print("Meeting link : ");
        rendezVous.setMeetingLink(emptyToNull(scanner.nextLine()));

        System.out.print("Location : ");
        rendezVous.setLocation(emptyToNull(scanner.nextLine()));

        System.out.print("Message : ");
        rendezVous.setMessage(emptyToNull(scanner.nextLine()));

        System.out.print("Created at (yyyy-MM-dd HH:mm, laisser vide pour maintenant) : ");
        String createdAtInput = scanner.nextLine().trim();
        rendezVous.setCreatedAt(createdAtInput.isEmpty() ? LocalDateTime.now() : parseDateTime(createdAtInput));

        System.out.print("Location label : ");
        rendezVous.setLocationLabel(emptyToNull(scanner.nextLine()));

        System.out.print("Location lat (laisser vide pour null) : ");
        rendezVous.setLocationLat(readOptionalFloat(scanner.nextLine()));

        System.out.print("Location lng (laisser vide pour null) : ");
        rendezVous.setLocationLng(readOptionalFloat(scanner.nextLine()));

        System.out.print("Refusal reason : ");
        rendezVous.setRefusalReason(emptyToNull(scanner.nextLine()));

        System.out.print("Course PDF name : ");
        rendezVous.setCoursePdfName(emptyToNull(scanner.nextLine()));

        return rendezVous;
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
