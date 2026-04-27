package tn.esprit.tools;

import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class AppIcons {
    private AppIcons() {}

    private static final String EYE =
            "M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z";

    private static final String EYE_OFF =
            "M17.94 17.94A10.94 10.94 0 0 1 12 19c-7 0-11-7-11-7a19.9 19.9 0 0 1 5.06-5.94 " +
            "M9.9 4.24A10.94 10.94 0 0 1 12 4c7 0 11 7 11 7a19.9 19.9 0 0 1-3.17 4.31 " +
            "M14.12 14.12a3 3 0 1 1-4.24-4.24 M1 1l22 22";

    private static final String USER =
            "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 " +
            "M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z";

    private static final String GEAR =
            "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z " +
            "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
            "1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z";

    private static final String PLUS = "M12 5v14 M5 12h14";
    private static final String ARROW_LEFT = "M19 12H5 M12 19l-7-7 7-7";
    private static final String OPEN = "M5 12h14 M13 6l6 6-6 6";
    private static final String EDIT = "M12 20h9 M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4 12.5-12.5z";
    private static final String TRASH = "M3 6h18 M8 6V4h8v2 M19 6l-1 14H6L5 6 M10 11v6 M14 11v6";
    private static final String BOOK_OPEN =
            "M2 4.5A3 3 0 0 1 5 2h6v20H5a3 3 0 0 0-3 3V4.5z " +
            "M22 4.5A3 3 0 0 0 19 2h-6v20h6a3 3 0 0 1 3 3V4.5z";

    public static SVGPath eye()    { return stroke(EYE,     "#64748b", 1.6); }
    public static SVGPath eyeOff() { return stroke(EYE_OFF, "#64748b", 1.6); }
    public static SVGPath user()   { return stroke(USER,    "#0891b2", 1.7); }
    public static SVGPath gear()   { return stroke(GEAR,    "#475569", 1.5); }
    public static SVGPath plus()   { return stroke(PLUS,    "#ffffff", 2.2); }
    public static SVGPath back()   { return stroke(ARROW_LEFT, "#2563eb", 2.0); }
    public static SVGPath open()   { return stroke(OPEN,    "#2563eb", 2.0); }
    public static SVGPath edit()   { return stroke(EDIT,    "#2563eb", 1.8); }
    public static SVGPath trash()  { return stroke(TRASH,   "#b91c1c", 1.8); }
    public static SVGPath lessons(){ return stroke(BOOK_OPEN, "#2563eb", 1.8); }

    private static SVGPath stroke(String path, String color, double width) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.setFill(Color.TRANSPARENT);
        p.setStroke(Color.web(color));
        p.setStrokeWidth(width);
        p.setStrokeLineCap(StrokeLineCap.ROUND);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return p;
    }
}
