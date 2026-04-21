package tn.esprit.entities;

import java.time.LocalDateTime;

public class ChatMessage {

    private Integer id;
    private Integer sessionId;
    private Integer userId;
    private String role;
    private String content;
    private String model;
    private LocalDateTime createdAt;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private String meta;

    public ChatMessage() {
    }

    public ChatMessage(Integer id, Integer sessionId, Integer userId, String role, String content, String model, LocalDateTime createdAt, Integer inputTokens, Integer outputTokens, Integer totalTokens, String meta) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.model = model;
        this.createdAt = createdAt;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.meta = meta;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getMeta() {
        return meta;
    }

    public void setMeta(String meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id + ", " + "sessionId=" + sessionId + ", " + "userId=" + userId + ", " + "role=" + role + ", " + "content=" + content + ", " + "model=" + model + ", " + "createdAt=" + createdAt + ", " + "inputTokens=" + inputTokens + ", " + "outputTokens=" + outputTokens + ", " + "totalTokens=" + totalTokens + ", " + "meta=" + meta +
                "}";
    }
}
