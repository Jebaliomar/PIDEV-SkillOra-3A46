package tn.esprit.tools;

import java.util.Collections;
import java.util.List;

public record TrackedHandState(
        boolean detected,
        List<HandLandmarkPoint> landmarks,
        HandLandmarkPoint drawingPoint,
        boolean drawingActive,
        String statusMessage,
        double gestureStrength
) {

    public static TrackedHandState unavailable(String statusMessage) {
        return new TrackedHandState(false, Collections.emptyList(), null, false, statusMessage, 0.0);
    }
}
