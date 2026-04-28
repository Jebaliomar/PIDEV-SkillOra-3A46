package tn.esprit.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour CoordinateMapper
 * Valide la conversion des coordonnées et le mirroring horizontal
 */
class CoordinateMapperTest {

    private CoordinateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CoordinateMapper();
        mapper.updateCanvasDimensions(640, 480);
    }

    @Test
    void testNormalizedToCanvasPixels_IdentityWithoutMirroring() {
        mapper.setMirrorHorizontal(false);
        HandLandmarkPoint normalized = new HandLandmarkPoint(0.5, 0.5, 1.0);
        HandLandmarkPoint canvas = mapper.normalizedToCanvasPixels(normalized);

        assertEquals(320, canvas.x(), 0.1, "X should be 320 (0.5 * 640)");
        assertEquals(240, canvas.y(), 0.1, "Y should be 240 (0.5 * 480)");
    }

    @Test
    void testNormalizedToCanvasPixels_MirroredX() {
        mapper.setMirrorHorizontal(true);
        HandLandmarkPoint normalized = new HandLandmarkPoint(0.2, 0.5, 1.0);
        HandLandmarkPoint canvas = mapper.normalizedToCanvasPixels(normalized);

        // 1.0 - 0.2 = 0.8, then 0.8 * 640 = 512
        assertEquals(512, canvas.x(), 0.1, "X should be mirrored: (1-0.2)*640 = 512");
        assertEquals(240, canvas.y(), 0.1, "Y should remain 240 (0.5 * 480)");
    }

    @Test
    void testNormalizedToCanvasPixels_CornerCases() {
        mapper.setMirrorHorizontal(true);

        // Test (0, 0)
        HandLandmarkPoint p00 = mapper.normalizedToCanvasPixels(new HandLandmarkPoint(0.0, 0.0, 1.0));
        assertEquals(640, p00.x(), 0.1, "Left-top should mirror to right");
        assertEquals(0, p00.y(), 0.1, "Top should remain top");

        // Test (1, 1)
        HandLandmarkPoint p11 = mapper.normalizedToCanvasPixels(new HandLandmarkPoint(1.0, 1.0, 1.0));
        assertEquals(0, p11.x(), 0.1, "Right should mirror to left");
        assertEquals(480, p11.y(), 0.1, "Bottom should remain bottom");
    }

    @Test
    void testCanvasPixelsToNormalized_InverseOperation() {
        mapper.setMirrorHorizontal(true);
        HandLandmarkPoint original = new HandLandmarkPoint(0.3, 0.7, 1.0);

        // Convert to pixels
        HandLandmarkPoint pixels = mapper.normalizedToCanvasPixels(original);

        // Convert back to normalized
        HandLandmarkPoint restored = mapper.canvasPixelsToNormalized(pixels);

        assertEquals(original.x(), restored.x(), 0.001, "X should be restored after round-trip");
        assertEquals(original.y(), restored.y(), 0.001, "Y should be restored after round-trip");
    }

    @Test
    void testMirrorToggle() {
        HandLandmarkPoint normalized = new HandLandmarkPoint(0.25, 0.5, 1.0);

        // Without mirroring
        mapper.setMirrorHorizontal(false);
        HandLandmarkPoint notMirrored = mapper.normalizedToCanvasPixels(normalized);

        // With mirroring
        mapper.setMirrorHorizontal(true);
        HandLandmarkPoint mirrored = mapper.normalizedToCanvasPixels(normalized);

        // X should be mirrored, Y should be same
        assertNotEquals(notMirrored.x(), mirrored.x(), "X should change when mirroring is toggled");
        assertEquals(notMirrored.y(), mirrored.y(), 0.1, "Y should remain the same");
    }

    @Test
    void testNormalizedXYMethods() {
        mapper.setMirrorHorizontal(true);

        double canvasX = mapper.normalizedXToCanvasX(0.4);
        double canvasY = mapper.normalizedYToCanvasY(0.6);

        // 1.0 - 0.4 = 0.6, then 0.6 * 640 = 384
        assertEquals(384, canvasX, 0.1, "X should be mirrored and scaled");
        // 0.6 * 480 = 288
        assertEquals(288, canvasY, 0.1, "Y should be scaled");
    }

    @Test
    void testClampingBehavior() {
        mapper.setMirrorHorizontal(false);

        // Out of bounds values should be clamped
        HandLandmarkPoint outOfBounds = new HandLandmarkPoint(-0.5, 1.5, 1.0);
        HandLandmarkPoint result = mapper.normalizedToCanvasPixels(outOfBounds);

        // After clamping: (0, 1) -> pixels (0, 480)
        assertEquals(0, result.x(), 0.1, "Negative X should be clamped to 0");
        assertEquals(480, result.y(), 0.1, "Y > 1 should be clamped to 480");
    }

    @Test
    void testDimensionUpdate() {
        mapper.updateCanvasDimensions(800, 600);
        assertEquals(800, mapper.getCanvasWidth(), 0.1);
        assertEquals(600, mapper.getCanvasHeight(), 0.1);

        HandLandmarkPoint normalized = new HandLandmarkPoint(0.5, 0.5, 1.0);
        HandLandmarkPoint canvas = mapper.normalizedToCanvasPixels(normalized);

        assertEquals(400, canvas.x(), 0.1, "Should use new dimensions (0.5 * 800)");
        assertEquals(300, canvas.y(), 0.1, "Should use new dimensions (0.5 * 600)");
    }

    @Test
    void testConfidence_Preservation() {
        HandLandmarkPoint normalized = new HandLandmarkPoint(0.5, 0.5, 0.85);
        HandLandmarkPoint canvas = mapper.normalizedToCanvasPixels(normalized);

        assertEquals(0.85, canvas.confidence(), 0.001, "Confidence should be preserved");
    }
}

