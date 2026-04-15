package tn.esprit.entities;

import java.time.LocalDateTime;

public class WordGame {

    private Integer id;
    private String title;
    private String description;
    private String level;
    private LocalDateTime createdAt;

    public WordGame() {
    }

    public WordGame(Integer id, String title, String description, String level, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.level = level;
        this.createdAt = createdAt;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "WordGame{" +
                "id=" + id + ", " + "title=" + title + ", " + "description=" + description + ", " + "level=" + level + ", " + "createdAt=" + createdAt +
                "}";
    }
}
