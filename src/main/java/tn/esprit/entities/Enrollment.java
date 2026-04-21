package tn.esprit.entities;

import java.time.LocalDateTime;

public class Enrollment {

    private Integer id;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private Short progressPercent;
    private String status;
    private Integer userId;
    private Integer courseId;

    public Enrollment() {
    }

    public Enrollment(Integer id, LocalDateTime enrolledAt, LocalDateTime completedAt, Short progressPercent, String status, Integer userId, Integer courseId) {
        this.id = id;
        this.enrolledAt = enrolledAt;
        this.completedAt = completedAt;
        this.progressPercent = progressPercent;
        this.status = status;
        this.userId = userId;
        this.courseId = courseId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Short getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Short progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    @Override
    public String toString() {
        return "Enrollment{" +
                "id=" + id + ", " + "enrolledAt=" + enrolledAt + ", " + "completedAt=" + completedAt + ", " + "progressPercent=" + progressPercent + ", " + "status=" + status + ", " + "userId=" + userId + ", " + "courseId=" + courseId +
                "}";
    }
}
