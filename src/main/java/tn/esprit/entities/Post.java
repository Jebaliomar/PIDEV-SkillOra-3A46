package tn.esprit.entities;

import java.time.LocalDateTime;

public class Post {

    private Integer id;
    private String type;
    private String title;
    private String topic;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer userId;

    public Post() {
    }

    public Post(Integer id, String type, String title, String topic, String content, LocalDateTime createdAt, LocalDateTime updatedAt, Integer userId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.topic = topic;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id + ", " + "type=" + type + ", " + "title=" + title + ", " + "topic=" + topic + ", " + "content=" + content + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt + ", " + "userId=" + userId +
                "}";
    }
}
