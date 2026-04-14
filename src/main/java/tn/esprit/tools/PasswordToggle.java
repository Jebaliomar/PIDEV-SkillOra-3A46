package tn.esprit.tools;

import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public final class PasswordToggle {
    private PasswordToggle() {}

    public static void wire(PasswordField pwd, TextField shown, Button eye) {
        shown.textProperty().bindBidirectional(pwd.textProperty());
        shown.setVisible(false);
        shown.setManaged(false);
        eye.setGraphic(AppIcons.eye());
        eye.setOnAction(e -> toggle(pwd, shown, eye));
    }

    private static void toggle(PasswordField pwd, TextField shown, Button eye) {
        boolean showing = shown.isVisible();
        if (showing) {
            shown.setVisible(false);
            shown.setManaged(false);
            pwd.setVisible(true);
            pwd.setManaged(true);
            pwd.requestFocus();
            pwd.positionCaret(pwd.getText() == null ? 0 : pwd.getText().length());
            eye.setGraphic(AppIcons.eye());
        } else {
            pwd.setVisible(false);
            pwd.setManaged(false);
            shown.setVisible(true);
            shown.setManaged(true);
            shown.requestFocus();
            shown.positionCaret(shown.getText() == null ? 0 : shown.getText().length());
            eye.setGraphic(AppIcons.eyeOff());
        }
    }
}
