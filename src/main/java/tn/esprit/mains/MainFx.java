package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.AppConfig;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.ThemeManager;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppConfig.loadEnvironment();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();

        Scene scene = AppWindow.createScene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        ThemeManager.applyTheme(scene);

        AppWindow.show(stage, scene, "Sign In - SkillORA", true);
    }

    public static void main(String[] args) {
        AppConfig.loadEnvironment();
        launch(args);
    }
}

