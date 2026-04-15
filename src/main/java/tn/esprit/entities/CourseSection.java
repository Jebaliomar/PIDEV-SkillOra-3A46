package tn.esprit.entities;

import java.time.LocalDateTime;

public class CourseSection {

    private Integer id;
    private String title;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer courseId;

    public CourseSection() {
    }

    public CourseSection(Integer id, String title, Integer position, LocalDateTime createdAt, LocalDateTime updatedAt, Integer courseId) {
        this.id = id;
        this.title = title;
        this.position = position;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.courseId = courseId;
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    @Override
    public String toString() {
        return "CourseSection{" +
                "id=" + id + ", " + "title=" + title + ", " + "position=" + position + ", " + "createdAt=" + createdAt + ", " + "updatedAt=" + updatedAt + ", " + "courseId=" + courseId +
                "}";
    }
}
