package tn.esprit.entities;

import java.time.LocalDateTime;

public class Reply {

    private Integer id;
    private Integer postId;
    private Integer parentId;
    private String content;
    private String authorName;
    private Integer upvotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer userId;

    public Reply() {
    }

    public Reply(Integer id, Integer postId, Integer parentId, String content, String authorName, Integer upvotes, LocalDateTime createdAt, LocalDateTime updatedAt, Integer userId) {
        this.id = id;
        this.postId = postId;
        this.parentId = parentId;
        this.content = content;
        this.authorName = authorName;
        this.upvotes = upvotes;
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

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(Integer upvotes) {
        this.upvotes = upvotes;
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
        return "Reply{" +
                "id=" + id + ", " + "postId=" + postId + ", " + "parentId=" + parentId + ", " + "content=" + content + ", " + "authorName=" + authorName + ", " + "upvotes=" + upvotes + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt + ", " + "userId=" + userId +
                "}";
    }
}
