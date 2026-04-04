package tn.esprit.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class User {

    private Integer id;
    private String email;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private Integer isActive;
    private Integer isVerified;
    private String verificationToken;
    private LocalDateTime verificationTokenExpiresAt;
    private String resetToken;
    private LocalDateTime resetTokenExpiresAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String phone;
    private String gender;
    private String bio;
    private String avatar;
    private String avatarType;
    private String faceData;
    private Boolean faceIdEnabled;
    private LocalDate dateOfBirth;
    private String fieldOfStudy;
    private String university;
    private String country;
    private Integer profileCompleted;

    public User() {
    }

    public User(Integer id, String email, String username, String password, String firstName, String lastName, Integer isActive, Integer isVerified, String verificationToken, LocalDateTime verificationTokenExpiresAt, String resetToken, LocalDateTime resetTokenExpiresAt, LocalDateTime lastLoginAt, LocalDateTime createdAt, String phone, String gender, String bio, String avatar, String avatarType, String faceData, Boolean faceIdEnabled, LocalDate dateOfBirth, String fieldOfStudy, String university, String country, Integer profileCompleted) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.isActive = isActive;
        this.isVerified = isVerified;
        this.verificationToken = verificationToken;
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
        this.resetToken = resetToken;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
        this.lastLoginAt = lastLoginAt;
        this.createdAt = createdAt;
        this.phone = phone;
        this.gender = gender;
        this.bio = bio;
        this.avatar = avatar;
        this.avatarType = avatarType;
        this.faceData = faceData;
        this.faceIdEnabled = faceIdEnabled;
        this.dateOfBirth = dateOfBirth;
        this.fieldOfStudy = fieldOfStudy;
        this.university = university;
        this.country = country;
        this.profileCompleted = profileCompleted;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public Integer getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Integer isVerified) {
        this.isVerified = isVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    public void setVerificationTokenExpiresAt(LocalDateTime verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetTokenExpiresAt(LocalDateTime resetTokenExpiresAt) {
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAvatarType() {
        return avatarType;
    }

    public void setAvatarType(String avatarType) {
        this.avatarType = avatarType;
    }

    public String getFaceData() {
        return faceData;
    }

    public void setFaceData(String faceData) {
        this.faceData = faceData;
    }

    public Boolean getFaceIdEnabled() {
        return faceIdEnabled;
    }

    public void setFaceIdEnabled(Boolean faceIdEnabled) {
        this.faceIdEnabled = faceIdEnabled;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public String getUniversity() {
        return university;
    }

    public void setUniversity(String university) {
        this.university = university;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Integer profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id + ", " + "email=" + email + ", " + "username=" + username + ", " + "password=" + password + ", " + "firstName=" + firstName + ", " + "lastName=" + lastName + ", " + "isActive=" + isActive + ", " + "isVerified=" + isVerified + ", " + "verificationToken=" + verificationToken + ", " + "verificationTokenExpiresAt=" + verificationTokenExpiresAt + ", " + "resetToken=" + resetToken + ", " + "resetTokenExpiresAt=" + resetTokenExpiresAt + ", " + "lastLoginAt=" + lastLoginAt + ", " + "createdAt=" + createdAt + ", " + "phone=" + phone + ", " + "gender=" + gender + ", " + "bio=" + bio + ", " + "avatar=" + avatar + ", " + "avatarType=" + avatarType + ", " + "faceData=" + faceData + ", " + "faceIdEnabled=" + faceIdEnabled + ", " + "dateOfBirth=" + dateOfBirth + ", " + "fieldOfStudy=" + fieldOfStudy + ", " + "university=" + university + ", " + "country=" + country + ", " + "profileCompleted=" + profileCompleted +
                "}";
    }
}
