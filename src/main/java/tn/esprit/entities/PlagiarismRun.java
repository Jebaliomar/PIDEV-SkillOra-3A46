package tn.esprit.entities;

import java.time.LocalDateTime;

public class PlagiarismRun {

    private Integer id;
    private Integer evaluationId;
    private LocalDateTime createdAt;
    private Integer submissionsCount;
    private Integer pairsCount;
    private String thresholds;

    public PlagiarismRun() {
    }

    public PlagiarismRun(Integer id, Integer evaluationId, LocalDateTime createdAt, Integer submissionsCount, Integer pairsCount, String thresholds) {
        this.id = id;
        this.evaluationId = evaluationId;
        this.createdAt = createdAt;
        this.submissionsCount = submissionsCount;
        this.pairsCount = pairsCount;
        this.thresholds = thresholds;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Integer evaluationId) {
        this.evaluationId = evaluationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getSubmissionsCount() {
        return submissionsCount;
    }

    public void setSubmissionsCount(Integer submissionsCount) {
        this.submissionsCount = submissionsCount;
    }

    public Integer getPairsCount() {
        return pairsCount;
    }

    public void setPairsCount(Integer pairsCount) {
        this.pairsCount = pairsCount;
    }

    public String getThresholds() {
        return thresholds;
    }

    public void setThresholds(String thresholds) {
        this.thresholds = thresholds;
    }

    @Override
    public String toString() {
        return "PlagiarismRun{" +
                "id=" + id + ", " + "evaluationId=" + evaluationId + ", " + "createdAt=" + createdAt + ", " + "submissionsCount=" + submissionsCount + ", " + "pairsCount=" + pairsCount + ", " + "thresholds=" + thresholds +
                "}";
    }
}
