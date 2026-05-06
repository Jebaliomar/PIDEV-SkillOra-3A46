package tn.esprit.tools;

public final class OneEuroPointFilter {

    private static final double DEFAULT_FREQUENCY = 60.0;
    private static final double DEFAULT_MIN_CUTOFF = 2.4;
    private static final double DEFAULT_BETA = 0.08;
    private static final double DEFAULT_DERIVATIVE_CUTOFF = 1.0;

    private final LowPassFilter xFilter = new LowPassFilter();
    private final LowPassFilter yFilter = new LowPassFilter();
    private final LowPassFilter dxFilter = new LowPassFilter();
    private final LowPassFilter dyFilter = new LowPassFilter();
    private final double frequency;
    private final double minCutoff;
    private final double beta;
    private final double derivativeCutoff;
    private HandLandmarkPoint lastRawPoint;

    public OneEuroPointFilter() {
        this(DEFAULT_FREQUENCY, DEFAULT_MIN_CUTOFF, DEFAULT_BETA, DEFAULT_DERIVATIVE_CUTOFF);
    }

    public OneEuroPointFilter(double frequency, double minCutoff, double beta, double derivativeCutoff) {
        this.frequency = frequency;
        this.minCutoff = minCutoff;
        this.beta = beta;
        this.derivativeCutoff = derivativeCutoff;
    }

    public HandLandmarkPoint filter(HandLandmarkPoint point) {
        if (point == null) {
            reset();
            return null;
        }

        if (lastRawPoint == null) {
            lastRawPoint = point;
            xFilter.reset(point.x());
            yFilter.reset(point.y());
            dxFilter.reset(0.0);
            dyFilter.reset(0.0);
            return point;
        }

        double dt = 1.0 / frequency;
        double rawDx = (point.x() - lastRawPoint.x()) / dt;
        double rawDy = (point.y() - lastRawPoint.y()) / dt;

        double dxHat = dxFilter.filter(rawDx, alpha(derivativeCutoff));
        double dyHat = dyFilter.filter(rawDy, alpha(derivativeCutoff));

        double cutoffX = minCutoff + beta * Math.abs(dxHat);
        double cutoffY = minCutoff + beta * Math.abs(dyHat);

        double filteredX = xFilter.filter(point.x(), alpha(cutoffX));
        double filteredY = yFilter.filter(point.y(), alpha(cutoffY));

        lastRawPoint = point;
        return new HandLandmarkPoint(filteredX, filteredY, point.confidence());
    }

    public void reset() {
        lastRawPoint = null;
        xFilter.clear();
        yFilter.clear();
        dxFilter.clear();
        dyFilter.clear();
    }

    private double alpha(double cutoff) {
        double tau = 1.0 / (2.0 * Math.PI * cutoff);
        double te = 1.0 / frequency;
        return 1.0 / (1.0 + tau / te);
    }

    private static final class LowPassFilter {
        private Double state;

        double filter(double value, double alpha) {
            if (state == null) {
                state = value;
                return value;
            }
            state = alpha * value + (1.0 - alpha) * state;
            return state;
        }

        void reset(double value) {
            state = value;
        }

        void clear() {
            state = null;
        }
    }
}
