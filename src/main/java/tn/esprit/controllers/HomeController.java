package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import tn.esprit.entities.User;

import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML private Label heroTitle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = StudentLayoutController.getCurrentUser();
        if (u != null && heroTitle != null) {
            heroTitle.setText("Welcome back, " + u.getFirstName() + "!");
        }
    }

    @FXML
    public void goToProfile() {
        StudentLayoutController.getInstance().showProfile();
    }

    @FXML
    public void goToSettings() {
        StudentLayoutController.getInstance().showSettings();
    }

    @FXML
    public void goToBrowseCourses() {
        StudentLayoutController controller = StudentLayoutController.getInstance();
        if (controller != null) {
            controller.showCourseHub();
        }
    }
}
