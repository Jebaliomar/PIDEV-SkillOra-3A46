package tn.esprit.controllers.rendezvous;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Axis;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import tn.esprit.controllers.AdminPanelController;
import tn.esprit.controllers.StudentLayoutController;
import tn.esprit.entities.AvailabilitySlot;
import tn.esprit.entities.Notification;
import tn.esprit.entities.RendezVous;
import tn.esprit.entities.User;
import tn.esprit.services.AIService;
import tn.esprit.services.AvailabilitySlotService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.RendezVousEmailService;
import tn.esprit.services.RendezVousService;
import tn.esprit.services.UserService;
import tn.esprit.tools.MyConnection;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class RendezVousController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter NOTIFICATION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter CARD_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd MMM yyyy")
            .toFormatter();
    private static final String BTN_ACTIVE = "-fx-background-color: linear-gradient(to bottom, #2f5fc8, #264fb2); -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 9 14;";
    private static final String BTN_INACTIVE = "-fx-background-color: #e9effb; -fx-text-fill: #264fb2; -fx-font-size: 14px; -fx-font-weight: 800; -fx-background-radius: 12; -fx-padding: 9 14;";
    private static final double DETAILS_KEY_WIDTH = 170;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_MEETING_LINK_LENGTH = 255;
    private static final int MAX_REFUSAL_REASON_LENGTH = 500;
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int RENDEZ_VOUS_PER_PAGE = 8;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private Pane cardsContainer;

    @FXML
    private ScrollPane cardsScrollPane;

    @FXML
    private Pagination rendezVousPagination;

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

    @FXML
    private Button createTopBtn;

    @FXML
    private Button createBottomBtn;

    @FXML
    private Label totalCountLabel;

    @FXML
    private Label pendingCountLabel;

    @FXML
    private Label confirmedCountLabel;

    @FXML
    private Label refusedCountLabel;

    @FXML
    private ComboBox<TeacherPerformanceStats> teacherStatsCombo;

    @FXML
    private VBox teacherStatsButtonsContainer;

    @FXML
    private PieChart teacherStatsPieChart;

    @FXML
    private Label teacherStatsSummaryLabel;

    @FXML
    private BarChart<String, Number> professorActivityBarChart;

    @FXML
    private Label professorActivitySummaryLabel;

    @FXML
    private VBox professorRankingContainer;

    private ContextMenu notificationsDropdown;

    private final AIService aiService = new AIService();
    private final RendezVousService rendezVousService = new RendezVousService();
    private final AvailabilitySlotService availabilitySlotService = new AvailabilitySlotService();
    private final NotificationService notificationService = new NotificationService();
    private final RendezVousEmailService rendezVousEmailService = new RendezVousEmailService();
    private final Map<Integer, String> userDisplayNameCache = new HashMap<>();
    private final Map<Integer, String> userEmailCache = new HashMap<>();
    private final Map<Integer, RendezVous> notificationRendezVousCache = new HashMap<>();
    private final Map<Integer, String> courseTitleCache = new HashMap<>();
    private List<RendezVous> allRendezVous = List.of();
    private List<RendezVous> filteredRendezVous = List.of();
    private RendezVous selectedRendezVous;
    private RendezVousFilter currentFilter = RendezVousFilter.ALL;
    private Integer selectedTeacherStatsProfessorId;

    private enum RendezVousFilter {
        ALL,
        PENDING,
        CONFIRMED,
        REFUSED
    }

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender(true));
        if (cardsScrollPane != null) {
            cardsScrollPane.viewportBoundsProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndRender(false));
        }
        if (rendezVousPagination != null) {
            rendezVousPagination.currentPageIndexProperty().addListener((obs, oldValue, newValue) -> renderCurrentPage());
        }
        if (teacherStatsCombo != null) {
            teacherStatsCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateTeacherStatsPieForSelection());
        }
        applyRolePermissions();
        setActiveFilter(RendezVousFilter.ALL);
        refreshRendezVous();
        updateNotificationBadge();
        Platform.runLater(() -> applyFiltersAndRender(false));
    }

    private void applyRolePermissions() {
        boolean studentMode = hasStudentPrivileges();
        if (createTopBtn != null) {
            createTopBtn.setManaged(studentMode);
            createTopBtn.setVisible(studentMode);
        }
        if (createBottomBtn != null) {
            createBottomBtn.setManaged(studentMode);
            createBottomBtn.setVisible(studentMode);
        }
    }

    @FXML
    private void refreshRendezVous() {
        try {
            userDisplayNameCache.clear();
            userEmailCache.clear();
            notificationRendezVousCache.clear();
            courseTitleCache.clear();
            List<RendezVous> loaded = rendezVousService.getAll();
            if (isAdminMode()) {
                // Admin: full visibility over all rendez-vous.
                allRendezVous = loaded;
            } else {
                allRendezVous = loaded.stream()
                        .filter(this::isVisibleForCurrentUser)
                        .collect(Collectors.toList());
            }
            selectedRendezVous = null;
            applyFiltersAndRender(true);
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
        applyFiltersAndRender(true);
    }

    private void applyFiltersAndRender() {
        applyFiltersAndRender(true);
    }

    private void applyFiltersAndRender(boolean resetToFirstPage) {
        updateFilterLabels();
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        filteredRendezVous = allRendezVous.stream()
                .filter(rdv -> matchesFilter(rdv, currentFilter))
                .filter(rdv -> matchesSearch(rdv, q))
                .sorted(buildRendezVousDisplayComparator())
                .collect(Collectors.toList());
        renderFilteredRendezVous(resetToFirstPage);
        statusLabel.setText(filteredRendezVous.size() + " rendez-vous shown");
    }

    private Comparator<RendezVous> buildRendezVousDisplayComparator() {
        return Comparator
                .comparingInt((RendezVous rdv) -> statusDisplayOrder(rdv == null ? null : rdv.getStatut()))
                .thenComparing(
                        RendezVous::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(
                        RendezVous::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())
                );
    }

    private int statusDisplayOrder(String rawStatus) {
        if (isPendingStatus(rawStatus)) {
            return 0;
        }
        if (isConfirmedStatus(rawStatus)) {
            return 1;
        }
        if (isRefusedStatus(rawStatus)) {
            return 2;
        }
        if (isCancelledStatus(rawStatus)) {
            return 3;
        }
        return 4;
    }

    private void renderFilteredRendezVous(boolean resetToFirstPage) {
        if (rendezVousPagination == null) {
            renderCards(filteredRendezVous);
            return;
        }

        int total = filteredRendezVous.size();
        int pageCount = Math.max(1, (int) Math.ceil(total / (double) RENDEZ_VOUS_PER_PAGE));
        rendezVousPagination.setPageCount(pageCount);
        rendezVousPagination.setDisable(total <= RENDEZ_VOUS_PER_PAGE);

        int targetPage = 0;
        if (!resetToFirstPage) {
            targetPage = Math.min(rendezVousPagination.getCurrentPageIndex(), pageCount - 1);
        }

        if (rendezVousPagination.getCurrentPageIndex() != targetPage) {
            rendezVousPagination.setCurrentPageIndex(targetPage);
            return;
        }

        renderCurrentPage();
    }

    private void renderCurrentPage() {
        if (rendezVousPagination == null) {
            renderCards(filteredRendezVous);
            return;
        }

        if (filteredRendezVous.isEmpty()) {
            renderCards(List.of());
            if (cardsScrollPane != null) {
                cardsScrollPane.setVvalue(0);
            }
            return;
        }

        int currentPage = Math.max(0, rendezVousPagination.getCurrentPageIndex());
        int fromIndex = currentPage * RENDEZ_VOUS_PER_PAGE;
        if (fromIndex >= filteredRendezVous.size()) {
            rendezVousPagination.setCurrentPageIndex(0);
            return;
        }

        int toIndex = Math.min(fromIndex + RENDEZ_VOUS_PER_PAGE, filteredRendezVous.size());
        renderCards(filteredRendezVous.subList(fromIndex, toIndex));
        if (cardsScrollPane != null) {
            cardsScrollPane.setVvalue(0);
        }
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

        if (totalCountLabel != null) {
            totalCountLabel.setText(String.valueOf(all));
        }
        if (pendingCountLabel != null) {
            pendingCountLabel.setText(String.valueOf(pending));
        }
        if (confirmedCountLabel != null) {
            confirmedCountLabel.setText(String.valueOf(confirmed));
        }
        if (refusedCountLabel != null) {
            refusedCountLabel.setText(String.valueOf(refused));
        }
        updateTeacherStatsChart();
        updateProfessorActivityLineChart();
    }

    private void updateTeacherStatsChart() {
        if (teacherStatsPieChart == null || (teacherStatsCombo == null && teacherStatsButtonsContainer == null)) {
            return;
        }
        Map<Integer, TeacherPerformanceStats> statsByProfessor = buildTeacherStatsByProfessor();

        TeacherPerformanceStats previousComboSelection = teacherStatsCombo == null ? null : teacherStatsCombo.getValue();
        List<TeacherPerformanceStats> sorted = new ArrayList<>(statsByProfessor.values());
        sorted.sort((a, b) -> {
            int processedCompare = Integer.compare(b.processedCount(), a.processedCount());
            if (processedCompare != 0) {
                return processedCompare;
            }
            return normalizeDefault(a.professorName, "").compareToIgnoreCase(normalizeDefault(b.professorName, ""));
        });

        List<TeacherPerformanceStats> topProfessors = sorted.stream()
                .limit(3)
                .collect(Collectors.toList());
        if (teacherStatsCombo != null) {
            teacherStatsCombo.getItems().setAll(topProfessors);
        }

        if (statsByProfessor.isEmpty()) {
            selectedTeacherStatsProfessorId = null;
            if (teacherStatsCombo != null) {
                teacherStatsCombo.setDisable(true);
            }
            if (teacherStatsButtonsContainer != null) {
                teacherStatsButtonsContainer.getChildren().clear();
                Label empty = new Label("Aucun professeur trouvé.");
                empty.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 12px; -fx-font-weight: 700;");
                teacherStatsButtonsContainer.getChildren().add(empty);
            }
            teacherStatsPieChart.setData(FXCollections.observableArrayList(new PieChart.Data("Aucune donnée", 100)));
            teacherStatsPieChart.setTitle("Répartition");
            if (teacherStatsSummaryLabel != null) {
                teacherStatsSummaryLabel.setText("Aucune donnée professeur disponible.");
            }
            Platform.runLater(() -> {
                if (!teacherStatsPieChart.getData().isEmpty() && teacherStatsPieChart.getData().get(0).getNode() != null) {
                    teacherStatsPieChart.getData().get(0).getNode().setStyle("-fx-pie-color: #cbd5e1;");
                }
            });
            return;
        }

        TeacherPerformanceStats selected = null;
        if (selectedTeacherStatsProfessorId != null) {
            selected = findTeacherStatsByProfessorId(topProfessors, selectedTeacherStatsProfessorId);
        }
        if (selected == null && previousComboSelection != null && previousComboSelection.professorId != null) {
            selected = findTeacherStatsByProfessorId(topProfessors, previousComboSelection.professorId);
        }
        if (selected == null) {
            selected = topProfessors.get(0);
        }
        selectedTeacherStatsProfessorId = selected.professorId;

        if (teacherStatsCombo != null) {
            teacherStatsCombo.setDisable(false);
            teacherStatsCombo.setValue(selected);
        }
        renderTeacherStatsButtons(topProfessors, selected.professorId);
        updateTeacherStatsPieForSelection(selected);
    }

    private Map<Integer, TeacherPerformanceStats> buildTeacherStatsByProfessor() {
        Map<Integer, Integer> slotProfessorCache = new HashMap<>();
        Map<Integer, TeacherPerformanceStats> statsByProfessor = new HashMap<>();

        for (RendezVous rdv : allRendezVous) {
            Integer professorId = resolveProfessorIdForStats(rdv, slotProfessorCache);
            if (professorId == null) {
                continue;
            }

            TeacherPerformanceStats stats = statsByProfessor.computeIfAbsent(
                    professorId,
                    key -> new TeacherPerformanceStats(key, resolveProfessorName(key))
            );
            stats.total++;
            if (isConfirmedStatus(rdv.getStatut())) {
                stats.confirmed++;
            } else if (isRefusedStatus(rdv.getStatut())) {
                stats.refused++;
            } else if (isPendingStatus(rdv.getStatut())) {
                stats.pending++;
            }
        }
        return statsByProfessor;
    }

    private void updateTeacherStatsPieForSelection() {
        TeacherPerformanceStats stats = null;
        if (teacherStatsCombo != null) {
            stats = teacherStatsCombo.getValue();
        }
        if (stats == null && selectedTeacherStatsProfessorId != null) {
            stats = buildTeacherStatsByProfessor().get(selectedTeacherStatsProfessorId);
        }
        updateTeacherStatsPieForSelection(stats);
    }

    private void updateTeacherStatsPieForSelection(TeacherPerformanceStats stats) {
        if (teacherStatsPieChart == null || stats == null) {
            return;
        }
        selectedTeacherStatsProfessorId = stats.professorId;
        highlightTeacherStatsButtons(stats.professorId);
        int processed = stats.processedCount();
        ObservableList<PieChart.Data> chartData;
        if (processed <= 0) {
            chartData = FXCollections.observableArrayList(new PieChart.Data("Sans décision 100%", 100));
        } else {
            chartData = FXCollections.observableArrayList();
            if (stats.confirmed > 0) {
                chartData.add(new PieChart.Data("Acceptés " + formatPercent(stats.acceptedRate()), stats.acceptedRate()));
            }
            if (stats.refused > 0) {
                chartData.add(new PieChart.Data("Refusés " + formatPercent(stats.refusedRate()), stats.refusedRate()));
            }
            if (chartData.isEmpty()) {
                chartData.add(new PieChart.Data("Sans décision 100%", 100));
            }
        }

        teacherStatsPieChart.setData(chartData);
        teacherStatsPieChart.setTitle("Répartition");
        teacherStatsPieChart.setLegendVisible(false);
        teacherStatsPieChart.setLabelsVisible(true);
        teacherStatsPieChart.setStartAngle(90);

        if (teacherStatsSummaryLabel != null) {
            String name = normalizeDefault(stats.professorName, "Professeur " + safeNumber(stats.professorId));
            teacherStatsSummaryLabel.setText(
                    name
                            + "  •  Acceptés: " + formatPercent(stats.acceptedRate())
                            + "  •  Refusés: " + formatPercent(stats.refusedRate())
                            + "  •  En attente: " + stats.pending
                            + "  •  Total: " + stats.total
            );
        }

        Platform.runLater(() -> applyTeacherStatsPieColors(stats, chartData));
    }

    private TeacherPerformanceStats findTeacherStatsByProfessorId(List<TeacherPerformanceStats> statsList, Integer professorId) {
        if (statsList == null || statsList.isEmpty() || professorId == null) {
            return null;
        }
        return statsList.stream()
                .filter(item -> item != null && sameInteger(item.professorId, professorId))
                .findFirst()
                .orElse(null);
    }

    private void renderTeacherStatsButtons(List<TeacherPerformanceStats> statsList, Integer selectedProfessorId) {
        if (teacherStatsButtonsContainer == null) {
            return;
        }
        teacherStatsButtonsContainer.getChildren().clear();
        if (statsList == null || statsList.isEmpty()) {
            return;
        }

        for (TeacherPerformanceStats stats : statsList) {
            if (stats == null || stats.professorId == null) {
                continue;
            }
            String professorName = normalizeDefault(stats.professorName, "Professeur " + safeNumber(stats.professorId));
            Button button = new Button(professorName);
            button.setUserData(stats.professorId);
            button.setWrapText(false);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setPrefWidth(390);
            button.setStyle(teacherStatsButtonStyle(sameInteger(stats.professorId, selectedProfessorId)));
            button.setOnAction(event -> {
                selectedTeacherStatsProfessorId = stats.professorId;
                if (teacherStatsCombo != null) {
                    TeacherPerformanceStats current = teacherStatsCombo.getValue();
                    if (current == null || !sameInteger(current.professorId, stats.professorId)) {
                        teacherStatsCombo.setValue(stats);
                    }
                }
                highlightTeacherStatsButtons(stats.professorId);
                updateTeacherStatsPieForSelection(stats);
            });
            teacherStatsButtonsContainer.getChildren().add(button);
        }
    }

    private void highlightTeacherStatsButtons(Integer selectedProfessorId) {
        if (teacherStatsButtonsContainer == null) {
            return;
        }
        for (Node node : teacherStatsButtonsContainer.getChildren()) {
            if (!(node instanceof Button button)) {
                continue;
            }
            Integer buttonProfessorId = button.getUserData() instanceof Integer value ? value : null;
            button.setStyle(teacherStatsButtonStyle(sameInteger(buttonProfessorId, selectedProfessorId)));
        }
    }

    private String teacherStatsButtonStyle(boolean selected) {
        if (selected) {
            return "-fx-alignment: center-left; -fx-background-color: linear-gradient(to bottom, #2f5fc8, #264fb2); -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 7 14;";
        }
        return "-fx-alignment: center-left; -fx-background-color: #edf2fc; -fx-text-fill: #264fb2; -fx-font-size: 12px; -fx-font-weight: 800; -fx-border-color: #b9c9e9; -fx-border-radius: 999; -fx-background-radius: 999; -fx-padding: 7 14;";
    }

    private void updateProfessorActivityLineChart() {
        if (professorActivityBarChart == null && professorRankingContainer == null) {
            return;
        }
        Map<Integer, TeacherPerformanceStats> statsByProfessor = buildTeacherStatsByProfessor();
        List<TeacherPerformanceStats> sorted = new ArrayList<>(statsByProfessor.values());
        sorted.sort((a, b) -> {
            int confirmedCompare = Integer.compare(b.confirmed, a.confirmed);
            if (confirmedCompare != 0) {
                return confirmedCompare;
            }
            int totalCompare = Integer.compare(b.total, a.total);
            if (totalCompare != 0) {
                return totalCompare;
            }
            return normalizeDefault(a.professorName, "").compareToIgnoreCase(normalizeDefault(b.professorName, ""));
        });

        updateProfessorRankingList(sorted);
        if (professorActivityBarChart == null) {
            return;
        }

        List<TeacherPerformanceStats> topActive = sorted.stream()
                .filter(stats -> stats.confirmed > 0)
                .limit(8)
                .collect(Collectors.toList());
        if (topActive.isEmpty()) {
            topActive = sorted.stream()
                    .filter(stats -> stats.total > 0)
                    .limit(8)
                    .collect(Collectors.toList());
        }

        professorActivityBarChart.setAnimated(false);
        professorActivityBarChart.getData().clear();

        if (topActive.isEmpty()) {
            if (professorActivitySummaryLabel != null) {
                professorActivitySummaryLabel.setText("Aucune donnée rendez-vous disponible.");
            }
            return;
        }

        XYChart.Series<String, Number> acceptedSeries = new XYChart.Series<>();
        acceptedSeries.setName("Acceptés");

        for (TeacherPerformanceStats stats : topActive) {
            String professorName = normalizeDefault(stats.professorName, "Prof " + safeNumber(stats.professorId));
            String label = shortenAxisLabel(professorName, 16);
            acceptedSeries.getData().add(new XYChart.Data<>(label, stats.confirmed));
        }

        professorActivityBarChart.setLegendVisible(false);
        professorActivityBarChart.setStyle("-fx-bar-fill: #3b82f6;");
        if (!professorActivityBarChart.getStyleClass().contains("professor-activity-chart")) {
            professorActivityBarChart.getStyleClass().add("professor-activity-chart");
        }
        ensureProfessorBarChartLabelsVisible();
        professorActivityBarChart.getData().setAll(acceptedSeries);

        TeacherPerformanceStats topEntry = topActive.get(0);
        String topProfessorName = normalizeDefault(topEntry.professorName, "Professeur " + safeNumber(topEntry.professorId));
        int topAcceptedCount = topEntry.confirmed;
        if (professorActivitySummaryLabel != null) {
            professorActivitySummaryLabel.setText(
                    "Acceptations par professeur. Top actif: "
                            + topProfessorName + " (" + topAcceptedCount + " acceptation(s))."
            );
        }

        Platform.runLater(() -> styleProfessorActivityBars(acceptedSeries));
    }

    private void ensureProfessorBarChartLabelsVisible() {
        if (professorActivityBarChart == null) {
            return;
        }
        Axis<String> xAxisRaw = professorActivityBarChart.getXAxis();
        if (xAxisRaw instanceof CategoryAxis xAxis) {
            xAxis.setLabel("Professeurs");
            xAxis.setTickLabelRotation(0);
            xAxis.setTickLabelGap(8);
            xAxis.setTickLabelFill(Color.web("#223a6d"));
            xAxis.setTickLabelsVisible(true);
            xAxis.setTickMarkVisible(false);
            xAxis.setOpacity(1.0);
            xAxis.setStyle("-fx-font-size: 12px; -fx-font-weight: 800;");
        }

        Axis<Number> yAxisRaw = professorActivityBarChart.getYAxis();
        if (yAxisRaw instanceof NumberAxis yAxis) {
            yAxis.setLabel("Acceptations");
            yAxis.setTickLabelFill(Color.web("#223a6d"));
            yAxis.setTickLabelsVisible(true);
            yAxis.setMinorTickVisible(false);
            yAxis.setForceZeroInRange(true);
            yAxis.setOpacity(1.0);
            yAxis.setStyle("-fx-font-size: 12px; -fx-font-weight: 700;");
        }

        professorActivityBarChart.setVerticalGridLinesVisible(false);
        professorActivityBarChart.setHorizontalGridLinesVisible(true);
    }

    private String shortenAxisLabel(String value, int maxLength) {
        String normalized = normalizeDefault(value, "-").replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxLength - 1)) + "…";
    }

    private void updateProfessorRankingList(List<TeacherPerformanceStats> sortedStats) {
        if (professorRankingContainer == null) {
            return;
        }
        professorRankingContainer.getChildren().clear();

        if (sortedStats == null || sortedStats.isEmpty()) {
            Label empty = new Label("Aucun professeur trouvé.");
            empty.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 12px; -fx-font-weight: 700;");
            professorRankingContainer.getChildren().add(empty);
            return;
        }

        int rank = 1;
        for (TeacherPerformanceStats stats : sortedStats) {
            HBox row = new HBox(10);
            row.setStyle("-fx-alignment: center-left; -fx-padding: 8 10; -fx-background-color: #ffffff; -fx-border-color: #dbe5f6; -fx-border-radius: 9; -fx-background-radius: 9;");

            Label rankBadge = new Label(String.valueOf(rank));
            rankBadge.setMinWidth(22);
            rankBadge.setStyle("-fx-alignment: center; -fx-text-fill: #264fb2; -fx-font-size: 12px; -fx-font-weight: 900; -fx-background-color: #edf2fc; -fx-background-radius: 999;");

            VBox textBox = new VBox(2);
            Label name = new Label(normalizeDefault(stats.professorName, "Professeur " + safeNumber(stats.professorId)));
            name.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 12px; -fx-font-weight: 800;");
            name.setWrapText(true);
            Label details = new Label(
                    "Acceptés: " + stats.confirmed
                            + "  •  Refusés: " + stats.refused
                            + "  •  En attente: " + stats.pending
                            + "  •  Total: " + stats.total
            );
            details.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 11px; -fx-font-weight: 700;");
            textBox.getChildren().addAll(name, details);
            HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

            Label acceptedChip = new Label(stats.confirmed + " OK");
            acceptedChip.setStyle("-fx-background-color: #eaf9f1; -fx-text-fill: #16855a; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 8; -fx-background-radius: 8;");

            row.getChildren().addAll(rankBadge, textBox, acceptedChip);
            professorRankingContainer.getChildren().add(row);
            rank++;
        }
    }

    private void styleProfessorActivityBars(XYChart.Series<String, Number> acceptedSeries) {
        String barColor = "#3b82f6";

        if (acceptedSeries == null) {
            return;
        }
        for (XYChart.Data<String, Number> bar : acceptedSeries.getData()) {
            if (bar.getNode() != null) {
                bar.getNode().setStyle("-fx-bar-fill: " + barColor + ";");
            }
            bar.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-bar-fill: " + barColor + ";");
                }
            });
        }
    }

    private void applyTeacherStatsPieColors(TeacherPerformanceStats stats, ObservableList<PieChart.Data> chartData) {
        if (chartData == null || chartData.isEmpty()) {
            return;
        }

        if (stats.processedCount() <= 0) {
            setPieSliceColor(chartData.get(0), "#cbd5e1");
            return;
        }

        for (PieChart.Data data : chartData) {
            String name = normalizeDefault(data.getName(), "").toLowerCase(Locale.ROOT);
            if (name.contains("accept")) {
                setPieSliceColor(data, "#3b82f6");
            } else if (name.contains("refus")) {
                setPieSliceColor(data, "#ef4444");
            } else {
                setPieSliceColor(data, "#cbd5e1");
            }
        }
    }

    private void setPieSliceColor(PieChart.Data data, String color) {
        if (data == null || color == null) {
            return;
        }
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
        }
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-pie-color: " + color + ";");
            }
        });
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String shortenForChart(String value, int maxLength) {
        String normalized = normalizeDefault(value, "-");
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(1, maxLength - 1)) + "…";
    }

    private String formatMonthLabel(YearMonth yearMonth) {
        if (yearMonth == null) {
            return "-";
        }
        String month = yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return month + " " + yearMonth.getYear();
    }

    private YearMonth resolveRendezVousMonthForStats(RendezVous rdv, Map<Integer, LocalDateTime> slotStartCache) {
        if (rdv == null) {
            return null;
        }
        LocalDateTime dateTime = rdv.getCreatedAt();
        if (dateTime != null) {
            return YearMonth.from(dateTime);
        }
        Integer slotId = rdv.getSlotId();
        if (slotId == null) {
            return null;
        }
        LocalDateTime slotStartAt = resolveSlotStartAtForStats(slotId, slotStartCache);
        if (slotStartAt == null) {
            return null;
        }
        return YearMonth.from(slotStartAt);
    }

    private LocalDateTime resolveSlotStartAtForStats(Integer slotId, Map<Integer, LocalDateTime> slotStartCache) {
        if (slotId == null) {
            return null;
        }
        if (slotStartCache.containsKey(slotId)) {
            return slotStartCache.get(slotId);
        }
        LocalDateTime resolved = null;
        try {
            AvailabilitySlot slot = availabilitySlotService.getById(slotId);
            if (slot != null) {
                resolved = slot.getStartAt();
            }
        } catch (SQLException ignored) {
            // Keep fallback when slot lookup fails.
        }
        slotStartCache.put(slotId, resolved);
        return resolved;
    }

    private Integer resolveProfessorIdForStats(RendezVous rdv, Map<Integer, Integer> slotProfessorCache) {
        if (rdv == null) {
            return null;
        }
        if (rdv.getProfessorId() != null) {
            return rdv.getProfessorId();
        }
        Integer slotId = rdv.getSlotId();
        if (slotId == null) {
            return null;
        }
        if (slotProfessorCache.containsKey(slotId)) {
            return slotProfessorCache.get(slotId);
        }
        Integer resolved = null;
        try {
            AvailabilitySlot slot = availabilitySlotService.getById(slotId);
            if (slot != null) {
                resolved = slot.getProfessorId();
            }
        } catch (SQLException ignored) {
            // Keep fallback when slot lookup fails.
        }
        slotProfessorCache.put(slotId, resolved);
        return resolved;
    }

    private static final class TeacherPerformanceStats {
        private final Integer professorId;
        private final String professorName;
        private int total;
        private int confirmed;
        private int refused;
        private int pending;

        private TeacherPerformanceStats(Integer professorId, String professorName) {
            this.professorId = professorId;
            this.professorName = professorName;
        }

        private int processedCount() {
            return confirmed + refused;
        }

        private double acceptedRate() {
            int processed = processedCount();
            if (processed <= 0) {
                return 0.0;
            }
            return (confirmed * 100.0) / processed;
        }

        private double refusedRate() {
            int processed = processedCount();
            if (processed <= 0) {
                return 0.0;
            }
            return (refused * 100.0) / processed;
        }

        @Override
        public String toString() {
            if (professorName != null && !professorName.isBlank()) {
                return professorName;
            }
            return "Professeur " + (professorId == null ? "-" : professorId);
        }
    }

    private boolean matchesSearch(RendezVous rdv, String q) {
        if (q.isEmpty()) {
            return true;
        }
        return String.valueOf(rdv.getId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getStudentId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getProfessorId()).toLowerCase().contains(q)
                || String.valueOf(rdv.getSlotId()).toLowerCase().contains(q)
                || resolveStudentName(rdv.getStudentId()).toLowerCase().contains(q)
                || resolveProfessorName(rdv.getProfessorId()).toLowerCase().contains(q)
                || resolveCourseName(rdv.getCourseId()).toLowerCase().contains(q)
                || normalizeDefault(rdv.getStatut(), "-").toLowerCase().contains(q)
                || normalizeDefault(rdv.getMeetingType(), "-").toLowerCase().contains(q)
                || normalizeDefault(formatDateTime(rdv.getCreatedAt()), "-").toLowerCase().contains(q);
    }

    private void renderCards(List<RendezVous> rendezVousList) {
        if (cardsContainer == null) {
            return;
        }
        if (cardsContainer instanceof GridPane gridContainer) {
            renderRendezVousCardsGrid(gridContainer, rendezVousList);
            return;
        }
        if (cardsContainer instanceof FlowPane flowContainer) {
            renderRendezVousCards(flowContainer, rendezVousList);
            return;
        }
        if (cardsContainer instanceof VBox tableContainer) {
            renderRendezVousRows(tableContainer, rendezVousList);
        }
    }

    private void renderRendezVousCardsGrid(GridPane gridContainer, List<RendezVous> rendezVousList) {
        gridContainer.getChildren().clear();
        gridContainer.setMinHeight(Region.USE_PREF_SIZE);
        gridContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        if (rendezVousList.isEmpty()) {
            Label empty = new Label("No rendez-vous found with current filter.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            gridContainer.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, 2);
            return;
        }

        int columns = rendezVousList.size() > 1 ? 2 : 1;
        double cardWidth = computeCardWidth(gridContainer, columns);
        int index = 0;
        for (RendezVous rdv : rendezVousList) {
            int row = index / columns;
            int col = index % columns;
            gridContainer.add(buildRendezVousCard(rdv, cardWidth), col, row);
            index++;
        }
    }

    private void renderRendezVousCards(FlowPane flowContainer, List<RendezVous> rendezVousList) {
        flowContainer.getChildren().clear();
        if (rendezVousList.isEmpty()) {
            Label empty = new Label("No rendez-vous found with current filter.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            flowContainer.getChildren().add(empty);
            return;
        }
        double cardWidth = computeCardWidth(flowContainer, rendezVousList.size());
        for (RendezVous rdv : rendezVousList) {
            flowContainer.getChildren().add(buildRendezVousCard(rdv, cardWidth));
        }
    }

    private void renderRendezVousRows(VBox tableContainer, List<RendezVous> rendezVousList) {
        tableContainer.getChildren().clear();
        if (rendezVousList.isEmpty()) {
            Label empty = new Label("No rendez-vous found with current filter.");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-padding: 16;");
            tableContainer.getChildren().add(empty);
            return;
        }
        for (int i = 0; i < rendezVousList.size(); i++) {
            RendezVous rdv = rendezVousList.get(i);
            tableContainer.getChildren().add(buildRendezVousRow(rdv, i == rendezVousList.size() - 1));
        }
    }

    private double computeCardWidth(FlowPane flowContainer, int cardCount) {
        double viewportWidth = 0;
        if (cardsScrollPane != null && cardsScrollPane.getViewportBounds() != null) {
            viewportWidth = cardsScrollPane.getViewportBounds().getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = flowContainer.getWidth();
        }
        if (viewportWidth <= 0) {
            return 540;
        }

        double horizontalPadding = flowContainer.getInsets().getLeft() + flowContainer.getInsets().getRight();
        double usableWidth = Math.max(0, viewportWidth - horizontalPadding);
        double gap = Math.max(0, flowContainer.getHgap());

        if (cardCount > 1) {
            double widthTwoCols = Math.floor((usableWidth - gap) / 2.0);
            double safeWidth = Math.max(220, widthTwoCols);
            flowContainer.setPrefWrapLength((safeWidth * 2) + gap + 1);
            return safeWidth;
        }

        flowContainer.setPrefWrapLength(usableWidth);
        return Math.max(340, Math.floor(usableWidth));
    }

    private double computeCardWidth(GridPane gridContainer, int columns) {
        double viewportWidth = 0;
        if (cardsScrollPane != null && cardsScrollPane.getViewportBounds() != null) {
            viewportWidth = cardsScrollPane.getViewportBounds().getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = gridContainer.getWidth();
        }
        if (viewportWidth <= 0) {
            return columns > 1 ? 520 : 980;
        }

        double horizontalPadding = gridContainer.getInsets().getLeft() + gridContainer.getInsets().getRight();
        double totalGap = gridContainer.getHgap() * Math.max(0, columns - 1);
        double usableWidth = Math.max(0, viewportWidth - horizontalPadding - totalGap);
        double width = usableWidth / Math.max(1, columns);
        double minCardWidth = columns > 1 ? 280 : 340;
        return Math.max(minCardWidth, Math.floor(width));
    }

    private VBox buildRendezVousCard(RendezVous rdv, double cardWidth) {
        boolean selected = selectedRendezVous != null
                && selectedRendezVous.getId() != null
                && selectedRendezVous.getId().equals(rdv.getId());

        VBox card = new VBox(10);
        String borderColor = selected ? "#2f5fc8" : "#d2dff3";
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: " + borderColor + "; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 16;");
        card.setPrefWidth(cardWidth);
        card.setMinWidth(cardWidth);
        card.setMaxWidth(cardWidth);

        HBox header = new HBox(8);
        Label idLabel = new Label("Rendez-vous " + safeNumber(rdv.getId()));
        idLabel.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 36px; -fx-font-weight: 900;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusBadge = new Label(statusDisplay(rdv.getStatut()));
        statusBadge.setStyle(statusStyle(rdv.getStatut()));
        header.getChildren().addAll(idLabel, spacer, statusBadge);

        HBox dateRow = new HBox(12);
        dateRow.setStyle("-fx-border-color: #c6d3eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-background-color: #f3f6fc; -fx-padding: 10 12;");
        Label dateLabel = new Label(formatCardDate(rdv.getCreatedAt()) + "  —  " + meetingTypeDisplay(rdv.getMeetingType()));
        dateLabel.setStyle("-fx-text-fill: #2a3e67; -fx-font-size: 14px; -fx-font-weight: 700;");
        Region dateSpacer = new Region();
        HBox.setHgrow(dateSpacer, javafx.scene.layout.Priority.ALWAYS);
        Label durationLabel = new Label("(60 min)");
        durationLabel.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 13px; -fx-font-weight: 700;");
        dateRow.getChildren().addAll(dateLabel, dateSpacer, durationLabel);

        Label line1 = new Label("Professeur :  " + resolveProfessorName(rdv.getProfessorId()));
        line1.setStyle("-fx-text-fill: #465c86; -fx-font-size: 15px;");
        Label line2 = new Label("Etudiant  :  " + resolveStudentName(rdv.getStudentId()));
        line2.setStyle("-fx-text-fill: #465c86; -fx-font-size: 15px;");
        Label line3 = new Label("Cours     :  " + resolveCourseName(rdv.getCourseId()));
        line3.setStyle("-fx-text-fill: #465c86; -fx-font-size: 15px;");

        HBox linkRow = new HBox();
        linkRow.setStyle("-fx-border-color: #c6d3eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-background-color: #f3f6fc; -fx-padding: 10 12;");
        String linkText = normalizeDefault(rdv.getMeetingLink(), normalizeDefault(rdv.getLocationLabel(), normalizeDefault(rdv.getLocation(), "-")));
        Label linkLabel = new Label("Lien / Lieu : " + linkText);
        linkLabel.setWrapText(true);
        linkLabel.setStyle("-fx-text-fill: #2a3e67; -fx-font-size: 14px; -fx-font-weight: 700;");
        linkRow.getChildren().add(linkLabel);

        HBox actions = new HBox(10);
        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle("-fx-background-color: #edf2fc; -fx-border-color: #9fb3dd; -fx-border-radius: 14; -fx-background-radius: 14; -fx-text-fill: #264fb2; -fx-font-weight: 800;");
        detailsBtn.setOnAction(event -> showRendezVousDetailsDialog(rdv));
        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #2f5fc8, #264fb2); -fx-background-radius: 14; -fx-text-fill: white; -fx-font-weight: 800;");
        editBtn.setOnAction(event -> {
            selectedRendezVous = rdv;
            editSelectedRendezVous();
        });
        Button coursePdfBtn = buildCoursePdfButton(rdv);

        if (hasProfessorPrivileges()) {
            boolean pending = isPendingStatus(rdv.getStatut());
            Label meetingLinkLabel = new Label("Lien de réunion");
            meetingLinkLabel.setStyle("-fx-text-fill: #5c7199; -fx-font-size: 13px; -fx-font-weight: 700;");
            TextField meetingLinkField = new TextField(normalizeDefault(rdv.getMeetingLink(), ""));
            meetingLinkField.setPromptText("https://...");
            meetingLinkField.setDisable(!pending);
            styleTextField(meetingLinkField);
            HBox.setHgrow(meetingLinkField, javafx.scene.layout.Priority.ALWAYS);

            Button acceptBtn = new Button("Accepter");
            acceptBtn.setStyle("-fx-background-color: #15803d; -fx-background-radius: 14; -fx-text-fill: white; -fx-font-weight: 800;");
            acceptBtn.setOnAction(event -> updateStatusAsProfessor(rdv, "confirme", meetingLinkField.getText()));

            Button refuseBtn = new Button("Refuser");
            refuseBtn.setStyle("-fx-background-color: #b91c1c; -fx-background-radius: 14; -fx-text-fill: white; -fx-font-weight: 800;");
            refuseBtn.setOnAction(event -> updateStatusAsProfessor(rdv, "refuse"));
            acceptBtn.setManaged(pending);
            acceptBtn.setVisible(pending);
            refuseBtn.setManaged(pending);
            refuseBtn.setVisible(pending);

            VBox professorActions = new VBox(8);
            HBox firstRow = new HBox(10, detailsBtn, coursePdfBtn);
            if (canStudentManage(rdv)) {
                firstRow.getChildren().add(editBtn);
            }
            HBox secondRow = new HBox(10, meetingLinkField, acceptBtn, refuseBtn);
            HBox.setHgrow(meetingLinkField, javafx.scene.layout.Priority.ALWAYS);
            VBox topApprovalBox = new VBox(4, meetingLinkLabel, secondRow);
            topApprovalBox.setStyle("-fx-background-color: #f3f6fc; -fx-border-color: #c6d3eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 12;");
            topApprovalBox.setManaged(pending);
            topApprovalBox.setVisible(pending);
            professorActions.getChildren().add(firstRow);
            card.getChildren().addAll(header, topApprovalBox, dateRow, line1, line2, line3, linkRow, professorActions);
        } else {
            actions.getChildren().addAll(detailsBtn, editBtn, coursePdfBtn);
            card.getChildren().addAll(header, dateRow, line1, line2, line3, linkRow, actions);
        }

        card.setOnMouseClicked(event -> {
            selectedRendezVous = rdv;
            applyFiltersAndRender(false);
        });
        return card;
    }

    private VBox buildRendezVousRow(RendezVous rdv, boolean lastRow) {
        boolean selected = selectedRendezVous != null
                && selectedRendezVous.getId() != null
                && selectedRendezVous.getId().equals(rdv.getId());

        VBox rowWrap = new VBox(8);
        String background = selected ? "#eef4ff" : "#ffffff";
        String borderColor = selected ? "#2f5fc8" : "#d2dff3";
        String borderWidth = selected
                ? (lastRow ? "1.4 0 0 3.4" : "1.4 0 1.4 3.4")
                : (lastRow ? "1 0 0 0" : "1 0 1 0");
        rowWrap.setStyle("-fx-background-color: " + background + "; -fx-border-color: " + borderColor + "; -fx-border-width: " + borderWidth + "; -fx-padding: 12 14;");

        HBox row = new HBox(0);
        row.setStyle("-fx-alignment: center-left;");

        VBox dateCell = new VBox(2);
        dateCell.setPrefWidth(180);
        Label dateMain = new Label(formatCardDate(rdv.getCreatedAt()));
        dateMain.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 15px; -fx-font-weight: 800;");
        Label dateSub = new Label(meetingTypeDisplay(rdv.getMeetingType()));
        dateSub.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 12px; -fx-font-weight: 700;");
        dateCell.getChildren().addAll(dateMain, dateSub);

        VBox professorCell = new VBox(2);
        professorCell.setPrefWidth(200);
        Label professorMain = new Label(resolveProfessorName(rdv.getProfessorId()));
        professorMain.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 800;");
        Label slotSub = new Label("Créneau " + safeNumber(rdv.getSlotId()));
        slotSub.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 12px;");
        professorCell.getChildren().addAll(professorMain, slotSub);

        VBox studentCell = new VBox(2);
        studentCell.setPrefWidth(200);
        Label studentMain = new Label(resolveStudentName(rdv.getStudentId()));
        studentMain.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 800;");
        Label idSub = new Label("RDV " + safeNumber(rdv.getId()));
        idSub.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 12px;");
        studentCell.getChildren().addAll(studentMain, idSub);

        VBox courseCell = new VBox(2);
        courseCell.setPrefWidth(230);
        Label courseMain = new Label(resolveCourseName(rdv.getCourseId()));
        courseMain.setWrapText(true);
        courseMain.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 800;");
        String linkText = normalizeDefault(rdv.getMeetingLink(), normalizeDefault(rdv.getLocationLabel(), normalizeDefault(rdv.getLocation(), "-")));
        Label linkSub = new Label(linkText);
        linkSub.setWrapText(true);
        linkSub.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 12px;");
        courseCell.getChildren().addAll(courseMain, linkSub);

        HBox statusCell = new HBox();
        statusCell.setPrefWidth(150);
        Label statusBadge = new Label(statusDisplay(rdv.getStatut()));
        statusBadge.setStyle(statusStyle(rdv.getStatut()) + "-fx-font-size: 12px;");
        statusCell.getChildren().add(statusBadge);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox actions = new HBox(8);
        actions.setPrefWidth(300);
        actions.setStyle("-fx-alignment: center-left;");

        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle("-fx-background-color: #edf2fc; -fx-border-color: #9fb3dd; -fx-border-radius: 10; -fx-background-radius: 10; -fx-text-fill: #264fb2; -fx-font-size: 12px; -fx-font-weight: 800;");
        detailsBtn.setOnAction(event -> showRendezVousDetailsDialog(rdv));

        Button editBtn = new Button("Modifier");
        editBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #2f5fc8, #264fb2); -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 800;");
        editBtn.setOnAction(event -> {
            selectedRendezVous = rdv;
            editSelectedRendezVous();
        });

        Button coursePdfBtn = buildCoursePdfButton(rdv);
        coursePdfBtn.setText(toNull(rdv.getCoursePdfName()) == null ? "PDF +" : "PDF");
        coursePdfBtn.setStyle("-fx-background-color: #edf2fc; -fx-background-radius: 10; -fx-text-fill: #264fb2; -fx-font-size: 12px; -fx-font-weight: 800;");

        row.getChildren().addAll(dateCell, professorCell, studentCell, courseCell, statusCell, spacer, actions);

        if (hasProfessorPrivileges()) {
            boolean pending = isPendingStatus(rdv.getStatut());
            TextField meetingLinkField = new TextField(normalizeDefault(rdv.getMeetingLink(), ""));
            meetingLinkField.setPromptText("https://...");
            meetingLinkField.setDisable(!pending);
            meetingLinkField.setMaxWidth(250);
            styleTextField(meetingLinkField);

            Button acceptBtn = new Button("Accepter");
            acceptBtn.setStyle("-fx-background-color: #15803d; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 800;");
            acceptBtn.setOnAction(event -> updateStatusAsProfessor(rdv, "confirme", meetingLinkField.getText()));

            Button refuseBtn = new Button("Refuser");
            refuseBtn.setStyle("-fx-background-color: #b91c1c; -fx-background-radius: 10; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 800;");
            refuseBtn.setOnAction(event -> updateStatusAsProfessor(rdv, "refuse"));
            acceptBtn.setManaged(pending);
            acceptBtn.setVisible(pending);
            refuseBtn.setManaged(pending);
            refuseBtn.setVisible(pending);

            actions.getChildren().addAll(detailsBtn, coursePdfBtn);
            if (canStudentManage(rdv)) {
                actions.getChildren().add(editBtn);
            }

            HBox professorRow = new HBox(8);
            professorRow.setStyle("-fx-alignment: center-right;");
            Region professorSpacer = new Region();
            HBox.setHgrow(professorSpacer, javafx.scene.layout.Priority.ALWAYS);
            professorRow.getChildren().addAll(professorSpacer, meetingLinkField, acceptBtn, refuseBtn);
            professorRow.setManaged(pending);
            professorRow.setVisible(pending);

            rowWrap.getChildren().addAll(professorRow, row);
        } else {
            actions.getChildren().addAll(detailsBtn, editBtn, coursePdfBtn);
            rowWrap.getChildren().add(row);
        }

        rowWrap.setOnMouseClicked(event -> {
            selectedRendezVous = rdv;
            applyFiltersAndRender(false);
        });
        return rowWrap;
    }

    private String statusStyle(String rawStatus) {
        String status = normalizeDefault(rawStatus, "").toLowerCase();
        if (status.contains("attente")) {
            return "-fx-background-color: #fff4db; -fx-text-fill: #a46708; -fx-font-weight: 800; -fx-padding: 6 14; -fx-background-radius: 14;";
        }
        if (status.contains("confirm")) {
            return "-fx-background-color: #eaf9f1; -fx-text-fill: #16855a; -fx-font-weight: 800; -fx-padding: 6 14; -fx-background-radius: 14;";
        }
        if (status.contains("annul") || status.contains("cancel")) {
            return "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563; -fx-font-weight: 800; -fx-padding: 6 14; -fx-background-radius: 14;";
        }
        if (status.contains("refus") || status.contains("rejet")) {
            return "-fx-background-color: #fdecee; -fx-text-fill: #c2414b; -fx-font-weight: 800; -fx-padding: 6 14; -fx-background-radius: 14;";
        }
        return "-fx-background-color: #edf2fc; -fx-text-fill: #264fb2; -fx-font-weight: 800; -fx-padding: 6 14; -fx-background-radius: 14;";
    }

    @FXML
    private void handleCreate() {
        if (!hasStudentPrivileges()) {
            showWarning("Action not allowed", "Only student/admin can create rendez-vous.");
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
            showWarning("Selection required", "Select a rendez-vous " + selectionTargetLabel() + " before trying to edit.");
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
                statusLabel.setText("Rendez-vous " + selectedRendezVous.getId() + " updated");
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
            showWarning("Selection required", "Select a rendez-vous " + selectionTargetLabel() + " before trying to delete.");
            return;
        }
        if (!canStudentManage(selectedRendezVous)) {
            showWarning("Action not allowed", "You can delete only your own rendez-vous as a student.");
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete rendez-vous " + selectedRendezVous.getId() + "?",
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
                statusLabel.setText("Rendez-vous " + deletedId + " deleted");
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
    private void goToRendezVousBackOffice(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/backoffice/rendezvous-backoffice.fxml", "SkillOra - BackOffice RendezVous");
    }

    @FXML
    private void goToSlots(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/availability-slot/availability-slot-list.fxml", "SkillOra - Availability Slots");
    }

    @FXML
    private void goToSlotsBackOffice(ActionEvent event) {
        switchScene(event, "/tn/esprit/views/backoffice/availability-slot-backoffice.fxml", "SkillOra - BackOffice Slots");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        System.clearProperty("skillora.userId");
        System.clearProperty("skillora.role");
        switchScene(event, "/tn/esprit/views/auth/login-view.fxml", "SkillOra - Login");
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
        dialog.setTitle("Détails rendez-vous " + safeNumber(rdv.getId()));
        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        Label title = new Label("Rendez-vous " + safeNumber(rdv.getId()));
        title.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 24px; -fx-font-weight: 800;");
        Label subtitle = new Label("Statut : " + statusDisplay(rdv.getStatut()));
        subtitle.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 14px; -fx-font-weight: 700;");

        Button exportBtn = new Button("Exporter en PDF");
        exportBtn.setStyle("-fx-background-color: #264fb2; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 12;");
        exportBtn.setOnAction(event -> exportRendezVousDetailsPdf(rdv));

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(12, new VBox(4, title, subtitle), headerSpacer, exportBtn);
        header.setStyle("-fx-alignment: center-left;");

        VBox detailsBox = new VBox(8);
        detailsBox.getChildren().addAll(
                buildDetailsRow("ID", safeNumber(rdv.getId())),
                buildDetailsRow("Statut", statusDisplay(rdv.getStatut())),
                buildDetailsRow("Professeur", safeNumber(rdv.getProfessorId())),
                buildDetailsRow("Etudiant", safeNumber(rdv.getStudentId())),
                buildDetailsRow("Cours", rdv.getCourseId() == null ? "-" : safeNumber(rdv.getCourseId())),
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
        content.setStyle("-fx-background-color: #f7f9fd; -fx-padding: 18; -fx-border-color: #c6d3eb; -fx-border-radius: 14; -fx-background-radius: 14;");
        content.setPrefWidth(920);
        content.setPrefHeight(650);

        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        pane.setContent(content);

        Button closeBtn = (Button) pane.lookupButton(closeType);
        if (closeBtn != null) {
            closeBtn.setStyle("-fx-background-color: #e9effb; -fx-text-fill: #264fb2; -fx-font-weight: 800; -fx-background-radius: 12; -fx-border-color: #9fb3dd; -fx-border-radius: 12;");
        }

        dialog.showAndWait();
    }

    private HBox buildDetailsRow(String labelText, String valueText) {
        Label label = new Label(labelText + " :");
        label.setMinWidth(DETAILS_KEY_WIDTH);
        label.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 13px; -fx-font-weight: 700;");

        Label value = new Label(normalizeDefault(valueText, "-"));
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px;");
        HBox.setHgrow(value, javafx.scene.layout.Priority.ALWAYS);

        HBox row = new HBox(12, label, value);
        row.setStyle("-fx-background-color: #ffffff; -fx-padding: 10 12; -fx-background-radius: 10; -fx-border-color: #c6d3eb; -fx-border-radius: 10;");
        return row;
    }

    private Button buildCoursePdfButton(RendezVous rdv) {
        Button button = new Button();
        button.setStyle("-fx-background-color: #edf2fc; -fx-background-radius: 14; -fx-text-fill: #264fb2; -fx-font-weight: 800;");

        boolean canEditPdf = canManageCoursePdf(rdv);
        String existingPdf = toNull(rdv == null ? null : rdv.getCoursePdfName());

        if (canEditPdf) {
            button.setText(existingPdf == null ? "Ajouter PDF cours" : "Modifier PDF cours");
            button.setOnAction(event -> attachCoursePdf(rdv));
            return button;
        }

        if (existingPdf != null) {
            button.setText("PDF cours");
            button.setOnAction(event -> showInfo("PDF cours", existingPdf));
        } else {
            button.setText("Aucun PDF");
            button.setDisable(true);
        }
        return button;
    }

    private boolean canManageCoursePdf(RendezVous rdv) {
        return (rdv != null) && (isAdminMode() || canStudentManage(rdv));
    }

    private void attachCoursePdf(RendezVous source) {
        if (source == null) {
            showWarning("Selection required", "Select a rendez-vous first.");
            return;
        }
        if (!canManageCoursePdf(source)) {
            showWarning("Action not allowed", "Only student owner can add course PDF.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Ajouter un PDF de cours");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File picked = chooser.showOpenDialog(statusLabel == null || statusLabel.getScene() == null ? null : statusLabel.getScene().getWindow());
        if (picked == null) {
            return;
        }

        RendezVous updated = copyRendezVous(source);
        updated.setCoursePdfName(validatePdfFileName(picked));
        try {
            boolean ok = rendezVousService.update(updated);
            if (!ok) {
                showWarning("Update failed", "Unable to save PDF for this rendez-vous.");
                return;
            }
            refreshRendezVous();
            if (statusLabel != null) {
                statusLabel.setText("PDF ajouté au rendez-vous " + safeNumber(updated.getId()));
            }
        } catch (IllegalArgumentException exception) {
            showWarning("Validation error", exception.getMessage());
        } catch (SQLException exception) {
            showError("Unable to update rendez-vous PDF", exception);
        }
    }

    private String resolveSlotSummary(Integer slotId) {
        if (slotId == null) {
            return "-";
        }
        try {
            AvailabilitySlot slot = availabilitySlotService.getById(slotId);
            if (slot == null) {
                return safeNumber(slotId);
            }
            String start = formatDateTime(slot.getStartAt());
            String end = formatDateTime(slot.getEndAt());
            return safeNumber(slot.getId()) + "  " + start + " -> " + end;
        } catch (SQLException exception) {
            return safeNumber(slotId);
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
            lines.add("Details du rendez-vous " + safeNumber(rdv.getId()));
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
        lines.add("Professeur: " + safeNumber(rdv.getProfessorId()));
        lines.add("Etudiant: " + safeNumber(rdv.getStudentId()));
        lines.add("Cours: " + (rdv.getCourseId() == null ? "-" : safeNumber(rdv.getCourseId())));
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
            slotOptions = loadSlotOptions(null, null);
        } catch (SQLException exception) {
            showError("Unable to load slots for rendez-vous creation", exception);
            return Optional.empty();
        }

        if (slotOptions.isEmpty()) {
            showWarning("No slot available", buildNoSlotAvailableMessage());
            return Optional.empty();
        }

        List<ProfessorOption> professorOptions = extractProfessorOptions(slotOptions);
        if (professorOptions.isEmpty()) {
            showWarning("No professor available", "No professor with available slots was found.");
            return Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouveau rendez-vous");
        ButtonType createType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, createType);

        TextField professorSearchField = new TextField();
        professorSearchField.setPromptText("Rechercher un professeur...");
        styleTextField(professorSearchField);

        ComboBox<ProfessorOption> professorCombo = new ComboBox<>();
        styleCombo(professorCombo);
        professorCombo.getItems().setAll(professorOptions);
        professorCombo.setValue(professorOptions.get(0));
        professorCombo.setPromptText("Choisir professeur");

        ComboBox<SlotOption> slotCombo = new ComboBox<>();
        styleCombo(slotCombo);
        slotCombo.setPromptText("Choisir créneau");

        ListView<SlotOption> slotsListView = new ListView<>();
        slotsListView.setPrefHeight(172);
        slotsListView.setMaxHeight(172);
        slotsListView.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b8c8e7; -fx-border-radius: 10; -fx-background-radius: 10;");
        slotCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!Objects.equals(slotsListView.getSelectionModel().getSelectedItem(), newValue)) {
                slotsListView.getSelectionModel().select(newValue);
            }
        });
        slotsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !Objects.equals(slotCombo.getValue(), newValue)) {
                slotCombo.setValue(newValue);
            }
        });

        ComboBox<CourseOption> courseCombo = new ComboBox<>();
        styleCombo(courseCombo);

        Button aiSuggestBtn = new Button("✨ Suggest Best Time");
        aiSuggestBtn.setStyle("-fx-background-color: #264fb2; -fx-text-fill: #ffffff; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 8 12;");
        ProgressIndicator aiProgressIndicator = new ProgressIndicator();
        aiProgressIndicator.setManaged(false);
        aiProgressIndicator.setVisible(false);
        aiProgressIndicator.setPrefSize(22, 22);
        Label aiSuggestionLabel = new Label(" ");
        aiSuggestionLabel.setWrapText(true);
        aiSuggestionLabel.setStyle("-fx-text-fill: #264fb2; -fx-font-size: 13px; -fx-font-weight: 700;");
        aiSuggestBtn.setOnAction(event -> suggestSlotWithAi(slotsListView, courseCombo, aiSuggestBtn, aiProgressIndicator, aiSuggestionLabel));

        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton onlineRadio = new RadioButton("En ligne");
        RadioButton inPersonRadio = new RadioButton("En personne");
        onlineRadio.setToggleGroup(typeGroup);
        inPersonRadio.setToggleGroup(typeGroup);
        onlineRadio.setSelected(true);
        onlineRadio.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 700;");
        inPersonRadio.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 700;");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Votre message (optionnel)...");
        messageArea.setPrefRowCount(4);
        styleTextArea(messageArea);

        Label pdfNameLabel = new Label("Aucun fichier choisi");
        pdfNameLabel.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 13px;");
        Button fileBtn = new Button("Choisir un fichier");
        fileBtn.setStyle("-fx-background-color: #edf2fc; -fx-text-fill: #264fb2; -fx-font-weight: 700; -fx-background-radius: 10;");
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
        content.setStyle("-fx-background-color: #f7f9fd; -fx-padding: 18; -fx-border-color: #c6d3eb; -fx-border-radius: 14; -fx-background-radius: 14;");

        Label popupTitle = new Label("Nouveau rendez-vous");
        popupTitle.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 30px; -fx-font-weight: 900;");
        Button closeTopBtn = new Button("Fermer");
        closeTopBtn.setStyle("-fx-background-color: #e9effb; -fx-border-color: #9fb3dd; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #264fb2; -fx-font-weight: 700;");
        closeTopBtn.setOnAction(event -> {
            dialog.setResult(cancelType);
            dialog.close();
        });
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox topHeader = new HBox(12, popupTitle, titleSpacer, closeTopBtn);
        topHeader.setStyle("-fx-alignment: center-left;");

        Region divider = new Region();
        divider.setStyle("-fx-border-color: #d2dff3; -fx-border-width: 1 0 0 0; -fx-min-height: 1;");

        Label inlineError = new Label();
        inlineError.setWrapText(true);
        inlineError.setManaged(false);
        inlineError.setVisible(false);
        inlineError.setStyle("-fx-background-color: #fdecee; -fx-border-color: #e9a8b2; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 12; -fx-text-fill: #c2414b; -fx-font-size: 14px; -fx-font-weight: 700;");

        Label professorSearchLabel = sectionLabel("Recherche professeur");
        Label professorLabel = sectionLabel("Professeur");
        Label slotLabel = sectionLabel("Créneaux disponibles");
        Label slotSelectLabel = sectionLabel("Créneau à sélectionner");
        Label aiLabel = sectionLabel("AI Suggestion");
        HBox aiSuggestRow = new HBox(10, aiSuggestBtn, aiProgressIndicator);
        aiSuggestRow.setStyle("-fx-alignment: center-left;");
        Label courseLabel = sectionLabel("Cours existant");
        Label typeLabel = sectionLabel("Type de rendez-vous");
        Label infoTitle = sectionLabel("Information");
        Label infoText = new Label("Le professeur ajoutera le lien après confirmation.");
        infoText.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 13px;");
        Label messageLabel = sectionLabel("Message (optionnel)");
        Label pdfLabel = sectionLabel("Cours PDF (optionnel)");
        Label locationTitle = sectionLabel("Lieu du créneau (en personne)");
        Label locationPreview = new Label();
        locationPreview.setWrapText(true);
        locationPreview.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 13px;");
        VBox locationPreviewBox = new VBox(6, locationTitle, locationPreview);
        locationPreviewBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 10 12; -fx-border-color: #c6d3eb; -fx-border-radius: 10; -fx-background-radius: 10;");

        HBox typeBox = new HBox(16, onlineRadio, inPersonRadio);
        HBox pdfBox = new HBox(10, fileBtn, pdfNameLabel);

        AtomicReference<List<SlotOption>> scopedSlotOptionsRef = new AtomicReference<>(List.of());
        Runnable refreshLocationPreview = () -> {
            locationPreview.setText(formatSlotLocationDisplay(slotCombo.getValue()));
            boolean showLocation = inPersonRadio.isSelected();
            locationPreviewBox.setManaged(showLocation);
            locationPreviewBox.setVisible(showLocation);
        };
        Runnable refreshScopedSlots = () -> {
            Integer selectedProfessorId = professorCombo.getValue() == null ? null : professorCombo.getValue().id();
            List<SlotOption> scoped = filterSlotOptionsByProfessor(slotOptions, selectedProfessorId);
            scopedSlotOptionsRef.set(scoped);

            Integer preferredSlotId = slotCombo.getValue() == null ? null : slotCombo.getValue().id();
            slotCombo.getItems().setAll(scoped);
            slotsListView.getItems().setAll(scoped);

            SlotOption selectedScopedSlot = findSlotOptionById(scoped, preferredSlotId);
            slotCombo.setDisable(scoped.isEmpty());
            slotsListView.setDisable(scoped.isEmpty());
            slotCombo.setValue(selectedScopedSlot);
            if (selectedScopedSlot != null) {
                slotsListView.getSelectionModel().select(selectedScopedSlot);
            } else {
                slotsListView.getSelectionModel().clearSelection();
            }

            refreshCourseOptionsForSlot(selectedScopedSlot, courseCombo, null);
            refreshLocationPreview.run();
            clearInlineDialogError(inlineError);
            aiSuggestionLabel.setText(" ");
        };

        professorSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            ProfessorOption previousSelection = professorCombo.getValue();
            List<ProfessorOption> filtered = filterProfessorOptions(professorOptions, newValue);
            professorCombo.getItems().setAll(filtered);
            ProfessorOption selected = previousSelection == null ? null : findProfessorOptionById(filtered, previousSelection.id());
            if (selected == null && !filtered.isEmpty()) {
                selected = filtered.get(0);
            }
            professorCombo.setValue(selected);
            clearInlineDialogError(inlineError);
            aiSuggestionLabel.setText(" ");
        });
        professorCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshScopedSlots.run());
        slotCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            refreshCourseOptionsForSlot(newValue, courseCombo, null);
            refreshLocationPreview.run();
            clearInlineDialogError(inlineError);
            aiSuggestionLabel.setText(" ");
        });
        typeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> refreshLocationPreview.run());
        courseCombo.valueProperty().addListener((obs, oldValue, newValue) -> clearInlineDialogError(inlineError));
        messageArea.textProperty().addListener((obs, oldValue, newValue) -> clearInlineDialogError(inlineError));

        refreshScopedSlots.run();
        refreshLocationPreview.run();

        content.getChildren().addAll(
                topHeader, divider, inlineError,
                professorSearchLabel, professorSearchField,
                professorLabel, professorCombo,
                slotSelectLabel, slotCombo,
                slotLabel, slotsListView,
                aiLabel, aiSuggestRow, aiSuggestionLabel,
                courseLabel, courseCombo,
                typeLabel, typeBox,
                locationPreviewBox,
                infoTitle, infoText,
                messageLabel, messageArea,
                pdfLabel, pdfBox
        );

        ScrollPane formScroll = new ScrollPane(content);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefViewportHeight(560);
        formScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        DialogPane dialogPane = dialog.getDialogPane();
        styleDialog(dialogPane);
        dialogPane.setContent(formScroll);
        dialogPane.setPrefSize(860, 700);
        styleDialogButtons(dialogPane, createType, cancelType);

        AtomicReference<RendezVous> createdRef = new AtomicReference<>();
        Button createBtn = (Button) dialog.getDialogPane().lookupButton(createType);
        if (createBtn != null) {
            createBtn.addEventFilter(ActionEvent.ACTION, actionEvent -> {
                clearInlineDialogError(inlineError);
                try {
                    SlotOption selectedSlot = slotCombo.getValue();
                    CourseOption selectedCourse = courseCombo.getValue();
                    AvailabilitySlot validatedSlot = validateSelectedSlotForSubmission(selectedSlot, scopedSlotOptionsRef.get(), null);
                    CourseOption validatedCourse = validateSelectedCourseForProfessor(selectedCourse, validatedSlot.getProfessorId());
                    String validatedMessage = validateMessageInput(messageArea.getText());
                    String validatedPdfName = validatePdfFileName(selectedFile[0]);

                    RendezVous rdv = new RendezVous();
                    rdv.setSlotId(validatedSlot.getId());
                    rdv.setProfessorId(validatedSlot.getProfessorId());
                    rdv.setStudentId(getCurrentUserId());
                    rdv.setCourseId(validatedCourse.id());
                    boolean inPerson = inPersonRadio.isSelected();
                    rdv.setMeetingType(inPerson ? "en_personne" : "en_ligne");
                    rdv.setStatut("en_attente");
                    rdv.setMeetingLink(null);
                    if (inPerson) {
                        rdv.setLocation(resolveSlotLocationValue(validatedSlot));
                        rdv.setLocationLabel(toNull(validatedSlot.getLocationLabel()));
                        rdv.setLocationLat(validatedSlot.getLocationLat());
                        rdv.setLocationLng(validatedSlot.getLocationLng());
                    } else {
                        rdv.setLocation(null);
                        rdv.setLocationLabel(null);
                        rdv.setLocationLat(null);
                        rdv.setLocationLng(null);
                    }
                    rdv.setMessage(validatedMessage);
                    rdv.setCreatedAt(LocalDateTime.now());
                    rdv.setCoursePdfName(validatedPdfName);
                    createdRef.set(rdv);
                } catch (SQLException ex) {
                    createdRef.set(null);
                    showInlineDialogError(inlineError, "Impossible de valider le rendez-vous pour le moment.");
                    actionEvent.consume();
                } catch (IllegalArgumentException ex) {
                    createdRef.set(null);
                    showInlineDialogError(inlineError, ex.getMessage());
                    actionEvent.consume();
                }
            });
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != createType) {
            return Optional.empty();
        }
        return Optional.ofNullable(createdRef.get());
    }

    private void suggestSlotWithAi(
            ListView<SlotOption> slotsListView,
            ComboBox<CourseOption> courseCombo,
            Button aiSuggestBtn,
            ProgressIndicator aiProgressIndicator,
            Label aiSuggestionLabel
    ) {
        if (slotsListView == null || slotsListView.getItems().isEmpty()) {
            aiSuggestionLabel.setText("No available slots for AI suggestion.");
            return;
        }

        List<String> availableSlots = slotsListView.getItems().stream()
                .filter(Objects::nonNull)
                .map(SlotOption::label)
                .map(label -> normalizeDefault(label, "-"))
                .collect(Collectors.toList());

        String studentName = resolveStudentName(getCurrentUserId());
        CourseOption selectedCourse = courseCombo == null ? null : courseCombo.getValue();
        String subject = selectedCourse == null
                ? "Rendez-vous"
                : normalizeDefault(selectedCourse.title(), "Rendez-vous");

        aiSuggestBtn.setDisable(true);
        aiProgressIndicator.setManaged(true);
        aiProgressIndicator.setVisible(true);
        aiSuggestionLabel.setText("Generating AI suggestion...");

        Task<String> suggestTask = new Task<>() {
            @Override
            protected String call() {
                return aiService.suggestBestSlot(availableSlots, studentName, subject);
            }
        };

        suggestTask.setOnSucceeded(event -> {
            aiProgressIndicator.setVisible(false);
            aiProgressIndicator.setManaged(false);
            aiSuggestBtn.setDisable(false);
            String suggestion = suggestTask.getValue();
            if (suggestion == null || suggestion.isBlank()) {
                aiSuggestionLabel.setText("No AI suggestion available.");
            } else {
                aiSuggestionLabel.setText(suggestion.trim());
            }
        });

        suggestTask.setOnFailed(event -> {
            aiProgressIndicator.setVisible(false);
            aiProgressIndicator.setManaged(false);
            aiSuggestBtn.setDisable(false);
            aiSuggestionLabel.setText("AI suggestion failed. Please try again.");
        });

        Thread worker = new Thread(suggestTask, "rdv-ai-suggest-task");
        worker.setDaemon(true);
        worker.start();
    }

    private Optional<RendezVous> showEditRendezVousDialog(RendezVous source) {
        List<SlotOption> slotOptions;
        try {
            Integer professorScope = resolveProfessorScopeForEdit(source);
            if (professorScope == null) {
                showWarning("Professor not found", "Unable to resolve professor for this rendez-vous.");
                return Optional.empty();
            }
            slotOptions = loadSlotOptions(source.getSlotId(), professorScope);
        } catch (SQLException exception) {
            showError("Unable to load slots for edit", exception);
            return Optional.empty();
        }

        if (slotOptions.isEmpty()) {
            showWarning("No slot available", "No available slot found for this professor.");
            return Optional.empty();
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le créneau " + safeNumber(source.getId()));
        ButtonType saveType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, saveType);

        Label currentSlotLabel = new Label("Créneau actuel: " + resolveSlotLabel(source.getSlotId(), slotOptions));
        currentSlotLabel.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 14px; -fx-font-weight: 700;");

        ComboBox<SlotOption> slotCombo = new ComboBox<>();
        slotCombo.getItems().setAll(slotOptions);
        slotCombo.setValue(findSlotOptionById(slotOptions, source.getSlotId()));
        styleCombo(slotCombo);

        ComboBox<CourseOption> courseCombo = new ComboBox<>();
        styleCombo(courseCombo);
        refreshCourseOptionsForSlot(slotCombo.getValue(), courseCombo, source.getCourseId());
        slotCombo.valueProperty().addListener((obs, oldValue, newValue) ->
                refreshCourseOptionsForSlot(newValue, courseCombo, null));

        Label pdfNameLabel = new Label(normalizeDefault(source.getCoursePdfName(), "Aucun fichier choisi"));
        pdfNameLabel.setStyle("-fx-text-fill: #5f739a; -fx-font-size: 13px;");
        Button fileBtn = new Button("Choisir PDF cours");
        fileBtn.setStyle("-fx-background-color: #edf2fc; -fx-text-fill: #264fb2; -fx-font-weight: 700; -fx-background-radius: 10;");
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
        HBox pdfBox = new HBox(10, fileBtn, pdfNameLabel);

        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: #f7f9fd; -fx-padding: 18; -fx-border-color: #c6d3eb; -fx-border-radius: 14; -fx-background-radius: 14;");
        content.getChildren().addAll(
                currentSlotLabel,
                sectionLabel("Nouveau créneau"),
                slotCombo,
                sectionLabel("Cours"),
                courseCombo,
                sectionLabel("PDF cours"),
                pdfBox
        );

        styleDialog(dialog.getDialogPane());
        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog.getDialogPane(), saveType, cancelType);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != saveType) {
            return Optional.empty();
        }

        try {
            SlotOption chosen = slotCombo.getValue();
            CourseOption chosenCourse = courseCombo.getValue();
            AvailabilitySlot validatedSlot = validateSelectedSlotForSubmission(chosen, slotOptions, source.getSlotId());
            CourseOption validatedCourse = validateSelectedCourseForProfessor(chosenCourse, validatedSlot.getProfessorId());

            RendezVous updated = copyRendezVous(source);
            updated.setSlotId(validatedSlot.getId());
            updated.setProfessorId(validatedSlot.getProfessorId());
            if (isInPersonMeetingType(updated.getMeetingType())) {
                updated.setLocation(resolveSlotLocationValue(validatedSlot));
                updated.setLocationLabel(toNull(validatedSlot.getLocationLabel()));
                updated.setLocationLat(validatedSlot.getLocationLat());
                updated.setLocationLng(validatedSlot.getLocationLng());
            }
            updated.setCourseId(validatedCourse.id());
            if (selectedFile[0] != null) {
                updated.setCoursePdfName(validatePdfFileName(selectedFile[0]));
            }
            return Optional.of(updated);
        } catch (SQLException exception) {
            showError("Unable to validate rendez-vous edit input", exception);
            return Optional.empty();
        } catch (IllegalArgumentException exception) {
            showWarning("Validation error", exception.getMessage());
            return Optional.empty();
        }
    }

    private List<SlotOption> loadSlotOptions(Integer includeSlotId, Integer professorIdScope) throws SQLException {
        Set<Integer> reservedSlotIds = rendezVousService.getAll().stream()
                .filter(rdv -> rdv != null && rdv.getSlotId() != null)
                .filter(rdv -> isReservedStatus(rdv.getStatut()))
                .map(RendezVous::getSlotId)
                .collect(Collectors.toSet());

        return loadVisibleSlotsForCurrentUser().stream()
                .filter(slot -> professorIdScope == null || sameInteger(slot.getProfessorId(), professorIdScope))
                .filter(slot -> isSlotAvailable(slot, includeSlotId, reservedSlotIds))
                .map(slot -> {
                    String professorName = resolveProfessorName(slot.getProfessorId());
                    return new SlotOption(
                            slot.getId(),
                            slot.getProfessorId(),
                            professorName,
                            formatSlotRange(slot, professorName),
                            slot.getLocationLabel(),
                            slot.getLocationLat(),
                            slot.getLocationLng()
                    );
                })
                .collect(Collectors.toList());
    }

    private List<AvailabilitySlot> loadVisibleSlotsForCurrentUser() throws SQLException {
        if (isStudentMode()) {
            // Student can book only slots created by professors.
            return availabilitySlotService.getAllCreatedByProfessors();
        }
        // Admin (and other non-student flows) can see all slots.
        return availabilitySlotService.getAll();
    }

    private List<ProfessorOption> extractProfessorOptions(List<SlotOption> slotOptions) {
        if (slotOptions == null || slotOptions.isEmpty()) {
            return List.of();
        }
        Map<Integer, ProfessorOption> byId = new HashMap<>();
        for (SlotOption option : slotOptions) {
            if (option == null || option.professorId() == null) {
                continue;
            }
            String resolvedName = normalizeDefault(toNull(option.professorName()), "Professeur " + option.professorId());
            byId.putIfAbsent(option.professorId(), new ProfessorOption(option.professorId(), resolvedName));
        }
        return byId.values().stream()
                .sorted(
                        Comparator.comparing((ProfessorOption option) -> normalizeDefault(option.name(), "").toLowerCase(Locale.ROOT))
                                .thenComparing(option -> option.id() == null ? Integer.MAX_VALUE : option.id())
                )
                .collect(Collectors.toList());
    }

    private List<ProfessorOption> filterProfessorOptions(List<ProfessorOption> options, String rawQuery) {
        if (options == null || options.isEmpty()) {
            return List.of();
        }
        String query = toNull(rawQuery);
        if (query == null) {
            return new ArrayList<>(options);
        }
        String lowered = query.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option != null)
                .filter(option -> {
                    String name = normalizeDefault(option.name(), "").toLowerCase(Locale.ROOT);
                    String id = option.id() == null ? "" : option.id().toString();
                    return name.contains(lowered) || id.contains(lowered);
                })
                .collect(Collectors.toList());
    }

    private ProfessorOption findProfessorOptionById(List<ProfessorOption> options, Integer professorId) {
        if (options == null || options.isEmpty() || professorId == null) {
            return null;
        }
        return options.stream()
                .filter(option -> option != null && sameInteger(option.id(), professorId))
                .findFirst()
                .orElse(null);
    }

    private List<SlotOption> filterSlotOptionsByProfessor(List<SlotOption> options, Integer professorId) {
        if (options == null || options.isEmpty() || professorId == null) {
            return List.of();
        }
        return options.stream()
                .filter(option -> option != null && sameInteger(option.professorId(), professorId))
                .collect(Collectors.toList());
    }

    private String buildNoSlotAvailableMessage() {
        String defaultMessage = "No available slot was found to create a rendez-vous.";
        try {
            List<AvailabilitySlot> allSlots = availabilitySlotService.getAll();
            LocalDateTime now = LocalDateTime.now();
            long futureSlots = allSlots.stream()
                    .filter(Objects::nonNull)
                    .filter(slot -> slot.getEndAt() == null || !slot.getEndAt().isBefore(now))
                    .count();
            if (futureSlots == 0) {
                return "Aucun créneau futur n'existe pour le moment.";
            }

            Set<Integer> reservedSlotIds = rendezVousService.getAll().stream()
                    .filter(rdv -> rdv != null && rdv.getSlotId() != null)
                    .filter(rdv -> isReservedStatus(rdv.getStatut()))
                    .map(RendezVous::getSlotId)
                    .collect(Collectors.toSet());

            long availableFutureSlots = allSlots.stream()
                    .filter(Objects::nonNull)
                    .filter(slot -> isSlotAvailable(slot, null, reservedSlotIds))
                    .count();

            if (availableFutureSlots == 0) {
                return "Tous les créneaux futurs sont déjà réservés (en attente ou confirmés).";
            }
        } catch (SQLException ignored) {
            // Keep default message when diagnostics cannot be loaded.
        }
        return defaultMessage;
    }

    private Integer resolveProfessorScopeForEdit(RendezVous source) throws SQLException {
        if (source == null) {
            return null;
        }
        if (source.getProfessorId() != null) {
            return source.getProfessorId();
        }
        if (source.getSlotId() == null) {
            return null;
        }
        AvailabilitySlot slot = availabilitySlotService.getById(source.getSlotId());
        return slot == null ? null : slot.getProfessorId();
    }

    private boolean isSlotAvailable(AvailabilitySlot slot, Integer includeSlotId) {
        return isSlotAvailable(slot, includeSlotId, Set.of());
    }

    private boolean isSlotAvailable(AvailabilitySlot slot, Integer includeSlotId, Set<Integer> reservedSlotIds) {
        if (slot == null || slot.getId() == null) {
            return false;
        }
        if (includeSlotId != null && includeSlotId.equals(slot.getId())) {
            return true;
        }
        if (slot.getEndAt() != null && slot.getEndAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            return false;
        }
        return reservedSlotIds == null || !reservedSlotIds.contains(slot.getId());
    }

    private String formatSlotRange(AvailabilitySlot slot, String professorName) {
        String start = slot.getStartAt() == null ? "-" : slot.getStartAt().format(DATE_TIME_FORMATTER);
        String end = slot.getEndAt() == null ? "-" : slot.getEndAt().format(DATE_TIME_FORMATTER);
        String currentFlag = selectedRendezVous != null && selectedRendezVous.getSlotId() != null
                && selectedRendezVous.getSlotId().equals(slot.getId()) ? " (actuel)" : "";
        String professor = normalizeDefault(professorName, "Professeur " + safeNumber(slot.getProfessorId()));
        return "Créneau " + safeNumber(slot.getId()) + "  " + start + " -> " + end + "  |  " + professor + currentFlag;
    }

    private String formatSlotLocationDisplay(SlotOption slotOption) {
        if (slotOption == null) {
            return "Choisissez un créneau.";
        }
        String label = normalizeDefault(slotOption.locationLabel(), "-");
        String coordinates = formatCoordinates(slotOption.locationLat(), slotOption.locationLng());
        if ("-".equals(label) && "-".equals(coordinates)) {
            return "Lieu non défini pour ce créneau.";
        }
        if ("-".equals(coordinates)) {
            return label;
        }
        if ("-".equals(label)) {
            return coordinates;
        }
        return label + " (" + coordinates + ")";
    }

    private String resolveSlotLocationValue(SlotOption slotOption) {
        if (slotOption == null) {
            return null;
        }
        String label = toNull(slotOption.locationLabel());
        if (label != null) {
            return label;
        }
        if (slotOption.locationLat() != null && slotOption.locationLng() != null) {
            return slotOption.locationLat() + ", " + slotOption.locationLng();
        }
        return null;
    }

    private String resolveSlotLocationValue(AvailabilitySlot slot) {
        if (slot == null) {
            return null;
        }
        String label = toNull(slot.getLocationLabel());
        if (label != null) {
            return label;
        }
        if (slot.getLocationLat() != null && slot.getLocationLng() != null) {
            return slot.getLocationLat() + ", " + slot.getLocationLng();
        }
        return null;
    }

    private String resolveProfessorName(Integer professorId) {
        String fallback = professorId == null ? "Professeur inconnu" : ("Professeur " + professorId);
        return resolveUserDisplayName(professorId, fallback);
    }

    private String resolveStudentName(Integer studentId) {
        String fallback = studentId == null ? "Etudiant inconnu" : ("Etudiant " + studentId);
        return resolveUserDisplayName(studentId, fallback);
    }

    private String resolveUserEmail(Integer userId) {
        if (userId == null) {
            return null;
        }
        if (userEmailCache.containsKey(userId)) {
            return userEmailCache.get(userId);
        }

        String resolved = null;
        try {
            String userTable = findExistingTable("user", "users");
            if (userTable != null) {
                String emailColumn = findExistingColumn(userTable, "email", "mail");
                if (emailColumn != null) {
                    Connection connection = MyConnection.getInstance().getConnection();
                    String sql = "SELECT `" + emailColumn + "` AS user_email FROM `" + userTable + "` WHERE id = ? LIMIT 1";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setInt(1, userId);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next()) {
                                resolved = toNull(resultSet.getString("user_email"));
                            }
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // Keep null when email lookup is unavailable.
        }

        userEmailCache.put(userId, resolved);
        return resolved;
    }

    private String resolveUserDisplayName(Integer userId, String fallback) {
        if (userId == null) {
            return fallback;
        }
        String cached = userDisplayNameCache.get(userId);
        if (cached != null) {
            return cached;
        }

        String resolved = fallback;
        try {
            String userTable = findExistingTable("user", "users");
            if (userTable != null) {
                Connection connection = MyConnection.getInstance().getConnection();
                String firstNameColumn = findExistingColumn(userTable, "first_name", "firstname");
                String lastNameColumn = findExistingColumn(userTable, "last_name", "lastname");
                String usernameColumn = findExistingColumn(userTable, "username", "user_name", "login");
                String emailColumn = findExistingColumn(userTable, "email", "mail");

                List<String> projections = new ArrayList<>();
                if (firstNameColumn != null) {
                    projections.add("`" + firstNameColumn + "` AS first_name");
                }
                if (lastNameColumn != null) {
                    projections.add("`" + lastNameColumn + "` AS last_name");
                }
                if (usernameColumn != null) {
                    projections.add("`" + usernameColumn + "` AS username");
                }
                if (emailColumn != null) {
                    projections.add("`" + emailColumn + "` AS email");
                }

                if (!projections.isEmpty()) {
                    String sql = "SELECT " + String.join(", ", projections) + " FROM `" + userTable + "` WHERE id = ? LIMIT 1";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setInt(1, userId);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next()) {
                                String firstName = firstNameColumn == null ? null : toNull(resultSet.getString("first_name"));
                                String lastName = lastNameColumn == null ? null : toNull(resultSet.getString("last_name"));
                                String username = usernameColumn == null ? null : toNull(resultSet.getString("username"));
                                String email = emailColumn == null ? null : toNull(resultSet.getString("email"));
                                String fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
                                if (!fullName.isBlank()) {
                                    resolved = fullName;
                                } else if (username != null) {
                                    resolved = username;
                                } else if (email != null) {
                                    resolved = email;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // Keep fallback display when user lookup is unavailable.
        }

        userDisplayNameCache.put(userId, resolved);
        return resolved;
    }

    private String resolveCourseName(Integer courseId) {
        if (courseId == null) {
            return "-";
        }
        String cached = courseTitleCache.get(courseId);
        if (cached != null) {
            return cached;
        }

        String resolved = "Cours " + courseId;
        try {
            String courseTable = findExistingTable("course", "courses");
            if (courseTable != null) {
                String titleColumn = findExistingColumn(courseTable, "title", "name", "label");
                if (titleColumn != null) {
                    Connection connection = MyConnection.getInstance().getConnection();
                    String sql = "SELECT `" + titleColumn + "` AS course_title FROM `" + courseTable + "` WHERE id = ? LIMIT 1";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                        preparedStatement.setInt(1, courseId);
                        try (ResultSet resultSet = preparedStatement.executeQuery()) {
                            if (resultSet.next()) {
                                String title = toNull(resultSet.getString("course_title"));
                                if (title != null) {
                                    resolved = title;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
            // Keep fallback display when course lookup is unavailable.
        }

        courseTitleCache.put(courseId, resolved);
        return resolved;
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
        if (courseTable == null || professorId == null) {
            return List.of();
        }

        String titleColumn = findExistingColumn(courseTable, "title", "name", "label");
        String professorColumn = findExistingColumn(
                courseTable,
                "professor_id",
                "prof_id",
                "professeur_id",
                "teacher_id",
                "instructor_id",
                "author_id",
                "owner_id",
                "user_id",
                "created_by"
        );
        if (professorColumn == null) {
            // No professor mapping on course table: return all existing courses.
            return queryCourseOptions(courseTable, titleColumn, null, null);
        }

        List<CourseOption> scoped = queryCourseOptions(courseTable, titleColumn, professorColumn, professorId);
        if (!scoped.isEmpty()) {
            return scoped;
        }

        // Mapping exists but this professor has no linked rows: keep form usable with all courses.
        return queryCourseOptions(courseTable, titleColumn, null, null);
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
                    String title = normalizeDefault(resultSet.getString("course_title"), "Course " + courseId);
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
        pane.setStyle("-fx-background-color: #f7f9fd; -fx-border-color: #c6d3eb; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");
    }

    private void styleDialogButtons(DialogPane pane, ButtonType primary, ButtonType secondary) {
        Button primaryBtn = (Button) pane.lookupButton(primary);
        Button secondaryBtn = (Button) pane.lookupButton(secondary);
        if (primaryBtn != null) {
            primaryBtn.setStyle("-fx-background-color: linear-gradient(to bottom, #2f5fc8, #264fb2); -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 12;");
        }
        if (secondaryBtn != null) {
            secondaryBtn.setStyle("-fx-background-color: #e9effb; -fx-text-fill: #264fb2; -fx-font-weight: 800; -fx-background-radius: 12; -fx-border-color: #9fb3dd; -fx-border-radius: 12;");
        }
    }

    private void styleCombo(ComboBox<?> comboBox) {
        comboBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #b8c8e7; -fx-border-radius: 12; -fx-background-radius: 12; -fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 700;");
    }

    private void styleTextField(TextField field) {
        field.setStyle("-fx-control-inner-background: #ffffff; -fx-background-color: #ffffff; -fx-text-fill: #1f2a44; -fx-prompt-text-fill: #8aa0c6; -fx-border-color: #b8c8e7; -fx-border-radius: 12; -fx-background-radius: 12;");
    }

    private void styleTextArea(TextArea area) {
        area.setStyle("-fx-control-inner-background: #ffffff; -fx-background-color: #ffffff; -fx-text-fill: #1f2a44; -fx-prompt-text-fill: #8aa0c6; -fx-border-color: #b8c8e7; -fx-border-radius: 12; -fx-background-radius: 12;");
    }

    private void showInlineDialogError(Label errorLabel, String message) {
        if (errorLabel == null) {
            return;
        }
        errorLabel.setText(normalizeDefault(message, "Une erreur est survenue."));
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearInlineDialogError(Label errorLabel) {
        if (errorLabel == null) {
            return;
        }
        errorLabel.setText("");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 14px; -fx-font-weight: 800;");
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

    private AvailabilitySlot validateSelectedSlotForSubmission(
            SlotOption selectedSlot,
            List<SlotOption> availableOptions,
            Integer includeSlotId
    ) throws SQLException {
        if (selectedSlot == null || selectedSlot.id() == null) {
            throw new IllegalArgumentException("Please choose a slot.");
        }
        boolean existsInCurrentOptions = availableOptions.stream()
                .anyMatch(option -> option != null && sameInteger(option.id(), selectedSlot.id()));
        if (!existsInCurrentOptions) {
            throw new IllegalArgumentException("Selected slot is invalid. Please reopen the form.");
        }

        AvailabilitySlot persistedSlot = availabilitySlotService.getById(selectedSlot.id());
        if (persistedSlot == null) {
            throw new IllegalArgumentException("Selected slot no longer exists.");
        }
        if (persistedSlot.getProfessorId() == null) {
            throw new IllegalArgumentException("Selected slot has no professor.");
        }
        if (!isSlotAvailable(persistedSlot, includeSlotId)) {
            throw new IllegalArgumentException("Selected slot is no longer available.");
        }
        if (persistedSlot.getStartAt() != null && persistedSlot.getStartAt().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new IllegalArgumentException("Selected slot is in the past.");
        }

        return persistedSlot;
    }

    private CourseOption validateSelectedCourseForProfessor(CourseOption selectedCourse, Integer professorId) throws SQLException {
        if (professorId == null) {
            throw new IllegalArgumentException("Selected slot has no professor.");
        }
        if (selectedCourse == null || selectedCourse.id() == null) {
            throw new IllegalArgumentException("Please choose an existing course.");
        }

        List<CourseOption> allowedCourses = loadCourseOptionsForProfessor(professorId);
        if (allowedCourses.isEmpty()) {
            throw new IllegalArgumentException("No course available for this professor.");
        }

        return allowedCourses.stream()
                .filter(option -> option != null && sameInteger(option.id(), selectedCourse.id()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selected course is invalid for this professor."));
    }

    private String validateMessageInput(String rawMessage) {
        String message = toNull(rawMessage);
        if (message == null) {
            return null;
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message is too long (max " + MAX_MESSAGE_LENGTH + " characters).");
        }
        return message;
    }

    private String validatePdfFileName(File pickedFile) {
        if (pickedFile == null) {
            return null;
        }
        String fileName = toNull(pickedFile.getName());
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }
        if (fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new IllegalArgumentException("PDF file name is too long (max " + MAX_FILE_NAME_LENGTH + " characters).");
        }
        return fileName;
    }

    private String validateMeetingLinkInput(String rawInput) {
        String meetingLink = toNull(rawInput);
        if (meetingLink == null) {
            return null;
        }
        if (meetingLink.length() > MAX_MEETING_LINK_LENGTH) {
            throw new IllegalArgumentException("Meeting link is too long (max " + MAX_MEETING_LINK_LENGTH + " characters).");
        }
        if (!isHttpMeetingLink(meetingLink)) {
            throw new IllegalArgumentException("Meeting link must be a valid http/https URL.");
        }
        return meetingLink;
    }

    private boolean isHttpMeetingLink(String value) {
        try {
            URI uri = new URI(value);
            String scheme = toNull(uri.getScheme());
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            return toNull(uri.getHost()) != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private record ProfessorOption(Integer id, String name) {
        @Override
        public String toString() {
            String displayName = name == null ? null : name.trim();
            if (displayName == null || displayName.isBlank()) {
                displayName = id == null ? "Professeur" : ("Professeur " + id);
            }
            if (id == null) {
                return displayName;
            }
            return displayName + "  (ID " + id + ")";
        }
    }

    private record SlotOption(
            Integer id,
            Integer professorId,
            String professorName,
            String label,
            String locationLabel,
            Float locationLat,
            Float locationLng
    ) {
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
            return "Cours " + idText + "  " + (titleText.isEmpty() ? "-" : titleText);
        }
    }

    private void markSlotBookedState(Integer slotId, boolean booked) {
        if (slotId == null) {
            return;
        }
        try {
            availabilitySlotService.updateBookedStatus(slotId, booked);
        } catch (SQLException exception) {
            showWarning("Slot update", "Rendez-vous saved, but slot " + slotId + " booking state could not be updated.");
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
            return isVisibleForCurrentProfessorBySlot(rdv);
        }
        return sameInteger(rdv.getStudentId(), getCurrentUserId());
    }

    private boolean isVisibleForCurrentProfessorBySlot(RendezVous rdv) {
        if (rdv == null || rdv.getSlotId() == null) {
            return false;
        }
        try {
            AvailabilitySlot slot = availabilitySlotService.getById(rdv.getSlotId());
            if (slot == null || slot.getProfessorId() == null) {
                return false;
            }
            return sameInteger(slot.getProfessorId(), getCurrentUserId());
        } catch (SQLException exception) {
            // Keep a fallback in case the slot lookup fails temporarily.
            return sameInteger(rdv.getProfessorId(), getCurrentUserId());
        }
    }

    private boolean canStudentManage(RendezVous rdv) {
        if (rdv == null) {
            return false;
        }
        if (isAdminMode()) {
            return true;
        }
        return isStudentMode() && sameInteger(rdv.getStudentId(), getCurrentUserId());
    }

    private boolean isAdminMode() {
        return getCurrentUserRole().contains("admin");
    }

    private boolean isProfessorMode() {
        String role = getCurrentUserRole();
        return !isAdminMode() && (role.contains("prof") || role.contains("teacher"));
    }

    private boolean isStudentMode() {
        return !isAdminMode() && !isProfessorMode();
    }

    private boolean hasProfessorPrivileges() {
        return isProfessorMode() || isAdminMode();
    }

    private boolean hasStudentPrivileges() {
        return isStudentMode() || isAdminMode();
    }

    private void updateStatusAsProfessor(RendezVous source, String newStatus) {
        updateStatusAsProfessor(source, newStatus, null);
    }

    private void updateStatusAsProfessor(RendezVous source, String newStatus, String meetingLinkInput) {
        if (source == null) {
            showWarning("Selection required", "Select a rendez-vous first.");
            return;
        }
        if (!hasProfessorPrivileges()) {
            showWarning("Action not allowed", "Only professor can accept or refuse a rendez-vous.");
            return;
        }
        if (!isPendingStatus(source.getStatut())) {
            showWarning("Action not allowed", "Only pending rendez-vous can be accepted or refused.");
            return;
        }

        try {
            AvailabilitySlot slot = source.getSlotId() == null ? null : availabilitySlotService.getById(source.getSlotId());
            if (!isAdminMode()) {
                if (slot == null) {
                    showWarning("Action not allowed", "This rendez-vous has no valid slot anymore.");
                    return;
                }
                if (slot.getProfessorId() == null || !sameInteger(slot.getProfessorId(), getCurrentUserId())) {
                    showWarning("Action not allowed", "You can only update rendez-vous assigned to your own slots.");
                    return;
                }
            }

            RendezVous updated = copyRendezVous(source);
            updated.setStatut(newStatus);

            if (isConfirmedStatus(newStatus)) {
                String meetingLink = validateMeetingLinkInput(meetingLinkInput);
                if (meetingLink != null) {
                    updated.setMeetingLink(meetingLink);
                }
                updated.setRefusalReason(null);
            } else if (isRefusedStatus(newStatus)) {
                Optional<String> refusalReason = showRefusalReasonDialog();
                if (refusalReason.isEmpty()) {
                    return;
                }
                updated.setRefusalReason(refusalReason.get());
            }

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
            sendStatusEmailToStudentAsync(updated, slot);
            boolean switchedFromPendingFilter = currentFilter == RendezVousFilter.PENDING;
            refreshRendezVous();
            if (switchedFromPendingFilter) {
                setActiveFilter(RendezVousFilter.ALL);
            }
            statusLabel.setText("Rendez-vous " + safeNumber(updated.getId()) + " is now " + statusDisplay(newStatus)
                    + (switchedFromPendingFilter ? " (filtre: Tous)" : ""));
        } catch (IllegalArgumentException exception) {
            showWarning("Validation error", exception.getMessage());
        } catch (SQLException exception) {
            showError("Unable to update rendez-vous status", exception);
        }
    }

    private Optional<String> showRefusalReasonDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Refusal reason");
        ButtonType confirmType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(cancelType, confirmType);

        Label reasonLabel = sectionLabel("Reason for refusal");
        TextField reasonField = new TextField();
        reasonField.setPromptText("Enter reason...");
        styleTextField(reasonField);

        Label inlineError = new Label();
        inlineError.setManaged(false);
        inlineError.setVisible(false);
        inlineError.setWrapText(true);
        inlineError.setStyle("-fx-background-color: #fdecee; -fx-border-color: #e9a8b2; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 10; -fx-text-fill: #c2414b; -fx-font-size: 12px; -fx-font-weight: 700;");

        VBox content = new VBox(10, reasonLabel, reasonField, inlineError);
        content.setStyle("-fx-background-color: #f7f9fd; -fx-padding: 16; -fx-border-color: #c6d3eb; -fx-border-radius: 12; -fx-background-radius: 12;");

        DialogPane pane = dialog.getDialogPane();
        styleDialog(pane);
        pane.setContent(content);
        styleDialogButtons(pane, confirmType, cancelType);

        Button confirmButton = (Button) pane.lookupButton(confirmType);
        if (confirmButton != null) {
            confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    validateRefusalReasonInput(reasonField.getText());
                    clearInlineDialogError(inlineError);
                } catch (IllegalArgumentException exception) {
                    showInlineDialogError(inlineError, exception.getMessage());
                    event.consume();
                }
            });
        }

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != confirmType) {
            return Optional.empty();
        }
        try {
            return Optional.of(validateRefusalReasonInput(reasonField.getText()));
        } catch (IllegalArgumentException exception) {
            showWarning("Validation error", exception.getMessage());
            return Optional.empty();
        }
    }

    private String validateRefusalReasonInput(String rawInput) {
        String reason = toNull(rawInput);
        if (reason == null) {
            throw new IllegalArgumentException("Refusal reason is required.");
        }
        if (reason.length() > MAX_REFUSAL_REASON_LENGTH) {
            throw new IllegalArgumentException("Refusal reason is too long (max " + MAX_REFUSAL_REASON_LENGTH + " characters).");
        }
        return reason;
    }

    private void sendStatusEmailToStudentAsync(RendezVous rdv, AvailabilitySlot slot) {
        if (rdv == null || rdv.getStudentId() == null) {
            return;
        }
        if (!isConfirmedStatus(rdv.getStatut()) && !isRefusedStatus(rdv.getStatut())) {
            return;
        }

        String studentEmail = resolveUserEmail(rdv.getStudentId());
        if (studentEmail == null) {
            showWarning("Email notification", "Student email not found. No email was sent.");
            return;
        }

        String professorName = resolveProfessorName(rdv.getProfessorId());
        String meetingDateTime = resolveRequestedMeetingDateTime(rdv, slot);
        String meetingLocation = resolveMeetingLocationForEmail(rdv, slot);
        String refusalReason = normalizeDefault(rdv.getRefusalReason(), "No reason provided.");
        boolean confirmed = isConfirmedStatus(rdv.getStatut());

        Thread worker = new Thread(() -> {
            try {
                if (confirmed) {
                    rendezVousEmailService.sendAppointmentConfirmedEmail(
                            studentEmail,
                            professorName,
                            meetingDateTime,
                            meetingLocation
                    );
                } else {
                    rendezVousEmailService.sendAppointmentDeclinedEmail(
                            studentEmail,
                            professorName,
                            meetingDateTime,
                            refusalReason
                    );
                }
            } catch (Exception exception) {
                Platform.runLater(() -> showWarning(
                        "Email notification",
                        "Rendez-vous updated, but email could not be sent: " + exception.getMessage()
                ));
            }
        }, "rdv-email-notification");
        worker.setDaemon(true);
        worker.start();
    }

    private String resolveRequestedMeetingDateTime(RendezVous rdv, AvailabilitySlot slot) {
        if (slot != null && (slot.getStartAt() != null || slot.getEndAt() != null)) {
            String start = formatDateTime(slot.getStartAt());
            String end = formatDateTime(slot.getEndAt());
            if (!"-".equals(start) && !"-".equals(end)) {
                return start + " -> " + end;
            }
            if (!"-".equals(start)) {
                return start;
            }
            if (!"-".equals(end)) {
                return end;
            }
        }
        if (rdv != null && rdv.getCreatedAt() != null) {
            return formatDateTime(rdv.getCreatedAt());
        }
        return "-";
    }

    private String resolveMeetingLocationForEmail(RendezVous rdv, AvailabilitySlot slot) {
        if (rdv == null) {
            return "-";
        }
        if (!isInPersonMeetingType(rdv.getMeetingType())) {
            String link = toNull(rdv.getMeetingLink());
            return link == null ? "Online meeting (link not provided)." : link;
        }

        String label = toNull(rdv.getLocationLabel());
        if (label != null) {
            return label;
        }
        String location = toNull(rdv.getLocation());
        if (location != null) {
            return location;
        }
        if (slot != null) {
            String slotLabel = toNull(slot.getLocationLabel());
            if (slotLabel != null) {
                return slotLabel;
            }
            String coordinates = formatCoordinates(slot.getLocationLat(), slot.getLocationLng());
            if (!"-".equals(coordinates)) {
                return coordinates;
            }
        }
        return "In-person meeting (location not specified).";
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

    private String selectionTargetLabel() {
        return cardsContainer instanceof VBox ? "row" : "card";
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

    private boolean isCancelledStatus(String rawStatus) {
        String value = normalizeDefault(rawStatus, "").toLowerCase();
        return value.contains("annul") || value.contains("cancel");
    }

    private boolean isReservedStatus(String rawStatus) {
        return !isRefusedStatus(rawStatus) && !isCancelledStatus(rawStatus);
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
        if (s.contains("annul") || s.contains("cancel")) {
            return "Annulé";
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

    private boolean isInPersonMeetingType(String rawType) {
        return normalizeDefault(rawType, "").toLowerCase().contains("personne");
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
        if (notificationButton == null) {
            return;
        }
        if (notificationsDropdown != null && notificationsDropdown.isShowing()) {
            notificationsDropdown.hide();
            return;
        }
        try {
            List<Notification> unread = notificationService.findUnreadByUser(getCurrentUserId(), 20);
            notificationsDropdown = buildNotificationsDropdown(unread);
            notificationsDropdown.show(notificationButton, Side.BOTTOM, 0, 6);
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
            int unread = notificationService.countUnreadByUser(getCurrentUserId());
            notificationButton.setText("🔔 " + unread);
        } catch (SQLException exception) {
            notificationButton.setText("🔔 !");
        }
    }

    private ContextMenu buildNotificationsDropdown(List<Notification> unread) {
        ContextMenu menu = new ContextMenu();

        int unreadCount = unread == null ? 0 : unread.size();
        VBox root = new VBox(10);
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f7fbff); -fx-border-color: #c8daf7; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 14; -fx-effect: dropshadow(gaussian, rgba(20,44,90,0.22), 16, 0.25, 0, 4);");

        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #1d2f57; -fx-font-size: 22px; -fx-font-weight: 900;");
        Label count = new Label(unreadCount + " non lue(s)");
        count.setStyle("-fx-text-fill: #5b6f96; -fx-font-size: 13px; -fx-font-weight: 700;");

        Button markAllBtn = new Button("Tout marquer lu");
        markAllBtn.setDisable(unreadCount == 0);
        markAllBtn.setStyle("-fx-background-color: #e7efff; -fx-text-fill: #214fb1; -fx-font-size: 12px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 7 14;");
        markAllBtn.setOnAction(event -> {
            try {
                notificationService.markAllAsReadForUser(getCurrentUserId());
                menu.hide();
                updateNotificationBadge();
            } catch (SQLException exception) {
                showError("Unable to mark notifications as read", exception);
            }
        });

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox header = new HBox(10, title, headerSpacer, markAllBtn);
        header.setStyle("-fx-alignment: center-left;");

        VBox center = new VBox(8);
        if (unread == null || unread.isEmpty()) {
            Label empty = new Label("Aucune notification non lue.");
            empty.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 13px; -fx-font-weight: 600;");
            center.getChildren().add(empty);
        } else {
            int previewCount = Math.min(unread.size(), 4);
            for (int i = 0; i < previewCount; i++) {
                center.getChildren().add(buildNotificationDropdownItem(unread.get(i), menu));
            }
        }

        Button viewAllBtn = new Button("Voir toutes les notifications");
        viewAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #214fb1; -fx-font-size: 14px; -fx-font-weight: 800;");
        viewAllBtn.setOnAction(event -> {
            menu.hide();
            showAllNotificationsDialog();
        });

        root.getChildren().addAll(header, count, center, viewAllBtn);

        CustomMenuItem contentItem = new CustomMenuItem(root, false);
        contentItem.setHideOnClick(false);
        menu.getItems().setAll(contentItem);
        menu.setOnHidden(event -> updateNotificationBadge());
        return menu;
    }

    private HBox buildNotificationDropdownItem(Notification notification, ContextMenu menu) {
        HBox row = new HBox(10);
        boolean unread = !Boolean.TRUE.equals(notification.getIsRead());
        String rowBackground = unread ? "#eef4ff" : "#ffffff";
        row.setStyle("-fx-background-color: " + rowBackground + "; -fx-border-color: #d6e2f8; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 12;");

        Label icon = new Label("🔔");
        icon.setStyle("-fx-background-color: #dbe7ff; -fx-text-fill: #204cad; -fx-font-size: 13px; -fx-padding: 6; -fx-background-radius: 14;");

        VBox textBox = new VBox(3);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);
        Label title = new Label(normalizeDefault(notification.getTitle(), "Notification"));
        title.setStyle("-fx-text-fill: #1d2f57; -fx-font-size: 13px; -fx-font-weight: 900;");
        title.setWrapText(true);
        Label message = new Label(resolveNotificationMessage(notification));
        message.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 12px; -fx-font-weight: 600;");
        message.setWrapText(true);
        Label time = new Label(formatNotificationDate(notification.getCreatedAt()));
        time.setStyle("-fx-text-fill: #6e83aa; -fx-font-size: 11px;");
        textBox.getChildren().addAll(title, message, time);

        Button markReadBtn = new Button("Lu");
        markReadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #138a5a; -fx-font-size: 12px; -fx-font-weight: 800;");
        markReadBtn.setManaged(unread);
        markReadBtn.setVisible(unread);
        markReadBtn.setOnAction(event -> {
            markNotificationAsReadSimple(notification);
            markReadBtn.setManaged(false);
            markReadBtn.setVisible(false);
            updateNotificationBadge();
        });

        row.setOnMouseClicked(event -> {
            showNotificationDetailsDialog(notification);
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                markNotificationAsReadSimple(notification);
            }
            menu.hide();
        });

        row.getChildren().addAll(icon, textBox, markReadBtn);
        return row;
    }

    private void markNotificationAsReadSimple(Notification notification) {
        if (notification == null || notification.getId() == null) {
            return;
        }
        try {
            notificationService.markAsRead(notification.getId());
            notification.setIsRead(true);
        } catch (SQLException exception) {
            showError("Unable to update notification", exception);
        }
    }

    private String resolveNotificationMessage(Notification notification) {
        if (notification == null) {
            return "-";
        }
        RendezVous rdv = resolveRendezVousFromNotification(notification);
        if (rdv != null) {
            String title = normalizeDefault(notification.getTitle(), "").toLowerCase(Locale.ROOT);
            if (title.contains("nouvelle demande")) {
                return buildProfessorRequestNotificationMessage(rdv);
            }
            if (title.contains("mise à jour")) {
                return buildStudentStatusNotificationMessage(rdv);
            }
            return rewriteNotificationMessageWithNames(normalizeDefault(notification.getMessage(), "-"), rdv);
        }
        return cleanupNotificationMessage(normalizeDefault(notification.getMessage(), "-"));
    }

    private RendezVous resolveRendezVousFromNotification(Notification notification) {
        if (notification == null) {
            return null;
        }
        Integer rdvId = extractRendezVousIdFromLink(notification.getLink());
        if (rdvId == null) {
            return null;
        }
        if (notificationRendezVousCache.containsKey(rdvId)) {
            return notificationRendezVousCache.get(rdvId);
        }
        RendezVous rdv = null;
        try {
            rdv = rendezVousService.getById(rdvId);
        } catch (SQLException ignored) {
            // Keep null when rendez-vous lookup fails.
        }
        notificationRendezVousCache.put(rdvId, rdv);
        return rdv;
    }

    private Integer extractRendezVousIdFromLink(String link) {
        String value = toNull(link);
        if (value == null) {
            return null;
        }
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex < 0 || slashIndex >= value.length() - 1) {
            return null;
        }
        String idPart = value.substring(slashIndex + 1).trim();
        if (idPart.contains("?")) {
            idPart = idPart.substring(0, idPart.indexOf('?')).trim();
        }
        try {
            return Integer.parseInt(idPart);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private AvailabilitySlot resolveSlotForNotification(Integer slotId) {
        if (slotId == null) {
            return null;
        }
        try {
            return availabilitySlotService.getById(slotId);
        } catch (SQLException ignored) {
            return null;
        }
    }

    private String buildProfessorRequestNotificationMessage(RendezVous rdv) {
        if (rdv == null) {
            return "Nouvelle demande de rendez-vous.";
        }
        String studentName = resolveStudentNameForNotification(rdv.getStudentId());
        String courseName = resolveCourseNameForNotification(rdv.getCourseId());
        AvailabilitySlot slot = resolveSlotForNotification(rdv.getSlotId());
        String requestedAt = resolveRequestedMeetingDateTime(rdv, slot);
        String coursePart = "un cours".equalsIgnoreCase(courseName) ? courseName : ("\"" + courseName + "\"");
        return "Nouvelle demande de " + studentName + " pour " + coursePart + " le " + requestedAt + ".";
    }

    private String buildStudentStatusNotificationMessage(RendezVous rdv) {
        if (rdv == null) {
            return "Mise à jour de votre rendez-vous.";
        }
        AvailabilitySlot slot = resolveSlotForNotification(rdv.getSlotId());
        String professorName = resolveProfessorNameForNotification(rdv.getProfessorId());
        String requestedAt = resolveRequestedMeetingDateTime(rdv, slot);
        String status = statusDisplay(rdv.getStatut()).toLowerCase(Locale.ROOT);

        StringBuilder message = new StringBuilder("Votre rendez-vous avec ")
                .append(professorName)
                .append(" prévu le ")
                .append(requestedAt)
                .append(" est ")
                .append(status)
                .append(".");

        if (isConfirmedStatus(rdv.getStatut())) {
            String location = resolveMeetingLocationForEmail(rdv, slot);
            message.append(" Détails: ").append(location).append(".");
        } else if (isRefusedStatus(rdv.getStatut())) {
            String reason = toNull(rdv.getRefusalReason());
            if (reason != null) {
                message.append(" Motif: ").append(reason).append(".");
            }
        }

        return message.toString();
    }

    private void createProfessorNotification(RendezVous rdv) {
        if (rdv.getProfessorId() == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(rdv.getProfessorId());
        notification.setTitle("Nouvelle demande de rendez-vous");
        notification.setMessage(buildProfessorRequestNotificationMessage(rdv));
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
        Notification notification = new Notification();
        notification.setUserId(rdv.getStudentId());
        notification.setTitle("Mise à jour rendez-vous");
        notification.setMessage(buildStudentStatusNotificationMessage(rdv));
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

        int unreadCount = unread == null ? 0 : unread.size();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d2dff3; -fx-border-width: 1; -fx-border-radius: 16; -fx-background-radius: 16;");
        root.setPrefWidth(360);
        root.setMaxWidth(360);

        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #1f2a44; -fx-font-size: 24px; -fx-font-weight: 800;");
        Label count = new Label(unreadCount + " non lue(s)");
        count.setStyle("-fx-text-fill: #ff4d6d; -fx-font-size: 14px; -fx-font-weight: 700;");
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, javafx.scene.layout.Priority.ALWAYS);
        HBox titleWrap = new HBox(10, title, titleSpacer, count);
        titleWrap.setStyle("-fx-alignment: center-left; -fx-padding: 16 16 12 16;");

        Region topDivider = new Region();
        topDivider.setStyle("-fx-border-color: #d2dff3; -fx-border-width: 1 0 0 0; -fx-min-height: 1;");

        VBox center = new VBox(10);
        center.setStyle("-fx-padding: 12 16 14 16;");
        if (unread == null || unread.isEmpty()) {
            Label empty = new Label("Aucune notification non lue.");
            empty.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 14px; -fx-font-weight: 500;");
            center.getChildren().add(empty);
        } else {
            center.getChildren().add(buildUnreadPreviewCard(unread.get(0)));
        }

        Region bottomDivider = new Region();
        bottomDivider.setStyle("-fx-border-color: #d2dff3; -fx-border-width: 1 0 0 0; -fx-min-height: 1;");

        Button footerAction = new Button("Voir toutes les notifications");
        footerAction.setStyle("-fx-background-color: transparent; -fx-text-fill: #264fb2; -fx-font-size: 15px; -fx-font-weight: 700;");
        footerAction.setOnAction(event -> {
            openAll.set(true);
            dialog.setResult(closeType);
        });
        VBox footerWrap = new VBox(footerAction);
        footerWrap.setStyle("-fx-padding: 12 16 14 16; -fx-alignment: center;");

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

    private HBox buildUnreadPreviewCard(Notification notification) {
        HBox row = new HBox(12);
        row.setStyle("-fx-background-color: #eef4ff; -fx-border-color: #d6e2f8; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12;");

        Label icon = new Label("🔔");
        icon.setStyle("-fx-background-color: #dbe7ff; -fx-text-fill: #214fb1; -fx-font-size: 15px; -fx-padding: 8; -fx-background-radius: 18;");

        VBox textBox = new VBox(4);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Label title = new Label(normalizeDefault(notification.getTitle(), "Notification"));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #1d2f57; -fx-font-size: 16px; -fx-font-weight: 900;");

        Label message = new Label(resolveNotificationMessage(notification));
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label time = new Label(formatNotificationDate(notification.getCreatedAt()));
        time.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 13px;");

        textBox.getChildren().addAll(title, message, time);
        row.getChildren().addAll(icon, textBox);
        return row;
    }

    private void showAllNotificationsDialog() {
        List<Notification> notifications;
        try {
            notifications = notificationService.findByUser(getCurrentUserId(), 200);
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
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #fafdff, #f5f9ff); -fx-border-color: #c8daf7; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 18; -fx-effect: dropshadow(gaussian, rgba(20,44,90,0.22), 18, 0.25, 0, 4);");
        root.setPrefWidth(1020);
        root.setMaxWidth(1020);
        root.setPrefHeight(640);

        HBox header = new HBox(12);
        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #1d2f57; -fx-font-size: 30px; -fx-font-weight: 900;");
        Button markAllBtn = new Button("Tout marquer comme lu");
        markAllBtn.setStyle("-fx-background-color: #214fb1; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 800; -fx-background-radius: 999; -fx-padding: 8 16;");
        markAllBtn.setDisable(unreadCount == 0);
        markAllBtn.setOnAction(event -> {
            try {
                notificationService.markAllAsReadForUser(getCurrentUserId());
                shouldReload.set(true);
                dialog.setResult(closeType);
            } catch (SQLException exception) {
                showError("Unable to mark notifications as read", exception);
            }
        });
        header.getChildren().addAll(title, markAllBtn);

        Label unreadLabel = new Label(unreadCount + " non lu(s)");
        unreadLabel.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 14px; -fx-font-weight: 600;");

        VBox rows = new VBox(0);
        rows.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d2dff3; -fx-border-width: 1; -fx-border-radius: 14; -fx-background-radius: 14;");

        if (notifications.isEmpty()) {
            Label empty = new Label("Aucune notification.");
            empty.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 16px; -fx-padding: 20;");
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
        String background = Boolean.TRUE.equals(notification.getIsRead()) ? "#ffffff" : "#eef4ff";
        row.setStyle("-fx-alignment: center-left; -fx-padding: 14 16; -fx-border-color: #d2dff3; -fx-border-width: " + border + "; -fx-background-color: " + background + ";");

        Label icon = new Label("🔔");
        icon.setStyle("-fx-background-color: #dbe7ff; -fx-text-fill: #214fb1; -fx-font-size: 14px; -fx-padding: 8; -fx-background-radius: 18;");

        VBox textBox = new VBox(4);
        textBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);

        Label title = new Label(normalizeDefault(notification.getTitle(), "Notification"));
        title.setStyle("-fx-text-fill: #1d2f57; -fx-font-size: 19px; -fx-font-weight: 900;");
        Label message = new Label(resolveNotificationMessage(notification));
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #4e6491; -fx-font-size: 14px; -fx-font-weight: 600;");
        Label time = new Label(formatNotificationDate(notification.getCreatedAt()));
        time.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 12px;");
        textBox.getChildren().addAll(title, message, time);

        HBox actions = new HBox(8);
        Button openBtn = new Button("Ouvrir");
        openBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #214fb1; -fx-font-size: 13px; -fx-font-weight: 800;");
        openBtn.setOnAction(event -> showNotificationDetailsDialog(notification));
        actions.getChildren().add(openBtn);

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            Button markReadBtn = new Button("Marquer lu");
            markReadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #138a5a; -fx-font-size: 13px; -fx-font-weight: 800;");
            markReadBtn.setOnAction(event -> markNotificationAsRead(notification, dialog, shouldReload, closeType));
            actions.getChildren().add(markReadBtn);
        }

        row.getChildren().addAll(icon, textBox, actions);
        return row;
    }

    private void showNotificationDetailsDialog(Notification notification) {
        if (notification == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Notification");
        ButtonType closeType = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(closeType);

        VBox root = new VBox(14);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f9fbff, #f3f8ff); -fx-border-color: #c8daf7; -fx-border-width: 1.2; -fx-border-radius: 16; -fx-background-radius: 16; -fx-padding: 18; -fx-effect: dropshadow(gaussian, rgba(20,44,90,0.2), 16, 0.25, 0, 4);");
        root.setPrefWidth(700);
        root.setMaxWidth(700);

        Label title = new Label(normalizeDefault(notification.getTitle(), "Notification"));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #1d2f57; -fx-font-size: 25px; -fx-font-weight: 900;");

        Label message = new Label(resolveNotificationMessage(notification));
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #334f80; -fx-font-size: 15px; -fx-font-weight: 600;");

        Label time = new Label("Reçue le " + formatNotificationDate(notification.getCreatedAt()));
        time.setStyle("-fx-text-fill: #6f84aa; -fx-font-size: 13px; -fx-font-weight: 600;");

        VBox details = new VBox(6);
        details.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d6e2f8; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10 12;");
        RendezVous rdv = resolveRendezVousFromNotification(notification);
        if (rdv != null) {
            AvailabilitySlot slot = resolveSlotForNotification(rdv.getSlotId());

            Label professorLine = new Label("Professeur: " + resolveProfessorNameForNotification(rdv.getProfessorId()));
            Label studentLine = new Label("Étudiant: " + resolveStudentNameForNotification(rdv.getStudentId()));
            Label courseLine = new Label("Cours: " + resolveCourseNameForNotification(rdv.getCourseId()));
            Label dateLine = new Label("Date: " + resolveRequestedMeetingDateTime(rdv, slot));

            professorLine.setStyle("-fx-text-fill: #29467f; -fx-font-size: 13px; -fx-font-weight: 700;");
            studentLine.setStyle("-fx-text-fill: #29467f; -fx-font-size: 13px; -fx-font-weight: 700;");
            courseLine.setStyle("-fx-text-fill: #29467f; -fx-font-size: 13px; -fx-font-weight: 700;");
            dateLine.setStyle("-fx-text-fill: #29467f; -fx-font-size: 13px; -fx-font-weight: 700;");
            details.getChildren().addAll(professorLine, studentLine, courseLine, dateLine);
        } else {
            Label fallback = new Label("Informations détaillées non disponibles.");
            fallback.setStyle("-fx-text-fill: #5d7198; -fx-font-size: 13px; -fx-font-weight: 600;");
            details.getChildren().add(fallback);
        }

        root.getChildren().addAll(title, message, time, details);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle("-fx-background-color: transparent;");
        pane.setContent(root);

        Button closeButton = (Button) pane.lookupButton(closeType);
        if (closeButton != null) {
            closeButton.setStyle("-fx-background-color: #214fb1; -fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 8 16;");
        }

        dialog.showAndWait();
    }

    private String resolveProfessorNameForNotification(Integer professorId) {
        String resolved = toNull(resolveProfessorName(professorId));
        if (resolved == null || resolved.equalsIgnoreCase("Professeur inconnu") || resolved.matches("(?i)^professeur\\s+\\d+$")) {
            return "le professeur";
        }
        return resolved;
    }

    private String resolveStudentNameForNotification(Integer studentId) {
        String resolved = toNull(resolveStudentName(studentId));
        if (resolved == null || resolved.equalsIgnoreCase("Etudiant inconnu")
                || resolved.equalsIgnoreCase("Étudiant inconnu")
                || resolved.matches("(?i)^etudiant\\s+\\d+$")
                || resolved.matches("(?i)^étudiant\\s+\\d+$")) {
            return "un étudiant";
        }
        return resolved;
    }

    private String resolveCourseNameForNotification(Integer courseId) {
        String resolved = toNull(resolveCourseName(courseId));
        if (resolved == null || resolved.equals("-") || resolved.matches("(?i)^cours\\s+\\d+$")) {
            return "un cours";
        }
        return resolved;
    }

    private String rewriteNotificationMessageWithNames(String rawMessage, RendezVous rdv) {
        String message = normalizeDefault(rawMessage, "-");
        if (rdv == null) {
            return cleanupNotificationMessage(message);
        }

        String studentName = resolveStudentNameForNotification(rdv.getStudentId());
        String professorName = resolveProfessorNameForNotification(rdv.getProfessorId());
        String courseName = resolveCourseNameForNotification(rdv.getCourseId());

        message = message
                .replaceAll("(?i)\\b(etudiant|étudiant|student)\\s*#?\\s*\\d+\\b", studentName)
                .replaceAll("(?i)\\b(professeur|professor|teacher)\\s*#?\\s*\\d+\\b", professorName)
                .replaceAll("(?i)\\b(cours|course)\\s*#?\\s*\\d+\\b", courseName);

        return cleanupNotificationMessage(message);
    }

    private String cleanupNotificationMessage(String message) {
        String cleaned = normalizeDefault(message, "-")
                .replaceAll("#\\s*\\d+", "")
                .replaceAll("\\s{2,}", " ")
                .replace(" .", ".")
                .trim();

        if (cleaned.isEmpty()) {
            return "-";
        }
        return cleaned;
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
        String normalized = raw.trim().toLowerCase().replace("role_", "");
        if (normalized.contains("admin")) {
            return "admin";
        }
        if (normalized.contains("prof") || normalized.contains("teacher") || normalized.contains("instructor")) {
            return "professor";
        }
        return "student";
    }

    private int getCurrentUserId() {
        String raw = System.getProperty("skillora.userId");
        if (raw != null) {
            try {
                int parsed = Integer.parseInt(raw.trim());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Fallback to layout-backed session.
            }
        }

        Integer fromLayout = resolveUserIdFromLayoutSession();
        if (fromLayout != null && fromLayout > 0) {
            System.setProperty("skillora.userId", String.valueOf(fromLayout));
            return fromLayout;
        }

        return parseCurrentUserId();
    }

    private String getCurrentUserRole() {
        String raw = System.getProperty("skillora.role");
        if (raw != null && !raw.trim().isEmpty()) {
            return normalizeRole(raw);
        }

        Integer userId = resolveUserIdFromLayoutSession();
        if (userId != null && userId > 0) {
            try {
                String resolved = normalizeRole(new UserService().getUserRole(userId));
                System.setProperty("skillora.role", resolved);
                return resolved;
            } catch (SQLException ignored) {
                // Keep fallback below.
            }
        }

        return "student";
    }

    private Integer resolveUserIdFromLayoutSession() {
        User adminUser = AdminPanelController.getCurrentUser();
        if (adminUser != null && adminUser.getId() != null) {
            return adminUser.getId();
        }
        User studentUser = StudentLayoutController.getCurrentUser();
        if (studentUser != null && studentUser.getId() != null) {
            return studentUser.getId();
        }
        return null;
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
