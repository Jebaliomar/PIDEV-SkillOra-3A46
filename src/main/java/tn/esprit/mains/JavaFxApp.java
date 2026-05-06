package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.AppWindow;

import java.io.IOException;

public class JavaFxApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                JavaFxApp.class.getResource("/tn/esprit/views/auth/login-view.fxml")
        );

        Scene scene = AppWindow.createScene(fxmlLoader.load());
        AppWindow.show(stage, scene, "SkillOra - Login", true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
