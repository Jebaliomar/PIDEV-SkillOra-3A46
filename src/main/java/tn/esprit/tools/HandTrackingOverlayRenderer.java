package tn.esprit.tools;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

public class HandTrackingOverlayRenderer {

    private static final int MAX_TRAIL_POINTS = 8;
    private final Deque<HandLandmarkPoint> guideTrail = new ArrayDeque<>();
    private final CoordinateMapper coordinateMapper = new CoordinateMapper();
    private double canvasWidth = 640.0;
    private double canvasHeight = 480.0;

    /**
     * Met à jour les dimensions du canvas et du CoordinateMapper.
     * Doit être appelée avant chaque rendu ou lors du redimensionnement.
     *
     * @param width  largeur du canvas en pixels
     * @param height hauteur du canvas en pixels
     */
    public void updateCanvasDimensions(double width, double height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
        coordinateMapper.updateCanvasDimensions(width, height);
    }

    public void render(GraphicsContext gc, double width, double height, TrackedHandState state) {
        gc.clearRect(0, 0, width, height);
        if (state == null || !state.detected() || state.landmarks() == null || state.landmarks().isEmpty()) {
            guideTrail.clear();
            return;
        }

        // Mettre à jour les dimensions si elles ont changé
        if (width != canvasWidth || height != canvasHeight) {
            updateCanvasDimensions(width, height);
        }

        for (HandLandmarkPoint point : state.landmarks()) {
            // ✅ Convertir les coordonnées normalisées en pixels via CoordinateMapper
            HandLandmarkPoint pixelPoint = coordinateMapper.normalizedToCanvasPixels(point);
            double px = pixelPoint.x();
            double py = pixelPoint.y();
            gc.setFill(Color.rgb(114, 194, 255, 0.92));
            gc.fillOval(px - 4.0, py - 4.0, 8.0, 8.0);
        }

        if ((state.phase() == HandTrackingPhase.PINCH_ACTIVE || state.phase() == HandTrackingPhase.DRAWING)
                && state.guidePoint() != null) {
            HandLandmarkPoint guidePoint = state.guidePoint();
            pushTrail(guidePoint);
            renderTrail(gc, width, height);

            // ✅ Convertir le guide point via CoordinateMapper
            HandLandmarkPoint pixelGuidePoint = coordinateMapper.normalizedToCanvasPixels(guidePoint);
            double px = pixelGuidePoint.x();
            double py = pixelGuidePoint.y();

            gc.setFill(Color.rgb(255, 77, 79, 0.12));
            gc.fillOval(px - 22, py - 22, 44, 44);
            gc.setStroke(Color.rgb(255, 77, 79, 0.95));
            gc.setLineWidth(3.0);
            gc.strokeOval(px - 16, py - 16, 32, 32);
            gc.setLineWidth(2.0);
            gc.strokeOval(px - 8, py - 8, 16, 16);
            gc.strokeLine(px - 5, py, px + 5, py);
            gc.strokeLine(px, py - 5, px, py + 5);
        } else {
            guideTrail.clear();
        }
    }

    private void pushTrail(HandLandmarkPoint point) {
        guideTrail.addLast(point);
        while (guideTrail.size() > MAX_TRAIL_POINTS) {
            guideTrail.removeFirst();
        }
    }

    private void renderTrail(GraphicsContext gc, double width, double height) {
        int index = 0;
        int size = guideTrail.size();
        for (HandLandmarkPoint point : guideTrail) {
            double progress = size <= 1 ? 1.0 : (index + 1) / (double) size;
            double alpha = 0.10 + progress * 0.28;
            double radius = 4.0 + progress * 5.0;

            // ✅ Convertir via CoordinateMapper
            HandLandmarkPoint pixelPoint = coordinateMapper.normalizedToCanvasPixels(point);
            double px = pixelPoint.x();
            double py = pixelPoint.y();

            gc.setFill(Color.rgb(255, 77, 79, alpha));
            gc.fillOval(px - radius / 2.0, py - radius / 2.0, radius, radius);
            index++;
        }
    }
}
