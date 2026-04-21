package tn.esprit.entities;

import java.time.LocalDateTime;

public class UserRole {

    private Integer userId;
    private String role;
    private LocalDateTime createdAt;

    public UserRole() {
    }

    public UserRole(Integer userId, String role, LocalDateTime createdAt) {
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "userId=" + userId + ", " + "role=" + role + ", " + "createdAt=" + createdAt +
                "}";
    }
}
