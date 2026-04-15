package tn.esprit.entities;

import java.time.LocalDateTime;

public class SkilloraKbArticle {

    private Integer id;
    private String title;
    private String content;
    private String tags;
    private LocalDateTime updatedAt;

    public SkilloraKbArticle() {
    }

    public SkilloraKbArticle(Integer id, String title, String content, String tags, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SkilloraKbArticle{" +
                "id=" + id + ", " + "title=" + title + ", " + "content=" + content + ", " + "tags=" + tags + ", " + "updatedAt=" + updatedAt +
                "}";
    }
}
