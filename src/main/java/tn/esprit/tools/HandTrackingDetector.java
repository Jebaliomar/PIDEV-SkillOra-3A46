package tn.esprit.tools;

import java.awt.image.BufferedImage;

public interface HandTrackingDetector {

    void start() throws Exception;

    void stop();

    TrackedHandState detect(BufferedImage frame);

    String describeAvailability();

    boolean isAvailable();
}
