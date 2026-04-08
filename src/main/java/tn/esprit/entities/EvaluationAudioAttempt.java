package tn.esprit.entities;

import java.time.LocalDateTime;

public class EvaluationAudioAttempt {

    private Integer id;
    private Integer userId;
    private Integer evaluationId;
    private Integer playCount;
    private LocalDateTime updatedAt;

    public EvaluationAudioAttempt() {
    }

    public EvaluationAudioAttempt(Integer id, Integer userId, Integer evaluationId, Integer playCount, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.evaluationId = evaluationId;
        this.playCount = playCount;
        this.updatedAt = updatedAt;
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

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "EvaluationAudioAttempt{" +
                "id=" + id + ", " + "userId=" + userId + ", " + "evaluationId=" + evaluationId + ", " + "playCount=" + playCount + ", " + "updatedAt=" + updatedAt +
                "}";
    }
}
