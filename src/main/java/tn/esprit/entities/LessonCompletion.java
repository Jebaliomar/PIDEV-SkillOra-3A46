package tn.esprit.entities;

import java.time.LocalDateTime;

public class LessonCompletion {

    private Integer id;
    private LocalDateTime completedAt;
    private Integer enrollmentId;
    private Integer lessonId;

    public LessonCompletion() {
    }

    public LessonCompletion(Integer id, LocalDateTime completedAt, Integer enrollmentId, Integer lessonId) {
        this.id = id;
        this.completedAt = completedAt;
        this.enrollmentId = enrollmentId;
        this.lessonId = lessonId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Integer getLessonId() {
        return lessonId;
    }

    public void setLessonId(Integer lessonId) {
        this.lessonId = lessonId;
    }

    @Override
    public String toString() {
        return "LessonCompletion{" +
                "id=" + id + ", " + "completedAt=" + completedAt + ", " + "enrollmentId=" + enrollmentId + ", " + "lessonId=" + lessonId +
                "}";
    }
}
