package tn.esprit.tools;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public final class CanvasOcrPreprocessor {

    private static final double ACTIVE_ALPHA = 0.05;
    private static final int PADDING = 20;
    private static final int SCALE = 4;
    private static final int THICKNESS = 2;

    private CanvasOcrPreprocessor() {
    }

    public static WritableImage prepare(WritableImage rawInk) {
        int width = (int) rawInk.getWidth();
        int height = (int) rawInk.getHeight();
        PixelReader reader = rawInk.getPixelReader();
        boolean[][] active = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                active[y][x] = reader.getColor(x, y).getOpacity() > ACTIVE_ALPHA;
            }
        }

        active = denoise(active, width, height);
        Bounds bounds = findBounds(active, width, height);
        if (bounds == null) {
            return createBlank(width, height);
        }

        int minX = Math.max(0, bounds.minX - PADDING);
        int minY = Math.max(0, bounds.minY - PADDING);
        int maxX = Math.min(width - 1, bounds.maxX + PADDING);
        int maxY = Math.min(height - 1, bounds.maxY + PADDING);

        int outputWidth = Math.max(1, (maxX - minX + 1) * SCALE);
        int outputHeight = Math.max(1, (maxY - minY + 1) * SCALE);
        WritableImage output = createBlank(outputWidth, outputHeight);
        PixelWriter writer = output.getPixelWriter();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!active[y][x]) {
                    continue;
                }
                int scaledX = (x - minX) * SCALE;
                int scaledY = (y - minY) * SCALE;
                drawDot(writer, outputWidth, outputHeight, scaledX, scaledY);
                bridgeToNeighbors(active, writer, minX, minY, outputWidth, outputHeight, x, y, width, height);
            }
        }
        return output;
    }

    private static boolean[][] denoise(boolean[][] source, int width, int height) {
        boolean[][] result = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!source[y][x]) {
                    continue;
                }
                if (countNeighbors(source, width, height, x, y, 1) >= 3) {
                    result[y][x] = true;
                }
            }
        }
        return result;
    }

    private static int countNeighbors(boolean[][] source, int width, int height, int x, int y, int radius) {
        int count = 0;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= width || py >= height) {
                    continue;
                }
                if (source[py][px]) {
                    count++;
                }
            }
        }
        return count;
    }

    private static Bounds findBounds(boolean[][] active, int width, int height) {
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!active[y][x]) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return null;
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    private static WritableImage createBlank(int width, int height) {
        WritableImage image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, Color.WHITE);
            }
        }
        return image;
    }

    private static void bridgeToNeighbors(
            boolean[][] active,
            PixelWriter writer,
            int minX,
            int minY,
            int outputWidth,
            int outputHeight,
            int x,
            int y,
            int width,
            int height
    ) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                int nx = x + dx;
                int ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= width || ny >= height || !active[ny][nx]) {
                    continue;
                }
                drawBridge(
                        writer,
                        outputWidth,
                        outputHeight,
                        (x - minX) * SCALE,
                        (y - minY) * SCALE,
                        (nx - minX) * SCALE,
                        (ny - minY) * SCALE
                );
            }
        }
    }

    private static void drawBridge(PixelWriter writer, int width, int height, int x1, int y1, int x2, int y2) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= Math.max(1, steps); i++) {
            double ratio = i / (double) Math.max(1, steps);
            int x = (int) Math.round(x1 + (x2 - x1) * ratio);
            int y = (int) Math.round(y1 + (y2 - y1) * ratio);
            drawDot(writer, width, height, x, y);
        }
    }

    private static void drawDot(PixelWriter writer, int width, int height, int x, int y) {
        for (int dy = -THICKNESS; dy <= THICKNESS; dy++) {
            for (int dx = -THICKNESS; dx <= THICKNESS; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px < 0 || py < 0 || px >= width || py >= height) {
                    continue;
                }
                writer.setColor(px, py, Color.BLACK);
            }
        }
    }

    private record Bounds(int minX, int minY, int maxX, int maxY) {
    }
}
