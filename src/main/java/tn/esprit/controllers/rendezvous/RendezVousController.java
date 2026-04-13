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
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.entities.Notification;
import tn.esprit.entities.RendezVous;
import tn.esprit.services.AvailabilitySlotService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.RendezVousService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RendezVousController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter CARD_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM yyyy")
            .toFormatter();
    private static final String BTN_ACTIVE = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";
    private static final String BTN_INACTIVE = "-fx-background-color: #0f1b38; -fx-text-fill: #cbd5e1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-background-radius: 12; -fx-padding: 12;";
    private static final int CURRENT_STUDENT_ID = 20;

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
            allRendezVous = rendezVousService.getAll();
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
        Label dateLabel = new Label(formatCardDate(rdv.getCreatedAt()) + "  —  " + normalizeDefault(rdv.getMeetingType(), "-"));
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
        String linkText = normalizeDefault(rdv.getMeetingLink(), normalizeDefault(rdv.getLocation(), "-"));
        Label linkLabel = new Label(linkText);
        linkLabel.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
        linkRow.getChildren().add(linkLabel);

        HBox actions = new HBox(10);
        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle("-fx-background-color: #0b1a3a; -fx-border-color: #dbeafe; -fx-border-radius: 18; -fx-background-radius: 18; -fx-text-fill: #e2e8f0; -fx-font-weight: 700;");
        detailsBtn.setOnAction(event -> showInfo("Rendez-vous details", "Rendez-vous #" + safeNumber(rdv.getId()) + "\nStatus: " + statusDisplay(rdv.getStatut())));
        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 18; -fx-text-fill: white; -fx-font-weight: 700;");
        editBtn.setOnAction(event -> {
            selectedRendezVous = rdv;
            editSelectedRendezVous();
        });
        Button pdfBtn = new Button("Télécharger PDF");
        pdfBtn.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 18; -fx-text-fill: #e2e8f0; -fx-font-weight: 700;");
        pdfBtn.setOnAction(event -> {
            String pdf = normalizeDefault(rdv.getCoursePdfName(), "");
            if (pdf.isBlank()) {
                showWarning("PDF", "No PDF attached to this rendez-vous.");
            } else {
                showInfo("PDF", "PDF file: " + pdf);
            }
        });
        actions.getChildren().addAll(detailsBtn, editBtn, pdfBtn);

        card.getChildren().addAll(header, dateRow, line1, line2, line3, linkRow, actions);
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
        Optional<RendezVous> input = showCreateRendezVousDialog();
        if (input.isEmpty()) {
            return;
        }

        try {
            rendezVousService.add(input.get());
            createProfessorNotification(input.get());
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

        Optional<RendezVous> input = showEditRendezVousDialog(selectedRendezVous);
        if (input.isEmpty()) {
            return;
        }

        RendezVous updated = input.get();
        updated.setId(selectedRendezVous.getId());
        try {
            boolean ok = rendezVousService.update(updated);
            if (ok) {
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
            boolean deleted = rendezVousService.delete(selectedRendezVous.getId());
            if (deleted) {
                Integer deletedId = selectedRendezVous.getId();
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

        TextField courseIdField = buildInputField("Aucun cours spécifique");

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
        Label courseLabel = sectionLabel("Cours (optionnel)");
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
                courseLabel, courseIdField,
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
            if (selectedSlot == null) {
                throw new IllegalArgumentException("Please choose a slot.");
            }
            if (selectedSlot.professorId() == null) {
                throw new IllegalArgumentException("Selected slot has no professor.");
            }

            RendezVous rdv = new RendezVous();
            rdv.setSlotId(selectedSlot.id());
            rdv.setProfessorId(selectedSlot.professorId());
            rdv.setStudentId(CURRENT_STUDENT_ID);
            rdv.setCourseId(parseIntOptional(courseIdField.getText()));
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

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: #101b36; -fx-padding: 18; -fx-border-color: #2b3f67; -fx-border-radius: 14; -fx-background-radius: 14;");
        content.getChildren().addAll(
                currentSlotLabel,
                sectionLabel("Nouveau créneau"),
                slotCombo
        );

        styleDialog(dialog.getDialogPane());
        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog.getDialogPane(), saveType, cancelType);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) {
            return Optional.empty();
        }

        SlotOption chosen = slotCombo.getValue();
        if (chosen == null) {
            showWarning("Validation error", "Please choose a slot.");
            return Optional.empty();
        }

        RendezVous updated = copyRendezVous(source);
        updated.setSlotId(chosen.id());
        if (chosen.professorId() != null) {
            updated.setProfessorId(chosen.professorId());
        }
        return Optional.of(updated);
    }

    private List<SlotOption> loadSlotOptions(Integer includeSlotId) throws SQLException {
        return availabilitySlotService.getAll().stream()
                .filter(slot -> !Boolean.TRUE.equals(slot.getIsBooked()) || (includeSlotId != null && includeSlotId.equals(slot.getId())))
                .map(slot -> new SlotOption(
                        slot.getId(),
                        slot.getProfessorId(),
                        formatSlotRange(slot)
                ))
                .collect(Collectors.toList());
    }

    private String formatSlotRange(AvailabilitySlot slot) {
        String start = slot.getStartAt() == null ? "-" : slot.getStartAt().format(DATE_TIME_FORMATTER);
        String end = slot.getEndAt() == null ? "-" : slot.getEndAt().format(DATE_TIME_FORMATTER);
        String currentFlag = selectedRendezVous != null && selectedRendezVous.getSlotId() != null
                && selectedRendezVous.getSlotId().equals(slot.getId()) ? " (actuel)" : "";
        return "#" + safeNumber(slot.getId()) + "  " + start + " -> " + end + currentFlag;
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

    private TextField buildInputField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #e2e8f0; -fx-prompt-text-fill: #94a3b8;");
        return field;
    }

    private void styleCombo(ComboBox<?> comboBox) {
        comboBox.setStyle("-fx-background-color: #101f40; -fx-border-color: #2754a3; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #e2e8f0; -fx-font-size: 14px; -fx-font-weight: 700;");
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

    private record SlotOption(Integer id, Integer professorId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private Integer parseIntOptional(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Course ID must be a valid integer.");
        }
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
            List<Notification> unread = notificationService.findUnreadByUser(CURRENT_STUDENT_ID, 20);
            if (unread.isEmpty()) {
                showInfo("Notifications", "No unread notifications.");
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (Notification notification : unread) {
                builder.append("- ").append(normalizeDefault(notification.getTitle(), "Notification"))
                        .append(": ").append(normalizeDefault(notification.getMessage(), ""))
                        .append("\n");
            }
            showInfo("Unread notifications", builder.toString().trim());
            notificationService.markAllAsReadForUser(CURRENT_STUDENT_ID);
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
            int unread = notificationService.countUnreadByUser(CURRENT_STUDENT_ID);
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
