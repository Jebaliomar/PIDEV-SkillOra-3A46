package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class StatisticsController implements Initializable {

    @FXML private Label statTotal;
    @FXML private Label statActive;
    @FXML private Label statActivePercent;
    @FXML private Label statNew;
    @FXML private Label statEngagement;
    @FXML private AreaChart<String, Number> growthAreaChart;
    @FXML private PieChart statusPie;
    @FXML private Label activeRateLabel;
    @FXML private Label growthRateLabel;
    @FXML private Label avgSignupsLabel;
    @FXML private Label banRateLabel;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStats();
        loadGrowthChart();
        loadStatusPie();
    }

    private void loadStats() {
        try {
            Map<String, Integer> stats = userService.getUserStats();
            int total = stats.get("total");
            int active = stats.get("active");
            int banned = stats.get("banned");
            int newWeek = stats.get("new_this_week");

            statTotal.setText(String.valueOf(total));
            statActive.setText(String.valueOf(active));
            statNew.setText(String.valueOf(newWeek));

            if (total > 0) {
                double activeRate = (active * 100.0 / total);
                statActivePercent.setText(String.format("%.1f%% activity rate", activeRate));
                activeRateLabel.setText(String.format("%.0f%% Active", activeRate));
                banRateLabel.setText(String.format("%.1f%%", banned * 100.0 / total));
            }

            double avgDaily = newWeek / 7.0;
            avgSignupsLabel.setText(String.format("%.1f", avgDaily));
            growthRateLabel.setText("+24%");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadGrowthChart() {
        try {
            List<Map<String, Object>> growth = userService.getUserGrowth(30);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("New Users");

            for (Map<String, Object> day : growth) {
                series.getData().add(new XYChart.Data<>((String) day.get("date"), (Number) day.get("count")));
            }

            growthAreaChart.getData().clear();
            growthAreaChart.getData().add(series);
            growthAreaChart.setLegendVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadStatusPie() {
        try {
            Map<String, Integer> stats = userService.getUserStats();
            statusPie.getData().clear();
            statusPie.getData().add(new PieChart.Data("Active (" + stats.get("active") + ")", stats.get("active")));
            statusPie.getData().add(new PieChart.Data("Banned (" + stats.get("banned") + ")", stats.get("banned")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Statistics");
        chooser.setInitialFileName("skillora_stats_" + LocalDate.now() + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        Stage stage = (Stage) statTotal.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // BOM for Excel
            pw.print("\uFEFF");

            // Header
            pw.println("SkillORA Platform Statistics Report");
            pw.println("Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            pw.println();

            // Stats
            Map<String, Integer> stats = userService.getUserStats();
            pw.println("Overall Statistics");
            pw.println("Metric,Value");
            pw.println("Total Users," + stats.get("total"));
            pw.println("Active Users," + stats.get("active"));
            pw.println("Banned Users," + stats.get("banned"));
            pw.println("New This Week," + stats.get("new_this_week"));
            if (stats.get("total") > 0) {
                pw.println("Activity Rate," + String.format("%.2f%%", stats.get("active") * 100.0 / stats.get("total")));
            }
            pw.println();

            // Growth
            pw.println("User Growth - Last 30 Days");
            pw.println("Date,New Users");
            List<Map<String, Object>> growth = userService.getUserGrowth(30);
            for (Map<String, Object> day : growth) {
                pw.println(day.get("date") + "," + day.get("count"));
            }
            pw.println();

            // User list
            pw.println("Complete User List");
            pw.println("ID,Email,First Name,Last Name,Role,Status,Joined Date");
            List<Map<String, Object>> users = userService.getAllWithRoles();
            for (Map<String, Object> row : users) {
                User u = (User) row.get("user");
                String role = (String) row.get("role");
                pw.printf("%d,%s,%s,%s,%s,%s,%s%n",
                        u.getId(), u.getEmail(),
                        u.getFirstName() != null ? u.getFirstName() : "",
                        u.getLastName() != null ? u.getLastName() : "",
                        role,
                        u.getIsActive() == 1 ? "Active" : "Banned",
                        u.getCreatedAt() != null ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
            }

            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Export Successful");
            alert.setHeaderText(null);
            alert.setContentText("Statistics exported to " + file.getName());
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
