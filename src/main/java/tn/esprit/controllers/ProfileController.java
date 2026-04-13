package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import tn.esprit.entities.User;
import tn.esprit.tools.Avatar3D;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private Label avatarInitials;
    @FXML private Circle avatarRing;
    @FXML private StackPane avatarHolder;
    @FXML private Label fullNameLabel;
    @FXML private Label fieldLabel;
    @FXML private Label roleBadge;
    @FXML private Label universityLabel;
    @FXML private Label countryLabel;
    @FXML private Label bioLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label dobLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label genderLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = StudentLayoutController.getCurrentUser();
        if (u == null) return;

        String first = safe(u.getFirstName());
        String last = safe(u.getLastName());
        fullNameLabel.setText((first + " " + last).trim());

        String initials = "";
        if (!first.isEmpty()) initials += first.charAt(0);
        if (!last.isEmpty()) initials += last.charAt(0);
        avatarInitials.setText(initials.toUpperCase());

        String avatarType = u.getAvatarType();
        if (avatarType != null && !avatarType.isEmpty()) {
            try {
                javafx.scene.layout.StackPane viewer = Avatar3D.buildViewer(avatarType, 200, 240);
                avatarHolder.getChildren().setAll(viewer);
                avatarInitials.setVisible(false);
                avatarInitials.setManaged(false);
                avatarRing.setVisible(false);
                avatarRing.setManaged(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        fieldLabel.setText(u.getFieldOfStudy() != null && !u.getFieldOfStudy().isEmpty() ? u.getFieldOfStudy() : "Student at SkillORA");
        universityLabel.setText(u.getUniversity() != null ? u.getUniversity() : "—");
        countryLabel.setText(u.getCountry() != null ? u.getCountry() : "—");
        bioLabel.setText(u.getBio() != null && !u.getBio().isEmpty() ? u.getBio() : "No bio yet. Tell us about yourself in Settings.");

        emailLabel.setText(safe(u.getEmail()));
        phoneLabel.setText(u.getPhone() != null && !u.getPhone().isEmpty() ? u.getPhone() : "Not set");
        genderLabel.setText(u.getGender() != null && !u.getGender().isEmpty() ? u.getGender() : "Not set");

        if (u.getDateOfBirth() != null) {
            dobLabel.setText(u.getDateOfBirth().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        } else {
            dobLabel.setText("Not set");
        }

        if (u.getCreatedAt() != null) {
            memberSinceLabel.setText(u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        } else {
            memberSinceLabel.setText("—");
        }
    }

    @FXML
    public void goToSettings() {
        StudentLayoutController.getInstance().showSettings();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
