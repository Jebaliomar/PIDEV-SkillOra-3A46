package tn.esprit.entities;

import java.time.LocalDateTime;

public class Reservation {

    private Integer id;
    private String nom;
    private String prenom;
    private String telephone;
    private String adresse;
    private String nombrePlaces;
    private LocalDateTime dateReservation;
    private Integer eventId;
    private Integer salleId;
    private Integer userId;

    public Reservation() {
    }

    public Reservation(Integer id, String nom, String prenom, String telephone, String adresse, String nombrePlaces, LocalDateTime dateReservation, Integer eventId, Integer salleId, Integer userId) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.adresse = adresse;
        this.nombrePlaces = nombrePlaces;
        this.dateReservation = dateReservation;
        this.eventId = eventId;
        this.salleId = salleId;
        this.userId = userId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getNombrePlaces() {
        return nombrePlaces;
    }

    public void setNombrePlaces(String nombrePlaces) {
        this.nombrePlaces = nombrePlaces;
    }

    public LocalDateTime getDateReservation() {
        return dateReservation;
    }

    public void setDateReservation(LocalDateTime dateReservation) {
        this.dateReservation = dateReservation;
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getSalleId() {
        return salleId;
    }

    public void setSalleId(Integer salleId) {
        this.salleId = salleId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id + ", " + "nom=" + nom + ", " + "prenom=" + prenom + ", " + "telephone=" + telephone + ", " + "adresse=" + adresse + ", " + "nombrePlaces=" + nombrePlaces + ", " + "dateReservation=" + dateReservation + ", " + "eventId=" + eventId + ", " + "salleId=" + salleId + ", " + "userId=" + userId +
                "}";
    }
}
