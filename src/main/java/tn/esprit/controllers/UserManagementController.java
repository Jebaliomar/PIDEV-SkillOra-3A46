package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.entities.User;
import tn.esprit.services.UserService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class UserManagementController implements Initializable {

    @FXML private Label totalLabel;
    @FXML private Label adminsLabel;
    @FXML private Label studentsLabel;
    @FXML private Label bannedLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private FlowPane userCardsPane;
    @FXML private Button filterAll;
    @FXML private Button filterActive;
    @FXML private Button filterBanned;

    private final UserService userService = new UserService();
    private List<Map<String, Object>> allUsers = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        roleFilter.setItems(FXCollections.observableArrayList("All Roles", "admin", "professor", "student"));
        roleFilter.setValue("All Roles");
        roleFilter.setOnAction(e -> applyFilters());
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        loadData();
    }

    private void loadData() {
        try {
            allUsers = userService.getAllWithRoles();
            updateStatBars();
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateStatBars() {
        int total = allUsers.size();
        int admins = 0, students = 0, banned = 0;
        for (Map<String, Object> row : allUsers) {
            User u = (User) row.get("user");
            String role = (String) row.get("role");
            if ("admin".equals(role)) admins++;
            if ("student".equals(role)) students++;
            if (u.getIsActive() == 0) banned++;
        }
        totalLabel.setText(String.valueOf(total));
        adminsLabel.setText(String.valueOf(admins));
        studentsLabel.setText(String.valueOf(students));
        bannedLabel.setText(String.valueOf(banned));
    }

    private void applyFilters() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String roleVal = roleFilter.getValue();

        List<Map<String, Object>> filtered = allUsers.stream().filter(row -> {
            User u = (User) row.get("user");
            String role = (String) row.get("role");

            // Search filter
            if (!search.isEmpty()) {
                String name = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).toLowerCase();
                String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                if (!name.contains(search) && !email.contains(search)) return false;
            }

            // Role filter
            if (roleVal != null && !"All Roles".equals(roleVal)) {
                if (!roleVal.equals(role)) return false;
            }

            // Status filter
            if ("active".equals(currentFilter) && u.getIsActive() == 0) return false;
            if ("banned".equals(currentFilter) && u.getIsActive() != 0) return false;

            return true;
        }).collect(Collectors.toList());

        renderCards(filtered);
    }

    private void renderCards(List<Map<String, Object>> users) {
        userCardsPane.getChildren().clear();
        for (Map<String, Object> row : users) {
            User user = (User) row.get("user");
            String role = (String) row.get("role");
            userCardsPane.getChildren().add(createUserCard(user, role));
        }
    }

    private VBox createUserCard(User user, String role) {
        VBox card = new VBox(0);
        card.getStyleClass().add("user-card");
        card.setPrefWidth(280);

        // Header with gradient
        StackPane header = new StackPane();
        header.getStyleClass().add("user-card-header");
        header.getStyleClass().add("card-header-" + role);
        header.setPrefHeight(60);

        // Avatar
        String initial = user.getFirstName() != null ? user.getFirstName().substring(0, 1).toUpperCase() : "?";
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-card-avatar");
        avatar.getStyleClass().add("avatar-" + role);
        Label avatarLabel = new Label(initial);
        avatarLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 20px;");
        avatar.getChildren().add(avatarLabel);

        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 16, 16, 16));
        body.setAlignment(Pos.TOP_CENTER);

        // Overlay avatar between header and body
        VBox topSection = new VBox();
        topSection.setAlignment(Pos.CENTER);
        topSection.getChildren().addAll(header);

        // Name
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : "");
        Label nameLabel = new Label(fullName.trim());
        nameLabel.getStyleClass().add("card-user-name");
        nameLabel.setWrapText(true);

        // Email
        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("card-user-email");
        emailLabel.setWrapText(true);

        // Role badge
        Label roleBadge = new Label(role.substring(0, 1).toUpperCase() + role.substring(1));
        roleBadge.getStyleClass().addAll("role-badge", "badge-" + role);

        // Status badge
        boolean isActive = user.getIsActive() != null && user.getIsActive() == 1;
        Label statusBadge = new Label(isActive ? "Active" : "Banned");
        statusBadge.getStyleClass().addAll("status-badge", isActive ? "badge-active" : "badge-banned");

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER);
        badges.getChildren().addAll(roleBadge, statusBadge);

        // Dates
        String joinDate = user.getCreatedAt() != null
                ? user.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                : "N/A";
        String lastLogin = user.getLastLoginAt() != null
                ? user.getLastLoginAt().format(DateTimeFormatter.ofPattern("MMM dd"))
                : "Never";

        HBox dates = new HBox(16);
        dates.setAlignment(Pos.CENTER);
        VBox joinBox = new VBox(2);
        joinBox.setAlignment(Pos.CENTER);
        joinBox.getChildren().addAll(
                new Label("Joined") {{ getStyleClass().add("card-date-label"); }},
                new Label(joinDate) {{ getStyleClass().add("card-date-value"); }}
        );
        VBox loginBox = new VBox(2);
        loginBox.setAlignment(Pos.CENTER);
        loginBox.getChildren().addAll(
                new Label("Last Active") {{ getStyleClass().add("card-date-label"); }},
                new Label(lastLogin) {{ getStyleClass().add("card-date-value"); }}
        );
        dates.getChildren().addAll(joinBox, loginBox);

        // Action buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        // Change Role
        ComboBox<String> roleCombo = new ComboBox<>(FXCollections.observableArrayList("admin", "professor", "student"));
        roleCombo.setValue(role);
        roleCombo.setPrefHeight(32);
        roleCombo.setOnAction(e -> {
            String newRole = roleCombo.getValue();
            if (!newRole.equals(role)) {
                try {
                    userService.changeRole(user.getId(), newRole);
                    loadData();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Ban/Unban
        Button banBtn;
        if (isActive) {
            banBtn = new Button("Ban");
            banBtn.getStyleClass().add("btn-danger");
            banBtn.setOnAction(e -> {
                try { userService.banUser(user.getId()); loadData(); }
                catch (Exception ex) { ex.printStackTrace(); }
            });
        } else {
            banBtn = new Button("Unban");
            banBtn.getStyleClass().add("btn-success");
            banBtn.setOnAction(e -> {
                try { userService.unbanUser(user.getId()); loadData(); }
                catch (Exception ex) { ex.printStackTrace(); }
            });
        }

        // Delete
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger-outline");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete user " + user.getEmail() + "?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Confirm Deletion");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    try { userService.delete(user.getId()); loadData(); }
                    catch (Exception ex) { ex.printStackTrace(); }
                }
            });
        });

        actions.getChildren().addAll(roleCombo, banBtn, deleteBtn);

        body.getChildren().addAll(nameLabel, emailLabel, badges, dates, actions);
        card.getChildren().addAll(topSection, avatar, body);
        VBox.setMargin(avatar, new Insets(-28, 0, 0, 0));

        // Center avatar
        card.setAlignment(Pos.TOP_CENTER);

        return card;
    }

    @FXML
    public void filterAll() {
        currentFilter = "all";
        setActiveFilter(filterAll);
        applyFilters();
    }

    @FXML
    public void filterActive() {
        currentFilter = "active";
        setActiveFilter(filterActive);
        applyFilters();
    }

    @FXML
    public void filterBanned() {
        currentFilter = "banned";
        setActiveFilter(filterBanned);
        applyFilters();
    }

    private void setActiveFilter(Button active) {
        filterAll.getStyleClass().remove("filter-btn-active");
        filterActive.getStyleClass().remove("filter-btn-active");
        filterBanned.getStyleClass().remove("filter-btn-active");
        active.getStyleClass().add("filter-btn-active");
    }

    @FXML
    public void showCreateUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create User");
        dialog.setHeaderText("Create a new user account");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField firstName = new TextField();
        firstName.setPromptText("First Name");
        TextField lastName = new TextField();
        lastName.setPromptText("Last Name");
        TextField email = new TextField();
        email.setPromptText("Email");
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("student", "professor", "admin"));
        roleBox.setValue("student");

        grid.add(new Label("First Name:"), 0, 0); grid.add(firstName, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1); grid.add(lastName, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(email, 1, 2);
        grid.add(new Label("Password:"), 0, 3); grid.add(password, 1, 3);
        grid.add(new Label("Role:"), 0, 4); grid.add(roleBox, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    User newUser = new User();
                    newUser.setFirstName(firstName.getText().trim());
                    newUser.setLastName(lastName.getText().trim());
                    newUser.setEmail(email.getText().trim());
                    newUser.setPassword(password.getText());
                    userService.register(newUser, roleBox.getValue());
                    loadData();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }
}
