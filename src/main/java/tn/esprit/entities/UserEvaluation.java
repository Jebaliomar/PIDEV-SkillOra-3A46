package tn.esprit.entities;

import java.time.LocalDateTime;

public class UserEvaluation {

    private Integer id;
    private Integer userId;
    private Integer evaluationId;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer score;
    private String aiFeedback;
    private LocalDateTime aiCorrectedAt;
    private Boolean isCorrected;

    public UserEvaluation() {
    }

    public UserEvaluation(Integer id, Integer userId, Integer evaluationId, LocalDateTime startedAt, LocalDateTime submittedAt, Integer score, String aiFeedback, LocalDateTime aiCorrectedAt, Boolean isCorrected) {
        this.id = id;
        this.userId = userId;
        this.evaluationId = evaluationId;
        this.startedAt = startedAt;
        this.submittedAt = submittedAt;
        this.score = score;
        this.aiFeedback = aiFeedback;
        this.aiCorrectedAt = aiCorrectedAt;
        this.isCorrected = isCorrected;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Integer evaluationId) {
        this.evaluationId = evaluationId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public LocalDateTime getAiCorrectedAt() {
        return aiCorrectedAt;
    }

    public void setAiCorrectedAt(LocalDateTime aiCorrectedAt) {
        this.aiCorrectedAt = aiCorrectedAt;
    }

    public Boolean getIsCorrected() {
        return isCorrected;
    }

    public void setIsCorrected(Boolean isCorrected) {
        this.isCorrected = isCorrected;
    }

    @Override
    public String toString() {
        return "UserEvaluation{" +
                "id=" + id + ", " + "userId=" + userId + ", " + "evaluationId=" + evaluationId + ", " + "startedAt=" + startedAt + ", " + "submittedAt=" + submittedAt + ", " + "score=" + score + ", " + "aiFeedback=" + aiFeedback + ", " + "aiCorrectedAt=" + aiCorrectedAt + ", " + "isCorrected=" + isCorrected +
                "}";
    }
}
