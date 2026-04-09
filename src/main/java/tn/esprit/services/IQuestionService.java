package tn.esprit.services;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface IQuestionService<T> {

    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
    T chercherParId(int id) throws SQLException;
    List<T> recuperer() throws SQLException;

    // Tri
    List<T> trierParContenuAsc() throws SQLException;
    List<T> trierParContenuDesc() throws SQLException;
    List<T> trierParScoreAsc() throws SQLException;
    List<T> trierParScoreDesc() throws SQLException;
    List<T> trierParTypeAsc() throws SQLException;
    List<T> trierParTypeDesc() throws SQLException;

    // Recherche / filtre
    List<T> rechercherParMotCle(String motCle) throws SQLException;
    List<T> filtrerParType(String type) throws SQLException;
    List<T> filtrerParEvaluationId(int evaluationId) throws SQLException;
    List<T> filtrerParScoreMin(int scoreMin) throws SQLException;

    // Pagination
    List<T> recupererParPage(int page, int taillePage) throws SQLException;

    // Statistiques
    int countQuestions() throws SQLException;
    double moyenneScores() throws SQLException;

    // Validation
    boolean validerQuestion(T t);

    // Contrôle métier
    boolean evaluationExiste(int evaluationId) throws SQLException;

    // Fonctionnalités avancées
    List<T> topQuestions(int limite) throws SQLException;
    Map<String, Integer> repartitionParType() throws SQLException;

    int sommeScoresQuestionsParEvaluation(int evaluationId) throws SQLException;
    Integer totalScoreEvaluation(int evaluationId) throws SQLException;
    boolean verifierCoherenceEvaluationQuestions(int evaluationId) throws SQLException;
}