package tn.esprit.entities;

import java.time.LocalDateTime;

public class QuestionAudioAttempt {

    private Integer id;
    private Integer userId;
    private Integer questionId;
    private Integer playCount;
    private LocalDateTime updatedAt;

    public QuestionAudioAttempt() {
    }

    public QuestionAudioAttempt(Integer id, Integer userId, Integer questionId, Integer playCount, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.questionId = questionId;
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

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
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
        return "QuestionAudioAttempt{" +
                "id=" + id + ", " + "userId=" + userId + ", " + "questionId=" + questionId + ", " + "playCount=" + playCount + ", " + "updatedAt=" + updatedAt +
                "}";
    }
}
