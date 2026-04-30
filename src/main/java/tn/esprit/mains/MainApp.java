package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.esprit.controllers.AdminPanelController;
import tn.esprit.controllers.ProfessorLayoutController;
import tn.esprit.controllers.StudentLayoutController;
import tn.esprit.entities.User;
import tn.esprit.services.SessionService;
import tn.esprit.services.UserService;
import tn.esprit.tools.Loaders;
import tn.esprit.tools.MyConnection;
import tn.esprit.tools.SessionStore;
import tn.esprit.tools.ThemeManager;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize database connection
        MyConnection.getInstance().getConnection();

        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        if (tryRestoreSession(primaryStage)) {
            return;
        }

        // Load the Login screen
        FXMLLoader loader = Loaders.loader(getClass(), "/fxml/Login.fxml");
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
        ThemeManager.applyTheme(scene);

        primaryStage.setTitle("Sign In - SkillORA");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private boolean tryRestoreSession(Stage stage) {
        String token = SessionStore.load();
        if (token == null) return false;
        User user = new SessionService().validate(token);
        if (user == null) {
            SessionStore.clear();
            return false;
        }
        try {
            String role = new UserService().getUserRole(user.getId());
            String normalizedRole = role == null ? "student" : role.toLowerCase().replace("role_", "");
            boolean isStudent = normalizedRole.equals("student");
            boolean isProfessor = normalizedRole.equals("professor");
            String fxmlPath;
            String title;
            if (isStudent) {
                fxmlPath = "/fxml/StudentLayout.fxml"; title = "SkillORA";
                StudentLayoutController.setCurrentUser(user);
            } else if (isProfessor) {
                fxmlPath = "/fxml/ProfessorLayout.fxml"; title = "SkillORA - Faculty";
                ProfessorLayoutController.setCurrentUser(user);
            } else {
                fxmlPath = "/fxml/AdminPanel.fxml"; title = "SkillORA - Admin Panel";
                AdminPanelController.setCurrentUser(user);
            }

            FXMLLoader loader = Loaders.loader(getClass(), fxmlPath);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            SessionStore.clear();
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
