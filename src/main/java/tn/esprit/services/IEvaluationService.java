package tn.esprit.services;

import java.sql.SQLException;
import java.util.List;

public interface IEvaluationService<T> {
    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
    T chercherParId(int id) throws SQLException;
    List<T> recuperer() throws SQLException;

    List<T> trierParTitreAsc() throws SQLException;
    List<T> trierParTitreDesc() throws SQLException;
    List<T> trierParScoreAsc() throws SQLException;
    List<T> trierParScoreDesc() throws SQLException;
    List<T> trierParDateAsc() throws SQLException;
    List<T> trierParDateDesc() throws SQLException;

    List<T> rechercherParMotCle(String motCle) throws SQLException;
    List<T> filtrerParType(String type) throws SQLException;

    List<T> recupererParPage(int page, int taillePage) throws SQLException;

    int countEvaluations() throws SQLException;
    double moyenneScores() throws SQLException;

    List<T> recupererRecentes(int limite) throws SQLException;

    boolean validerEvaluation(T t);
}