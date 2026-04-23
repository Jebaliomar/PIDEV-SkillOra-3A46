package tn.esprit.tools;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class ThemeIcon {
    private ThemeIcon() {}

    private static final String MOON_PATH =
            "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z";

    private static final String SUN_PATH =
            "M12 4V2 M12 22v-2 M4 12H2 M22 12h-2 " +
            "M5.6 5.6L4.2 4.2 M19.8 19.8l-1.4-1.4 " +
            "M18.4 5.6l1.4-1.4 M4.2 19.8l1.4-1.4 " +
            "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10z";

    public static SVGPath moon() {
        SVGPath p = new SVGPath();
        p.setContent(MOON_PATH);
        p.setFill(Color.web("#c4b5fd"));
        p.setStroke(Color.web("#c4b5fd"));
        p.setStrokeWidth(1.4);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return p;
    }

    public static SVGPath sun() {
        SVGPath p = new SVGPath();
        p.setContent(SUN_PATH);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web("#fbbf24"));
        p.setStrokeWidth(1.8);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return p;
    }
}
