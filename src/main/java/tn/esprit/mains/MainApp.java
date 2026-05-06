package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.AppConfig;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.ThemeManager;

import java.util.Objects;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppConfig.loadEnvironment();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Scene scene = AppWindow.createScene(loader.load());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());
        ThemeManager.applyTheme(scene);

        AppWindow.show(stage, scene, "Sign In - SkillORA", true);
    }

    public static void main(String[] args) {
        AppConfig.loadEnvironment();
        launch(args);
    }
}
