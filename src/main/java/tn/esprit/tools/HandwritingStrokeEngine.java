package tn.esprit.tools;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class HandwritingStrokeEngine {

    private static final double SEGMENT_STEP = 2.6;
    private static final double ASSIST_RADIUS = 3.4;
    private static final double MAX_STEP_DISTANCE = 12.0;

    private final Canvas canvas;
    private final GraphicsContext graphics;
    private final double minDistance;
    private final double baseSmoothingFactor;
    private final double fastSmoothingFactor;
    private final double strokeWidth;
    private final Color strokeColor;
    private double lastRawX;
    private double lastRawY;
    private double lastFilteredX;
    private double lastFilteredY;
    private double lastMidX;
    private double lastMidY;
    private boolean strokeActive;
    private boolean hasInk;

    public HandwritingStrokeEngine(Canvas canvas, GraphicsContext graphics, CameraReservationConfig config) {
        this.canvas = canvas;
        this.graphics = graphics;
        this.minDistance = Math.max(0.35, config.getSmoothingMinMovement() / 2.7);
        this.baseSmoothingFactor = Math.max(0.08, Math.min(0.52, config.getSmoothingAlpha() * 0.28));
        this.fastSmoothingFactor = Math.max(baseSmoothingFactor, Math.min(0.76, config.getSmoothingAlpha() * 0.54));
        this.strokeWidth = Math.max(3.5, config.getCanvasLineWidth() * 1.6);
        this.strokeColor = Color.web(config.getCanvasLineColor());
        configureBrush();
    }

    public void beginStroke(double x, double y) {
        configureBrush();
        lastRawX = x;
        lastRawY = y;
        lastFilteredX = x;
        lastFilteredY = y;
        lastMidX = x;
        lastMidY = y;
        strokeActive = true;
        hasInk = true;

        graphics.strokeLine(x, y, x, y);
        paintDot(x, y, strokeWidth * 0.72);
    }

    public void continueStroke(double x, double y) {
        if (!strokeActive) {
            beginStroke(x, y);
            return;
        }

        double rawDistance = Math.hypot(x - lastRawX, y - lastRawY);
        if (rawDistance < minDistance) {
            return;
        }

        double smoothingFactor = rawDistance > 10 ? fastSmoothingFactor : baseSmoothingFactor;
        if (rawDistance < strokeWidth * 0.65) {
            smoothingFactor *= 0.72;
        }
        double filteredX = lastFilteredX + (x - lastFilteredX) * smoothingFactor;
        double filteredY = lastFilteredY + (y - lastFilteredY) * smoothingFactor;
        double deltaX = filteredX - lastFilteredX;
        double deltaY = filteredY - lastFilteredY;
        double filteredDistance = Math.hypot(deltaX, deltaY);
        if (filteredDistance < minDistance * 0.92) {
            lastRawX = x;
            lastRawY = y;
            return;
        }
        if (filteredDistance > MAX_STEP_DISTANCE) {
            double scale = MAX_STEP_DISTANCE / filteredDistance;
            filteredX = lastFilteredX + deltaX * scale;
            filteredY = lastFilteredY + deltaY * scale;
        }
        drawInterpolatedSegment(filteredX, filteredY);

        lastRawX = x;
        lastRawY = y;
        lastFilteredX = filteredX;
        lastFilteredY = filteredY;
    }

    public void endStroke() {
        strokeActive = false;
    }

    public void clear() {
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        strokeActive = false;
        hasInk = false;
    }

    public boolean hasInk() {
        return hasInk;
    }

    public WritableImage snapshot() {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, null);
    }

    private void configureBrush() {
        graphics.setStroke(strokeColor);
        graphics.setLineWidth(strokeWidth);
        graphics.setLineCap(StrokeLineCap.ROUND);
        graphics.setLineJoin(StrokeLineJoin.ROUND);
        graphics.setFill(strokeColor);
    }

    private void drawInterpolatedSegment(double targetX, double targetY) {
        double startX = lastFilteredX;
        double startY = lastFilteredY;
        double distance = Math.hypot(targetX - startX, targetY - startY);
        int steps = Math.max(1, (int) Math.ceil(distance / SEGMENT_STEP));
        for (int step = 1; step <= steps; step++) {
            double ratio = step / (double) steps;
            double interpolatedX = startX + (targetX - startX) * ratio;
            double interpolatedY = startY + (targetY - startY) * ratio;
            interpolatedX = softenPoint(interpolatedX, lastFilteredX);
            interpolatedY = softenPoint(interpolatedY, lastFilteredY);

            drawContinuousLine(lastFilteredX, lastFilteredY, interpolatedX, interpolatedY);

            lastMidX = (lastFilteredX + interpolatedX) / 2.0;
            lastMidY = (lastFilteredY + interpolatedY) / 2.0;
            lastFilteredX = interpolatedX;
            lastFilteredY = interpolatedY;
        }
    }

    private void drawContinuousLine(double fromX, double fromY, double toX, double toY) {
        graphics.strokeLine(fromX, fromY, toX, toY);
    }

    private double softenPoint(double candidate, double anchor) {
        double delta = candidate - anchor;
        if (Math.abs(delta) < ASSIST_RADIUS) {
            return anchor + delta * 0.55;
        }
        return candidate;
    }

    private void paintDot(double x, double y, double size) {
        double radius = size / 2.0;
        graphics.fillOval(x - radius, y - radius, size, size);
    }
}
