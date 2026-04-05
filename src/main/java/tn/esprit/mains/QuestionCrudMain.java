package tn.esprit.mains;

import tn.esprit.entities.Question;
import tn.esprit.services.QuestionService;

import java.sql.SQLException;
import java.util.List;
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
                    case "0" -> {
                        running = false;
                        System.out.println("Application fermée.");
                    }

                    default -> System.out.println("Choix invalide.");
                }

            } catch (SQLException e) {
                System.out.println("Erreur SQL : " + e.getMessage());
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
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addQuestion(Scanner scanner, QuestionService questionService) throws SQLException {

        Question q = readQuestionData(scanner, new Question());
        questionService.ajouter(q);

        System.out.println("Question ajoutée avec succès.");
    }

    private static void showAllQuestions(QuestionService questionService) throws SQLException {

        List<Question> questions = questionService.recuperer();

        if (questions.isEmpty()) {
            System.out.println("Aucune question trouvée.");
            return;
        }

        for (Question q : questions) {
            System.out.println(q);
        }
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