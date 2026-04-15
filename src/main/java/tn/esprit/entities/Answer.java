package tn.esprit.entities;

import java.time.LocalDateTime;

public class Answer {

    private Integer id;
    private String content;
    private Boolean isCorrect;
    private String role;
    private Integer questionId;
    private Integer studentId;
    private LocalDateTime createdAt;
    private Integer webPlagiarismPercent;
    private Integer aiSuspicionPercent;
    private String webSources;
    private Integer pasteCount;
    private Integer tabSwitchCount;
    private LocalDateTime lastIntegrityEventAt;
    private LocalDateTime lastPlagiarismCheckAt;

    public Answer() {
    }

    public Answer(Integer id, String content, Boolean isCorrect, String role, Integer questionId, Integer studentId, LocalDateTime createdAt, Integer webPlagiarismPercent, Integer aiSuspicionPercent, String webSources, Integer pasteCount, Integer tabSwitchCount, LocalDateTime lastIntegrityEventAt, LocalDateTime lastPlagiarismCheckAt) {
        this.id = id;
        this.content = content;
        this.isCorrect = isCorrect;
        this.role = role;
        this.questionId = questionId;
        this.studentId = studentId;
        this.createdAt = createdAt;
        this.webPlagiarismPercent = webPlagiarismPercent;
        this.aiSuspicionPercent = aiSuspicionPercent;
        this.webSources = webSources;
        this.pasteCount = pasteCount;
        this.tabSwitchCount = tabSwitchCount;
        this.lastIntegrityEventAt = lastIntegrityEventAt;
        this.lastPlagiarismCheckAt = lastPlagiarismCheckAt;
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

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getWebPlagiarismPercent() {
        return webPlagiarismPercent;
    }

    public void setWebPlagiarismPercent(Integer webPlagiarismPercent) {
        this.webPlagiarismPercent = webPlagiarismPercent;
    }

    public Integer getAiSuspicionPercent() {
        return aiSuspicionPercent;
    }

    public void setAiSuspicionPercent(Integer aiSuspicionPercent) {
        this.aiSuspicionPercent = aiSuspicionPercent;
    }

    public String getWebSources() {
        return webSources;
    }

    public void setWebSources(String webSources) {
        this.webSources = webSources;
    }

    public Integer getPasteCount() {
        return pasteCount;
    }

    public void setPasteCount(Integer pasteCount) {
        this.pasteCount = pasteCount;
    }

    public Integer getTabSwitchCount() {
        return tabSwitchCount;
    }

    public void setTabSwitchCount(Integer tabSwitchCount) {
        this.tabSwitchCount = tabSwitchCount;
    }

    public LocalDateTime getLastIntegrityEventAt() {
        return lastIntegrityEventAt;
    }

    public void setLastIntegrityEventAt(LocalDateTime lastIntegrityEventAt) {
        this.lastIntegrityEventAt = lastIntegrityEventAt;
    }

    public LocalDateTime getLastPlagiarismCheckAt() {
        return lastPlagiarismCheckAt;
    }

    public void setLastPlagiarismCheckAt(LocalDateTime lastPlagiarismCheckAt) {
        this.lastPlagiarismCheckAt = lastPlagiarismCheckAt;
    }

    @Override
    public String toString() {
        return "Answer{" +
                "id=" + id + ", " + "content=" + content + ", " + "isCorrect=" + isCorrect + ", " + "role=" + role + ", " + "questionId=" + questionId + ", " + "studentId=" + studentId + ", " + "createdAt=" + createdAt + ", " + "webPlagiarismPercent=" + webPlagiarismPercent + ", " + "aiSuspicionPercent=" + aiSuspicionPercent + ", " + "webSources=" + webSources + ", " + "pasteCount=" + pasteCount + ", " + "tabSwitchCount=" + tabSwitchCount + ", " + "lastIntegrityEventAt=" + lastIntegrityEventAt + ", " + "lastPlagiarismCheckAt=" + lastPlagiarismCheckAt +
                "}";
    }
}
