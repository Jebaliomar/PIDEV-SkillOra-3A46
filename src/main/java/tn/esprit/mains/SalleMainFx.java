package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.AppWindow;
import tn.esprit.tools.ThemeManager;

public class SalleMainFx extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/SalleDashboard.fxml"));
        Parent root = loader.load();

        Scene scene = AppWindow.createScene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
        ThemeManager.applyTheme(scene);

        AppWindow.show(stage, scene, "Skillora Salles", true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

