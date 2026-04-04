package tn.esprit.entities;

import java.time.LocalDateTime;

public class HangmanAttempt {

    private Integer id;
    private Integer userId;
    private Integer gameId;
    private String guessed;
    private Boolean won;
    private Boolean lost;
    private Integer score;
    private LocalDateTime updatedAt;
    private Short mistakes;
    private LocalDateTime playedAt;

    public HangmanAttempt() {
    }

    public HangmanAttempt(Integer id, Integer userId, Integer gameId, String guessed, Boolean won, Boolean lost, Integer score, LocalDateTime updatedAt, Short mistakes, LocalDateTime playedAt) {
        this.id = id;
        this.userId = userId;
        this.gameId = gameId;
        this.guessed = guessed;
        this.won = won;
        this.lost = lost;
        this.score = score;
        this.updatedAt = updatedAt;
        this.mistakes = mistakes;
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

    public String getGuessed() {
        return guessed;
    }

    public void setGuessed(String guessed) {
        this.guessed = guessed;
    }

    public Boolean getWon() {
        return won;
    }

    public void setWon(Boolean won) {
        this.won = won;
    }

    public Boolean getLost() {
        return lost;
    }

    public void setLost(Boolean lost) {
        this.lost = lost;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Short getMistakes() {
        return mistakes;
    }

    public void setMistakes(Short mistakes) {
        this.mistakes = mistakes;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }

    @Override
    public String toString() {
        return "HangmanAttempt{" +
                "id=" + id + ", " + "userId=" + userId + ", " + "gameId=" + gameId + ", " + "guessed=" + guessed + ", " + "won=" + won + ", " + "lost=" + lost + ", " + "score=" + score + ", " + "updatedAt=" + updatedAt + ", " + "mistakes=" + mistakes + ", " + "playedAt=" + playedAt +
                "}";
    }
}
