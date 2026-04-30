package tn.esprit.tools;

import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public final class LanguageManager {

    public enum Language {
        EN("English",  Locale.ROOT,               NodeOrientation.LEFT_TO_RIGHT),
        FR("Français", Locale.FRENCH,             NodeOrientation.LEFT_TO_RIGHT),
        AR("العربية",  new Locale("ar"),          NodeOrientation.RIGHT_TO_LEFT);

        public final String label;
        public final Locale locale;
        public final NodeOrientation orientation;

        Language(String label, Locale locale, NodeOrientation o) {
            this.label = label; this.locale = locale; this.orientation = o;
        }
    }

    private static final Preferences prefs = Preferences.userNodeForPackage(LanguageManager.class);
    private static final String K_LANG = "skillora_language";
    private static Language current = read();

    private LanguageManager() {}

    private static Language read() {
        try { return Language.valueOf(prefs.get(K_LANG, Language.EN.name())); }
        catch (Exception e) { return Language.EN; }
    }

    public static Language getCurrent() { return current; }

    public static void setLanguage(Language lang) {
        current = lang;
        prefs.put(K_LANG, lang.name());
    }

    public static ResourceBundle bundle() {
        // Disable fallback to JVM default locale - otherwise EN on a French system
        // would fall back to messages_fr.properties.
        ResourceBundle.Control noFallback = ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_DEFAULT);
        return ResourceBundle.getBundle("i18n.messages", current.locale, noFallback);
    }

    public static void applyToScene(Scene scene) {
        if (scene == null) return;
        scene.setNodeOrientation(current.orientation);
    }
}
