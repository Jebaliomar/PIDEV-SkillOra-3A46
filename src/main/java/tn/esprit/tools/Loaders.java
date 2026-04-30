package tn.esprit.tools;

import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;

/**
 * Always-localized FXMLLoader factory. Use these instead of raw {@code FXMLLoader.load(url)}
 * so every screen automatically picks up the current language bundle.
 */
public final class Loaders {
    private Loaders() {}

    public static FXMLLoader loader(URL fxml) {
        FXMLLoader l = new FXMLLoader(fxml);
        l.setResources(LanguageManager.bundle());
        return l;
    }

    public static FXMLLoader loader(Class<?> base, String resourcePath) {
        return loader(base.getResource(resourcePath));
    }

    @SuppressWarnings("unchecked")
    public static <T> T load(Class<?> base, String resourcePath) throws IOException {
        return (T) loader(base, resourcePath).load();
    }
}
