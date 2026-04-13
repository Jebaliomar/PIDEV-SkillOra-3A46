package tn.esprit.tools;

import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class UserGalaxy {

    public static class StarProfile {
        public final Color color;
        public final double size;
        public final String label;
        public StarProfile(Color color, double size, String label) {
            this.color = color; this.size = size; this.label = label;
        }
    }

    private static final StarProfile ADMIN    = new StarProfile(Color.web("#ffd700"), 0.75, "Admin");
    private static final StarProfile BANNED   = new StarProfile(Color.web("#ff3333"), 0.60, "Banned");
    private static final StarProfile NEW_USER = new StarProfile(Color.web("#60a5fa"), 0.66, "New");
    private static final StarProfile ACTIVE   = new StarProfile(Color.web("#34d399"), 0.69, "Active");
    private static final StarProfile INACTIVE = new StarProfile(Color.web("#6b7280"), 0.54, "Inactive");

    private final Group world = new Group();
    private final Group galaxy = new Group();
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate rotateX = new Rotate(-15, Rotate.X_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final List<Sphere> stars = new ArrayList<>();
    private final Map<Sphere, Map<String, Object>> starData = new HashMap<>();

    private double cameraDistance = 25;
    private double anchorX, anchorY, anchorAngleX, anchorAngleY;
    private Sphere selected;
    private double selectedBaseRadius;

    private StackPane root;
    private javafx.scene.control.Label infoCard;

    public StackPane build(List<Map<String, Object>> users, double width, double height) {
        galaxy.getTransforms().addAll(rotateY, rotateX);
        world.getChildren().add(galaxy);

        addBackgroundStars();
        addUserStars(users);

        AmbientLight ambient = new AmbientLight(Color.web("#404060"));
        PointLight key = new PointLight(Color.web("#aab4ff"));
        key.setTranslateZ(-30);
        world.getChildren().addAll(ambient, key);

        camera.setNearClip(0.1);
        camera.setFarClip(1000);
        camera.setFieldOfView(55);
        camera.setTranslateZ(-cameraDistance);

        SubScene sub = new SubScene(world, width, height, true, SceneAntialiasing.BALANCED);
        sub.setFill(Color.web("#05060f"));
        sub.setCamera(camera);

        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0a0a1a, #1a0a2e);");
        root.getChildren().add(sub);

        infoCard = new javafx.scene.control.Label("");
        infoCard.setStyle(
            "-fx-background-color: rgba(15,15,30,0.92);" +
            "-fx-text-fill: #e5e7eb;" +
            "-fx-padding: 12 16 12 16;" +
            "-fx-background-radius: 8;" +
            "-fx-border-color: rgba(124,58,237,0.6);" +
            "-fx-border-radius: 8;" +
            "-fx-border-width: 1;" +
            "-fx-font-size: 12px;"
        );
        infoCard.setVisible(false);
        infoCard.setManaged(false);
        StackPane.setAlignment(infoCard, javafx.geometry.Pos.TOP_LEFT);
        StackPane.setMargin(infoCard, new javafx.geometry.Insets(16, 0, 0, 16));
        root.getChildren().add(infoCard);

        sub.widthProperty().bind(root.widthProperty());
        sub.heightProperty().bind(root.heightProperty());

        installMouseControls(sub);
        startAnimation();

        return root;
    }

    private void addBackgroundStars() {
        Random rng = new Random(42);
        PhongMaterial whiteMat = new PhongMaterial(Color.web("#ffffff", 0.7));
        for (int i = 0; i < 400; i++) {
            double r = 60 + rng.nextDouble() * 140;
            double theta = rng.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rng.nextDouble() - 1);
            Sphere s = new Sphere(0.12 + rng.nextDouble() * 0.18);
            s.setMaterial(whiteMat);
            s.setTranslateX(r * Math.sin(phi) * Math.cos(theta));
            s.setTranslateY(r * Math.cos(phi));
            s.setTranslateZ(r * Math.sin(phi) * Math.sin(theta));
            world.getChildren().add(s);
        }
    }

    private void addUserStars(List<Map<String, Object>> users) {
        int total = users.size();
        double golden = (1 + Math.sqrt(5)) / 2;
        for (int i = 0; i < total; i++) {
            Map<String, Object> u = users.get(i);
            StarProfile prof = profileFor(u);

            double idx = i + 0.5;
            double phi = Math.acos(1 - (2 * idx) / Math.max(total, 1));
            double theta = (2 * Math.PI * idx) / golden;
            double r = 8 + (i / (double) Math.max(total, 1)) * 14;

            Sphere star = new Sphere(prof.size);
            PhongMaterial mat = new PhongMaterial(prof.color);
            mat.setSpecularColor(prof.color.brighter());
            mat.setSelfIlluminationMap(glowTexture(prof.color));
            star.setMaterial(mat);
            star.setTranslateX(r * Math.sin(phi) * Math.cos(theta));
            star.setTranslateY(r * Math.cos(phi) * 0.4);
            star.setTranslateZ(r * Math.sin(phi) * Math.sin(theta));

            Map<String, Object> data = new HashMap<>(u);
            data.put("baseRadius", prof.size);
            data.put("phase", Math.random() * Math.PI * 2);
            data.put("profile", prof);
            starData.put(star, data);
            stars.add(star);
            galaxy.getChildren().add(star);
        }
    }

    private StarProfile profileFor(Map<String, Object> u) {
        String role = String.valueOf(u.getOrDefault("role", "user"));
        boolean banned = Boolean.TRUE.equals(u.get("banned"));
        int daysOld = u.get("daysOld") instanceof Integer ? (int) u.get("daysOld") : 0;
        if ("admin".equalsIgnoreCase(role)) return ADMIN;
        if (banned) return BANNED;
        if (daysOld < 7) return NEW_USER;
        if (daysOld > 90) return INACTIVE;
        return ACTIVE;
    }

    private Image glowTexture(Color c) {
        int size = 64;
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        double cx = size / 2.0, cy = size / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - cx, dy = y - cy;
                double d = Math.sqrt(dx * dx + dy * dy) / (size / 2.0);
                double a = Math.max(0, 1 - d);
                a = a * a;
                pw.setColor(x, y, new Color(c.getRed(), c.getGreen(), c.getBlue(), a));
            }
        }
        return img;
    }

    private void installMouseControls(SubScene sub) {
        sub.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });
        sub.setOnMouseDragged(e -> {
            rotateY.setAngle(anchorAngleY + (e.getSceneX() - anchorX) * 0.4);
            rotateX.setAngle(anchorAngleX - (e.getSceneY() - anchorY) * 0.4);
        });
        sub.setOnScroll(e -> {
            cameraDistance = Math.max(10, Math.min(60, cameraDistance - e.getDeltaY() * 0.05));
            camera.setTranslateZ(-cameraDistance);
        });
        sub.setOnMouseClicked(e -> {
            Node picked = e.getPickResult().getIntersectedNode();
            if (picked instanceof Sphere && starData.containsKey(picked)) {
                selectStar((Sphere) picked);
            } else {
                clearSelection();
            }
        });
    }

    private void selectStar(Sphere s) {
        clearSelection();
        selected = s;
        selectedBaseRadius = (double) starData.get(s).get("baseRadius");
        Map<String, Object> d = starData.get(s);
        StarProfile prof = (StarProfile) d.get("profile");
        String name = String.valueOf(d.getOrDefault("name", "Unknown"));
        int daysOld = d.get("daysOld") instanceof Integer ? (int) d.get("daysOld") : 0;
        boolean banned = Boolean.TRUE.equals(d.get("banned"));
        int id = d.get("id") instanceof Integer ? (int) d.get("id") : 0;

        String text = name + "\n" +
                      prof.label + "  -  " + daysOld + " days old\n" +
                      "ID #" + id + (banned ? "  -  BANNED" : "");
        infoCard.setText(text);
        infoCard.setVisible(true);
        infoCard.setManaged(true);
    }

    private void clearSelection() {
        if (selected != null) {
            selected.setRadius(selectedBaseRadius);
        }
        selected = null;
        infoCard.setVisible(false);
        infoCard.setManaged(false);
    }

    private void startAnimation() {
        final long start = System.nanoTime();
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                double t = (now - start) / 1_000_000_000.0;
                rotateY.setAngle(rotateY.getAngle() + 0.05);
                for (Sphere s : stars) {
                    Map<String, Object> d = starData.get(s);
                    double base = (double) d.get("baseRadius");
                    double phase = (double) d.get("phase");
                    double pulse = 1 + 0.12 * Math.sin(t * 0.9 + phase);
                    s.setRadius(base * pulse);
                }
                if (selected != null) {
                    selected.setRadius(selectedBaseRadius * 1.6);
                }
            }
        }.start();
    }
}
