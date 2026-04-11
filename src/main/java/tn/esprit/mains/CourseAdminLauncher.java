package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CourseAdminLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("CourseAdminLauncher.start() called");
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/courses/course_index.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 720);
        stage.setTitle("Skillora Admin - Courses");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
