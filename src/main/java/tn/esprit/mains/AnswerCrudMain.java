package tn.esprit.mains;

import tn.esprit.entities.Answer;
import tn.esprit.services.AnswerService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class AnswerCrudMain {

    public static void main(String[] args) {

        AnswerService answerService = new AnswerService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {

            printMenu();
            String choice = scanner.nextLine().trim();

            try {

                switch (choice) {

                    case "1" -> addAnswer(scanner, answerService);
                    case "2" -> showAllAnswers(answerService);
                    case "3" -> showAnswerById(scanner, answerService);
                    case "4" -> updateAnswer(scanner, answerService);
                    case "5" -> deleteAnswer(scanner, answerService);
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

        System.out.println("\n===== Gestion des Answers =====");
        System.out.println("1. Ajouter une answer");
        System.out.println("2. Afficher toutes les answers");
        System.out.println("3. Rechercher une answer par id");
        System.out.println("4. Modifier une answer");
        System.out.println("5. Supprimer une answer");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addAnswer(Scanner scanner, AnswerService answerService) throws SQLException {

        Answer a = readAnswerData(scanner, new Answer());

        answerService.ajouter(a);

        System.out.println("Answer ajoutée avec succès.");
    }

    private static void showAllAnswers(AnswerService answerService) throws SQLException {

        List<Answer> answers = answerService.recuperer();

        if (answers.isEmpty()) {
            System.out.println("Aucune answer trouvée.");
            return;
        }

        for (Answer a : answers) {
            System.out.println(a);
        }
    }

    private static void showAnswerById(Scanner scanner, AnswerService answerService) throws SQLException {

        int id = readInt(scanner, "Entrer l'id de l'answer : ");

        Answer a = answerService.chercherParId(id);

        if (a == null)
            System.out.println("Answer introuvable.");
        else
            System.out.println(a);
    }

    private static void updateAnswer(Scanner scanner, AnswerService answerService) throws SQLException {

        int id = readInt(scanner, "Entrer l'id de l'answer à modifier : ");

        Answer existing = answerService.chercherParId(id);

        if (existing == null) {
            System.out.println("Answer introuvable.");
            return;
        }

        Answer updated = readAnswerData(scanner, existing);
        updated.setId(id);

        answerService.modifier(updated);
    }

    private static void deleteAnswer(Scanner scanner, AnswerService answerService) throws SQLException {

        int id = readInt(scanner, "Entrer l'id de l'answer à supprimer : ");

        Answer a = new Answer();
        a.setId(id);

        answerService.supprimer(a);
    }

    private static Answer readAnswerData(Scanner scanner, Answer answer) {

        System.out.print("Content : ");
        answer.setContent(scanner.nextLine().trim());

        System.out.print("Is Correct (true/false) : ");
        answer.setIsCorrect(Boolean.parseBoolean(scanner.nextLine().trim()));

        System.out.print("Role : ");
        answer.setRole(scanner.nextLine().trim());

        answer.setQuestionId(readInt(scanner, "Question ID : "));
        answer.setStudentId(readInt(scanner, "Student ID : "));

        answer.setCreatedAt(LocalDateTime.now());

        answer.setWebPlagiarismPercent(readInt(scanner, "Web Plagiarism % : "));
        answer.setAiSuspicionPercent(readInt(scanner, "AI Suspicion % : "));

        System.out.print("Web Sources : ");
        answer.setWebSources(scanner.nextLine().trim());

        answer.setPasteCount(readInt(scanner, "Paste Count : "));
        answer.setTabSwitchCount(readInt(scanner, "Tab Switch Count : "));

        answer.setLastIntegrityEventAt(LocalDateTime.now());
        answer.setLastPlagiarismCheckAt(LocalDateTime.now());

        return answer;
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