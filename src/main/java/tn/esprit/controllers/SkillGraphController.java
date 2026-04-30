package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;
import tn.esprit.entities.User;
import tn.esprit.tools.SkillStore;
import tn.esprit.tools.SkillStore.Category;
import tn.esprit.tools.SkillStore.Skill;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SkillGraphController implements Initializable {

    @FXML private Pane graphPane;
    @FXML private VBox detailCard;
    @FXML private Label detailTitle;
    @FXML private Label detailCategory;
    @FXML private Label detailProgress;
    @FXML private ProgressBar detailBar;
    @FXML private Label detailDesc;

    private List<Category> categories = new ArrayList<>();
    private Skill selected;
    private Category selectedCategory;
    private int userId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = StudentLayoutController.getCurrentUser();
        userId = u != null ? u.getId() : 0;
        categories = SkillStore.load(userId);

        graphPane.widthProperty().addListener((o, a, v) -> rebuild());
        graphPane.heightProperty().addListener((o, a, v) -> rebuild());

        // Re-render when theme switches so curve / shadow opacities adapt to dark mode.
        graphPane.sceneProperty().addListener((o, a, scene) -> {
            if (scene == null) return;
            scene.getStylesheets().addListener((javafx.collections.ListChangeListener<String>) c -> rebuild());
        });
    }

    private void persist() {
        try { SkillStore.save(userId, categories); } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleAdd() {
        SkillEditorController.Result r = SkillEditorController.open(
                graphPane.getScene().getWindow(), categories, null, null);
        if (r.action == SkillEditorController.Action.SAVED) {
            persist(); rebuild();
        }
    }

    @FXML public void handleEdit() {
        if (selected == null) return;
        SkillEditorController.Result r = SkillEditorController.open(
                graphPane.getScene().getWindow(), categories, selectedCategory, selected);
        if (r.action == SkillEditorController.Action.SAVED) {
            persist(); rebuild();
            showDetail(r.skill, r.category);
        } else if (r.action == SkillEditorController.Action.DELETED) {
            r.category.skills.remove(r.skill);
            persist(); rebuild();
            handleCloseDetail();
        }
    }

    @FXML public void handleCloseDetail() {
        detailCard.setVisible(false);
        detailCard.setManaged(false);
        selected = null;
        selectedCategory = null;
    }

    private void rebuild() {
        double w = graphPane.getWidth();
        double h = graphPane.getHeight();
        if (w <= 0 || h <= 0) return;
        graphPane.getChildren().clear();

        boolean dark = ThemeManager.isDarkMode();
        double curveAlpha = dark ? 0.85 : 0.55;
        double curveWidthMain = dark ? 3.6 : 3.2;
        double curveWidthLeaf = dark ? 2.4 : 2.0;
        double shadowAlpha = dark ? 0.65 : 0.45;

        double cx = w / 2.0;
        double cy = h / 2.0 + 30;

        StackPane hub = makeHub(overallAvg(), shadowAlpha);
        place(hub, cx, cy);

        int n = categories.size();
        if (n == 0) return;
        double catRadius = Math.min(w, h) * 0.22 + 60;
        double skillRadius = catRadius + 160;

        List<StackPane> catNodes = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Category cat = categories.get(i);
            Color color = parseColor(cat.colorHex);
            double angle = -Math.PI / 2 + (2 * Math.PI * i / n);
            double catX = cx + catRadius * Math.cos(angle);
            double catY = cy + catRadius * Math.sin(angle);

            graphPane.getChildren().add(makeCurve(cx, cy, catX, catY, color, curveWidthMain, curveAlpha));

            StackPane catNode = makeCircleNode(cat.name, avg(cat) + "%", color, 56, shadowAlpha);
            place(catNode, catX, catY);
            catNodes.add(catNode);

            int m = cat.skills.size();
            if (m == 0) continue;
            double arcSpan = Math.PI / 2.4;
            double arcStart = angle - arcSpan / 2;
            for (int j = 0; j < m; j++) {
                Skill s = cat.skills.get(j);
                double t = m == 1 ? 0.5 : (double) j / (m - 1);
                double sa = arcStart + t * arcSpan;
                double sx = cx + skillRadius * Math.cos(sa);
                double sy = cy + skillRadius * Math.sin(sa);

                graphPane.getChildren().add(makeCurve(catX, catY, sx, sy,
                        color.deriveColor(0, 1, 1.15, 1), curveWidthLeaf, curveAlpha));

                StackPane skillNode = makeSkillNode(s, cat, color, shadowAlpha);
                place(skillNode, sx, sy);
                graphPane.getChildren().add(skillNode);
            }
        }

        for (StackPane c : catNodes) graphPane.getChildren().add(c);
        graphPane.getChildren().add(hub);

        animateIn();
    }

    private int overallAvg() {
        if (categories.isEmpty()) return 0;
        double s = 0; int count = 0;
        for (Category c : categories) for (Skill k : c.skills) { s += k.progress; count++; }
        return count == 0 ? 0 : (int) (s / count);
    }

    private int avg(Category c) {
        if (c.skills.isEmpty()) return 0;
        return (int) c.skills.stream().mapToInt(s -> s.progress).average().orElse(0);
    }

    private StackPane makeHub(int avg, double shadowAlpha) {
        return makeCircleNode("My Skills", avg + "%", Color.web("#7c3aed"), 70, shadowAlpha);
    }

    private StackPane makeCircleNode(String label, String sub, Color color, double radius, double shadowAlpha) {
        Circle c = new Circle(radius);
        c.setFill(new LinearGradient(0, 0, 1, 1, true, null,
                new Stop(0, color.brighter()),
                new Stop(1, color)));
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(3);
        DropShadow shadow = new DropShadow(18, color.deriveColor(0, 1, 1, shadowAlpha));
        shadow.setOffsetY(4);
        c.setEffect(shadow);

        Label name = new Label(label);
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label sublabel = new Label(sub);
        sublabel.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 11px;");
        VBox text = new VBox(2, name, sublabel);
        text.setStyle("-fx-alignment: center;");

        StackPane node = new StackPane(c, text);
        node.setCursor(Cursor.HAND);
        attachHover(node, 1.08);
        return node;
    }

    private StackPane makeSkillNode(Skill s, Category cat, Color color, double shadowAlpha) {
        double radius = 38 + s.progress * 0.18;
        Circle c = new Circle(radius);
        c.setFill(new LinearGradient(0, 0, 1, 1, true, null,
                new Stop(0, color.deriveColor(0, 0.8, 1.25, 1)),
                new Stop(1, color)));
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(2.5);
        DropShadow shadow = new DropShadow(12, color.deriveColor(0, 1, 1, shadowAlpha * 0.8));
        shadow.setOffsetY(3);
        c.setEffect(shadow);

        Label name = new Label(s.name);
        name.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11.5px;");
        Label pct = new Label(s.progress + "%");
        pct.setStyle("-fx-text-fill: rgba(255,255,255,0.9); -fx-font-size: 10.5px;");
        VBox text = new VBox(1, name, pct);
        text.setStyle("-fx-alignment: center;");

        StackPane node = new StackPane(c, text);
        node.setCursor(Cursor.HAND);
        attachHover(node, 1.12);
        node.setOnMouseClicked(e -> showDetail(s, cat));
        return node;
    }

    private void attachHover(StackPane node, double scale) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(140), node);
        grow.setToX(scale); grow.setToY(scale);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(140), node);
        shrink.setToX(1.0); shrink.setToY(1.0);
        node.setOnMouseEntered(e -> grow.playFromStart());
        node.setOnMouseExited(e -> shrink.playFromStart());
    }

    private CubicCurve makeCurve(double x1, double y1, double x2, double y2, Color color, double width, double alpha) {
        CubicCurve curve = new CubicCurve();
        curve.setStartX(x1); curve.setStartY(y1);
        curve.setEndX(x2);   curve.setEndY(y2);
        double midX = (x1 + x2) / 2;
        curve.setControlX1(midX); curve.setControlY1(y1);
        curve.setControlX2(midX); curve.setControlY2(y2);
        curve.setStroke(color.deriveColor(0, 1, 1, alpha));
        curve.setStrokeWidth(width);
        curve.setStrokeLineCap(StrokeLineCap.ROUND);
        curve.setFill(null);
        return curve;
    }

    private void place(StackPane node, double x, double y) {
        node.layoutXProperty().bind(node.widthProperty().multiply(-0.5).add(x));
        node.layoutYProperty().bind(node.heightProperty().multiply(-0.5).add(y));
    }

    private void showDetail(Skill s, Category cat) {
        selected = s;
        selectedCategory = cat;
        Color color = parseColor(cat.colorHex);
        detailTitle.setText(s.name);
        detailCategory.setText(cat.name);
        detailCategory.setStyle("-fx-text-fill: " + cat.colorHex + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        detailProgress.setText(s.progress + "% complete");
        detailBar.setProgress(s.progress / 100.0);
        detailBar.setStyle("-fx-accent: " + cat.colorHex + ";");
        detailDesc.setText(s.desc == null ? "" : s.desc);

        if (!detailCard.isVisible()) {
            detailCard.setVisible(true);
            detailCard.setManaged(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), detailCard);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();
        }
    }

    private void animateIn() {
        for (javafx.scene.Node n : graphPane.getChildren()) {
            FadeTransition ft = new FadeTransition(Duration.millis(360), n);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();
        }
    }

    private Color parseColor(String hex) {
        try { return Color.web(hex); } catch (Exception e) { return Color.web("#7c3aed"); }
    }
}
