package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label activeUsersLabel;
    @FXML private Label activePercentLabel;
    @FXML private Label newUsersLabel;
    @FXML private Label bannedUsersLabel;
    @FXML private Label bannedPercentLabel;
    @FXML private LineChart<String, Number> growthChart;
    @FXML private PieChart distributionChart;
    @FXML private VBox recentUsersContainer;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStats();
        loadGrowthChart();
        loadDistributionChart();
        loadRecentUsers();

        User current = AdminPanelController.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Welcome back, " + current.getFirstName() + "!");
        }
    }

    private void loadStats() {
        try {
            Map<String, Integer> stats = userService.getUserStats();
            int total = stats.get("total");
            int active = stats.get("active");
            int banned = stats.get("banned");
            int newWeek = stats.get("new_this_week");

            totalUsersLabel.setText(String.valueOf(total));
            activeUsersLabel.setText(String.valueOf(active));
            newUsersLabel.setText(String.valueOf(newWeek));
            bannedUsersLabel.setText(String.valueOf(banned));

            if (total > 0) {
                activePercentLabel.setText(String.format("%.0f%% of total", (active * 100.0 / total)));
                bannedPercentLabel.setText(String.format("%.0f%% of total", (banned * 100.0 / total)));
            }
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

            growthChart.getData().clear();
            growthChart.getData().add(series);
            growthChart.setLegendVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDistributionChart() {
        try {
            Map<String, Integer> dist = userService.getUserDistribution();
            distributionChart.getData().clear();

            for (Map.Entry<String, Integer> entry : dist.entrySet()) {
                distributionChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentUsers() {
        try {
            List<Map<String, Object>> users = userService.getAllWithRoles();
            recentUsersContainer.getChildren().clear();

            int limit = Math.min(users.size(), 5);
            for (int i = 0; i < limit; i++) {
                User user = (User) users.get(i).get("user");
                String role = (String) users.get(i).get("role");
                recentUsersContainer.getChildren().add(createUserRow(user, role));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createUserRow(User user, String role) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("user-row");
        row.setPadding(new Insets(10, 16, 10, 16));

        // Avatar circle
        String initial = user.getFirstName() != null ? user.getFirstName().substring(0, 1).toUpperCase() : "?";
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-avatar");
        avatar.getStyleClass().add("avatar-" + role);
        Label avatarLabel = new Label(initial);
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        avatar.getChildren().add(avatarLabel);

        // Name + email
        VBox info = new VBox(2);
        Label nameLabel = new Label((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""));
        nameLabel.getStyleClass().add("user-row-name");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("user-row-email");
        info.getChildren().addAll(nameLabel, emailLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Date
        String dateStr = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd"))
                : "N/A";
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().add("user-row-date");

        row.getChildren().addAll(avatar, info, spacer, dateLabel);
        return row;
    }
}
