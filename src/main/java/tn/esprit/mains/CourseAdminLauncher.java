package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.tools.ThemeManager;

import java.util.Objects;

public class CourseAdminLauncher extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin/admin_shell.fxml"));
        Scene scene = new Scene(loader.load(), 1240, 760);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/atlantafx/base/theme/primer-light.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/macos-theme.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/style.css")).toExternalForm());
        ThemeManager.applyTheme(scene);

        stage.setTitle("SkillORA - Course Admin");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
