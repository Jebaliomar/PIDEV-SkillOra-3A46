package tn.esprit.tools;

/**
 * Gestionnaire centralisé pour la conversion des coordonnées de détection des mains.
 *
 * Cette classe gère la conversion entre:
 * - Coordonnées brutes de la caméra (normalisées 0.0-1.0)
 * - Coordonnées du canvas JavaFX (en pixels)
 *
 * Elle applique également le mirroring horizontal de manière cohérente et centralisée.
 */
public class CoordinateMapper {

    private double canvasWidth = 640.0;
    private double canvasHeight = 480.0;
    private boolean mirrorHorizontal = true;  // Mirroring par défaut pour correspondre à l'affichage

    /**
     * Crée un nouveau CoordinateMapper avec les dimensions par défaut.
     */
    public CoordinateMapper() {
        // Dimensions par défaut
    }

    /**
     * Met à jour les dimensions du canvas.
     *
     * Doit être appelée à chaque fois que le canvas est resizé ou au démarrage.
     *
     * @param width largeur du canvas en pixels
     * @param height hauteur du canvas en pixels
     */
    public void updateCanvasDimensions(double width, double height) {
        this.canvasWidth = width;
        this.canvasHeight = height;
    }

    /**
     * Retourne la largeur actuelle du canvas.
     *
     * @return largeur en pixels
     */
    public double getCanvasWidth() {
        return canvasWidth;
    }

    /**
     * Retourne la hauteur actuelle du canvas.
     *
     * @return hauteur en pixels
     */
    public double getCanvasHeight() {
        return canvasHeight;
    }

    /**
     * Active ou désactive le mirroring horizontal.
     *
     * @param mirror true pour activer le mirroring (défaut), false sinon
     */
    public void setMirrorHorizontal(boolean mirror) {
        this.mirrorHorizontal = mirror;
    }

    /**
     * Retourne l'état actuel du mirroring horizontal.
     *
     * @return true si le mirroring est activé
     */
    public boolean isMirrorHorizontal() {
        return mirrorHorizontal;
    }

    /**
     * Convertit une coordonnée X normalisée (0.0-1.0) en coordonnée pixel sur le canvas.
     *
     * Applique le mirroring si activé.
     *
     * @param normalizedX coordonnée X normalisée (0.0-1.0)
     * @return coordonnée X en pixels (0 à canvasWidth)
     */
    public double normalizedXToCanvasX(double normalizedX) {
        // Clamp la valeur entre 0 et 1
        double x = Math.max(0.0, Math.min(1.0, normalizedX));

        // Applique le mirroring si activé
        if (mirrorHorizontal) {
            x = 1.0 - x;
        }

        // Convertit en pixels
        return x * canvasWidth;
    }

    /**
     * Convertit une coordonnée Y normalisée (0.0-1.0) en coordonnée pixel sur le canvas.
     *
     * @param normalizedY coordonnée Y normalisée (0.0-1.0)
     * @return coordonnée Y en pixels (0 à canvasHeight)
     */
    public double normalizedYToCanvasY(double normalizedY) {
        // Clamp la valeur entre 0 et 1
        double y = Math.max(0.0, Math.min(1.0, normalizedY));

        // Convertit en pixels (pas de mirroring vertical)
        return y * canvasHeight;
    }

    /**
     * Convertit un point en coordonnées normalisées en point en pixels sur le canvas.
     *
     * @param normalizedPoint point en coordonnées normalisées
     * @return point en coordonnées pixels
     */
    public HandLandmarkPoint normalizedToCanvasPixels(HandLandmarkPoint normalizedPoint) {
        if (normalizedPoint == null) {
            return null;
        }

        double pixelX = normalizedXToCanvasX(normalizedPoint.x());
        double pixelY = normalizedYToCanvasY(normalizedPoint.y());
        double confidence = normalizedPoint.confidence();

        return new HandLandmarkPoint(pixelX, pixelY, confidence);
    }

    /**
     * Convertit un point en pixels en point en coordonnées normalisées.
     *
     * @param pixelPoint point en coordonnées pixels
     * @return point en coordonnées normalisées
     */
    public HandLandmarkPoint canvasPixelsToNormalized(HandLandmarkPoint pixelPoint) {
        if (pixelPoint == null) {
            return null;
        }

        // Convertit de pixels en normalisé
        double normalizedX = pixelPoint.x() / canvasWidth;
        double normalizedY = pixelPoint.y() / canvasHeight;

        // Inverse le mirroring si activé
        if (mirrorHorizontal) {
            normalizedX = 1.0 - normalizedX;
        }

        // Clamp entre 0 et 1
        normalizedX = Math.max(0.0, Math.min(1.0, normalizedX));
        normalizedY = Math.max(0.0, Math.min(1.0, normalizedY));

        double confidence = pixelPoint.confidence();

        return new HandLandmarkPoint(normalizedX, normalizedY, confidence);
    }

    /**
     * Convertit une coordonnée X de pixels en coordonnée X normalisée.
     *
     * @param pixelX coordonnée X en pixels
     * @return coordonnée X normalisée (0.0-1.0)
     */
    public double canvasXToNormalizedX(double pixelX) {
        double normalizedX = pixelX / canvasWidth;

        // Inverse le mirroring si activé
        if (mirrorHorizontal) {
            normalizedX = 1.0 - normalizedX;
        }

        // Clamp entre 0 et 1
        return Math.max(0.0, Math.min(1.0, normalizedX));
    }

    /**
     * Convertit une coordonnée Y de pixels en coordonnée Y normalisée.
     *
     * @param pixelY coordonnée Y en pixels
     * @return coordonnée Y normalisée (0.0-1.0)
     */
    public double canvasYToNormalizedY(double pixelY) {
        double normalizedY = pixelY / canvasHeight;

        // Clamp entre 0 et 1
        return Math.max(0.0, Math.min(1.0, normalizedY));
    }

    @Override
    public String toString() {
        return String.format("CoordinateMapper[canvas=%.0fx%.0f, mirrorH=%b]",
                             canvasWidth, canvasHeight, mirrorHorizontal);
    }
}

