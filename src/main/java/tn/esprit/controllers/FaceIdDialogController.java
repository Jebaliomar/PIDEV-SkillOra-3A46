package tn.esprit.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import tn.esprit.tools.FaceIdServer;
import tn.esprit.tools.FaceIdServer.Session;
import tn.esprit.tools.FaceIdServer.SessionStatus;
import tn.esprit.tools.FaceIdServer.SessionType;
import tn.esprit.tools.QrGen;

public class FaceIdDialogController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label urlLabel;
    @FXML private Label statusLabel;
    @FXML private ImageView qrImage;

    private Stage stage;
    private Session session;
    private Timeline poller;
    private double[] descriptor;
    private boolean confirmed;

    public void init(Stage stage, Session session, String title, String subtitle) throws Exception {
        this.stage = stage;
        this.session = session;
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
        String url = FaceIdServer.get().urlFor(session);
        urlLabel.setText(url);
        qrImage.setImage(QrGen.make(url, 520));
        startPolling();
    }

    private void startPolling() {
        poller = new Timeline(new KeyFrame(Duration.millis(700), e -> {
            Session s = FaceIdServer.get_unsafe(session.token);
            if (s == null) return;
            if (s.status == SessionStatus.CAPTURED && s.descriptor != null) {
                descriptor = s.descriptor;
                confirmed = true;
                statusLabel.setText("Face captured. Closing...");
                poller.stop();
                Timeline close = new Timeline(new KeyFrame(Duration.millis(700), ev -> stage.close()));
                close.play();
            } else if (s.status == SessionStatus.FAILED) {
                statusLabel.setText("Failed: " + (s.error == null ? "unknown" : s.error));
                poller.stop();
            }
        }));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }

    @FXML
    public void handleCancel() {
        if (poller != null) poller.stop();
        FaceIdServer.get_unsafe_remove(session.token);
        stage.close();
    }

    public double[] getDescriptor() { return descriptor; }
    public boolean isConfirmed() { return confirmed; }

    public static FaceIdDialogController show(SessionType type, String title, String subtitle, String email) {
        try {
            Session session = FaceIdServer.get().createSession(type, email);
            FXMLLoader loader = new FXMLLoader(FaceIdDialogController.class.getResource("/fxml/FaceIdDialog.fxml"));
            javafx.scene.Parent root = loader.load();
            FaceIdDialogController ctrl = loader.getController();
            Stage dialog = new Stage(StageStyle.UTILITY);
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Face ID");
            dialog.setScene(new Scene(root));
            dialog.setResizable(false);
            ctrl.init(dialog, session, title, subtitle);
            dialog.showAndWait();
            FaceIdServer.get_unsafe_remove(session.token);
            return ctrl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
