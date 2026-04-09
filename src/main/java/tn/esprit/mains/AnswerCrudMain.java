package tn.esprit.mains;

import tn.esprit.entities.Answer;
import tn.esprit.services.AnswerService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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

                    case "6" -> sortMenu(scanner, answerService);
                    case "7" -> searchByKeyword(scanner, answerService);
                    case "8" -> filterMenu(scanner, answerService);
                    case "9" -> paginationMenu(scanner, answerService);
                    case "10" -> showStatistics(answerService);
                    case "11" -> showRecentAnswers(scanner, answerService);
                    case "12" -> showSuspiciousAnswers(scanner, answerService);
                    case "13" -> showTopSuspiciousAnswers(scanner, answerService);
                    case "14" -> showRepartitionParRole(answerService);
                    case "15" -> showRepartitionCorrectIncorrect(answerService);
                    case "16" -> verifierCoherenceIntegrite(scanner, answerService);

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
        System.out.println("\n===== Gestion des Answers =====");
        System.out.println("1. Ajouter une answer");
        System.out.println("2. Afficher toutes les answers");
        System.out.println("3. Rechercher une answer par id");
        System.out.println("4. Modifier une answer");
        System.out.println("5. Supprimer une answer");
        System.out.println("6. Trier les answers");
        System.out.println("7. Rechercher par mot-clé");
        System.out.println("8. Filtres");
        System.out.println("9. Pagination");
        System.out.println("10. Statistiques");
        System.out.println("11. Réponses récentes");
        System.out.println("12. Réponses suspectes");
        System.out.println("13. Top réponses suspectes");
        System.out.println("14. Répartition par rôle");
        System.out.println("15. Répartition correct / incorrect");
        System.out.println("16. Vérifier cohérence d'intégrité");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void addAnswer(Scanner scanner, AnswerService answerService) throws SQLException {
        Answer a = readAnswerData(scanner, new Answer());
        answerService.ajouter(a);
    }

    private static void showAllAnswers(AnswerService answerService) throws SQLException {
        List<Answer> answers = answerService.recuperer();
        afficherListe(answers);
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

    private static void sortMenu(Scanner scanner, AnswerService answerService) throws SQLException {
        System.out.println("\n===== Tri =====");
        System.out.println("1. Trier par date ASC");
        System.out.println("2. Trier par date DESC");
        System.out.println("3. Trier par plagiat DESC");
        System.out.println("4. Trier par suspicion IA DESC");
        System.out.print("Votre choix : ");

        String choix = scanner.nextLine().trim();
        List<Answer> answers;

        switch (choix) {
            case "1" -> answers = answerService.trierParDateAsc();
            case "2" -> answers = answerService.trierParDateDesc();
            case "3" -> answers = answerService.trierParPlagiatDesc();
            case "4" -> answers = answerService.trierParAiSuspicionDesc();
            default -> {
                System.out.println("Choix invalide.");
                return;
            }
        }

        afficherListe(answers);
    }

    private static void searchByKeyword(Scanner scanner, AnswerService answerService) throws SQLException {
        System.out.print("Entrer le mot-clé : ");
        String motCle = scanner.nextLine().trim();

        List<Answer> answers = answerService.rechercherParMotCle(motCle);
        afficherListe(answers);
    }

    private static void filterMenu(Scanner scanner, AnswerService answerService) throws SQLException {
        System.out.println("\n===== Filtres =====");
        System.out.println("1. Filtrer par question ID");
        System.out.println("2. Filtrer par student ID");
        System.out.println("3. Filtrer par rôle");
        System.out.println("4. Filtrer par correct / incorrect");
        System.out.print("Votre choix : ");

        String choix = scanner.nextLine().trim();
        List<Answer> answers;

        switch (choix) {
            case "1" -> {
                int questionId = readInt(scanner, "Question ID : ");
                answers = answerService.filtrerParQuestionId(questionId);
            }
            case "2" -> {
                int studentId = readInt(scanner, "Student ID : ");
                answers = answerService.filtrerParStudentId(studentId);
            }
            case "3" -> {
                System.out.print("Rôle : ");
                String role = scanner.nextLine().trim();
                answers = answerService.filtrerParRole(role);
            }
            case "4" -> {
                boolean correct = readBoolean(scanner, "Réponse correcte ? (true/false) : ");
                answers = answerService.filtrerParCorrect(correct);
            }
            default -> {
                System.out.println("Choix invalide.");
                return;
            }
        }

        afficherListe(answers);
    }

    private static void paginationMenu(Scanner scanner, AnswerService answerService) throws SQLException {
        int page = readInt(scanner, "Entrer le numéro de page : ");
        int taille = readInt(scanner, "Entrer la taille de page : ");

        List<Answer> answers = answerService.recupererParPage(page, taille);
        afficherListe(answers);
    }

    private static void showStatistics(AnswerService answerService) throws SQLException {
        System.out.println("\n===== Statistiques =====");
        System.out.println("Nombre total d'answers : " + answerService.countAnswers());
        System.out.println("Moyenne plagiat web : " + answerService.moyennePlagiatWeb());
        System.out.println("Moyenne suspicion IA : " + answerService.moyenneAiSuspicion());
        System.out.println("Nombre correctes : " + answerService.countCorrectes());
        System.out.println("Nombre incorrectes : " + answerService.countIncorrectes());
    }

    private static void showRecentAnswers(Scanner scanner, AnswerService answerService) throws SQLException {
        int limite = readInt(scanner, "Nombre d'answers récentes à afficher : ");
        List<Answer> answers = answerService.recupererRecentes(limite);
        afficherListe(answers);
    }

    private static void showSuspiciousAnswers(Scanner scanner, AnswerService answerService) throws SQLException {
        int seuilPlagiat = readInt(scanner, "Seuil plagiat web : ");
        int seuilAi = readInt(scanner, "Seuil suspicion IA : ");
        int seuilPaste = readInt(scanner, "Seuil paste count : ");
        int seuilTabSwitch = readInt(scanner, "Seuil tab switch count : ");

        List<Answer> answers = answerService.reponsesSuspectes(seuilPlagiat, seuilAi, seuilPaste, seuilTabSwitch);
        afficherListe(answers);
    }

    private static void showTopSuspiciousAnswers(Scanner scanner, AnswerService answerService) throws SQLException {
        int limite = readInt(scanner, "Combien de réponses suspectes afficher : ");
        List<Answer> answers = answerService.topAnswersSuspectes(limite);
        afficherListe(answers);
    }

    private static void showRepartitionParRole(AnswerService answerService) throws SQLException {
        Map<String, Integer> repartition = answerService.repartitionParRole();

        System.out.println("\n===== Répartition par rôle =====");
        if (repartition.isEmpty()) {
            System.out.println("Aucune donnée trouvée.");
            return;
        }

        for (Map.Entry<String, Integer> entry : repartition.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    private static void showRepartitionCorrectIncorrect(AnswerService answerService) throws SQLException {
        Map<String, Integer> repartition = answerService.repartitionCorrectIncorrect();

        System.out.println("\n===== Répartition correct / incorrect =====");
        if (repartition.isEmpty()) {
            System.out.println("Aucune donnée trouvée.");
            return;
        }

        for (Map.Entry<String, Integer> entry : repartition.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    private static void verifierCoherenceIntegrite(Scanner scanner, AnswerService answerService) throws SQLException {
        int answerId = readInt(scanner, "Entrer l'id de l'answer : ");
        boolean coherence = answerService.verifierCoherenceIntegrite(answerId);

        System.out.println("\n===== Cohérence intégrité =====");
        System.out.println("Cohérence : " + (coherence ? "OUI" : "NON"));
    }

    private static void afficherListe(List<Answer> answers) {
        if (answers == null || answers.isEmpty()) {
            System.out.println("Aucune answer trouvée.");
            return;
        }

        for (Answer a : answers) {
            System.out.println(a);
        }
    }

    private static Answer readAnswerData(Scanner scanner, Answer answer) {
        System.out.print("Content : ");
        answer.setContent(scanner.nextLine().trim());

        answer.setIsCorrect(readBoolean(scanner, "Is Correct (true/false) : "));

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

    private static boolean readBoolean(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("true")) return true;
            if (input.equals("false")) return false;

            System.out.println("Veuillez saisir true ou false.");
        }
    }
}