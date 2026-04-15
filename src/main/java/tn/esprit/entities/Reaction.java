package tn.esprit.entities;

import java.time.LocalDateTime;

public class Reaction {

    private Integer id;
    private String type;
    private LocalDateTime createdAt;
    private Integer postId;
    private Integer replyId;
    private Integer userId;

    public Reaction() {
    }

    public Reaction(Integer id, String type, LocalDateTime createdAt, Integer postId, Integer replyId, Integer userId) {
        this.id = id;
        this.type = type;
        this.createdAt = createdAt;
        this.postId = postId;
        this.replyId = replyId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getReplyId() {
        return replyId;
    }

    public void setReplyId(Integer replyId) {
        this.replyId = replyId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "id=" + id + ", " + "type=" + type + ", " + "createdAt=" + createdAt + ", " + "postId=" + postId + ", " + "replyId=" + replyId + ", " + "userId=" + userId +
                "}";
    }
}
