package tn.esprit.entities;

import java.time.LocalDateTime;

public class Evaluation {

    private Integer id;
    private String title;
    private String description;
    private String type;
    private Integer duration;
    private Integer totalScore;
    private LocalDateTime createdAt;
    private String docxPath;
    private String pdfPath;

    public Evaluation() {
    }

    public Evaluation(Integer id, String title, String description, String type, Integer duration, Integer totalScore, LocalDateTime createdAt, String docxPath, String pdfPath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.duration = duration;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
        this.docxPath = docxPath;
        this.pdfPath = pdfPath;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getDocxPath() {
        return docxPath;
    }

    public void setDocxPath(String docxPath) {
        this.docxPath = docxPath;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    @Override
    public String toString() {
        return "Evaluation{" +
                "id=" + id + ", " + "title=" + title + ", " + "description=" + description + ", " + "type=" + type + ", " + "duration=" + duration + ", " + "totalScore=" + totalScore + ", " + "createdAt=" + createdAt + ", " + "docxPath=" + docxPath + ", " + "pdfPath=" + pdfPath +
                "}";
    }
}
