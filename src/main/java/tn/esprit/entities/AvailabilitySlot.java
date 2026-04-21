package tn.esprit.entities;

import java.time.LocalDateTime;

public class AvailabilitySlot {

    private Integer id;
    private Integer professorId;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Boolean isBooked;
    private String locationLabel;
    private Float locationLat;
    private Float locationLng;
    private LocalDateTime createdAt;

    public AvailabilitySlot() {
    }

    public AvailabilitySlot(Integer id, Integer professorId, LocalDateTime startAt, LocalDateTime endAt, Boolean isBooked, String locationLabel, Float locationLat, Float locationLng, LocalDateTime createdAt) {
        this.id = id;
        this.professorId = professorId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.isBooked = isBooked;
        this.locationLabel = locationLabel;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
        this.createdAt = createdAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Integer professorId) {
        this.professorId = professorId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Boolean getIsBooked() {
        return isBooked;
    }

    public void setIsBooked(Boolean isBooked) {
        this.isBooked = isBooked;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AvailabilitySlot{" +
                "id=" + id + ", " + "professorId=" + professorId + ", " + "startAt=" + startAt + ", " + "endAt=" + endAt + ", " + "isBooked=" + isBooked + ", " + "locationLabel=" + locationLabel + ", " + "locationLat=" + locationLat + ", " + "locationLng=" + locationLng + ", " + "createdAt=" + createdAt +
                "}";
    }
}
