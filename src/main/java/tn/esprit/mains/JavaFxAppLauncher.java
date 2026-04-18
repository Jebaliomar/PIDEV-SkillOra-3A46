package tn.esprit.mains;

import javafx.application.Application;

public final class JavaFxAppLauncher {

    private JavaFxAppLauncher() {
        // Utility launcher class.
    }

    public static void main(String[] args) {
        Application.launch(JavaFxApp.class, args);
    }
}
