package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CourseAdminLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("CourseAdminLauncher.start() called");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/admin_shell.fxml"));
        Scene scene = new Scene(loader.load(), 1240, 760);
        scene.getStylesheets().add(getClass().getResource("/styles/macos-theme.css").toExternalForm());
        stage.setTitle("SkillORA");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
