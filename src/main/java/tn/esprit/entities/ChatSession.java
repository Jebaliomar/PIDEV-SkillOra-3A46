package tn.esprit.entities;

import java.time.LocalDateTime;

public class ChatSession {

    private Integer id;
    private Integer userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ChatSession() {
    }

    public ChatSession(Integer id, Integer userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ChatSession{" +
                "id=" + id + ", " + "userId=" + userId + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt +
                "}";
    }
}
