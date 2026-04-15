package tn.esprit.entities;

import java.time.LocalDateTime;

public class Message {

    private Integer id;
    private Integer conversationId;
    private Integer senderId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Integer replyToId;
    private Boolean isRead;

    public Message() {
    }

    public Message(Integer id, Integer conversationId, Integer senderId, String content, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, Integer replyToId, Boolean isRead) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.replyToId = replyToId;
        this.isRead = isRead;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getConversationId() {
        return conversationId;
    }

    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Integer getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(Integer replyToId) {
        this.replyToId = replyToId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id + ", " + "conversationId=" + conversationId + ", " + "senderId=" + senderId + ", " + "content=" + content + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt + ", " + "deletedAt=" + deletedAt + ", " + "replyToId=" + replyToId + ", " + "isRead=" + isRead +
                "}";
    }
}
