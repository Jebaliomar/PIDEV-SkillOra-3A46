package tn.esprit.tools;

import javafx.scene.image.WritableImage;

public interface OcrTextRecognizer {

    String recognize(WritableImage image, CameraReservationField field) throws Exception;

    String describeAvailability();

    boolean isAvailable();
}
