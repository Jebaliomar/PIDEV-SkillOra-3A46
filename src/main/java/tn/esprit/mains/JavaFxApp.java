package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class JavaFxApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                JavaFxApp.class.getResource("/tn/esprit/views/rendezvous/rendezvous-list.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 1160, 740);
        stage.setTitle("SkillOra - RendezVous");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
