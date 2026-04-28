# 📘 GUIDE D'UTILISATION - SUIVI DES POINTS ROUGES CORRIGÉ

## Vue d'ensemble

Le système de détection de main et d'overlay des points rouges utilise maintenant une architecture centralisée avec le `CoordinateMapper`.

## Architecture système

```
┌─────────────────────────────────────────────────────────────────┐
│                    CameraReservationDialog                      │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │ renderHandTracking(CameraFrame frame)                     │ │
│  │                                                            │ │
│  │ 1. detectorState = handTrackingDetector.detect(...)       │ │
│  │    (coordonnées brutes caméra)                            │ │
│  │                                                            │ │
│  │ 2. effectiveState = stabilizeTrackingState(detectorState) │ │
│  │    (gère les frames manquantes)                           │ │
│  │                                                            │ │
│  │ 3. smoothedState = appliquer smoothing                    │ │
│  │    (en coordonnées normalisées 0.0-1.0)                   │ │
│  │                                                            │ │
│  │ 4. coordinateMapper.updateCanvasDimensions(w, h)          │ │
│  │    handTrackingOverlayRenderer.updateCanvasDimensions(...)│ │
│  │                                                            │ │
│  │ 5. handTrackingOverlayRenderer.render(...)                │ │
│  │    (renderise les points rouges avec conversion)          │ │
│  │                                                            │ │
│  │ 6. driveDrawingFromTrackedHand(smoothedState)             │ │
│  │    (utilise coordinateMapper pour le tracé)               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  Composants:                                                     │
│  • handTrackingDetector: SimpleColorHandTrackingDetector         │
│  • coordinateMapper: CoordinateMapper (NOUVEAU)                 │
│  • handTrackingOverlayRenderer: HandTrackingOverlayRenderer      │
│  • trackedPointerSmoother: Lissage des points                    │
│  • guidePointFilter: Filtre OneEuro                              │
│  • strokeEngine: Moteur de dessin                                │
└─────────────────────────────────────────────────────────────────┘
```

## Flux de données

### Phase 1: Détection
```
BufferedImage (frame caméra brute)
         ↓
SimpleColorHandTrackingDetector.detect()
         ↓
TrackedHandState (landmarks en 0.0-1.0)
```

### Phase 2: Stabilisation
```
TrackedHandState (brute)
         ↓
stabilizeTrackingState() (gère 8 frames sans main)
         ↓
TrackedHandState (stabilisée)
```

### Phase 3: Smoothing
```
HandLandmarkPoint (stabilisée)
         ↓
trackedPointerSmoother.update()
         ↓
HandLandmarkPoint (lissée)
         ↓
guidePointFilter.filter() (OneEuro filter)
         ↓
HandLandmarkPoint (filtrée finale)
```

### Phase 4: Rendu overlay
```
HandLandmarkPoint (normalisée 0.0-1.0)
         ↓
coordinateMapper.normalizedToCanvasPixels()
         ↓
HandLandmarkPoint (en pixels avec mirroring appliqué)
         ↓
handTrackingOverlayRenderer.render() sur landmarksCanvas
         ↓
Points rouges affichés à l'écran ✓
```

### Phase 5: Tracé
```
HandLandmarkPoint (normalisée)
         ↓
coordinateMapper.normalizedToCanvasPixels()
         ↓
Coordonnées pixels
         ↓
inkGraphics.drawLine/Curve()
         ↓
Tracé affiché sur inkCanvas ✓
```

## Utilisation du CoordinateMapper

### Initialisation (dans CameraReservationDialog)
```java
private final CoordinateMapper coordinateMapper = new CoordinateMapper();
```

### Mise à jour des dimensions
```java
// À chaque frame ou lors du resize du canvas
coordinateMapper.updateCanvasDimensions(landmarksCanvas.getWidth(), landmarksCanvas.getHeight());
```

### Conversion normalisée → pixels
```java
// Convertir un point de coordonnées normalisées en pixels
HandLandmarkPoint normalized = new HandLandmarkPoint(0.5, 0.3, 1.0);
HandLandmarkPoint pixels = coordinateMapper.normalizedToCanvasPixels(normalized);
double x = pixels.x();  // Par exemple: 320 (pour width=640)
double y = pixels.y();  // Par exemple: 144 (pour height=480)
```

### Conversion pixels → normalisées
```java
// Inverse: convertir des pixels en coordonnées normalisées
HandLandmarkPoint pixelPoint = new HandLandmarkPoint(320, 144, 1.0);
HandLandmarkPoint normalized = coordinateMapper.canvasPixelsToNormalized(pixelPoint);
// Résultat: (0.5, 0.3, 1.0)
```

### Conversion directe X et Y
```java
// Convertir directement une coordonnée X normalisée en pixel
double canvasX = coordinateMapper.normalizedXToCanvasX(0.5);  // 320

// Convertir directement une coordonnée Y normalisée en pixel
double canvasY = coordinateMapper.normalizedYToCanvasY(0.3);  // 144
```

### Contrôle du mirroring
```java
// Vérifier si le mirroring est activé
boolean mirrored = coordinateMapper.isMirrorHorizontal();  // true par défaut

// Changer le mirroring (rare)
coordinateMapper.setMirrorHorizontal(false);
```

## Points importants

### ✅ À FAIRE

✅ **Toujours utiliser CoordinateMapper** pour convertir les coordonnées détectées
```java
HandLandmarkPoint canvasPoint = coordinateMapper.normalizedToCanvasPixels(detectedPoint);
```

✅ **Mettre à jour les dimensions** avant chaque rendu
```java
coordinateMapper.updateCanvasDimensions(canvas.getWidth(), canvas.getHeight());
```

✅ **Laisser le mirroring activé par défaut** (sauf cas spécial)
```java
coordinateMapper.setMirrorHorizontal(true);  // Défaut
```

✅ **Garder les coordonnées intermédiaires en normalisé**
```java
// Correct: travailler en 0.0-1.0, convertir en pixels seulement au rendu
HandLandmarkPoint normalized = detector.detect(...);  // 0.0-1.0
HandLandmarkPoint smoothed = smoother.update(normalized);  // 0.0-1.0
HandLandmarkPoint filtered = filter.filter(smoothed);  // 0.0-1.0
// ... puis convertir seulement au rendu ou tracé
```

### ❌ À ÉVITER

❌ **Ne pas faire de mirroring manuel**
```java
// ✗ MAUVAIS:
double x = 1.0 - point.x();  // Mirroring manuel, conflictuel avec CoordinateMapper

// ✓ BON:
HandLandmarkPoint converted = coordinateMapper.normalizedToCanvasPixels(point);
```

❌ **Ne pas multiplier directement par width/height**
```java
// ✗ MAUVAIS:
double x = point.x() * canvas.getWidth();  // Pas de mirroring!

// ✓ BON:
HandLandmarkPoint converted = coordinateMapper.normalizedToCanvasPixels(point);
double x = converted.x();
```

❌ **Ne pas mélanger plusieurs repères**
```java
// ✗ MAUVAIS:
double canvasX = point.x() * width;  // Repère pixel
HandLandmarkPoint normalized = new HandLandmarkPoint(canvasX / width, ...);  // Retour en normalisé?

// ✓ BON:
HandLandmarkPoint canvasPoint = coordinateMapper.normalizedToCanvasPixels(point);
// Utiliser directement canvasPoint.x() et canvasPoint.y()
```

## Debugging

### Vérifier l'alignment

Les labels de debug affichent les coordonnées de la caméra brute et du canvas:
```
raw=(0.3, 0.5) canvas=(384, 240)
```

- `raw`: coordonnées normalisées brutes du détecteur (0.0-1.0)
- `canvas`: coordonnées converties en pixels du canvas

**Pour vérifier:** Les points rouges et le curseur doivent être au même endroit.

### Logs de debug

```
handDetected=true, pinchDistance=0.0234, pinchActive=true, drawingActive=true, 
currentState=DRAWING, rawFingertip=(0.3, 0.5), canvasPoint=(384, 240)
```

**À vérifier:**
- `pinchDistance` décroit quand vous faites un pinch
- `drawingActive=true` pendant un pinch
- `canvasPoint` varie fluide

## Tests unitaires

Exécuter les tests pour valider le CoordinateMapper:
```bash
mvn test -Dtest=CoordinateMapperTest
```

**Teste:**
- Conversion normalisé → pixels
- Mirroring horizontal
- Cas limites (0, 0) et (1, 1)
- Round-trip (normalisé → pixels → normalisé)
- Clamping des valeurs

## Performance

- **CoordinateMapper:** O(1) pour chaque conversion (pas d'allocation)
- **HandTrackingOverlayRenderer:** O(n) où n = nombre de landmarks (3-4)
- **Rendu total:** < 1ms par frame (30 FPS compatible)

## Troubleshooting

### Problème: Points rouges décalés horizontalement
**Cause:** Mirroring mal appliqué
**Solution:** 
```java
// Vérifier:
coordinateMapper.isMirrorHorizontal();  // Doit être true
coordinateMapper.updateCanvasDimensions(w, h);  // Doit être appelé
```

### Problème: Tracé au mauvais endroit
**Cause:** `driveDrawingFromTrackedHand()` n'utilise pas le mapper
**Solution:**
```java
// Corriger le code:
HandLandmarkPoint canvasPoint = coordinateMapper.normalizedToCanvasPixels(lastTrackingPoint);
double x = canvasPoint.x();
double y = canvasPoint.y();
// Puis dessiner avec x, y
```

### Problème: Jitter / scintillement
**Cause:** Pas assez de smoothing
**Solution:** Vérifier les paramètres du smoother dans `CameraReservationConfig`
```java
trackedPointerSmoother = TrackedPointerSmoother.fromConfig(config);
guidePointFilter = new OneEuroPointFilter();  // Réduire le beta si besoin
```

## Améliorations futures possibles

- [ ] Ajouter une transformation d'échelle (zoom)
- [ ] Supporter la rotation de l'écran
- [ ] Ajouter un offset d'étalonnage
- [ ] Profils de conversion (USB vs socket caméra)

