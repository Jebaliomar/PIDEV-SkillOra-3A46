package tn.esprit.entities;

import java.time.LocalDateTime;

public class WordGameProgress {

    private Integer id;
    private Integer userId;
    private Integer gameId;
    private Integer score;
    private Boolean completed;
    private LocalDateTime playedAt;

    public WordGameProgress() {
    }

    public WordGameProgress(Integer id, Integer userId, Integer gameId, Integer score, Boolean completed, LocalDateTime playedAt) {
        this.id = id;
        this.userId = userId;
        this.gameId = gameId;
        this.score = score;
        this.completed = completed;
        this.playedAt = playedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getGameId() {
        return gameId;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }

    @Override
    public String toString() {
        return "WordGameProgress{" +
                "id=" + id + ", " + "userId=" + userId + ", " + "gameId=" + gameId + ", " + "score=" + score + ", " + "completed=" + completed + ", " + "playedAt=" + playedAt +
                "}";
    }
}
