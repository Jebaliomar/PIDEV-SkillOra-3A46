package tn.esprit.controlles;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.AdminReservationRow;
import tn.esprit.services.ReservationDashboardService;
import tn.esprit.services.ReservationService;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javafx.util.Duration;

public class ReservationDashboardController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    @FXML
    private Button eventsNavButton;
    @FXML
    private Button sallesNavButton;
    @FXML
    private Button reservationsNavButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortCombo;
    @FXML
    private Label totalPlacesValueLabel;
    @FXML
    private Label totalPlacesSubtitleLabel;
    @FXML
    private VBox placesByEventBox;
    @FXML
    private PieChart shareByEventChart;
    @FXML
    private Label totalReservationsValueLabel;
    @FXML
    private Label totalReservationPlacesValueLabel;
    @FXML
    private Label totalRowsLabel;
    @FXML
    private Label deleteFlashLabel;
    @FXML
    private VBox reservationRowsContainer;

    private final ReservationDashboardService dashboardService = new ReservationDashboardService();
    private final ReservationService reservationService = new ReservationService();
    private List<AdminReservationRow> allRows = new ArrayList<>();
    private final PauseTransition deleteFlashTimer = new PauseTransition(Duration.seconds(2.8));

    @FXML
    public void initialize() {
        sortCombo.setItems(FXCollections.observableArrayList(
                "Places (High to Low)",
                "Places (Low to High)",
                "Date (Newest)",
                "Date (Oldest)",
                "Name (A-Z)",
                "Name (Z-A)"
        ));
        sortCombo.setValue("Places (High to Low)");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshView());
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> refreshView());

        loadData();
        setActiveNav(reservationsNavButton);
        ThemeManager.syncToggleButton(themeToggleButton);
    }

    @FXML
    private void onToggleTheme() {
        if (themeToggleButton.getScene() != null) {
            ThemeManager.toggleTheme(themeToggleButton.getScene());
            ThemeManager.syncToggleButton(themeToggleButton);
        }
    }

    @FXML
    private void onOpenEvents() {
        navigateTo("/viewsadmin/event/EventDashboard.fxml", "Skillora Events", "/styles/event.css", "/styles/salle.css");
    }

    @FXML
    private void onOpenSalles() {
        navigateTo("/viewsadmin/event/SalleDashboard.fxml", "Skillora Salles", "/styles/event.css", "/styles/salle.css");
    }

    @FXML
    private void onBackToSite() {
        navigateTo("/viewsadmin/event/SiteEventsView.fxml", "SkillHarbor Events", "/styles/site-events.css");
    }

    @FXML
    private void onExportTxt() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export reservations");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        chooser.setInitialFileName("reservations.txt");
        Stage owner = searchField.getScene() == null ? null : (Stage) searchField.getScene().getWindow();
        File target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }

        try {
            List<String> lines = new ArrayList<>();
            for (AdminReservationRow row : filteredRows()) {
                lines.add("#" + row.getReservationId()
                        + " | " + row.getEventTitle()
                        + " | " + row.getSalleName()
                        + " | " + row.getFirstName() + " " + row.getLastName()
                        + " | " + row.getPhone()
                        + " | seats: " + row.getSeats()
                        + " | " + row.getAddress()
                        + " | " + formatDate(row));
            }
            Files.write(target.toPath(), lines);
        } catch (Exception e) {
            showError("Export failed", e.getMessage());
        }
    }

    private void loadData() {
        try {
            allRows = dashboardService.loadRows();
            refreshView();
        } catch (SQLException e) {
            showError("Load failed", e.getMessage());
        }
    }

    private void refreshView() {
        List<AdminReservationRow> rows = filteredRows();
        renderStats(rows);
        renderPlacesByEvent(rows);
        renderChart(rows);
        renderRows(rows);
    }

    private List<AdminReservationRow> filteredRows() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<AdminReservationRow> rows = new ArrayList<>(allRows);

        if (!query.isEmpty()) {
            rows.removeIf(row -> !(safe(row.getFirstName()).toLowerCase().contains(query)
                    || safe(row.getLastName()).toLowerCase().contains(query)
                    || safe(row.getEventTitle()).toLowerCase().contains(query)
                    || safe(row.getSalleName()).toLowerCase().contains(query)));
        }

        sortRows(rows);
        return rows;
    }

    private void sortRows(List<AdminReservationRow> rows) {
        String selected = sortCombo.getValue();
        if (selected == null) {
            return;
        }

        switch (selected) {
            case "Places (Low to High)" -> rows.sort(Comparator.comparingInt(AdminReservationRow::getSeatsCount));
            case "Date (Newest)" -> rows.sort(Comparator.comparing(AdminReservationRow::getReservationDate,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            case "Date (Oldest)" -> rows.sort(Comparator.comparing(AdminReservationRow::getReservationDate,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            case "Name (A-Z)" -> rows.sort(Comparator.comparing(row -> (safe(row.getFirstName()) + " " + safe(row.getLastName())).toLowerCase()));
            case "Name (Z-A)" -> rows.sort(Comparator.comparing((AdminReservationRow row) ->
                    (safe(row.getFirstName()) + " " + safe(row.getLastName())).toLowerCase()).reversed());
            default -> rows.sort(Comparator.comparingInt(AdminReservationRow::getSeatsCount).reversed());
        }
    }

    private void renderStats(List<AdminReservationRow> rows) {
        int totalPlaces = dashboardService.totalPlaces(rows);
        totalPlacesValueLabel.setText(String.valueOf(totalPlaces));
        totalPlacesSubtitleLabel.setText("From " + rows.size() + " reservations");
        totalReservationsValueLabel.setText(String.valueOf(rows.size()));
        totalReservationPlacesValueLabel.setText(String.valueOf(totalPlaces));
        totalRowsLabel.setText("All Reservations (" + rows.size() + ")");
    }

    private void renderPlacesByEvent(List<AdminReservationRow> rows) {
        placesByEventBox.getChildren().clear();
        Map<String, Integer> stats = dashboardService.placesByEvent(rows);
        if (stats.isEmpty()) {
            Label empty = new Label("No event data yet.");
            empty.getStyleClass().add("reservation-card-copy");
            placesByEventBox.getChildren().add(empty);
            return;
        }

        stats.forEach((eventTitle, count) -> {
            HBox row = new HBox(10);
            row.getStyleClass().add("reservation-event-stat-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Label eventLabel = new Label(eventTitle);
            eventLabel.getStyleClass().add("reservation-event-stat-title");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label value = new Label(count + " places");
            value.getStyleClass().add("reservation-event-stat-value");
            row.getChildren().addAll(eventLabel, spacer, value);
            placesByEventBox.getChildren().add(row);
        });
    }

    private void renderChart(List<AdminReservationRow> rows) {
        Map<String, Integer> stats = dashboardService.placesByEvent(rows);
        shareByEventChart.setLegendVisible(true);
        shareByEventChart.setLabelsVisible(false);
        shareByEventChart.setClockwise(true);
        shareByEventChart.setData(FXCollections.observableArrayList());

        stats.forEach((eventTitle, count) ->
                shareByEventChart.getData().add(new PieChart.Data(eventTitle + " (" + count + ")", count)));
    }

    private void renderRows(List<AdminReservationRow> rows) {
        reservationRowsContainer.getChildren().clear();
        for (AdminReservationRow row : rows) {
            reservationRowsContainer.getChildren().add(createReservationRow(row));
        }

        if (rows.isEmpty()) {
            Label empty = new Label("No reservations found for the current filters.");
            empty.getStyleClass().add("reservation-empty-state");
            reservationRowsContainer.getChildren().add(empty);
        }
    }

    private VBox createReservationRow(AdminReservationRow row) {
        VBox wrap = new VBox(10);
        wrap.getStyleClass().add("reservation-row");
        wrap.setPadding(new Insets(14));

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("reservation-row-top");

        Label event = createCell(row.getEventTitle(), "reservation-col-event");
        Label salle = createCell(row.getSalleName(), "reservation-col-salle");
        Label name = createCell(row.getFirstName() + " " + row.getLastName(), "reservation-col-name");
        Label phone = createCell(row.getPhone(), "reservation-col-phone");
        Label seats = createChipLabel(row.getSeats(), "reservation-col-seats", "reservation-seats-chip");
        Label date = createCell(formatDate(row), "reservation-col-date");

        HBox actions = new HBox(6);
        actions.getStyleClass().add("reservation-actions");
        Button viewButton = new Button("View");
        viewButton.getStyleClass().addAll("btn-row", "btn-row-view", "reservation-action-button");
        viewButton.setOnAction(evt -> showDetails(row));

        Button editButton = new Button("Edit");
        editButton.getStyleClass().addAll("btn-row", "btn-row-edit", "reservation-action-button");
        editButton.setOnAction(evt -> openEditForm(row));

        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().addAll("btn-row", "btn-row-delete", "reservation-action-button");
        deleteButton.setOnAction(evt -> deleteReservation(row));
        actions.getChildren().addAll(viewButton, editButton, deleteButton);

        top.getChildren().addAll(event, salle, name, phone, seats, date, actions);

        HBox secondary = new HBox(18);
        secondary.setAlignment(Pos.CENTER_LEFT);
        secondary.getStyleClass().add("reservation-row-secondary");
        Label address = new Label("Address: " + row.getAddress());
        address.getStyleClass().add("reservation-row-subtext");
        Label status = createChipLabel("Status: " + safe(row.getStatus()), "reservation-row-subtext", "reservation-status-chip");
        status.getStyleClass().add(resolveStatusClass(row.getStatus()));
        secondary.getChildren().addAll(address, status);

        wrap.getChildren().addAll(top, secondary);
        return wrap;
    }

    private Label createCell(String value, String styleClass) {
        Label label = new Label(value);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private Label createChipLabel(String value, String textClass, String chipClass) {
        Label label = new Label(value);
        label.getStyleClass().addAll(textClass, chipClass);
        return label;
    }

    private String resolveStatusClass(String status) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        if (normalized.contains("confirm")) {
            return "reservation-status-confirmed";
        }
        if (normalized.contains("cancel")) {
            return "reservation-status-cancelled";
        }
        return "reservation-status-pending";
    }

    private void showDetails(AdminReservationRow row) {
        showInfo("Reservation #" + row.getReservationId(),
                "Event: " + row.getEventTitle()
                        + "\nSalle: " + row.getSalleName()
                        + "\nFirst Name: " + row.getFirstName()
                        + "\nLast Name: " + row.getLastName()
                        + "\nPhone: " + row.getPhone()
                        + "\nSeats: " + row.getSeats()
                        + "\nAddress: " + row.getAddress()
                        + "\nDate: " + formatDate(row)
                        + "\nStatus: " + row.getStatus());
    }

    private void deleteReservation(AdminReservationRow row) {
        try {
            boolean deleted = reservationService.delete(row.getReservationId());
            if (!deleted) {
                showError("Delete failed", "No reservation was deleted.");
                return;
            }
            loadData();
            showDeleteFlash("reservation deleted successfully.");
        } catch (SQLException e) {
            showError("Delete failed", e.getMessage());
        }
    }

    private void showDeleteFlash(String message) {
        if (deleteFlashLabel == null) {
            return;
        }
        deleteFlashTimer.stop();
        deleteFlashLabel.setText("✓  " + message);
        deleteFlashLabel.setVisible(true);
        deleteFlashLabel.setManaged(true);
        deleteFlashTimer.setOnFinished(evt -> {
            deleteFlashLabel.setVisible(false);
            deleteFlashLabel.setManaged(false);
        });
        deleteFlashTimer.playFromStart();
    }

    private void openEditForm(AdminReservationRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/viewsadmin/event/ReservationEditView.fxml"));
            Parent root = loader.load();
            ReservationEditController controller = loader.getController();
            controller.setReservationId(row.getReservationId());

            Scene scene = new Scene(root, 1440, 900);
            scene.getStylesheets().add(getClass().getResource("/styles/event.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/salle.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/styles/reservations.css").toExternalForm());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setTitle("Edit Reservation");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void navigateTo(String fxml, String title, String... stylesheets) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1440, 900);
            for (String stylesheet : stylesheets) {
                scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
            }
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation failed", e.getMessage());
        }
    }

    private void setActiveNav(Button activeButton) {
        eventsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        sallesNavButton.getStyleClass().remove("sidebar-nav-item-active");
        reservationsNavButton.getStyleClass().remove("sidebar-nav-item-active");
        if (!activeButton.getStyleClass().contains("sidebar-nav-item-active")) {
            activeButton.getStyleClass().add("sidebar-nav-item-active");
        }
    }

    private String formatDate(AdminReservationRow row) {
        return row.getReservationDate() == null ? "-" : DATE_FORMATTER.format(row.getReservationDate());
    }

    private String safe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private void showInfo(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Reservations");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content == null || content.isBlank() ? "Unknown error" : content);
        alert.showAndWait();
    }
}
