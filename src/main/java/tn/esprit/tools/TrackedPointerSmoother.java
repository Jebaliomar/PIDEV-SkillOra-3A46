package tn.esprit.tools;

public class TrackedPointerSmoother {

    private final double alpha;
    private HandLandmarkPoint smoothedPoint;

    public TrackedPointerSmoother() {
        this(0.35);
    }

    public TrackedPointerSmoother(double alpha) {
        this.alpha = alpha;
    }

    public HandLandmarkPoint update(HandLandmarkPoint rawPoint) {
        if (rawPoint == null) {
            smoothedPoint = null;
            return null;
        }
        if (smoothedPoint == null) {
            smoothedPoint = rawPoint;
            return smoothedPoint;
        }

        double deltaX = rawPoint.x() - smoothedPoint.x();
        double deltaY = rawPoint.y() - smoothedPoint.y();
        double distance = Math.hypot(deltaX, deltaY);
        if (distance < 0.0018) {
            return smoothedPoint;
        }

        double adaptiveAlpha = Math.max(0.22, Math.min(0.78, alpha + Math.min(0.22, distance * 0.85)));
        double nextX = smoothedPoint.x() + deltaX * adaptiveAlpha;
        double nextY = smoothedPoint.y() + deltaY * adaptiveAlpha;
        smoothedPoint = new HandLandmarkPoint(nextX, nextY, rawPoint.confidence());
        return smoothedPoint;
    }

    public void reset() {
        smoothedPoint = null;
    }
}
