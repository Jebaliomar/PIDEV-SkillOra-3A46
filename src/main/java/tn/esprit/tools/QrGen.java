package tn.esprit.tools;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public final class QrGen {
    private QrGen() {}

    public static Image make(String text, int size) throws Exception {
        BitMatrix matrix = makeMatrix(text, size);
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pw.setArgb(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return img;
    }

    public static byte[] makePngBytes(String text, int size) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(makeMatrix(text, size), "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static BitMatrix makeMatrix(String text, int size) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        return new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints);
    }
}
