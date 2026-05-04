package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import tn.esprit.entities.User;
import tn.esprit.tools.AppIcons;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.PomodoroIcon;
import tn.esprit.tools.PomodoroPopup;
import tn.esprit.tools.ThemeIcon;
import tn.esprit.tools.ThemeManager;

import java.net.URL;
import java.util.ResourceBundle;

public class StudentLayoutController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button themeToggleBtn;
    @FXML private Button pomodoroBtn;
    @FXML private Button navHome;
    @FXML private Button navCourses;
    @FXML private Button navEvents;
    @FXML private Button navForum;
    @FXML private Button navAssessment;
    @FXML private Button navSkills;
    @FXML private MenuButton userMenu;
    @FXML private MenuItem menuProfile;
    @FXML private MenuItem menuSettings;

    private static User currentUser;
    private static StudentLayoutController instance;

    public static StudentLayoutController getInstance() {
        return instance;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        if (userMenu != null) {
            userMenu.setGraphic(AppIcons.user());
            if (currentUser != null) {
                userMenu.setText(currentUser.getFirstName() + " " + currentUser.getLastName());
            }
        }
        if (menuProfile != null) menuProfile.setGraphic(AppIcons.user());
        if (menuSettings != null) menuSettings.setGraphic(AppIcons.gear());
        updateThemeButton();
        if (pomodoroBtn != null) {
            pomodoroBtn.setText("");
            pomodoroBtn.setGraphic(PomodoroIcon.small());
            pomodoroBtn.setTooltip(new javafx.scene.control.Tooltip("Pomodoro"));
        }
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
            setActiveNav(navSkills);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showHome() {
        loadContent("/fxml/HomeContent.fxml");
        setActiveNav(navHome);
    }

    @FXML
    public void showCourses() {
        try {
            tn.esprit.controllers.front.FrontShellController shell =
                    new tn.esprit.controllers.front.FrontShellController();
            shell.bindContentContainer(contentArea);
            shell.showBrowseCourses();
            setActiveNav(navCourses);
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
            setActiveNav(navEvents);
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
            setActiveNav(navForum);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showAssessment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserAssessmentView.fxml"));
            Parent content = loader.load();
            contentArea.getChildren().setAll(content);
            addStylesheet(contentArea.getScene(), "/user-assessment.css");
            setActiveNav(navAssessment);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        setActiveNav(null);
    }

    @FXML
    public void showSettings() {
        loadContent("/fxml/SettingsContent.fxml");
        setActiveNav(null);
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

    private void setActiveNav(Button active) {
        for (Button b : new Button[]{navHome, navCourses, navEvents}) {
            if (b != null) {
                b.getStyleClass().remove("student-nav-btn-active");
            }
        }
        if (active != null && !active.getStyleClass().contains("student-nav-btn-active")) {
            active.getStyleClass().add("student-nav-btn-active");
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
        try {
            currentUser = null;
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("SkillORA - Sign In");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
