package tn.esprit.tools;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class AvatarService {

    public static final List<String> MALE_AVATARS = Arrays.asList(
            "male-avatar.glb", "male-avatar1.glb", "male-avatar2.glb",
            "male-avatar3.glb", "male-avatar4.glb", "male-avatar5.glb",
            "male-avatar6.glb", "male-avatar7.glb"
    );

    public static final List<String> FEMALE_AVATARS = Arrays.asList(
            "female-avatar.glb", "female-avatar2.glb", "female-avatar3.glb"
    );

    /** Resolve the local avatars directory. Prefers <project>/avatars, falls back to sibling Symfony project. */
    public static File getAvatarsDir() {
        File local = new File(System.getProperty("user.dir"), "avatars");
        if (local.isDirectory()) return local;

        // Fallback: look for sibling Symfony project
        File fallback = new File("C:/Users/alare/OneDrive/Desktop/skillharbor (5)/skillharbor (3)/skillharbor/public/avatars");
        if (fallback.isDirectory()) return fallback;

        return local; // may not exist
    }

    public static File getAvatarFile(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        return new File(getAvatarsDir(), filename);
    }

    /** file:// URL for a glb file that can be loaded by Three.js inside WebView. */
    public static String getAvatarFileUrl(String filename) {
        File f = getAvatarFile(filename);
        if (f == null || !f.exists()) return null;
        return f.toURI().toString();
    }

    /** Build the viewer HTML URL with the glb src as a query parameter. */
    public static String buildViewerUrl(String filename) {
        String avatarUrl = getAvatarFileUrl(filename);
        if (avatarUrl == null) return null;
        String viewer = AvatarService.class.getResource("/webview/avatar-viewer.html").toExternalForm();
        String encoded = URLEncoder.encode(avatarUrl, StandardCharsets.UTF_8);
        return viewer + "?src=" + encoded;
    }

    public static String displayName(String filename) {
        if (filename == null) return "";
        String name = filename.replace(".glb", "").replace("-", " ");
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
