package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ForumCrudMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(ForumCrudMain.class.getResource("/tn/esprit/forum-crud-view.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 760);

        stage.setTitle("Forum CRUD Showcase");
        stage.setScene(scene);
        stage.setMinWidth(1080);
        stage.setMinHeight(680);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
