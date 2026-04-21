package tn.esprit.entities;

import java.time.LocalDateTime;

public class HangmanGame {

    private Integer id;
    private String title;
    private String topic;
    private String level;
    private String hint;
    private String answer;
    private Short maxMistakes;
    private LocalDateTime createdAt;

    public HangmanGame() {
    }

    public HangmanGame(Integer id, String title, String topic, String level, String hint, String answer, Short maxMistakes, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.topic = topic;
        this.level = level;
        this.hint = hint;
        this.answer = answer;
        this.maxMistakes = maxMistakes;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Short getMaxMistakes() {
        return maxMistakes;
    }

    public void setMaxMistakes(Short maxMistakes) {
        this.maxMistakes = maxMistakes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "HangmanGame{" +
                "id=" + id + ", " + "title=" + title + ", " + "topic=" + topic + ", " + "level=" + level + ", " + "hint=" + hint + ", " + "answer=" + answer + ", " + "maxMistakes=" + maxMistakes + ", " + "createdAt=" + createdAt +
                "}";
    }
}
