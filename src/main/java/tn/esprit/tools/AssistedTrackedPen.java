package tn.esprit.tools;

public final class AssistedTrackedPen {

    private static final double DEADZONE = 0.0052;
    private static final double SMALL_GAIN = 0.32;
    private static final double MEDIUM_GAIN = 0.48;
    private static final double LARGE_GAIN = 0.72;
    private static final double MAX_DELTA = 0.028;
    private static final double PREVIEW_DRIFT = 0.15;

    private HandLandmarkPoint virtualPenPoint;
    private HandLandmarkPoint lastFingerPoint;
    private boolean writingActive;

    public HandLandmarkPoint update(HandLandmarkPoint fingerPoint, boolean gestureActive) {
        if (fingerPoint == null) {
            reset();
            return null;
        }

        if (virtualPenPoint == null || lastFingerPoint == null) {
            virtualPenPoint = fingerPoint;
            lastFingerPoint = fingerPoint;
            writingActive = gestureActive;
            return virtualPenPoint;
        }

        if (!gestureActive) {
            writingActive = false;
            lastFingerPoint = fingerPoint;
            virtualPenPoint = driftToward(virtualPenPoint, fingerPoint, PREVIEW_DRIFT);
            return virtualPenPoint;
        }

        if (!writingActive) {
            virtualPenPoint = fingerPoint;
            lastFingerPoint = fingerPoint;
            writingActive = true;
            return virtualPenPoint;
        }

        double deltaX = fingerPoint.x() - lastFingerPoint.x();
        double deltaY = fingerPoint.y() - lastFingerPoint.y();
        double distance = Math.hypot(deltaX, deltaY);
        if (distance < DEADZONE) {
            lastFingerPoint = fingerPoint;
            return virtualPenPoint;
        }

        double gain = distance > 0.02 ? LARGE_GAIN : distance > 0.01 ? MEDIUM_GAIN : SMALL_GAIN;
        double assistedDeltaX = clamp(deltaX * gain, -MAX_DELTA, MAX_DELTA);
        double assistedDeltaY = clamp(deltaY * gain, -MAX_DELTA, MAX_DELTA);
        virtualPenPoint = new HandLandmarkPoint(
                clamp(virtualPenPoint.x() + assistedDeltaX, 0.0, 1.0),
                clamp(virtualPenPoint.y() + assistedDeltaY, 0.0, 1.0),
                fingerPoint.confidence()
        );
        lastFingerPoint = fingerPoint;
        return virtualPenPoint;
    }

    public void reset() {
        virtualPenPoint = null;
        lastFingerPoint = null;
        writingActive = false;
    }

    private HandLandmarkPoint driftToward(HandLandmarkPoint from, HandLandmarkPoint to, double amount) {
        return new HandLandmarkPoint(
                from.x() + (to.x() - from.x()) * amount,
                from.y() + (to.y() - from.y()) * amount,
                to.confidence()
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
