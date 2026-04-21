package tn.esprit.entities;

import java.time.LocalDateTime;

public class Event {

    private Integer id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String eventType;
    private String priceType;
    private String image;
    private Integer salleId;

    public Event() {
    }

    public Event(Integer id, String title, String description, LocalDateTime startDate, LocalDateTime endDate, String eventType, String priceType, String image, Integer salleId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.eventType = eventType;
        this.priceType = priceType;
        this.image = image;
        this.salleId = salleId;
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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPriceType() {
        return priceType;
    }

    public void setPriceType(String priceType) {
        this.priceType = priceType;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getSalleId() {
        return salleId;
    }

    public void setSalleId(Integer salleId) {
        this.salleId = salleId;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id + ", " + "title=" + title + ", " + "description=" + description + ", " + "startDate=" + startDate + ", " + "endDate=" + endDate + ", " + "eventType=" + eventType + ", " + "priceType=" + priceType + ", " + "image=" + image + ", " + "salleId=" + salleId +
                "}";
    }
}
