package tn.esprit.controllers.rendezvous;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.entities.Notification;
import tn.esprit.entities.RendezVous;
import tn.esprit.services.AvailabilitySlotService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.RendezVousService;
import tn.esprit.tools.MyConnection;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class RendezVousController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter NOTIFICATION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter CARD_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM yyyy")
            .toFormatter();
    private static final String BTN_ACTIVE = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";
    private static final String BTN_INACTIVE = "-fx-background-color: #0f1b38; -fx-text-fill: #cbd5e1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";
    private static final double DETAILS_KEY_WIDTH = 170;
    private static final int CURRENT_USER_ID = parseCurrentUserId();
    private static final String CURRENT_USER_ROLE = normalizeRole(System.getProperty("skillora.role", "student"));

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private FlowPane cardsContainer;

    @FXML
    private Button filterAllBtn;

    @FXML
    private Button filterPendingBtn;

    @FXML
    private Button filterConfirmedBtn;

    @FXML
    private Button filterRefusedBtn;

    @FXML
    private Button notificationButton;

    private final RendezVousService rendezVousService = new RendezVousService();
    private final AvailabilitySlotService availabilitySlotService = new AvailabilitySlotService();
    private final NotificationService notificationService = new NotificationService();
    private List<RendezVous> allRendezVous = List.of();
    private RendezVous selectedRendezVous;
    private RendezVousFilter currentFilter = RendezVousFilter.ALL;

    private enum RendezVousFilter {
        ALL,
        PENDING,
        CONFIRMED,
        REFUSED
    }

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender());
        setActiveFilter(RendezVousFilter.ALL);
        refreshRendezVous();
        updateNotificationBadge();
    }

    @FXML
    private void refreshRendezVous() {
        try {
            allRendezVous = rendezVousService.getAll().stream()
                    .filter(this::isVisibleForCurrentUser)
                    .collect(Collectors.toList());
            selectedRendezVous = null;
            applyFiltersAndRender();
            updateNotificationBadge();
        } catch (SQLException exception) {
            showError("Unable to load rendez-vous", exception);
        }
    }

    @FXML
    private void setFilterAll() {
        setActiveFilter(RendezVousFilter.ALL);
    }

    @FXML
    private void setFilterPending() {
        setActiveFilter(RendezVousFilter.PENDING);
    }

    @FXML
    private void setFilterConfirmed() {
        setActiveFilter(RendezVousFilter.CONFIRMED);
    }

    @FXML
    private void setFilterRefused() {
        setActiveFilter(RendezVousFilter.REFUSED);
    }

    private void setActiveFilter(RendezVousFilter filter) {
        currentFilter = filter;
        filterAllBtn.setStyle(filter == RendezVousFilter.ALL ? BTN_ACTIVE : BTN_INACTIVE);
        filterPendingBtn.setStyle(filter == RendezVousFilter.PENDING ? BTN_ACTIVE : BTN_INACTIVE);
        filterConfirmedBtn.setStyle(filter == RendezVousFilter.CONFIRMED ? BTN_ACTIVE : BTN_INACTIVE);
        filterRefusedBtn.setStyle(filter == RendezVousFilter.REFUSED ? BTN_ACTIVE : BTN_INACTIVE);
        applyFiltersAndRender();
    }

    private void applyFiltersAndRender() {
        updateFilterLabels();
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<RendezVous> filtered = allRendezVous.stream()
                .filter(rdv -> matchesFilter(rdv, currentFilter))
                .filter(rdv -> matchesSearch(rdv, q))
                .collect(Collectors.toList());
        renderCards(filtered);
        statusLabel.setText(filtered.size() + " rendez-vous shown");
    }

    private boolean matchesFilter(RendezVous rdv, RendezVousFilter filter) {
        return switch (filter) {
            case ALL -> true;
            case PENDING -> isPendingStatus(rdv.getStatut());
            case CONFIRMED -> isConfirmedStatus(rdv.getStatut());
            case REFUSED -> isRefusedStatus(rdv.getStatut());
        };
    }

    private void updateFilterLabels() {
        long all = allRendezVous.size();
        long pending = allRendezVous.stream().filter(rdv -> isPendingStatus(rdv.getStatut())).count();
        long confirmed = allRendezVous.stream().filter(rdv -> isConfirmedStatus(rdv.getStatut())).count();
        long refused = allRendezVous.stream().filter(rdv -> isRefusedStatus(rdv.getStatut())).count();

        filterAllBtn.setText("Tous (" + all + ")");
        filterPendingBtn.setText("En attente (" + pending + ")");
        filterConfirmedBtn.setText("Confirmés (" + confirmed + ")");
        filterRefusedBtn.setText("Refusés (" + refused + ")");
    }

    private boolean matchesSearch(RendezVous rdv, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return String.valueOf(rdv.getId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getStudentId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getProfessorId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getSlotId()).toLowerCase().contains(q)
                || normalizeDefault(rdv.getStatut(), "-").toLowerCase().contains(q)
                || normalizeDefault(rdv.getMeetingType(), "-").toLowerCase().contains(q)
                || normalizeDefault(formatDateTime(rdv.getCreatedAt()), "-").toLowerCase().contains(q);
    }

    private void renderCards(List<RendezVous> rendezVousList) {
        cardsContainer.getChildren().clear();
        if (rendezVousList.isEmpty()) {
            Label empty = new Label("No rendez-vous found with current filter.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            cardsContainer.getChildren().add(empty);
            return;
        }
        for (RendezVous rdv : rendezVousList) {
            cardsContainer.getChildren().add(buildRendezVousCard(rdv));
        }
    }

    private VBox buildRendezVousCard(RendezVous rdv) {
        boolean selected = selectedRendezVous != null
                && selectedRendezVous.getId() != null
                && selectedRendezVous.getId().equals(rdv.getId());

        VBox card = new VBox(10);
        String borderColor = selected ? "#38bdf8" : "#1f3b70";
        card.setStyle("-fx-background-color: #020f2d; -fx-border-color: " + borderColor + "; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 16;");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(540);
        card.setMinWidth(520);

        HBox header = new HBox(8);
        Label idLabel = new Label("Rendez-vous " + safeNumber(rdv.getId()));
        idLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 30px; -fx-font-weight: 800;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(statusDisplay(rdv.getStatut()));
        statusBadge.setStyle(statusStyle(rdv.getStatut()));
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        HBox dateRow = new HBox(12);
        dateRow.setStyle("-fx-border-color: #dbeafe; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 12;");
        Label dateLabel = new Label(formatCardDate(rdv.getCreatedAt()) + "  —  " + meetingTypeDisplay(rdv.getMeetingType()));
        dateLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
        Region dateSpacer = new Region();
        HBox.setHgrow(dateSpacer, javafx.scene.layout.Priority.ALWAYS);
        Label durationLabel = new Label("(60 min)");
        durationLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: 700;");
        dateRow.getChildren().addAll(dateLabel, dateSpacer, durationLabel);

        Label line1 = new Label("Professeur :  #" + safeNumber(rdv.getProfessorId()));
        line1.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 15px;");
        Label line2 = new Label("Etudiant  :  #" + safeNumber(rdv.getStudentId()));
        line2.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 15px;");
        Label line3 = new Label("Cours     :  " + (rdv.getCourseId() == null ? "-" : ("#" + rdv.getCourseId())));
        line3.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 15px;");

        HBox linkRow = new HBox();
        linkRow.setStyle("-fx-border-color: #dbeafe; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 12;");
        String linkText = normalizeDefault(rdv.getMeetingLink(), normalizeDefault(rdv.getLocationLabel(), normalizeDefault(rdv.getLocation(), "-")));
        Label linkLabel = new Label("Lien / Lieu : " + linkText);
        linkLabel.setWrapText(true);
        linkLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
        linkRow.getChildren().add(linkLabel);

        HBox actions = new HBox(10);
        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle("-fx-background-color: #0b1a3a; -fx-border-color: #dbeafe; -fx-border-radius: 18; -fx-background-radius: 18; -fx-text-fill: #e2e8f0; -fx-font-weight: 700;");
        detailsBtn.setOnAction(event -> showRendezVousDetailsDialog(rdv));
        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 18; -fx-text-fill: white; -fx-font-weight: 700;");
        editBtn.setOnAction(event -> {
            selectedRendezVous = rdv;
            editSelectedRendezVous();
        });
        Button pdfBtn = new Button("Exporter PDF");
        pdfBtn.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 18; -fx-text-fill: #e2e8f0; -fx-font-weight: 700;");
        pdfBtn.setOnAction(event -> exportRendezVousDetailsPdf(rdv));

        if (isProfessorMode()) {
            boolean pending = isPendingStatus(rdv.getStatut());
            Label meetingLinkLabel = new Label("Lien de réunion");
            meetingLinkLabel.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: 700;");
            TextField meetingLinkField = new TextField(normalizeDefault(rdv.getMeetingLink(), ""));
            meetingLinkField.setPromptText("https://...");
            meetingLinkField.setDisable(!pending);
            styleTextField(meetingLinkField);
            HBox.setHgrow(meetingLinkField, javafx.scene.layout.Priority.ALWAYS);

            Button acceptBtn = new Button("Accepter");
            acceptBtn.setStyle("-fx-background-color: #16a34a; -fx-background-radius: 18; -fx-text-fill: white; -fx-font-weight: 700;");
            acceptBtn.setDisable(!pending);
            acceptBtn.setOnAction(event -> updateStatusAsProfessor(rdv, "confirme", meetingLinkField.getText()));

            Button refuseBtn = new Button("Refuser");
            refuseBtn.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 18; -fx-text-fill: white; -fx-font-weight: 700;");
            refuseBtn.setDisable(!pending);
            refuseBtn.setOnAction(event -> updateStatusAsProfessor(rdv, "refuse"));

            VBox professorActions = new VBox(8);
            HBox firstRow = new HBox(10, detailsBtn, pdfBtn);
            HBox secondRow = new HBox(10, meetingLinkField, acceptBtn, refuseBtn);
            HBox.setHgrow(meetingLinkField, javafx.scene.layout.Priority.ALWAYS);
            VBox linkInput = new VBox(4, meetingLinkLabel, secondRow);
            professorActions.getChildren().addAll(firstRow, linkInput);
            card.getChildren().addAll(header, dateRow, line1, line2, line3, linkRow, professorActions);
        } else {
            actions.getChildren().addAll(detailsBtn, editBtn, pdfBtn);
            card.getChildren().addAll(header, dateRow, line1, line2, line3, linkRow, actions);
        }

        card.setOnMouseClicked(event -> {
            selectedRendezVous = rdv;
            applyFiltersAndRender();
        });
        return card;
    }

    private String statusStyle(String rawStatus) {
        String status = normalizeDefault(rawStatus, "").toLowerCase();
        if (status.contains("attente")) {
            return "-fx-background-color: #78350f; -fx-text-fill: #fcd34d; -fx-font-weight: 700; -fx-padding: 6 14; -fx-background-radius: 16;";
        }
        if (status.contains("confirm")) {
            return "-fx-background-color: #a7f3d0; -fx-text-fill: #065f46; -fx-font-weight: 700; -fx-padding: 6 14; -fx-background-radius: 16;";
        }
        if (status.contains("refus") || status.contains("rejet")) {
            return "-fx-background-color: #7f1d1d; -fx-text-fill: #fecaca; -fx-font-weight: 700; -fx-padding: 6 14; -fx-background-radius: 16;";
        }
        return "-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-weight: 700; -fx-padding: 6 14; -fx-background-radius: 16;";
    }

    @FXML
    private void handleCreate() {
        if (!isStudentMode()) {
            showWarning("Action not allowed", "Only students can create rendez-vous.");
            return;
        }

        Optional<RendezVous> input = showCreateRendezVousDialog();
        if (input.isEmpty()) {
            return;
        }

        RendezVous created = input.get();
        try {
            rendezVousService.add(created);
            markSlotBookedState(created.getSlotId(), true);
            createProfessorNotification(created);
            refreshRendezVous();
            statusLabel.setText("Rendez-vous added successfully");
        } catch (SQLException exception) {
            showError("Unable to add rendez-vous", exception);
        }
    }

    @FXML
    private void handleEdit() {
        editSelectedRendezVous();
    }

    private void editSelectedRendezVous() {
        if (selectedRendezVous == null) {
            showWarning("Selection required", "Select a rendez-vous card before trying to edit.");
            return;
        }
        if (!canStudentManage(selectedRendezVous)) {
            showWarning("Action not allowed", "You can edit only your own rendez-vous as a student.");
            return;
        }

        Optional<RendezVous> input = showEditRendezVousDialog(selectedRendezVous);
        if (input.isEmpty()) {
            return;
        }

        RendezVous updated = input.get();
        updated.setId(selectedRendezVous.getId());
        Integer previousSlotId = selectedRendezVous.getSlotId();
        try {
            boolean ok = rendezVousService.update(updated);
            if (ok) {
                if (!sameInteger(previousSlotId, updated.getSlotId())) {
                    markSlotBookedState(previousSlotId, false);
                    markSlotBookedState(updated.getSlotId(), true);
                }
                refreshRendezVous();
                statusLabel.setText("Rendez-vous #" + selectedRendezVous.getId() + " updated");
            } else {
                showWarning("Update failed", "The selected rendez-vous could not be updated.");
            }
        } catch (SQLException exception) {
            showError("Unable to update rendez-vous", exception);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedRendezVous == null) {
            showWarning("Selection required", "Select a rendez-vous card before trying to delete.");
            return;
        }
        if (!canStudentManage(selectedRendezVous)) {
            showWarning("Action not allowed", "You can delete only your own rendez-vous as a student.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete rendez-vous #" + selectedRendezVous.getId() + "?",
                ButtonType.YES,
                ButtonType.CANCEL
        );
        confirmation.setHeaderText("Confirm deletion");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) {
            return;
        }

        try {
            Integer deletedSlotId = selectedRendezVous.getSlotId();
            boolean deleted = rendezVousService.delete(selectedRendezVous.getId());
            if (deleted) {
                Integer deletedId = selectedRendezVous.getId();
                markSlotBookedState(deletedSlotId, false);
                selectedRendezVous = null;
                refreshRendezVous();
                statusLabel.setText("Rendez-vous #" + deletedId + " deleted");
            } else {
                showWarning("Delete failed", "The selected rendez-vous could not be deleted.");
            }
        } catch (SQLException exception) {
            showError("Unable to delete the selected rendez-vous", exception);
        }
    }

    @FXML
    private void goToEvents(ActionEvent event) {
        showWarning("Events", "Events page is not implemented yet.");
    }

    @FXML
    private void goToRendezVous(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/rendezvous/rendezvous-list.fxml", "SkillOra - RendezVous");
    }

    @FXML
    private void goToSlots(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/availability-slot/availability-slot-list.fxml", "SkillOra - Availability Slots");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 1160, 740));
            stage.setTitle(title);
            stage.show();
        } catch (IOException exception) {
            showError("Unable to open page", exception);
        }
    }

    private void showRendezVousDetailsDialog(RendezVous rdv) {
        if (rdv == null) {
            showWarning("Selection required", "Select a rendez-vous first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails rendez-vous #" + safeNumber(rdv.getId()));
        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        Label title = new Label("Rendez-vous #" + safeNumber(rdv.getId()));
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 24px; -fx-font-weight: 800;");
        Label subtitle = new Label("Statut : " + statusDisplay(rdv.getStatut()));
        subtitle.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 14px; -fx-font-weight: 700;");

        Button exportBtn = new Button("Exporter ce détail en PDF");
        exportBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 12;");
        exportBtn.setOnAction(event -> exportRendezVousDetailsPdf(rdv));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(12, new VBox(4, title, subtitle), headerSpacer, exportBtn);
        header.setStyle("-fx-alignment: center-left;");

        VBox detailsBox = new VBox(8);
        detailsBox.getChildren().addAll(
                buildDetailsRow("ID", "#" + safeNumber(rdv.getId())),
                buildDetailsRow("Statut", statusDisplay(rdv.getStatut())),
                buildDetailsRow("Professeur", "#" + safeNumber(rdv.getProfessorId())),
                buildDetailsRow("Etudiant", "#" + safeNumber(rdv.getStudentId())),
                buildDetailsRow("Cours", rdv.getCourseId() == null ? "-" : "#" + rdv.getCourseId()),
                buildDetailsRow("Créneau", resolveSlotSummary(rdv.getSlotId())),
                buildDetailsRow("Type", meetingTypeDisplay(rdv.getMeetingType())),
                buildDetailsRow("Lien de réunion", normalizeDefault(rdv.getMeetingLink(), "-")),
                buildDetailsRow("Lieu", normalizeDefault(rdv.getLocationLabel(), normalizeDefault(rdv.getLocation(), "-"))),
                buildDetailsRow("Coordonnées", formatCoordinates(rdv.getLocationLat(), rdv.getLocationLng())),
                buildDetailsRow("Message", normalizeDefault(rdv.getMessage(), "-")),
                buildDetailsRow("Raison de refus", normalizeDefault(rdv.getRefusalReason(), "-")),
                buildDetailsRow("PDF cours", normalizeDefault(rdv.getCoursePdfName(), "-")),
                buildDetailsRow("Créé le", formatDateTime(rdv.getCreatedAt()))
        );

        ScrollPane detailsScroll = new ScrollPane(detailsBox);
        detailsScroll.setFitToWidth(true);
        detailsScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        detailsScroll.setPrefViewportHeight(560);

        VBox content = new VBox(14, header, detailsScroll);
        content.setStyle("-fx-background-color: #0b1736; -fx-padding: 18; -fx-border-color: #27406d; -fx-border-radius: 14; -fx-background-radius: 14;");
        content.setPrefWidth(920);
        content.setPrefHeight(650);

        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        pane.setContent(content);

        Button closeBtn = (Button) pane.lookupButton(closeType);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #162546; -fx-text-fill: #e2e8f0; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-color: #2e4677; -fx-border-radius: 12;");
        }

        dialog.showAndWait();
    }

    private HBox buildDetailsRow(String labelText, String valueText) {
        Label label = new Label(labelText + " :");
        label.setMinWidth(DETAILS_KEY_WIDTH);
        label.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 13px; -fx-font-weight: 700;");

        Label value = new Label(normalizeDefault(valueText, "-"));
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px;");
        HBox.setHgrow(value, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(12, label, value);
        row.setStyle("-fx-background-color: #102146; -fx-padding: 10 12; -fx-background-radius: 10; -fx-border-color: #1f3b70; -fx-border-radius: 10;");
        return row;
    }

    private String resolveSlotSummary(Integer slotId) {
        if (slotId == null) {
            return "-";
        }
        try {
            AvailabilitySlot slot = availabilitySlotService.getById(slotId);
            if (slot == null) {
                return "#" + slotId;
            }
            String start = formatDateTime(slot.getStartAt());
            String end = formatDateTime(slot.getEndAt());
            return "#" + safeNumber(slot.getId()) + "  " + start + " -> " + end;
        } catch (SQLException exception) {
            return "#" + slotId;
        }
    }

    private String formatCoordinates(Float latitude, Float longitude) {
        if (latitude == null || longitude == null) {
            return "-";
        }
        return latitude + ", " + longitude;
    }

    private void exportRendezVousDetailsPdf(RendezVous rdv) {
        if (rdv == null) {
            showWarning("Selection required", "Select a rendez-vous first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter rendez-vous en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        chooser.setInitialFileName("rendezvous-" + safeNumber(rdv.getId()) + ".pdf");
        File target = chooser.showSaveDialog(statusLabel == null || statusLabel.getScene() == null ? null : statusLabel.getScene().getWindow());
        if (target == null) {
            return;
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            List<String> lines = new ArrayList<>();
            lines.add("Details du rendez-vous #" + safeNumber(rdv.getId()));
            lines.add("");
            lines.addAll(buildRendezVousDetailLines(rdv));

            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                float marginX = 48f;
                float startY = page.getMediaBox().getHeight() - 56f;
                float lineHeight = 16f;
                float y = startY;

                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 11);
                stream.newLineAtOffset(marginX, y);

                for (String line : lines) {
                    List<String> wrapped = new ArrayList<>();
                    appendWrappedPdfLines(wrapped, line, 98);
                    for (String wrappedLine : wrapped) {
                        if (y < 72f) {
                            stream.showText("...");
                            stream.endText();
                            document.save(target);
                            if (statusLabel != null) {
                                statusLabel.setText("PDF exporté: " + target.getName());
                            }
                            showInfo("Export PDF", "Le détail a été exporté dans:\n" + target.getAbsolutePath());
                            return;
                        }
                        stream.showText(sanitizePdfText(wrappedLine));
                        stream.newLineAtOffset(0, -lineHeight);
                        y -= lineHeight;
                    }
                }
                stream.endText();
            }

            document.save(target);
            if (statusLabel != null) {
                statusLabel.setText("PDF exporté: " + target.getName());
            }
            showInfo("Export PDF", "Le détail a été exporté dans:\n" + target.getAbsolutePath());
        } catch (IOException exception) {
            showError("Unable to export rendez-vous PDF", exception);
        }
    }

    private List<String> buildRendezVousDetailLines(RendezVous rdv) {
        List<String> lines = new ArrayList<>();
        lines.add("Statut: " + statusDisplay(rdv.getStatut()));
        lines.add("Professeur: #" + safeNumber(rdv.getProfessorId()));
        lines.add("Etudiant: #" + safeNumber(rdv.getStudentId()));
        lines.add("Cours: " + (rdv.getCourseId() == null ? "-" : "#" + rdv.getCourseId()));
        lines.add("Créneau: " + resolveSlotSummary(rdv.getSlotId()));
        lines.add("Type: " + meetingTypeDisplay(rdv.getMeetingType()));
        lines.add("Lien de réunion: " + normalizeDefault(rdv.getMeetingLink(), "-"));
        lines.add("Lieu: " + normalizeDefault(rdv.getLocationLabel(), normalizeDefault(rdv.getLocation(), "-")));
        lines.add("Coordonnées: " + formatCoordinates(rdv.getLocationLat(), rdv.getLocationLng()));
        lines.add("Message: " + normalizeDefault(rdv.getMessage(), "-"));
        lines.add("Raison de refus: " + normalizeDefault(rdv.getRefusalReason(), "-"));
        lines.add("PDF cours: " + normalizeDefault(rdv.getCoursePdfName(), "-"));
        lines.add("Créé le: " + formatDateTime(rdv.getCreatedAt()));
        lines.add("Exporté le: " + formatDateTime(LocalDateTime.now()));
        return lines;
    }

    private void appendWrappedPdfLines(List<String> target, String source, int maxChars) {
        String text = normalizeDefault(source, "").replace('\n', ' ').replace('\r', ' ').trim();
        if (text.isEmpty()) {
            target.add("");
            return;
        }

        String remaining = text;
        while (remaining.length() > maxChars) {
            int breakAt = remaining.lastIndexOf(' ', maxChars);
            if (breakAt <= 0) {
                breakAt = maxChars;
            }
            target.add(remaining.substring(0, breakAt).trim());
            remaining = remaining.substring(breakAt).trim();
        }
        if (!remaining.isEmpty()) {
            target.add(remaining);
        }
    }

    private String sanitizePdfText(String text) {
        String value = normalizeDefault(text, "-");
        value = value.replace("’", "'")
                .replace("–", "-")
                .replace("—", "-");
        return value.replaceAll("[^\\x20-\\x7EÀ-ÿ]", " ");
    }

    private Optional<RendezVous> showCreateRendezVousDialog() {
        List<SlotOption> slotOptions;
        try {
            slotOptions = loadSlotOptions(null);
        } catch (SQLException exception) {
            showError("Unable to load slots for rendez-vous creation", exception);
            return Optional.empty();
        }

        if (slotOptions.isEmpty()) {
            showWarning("No slot available", "No available slot was found to create a rendez-vous.");
            return Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau rendez-vous");
        ButtonType createType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, createType);

        ComboBox<SlotOption> slotCombo = new ComboBox<>();
        slotCombo.getItems().setAll(slotOptions);
        slotCombo.setValue(slotOptions.get(0));
        styleCombo(slotCombo);

        ComboBox<CourseOption> courseCombo = new ComboBox<>();
        styleCombo(courseCombo);
        refreshCourseOptionsForSlot(slotCombo.getValue(), courseCombo, null);
        slotCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                refreshCourseOptionsForSlot(newValue, courseCombo, null));

        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton onlineRadio = new RadioButton("En ligne");
        RadioButton inPersonRadio = new RadioButton("En personne");
        onlineRadio.setToggleGroup(typeGroup);
        inPersonRadio.setToggleGroup(typeGroup);
        onlineRadio.setSelected(true);
        onlineRadio.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
        inPersonRadio.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Votre message (optionnel)...");
        messageArea.setPrefRowCount(4);
        styleTextArea(messageArea);

        Label pdfNameLabel = new Label("Aucun fichier choisi");
        pdfNameLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        Button fileBtn = new Button("Choisir un fichier");
        fileBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #e2e8f0; -fx-font-weight: 700; -fx-background-radius: 10;");
        File[] selectedFile = new File[1];
        fileBtn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Cours PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
            File picked = chooser.showOpenDialog(fileBtn.getScene().getWindow());
            if (picked != null) {
                selectedFile[0] = picked;
                pdfNameLabel.setText(picked.getName());
            }
        });

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: #101b36; -fx-padding: 18; -fx-border-color: #2b3f67; -fx-border-radius: 14; -fx-background-radius: 14;");

        Label slotLabel = sectionLabel("Créneau");
        Label courseLabel = sectionLabel("Cours existant");
        Label typeLabel = sectionLabel("Type de rendez-vous");
        Label infoTitle = sectionLabel("Information");
        Label infoText = new Label("Le professeur ajoutera le lien après confirmation.");
        infoText.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        Label messageLabel = sectionLabel("Message (optionnel)");
        Label pdfLabel = sectionLabel("Cours PDF (optionnel)");

        HBox typeBox = new HBox(16, onlineRadio, inPersonRadio);
        HBox pdfBox = new HBox(10, fileBtn, pdfNameLabel);

        content.getChildren().addAll(
                slotLabel, slotCombo,
                courseLabel, courseCombo,
                typeLabel, typeBox,
                infoTitle, infoText,
                messageLabel, messageArea,
                pdfLabel, pdfBox
        );

        styleDialog(dialog.getDialogPane());
        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog.getDialogPane(), createType, cancelType);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != createType) {
            return Optional.empty();
        }

        try {
            SlotOption selectedSlot = slotCombo.getValue();
            CourseOption selectedCourse = courseCombo.getValue();
            if (selectedSlot == null) {
                throw new IllegalArgumentException("Please choose a slot.");
            }
            if (selectedSlot.professorId() == null) {
                throw new IllegalArgumentException("Selected slot has no professor.");
            }
            if (selectedCourse == null || selectedCourse.id() == null) {
                throw new IllegalArgumentException("Please choose an existing course.");
            }

            RendezVous rdv = new RendezVous();
            rdv.setSlotId(selectedSlot.id());
            rdv.setProfessorId(selectedSlot.professorId());
            rdv.setStudentId(CURRENT_USER_ID);
            rdv.setCourseId(selectedCourse.id());
            rdv.setMeetingType(onlineRadio.isSelected() ? "en_ligne" : "en_personne");
            rdv.setStatut("en_attente");
            rdv.setMeetingLink(null);
            rdv.setLocation(null);
            rdv.setMessage(toNull(messageArea.getText()));
            rdv.setCreatedAt(LocalDateTime.now());
            rdv.setCoursePdfName(selectedFile[0] != null ? selectedFile[0].getName() : null);
            return Optional.of(rdv);
        } catch (IllegalArgumentException ex) {
            showWarning("Validation error", ex.getMessage());
            return Optional.empty();
        }
    }

    private Optional<RendezVous> showEditRendezVousDialog(RendezVous source) {
        List<SlotOption> slotOptions;
        try {
            slotOptions = loadSlotOptions(source.getSlotId());
        } catch (SQLException exception) {
            showError("Unable to load slots for edit", exception);
            return Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le créneau #" + safeNumber(source.getId()));
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, saveType);

        Label currentSlotLabel = new Label("Créneau actuel: " + resolveSlotLabel(source.getSlotId(), slotOptions));
        currentSlotLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-font-weight: 700;");

        ComboBox<SlotOption> slotCombo = new ComboBox<>();
        slotCombo.getItems().setAll(slotOptions);
        slotCombo.setValue(findSlotOptionById(slotOptions, source.getSlotId()));
        styleCombo(slotCombo);

        ComboBox<CourseOption> courseCombo = new ComboBox<>();
        styleCombo(courseCombo);
        refreshCourseOptionsForSlot(slotCombo.getValue(), courseCombo, source.getCourseId());
        slotCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                refreshCourseOptionsForSlot(newValue, courseCombo, null));

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: #101b36; -fx-padding: 18; -fx-border-color: #2b3f67; -fx-border-radius: 14; -fx-background-radius: 14;");
        content.getChildren().addAll(
                currentSlotLabel,
                sectionLabel("Nouveau créneau"),
                slotCombo,
                sectionLabel("Cours"),
                courseCombo
        );

        styleDialog(dialog.getDialogPane());
        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog.getDialogPane(), saveType, cancelType);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) {
            return Optional.empty();
        }

        SlotOption chosen = slotCombo.getValue();
        CourseOption chosenCourse = courseCombo.getValue();
        if (chosen == null) {
            showWarning("Validation error", "Please choose a slot.");
            return Optional.empty();
        }
        if (chosenCourse == null || chosenCourse.id() == null) {
            showWarning("Validation error", "Please choose an existing course.");
            return Optional.empty();
        }

        RendezVous updated = copyRendezVous(source);
        updated.setSlotId(chosen.id());
        if (chosen.professorId() != null) {
            updated.setProfessorId(chosen.professorId());
        }
        updated.setCourseId(chosenCourse.id());
        return Optional.of(updated);
    }

    private List<SlotOption> loadSlotOptions(Integer includeSlotId) throws SQLException {
        return availabilitySlotService.getAll().stream()
                .filter(slot -> isSlotAvailable(slot, includeSlotId))
                .map(slot -> {
                    String professorName = resolveProfessorName(slot.getProfessorId());
                    return new SlotOption(
                            slot.getId(),
                            slot.getProfessorId(),
                            professorName,
                            formatSlotRange(slot, professorName)
                    );
                })
                .collect(Collectors.toList());
    }

    private boolean isSlotAvailable(AvailabilitySlot slot, Integer includeSlotId) {
        if (includeSlotId != null && includeSlotId.equals(slot.getId())) {
            return true;
        }
        return !Boolean.TRUE.equals(slot.getIsBooked());
    }

    private String formatSlotRange(AvailabilitySlot slot, String professorName) {
        String start = slot.getStartAt() == null ? "-" : slot.getStartAt().format(DATE_TIME_FORMATTER);
        String end = slot.getEndAt() == null ? "-" : slot.getEndAt().format(DATE_TIME_FORMATTER);
        String currentFlag = selectedRendezVous != null && selectedRendezVous.getSlotId() != null
                && selectedRendezVous.getSlotId().equals(slot.getId()) ? " (actuel)" : "";
        String professor = normalizeDefault(professorName, "Prof #" + safeNumber(slot.getProfessorId()));
        return "#" + safeNumber(slot.getId()) + "  " + start + " -> " + end + "  |  " + professor + currentFlag;
    }

    private String resolveProfessorName(Integer professorId) {
        if (professorId == null) {
            return "Professeur inconnu";
        }

        try {
            String userTable = findExistingTable("user", "users");
            if (userTable == null) {
                return "Prof #" + professorId;
            }

            Connection connection = MyConnection.getInstance().getConnection();
            String sql = "SELECT first_name, last_name, username FROM `" + userTable + "` WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setInt(1, professorId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String fullName = (normalizeDefault(resultSet.getString("first_name"), "") + " "
                                + normalizeDefault(resultSet.getString("last_name"), "")).trim();
                        if (!fullName.isBlank()) {
                            return fullName;
                        }
                        String username = normalizeDefault(resultSet.getString("username"), "");
                        if (!username.isBlank()) {
                            return username;
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // Fallback to ID-based label when user lookup is unavailable.
        }

        return "Prof #" + professorId;
    }

    private void refreshCourseOptionsForSlot(SlotOption selectedSlot, ComboBox<CourseOption> courseCombo, Integer preferredCourseId) {
        courseCombo.getItems().clear();
        courseCombo.setDisable(true);
        if (selectedSlot == null || selectedSlot.professorId() == null) {
            courseCombo.setPromptText("Choisir un créneau");
            return;
        }

        try {
            List<CourseOption> options = loadCourseOptionsForProfessor(selectedSlot.professorId());
            if (options.isEmpty()) {
                courseCombo.setPromptText("Aucun cours disponible");
                return;
            }
            courseCombo.getItems().setAll(options);
            courseCombo.setDisable(false);
            courseCombo.setValue(findCourseOptionById(options, preferredCourseId));
        } catch (SQLException exception) {
            showError("Unable to load courses", exception);
        }
    }

    private List<CourseOption> loadCourseOptionsForProfessor(Integer professorId) throws SQLException {
        String courseTable = findExistingTable("course", "courses");
        if (courseTable == null) {
            return List.of();
        }

        String titleColumn = findExistingColumn(courseTable, "title", "name", "label");
        String professorColumn = findExistingColumn(courseTable, "professor_id", "teacher_id", "instructor_id", "author_id", "user_id", "created_by");

        List<CourseOption> options = queryCourseOptions(courseTable, titleColumn, professorColumn, professorId);
        if (options.isEmpty() && professorColumn != null && professorId != null) {
            options = queryCourseOptions(courseTable, titleColumn, null, null);
        }
        return options;
    }

    private List<CourseOption> queryCourseOptions(String courseTable, String titleColumn, String professorColumn, Integer professorId) throws SQLException {
        Connection connection = MyConnection.getInstance().getConnection();
        List<CourseOption> options = new ArrayList<>();

        String titleSelect = titleColumn == null ? "NULL AS course_title" : "`" + titleColumn + "` AS course_title";
        StringBuilder sql = new StringBuilder("SELECT `id`, ").append(titleSelect).append(" FROM `").append(courseTable).append("`");
        if (professorColumn != null && professorId != null) {
            sql.append(" WHERE `").append(professorColumn).append("` = ?");
        }
        if (titleColumn != null) {
            sql.append(" ORDER BY `").append(titleColumn).append("` ASC");
        } else {
            sql.append(" ORDER BY `id` ASC");
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql.toString())) {
            if (professorColumn != null && professorId != null) {
                preparedStatement.setInt(1, professorId);
            }
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    int courseId = resultSet.getInt("id");
                    String title = normalizeDefault(resultSet.getString("course_title"), "Course #" + courseId);
                    options.add(new CourseOption(courseId, title));
                }
            }
        }

        return options;
    }

    private String findExistingTable(String... candidates) throws SQLException {
        Connection connection = MyConnection.getInstance().getConnection();
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (tableExists(connection, catalog, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tableExists(Connection connection, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            return resultSet.next();
        }
    }

    private String findExistingColumn(String tableName, String... candidates) throws SQLException {
        Connection connection = MyConnection.getInstance().getConnection();
        String catalog = connection.getCatalog();
        for (String candidate : candidates) {
            if (columnExists(connection, catalog, tableName, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean columnExists(Connection connection, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName, columnName)) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return resultSet.next();
        }
    }

    private SlotOption findSlotOptionById(List<SlotOption> options, Integer slotId) {
        if (slotId == null) {
            return options.isEmpty() ? null : options.get(0);
        }
        return options.stream()
                .filter(option -> slotId.equals(option.id()))
                .findFirst()
                .orElse(options.isEmpty() ? null : options.get(0));
    }

    private CourseOption findCourseOptionById(List<CourseOption> options, Integer courseId) {
        if (options.isEmpty()) {
            return null;
        }
        if (courseId == null) {
            return options.get(0);
        }
        return options.stream()
                .filter(option -> courseId.equals(option.id()))
                .findFirst()
                .orElse(options.get(0));
    }

    private String resolveSlotLabel(Integer slotId, List<SlotOption> options) {
        SlotOption option = findSlotOptionById(options, slotId);
        return option == null ? "-" : option.label();
    }

    private void styleDialog(DialogPane pane) {
        pane.setStyle("-fx-background-color: #0f1b38; -fx-border-color: #2f4675; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");
    }

    private void styleDialogButtons(DialogPane pane, ButtonType primary, ButtonType secondary) {
        Button primaryBtn = (Button) pane.lookupButton(primary);
        Button secondaryBtn = (Button) pane.lookupButton(secondary);
        if (primaryBtn != null) {
            primaryBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 12;");
        }
        if (secondaryBtn != null) {
            secondaryBtn.setStyle("-fx-background-color: #162546; -fx-text-fill: #e2e8f0; -fx-font-weight: 700; -fx-background-radius: 12; -fx-border-color: #2e4677; -fx-border-radius: 12;");
        }
    }

    private void styleCombo(ComboBox<?> comboBox) {
        comboBox.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-control-inner-background: #101f40; -fx-background-color: #101f40; -fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #94a3b8; -fx-border-color: #2b436f; -fx-border-radius: 12; -fx-background-radius: 12;");
    }

    private void styleTextArea(TextArea area) {
        area.setStyle("-fx-control-inner-background: #101f40; -fx-background-color: #101f40; -fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #94a3b8; -fx-border-color: #2b436f; -fx-border-radius: 12; -fx-background-radius: 12;");
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
        return label;
    }

    private RendezVous copyRendezVous(RendezVous source) {
        RendezVous copy = new RendezVous();
        copy.setId(source.getId());
        copy.setStudentId(source.getStudentId());
        copy.setProfessorId(source.getProfessorId());
        copy.setCourseId(source.getCourseId());
        copy.setSlotId(source.getSlotId());
        copy.setStatut(source.getStatut());
        copy.setMeetingType(source.getMeetingType());
        copy.setMeetingLink(source.getMeetingLink());
        copy.setLocation(source.getLocation());
        copy.setMessage(source.getMessage());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setLocationLabel(source.getLocationLabel());
        copy.setLocationLat(source.getLocationLat());
        copy.setLocationLng(source.getLocationLng());
        copy.setRefusalReason(source.getRefusalReason());
        copy.setCoursePdfName(source.getCoursePdfName());
        return copy;
    }

    private record SlotOption(Integer id, Integer professorId, String professorName, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private record CourseOption(Integer id, String title) {
        @Override
        public String toString() {
            String idText = id == null ? "-" : id.toString();
            String titleText = title == null ? "" : title.trim();
            return "#" + idText + "  " + (titleText.isEmpty() ? "-" : titleText);
        }
    }

    private void markSlotBookedState(Integer slotId, boolean booked) {
        if (slotId == null) {
            return;
        }
        try {
            availabilitySlotService.updateBookedStatus(slotId, booked);
        } catch (SQLException exception) {
            showWarning("Slot update", "Rendez-vous saved, but slot #" + slotId + " booking state could not be updated.");
        }
    }

    private boolean sameInteger(Integer left, Integer right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private boolean isVisibleForCurrentUser(RendezVous rdv) {
        if (rdv == null) {
            return false;
        }
        if (isAdminMode()) {
            return true;
        }
        if (isProfessorMode()) {
            return sameInteger(rdv.getProfessorId(), CURRENT_USER_ID);
        }
        return sameInteger(rdv.getStudentId(), CURRENT_USER_ID);
    }

    private boolean canStudentManage(RendezVous rdv) {
        return isStudentMode() && rdv != null && sameInteger(rdv.getStudentId(), CURRENT_USER_ID);
    }

    private boolean isAdminMode() {
        return CURRENT_USER_ROLE.contains("admin");
    }

    private boolean isProfessorMode() {
        return !isAdminMode() && (CURRENT_USER_ROLE.contains("prof") || CURRENT_USER_ROLE.contains("teacher"));
    }

    private boolean isStudentMode() {
        return !isAdminMode() && !isProfessorMode();
    }

    private void updateStatusAsProfessor(RendezVous source, String newStatus) {
        updateStatusAsProfessor(source, newStatus, null);
    }

    private void updateStatusAsProfessor(RendezVous source, String newStatus, String meetingLinkInput) {
        if (source == null) {
            showWarning("Selection required", "Select a rendez-vous first.");
            return;
        }
        if (!isProfessorMode() && !isAdminMode()) {
            showWarning("Action not allowed", "Only professor can accept or refuse a rendez-vous.");
            return;
        }
        if (!isPendingStatus(source.getStatut())) {
            showWarning("Action not allowed", "Only pending rendez-vous can be accepted or refused.");
            return;
        }

        try {
            if (!isAdminMode() && !isCurrentProfessorSlotOwner(source)) {
                showWarning("Action not allowed", "You can only update rendez-vous assigned to your own slots.");
                return;
            }

            RendezVous updated = copyRendezVous(source);
            updated.setStatut(newStatus);

            if (isConfirmedStatus(newStatus)) {
                String meetingLink = toNull(meetingLinkInput);
                if (meetingLink == null) {
                    showWarning("Validation error", "Le lien de réunion est obligatoire lors de l'acceptation.");
                    return;
                }
                updated.setMeetingLink(meetingLink);
                updated.setRefusalReason(null);
            }

            AvailabilitySlot slot = source.getSlotId() == null ? null : availabilitySlotService.getById(source.getSlotId());
            if (slot != null && slot.getProfessorId() != null) {
                updated.setProfessorId(slot.getProfessorId());
            }

            boolean ok = rendezVousService.update(updated);
            if (!ok) {
                showWarning("Update failed", "The selected rendez-vous could not be updated.");
                return;
            }
            if (isRefusedStatus(newStatus)) {
                markSlotBookedState(updated.getSlotId(), false);
            } else if (isConfirmedStatus(newStatus)) {
                markSlotBookedState(updated.getSlotId(), true);
            }
            createStudentStatusNotification(updated);
            refreshRendezVous();
            statusLabel.setText("Rendez-vous #" + safeNumber(updated.getId()) + " is now " + statusDisplay(newStatus));
        } catch (SQLException exception) {
            showError("Unable to update rendez-vous status", exception);
        }
    }

    private boolean isCurrentProfessorSlotOwner(RendezVous source) throws SQLException {
        if (source == null || source.getSlotId() == null) {
            return false;
        }
        AvailabilitySlot slot = availabilitySlotService.getById(source.getSlotId());
        if (slot == null || slot.getProfessorId() == null) {
            return false;
        }
        return sameInteger(slot.getProfessorId(), CURRENT_USER_ID);
    }

    private String toNull(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeDefault(String raw, String fallback) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() ? fallback : value;
    }

    private boolean isPendingStatus(String rawStatus) {
        return normalizeDefault(rawStatus, "").toLowerCase().contains("attente");
    }

    private boolean isConfirmedStatus(String rawStatus) {
        return normalizeDefault(rawStatus, "").toLowerCase().contains("confirm");
    }

    private boolean isRefusedStatus(String rawStatus) {
        String value = normalizeDefault(rawStatus, "").toLowerCase();
        return value.contains("refus") || value.contains("rejet");
    }

    private String safeNumber(Integer value) {
        return value == null ? "-" : value.toString();
    }

    private String statusDisplay(String rawStatus) {
        String s = normalizeDefault(rawStatus, "unknown").toLowerCase();
        if (s.contains("attente")) {
            return "En attente";
        }
        if (s.contains("confirm")) {
            return "Confirmé";
        }
        if (s.contains("refus") || s.contains("rejet")) {
            return "Refusé";
        }
        return normalizeDefault(rawStatus, "unknown");
    }

    private String meetingTypeDisplay(String rawType) {
        String type = normalizeDefault(rawType, "").toLowerCase();
        if (type.contains("ligne")) {
            return "En ligne";
        }
        if (type.contains("personne")) {
            return "En personne";
        }
        return normalizeDefault(rawType, "-");
    }

    private String formatCardDate(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.format(CARD_DATE_FORMATTER);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME_FORMATTER);
    }

    @FXML
    private void showUnreadNotifications() {
        try {
            List<Notification> unread = notificationService.findUnreadByUser(CURRENT_USER_ID, 20);
            boolean openAll = showNotificationsDialog(unread);
            if (openAll) {
                showAllNotificationsDialog();
            }
            updateNotificationBadge();
        } catch (SQLException exception) {
            showError("Unable to load notifications", exception);
        }
    }

    private void updateNotificationBadge() {
        if (notificationButton == null) {
            return;
        }
        try {
            int unread = notificationService.countUnreadByUser(CURRENT_USER_ID);
            notificationButton.setText("🔔 " + unread);
        } catch (SQLException exception) {
            notificationButton.setText("🔔 !");
        }
    }

    private void createProfessorNotification(RendezVous rdv) {
        if (rdv.getProfessorId() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(rdv.getProfessorId());
        notification.setTitle("Nouvelle demande de rendez-vous");
        notification.setMessage("Nouvelle demande de l'étudiant #" + rdv.getStudentId() + " pour le rendez-vous #" + rdv.getId());
        notification.setLink("/rendezvous/" + rdv.getId());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notificationService.add(notification);
        } catch (SQLException exception) {
            showWarning("Notification", "Rendez-vous created, but notification was not saved: " + exception.getMessage());
        }
    }

    private void createStudentStatusNotification(RendezVous rdv) {
        if (rdv == null || rdv.getStudentId() == null) {
            return;
        }
        String message = "Votre rendez-vous #" + rdv.getId() + " a été " + statusDisplay(rdv.getStatut()).toLowerCase() + ".";
        if (isConfirmedStatus(rdv.getStatut()) && toNull(rdv.getMeetingLink()) != null) {
            message += " Lien de réunion: " + rdv.getMeetingLink();
        }
        Notification notification = new Notification();
        notification.setUserId(rdv.getStudentId());
        notification.setTitle("Mise à jour rendez-vous");
        notification.setMessage(message);
        notification.setLink("/rendezvous/" + rdv.getId());
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        try {
            notificationService.add(notification);
        } catch (SQLException exception) {
            showWarning("Notification", "Rendez-vous updated, but notification was not saved: " + exception.getMessage());
        }
    }

    private boolean showNotificationsDialog(List<Notification> unread) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);
        AtomicBoolean openAll = new AtomicBoolean(false);

        Button closeButton = (Button) dialog.getDialogPane().lookupButton(closeType);
        if (closeButton != null) {
            closeButton.setVisible(false);
            closeButton.setManaged(false);
        }

        VBox root = new VBox();
        root.setStyle("-fx-background-color: #08142d; -fx-border-color: #1f325f; -fx-border-width: 1; -fx-border-radius: 18; -fx-background-radius: 18;");
        root.setPrefWidth(360);
        root.setMaxWidth(360);

        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 16px; -fx-font-weight: 700;");
        VBox titleWrap = new VBox(title);
        titleWrap.setStyle("-fx-padding: 20 24 16 24;");

        Region topDivider = new Region();
        topDivider.setStyle("-fx-border-color: #162a4f; -fx-border-width: 1 0 0 0; -fx-min-height: 1;");

        VBox center = new VBox(10);
        center.setStyle("-fx-padding: 22 24 22 24; -fx-alignment: center;");
        if (unread == null || unread.isEmpty()) {
            Label empty = new Label("Aucune notification non lue.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 15px; -fx-font-weight: 500;");
            center.getChildren().add(empty);
        } else {
            int limit = Math.min(unread.size(), 5);
            for (int i = 0; i < limit; i++) {
                Notification notification = unread.get(i);
                Label line = new Label("• " + normalizeDefault(notification.getTitle(), "Notification")
                        + " : " + normalizeDefault(notification.getMessage(), ""));
                line.setWrapText(true);
                line.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
                center.getChildren().add(line);
            }
        }

        Region bottomDivider = new Region();
        bottomDivider.setStyle("-fx-border-color: #162a4f; -fx-border-width: 1 0 0 0; -fx-min-height: 1;");

        Button footerAction = new Button("Voir toutes les notifications");
        footerAction.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b82f6; -fx-font-size: 16px; -fx-font-weight: 700;");
        footerAction.setOnAction(event -> {
            openAll.set(true);
            dialog.setResult(closeType);
        });
        VBox footerWrap = new VBox(footerAction);
        footerWrap.setStyle("-fx-padding: 14 24 16 24; -fx-alignment: center;");

        root.getChildren().addAll(titleWrap, topDivider, center, bottomDivider, footerWrap);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setContent(root);

        Node buttonBar = pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
        }

        dialog.showAndWait();
        return openAll.get();
    }

    private void showAllNotificationsDialog() {
        List<Notification> notifications;
        try {
            notifications = notificationService.findByUser(CURRENT_USER_ID, 200);
        } catch (SQLException exception) {
            showError("Unable to load notifications", exception);
            return;
        }

        long unreadCount = notifications.stream()
                .filter(notification -> !Boolean.TRUE.equals(notification.getIsRead()))
                .count();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notifications");
        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        Button closeButton = (Button) dialog.getDialogPane().lookupButton(closeType);
        if (closeButton != null) {
            closeButton.setVisible(false);
            closeButton.setManaged(false);
        }

        AtomicBoolean shouldReload = new AtomicBoolean(false);
        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: #020f2d; -fx-border-color: #10295b; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 18;");
        root.setPrefWidth(980);
        root.setMaxWidth(980);
        root.setPrefHeight(640);

        HBox header = new HBox(12);
        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 42px; -fx-font-weight: 800;");
        Button markAllBtn = new Button("Tout marquer comme lu");
        markAllBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 8 16;");
        markAllBtn.setDisable(unreadCount == 0);
        markAllBtn.setOnAction(event -> {
            try {
                notificationService.markAllAsReadForUser(CURRENT_USER_ID);
                shouldReload.set(true);
                dialog.setResult(closeType);
            } catch (SQLException exception) {
                showError("Unable to mark notifications as read", exception);
            }
        });
        header.getChildren().addAll(title, markAllBtn);

        Label unreadLabel = new Label(unreadCount + " non lu(s)");
        unreadLabel.setStyle("-fx-text-fill: #60a5fa; -fx-font-size: 14px; -fx-font-weight: 600;");

        VBox rows = new VBox(0);
        rows.setStyle("-fx-background-color: #081b45; -fx-border-color: #173467; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");

        if (notifications.isEmpty()) {
            Label empty = new Label("Aucune notification.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-padding: 20;");
            rows.getChildren().add(empty);
        } else {
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = notifications.get(i);
                rows.getChildren().add(buildNotificationRow(notification, i == notifications.size() - 1, dialog, shouldReload, closeType));
            }
        }

        ScrollPane listScroll = new ScrollPane(rows);
        listScroll.setFitToWidth(true);
        listScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        root.getChildren().addAll(header, unreadLabel, listScroll);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setContent(root);

        Node buttonBar = pane.lookup(".button-bar");
        if (buttonBar != null) {
            buttonBar.setVisible(false);
            buttonBar.setManaged(false);
        }

        dialog.showAndWait();
        updateNotificationBadge();
        if (shouldReload.get()) {
            showAllNotificationsDialog();
        }
    }

    private HBox buildNotificationRow(Notification notification, boolean lastRow, Dialog<ButtonType> dialog, AtomicBoolean shouldReload, ButtonType closeType) {
        HBox row = new HBox(14);
        String border = lastRow ? "0 0 0 0" : "0 0 1 0";
        String background = Boolean.TRUE.equals(notification.getIsRead()) ? "#061635" : "#0a1f4f";
        row.setStyle("-fx-alignment: center-left; -fx-padding: 14 16; -fx-border-color: #12305f; -fx-border-width: " + border + "; -fx-background-color: " + background + ";");

        Label icon = new Label("🔔");
        icon.setStyle("-fx-background-color: #10336e; -fx-text-fill: #60a5fa; -fx-font-size: 14px; -fx-padding: 8; -fx-background-radius: 18;");

        VBox textBox = new VBox(4);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Label title = new Label(normalizeDefault(notification.getTitle(), "Notification"));
        title.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 20px; -fx-font-weight: 800;");
        Label message = new Label(normalizeDefault(notification.getMessage(), "-"));
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");
        Label time = new Label(formatNotificationDate(notification.getCreatedAt()));
        time.setStyle("-fx-text-fill: #93c5fd; -fx-font-size: 12px;");
        textBox.getChildren().addAll(title, message, time);

        HBox actions = new HBox(8);
        Button openBtn = new Button("Ouvrir");
        openBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b82f6; -fx-font-size: 13px; -fx-font-weight: 700;");
        openBtn.setOnAction(event -> showInfo(
                normalizeDefault(notification.getTitle(), "Notification"),
                normalizeDefault(notification.getMessage(), "") + "\n" + normalizeDefault(notification.getLink(), "")
        ));
        actions.getChildren().add(openBtn);

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            Button markReadBtn = new Button("Marquer lu");
            markReadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: 700;");
            markReadBtn.setOnAction(event -> markNotificationAsRead(notification, dialog, shouldReload, closeType));
            actions.getChildren().add(markReadBtn);
        }

        row.getChildren().addAll(icon, textBox, actions);
        return row;
    }

    private void markNotificationAsRead(Notification notification, Dialog<ButtonType> dialog, AtomicBoolean shouldReload, ButtonType closeType) {
        if (notification == null || notification.getId() == null) {
            return;
        }
        try {
            notificationService.markAsRead(notification.getId());
            shouldReload.set(true);
            dialog.setResult(closeType);
        } catch (SQLException exception) {
            showError("Unable to update notification", exception);
        }
    }

    private String formatNotificationDate(LocalDateTime value) {
        return value == null ? "-" : value.format(NOTIFICATION_DATE_FORMATTER);
    }

    private static int parseCurrentUserId() {
        String raw = System.getProperty("skillora.userId", "20");
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 20;
        }
    }

    private static String normalizeRole(String raw) {
        if (raw == null) {
            return "student";
        }
        String normalized = raw.trim().toLowerCase();
        return normalized.isEmpty() ? "student" : normalized;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
    }
}
