package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import tn.esprit.entities.User;

import java.net.URL;
import java.util.ResourceBundle;

public class ProfessorHomeController implements Initializable {

    @FXML private Label heroTitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = ProfessorLayoutController.getCurrentUser();
        if (u != null && heroTitle != null) {
            heroTitle.setText("Welcome back, Prof. " + u.getLastName() + "!");
        }
    }

    @FXML
    public void goToProfile() {
        ProfessorLayoutController.getInstance().showProfile();
    }

    @FXML
    public void goToSettings() {
        ProfessorLayoutController.getInstance().showSettings();
    }
}
