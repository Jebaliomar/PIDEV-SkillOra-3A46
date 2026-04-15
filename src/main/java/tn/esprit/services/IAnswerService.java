package tn.esprit.services;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IAnswerService<T> {

    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
    T chercherParId(int id) throws SQLException;
    List<T> recuperer() throws SQLException;

    // Tri
    List<T> trierParDateAsc() throws SQLException;
    List<T> trierParDateDesc() throws SQLException;
    List<T> trierParPlagiatDesc() throws SQLException;
    List<T> trierParAiSuspicionDesc() throws SQLException;

    // Recherche / filtre
    List<T> rechercherParMotCle(String motCle) throws SQLException;
    List<T> filtrerParQuestionId(int questionId) throws SQLException;
    List<T> filtrerParStudentId(int studentId) throws SQLException;
    List<T> filtrerParRole(String role) throws SQLException;
    List<T> filtrerParCorrect(boolean isCorrect) throws SQLException;

    // Pagination
    List<T> recupererParPage(int page, int taillePage) throws SQLException;

    // Statistiques
    int countAnswers() throws SQLException;
    double moyennePlagiatWeb() throws SQLException;
    double moyenneAiSuspicion() throws SQLException;
    int countCorrectes() throws SQLException;
    int countIncorrectes() throws SQLException;

    // Validation
    boolean validerAnswer(T t);

    // Contrôle métier
    boolean questionExiste(int questionId) throws SQLException;
    boolean reponseExistePourEtudiantEtQuestion(int studentId, int questionId) throws SQLException;

    // Fonctionnalités avancées
    List<T> recupererRecentes(int limite) throws SQLException;
    List<T> reponsesSuspectes(int seuilPlagiat, int seuilAi, int seuilPaste, int seuilTabSwitch) throws SQLException;
    List<T> topAnswersSuspectes(int limite) throws SQLException;

    Map<String, Integer> repartitionParRole() throws SQLException;
    Map<String, Integer> repartitionCorrectIncorrect() throws SQLException;

    boolean verifierCoherenceIntegrite(int answerId) throws SQLException;
}