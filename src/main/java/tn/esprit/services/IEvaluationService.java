package tn.esprit.services;

import java.sql.SQLException;
import java.util.List;

public interface IEvaluationService<T> {
    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
    T chercherParId(int id) throws SQLException;
    List<T> recuperer() throws SQLException;

    // Tri
    List<T> trierParTitreAsc() throws SQLException;
    List<T> trierParTitreDesc() throws SQLException;
    List<T> trierParScoreAsc() throws SQLException;
    List<T> trierParScoreDesc() throws SQLException;
    List<T> trierParDateAsc() throws SQLException;
    List<T> trierParDateDesc() throws SQLException;

    // Recherche / filtre
    List<T> rechercherParMotCle(String motCle) throws SQLException;
    List<T> filtrerParType(String type) throws SQLException;

    // Pagination
    List<T> recupererParPage(int page, int taillePage) throws SQLException;

    // Statistiques
    int countEvaluations() throws SQLException;
    double moyenneScores() throws SQLException;

    // Récentes
    List<T> recupererRecentes(int limite) throws SQLException;

    // Validation
    boolean validerEvaluation(T t);
}