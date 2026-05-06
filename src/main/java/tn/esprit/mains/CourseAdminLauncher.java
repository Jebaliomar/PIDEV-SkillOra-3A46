package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.controllers.AdminPanelController;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.ThemeManager;

import java.util.Objects;

public class CourseAdminLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AdminPanelController.setInitialView(AdminPanelController.InitialView.COURSES);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminPanel.fxml"));
        Scene scene = AppWindow.createScene(loader.load());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/event.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/salle.css")).toExternalForm());
        ThemeManager.applyTheme(scene);

        AppWindow.show(stage, scene, "SkillORA - Course Admin", true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
