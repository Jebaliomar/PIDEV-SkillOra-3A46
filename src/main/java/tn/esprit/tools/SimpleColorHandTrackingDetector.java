package tn.esprit.tools;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class SimpleColorHandTrackingDetector implements HandTrackingDetector {

    private static final int SAMPLE_STEP = 3;
    private static final int MIN_COMPONENT_PIXELS = 150;
    private static final double GESTURE_ON_THRESHOLD = 0.60;
    private static final double GESTURE_OFF_THRESHOLD = 0.42;
    private boolean drawingGestureActive;

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public TrackedHandState detect(BufferedImage frame) {
        if (frame == null || frame.getWidth() <= 0 || frame.getHeight() <= 0) {
            return TrackedHandState.unavailable("Aucune frame camera disponible.");
        }

        DetectionGrid grid = buildSkinMask(frame);
        Component component = findLargestComponent(grid);
        if (component == null || component.area < MIN_COMPONENT_PIXELS) {
            return TrackedHandState.unavailable("Main non detectee. Placez votre main dans la zone camera.");
        }

        HandLandmarkPoint centroid = new HandLandmarkPoint(component.centroidX, component.centroidY, 1.0);
        HandLandmarkPoint fingertip = component.boundary.stream()
                .filter(point -> point.y() <= centroid.y() + 0.04)
                .max(Comparator.comparingDouble(point ->
                        (centroid.y() - point.y()) - Math.abs(point.x() - centroid.x()) * 0.45))
                .orElseGet(() -> component.boundary.stream()
                        .min(Comparator.comparingDouble(HandLandmarkPoint::y))
                        .orElse(centroid));

        double dx = fingertip.x() - centroid.x();
        double dy = fingertip.y() - centroid.y();
        double distance = Math.hypot(dx, dy);
        double bboxDiagonal = Math.hypot(component.maxX - component.minX, component.maxY - component.minY);
        double bboxWidth = Math.max(0.001, component.maxX - component.minX);
        double bboxHeight = Math.max(0.001, component.maxY - component.minY);
        double fillRatio = clamp(component.area / Math.max(1.0, component.boundingArea));
        double topSpread = computeTopSpread(component.boundary, component.minY, bboxWidth, bboxHeight);
        double fingertipOffset = clamp(distance / Math.max(0.001, bboxDiagonal));
        double gestureStrength = clamp(
                fillRatio * 0.48
                        + (1.0 - topSpread) * 0.34
                        + (1.0 - fingertipOffset) * 0.18
        );
        drawingGestureActive = drawingGestureActive
                ? gestureStrength >= GESTURE_OFF_THRESHOLD
                : gestureStrength >= GESTURE_ON_THRESHOLD;
        boolean drawingActive = drawingGestureActive && fingertip.y() < centroid.y() + bboxHeight * 0.18;

        List<HandLandmarkPoint> landmarks = sampleBoundary(component.boundary, 16);
        landmarks = new ArrayList<>(landmarks);
        landmarks.add(fingertip);

        return new TrackedHandState(
                true,
                landmarks,
                fingertip,
                drawingActive,
                drawingActive
                        ? "Geste ecriture actif."
                        : "Main ouverte : pause. Fermez la main ou faites un pinch pour tracer.",
                gestureStrength
        );
    }

    @Override
    public String describeAvailability() {
        return "Hand tracking desktop actif : detection couleur simplifiee avec repères bleus.";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private double computeTopSpread(List<HandLandmarkPoint> boundary, double minY, double bboxWidth, double bboxHeight) {
        double topBandLimit = minY + bboxHeight * 0.35;
        double minTopX = Double.MAX_VALUE;
        double maxTopX = Double.MIN_VALUE;
        boolean found = false;
        for (HandLandmarkPoint point : boundary) {
            if (point.y() <= topBandLimit) {
                minTopX = Math.min(minTopX, point.x());
                maxTopX = Math.max(maxTopX, point.x());
                found = true;
            }
        }
        if (!found) {
            return 1.0;
        }
        return clamp((maxTopX - minTopX) / Math.max(0.001, bboxWidth));
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private DetectionGrid buildSkinMask(BufferedImage frame) {
        int sampledWidth = Math.max(1, frame.getWidth() / SAMPLE_STEP);
        int sampledHeight = Math.max(1, frame.getHeight() / SAMPLE_STEP);
        boolean[][] skin = new boolean[sampledHeight][sampledWidth];

        for (int gy = 0; gy < sampledHeight; gy++) {
            for (int gx = 0; gx < sampledWidth; gx++) {
                int px = Math.min(frame.getWidth() - 1, gx * SAMPLE_STEP);
                int py = Math.min(frame.getHeight() - 1, gy * SAMPLE_STEP);
                int rgb = frame.getRGB(px, py);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                skin[gy][gx] = isSkinColor(r, g, b);
            }
        }

        smoothMask(skin, sampledWidth, sampledHeight);
        return new DetectionGrid(skin, sampledWidth, sampledHeight);
    }

    private void smoothMask(boolean[][] skin, int width, int height) {
        boolean[][] copy = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(skin[y], 0, copy[y], 0, width);
        }

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int neighbors = 0;
                for (int dy2 = -1; dy2 <= 1; dy2++) {
                    for (int dx2 = -1; dx2 <= 1; dx2++) {
                        if (copy[y + dy2][x + dx2]) {
                            neighbors++;
                        }
                    }
                }
                skin[y][x] = neighbors >= 4;
            }
        }
    }

    private boolean isSkinColor(int r, int g, int b) {
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        boolean rgbRule = r > 90 && g > 35 && b > 15 && (max - min) > 12 && Math.abs(r - g) > 10 && r >= g && r > b;

        double cb = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
        double cr = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;
        boolean yCrCbRule = cr >= 132 && cr <= 182 && cb >= 80 && cb <= 140;

        return rgbRule && yCrCbRule;
    }

    private Component findLargestComponent(DetectionGrid grid) {
        boolean[][] visited = new boolean[grid.height][grid.width];
        Component best = null;

        for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
                if (!grid.skin[y][x] || visited[y][x]) {
                    continue;
                }
                Component component = floodFill(grid, visited, x, y);
                if (best == null || component.area > best.area) {
                    best = component;
                }
            }
        }
        return best;
    }

    private Component floodFill(DetectionGrid grid, boolean[][] visited, int startX, int startY) {
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        int area = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        double sumX = 0;
        double sumY = 0;
        List<HandLandmarkPoint> boundary = new ArrayList<>();

        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!queue.isEmpty()) {
            int[] point = queue.removeFirst();
            int x = point[0];
            int y = point[1];
            area++;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            sumX += x;
            sumY += y;

            boolean isBoundary = false;
            for (int[] dir : directions) {
                int nx = x + dir[0];
                int ny = y + dir[1];
                if (nx < 0 || ny < 0 || nx >= grid.width || ny >= grid.height) {
                    isBoundary = true;
                    continue;
                }
                if (!grid.skin[ny][nx]) {
                    isBoundary = true;
                    continue;
                }
                if (!visited[ny][nx]) {
                    visited[ny][nx] = true;
                    queue.addLast(new int[]{nx, ny});
                }
            }

            if (isBoundary) {
                boundary.add(toNormalizedPoint(grid, x, y, 1.0));
            }
        }

        if (boundary.isEmpty()) {
            boundary = Collections.singletonList(toNormalizedPoint(grid, startX, startY, 1.0));
        }

        return new Component(
                area,
                normalize(sumX / area, grid.width),
                normalize(sumY / area, grid.height),
                normalize(minX, grid.width),
                normalize(minY, grid.height),
                normalize(maxX, grid.width),
                normalize(maxY, grid.height),
                Math.max(1.0, (maxX - minX + 1.0) * (maxY - minY + 1.0)),
                boundary
        );
    }

    private List<HandLandmarkPoint> sampleBoundary(List<HandLandmarkPoint> boundary, int maxPoints) {
        if (boundary.isEmpty()) {
            return Collections.emptyList();
        }
        if (boundary.size() <= maxPoints) {
            return boundary;
        }

        List<HandLandmarkPoint> sampled = new ArrayList<>();
        double step = (double) boundary.size() / maxPoints;
        for (int i = 0; i < maxPoints; i++) {
            sampled.add(boundary.get(Math.min(boundary.size() - 1, (int) Math.round(i * step))));
        }
        return sampled;
    }

    private HandLandmarkPoint toNormalizedPoint(DetectionGrid grid, int x, int y, double confidence) {
        return new HandLandmarkPoint(normalize(x, grid.width), normalize(y, grid.height), confidence);
    }

    private double normalize(double value, int length) {
        if (length <= 1) {
            return 0;
        }
        return Math.max(0, Math.min(1, value / (double) (length - 1)));
    }

    private record DetectionGrid(boolean[][] skin, int width, int height) {
    }

    private record Component(
            int area,
            double centroidX,
            double centroidY,
            double minX,
            double minY,
            double maxX,
            double maxY,
            double boundingArea,
            List<HandLandmarkPoint> boundary
    ) {
    }
}
