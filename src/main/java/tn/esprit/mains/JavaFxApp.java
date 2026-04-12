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
                JavaFxApp.class.getResource("/tn/esprit/views/menu.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 900, 560);
        stage.setTitle("SkillOra - Menu");
        stage.setScene(scene);
        stage.setMinWidth(860);
        stage.setMinHeight(520);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
