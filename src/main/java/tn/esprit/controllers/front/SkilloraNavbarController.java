package tn.esprit.controllers.front;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.StudentLayoutController;
import tn.esprit.entities.Notification;
import tn.esprit.entities.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.UserService;
import tn.esprit.tools.AppIcons;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AuthSession;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SkilloraNavbarController {

    public enum ActivePage {
        HOME,
        COURSES,
        MY_LEARNING,
        EVENTS,
        RENDEZ_VOUS,
        SLOTS,
        FORUM,
        ASSESSMENT,
        NONE
    }

    public interface Host {
        void showHome();

        void showCourses();

        void showMyLearning();

        void showEvents();

        void showRendezVous();

        void showSlots();

        void showForum();

        void showAssessment();

        void showProfile();

        void showSettings();

        void handleLogout();
    }

    @FXML
    private Button navHome;
    @FXML
    private Button navCourses;
    @FXML
    private Button navMyLearning;
    @FXML
    private Button navEvents;
    @FXML
    private Button navRendezVous;
    @FXML
    private Button navSlots;
    @FXML
    private Button navForum;
    @FXML
    private Button navAssessment;
    @FXML
    private Button notificationButton;
    @FXML
    private Button themeToggleBtn;
    @FXML
    private MenuButton userMenu;
    @FXML
    private MenuItem menuProfile;
    @FXML
    private MenuItem menuSettings;

    private static final DateTimeFormatter NOTIFICATION_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();
    private ContextMenu notificationsMenu;
    private Host host;
    private Runnable beforeNavigation;

    @FXML
    public void initialize() {
        configureIcons();
        updateNotificationBadge();
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public void setBeforeNavigation(Runnable beforeNavigation) {
        this.beforeNavigation = beforeNavigation;
    }

    public void setActivePage(ActivePage activePage) {
        clearActive(navHome);
        clearActive(navCourses);
        clearActive(navMyLearning);
        clearActive(navEvents);
        clearActive(navRendezVous);
        clearActive(navSlots);
        clearActive(navForum);
        clearActive(navAssessment);

        Button activeButton = switch (activePage == null ? ActivePage.NONE : activePage) {
            case HOME -> navHome;
            case COURSES -> navCourses;
            case MY_LEARNING -> navMyLearning;
            case EVENTS -> navEvents;
            case RENDEZ_VOUS -> navRendezVous;
            case SLOTS -> navSlots;
            case FORUM -> navForum;
            case ASSESSMENT -> navAssessment;
            case NONE -> null;
        };

        if (activeButton != null && !activeButton.getStyleClass().contains("student-nav-btn-active")) {
            activeButton.getStyleClass().add("student-nav-btn-active");
        }
    }

    @FXML
    public void showHome() {
        runBeforeNavigation();
        if (host != null) {
            host.showHome();
        } else {
            AppNavigator.showFrontHome(navigationSource());
        }
        setActivePage(ActivePage.HOME);
    }

    @FXML
    public void showCourses() {
        runBeforeNavigation();
        if (host != null) {
            host.showCourses();
        } else {
            AppNavigator.showFrontBrowseCourses(navigationSource());
        }
        setActivePage(ActivePage.COURSES);
    }

    @FXML
    public void showMyLearning() {
        runBeforeNavigation();
        if (host != null) {
            host.showMyLearning();
        } else {
            AppNavigator.showFrontMyLearning(navigationSource());
        }
        setActivePage(ActivePage.MY_LEARNING);
    }

    @FXML
    public void showEvents() {
        runBeforeNavigation();
        if (host != null) {
            host.showEvents();
        } else {
            AppNavigator.showFrontEvents(navigationSource());
        }
        setActivePage(ActivePage.EVENTS);
    }

    @FXML
    public void showRendezVous() {
        runBeforeNavigation();
        if (host != null) {
            host.showRendezVous();
        } else {
            AppNavigator.showFrontRendezVous(navigationSource());
        }
        setActivePage(ActivePage.RENDEZ_VOUS);
    }

    @FXML
    public void showSlots() {
        runBeforeNavigation();
        if (host != null) {
            host.showSlots();
        } else {
            AppNavigator.showFrontSlots(navigationSource());
        }
        setActivePage(ActivePage.SLOTS);
    }

    @FXML
    public void showForum() {
        runBeforeNavigation();
        if (host != null) {
            host.showForum();
        } else {
            AppNavigator.showFrontForum(navigationSource());
        }
        setActivePage(ActivePage.FORUM);
    }

    @FXML
    public void showAssessment() {
        runBeforeNavigation();
        if (host != null) {
            host.showAssessment();
        } else {
            AppNavigator.showFrontAssessment(navigationSource());
        }
        setActivePage(ActivePage.ASSESSMENT);
    }

    @FXML
    public void showProfile() {
        runBeforeNavigation();
        if (host != null) {
            host.showProfile();
        } else {
            AppNavigator.showFrontProfile(navigationSource());
        }
        setActivePage(ActivePage.NONE);
    }

    @FXML
    public void showSettings() {
        runBeforeNavigation();
        if (host != null) {
            host.showSettings();
        } else {
            AppNavigator.showFrontSettings(navigationSource());
        }
        setActivePage(ActivePage.NONE);
    }

    @FXML
    public void handleLogout() {
        runBeforeNavigation();
        if (host != null) {
            host.handleLogout();
        } else {
            AppNavigator.showLogin(navigationSource());
        }
    }

    @FXML
    public void toggleTheme() {
        if (themeToggleBtn != null && themeToggleBtn.getScene() != null) {
            ThemeManager.toggleTheme(themeToggleBtn.getScene());
            configureIcons();
        }
    }

    @FXML
    public void showNotifications() {
        if (notificationButton == null) {
            return;
        }
        if (notificationsMenu != null && notificationsMenu.isShowing()) {
            notificationsMenu.hide();
            return;
        }

        Integer userId = resolveCurrentUserId();
        if (userId == null) {
            notificationsMenu = buildNotificationMessageMenu("Please sign in to view notifications.");
            notificationsMenu.show(notificationButton, Side.BOTTOM, 0, 8);
            return;
        }

        try {
            List<Notification> unread = notificationService.findUnreadByUser(userId, 8);
            notificationsMenu = buildNotificationsMenu(userId, unread);
            notificationsMenu.show(notificationButton, Side.BOTTOM, 0, 8);
            updateNotificationBadge();
        } catch (SQLException e) {
            showNotificationError(e.getMessage());
        }
    }

    public void refreshNotificationBadge() {
        updateNotificationBadge();
    }

    private void configureIcons() {
        if (themeToggleBtn != null) {
            themeToggleBtn.setText("");
            themeToggleBtn.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
        }
        if (userMenu != null) {
            userMenu.setText("");
            userMenu.setGraphic(AppIcons.user());
        }
        if (menuProfile != null) {
            menuProfile.setGraphic(AppIcons.user());
        }
        if (menuSettings != null) {
            menuSettings.setGraphic(AppIcons.gear());
        }
    }

    private ContextMenu buildNotificationsMenu(int userId, List<Notification> notifications) {
        ContextMenu menu = new ContextMenu();
        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 16; -fx-border-color: #e2e8f0; -fx-border-radius: 16;");
        root.setPrefWidth(340);

        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 900;");
        root.getChildren().add(title);

        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("No new notifications.");
            empty.setWrapText(true);
            empty.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            root.getChildren().add(empty);
        } else {
            for (Notification notification : notifications) {
                root.getChildren().add(notificationRow(notification, menu));
            }

            Button markAll = new Button("Mark all as read");
            markAll.setMaxWidth(Double.MAX_VALUE);
            markAll.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #1d4ed8; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 8 12;");
            markAll.setOnAction(event -> {
                try {
                    notificationService.markAllAsReadForUser(userId);
                    menu.hide();
                    updateNotificationBadge();
                } catch (SQLException e) {
                    showNotificationError(e.getMessage());
                }
            });
            root.getChildren().add(markAll);
        }

        CustomMenuItem item = new CustomMenuItem(root);
        item.setHideOnClick(false);
        menu.getItems().setAll(item);
        menu.setOnHidden(event -> updateNotificationBadge());
        return menu;
    }

    private VBox notificationRow(Notification notification, ContextMenu menu) {
        Label title = new Label(valueOrFallback(notification.getTitle(), "Notification"));
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 12.5px; -fx-font-weight: 900;");

        Label message = new Label(valueOrFallback(notification.getMessage(), ""));
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

        String dateText = notification.getCreatedAt() == null ? "" : NOTIFICATION_TIME_FMT.format(notification.getCreatedAt());
        Label date = new Label(dateText);
        date.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        VBox row = new VBox(4, title, message, date);
        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 10;");
        row.setOnMouseClicked(event -> {
            if (notification.getId() != null) {
                try {
                    notificationService.markAsRead(notification.getId());
                } catch (SQLException ignored) {
                    // Navigation should still continue if marking a notification read fails.
                }
            }
            menu.hide();
            updateNotificationBadge();
            showRendezVous();
        });
        return row;
    }

    private ContextMenu buildNotificationMessageMenu(String message) {
        ContextMenu menu = new ContextMenu();
        VBox root = new VBox(10);
        root.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 16; -fx-border-color: #e2e8f0; -fx-border-radius: 16;");
        root.setPrefWidth(320);

        Label title = new Label("Notifications");
        title.setStyle("-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 900;");

        Label content = new Label(valueOrFallback(message, "No notifications available."));
        content.setWrapText(true);
        content.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        root.getChildren().addAll(title, content);
        CustomMenuItem item = new CustomMenuItem(root);
        item.setHideOnClick(false);
        menu.getItems().setAll(item);
        return menu;
    }

    private void updateNotificationBadge() {
        if (notificationButton == null) {
            return;
        }
        Integer userId = resolveCurrentUserId();
        if (userId == null) {
            notificationButton.setText("🔔");
            return;
        }
        try {
            int unread = notificationService.countUnreadByUser(userId);
            notificationButton.setText(unread > 0 ? "🔔 " + unread : "🔔");
        } catch (SQLException e) {
            notificationButton.setText("🔔 !");
        }
    }

    private Integer resolveCurrentUserId() {
        User user = resolveCurrentUser();
        if (user != null && user.getId() != null) {
            return user.getId();
        }
        return parseUserId(System.getProperty("skillora.userId"));
    }

    private User resolveCurrentUser() {
        User studentUser = StudentLayoutController.getCurrentUser();
        if (studentUser != null && studentUser.getId() != null) {
            return studentUser;
        }

        User sessionUser = AuthSession.getCurrentUser();
        if (sessionUser != null && sessionUser.getId() != null) {
            StudentLayoutController.setCurrentUser(sessionUser);
            return sessionUser;
        }

        Integer propertyUserId = parseUserId(System.getProperty("skillora.userId"));
        if (propertyUserId == null) {
            return null;
        }

        try {
            User propertyUser = userService.getById(propertyUserId);
            if (propertyUser != null && propertyUser.getId() != null) {
                StudentLayoutController.setCurrentUser(propertyUser);
                return propertyUser;
            }
        } catch (SQLException ignored) {
            // A valid property id is still enough for notification queries.
        }

        return null;
    }

    private Integer parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void clearActive(Button button) {
        if (button != null) {
            button.getStyleClass().remove("student-nav-btn-active");
        }
    }

    private void runBeforeNavigation() {
        if (beforeNavigation != null) {
            beforeNavigation.run();
        }
    }

    private Node navigationSource() {
        if (themeToggleBtn != null) {
            return themeToggleBtn;
        }
        if (notificationButton != null) {
            return notificationButton;
        }
        return navHome;
    }

    private void showNotificationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Notifications");
        alert.setHeaderText("Unable to load notifications");
        alert.setContentText(valueOrFallback(message, "Unknown error"));
        alert.showAndWait();
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
