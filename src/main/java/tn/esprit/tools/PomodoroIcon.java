package tn.esprit.tools;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class PomodoroIcon {
    private PomodoroIcon() {}

    /** Tiny tomato icon for the toolbar button. */
    public static Group small() {
        return tomato(1.0);
    }

    /** Large animated tomato for the popup, with a cute face. */
    public static Group large() {
        Group g = tomato(6.0);
        // Face on the body: body center is (12*6, 14*6) = (72, 84)
        SVGPath leftEye = new SVGPath();
        leftEye.setContent("M 60 80 Q 64 72 68 80");
        SVGPath rightEye = new SVGPath();
        rightEye.setContent("M 76 80 Q 80 72 84 80");
        SVGPath smile = new SVGPath();
        smile.setContent("M 63 92 Q 72 100 81 92");
        for (SVGPath p : new SVGPath[]{leftEye, rightEye, smile}) {
            p.setStroke(Color.web("#7f1d1d"));
            p.setFill(Color.TRANSPARENT);
            p.setStrokeLineCap(StrokeLineCap.ROUND);
            p.setStrokeWidth(3.4);
        }
        Circle leftCheek = new Circle(56, 92, 5, Color.web("#fda4af", 0.75));
        Circle rightCheek = new Circle(88, 92, 5, Color.web("#fda4af", 0.75));
        g.getChildren().addAll(leftCheek, rightCheek, leftEye, rightEye, smile);
        return g;
    }

    private static Group tomato(double scale) {
        // Body
        Circle body = new Circle(12 * scale, 14 * scale, 8.5 * scale);
        body.setFill(new RadialGradient(0, 0, 0.35, 0.35, 0.9, true, null,
                new Stop(0, Color.web("#ff6b6b")),
                new Stop(0.6, Color.web("#ef4444")),
                new Stop(1, Color.web("#b91c1c"))));
        body.setStroke(Color.web("#7f1d1d", 0.6));
        body.setStrokeWidth(0.6 * scale);

        // Glossy highlight
        Ellipse shine = new Ellipse(9.5 * scale, 11 * scale, 2.6 * scale, 1.6 * scale);
        shine.setFill(Color.web("#ffffff", 0.55));
        shine.setRotate(-25);

        // Leaf (top)
        SVGPath leaf = new SVGPath();
        leaf.setContent(
                "M12 6 " +
                "C 9 4, 7 4.5, 6.5 6.5 " +
                "C 8 6.2, 9.5 6.5, 10.5 7.4 " +
                "C 10.2 6, 10.6 5, 12 4.5 " +
                "C 13.4 5, 13.8 6, 13.5 7.4 " +
                "C 14.5 6.5, 16 6.2, 17.5 6.5 " +
                "C 17 4.5, 15 4, 12 6 Z");
        leaf.setScaleX(scale);
        leaf.setScaleY(scale);
        leaf.setTranslateX(-12 * (1 - scale));
        leaf.setTranslateY(-6 * (1 - scale));
        leaf.setFill(new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, Color.web("#86efac")),
                new Stop(1, Color.web("#16a34a"))));
        leaf.setStroke(Color.web("#14532d", 0.7));
        leaf.setStrokeWidth(0.4 * scale);
        leaf.setStrokeLineJoin(StrokeLineJoin.ROUND);
        leaf.setStrokeLineCap(StrokeLineCap.ROUND);

        // Stem
        SVGPath stem = new SVGPath();
        stem.setContent("M12 6 L12 4.2");
        stem.setScaleX(scale);
        stem.setScaleY(scale);
        stem.setTranslateX(-12 * (1 - scale));
        stem.setTranslateY(-5 * (1 - scale));
        stem.setStroke(Color.web("#15803d"));
        stem.setStrokeWidth(0.9 * scale);
        stem.setStrokeLineCap(StrokeLineCap.ROUND);

        Group g = new Group(body, shine, leaf, stem);
        return g;
    }

    /** Simple closed-eye smiley face overlay used on the big tomato. */
    public static Group cuteFace(double cx, double cy, double size) {
        // Eyes (closed/squinting arcs) and a tiny smile
        SVGPath leftEye = new SVGPath();
        leftEye.setContent("M -3 0 Q -2 -2 -1 0");
        SVGPath rightEye = new SVGPath();
        rightEye.setContent("M 1 0 Q 2 -2 3 0");
        SVGPath smile = new SVGPath();
        smile.setContent("M -2.2 2.4 Q 0 4.4 2.2 2.4");

        for (SVGPath p : new SVGPath[]{leftEye, rightEye, smile}) {
            p.setStroke(Color.web("#7f1d1d"));
            p.setFill(Color.TRANSPARENT);
            p.setStrokeLineCap(StrokeLineCap.ROUND);
            p.setStrokeWidth(1.4);
        }

        // Cheeks
        Circle leftCheek = new Circle(-3.6, 1.6, 1.1, Color.web("#fda4af", 0.75));
        Circle rightCheek = new Circle(3.6, 1.6, 1.1, Color.web("#fda4af", 0.75));

        Group face = new Group(leftCheek, rightCheek, leftEye, rightEye, smile);
        face.setScaleX(size);
        face.setScaleY(size);
        face.setTranslateX(cx);
        face.setTranslateY(cy);
        return face;
    }
}
