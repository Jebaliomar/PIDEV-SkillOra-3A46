package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.ThemeManager;

public class SalleMainFx extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/event/SalleDashboard.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 780);
        scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
        ThemeManager.applyTheme(scene);

        stage.setTitle("Skillora Salles");
        stage.setMinWidth(1024);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

