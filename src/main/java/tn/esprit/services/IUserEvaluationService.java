package tn.esprit.services;

import tn.esprit.entities.UserEvaluation;

import java.sql.SQLException;
import java.util.List;

public interface IUserEvaluationService {

    List<Object[]> getUserEvaluationsWithEvaluation(int userId) throws SQLException;

    Integer findUserEvaluationId(int userId, int evaluationId) throws SQLException;

    void createStartedUserEvaluation(int userId, int evaluationId) throws SQLException;

    void submitExamResponse(int userId, int evaluationId, String responseText) throws SQLException;

    UserEvaluation getByUserIdAndEvaluationId(int userId, int evaluationId) throws SQLException;

    void saveOrUpdate(UserEvaluation ue) throws SQLException;
}