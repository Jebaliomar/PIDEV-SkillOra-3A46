package tn.esprit.entities;

import java.time.LocalDateTime;

public class Lesson {

    private Integer id;
    private String title;
    private String type;
    private String content;
    private String filePath;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer sectionId;

    public Lesson() {
    }

    public Lesson(Integer id, String title, String type, String content, String filePath, Integer position, LocalDateTime createdAt, LocalDateTime updatedAt, Integer sectionId) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.content = content;
        this.filePath = filePath;
        this.position = position;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.sectionId = sectionId;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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

    public Integer getSectionId() {
        return sectionId;
    }

    public void setSectionId(Integer sectionId) {
        this.sectionId = sectionId;
    }

    @Override
    public String toString() {
        return "Lesson{" +
                "id=" + id + ", " + "title=" + title + ", " + "type=" + type + ", " + "content=" + content + ", " + "filePath=" + filePath + ", " + "position=" + position + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt + ", " + "sectionId=" + sectionId +
                "}";
    }
}
