package tn.esprit.entities;

public class WordGameWord {

    private Integer id;
    private Integer gameId;
    private String wordText;
    private String hint;

    public WordGameWord() {
    }

    public WordGameWord(Integer id, Integer gameId, String wordText, String hint) {
        this.id = id;
        this.gameId = gameId;
        this.wordText = wordText;
        this.hint = hint;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getGameId() {
        return gameId;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public String getWordText() {
        return wordText;
    }

    public void setWordText(String wordText) {
        this.wordText = wordText;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    @Override
    public String toString() {
        return "WordGameWord{" +
                "id=" + id + ", " + "gameId=" + gameId + ", " + "wordText=" + wordText + ", " + "hint=" + hint +
                "}";
    }
}
