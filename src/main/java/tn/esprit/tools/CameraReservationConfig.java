package tn.esprit.tools;

import java.io.InputStream;
import java.util.Properties;

public final class CameraReservationConfig {

    private static final String RESOURCE_PATH = "/camera-reservation.properties";
    private static final CameraReservationConfig INSTANCE = load();

    private final int cameraDeviceIndex;
    private final int cameraWidth;
    private final int cameraHeight;
    private final int previewWidth;
    private final int previewHeight;
    private final int cameraFps;
    private final double pinchOnThreshold;
    private final double pinchOffThreshold;
    private final String ocrLanguage;
    private final int ocrMinConfidence;
    private final String ocrCommand;

    private CameraReservationConfig(
            int cameraDeviceIndex,
            int cameraWidth,
            int cameraHeight,
            int previewWidth,
            int previewHeight,
            int cameraFps,
            double pinchOnThreshold,
            double pinchOffThreshold,
            String ocrLanguage,
            int ocrMinConfidence,
            String ocrCommand
    ) {
        this.cameraDeviceIndex = cameraDeviceIndex;
        this.cameraWidth = cameraWidth;
        this.cameraHeight = cameraHeight;
        this.previewWidth = previewWidth;
        this.previewHeight = previewHeight;
        this.cameraFps = cameraFps;
        this.pinchOnThreshold = pinchOnThreshold;
        this.pinchOffThreshold = pinchOffThreshold;
        this.ocrLanguage = ocrLanguage;
        this.ocrMinConfidence = ocrMinConfidence;
        this.ocrCommand = ocrCommand;
    }

    public static CameraReservationConfig getInstance() {
        return INSTANCE;
    }

    public int getCameraDeviceIndex() {
        return cameraDeviceIndex;
    }

    public int getCameraWidth() {
        return cameraWidth;
    }

    public int getCameraHeight() {
        return cameraHeight;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public int getCameraFps() {
        return cameraFps;
    }

    public double getPinchOnThreshold() {
        return pinchOnThreshold;
    }

    public double getPinchOffThreshold() {
        return pinchOffThreshold;
    }

    public String getOcrLanguage() {
        return ocrLanguage;
    }

    public int getOcrMinConfidence() {
        return ocrMinConfidence;
    }

    public String getOcrCommand() {
        return ocrCommand;
    }

    private static CameraReservationConfig load() {
        Properties properties = new Properties();
        try (InputStream input = CameraReservationConfig.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception ignored) {
        }

        return new CameraReservationConfig(
                readInt(properties, "camera.device.index", 0),
                readInt(properties, "camera.width", 1280),
                readInt(properties, "camera.height", 720),
                readInt(properties, "camera.preview.width", 880),
                readInt(properties, "camera.preview.height", 505),
                readInt(properties, "camera.fps", 30),
                readDouble(properties, "gesture.pinch.on", 0.08),
                readDouble(properties, "gesture.pinch.off", 0.11),
                properties.getProperty("ocr.language", "eng"),
                readInt(properties, "ocr.min.confidence", 60),
                properties.getProperty("ocr.command", "").trim()
        );
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)).trim());
        } catch (Exception exception) {
            return fallback;
        }
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, String.valueOf(fallback)).trim());
        } catch (Exception exception) {
            return fallback;
        }
    }
}
