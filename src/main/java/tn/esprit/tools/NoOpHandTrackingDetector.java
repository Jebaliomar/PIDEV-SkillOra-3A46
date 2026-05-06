package tn.esprit.tools;

import java.awt.image.BufferedImage;

public class NoOpHandTrackingDetector implements HandTrackingDetector {

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public TrackedHandState detect(BufferedImage frame) {
        return TrackedHandState.unavailable("Hand tracking backend not installed.");
    }

    @Override
    public String describeAvailability() {
        return "Hand tracking indisponible : aucun detecteur desktop n'est encore installe.";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
