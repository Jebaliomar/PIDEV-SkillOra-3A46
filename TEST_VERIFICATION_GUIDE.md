# 🧪 GUIDE DE TEST ET VÉRIFICATION

## ✅ Avant de tester

1. **Recompiler le projet:**
```bash
mvn clean install
```

2. **Vérifier qu'il n'y a pas d'erreurs:**
```bash
mvn compile
```

3. **Exécuter les tests unitaires:**
```bash
mvn test -Dtest=CoordinateMapperTest
```

## 🎬 SCÉNARIO DE TEST 1: Alignement des points rouges

### Setup
- Ouvrir l'application
- Aller à l'écran "Camera - Ecriture gestuelle"
- Sélectionner le mode "Suivi caméra"
- Pointer une caméra connectée

### Procédure
1. **Placer la main** devant la caméra
2. **Observer les points bleus** (landmarks) - ils doivent suivre les 3 articulations de la main
3. **Faire un PINCH** (rapprocher le pouce et l'index)
4. **Observer les points rouges** - ils doivent apparaître

### ✅ Critères de succès
- [ ] Les points rouges apparaissent EXACTEMENT où l'overlay bleu les affiche
- [ ] **PAS DE DÉCALAGE HORIZONTAL**
- [ ] Les points rouges suivent le doigt en temps réel
- [ ] Le mouvement est fluide, pas de saut

### ❌ Signes d'erreur
- Points rouges décalés à gauche ou à droite
- Points rouges et landmarks bleus à des positions différentes
- Mouvement saccadé ou avec lag

## 🎬 SCÉNARIO DE TEST 2: Tracé du dessin

### Setup
- Même setup que le test 1
- Mode "Suivi caméra" activé

### Procédure
1. **Faire un PINCH** avec le pouce et l'index
2. **Bouger le doigt** dans l'air (garder le pinch)
3. **Observer le tracé** qui devrait apparaître sur le canvas

### ✅ Critères de succès
- [ ] Le tracé commence EXACTEMENT où les points rouges sont affichés
- [ ] Le tracé suit le mouvement du doigt
- [ ] Le tracé est **AU MÊME ENDROIT** que les points rouges
- [ ] L'écriture est fluide et lisible

### ❌ Signes d'erreur
- Tracé décalé par rapport aux points rouges
- Tracé qui commence au mauvais endroit
- Tracé qui saute

## 🎬 SCÉNARIO DE TEST 3: Debug labels

### Setup
- Application ouverte
- Mode "Suivi caméra" activé

### Procédure
1. Regarder le **label de debug** en bas à gauche:
   ```
   raw=(x, y) canvas=(px, py)
   ```

2. Faire bouger la main et observer les coordonnées:
   - `raw` doit varier de 0.0 à 1.0
   - `canvas` doit varier de 0 à width/height

3. **Vérifier l'alignment** visuellement:
   - Les points rouges doivent être au `canvas=(px, py)`

### ✅ Critères de succès
- [ ] Les coordonnées changent fluidement
- [ ] Les points rouges correspondent aux coordonnées affichées
- [ ] Pas de coordonnées négatives ou > 1.0 en raw
- [ ] Pas de coordonnées hors du canvas en pixels

### ❌ Signes d'erreur
- Coordonnées figées
- Sauts brutaux dans les coordonnées
- Points rouges à une position différente des coordonnées affichées

## 🎬 SCÉNARIO DE TEST 4: Mirroring horizontal

### Setup
- Caméra en face de vous
- Application ouverte

### Procédure
1. **Lever la main droite**
2. Observer: les points rouges doivent être sur le **CÔTÉ DROIT** de l'écran
3. **Lever la main gauche**
4. Observer: les points rouges doivent être sur le **CÔTÉ GAUCHE** de l'écran

### ✅ Critères de succès
- [ ] Main droite → points rouges droite ✓
- [ ] Main gauche → points rouges gauche ✓
- [ ] Mirroring est **CORRECT** et naturel

### ❌ Signes d'erreur
- Main droite → points rouges gauche (mirroring inversé)
- Points rouges au milieu quand on lève la main

## 🔧 TROUBLESHOOTING

### Problème: Points rouges décalés horizontalement

**Diagnostic:**
```java
// Vérifier dans les logs:
// 1. Le mirroring est-il activé?
coordinateMapper.isMirrorHorizontal();  // Doit être true

// 2. Les dimensions sont-elles correctes?
coordinateMapper.getCanvasWidth();   // Doit correspondre au canvas
coordinateMapper.getCanvasHeight();  // Doit correspondre au canvas
```

**Solution:**
```java
// Dans CameraReservationDialog.renderHandTracking():
coordinateMapper.updateCanvasDimensions(
    landmarksCanvas.getWidth(), 
    landmarksCanvas.getHeight()
);
// Doit être appelé AVANT handTrackingOverlayRenderer.render()
```

### Problème: Tracé au mauvais endroit

**Diagnostic:**
```
1. Les points rouges sont au bon endroit? (voir test 1)
2. Le tracé s'affiche mais au mauvais endroit?
```

**Solution:**
```java
// Vérifier que driveDrawingFromTrackedHand() utilise le mapper:
HandLandmarkPoint canvasPoint = coordinateMapper.normalizedToCanvasPixels(lastTrackingPoint);
double x = canvasPoint.x();
double y = canvasPoint.y();
// Puis tracer avec ces coordonnées
```

### Problème: Jitter / scintillement

**Cause:** Smoothing insuffisant

**Solution:**
1. Vérifier les paramètres dans `CameraReservationConfig`
2. Réduire le `beta` du filtre OneEuro
3. Augmenter le `minCutoff`

```java
guidePointFilter = new OneEuroPointFilter();
// Ajuster les paramètres si nécessaire
```

### Problème: Lag entre main réelle et points affichés

**Cause 1:** Framerate trop bas
```java
// Vérifier dans CameraReservationConfig:
config.getRenderFps();  // Doit être >= 30
```

**Cause 2:** Smoothing trop fort
```java
// Réduire le smoothing:
trackedPointerSmoother = TrackedPointerSmoother.fromConfig(config);
// Vérifier les paramètres du config
```

## 📊 TABLEAU DE VÉRIFICATION

Remplir ce tableau après chaque test:

| Test | Réussi | Observations | Action |
|------|--------|--------------|--------|
| Alignement points rouges | ☐ | | |
| Tracé au bon endroit | ☐ | | |
| Debug labels | ☐ | | |
| Mirroring horizontal | ☐ | | |
| Performance (30 FPS) | ☐ | | |

## 🔍 LOGS À VÉRIFIER

### Log au démarrage:
```
[INFO] Hand tracking desktop actif : detection couleur simplifiee avec repères bleus.
[INFO] Flux camera actif
```

### Logs pendant le suivi:
```
[INFO] handDetected=true, pinchDistance=0.0234, pinchActive=true, drawingActive=true, 
       currentState=DRAWING, rawFingertip=(0.3, 0.5), canvasPoint=(384, 240)
```

**À vérifier:**
- `handDetected=true` pendant suivi
- `pinchDistance` décroit au pinch
- `drawingActive=true` pendant pinch
- `canvasPoint` varie fluidement

### Erreurs à éviter:
```
❌ "Main non detectee"
❌ Coordonnées négatives
❌ Coordonnées > canvas dimensions
❌ Logs arrêtés (signifie un freeze)
```

## 🎯 RÉSULTAT FINAL ATTENDU

Après correction et tests réussis:

✅ **Points rouges:**
- Suivent le doigt exactement
- Pas de décalage
- Mouvement fluide

✅ **Tracé:**
- Débute au bon endroit
- Suit le doigt en temps réel
- Pas de saut

✅ **Overlay:**
- Tous les éléments alignés
- Pas de désynchronisation
- Mouvement naturel

✅ **Performance:**
- 30+ FPS constant
- Pas de lag
- Pas de jitter

---

**Si tous les tests ✅ réussis → La correction est complète! 🎉**

