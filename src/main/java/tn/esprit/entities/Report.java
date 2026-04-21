package tn.esprit.entities;

import java.time.LocalDateTime;

public class Report {

    private Integer id;
    private Integer postId;
    private Integer userId;
    private String reason;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    private Integer reviewedBy;

    public Report() {
    }

    public Report(Integer id, Integer postId, Integer userId, String reason, String description, String status, LocalDateTime createdAt, LocalDateTime reviewedAt, Integer reviewedBy) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.reason = reason;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Integer reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id + ", " + "postId=" + postId + ", " + "userId=" + userId + ", " + "reason=" + reason + ", " + "description=" + description + ", " + "status=" + status + ", " + "createdAt=" + createdAt + ", " + "reviewedAt=" + reviewedAt + ", " + "reviewedBy=" + reviewedBy +
                "}";
    }
}
