package tn.esprit.entities;

import java.time.LocalDateTime;

public class RendezVous {

    private Integer id;
    private Integer slotId;
    private String statut;
    private LocalDateTime createdAt;
    private String ownerToken;
    private Integer studentId;
    private Integer professorId;
    private Integer courseId;
    private String meetingType;
    private String meetingLink;
    private String location;
    private String locationLabel;
    private Float locationLat;
    private Float locationLng;
    private String message;
    private String refusalReason;
    private String coursePdfName;

    public RendezVous() {
    }

    public RendezVous(Integer id, Integer slotId, String statut, LocalDateTime createdAt, String ownerToken, Integer studentId, Integer professorId, Integer courseId, String meetingType, String meetingLink, String location, String locationLabel, Float locationLat, Float locationLng, String message, String refusalReason, String coursePdfName) {
        this.id = id;
        this.slotId = slotId;
        this.statut = statut;
        this.createdAt = createdAt;
        this.ownerToken = ownerToken;
        this.studentId = studentId;
        this.professorId = professorId;
        this.courseId = courseId;
        this.meetingType = meetingType;
        this.meetingLink = meetingLink;
        this.location = location;
        this.locationLabel = locationLabel;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.message = message;
        this.refusalReason = refusalReason;
        this.coursePdfName = coursePdfName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSlotId() {
        return slotId;
    }

    public void setSlotId(Integer slotId) {
        this.slotId = slotId;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getOwnerToken() {
        return ownerToken;
    }

    public void setOwnerToken(String ownerToken) {
        this.ownerToken = ownerToken;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Integer professorId) {
        this.professorId = professorId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationLabel() {
        return locationLabel;
    }

    public void setLocationLabel(String locationLabel) {
        this.locationLabel = locationLabel;
    }

    public Float getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(Float locationLat) {
        this.locationLat = locationLat;
    }

    public Float getLocationLng() {
        return locationLng;
    }

    public void setLocationLng(Float locationLng) {
        this.locationLng = locationLng;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRefusalReason() {
        return refusalReason;
    }

    public void setRefusalReason(String refusalReason) {
        this.refusalReason = refusalReason;
    }

    public String getCoursePdfName() {
        return coursePdfName;
    }

    public void setCoursePdfName(String coursePdfName) {
        this.coursePdfName = coursePdfName;
    }

    @Override
    public String toString() {
        return "RendezVous{" +
                "id=" + id + ", " + "slotId=" + slotId + ", " + "statut=" + statut + ", " + "createdAt=" + createdAt + ", " + "ownerToken=" + ownerToken + ", " + "studentId=" + studentId + ", " + "professorId=" + professorId + ", " + "courseId=" + courseId + ", " + "meetingType=" + meetingType + ", " + "meetingLink=" + meetingLink + ", " + "location=" + location + ", " + "locationLabel=" + locationLabel + ", " + "locationLat=" + locationLat + ", " + "locationLng=" + locationLng + ", " + "message=" + message + ", " + "refusalReason=" + refusalReason + ", " + "coursePdfName=" + coursePdfName +
                "}";
    }
}
