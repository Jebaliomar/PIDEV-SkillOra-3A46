package tn.esprit.mains;

import tn.esprit.entities.Question;
import tn.esprit.services.QuestionService;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class QuestionCrudMain {

    public static void main(String[] args) {

        QuestionService questionService = new QuestionService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {

            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> addQuestion(scanner, questionService);
                    case "2" -> showAllQuestions(questionService);
                    case "3" -> showQuestionById(scanner, questionService);
                    case "4" -> updateQuestion(scanner, questionService);
                    case "5" -> deleteQuestion(scanner, questionService);

                    case "6" -> sortMenu(scanner, questionService);
                    case "7" -> searchByKeyword(scanner, questionService);
                    case "8" -> filterByType(scanner, questionService);
                    case "9" -> filterByEvaluationId(scanner, questionService);
                    case "10" -> paginationMenu(scanner, questionService);
                    case "11" -> showStatistics(questionService);

                    case "12" -> showTopQuestions(scanner, questionService);
                    case "13" -> showRepartitionParType(questionService);
                    case "14" -> verifierCoherenceEvaluation(scanner, questionService);

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
        System.out.println("\n===== Gestion des Questions =====");
        System.out.println("1. Ajouter une question");
        System.out.println("2. Afficher toutes les questions");
        System.out.println("3. Rechercher une question par id");
        System.out.println("4. Modifier une question");
        System.out.println("5. Supprimer une question");
        System.out.println("6. Trier les questions");
        System.out.println("7. Rechercher par mot-clé");
        System.out.println("8. Filtrer par type");
        System.out.println("9. Filtrer par evaluation ID");
        System.out.println("10. Pagination");
        System.out.println("11. Statistiques");
        System.out.println("12. Top questions");
        System.out.println("13. Répartition des types");
        System.out.println("14. Vérification cohérence Evaluation ↔ Questions");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addQuestion(Scanner scanner, QuestionService questionService) throws SQLException {
        Question q = readQuestionData(scanner, new Question());
        questionService.ajouter(q);
    }

    private static void showAllQuestions(QuestionService questionService) throws SQLException {
        List<Question> questions = questionService.recuperer();
        afficherListe(questions);
    }

    private static void showQuestionById(Scanner scanner, QuestionService questionService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la question : ");
        Question q = questionService.chercherParId(id);

        if (q == null)
            System.out.println("Question introuvable.");
        else
            System.out.println(q);
    }

    private static void updateQuestion(Scanner scanner, QuestionService questionService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la question à modifier : ");
        Question existing = questionService.chercherParId(id);

        if (existing == null) {
            System.out.println("Question introuvable.");
            return;
        }

        Question updated = readQuestionData(scanner, existing);
        updated.setId(id);

        questionService.modifier(updated);
    }

    private static void deleteQuestion(Scanner scanner, QuestionService questionService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id de la question à supprimer : ");

        Question q = new Question();
        q.setId(id);

        questionService.supprimer(q);
    }

    private static void sortMenu(Scanner scanner, QuestionService questionService) throws SQLException {
        System.out.println("\n===== Tri =====");
        System.out.println("1. Trier par contenu ASC");
        System.out.println("2. Trier par contenu DESC");
        System.out.println("3. Trier par score ASC");
        System.out.println("4. Trier par score DESC");
        System.out.println("5. Trier par type ASC");
        System.out.println("6. Trier par type DESC");
        System.out.print("Votre choix : ");

        String choix = scanner.nextLine().trim();
        List<Question> questions;

        switch (choix) {
            case "1" -> questions = questionService.trierParContenuAsc();
            case "2" -> questions = questionService.trierParContenuDesc();
            case "3" -> questions = questionService.trierParScoreAsc();
            case "4" -> questions = questionService.trierParScoreDesc();
            case "5" -> questions = questionService.trierParTypeAsc();
            case "6" -> questions = questionService.trierParTypeDesc();
            default -> {
                System.out.println("Choix invalide.");
                return;
            }
        }

        afficherListe(questions);
    }

    private static void searchByKeyword(Scanner scanner, QuestionService questionService) throws SQLException {
        System.out.print("Entrer le mot-clé : ");
        String motCle = scanner.nextLine().trim();

        List<Question> questions = questionService.rechercherParMotCle(motCle);
        afficherListe(questions);
    }

    private static void filterByType(Scanner scanner, QuestionService questionService) throws SQLException {
        System.out.print("Entrer le type : ");
        String type = scanner.nextLine().trim();

        List<Question> questions = questionService.filtrerParType(type);
        afficherListe(questions);
    }

    private static void filterByEvaluationId(Scanner scanner, QuestionService questionService) throws SQLException {
        int evaluationId = readInt(scanner, "Entrer l'evaluation ID : ");
        List<Question> questions = questionService.filtrerParEvaluationId(evaluationId);
        afficherListe(questions);
    }

    private static void paginationMenu(Scanner scanner, QuestionService questionService) throws SQLException {
        int page = readInt(scanner, "Entrer le numéro de page : ");
        int taille = readInt(scanner, "Entrer la taille de page : ");

        List<Question> questions = questionService.recupererParPage(page, taille);
        afficherListe(questions);
    }

    private static void showStatistics(QuestionService questionService) throws SQLException {
        int total = questionService.countQuestions();
        double moyenne = questionService.moyenneScores();

        System.out.println("\n===== Statistiques =====");
        System.out.println("Nombre total de questions : " + total);
        System.out.println("Moyenne des scores : " + moyenne);
    }

    private static void showTopQuestions(Scanner scanner, QuestionService questionService) throws SQLException {
        int limite = readInt(scanner, "Entrer le nombre de top questions à afficher : ");
        List<Question> questions = questionService.topQuestions(limite);

        System.out.println("\n===== Top Questions =====");
        afficherListe(questions);
    }

    private static void showRepartitionParType(QuestionService questionService) throws SQLException {
        Map<String, Integer> repartition = questionService.repartitionParType();

        System.out.println("\n===== Répartition des types =====");

        if (repartition.isEmpty()) {
            System.out.println("Aucune donnée trouvée.");
            return;
        }

        for (Map.Entry<String, Integer> entry : repartition.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    private static void verifierCoherenceEvaluation(Scanner scanner, QuestionService questionService) throws SQLException {
        int evaluationId = readInt(scanner, "Entrer l'evaluation ID : ");

        int sommeQuestions = questionService.sommeScoresQuestionsParEvaluation(evaluationId);
        Integer totalEvaluation = questionService.totalScoreEvaluation(evaluationId);
        boolean coherence = questionService.verifierCoherenceEvaluationQuestions(evaluationId);

        System.out.println("\n===== Vérification cohérence =====");
        System.out.println("Evaluation ID : " + evaluationId);
        System.out.println("Somme des scores des questions : " + sommeQuestions);
        System.out.println("Total score de l'évaluation : " + totalEvaluation);
        System.out.println("Cohérence : " + (coherence ? "OUI" : "NON"));
    }

    private static void afficherListe(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            System.out.println("Aucune question trouvée.");
            return;
        }

        for (Question q : questions) {
            System.out.println(q);
        }
    }

    private static Question readQuestionData(Scanner scanner, Question question) {
        System.out.print("Contenu : ");
        question.setContent(scanner.nextLine().trim());

        System.out.print("Explication : ");
        question.setExplanation(scanner.nextLine().trim());

        System.out.print("Type : ");
        question.setType(scanner.nextLine().trim());

        question.setScore(readInt(scanner, "Score : "));
        question.setEvaluationId(readInt(scanner, "Evaluation ID : "));

        return question;
    }

    private static int readInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Valeur numérique invalide.");
            }
        }
    }
}