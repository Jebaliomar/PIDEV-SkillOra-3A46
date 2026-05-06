package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.geometry.Side;
import tn.esprit.entities.Notification;
import tn.esprit.entities.User;
import tn.esprit.services.NotificationService;
import tn.esprit.services.UserService;
import tn.esprit.controllers.front.SkilloraNavbarController;
import tn.esprit.tools.AppIcons;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AuthSession;
import tn.esprit.tools.PomodoroIcon;
import tn.esprit.tools.PomodoroPopup;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.net.URL;
import java.util.ResourceBundle;

public class StudentLayoutController implements Initializable, SkilloraNavbarController.Host {

    @FXML private StackPane contentArea;
    @FXML private SkilloraNavbarController navbarController;
    @FXML private Button themeToggleBtn;
    @FXML private Button pomodoroBtn;
    @FXML private Button navHome;
    @FXML private Button navCourses;
    @FXML private Button navMyLearning;
    @FXML private Button navEvents;
    @FXML private Button navRendezVous;
    @FXML private Button navSlots;
    @FXML private Button navForum;
    @FXML private Button navAssessment;
    @FXML private Button notificationButton;
    @FXML private MenuButton userMenu;
    @FXML private MenuItem menuProfile;
    @FXML private MenuItem menuSettings;

    private static User currentUser;
    private static StudentLayoutController instance;
    private static final DateTimeFormatter NOTIFICATION_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private final NotificationService notificationService = new NotificationService();
    private final UserService userService = new UserService();
    private ContextMenu notificationsMenu;

    public static StudentLayoutController getInstance() {
        return instance;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
        if (user != null && user.getId() != null) {
            bindUserToSession(user, "student");
        } else {
            System.clearProperty("skillora.userId");
            System.clearProperty("skillora.role");
        }
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        if (navbarController != null) {
            navbarController.setHost(this);
            navbarController.refreshNotificationBadge();
        }
        if (userMenu != null) {
            userMenu.setGraphic(AppIcons.user());
            userMenu.setText("");
        }
        if (menuProfile != null) menuProfile.setGraphic(AppIcons.user());
        if (menuSettings != null) menuSettings.setGraphic(AppIcons.gear());
        updateThemeButton();
        if (pomodoroBtn != null) {
            pomodoroBtn.setText("");
            pomodoroBtn.setGraphic(PomodoroIcon.small());
            pomodoroBtn.setTooltip(new javafx.scene.control.Tooltip("Pomodoro"));
        }
        updateNotificationBadge();
        showHome();
    }

    @FXML
    public void togglePomodoro() {
        PomodoroPopup.toggle(themeToggleBtn.getScene().getWindow());
    }

    @FXML
    public void showSkills() {
        try {
            FXMLLoader loader = tn.esprit.tools.Loaders.loader(getClass(), "/fxml/SkillGraphContent.fxml");
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
            setActiveNav(SkilloraNavbarController.ActivePage.NONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showHome() {
        loadContent("/fxml/HomeContent.fxml");
        setActiveNav(SkilloraNavbarController.ActivePage.HOME);
    }

    @FXML
    public void showCourses() {
        try {
            syncStudentSession();
            ensureCourseStylesheets();
            tn.esprit.controllers.front.FrontShellController shell =
                    new tn.esprit.controllers.front.FrontShellController();
            shell.bindContentContainer(contentArea);
            shell.showBrowseCourses();
            setActiveNav(SkilloraNavbarController.ActivePage.COURSES);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showMyLearning() {
        try {
            syncStudentSession();
            ensureCourseStylesheets();
            tn.esprit.controllers.front.FrontShellController shell =
                    new tn.esprit.controllers.front.FrontShellController();
            shell.bindContentContainer(contentArea);
            shell.showMyLearning();
            setActiveNav(SkilloraNavbarController.ActivePage.MY_LEARNING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showEvents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/event/SiteEventsView.fxml"));
            Parent root = loader.load();
            // Strip the view's own top navbar so the student nav stays visible.
            if (root instanceof BorderPane bp) {
                Node center = bp.getCenter();
                bp.setTop(null);
                bp.setBottom(null);
                bp.setLeft(null);
                bp.setRight(null);
                if (center != null) {
                    contentArea.getChildren().setAll(center);
                } else {
                    contentArea.getChildren().setAll(root);
                }
            } else {
                contentArea.getChildren().setAll(root);
            }
            ensureEventStylesheets();
            setActiveNav(SkilloraNavbarController.ActivePage.EVENTS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showRendezVous() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/views/rendezvous/rendezvous-list.fxml"));
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
            ensureRendezVousStylesheet();
            setActiveNav(SkilloraNavbarController.ActivePage.RENDEZ_VOUS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showSlots() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/views/availability-slot/availability-slot-list.fxml"));
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
            ensureRendezVousStylesheet();
            setActiveNav(SkilloraNavbarController.ActivePage.SLOTS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private tn.esprit.mains.ForumCrudLauncher embeddedForumLauncher;

    @FXML
    public void showForum() {
        try {
            if (embeddedForumLauncher == null) {
                embeddedForumLauncher = new tn.esprit.mains.ForumCrudLauncher() {
                    @Override
                    protected void setScene(Parent root, String title) {
                        contentArea.getChildren().setAll(root);
                    }
                };
                embeddedForumLauncher.initForEmbedded((Stage) contentArea.getScene().getWindow());
            }
            embeddedForumLauncher.showOverviewScene();
            ensureForumStylesheet();
            setActiveNav(SkilloraNavbarController.ActivePage.FORUM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showAssessment() {
        showMyAssessments();
    }

    @FXML
    public void showMyAssessments() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent content = loader.load();
            contentArea.getChildren().setAll(extractPageContent(content));
            addStylesheet(contentArea.getScene(), "/user-assessment.css");
            setActiveNav(SkilloraNavbarController.ActivePage.ASSESSMENT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Node extractPageContent(Parent content) {
        if (content instanceof BorderPane borderPane && borderPane.getTop() != null && borderPane.getCenter() != null) {
            Node center = borderPane.getCenter();
            borderPane.setCenter(null);
            return center;
        }
        return content;
    }

    private void ensureForumStylesheet() {
        Scene scene = contentArea.getScene();
        if (scene == null) return;
        addStylesheet(scene, "/tn/esprit/forum/forum.css");
    }

    private void ensureEventStylesheets() {
        Scene scene = contentArea.getScene();
        if (scene == null) return;
        addStylesheet(scene, "/styles/site-events.css");
        addStylesheet(scene, "/styles/event.css");
        addStylesheet(scene, "/styles/salle.css");
        addStylesheet(scene, "/styles/reservations.css");
    }

    private void ensureRendezVousStylesheet() {
        Scene scene = contentArea.getScene();
        if (scene == null) return;
        addStylesheet(scene, "/tn/esprit/views/theme/skillora-pro.css");
    }

    private void ensureCourseStylesheets() {
        Scene scene = contentArea.getScene();
        if (scene == null) return;
        addStylesheet(scene, "/atlantafx/base/theme/primer-light.css");
        addStylesheet(scene, "/styles/macos-theme.css");
        addStylesheet(scene, "/styles/skillora-unified.css");
        ThemeManager.applyTheme(scene);
    }

    private void addStylesheet(Scene scene, String path) {
        try {
            String url = getClass().getResource(path).toExternalForm();
            if (!scene.getStylesheets().contains(url)) scene.getStylesheets().add(url);
        } catch (Exception ignored) {
        }
    }

    @FXML
    public void showProfile() {
        loadContent("/fxml/ProfileContent.fxml");
        setActiveNav(SkilloraNavbarController.ActivePage.NONE);
    }

    @FXML
    public void showSettings() {
        loadContent("/fxml/SettingsContent.fxml");
        setActiveNav(SkilloraNavbarController.ActivePage.NONE);
    }

    public void showCourseHub() {
        showCourses();
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActiveNav(SkilloraNavbarController.ActivePage activePage) {
        if (navbarController != null) {
            navbarController.setActivePage(activePage);
        }
    }

    @FXML
    public void toggleTheme() {
        ThemeManager.toggle(themeToggleBtn.getScene());
        updateThemeButton();
    }

    private void updateThemeButton() {
        if (themeToggleBtn == null) return;
        themeToggleBtn.setText("");
        themeToggleBtn.setGraphic(ThemeManager.isDarkMode() ? ThemeIcon.sun() : ThemeIcon.moon());
    }

    @FXML
    public void handleLogout() {
        AppNavigator.showLogin(contentArea);
    }

    @FXML
    private void showNotifications() {
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
                    // The row should still open the Rendez-vous view if marking read fails.
                }
            }
            menu.hide();
            updateNotificationBadge();
            showRendezVous();
        });
        return row;
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
        if (currentUser != null && currentUser.getId() != null) {
            bindUserToSession(currentUser, "student");
            return currentUser;
        }

        User sessionUser = AuthSession.getCurrentUser();
        if (sessionUser != null && sessionUser.getId() != null) {
            currentUser = sessionUser;
            bindUserToSession(sessionUser, "student");
            return sessionUser;
        }

        Integer propertyUserId = parseUserId(System.getProperty("skillora.userId"));
        if (propertyUserId == null) {
            return null;
        }

        try {
            User propertyUser = userService.getById(propertyUserId);
            if (propertyUser != null && propertyUser.getId() != null) {
                currentUser = propertyUser;
                bindUserToSession(propertyUser, "student");
                return propertyUser;
            }
        } catch (SQLException ignored) {
            // A valid property id is still enough for notification queries.
        }

        return null;
    }

    private void syncStudentSession() {
        User user = resolveCurrentUser();
        if (user != null && user.getId() != null) {
            bindUserToSession(user, "student");
        }
    }

    private static void bindUserToSession(User user, String fallbackRole) {
        if (user == null || user.getId() == null) {
            return;
        }
        currentUser = user;
        String role = resolveSessionRole(fallbackRole);
        System.setProperty("skillora.userId", String.valueOf(user.getId()));
        System.setProperty("skillora.role", role);
        AuthSession.setCurrentUser(user, role);
    }

    private static String resolveSessionRole(String fallbackRole) {
        String propertyRole = System.getProperty("skillora.role");
        if (propertyRole != null && !propertyRole.isBlank()) {
            return propertyRole.trim();
        }
        String sessionRole = AuthSession.getCurrentRole();
        if (sessionRole != null && !sessionRole.isBlank()) {
            return sessionRole.trim();
        }
        return fallbackRole == null || fallbackRole.isBlank() ? "student" : fallbackRole.trim();
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
