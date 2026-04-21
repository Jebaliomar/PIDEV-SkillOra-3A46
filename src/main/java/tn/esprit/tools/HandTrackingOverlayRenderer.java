package tn.esprit.tools;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.List;

public class HandTrackingOverlayRenderer {

    private static final int[][] CONNECTIONS = {
            {0, 1}, {1, 2}, {2, 3}, {3, 4},
            {0, 5}, {5, 6}, {6, 7}, {7, 8},
            {5, 9}, {9, 10}, {10, 11}, {11, 12},
            {9, 13}, {13, 14}, {14, 15}, {15, 16},
            {13, 17}, {17, 18}, {18, 19}, {19, 20},
            {0, 17}, {5, 9}, {9, 13}, {13, 17}
    };

    public void render(GraphicsContext gc, double width, double height, TrackedHandState state) {
        gc.clearRect(0, 0, width, height);
        if (state == null || !state.detected() || state.landmarks() == null || state.landmarks().isEmpty()) {
            return;
        }

        List<HandLandmarkPoint> landmarks = state.landmarks();
        gc.setStroke(Color.rgb(120, 190, 255, 0.85));
        gc.setLineWidth(2.0);

        if (landmarks.size() >= 21) {
            for (int[] connection : CONNECTIONS) {
                if (connection[0] >= landmarks.size() || connection[1] >= landmarks.size()) {
                    continue;
                }
                HandLandmarkPoint from = landmarks.get(connection[0]);
                HandLandmarkPoint to = landmarks.get(connection[1]);
                gc.strokeLine(from.x() * width, from.y() * height, to.x() * width, to.y() * height);
            }
        }

        for (int i = 0; i < landmarks.size(); i++) {
            HandLandmarkPoint point = landmarks.get(i);
            double px = point.x() * width;
            double py = point.y() * height;

            boolean highlighted = state.drawingPoint() != null
                    && Math.abs(point.x() - state.drawingPoint().x()) < 0.0001
                    && Math.abs(point.y() - state.drawingPoint().y()) < 0.0001;
            gc.setFill(highlighted ? Color.rgb(255, 77, 79, 0.95) : Color.rgb(114, 194, 255, 0.95));
            double size = highlighted ? 10 : 7;
            gc.fillOval(px - size / 2, py - size / 2, size, size);
        }

        if (state.drawingPoint() != null) {
            double px = state.drawingPoint().x() * width;
            double py = state.drawingPoint().y() * height;
            gc.setStroke(Color.rgb(255, 77, 79, 0.95));
            gc.setLineWidth(3.0);
            gc.strokeOval(px - 16, py - 16, 32, 32);
            gc.strokeOval(px - 8, py - 8, 16, 16);
        }
    }
}
