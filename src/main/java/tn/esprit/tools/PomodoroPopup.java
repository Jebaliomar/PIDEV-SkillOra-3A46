package tn.esprit.tools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Singleton non-modal Pomodoro popup. The same window is reused across
 * navigation so the timer survives screen changes.
 */
public final class PomodoroPopup {

    private static Stage stage;

    private PomodoroPopup() {}

    public static void toggle(Window owner) {
        if (stage != null && stage.isShowing()) {
            stage.hide();
            return;
        }
        show(owner);
    }

    public static void show(Window owner) {
        try {
            if (stage == null) {
                FXMLLoader loader = Loaders.loader(PomodoroPopup.class, "/fxml/PomodoroPopup.fxml");
                Parent root = loader.load();
                Scene scene = new Scene(root);
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
                scene.getStylesheets().add(PomodoroPopup.class.getResource("/styles/style.css").toExternalForm());

                stage = new Stage(StageStyle.TRANSPARENT);
                stage.initModality(Modality.NONE);
                stage.setAlwaysOnTop(true);
                stage.setResizable(false);
                stage.setTitle("Pomodoro");
                stage.setScene(scene);
                ThemeManager.applyTheme(scene);
                if (owner != null) {
                    stage.setX(owner.getX() + Math.max(0, owner.getWidth() - 380));
                    stage.setY(owner.getY() + 80);
                }
            }
            if (!stage.isShowing()) stage.show();
            stage.toFront();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isOpen() {
        return stage != null && stage.isShowing();
    }
}
