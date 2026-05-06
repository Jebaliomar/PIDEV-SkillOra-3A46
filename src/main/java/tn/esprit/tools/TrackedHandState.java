package tn.esprit.tools;

import java.util.Collections;
import java.util.List;

public record TrackedHandState(
        boolean detected,
        List<HandLandmarkPoint> landmarks,
        HandLandmarkPoint guidePoint,
        HandLandmarkPoint drawingPoint,
        boolean drawingActive,
        HandTrackingPhase phase,
        String statusMessage,
        double gestureStrength,
        double pinchDistance
) {

    public static TrackedHandState unavailable(String statusMessage) {
        return new TrackedHandState(false, Collections.emptyList(), null, null, false, HandTrackingPhase.NO_HAND, statusMessage, 0.0, 1.0);
    }
}
