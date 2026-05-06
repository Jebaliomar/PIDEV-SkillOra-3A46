package tn.esprit.tools;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class StableCameraPreviewPane extends StackPane {

    private final Region frameSurface = new Region();
    private final StackPane videoLayer = new StackPane();
    private final ImageView imageView = new ImageView();
    private final Canvas landmarksCanvas = new Canvas();
    private final Canvas drawingCanvas = new Canvas();
    private final DoubleProperty sourceAspectRatio = new SimpleDoubleProperty(4.0 / 3.0);

    public StableCameraPreviewPane() {
        getStyleClass().add("site-camera-preview-shell");
        setAlignment(Pos.CENTER);

        frameSurface.getStyleClass().add("site-camera-preview-surface");
        frameSurface.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);
        imageView.setScaleX(-1);

        landmarksCanvas.setMouseTransparent(true);
        drawingCanvas.setMouseTransparent(false);

        videoLayer.setManaged(false);
        videoLayer.setAlignment(Pos.CENTER);
        videoLayer.getChildren().addAll(imageView, landmarksCanvas, drawingCanvas);

        getChildren().addAll(frameSurface, videoLayer);

        widthProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        heightProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        sourceAspectRatio.addListener((obs, oldValue, newValue) -> requestLayout());
    }

    public ImageView getImageView() {
        return imageView;
    }

    public Canvas getLandmarksCanvas() {
        return landmarksCanvas;
    }

    public Canvas getDrawingCanvas() {
        return drawingCanvas;
    }

    public void setSourceDimensions(double width, double height) {
        if (width > 0 && height > 0) {
            sourceAspectRatio.set(width / height);
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        double availableWidth = getWidth();
        double availableHeight = getHeight();
        if (availableWidth <= 0 || availableHeight <= 0) {
            return;
        }

        double aspectRatio = sourceAspectRatio.get() <= 0 ? (4.0 / 3.0) : sourceAspectRatio.get();
        double fittedWidth = availableWidth;
        double fittedHeight = fittedWidth / aspectRatio;

        if (fittedHeight > availableHeight) {
            fittedHeight = availableHeight;
            fittedWidth = fittedHeight * aspectRatio;
        }

        double x = (availableWidth - fittedWidth) / 2.0;
        double y = (availableHeight - fittedHeight) / 2.0;

        frameSurface.resizeRelocate(0, 0, availableWidth, availableHeight);
        videoLayer.resizeRelocate(x, y, fittedWidth, fittedHeight);

        imageView.setFitWidth(fittedWidth);
        imageView.setFitHeight(fittedHeight);
        landmarksCanvas.setWidth(fittedWidth);
        landmarksCanvas.setHeight(fittedHeight);
        drawingCanvas.setWidth(fittedWidth);
        drawingCanvas.setHeight(fittedHeight);
    }
}
