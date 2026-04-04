package tn.esprit.entities;

public class Salle {

    private Integer id;
    private String name;
    private String image3d;
    private Integer maxParticipants;
    private Integer duration;
    private String equipment;
    private String location;
    private Integer eventId;

    public Salle() {
    }

    public Salle(Integer id, String name, String image3d, Integer maxParticipants, Integer duration, String equipment, String location, Integer eventId) {
        this.id = id;
        this.name = name;
        this.image3d = image3d;
        this.maxParticipants = maxParticipants;
        this.duration = duration;
        this.equipment = equipment;
        this.location = location;
        this.eventId = eventId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage3d() {
        return image3d;
    }

    public void setImage3d(String image3d) {
        this.image3d = image3d;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    @Override
    public String toString() {
        return "Salle{" +
                "id=" + id + ", " + "name=" + name + ", " + "image3d=" + image3d + ", " + "maxParticipants=" + maxParticipants + ", " + "duration=" + duration + ", " + "equipment=" + equipment + ", " + "location=" + location + ", " + "eventId=" + eventId +
                "}";
    }
}
