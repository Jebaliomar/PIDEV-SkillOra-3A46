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

                    case "6" -> sortMenu(scanner, evaluationService);
                    case "7" -> searchByKeyword(scanner, evaluationService);
                    case "8" -> filterByType(scanner, evaluationService);
                    case "9" -> paginationMenu(scanner, evaluationService);
                    case "10" -> showStatistics(evaluationService);
                    case "11" -> showRecentEvaluations(scanner, evaluationService);

                    case "0" -> {
                        running = false;
                        System.out.println("Application fermée.");
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
        System.out.println("6. Trier les evaluations");
        System.out.println("7. Rechercher par mot-cle");
        System.out.println("8. Filtrer par type");
        System.out.println("9. Pagination");
        System.out.println("10. Statistiques");
        System.out.println("11. Afficher les evaluations recentes");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addEvaluation(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        Evaluation evaluation = readEvaluationData(scanner, new Evaluation());
        evaluationService.ajouter(evaluation);
    }

    private static void showAllEvaluations(EvaluationService evaluationService) throws SQLException {
        List<Evaluation> evaluations = evaluationService.recuperer();
        afficherListe(evaluations);
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

    private static void sortMenu(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        System.out.println("\n===== Tri =====");
        System.out.println("1. Trier par titre ASC");
        System.out.println("2. Trier par titre DESC");
        System.out.println("3. Trier par score ASC");
        System.out.println("4. Trier par score DESC");
        System.out.println("5. Trier par date ASC");
        System.out.println("6. Trier par date DESC");
        System.out.print("Votre choix : ");

        String choix = scanner.nextLine().trim();
        List<Evaluation> evaluations;

        switch (choix) {
            case "1" -> evaluations = evaluationService.trierParTitreAsc();
            case "2" -> evaluations = evaluationService.trierParTitreDesc();
            case "3" -> evaluations = evaluationService.trierParScoreAsc();
            case "4" -> evaluations = evaluationService.trierParScoreDesc();
            case "5" -> evaluations = evaluationService.trierParDateAsc();
            case "6" -> evaluations = evaluationService.trierParDateDesc();
            default -> {
                System.out.println("Choix invalide.");
                return;
            }
        }

        afficherListe(evaluations);
    }

    private static void searchByKeyword(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        System.out.print("Entrer le mot-cle : ");
        String motCle = scanner.nextLine().trim();

        List<Evaluation> evaluations = evaluationService.rechercherParMotCle(motCle);
        afficherListe(evaluations);
    }

    private static void filterByType(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        System.out.print("Entrer le type : ");
        String type = scanner.nextLine().trim();

        List<Evaluation> evaluations = evaluationService.filtrerParType(type);
        afficherListe(evaluations);
    }

    private static void paginationMenu(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        int page = readInt(scanner, "Entrer le numero de page : ");
        int taille = readInt(scanner, "Entrer la taille de page : ");

        List<Evaluation> evaluations = evaluationService.recupererParPage(page, taille);
        afficherListe(evaluations);
    }

    private static void showStatistics(EvaluationService evaluationService) throws SQLException {
        int total = evaluationService.countEvaluations();
        double moyenne = evaluationService.moyenneScores();

        System.out.println("\n===== Statistiques =====");
        System.out.println("Nombre total d'evaluations : " + total);
        System.out.println("Moyenne des scores : " + moyenne);
    }

    private static void showRecentEvaluations(Scanner scanner, EvaluationService evaluationService) throws SQLException {
        int limite = readInt(scanner, "Entrer le nombre d'evaluations recentes a afficher : ");
        List<Evaluation> evaluations = evaluationService.recupererRecentes(limite);
        afficherListe(evaluations);
    }

    private static void afficherListe(List<Evaluation> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            System.out.println("Aucune evaluation trouvee.");
            return;
        }

        for (Evaluation evaluation : evaluations) {
            System.out.println(evaluation);
        }
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