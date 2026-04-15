package tn.esprit.entities;

public class Question {

    private Integer id;
    private String content;
    private String explanation;
    private String type;
    private Integer score;
    private Integer evaluationId;

    public Question() {
    }

    public Question(Integer id, String content, String explanation, String type, Integer score, Integer evaluationId) {
        this.id = id;
        this.content = content;
        this.explanation = explanation;
        this.type = type;
        this.score = score;
        this.evaluationId = evaluationId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Integer evaluationId) {
        this.evaluationId = evaluationId;
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id + ", " + "content=" + content + ", " + "explanation=" + explanation + ", " + "type=" + type + ", " + "score=" + score + ", " + "evaluationId=" + evaluationId +
                "}";
    }
}
