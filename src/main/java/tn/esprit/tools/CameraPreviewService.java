package tn.esprit.tools;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CameraPreviewService {

    public record CameraFrame(Image image, BufferedImage bufferedImage, int width, int height) {
    }

    private final CameraReservationConfig config;
    private Webcam webcam;
    private ScheduledExecutorService executor;
    private Consumer<String> statusConsumer;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean firstFrameReceived = new AtomicBoolean(false);
    private volatile CameraFrame latestFrame;

    public CameraPreviewService() {
        this(CameraReservationConfig.getInstance());
    }

    public CameraPreviewService(CameraReservationConfig config) {
        this.config = config;
    }

    public void bind(Consumer<CameraFrame> frameConsumer, Consumer<String> statusConsumer) {
        this.statusConsumer = statusConsumer;
    }

    public CameraFrame getLatestFrame() {
        return latestFrame;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }

        updateStatus("Connexion a la camera...");
        firstFrameReceived.set(false);
        running.set(true);
        latestFrame = null;

        executor = Executors.newSingleThreadScheduledExecutor(buildThreadFactory());
        executor.execute(this::openAndStreamCamera);
    }

    public synchronized void stop() {
        running.set(false);

        ScheduledExecutorService currentExecutor = executor;
        executor = null;
        if (currentExecutor != null) {
            currentExecutor.shutdownNow();
        }

        Webcam currentWebcam = webcam;
        webcam = null;
        if (currentWebcam != null) {
            try {
                currentWebcam.close();
            } catch (Exception ignored) {
            }
        }

        latestFrame = null;
        updateStatus("Camera arretee.");
    }

    public boolean isRunning() {
        return running.get();
    }

    private void openAndStreamCamera() {
        try {
            webcam = chooseWebcam();
            if (webcam == null) {
                updateStatus("Aucune camera detectee sur cette machine.");
                running.set(false);
                return;
            }

            webcam.setViewSize(selectStableViewSize(webcam));
            webcam.open(true);
            updateStatus("Camera ouverte, attente du flux video...");

            ScheduledExecutorService currentExecutor = executor;
            if (currentExecutor == null) {
                return;
            }
            currentExecutor.scheduleAtFixedRate(this::grabFrame, 0, getFramePeriodMs(), TimeUnit.MILLISECONDS);
            currentExecutor.schedule(this::reportIfNoFrameYet, 4, TimeUnit.SECONDS);
        } catch (Exception exception) {
            updateStatus("Impossible d'activer la camera: " + safeMessage(exception));
            stop();
        }
    }

    private void grabFrame() {
        if (!running.get()) {
            return;
        }

        try {
            Webcam currentWebcam = webcam;
            if (currentWebcam == null || !currentWebcam.isOpen()) {
                return;
            }

            BufferedImage rawFrame = currentWebcam.getImage();
            if (rawFrame == null) {
                return;
            }

            BufferedImage copiedFrame = copyFrame(rawFrame);
            Image fxFrame = SwingFXUtils.toFXImage(copiedFrame, null);
            latestFrame = new CameraFrame(fxFrame, copiedFrame, copiedFrame.getWidth(), copiedFrame.getHeight());

            if (firstFrameReceived.compareAndSet(false, true)) {
                updateStatus("Flux camera actif");
            }
        } catch (Exception exception) {
            updateStatus("Lecture camera interrompue: " + safeMessage(exception));
        }
    }

    private Webcam chooseWebcam() {
        List<Webcam> webcams = Webcam.getWebcams();
        if (webcams == null || webcams.isEmpty()) {
            return null;
        }

        Webcam configured = webcams.stream()
                .filter(Objects::nonNull)
                .skip(Math.max(0, config.getCameraDeviceIndex()))
                .findFirst()
                .orElse(null);
        if (configured != null) {
            return configured;
        }

        return webcams.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(webcam -> webcam.getName() == null ? Integer.MAX_VALUE : webcam.getName().length()))
                .orElse(webcams.get(0));
    }

    private Dimension selectStableViewSize(Webcam webcam) {
        Dimension[] supportedSizes = webcam.getViewSizes();
        if (supportedSizes == null || supportedSizes.length == 0) {
            return WebcamResolution.VGA.getSize();
        }

        List<Dimension> preferredOrder = List.of(
                new Dimension(config.getCameraWidth(), config.getCameraHeight()),
                new Dimension(960, 720),
                new Dimension(800, 600),
                new Dimension(640, 480)
        );

        for (Dimension preferred : preferredOrder) {
            for (Dimension supported : supportedSizes) {
                if (supported.width == preferred.width && supported.height == preferred.height) {
                    return supported;
                }
            }
        }

        return Arrays.stream(supportedSizes)
                .max(Comparator.comparingInt(size -> Math.min(size.width, 1280) * Math.min(size.height, 720)))
                .orElse(WebcamResolution.VGA.getSize());
    }

    private void reportIfNoFrameYet() {
        if (running.get() && !firstFrameReceived.get()) {
            updateStatus("La camera est ouverte mais aucun flux n'arrive. Fermez Zoom, Teams, navigateur ou toute application qui utilise deja la webcam.");
        }
    }

    private void updateStatus(String message) {
        if (statusConsumer != null) {
            Platform.runLater(() -> statusConsumer.accept(message));
        }
    }

    private long getFramePeriodMs() {
        int fps = Math.max(1, config.getCameraFps());
        return Math.max(15L, 1000L / fps);
    }

    private ThreadFactory buildThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "camera-preview-thread");
            thread.setDaemon(true);
            return thread;
        };
    }

    private BufferedImage copyFrame(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        java.awt.Graphics2D graphics = copy.createGraphics();
        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private String safeMessage(Exception exception) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "erreur inconnue";
        }
        return exception.getMessage();
    }
}
