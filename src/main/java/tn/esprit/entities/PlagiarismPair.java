package tn.esprit.entities;

import java.time.LocalDateTime;

public class PlagiarismPair {

    private Integer id;
    private Integer runId;
    private Integer evaluationId;
    private Integer answerAId;
    private Integer answerBId;
    private Float semantic;
    private Float lexical;
    private Float structure;
    private Float finalScore;
    private Integer plagiarismPercent;
    private String status;
    private String highlights;
    private LocalDateTime createdAt;

    public PlagiarismPair() {
    }

    public PlagiarismPair(Integer id, Integer runId, Integer evaluationId, Integer answerAId, Integer answerBId, Float semantic, Float lexical, Float structure, Float finalScore, Integer plagiarismPercent, String status, String highlights, LocalDateTime createdAt) {
        this.id = id;
        this.runId = runId;
        this.evaluationId = evaluationId;
        this.answerAId = answerAId;
        this.answerBId = answerBId;
        this.semantic = semantic;
        this.lexical = lexical;
        this.structure = structure;
        this.finalScore = finalScore;
        this.plagiarismPercent = plagiarismPercent;
        this.status = status;
        this.highlights = highlights;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRunId() {
        return runId;
    }

    public void setRunId(Integer runId) {
        this.runId = runId;
    }

    public Integer getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Integer evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Integer getAnswerAId() {
        return answerAId;
    }

    public void setAnswerAId(Integer answerAId) {
        this.answerAId = answerAId;
    }

    public Integer getAnswerBId() {
        return answerBId;
    }

    public void setAnswerBId(Integer answerBId) {
        this.answerBId = answerBId;
    }

    public Float getSemantic() {
        return semantic;
    }

    public void setSemantic(Float semantic) {
        this.semantic = semantic;
    }

    public Float getLexical() {
        return lexical;
    }

    public void setLexical(Float lexical) {
        this.lexical = lexical;
    }

    public Float getStructure() {
        return structure;
    }

    public void setStructure(Float structure) {
        this.structure = structure;
    }

    public Float getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Float finalScore) {
        this.finalScore = finalScore;
    }

    public Integer getPlagiarismPercent() {
        return plagiarismPercent;
    }

    public void setPlagiarismPercent(Integer plagiarismPercent) {
        this.plagiarismPercent = plagiarismPercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHighlights() {
        return highlights;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PlagiarismPair{" +
                "id=" + id + ", " + "runId=" + runId + ", " + "evaluationId=" + evaluationId + ", " + "answerAId=" + answerAId + ", " + "answerBId=" + answerBId + ", " + "semantic=" + semantic + ", " + "lexical=" + lexical + ", " + "structure=" + structure + ", " + "finalScore=" + finalScore + ", " + "plagiarismPercent=" + plagiarismPercent + ", " + "status=" + status + ", " + "highlights=" + highlights + ", " + "createdAt=" + createdAt +
                "}";
    }
}
