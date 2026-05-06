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
    private static final double FINGERTIP_INSET = 0.02;
    private static final double DETECTOR_DEADZONE = 0.004;
    private static final double DETECTOR_SMOOTHING = 0.26;
    private static final double DETECTOR_FAST_SMOOTHING = 0.42;
    private static final double DETECTOR_OUTLIER_THRESHOLD = 0.09;
    private static final double PINCH_ON_FACTOR = 0.20;
    private static final double PINCH_OFF_FACTOR = 0.29;
    private static final int POINT_HISTORY_SIZE = 4;
    private boolean drawingGestureActive;
    private HandLandmarkPoint stabilizedDrawingPoint;
    private HandLandmarkPoint stabilizedCentroid;
    private HandLandmarkPoint stabilizedThumbPoint;
    private final Deque<HandLandmarkPoint> indexHistory = new ArrayDeque<>();
    private final Deque<HandLandmarkPoint> thumbHistory = new ArrayDeque<>();
    private final Deque<HandLandmarkPoint> centroidHistory = new ArrayDeque<>();

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        stabilizedDrawingPoint = null;
        stabilizedCentroid = null;
        stabilizedThumbPoint = null;
        drawingGestureActive = false;
        indexHistory.clear();
        thumbHistory.clear();
        centroidHistory.clear();
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

        HandLandmarkPoint centroid = stabilizeCentroid(
                averagePoint(centroidHistory, new HandLandmarkPoint(component.centroidX, component.centroidY, 1.0))
        );
        HandLandmarkPoint indexTip = stabilizeDrawingPoint(
                averagePoint(indexHistory, refineIndexTip(component, centroid))
        );
        double bboxDiagonal = Math.hypot(component.maxX - component.minX, component.maxY - component.minY);
        double bboxWidth = Math.max(0.001, component.maxX - component.minX);
        double bboxHeight = Math.max(0.001, component.maxY - component.minY);
        double fillRatio = clamp(component.area / Math.max(1.0, component.boundingArea));
        double topSpread = computeTopSpread(component.boundary, component.minY, bboxWidth, bboxHeight);
        HandLandmarkPoint thumbTip = stabilizeThumbPoint(
                averagePoint(thumbHistory, refineThumbTip(component, centroid, indexTip, bboxDiagonal))
        );
        double pinchDistance = thumbTip == null || indexTip == null
                ? 1.0
                : Math.hypot(indexTip.x() - thumbTip.x(), indexTip.y() - thumbTip.y());
        double pinchStartThreshold = Math.max(0.055, bboxDiagonal * PINCH_ON_FACTOR);
        double pinchEndThreshold = Math.max(pinchStartThreshold + 0.028, bboxDiagonal * PINCH_OFF_FACTOR);
        double pinchRatio = pinchDistance / Math.max(0.001, bboxDiagonal);
        double pinchStrength = clamp(1.0 - pinchDistance / Math.max(0.001, pinchEndThreshold));
        double fingertipExtension = indexTip == null
                ? 0.0
                : clamp(Math.hypot(indexTip.x() - centroid.x(), indexTip.y() - centroid.y()) / Math.max(0.001, bboxDiagonal));
        double thumbExtension = thumbTip == null
                ? 0.0
                : clamp(Math.hypot(thumbTip.x() - centroid.x(), thumbTip.y() - centroid.y()) / Math.max(0.001, bboxDiagonal));
        boolean pinchShape = thumbTip != null
                && indexTip != null
                && fingertipExtension > 0.18
                && thumbExtension > 0.10
                && pinchRatio < 0.38;
        drawingGestureActive = drawingGestureActive
                ? pinchShape && pinchDistance <= pinchEndThreshold
                : pinchShape && pinchDistance <= pinchStartThreshold;
        boolean drawingActive = drawingGestureActive && indexTip != null && thumbTip != null;
        double gestureStrength = drawingActive
                ? clamp(0.65 + pinchStrength * 0.35)
                : clamp(pinchStrength * 0.45 + (1.0 - topSpread) * 0.20 + fillRatio * 0.10);

        List<HandLandmarkPoint> landmarks = new ArrayList<>();
        if (centroid != null) {
            landmarks.add(centroid);
        }
        if (thumbTip != null) {
            landmarks.add(thumbTip);
        }
        if (indexTip != null) {
            landmarks.add(indexTip);
        }
        HandTrackingPhase phase = drawingActive ? HandTrackingPhase.PINCH_ACTIVE : HandTrackingPhase.HAND_OPEN;

        return new TrackedHandState(
                true,
                landmarks,
                drawingActive ? indexTip : null,
                drawingActive ? indexTip : null,
                drawingActive,
                phase,
                drawingActive
                        ? "Pinch actif : point rouge visible, pret a dessiner."
                        : "Main ouverte : dessin desactive. Faites un pinch pour tracer.",
                gestureStrength,
                pinchDistance
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

    private HandLandmarkPoint refineIndexTip(Component component, HandLandmarkPoint centroid) {
        List<HandLandmarkPoint> candidates = component.boundary.stream()
                .filter(point -> point.y() <= centroid.y() + 0.10)
                .sorted(Comparator.comparingDouble((HandLandmarkPoint point) ->
                        scoreFingertip(point, centroid)).reversed())
                .limit(5)
                .toList();

        HandLandmarkPoint contourTip;
        if (candidates.isEmpty()) {
            contourTip = component.boundary.stream()
                    .min(Comparator.comparingDouble(HandLandmarkPoint::y))
                    .orElse(centroid);
        } else {
            double sumX = 0.0;
            double sumY = 0.0;
            for (HandLandmarkPoint candidate : candidates) {
                sumX += candidate.x();
                sumY += candidate.y();
            }
            contourTip = new HandLandmarkPoint(sumX / candidates.size(), sumY / candidates.size(), 1.0);
        }

        double insetX = contourTip.x() + (centroid.x() - contourTip.x()) * FINGERTIP_INSET;
        double insetY = contourTip.y() + (centroid.y() - contourTip.y()) * FINGERTIP_INSET;
        return new HandLandmarkPoint(clamp(insetX), clamp(insetY), 1.0);
    }

    private HandLandmarkPoint refineThumbTip(Component component, HandLandmarkPoint centroid, HandLandmarkPoint indexTip, double bboxDiagonal) {
        if (indexTip == null) {
            return null;
        }

        double indexAngle = Math.atan2(indexTip.y() - centroid.y(), indexTip.x() - centroid.x());
        return component.boundary.stream()
                .filter(point -> point.y() <= centroid.y() + 0.18)
                .filter(point -> !isNear(point, indexTip, 0.03))
                .filter(point -> {
                    double angle = Math.atan2(point.y() - centroid.y(), point.x() - centroid.x());
                    double angleDiff = Math.abs(normalizeAngle(angle - indexAngle));
                    return angleDiff > 0.45 && angleDiff < 2.7;
                })
                .min(Comparator.comparingDouble(point -> thumbScore(point, centroid, indexTip, bboxDiagonal)))
                .map(point -> {
                    double insetX = point.x() + (centroid.x() - point.x()) * FINGERTIP_INSET;
                    double insetY = point.y() + (centroid.y() - point.y()) * FINGERTIP_INSET;
                    return new HandLandmarkPoint(clamp(insetX), clamp(insetY), 1.0);
                })
                .orElse(null);
    }

    private double scoreFingertip(HandLandmarkPoint point, HandLandmarkPoint centroid) {
        double verticalReach = centroid.y() - point.y();
        double radialReach = Math.hypot(point.x() - centroid.x(), point.y() - centroid.y());
        double sidePenalty = Math.abs(point.x() - centroid.x()) * 0.16;
        return verticalReach * 0.72 + radialReach * 0.28 - sidePenalty;
    }

    private HandLandmarkPoint stabilizeDrawingPoint(HandLandmarkPoint rawPoint) {
        if (rawPoint == null) {
            stabilizedDrawingPoint = null;
            indexHistory.clear();
            return null;
        }
        if (stabilizedDrawingPoint == null) {
            stabilizedDrawingPoint = rawPoint;
            return rawPoint;
        }
        stabilizedDrawingPoint = smoothPoint(stabilizedDrawingPoint, rawPoint);
        return stabilizedDrawingPoint;
    }

    private HandLandmarkPoint stabilizeThumbPoint(HandLandmarkPoint rawPoint) {
        if (rawPoint == null) {
            stabilizedThumbPoint = null;
            thumbHistory.clear();
            return null;
        }
        if (stabilizedThumbPoint == null) {
            stabilizedThumbPoint = rawPoint;
            return rawPoint;
        }
        stabilizedThumbPoint = smoothPoint(stabilizedThumbPoint, rawPoint);
        return stabilizedThumbPoint;
    }

    private HandLandmarkPoint stabilizeCentroid(HandLandmarkPoint rawPoint) {
        if (rawPoint == null) {
            stabilizedCentroid = null;
            centroidHistory.clear();
            return null;
        }
        if (stabilizedCentroid == null) {
            stabilizedCentroid = rawPoint;
            return rawPoint;
        }
        stabilizedCentroid = smoothPoint(stabilizedCentroid, rawPoint);
        return stabilizedCentroid;
    }

    private HandLandmarkPoint averagePoint(Deque<HandLandmarkPoint> history, HandLandmarkPoint point) {
        if (point == null) {
            history.clear();
            return null;
        }
        history.addLast(point);
        while (history.size() > POINT_HISTORY_SIZE) {
            history.removeFirst();
        }
        double sumX = 0.0;
        double sumY = 0.0;
        double sumConfidence = 0.0;
        int size = 0;
        for (HandLandmarkPoint value : history) {
            sumX += value.x();
            sumY += value.y();
            sumConfidence += value.confidence();
            size++;
        }
        if (size == 0) {
            return point;
        }
        return new HandLandmarkPoint(sumX / size, sumY / size, sumConfidence / size);
    }

    private HandLandmarkPoint smoothPoint(HandLandmarkPoint previous, HandLandmarkPoint raw) {
        double dx = raw.x() - previous.x();
        double dy = raw.y() - previous.y();
        double distance = Math.hypot(dx, dy);
        if (distance < DETECTOR_DEADZONE) {
            return previous;
        }
        if (distance > DETECTOR_OUTLIER_THRESHOLD) {
            return previous;
        }
        double alpha = distance > 0.025 ? DETECTOR_FAST_SMOOTHING : DETECTOR_SMOOTHING;
        return new HandLandmarkPoint(
                previous.x() + dx * alpha,
                previous.y() + dy * alpha,
                raw.confidence()
        );
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

    private double thumbScore(HandLandmarkPoint point, HandLandmarkPoint centroid, HandLandmarkPoint indexTip, double bboxDiagonal) {
        double pairDistance = Math.hypot(point.x() - indexTip.x(), point.y() - indexTip.y());
        double centroidDistance = Math.hypot(point.x() - centroid.x(), point.y() - centroid.y());
        return pairDistance * 0.75 - centroidDistance * 0.18 + Math.abs(point.y() - indexTip.y()) * 0.22 + bboxDiagonal * 0.05;
    }

    private boolean isNear(HandLandmarkPoint a, HandLandmarkPoint b, double threshold) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y()) <= threshold;
    }

    private double normalizeAngle(double angle) {
        while (angle <= -Math.PI) {
            angle += Math.PI * 2.0;
        }
        while (angle > Math.PI) {
            angle -= Math.PI * 2.0;
        }
        return angle;
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
