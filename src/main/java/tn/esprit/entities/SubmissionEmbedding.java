package tn.esprit.entities;

import java.time.LocalDateTime;

public class SubmissionEmbedding {

    private Integer id;
    private Integer userEvaluationId;
    private String embedding;
    private LocalDateTime createdAt;

    public SubmissionEmbedding() {
    }

    public SubmissionEmbedding(Integer id, Integer userEvaluationId, String embedding, LocalDateTime createdAt) {
        this.id = id;
        this.userEvaluationId = userEvaluationId;
        this.embedding = embedding;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserEvaluationId() {
        return userEvaluationId;
    }

    public void setUserEvaluationId(Integer userEvaluationId) {
        this.userEvaluationId = userEvaluationId;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SubmissionEmbedding{" +
                "id=" + id + ", " + "userEvaluationId=" + userEvaluationId + ", " + "embedding=" + embedding + ", " + "createdAt=" + createdAt +
                "}";
    }
}
