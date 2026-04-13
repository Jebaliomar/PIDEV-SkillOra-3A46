package tn.esprit.tools;

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

    public static String displayName(String filename) {
        if (filename == null) return "";
        String name = filename.replace(".glb", "").replace("-", " ");
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
