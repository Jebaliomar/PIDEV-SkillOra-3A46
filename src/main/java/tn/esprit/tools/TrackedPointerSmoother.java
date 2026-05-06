package tn.esprit.tools;

public class TrackedPointerSmoother {

    private static final double JITTER_DEADZONE = 0.0030;
    private static final double EMA_ALPHA = 0.24;
    private static final double SLOW_POSITION_GAIN = 0.13;
    private static final double FAST_POSITION_GAIN = 0.29;
    private static final double SLOW_VELOCITY_GAIN = 0.08;
    private static final double FAST_VELOCITY_GAIN = 0.16;
    private static final double MAX_VELOCITY = 0.032;
    private static final double PREDICTION_FACTOR = 0.42;
    private static final double OUTLIER_DISTANCE = 0.07;
    private static final double SNAP_DISTANCE = 0.11;

    private final double alpha;
    private HandLandmarkPoint smoothedPoint;
    private HandLandmarkPoint lastRawPoint;
    private HandLandmarkPoint emaPoint;
    private HandLandmarkPoint pendingOutlier;
    private int outlierRepeatCount;
    private double velocityX;
    private double velocityY;

    public TrackedPointerSmoother() {
        this(SLOW_POSITION_GAIN);
    }

    public TrackedPointerSmoother(double alpha) {
        this.alpha = alpha;
    }

    public static TrackedPointerSmoother fromConfig(CameraReservationConfig config) {
        double configuredAlpha = Math.max(0.08, Math.min(0.55, config.getSmoothingAlpha() * 0.28));
        return new TrackedPointerSmoother(configuredAlpha);
    }

    public HandLandmarkPoint update(HandLandmarkPoint rawPoint) {
        if (rawPoint == null) {
            smoothedPoint = null;
            lastRawPoint = null;
            emaPoint = null;
            pendingOutlier = null;
            outlierRepeatCount = 0;
            velocityX = 0.0;
            velocityY = 0.0;
            return null;
        }
        if (smoothedPoint == null) {
            smoothedPoint = rawPoint;
            lastRawPoint = rawPoint;
            emaPoint = rawPoint;
            return smoothedPoint;
        }

        if (lastRawPoint != null && isOutlier(rawPoint, lastRawPoint, smoothedPoint)) {
            if (pendingOutlier != null && areClose(rawPoint, pendingOutlier, JITTER_DEADZONE * 3.0)) {
                outlierRepeatCount++;
            } else {
                pendingOutlier = rawPoint;
                outlierRepeatCount = 1;
            }
            if (outlierRepeatCount < 2) {
                return smoothedPoint;
            }
        } else {
            pendingOutlier = null;
            outlierRepeatCount = 0;
        }

        emaPoint = applyEma(rawPoint);

        double predictedX = clamp(smoothedPoint.x() + velocityX * PREDICTION_FACTOR, 0.0, 1.0);
        double predictedY = clamp(smoothedPoint.y() + velocityY * PREDICTION_FACTOR, 0.0, 1.0);
        double residualX = emaPoint.x() - predictedX;
        double residualY = emaPoint.y() - predictedY;
        double residualDistance = Math.hypot(residualX, residualY);

        if (residualDistance < JITTER_DEADZONE) {
            lastRawPoint = rawPoint;
            return smoothedPoint;
        }

        if (residualDistance > SNAP_DISTANCE) {
            smoothedPoint = emaPoint;
            lastRawPoint = rawPoint;
            emaPoint = rawPoint;
            velocityX = 0.0;
            velocityY = 0.0;
            pendingOutlier = null;
            outlierRepeatCount = 0;
            return smoothedPoint;
        }

        double positionGain = residualDistance > 0.018 ? FAST_POSITION_GAIN : alpha;
        double velocityGain = residualDistance > 0.018 ? FAST_VELOCITY_GAIN : SLOW_VELOCITY_GAIN;
        if (residualDistance < JITTER_DEADZONE * 2.2) {
            positionGain *= 0.45;
            velocityGain *= 0.35;
        }

        double nextX = clamp(predictedX + residualX * positionGain, 0.0, 1.0);
        double nextY = clamp(predictedY + residualY * positionGain, 0.0, 1.0);
        if (Math.hypot(nextX - smoothedPoint.x(), nextY - smoothedPoint.y()) < JITTER_DEADZONE * 0.9) {
            lastRawPoint = rawPoint;
            return smoothedPoint;
        }
        velocityX = clamp((velocityX + residualX * velocityGain) * 0.92, -MAX_VELOCITY, MAX_VELOCITY);
        velocityY = clamp((velocityY + residualY * velocityGain) * 0.92, -MAX_VELOCITY, MAX_VELOCITY);
        smoothedPoint = new HandLandmarkPoint(nextX, nextY, rawPoint.confidence());
        lastRawPoint = rawPoint;
        return smoothedPoint;
    }

    public void reset() {
        smoothedPoint = null;
        lastRawPoint = null;
        emaPoint = null;
        pendingOutlier = null;
        outlierRepeatCount = 0;
        velocityX = 0.0;
        velocityY = 0.0;
    }

    private boolean isOutlier(HandLandmarkPoint rawPoint, HandLandmarkPoint previousRawPoint, HandLandmarkPoint currentSmoothedPoint) {
        double rawJump = Math.hypot(rawPoint.x() - previousRawPoint.x(), rawPoint.y() - previousRawPoint.y());
        double smoothedJump = Math.hypot(rawPoint.x() - currentSmoothedPoint.x(), rawPoint.y() - currentSmoothedPoint.y());
        return rawJump > OUTLIER_DISTANCE && smoothedJump > OUTLIER_DISTANCE * 0.75;
    }

    private boolean areClose(HandLandmarkPoint a, HandLandmarkPoint b, double threshold) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) <= threshold;
    }

    private HandLandmarkPoint applyEma(HandLandmarkPoint rawPoint) {
        if (emaPoint == null) {
            emaPoint = rawPoint;
            return rawPoint;
        }
        return emaPoint = new HandLandmarkPoint(
                emaPoint.x() + (rawPoint.x() - emaPoint.x()) * EMA_ALPHA,
                emaPoint.y() + (rawPoint.y() - emaPoint.y()) * EMA_ALPHA,
                rawPoint.confidence()
        );
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
