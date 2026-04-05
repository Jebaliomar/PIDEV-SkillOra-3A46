package tn.esprit.services;

import tn.esprit.entities.Evaluation;

import java.sql.SQLException;
import java.util.List;

public interface IEvaluationService<T> {
    void ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
    T chercherParId(int id) throws SQLException;
    List<T> recuperer() throws SQLException;
}