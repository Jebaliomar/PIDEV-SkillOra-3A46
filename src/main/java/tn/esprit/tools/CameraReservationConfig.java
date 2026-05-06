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
    private final int renderFps;
    private final double pinchOnThreshold;
    private final double pinchOffThreshold;
    private final double gestureConfidenceThreshold;
    private final double smoothingAlpha;
    private final double smoothingMinMovement;
    private final double canvasLineWidth;
    private final String canvasLineColor;
    private final boolean canvasAntialiasing;
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
            int renderFps,
            double pinchOnThreshold,
            double pinchOffThreshold,
            double gestureConfidenceThreshold,
            double smoothingAlpha,
            double smoothingMinMovement,
            double canvasLineWidth,
            String canvasLineColor,
            boolean canvasAntialiasing,
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
        this.renderFps = renderFps;
        this.pinchOnThreshold = pinchOnThreshold;
        this.pinchOffThreshold = pinchOffThreshold;
        this.gestureConfidenceThreshold = gestureConfidenceThreshold;
        this.smoothingAlpha = smoothingAlpha;
        this.smoothingMinMovement = smoothingMinMovement;
        this.canvasLineWidth = canvasLineWidth;
        this.canvasLineColor = canvasLineColor;
        this.canvasAntialiasing = canvasAntialiasing;
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

    public int getRenderFps() {
        return renderFps;
    }

    public double getPinchOnThreshold() {
        return pinchOnThreshold;
    }

    public double getPinchOffThreshold() {
        return pinchOffThreshold;
    }

    public double getGestureConfidenceThreshold() {
        return gestureConfidenceThreshold;
    }

    public double getSmoothingAlpha() {
        return smoothingAlpha;
    }

    public double getSmoothingMinMovement() {
        return smoothingMinMovement;
    }

    public double getCanvasLineWidth() {
        return canvasLineWidth;
    }

    public String getCanvasLineColor() {
        return canvasLineColor;
    }

    public boolean isCanvasAntialiasing() {
        return canvasAntialiasing;
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
                readInt(properties, "render.fps", 60),
                readDouble(properties, "gesture.pinch.on", 0.08),
                readDouble(properties, "gesture.pinch.off", 0.11),
                readDouble(properties, "gesture.confidence.threshold", 0.65),
                readDouble(properties, "smoothing.alpha", 0.6),
                readDouble(properties, "smoothing.min.movement", 1.5),
                readDouble(properties, "canvas.line.width", 4.0),
                properties.getProperty("canvas.line.color", "#ef4444").trim(),
                readBoolean(properties, "canvas.antialiasing", true),
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

    private static boolean readBoolean(Properties properties, String key, boolean fallback) {
        try {
            return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(fallback)).trim());
        } catch (Exception exception) {
            return fallback;
        }
    }
}
