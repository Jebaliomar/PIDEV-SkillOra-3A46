package tn.esprit.tools;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public final class RightPanelAnimator {
    private RightPanelAnimator() {}

    public static void attach(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;
        scene.getRoot().applyCss();
        scene.getRoot().layout();

        Node panel = scene.lookup(".right-panel");
        if (panel == null) return;

        List<Circle> circles = new ArrayList<>();
        collectCircles(panel, circles);
        if (circles.isEmpty()) return;

        // Big halo circle - slow breathe + soft fade
        Circle halo = circles.get(0);
        ScaleTransition haloScale = new ScaleTransition(Duration.seconds(4.5), halo);
        haloScale.setFromX(1.0); haloScale.setFromY(1.0);
        haloScale.setToX(1.12);  haloScale.setToY(1.12);
        haloScale.setAutoReverse(true);
        haloScale.setCycleCount(ScaleTransition.INDEFINITE);
        haloScale.setInterpolator(Interpolator.EASE_BOTH);
        haloScale.play();

        FadeTransition haloFade = new FadeTransition(Duration.seconds(4.5), halo);
        haloFade.setFromValue(halo.getOpacity());
        haloFade.setToValue(Math.min(1.0, halo.getOpacity() + 0.18));
        haloFade.setAutoReverse(true);
        haloFade.setCycleCount(FadeTransition.INDEFINITE);
        haloFade.setInterpolator(Interpolator.EASE_BOTH);
        haloFade.play();

        // Inner gradient circle - slow counter-rotate + opposite scale
        if (circles.size() >= 2) {
            Circle inner = circles.get(1);
            RotateTransition rot = new RotateTransition(Duration.seconds(18), inner);
            rot.setByAngle(360);
            rot.setCycleCount(RotateTransition.INDEFINITE);
            rot.setInterpolator(Interpolator.LINEAR);
            rot.play();

            ScaleTransition innerScale = new ScaleTransition(Duration.seconds(4.5), inner);
            innerScale.setFromX(1.0); innerScale.setFromY(1.0);
            innerScale.setToX(0.92);  innerScale.setToY(0.92);
            innerScale.setAutoReverse(true);
            innerScale.setCycleCount(ScaleTransition.INDEFINITE);
            innerScale.setInterpolator(Interpolator.EASE_BOTH);
            innerScale.play();
        }

        // Footer dots - staggered opacity wave
        for (int i = 2; i < circles.size(); i++) {
            Circle dot = circles.get(i);
            FadeTransition f = new FadeTransition(Duration.seconds(1.4), dot);
            f.setFromValue(0.3);
            f.setToValue(1.0);
            f.setAutoReverse(true);
            f.setCycleCount(FadeTransition.INDEFINITE);
            f.setInterpolator(Interpolator.EASE_BOTH);
            f.setDelay(Duration.millis(250L * (i - 2)));
            f.play();
        }
    }

    private static void collectCircles(Node node, List<Circle> out) {
        if (node instanceof Circle) out.add((Circle) node);
        if (node instanceof Parent) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                collectCircles(child, out);
            }
        }
    }
}
