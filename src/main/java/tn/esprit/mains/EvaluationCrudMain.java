package tn.esprit.mains;

import tn.esprit.entities.Evaluation;
import tn.esprit.services.EvaluationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class EvaluationCrudMain {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        EvaluationService evaluationService = new EvaluationService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addEvaluation(scanner, evaluationService);
                    case "2" -> showAllEvaluations(evaluationService);
                    case "3" -> showEvaluationById(scanner, evaluationService);
                    case "4" -> updateEvaluation(scanner, evaluationService);
                    case "5" -> deleteEvaluation(scanner, evaluationService);
                    case "0" -> {
                        running = false;
                        System.out.println("Application fermee.");
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur SQL : " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur de saisie : " + e.getMessage());
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n===== Gestion des Evaluations =====");
        System.out.println("1. Ajouter une evaluation");
        System.out.println("2. Afficher toutes les evaluations");
        System.out.println("3. Rechercher une evaluation par id");
        System.out.println("4. Modifier une evaluation");
        System.out.println("5. Supprimer une evaluation");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addEvaluation(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        Evaluation evaluation = readEvaluationData(scanner, new Evaluation());
        evaluationService.ajouter(evaluation);
        System.out.println("Evaluation ajoutee avec succes.");
    }

    private static void showAllEvaluations(EvaluationService evaluationService) throws SQLException {
        List<Evaluation> evaluations = evaluationService.recuperer();

        if (evaluations.isEmpty()) {
            System.out.println("Aucune evaluation trouvee.");
            return;
        }

        for (Evaluation evaluation : evaluations) {
            System.out.println(evaluation);
        }
    }

    private static void showEvaluationById(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'evaluation : ");
        Evaluation evaluation = evaluationService.chercherParId(id);

        if (evaluation == null) {
            System.out.println("Evaluation introuvable.");
        } else {
            System.out.println(evaluation);
        }
    }

    private static void updateEvaluation(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'evaluation a modifier : ");
        Evaluation existingEvaluation = evaluationService.chercherParId(id);

        if (existingEvaluation == null) {
            System.out.println("Evaluation introuvable.");
            return;
        }

        Evaluation updatedEvaluation = readEvaluationData(scanner, existingEvaluation);
        updatedEvaluation.setId(id);

        evaluationService.modifier(updatedEvaluation);
    }

    private static void deleteEvaluation(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de l'evaluation a supprimer : ");

        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);

        evaluationService.supprimer(evaluation);
    }

    private static Evaluation readEvaluationData(Scanner scanner, Evaluation evaluation) {
        System.out.print("Titre : ");
        evaluation.setTitle(scanner.nextLine().trim());

        System.out.print("Description : ");
        evaluation.setDescription(scanner.nextLine().trim());

        System.out.print("Type : ");
        evaluation.setType(scanner.nextLine().trim());

        evaluation.setDuration(readInt(scanner, "Duree : "));
        evaluation.setTotalScore(readInt(scanner, "Score total : "));

        System.out.print("Date creation (yyyy-MM-dd HH:mm) ou vide pour maintenant : ");
        String createdAtInput = scanner.nextLine().trim();
        if (createdAtInput.isEmpty()) {
            evaluation.setCreatedAt(LocalDateTime.now());
        } else {
            evaluation.setCreatedAt(parseDateTime(createdAtInput));
        }

        System.out.print("Chemin DOCX : ");
        evaluation.setDocxPath(scanner.nextLine().trim());

        System.out.print("Chemin PDF : ");
        evaluation.setPdfPath(scanner.nextLine().trim());

        return evaluation;
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

    private static LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format invalide. Utilisez yyyy-MM-dd HH:mm");
        }
    }
}