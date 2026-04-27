package tn.esprit.controllers.front;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.front.courses.FrontCourseConsumeController;
import tn.esprit.controllers.front.courses.FrontCourseShowController;
import tn.esprit.entities.Course;
import tn.esprit.entities.Lesson;
import tn.esprit.tools.AppNavigator;
import tn.esprit.tools.AuthSession;

public class FrontShellController {

    @FXML
    private Button homeNavButton;

    @FXML
    private Button browseCoursesNavButton;

    @FXML
    private Button myLearningNavButton;

    @FXML
    private Button communityNavButton;

    @FXML
    private Button eventsNavButton;

    @FXML
    private Button rendezVousNavButton;

    @FXML
    private Button dashboardButton;

    @FXML
    private Button logoutButton;

    @FXML
    private BorderPane shellPane;

    @FXML
    private StackPane sidebarContainer;

    @FXML
    private StackPane contentContainer;

    @FXML
    public void initialize() {
        configureAccessForCurrentUser();
        applyExpandedLayout();
        showHome();
    }

    @FXML
    private void handleHome() {
        showHome();
    }

    @FXML
    private void handleBrowseCourses() {
        setActiveModule(browseCoursesNavButton);
        loadContent("/views/front/home/front_browse_courses.fxml", controller -> {
        });
    }

    @FXML
    private void handleMyLearning() {
        setActiveModule(myLearningNavButton);
        loadContent("/views/front/home/front_my_learning.fxml", controller -> {
        });
    }

    @FXML
    private void handleCommunity() {
        setActiveModule(communityNavButton);
        showPlaceholder("Community", "Community features will appear here.");
    }

    @FXML
    private void handleEvents() {
        setActiveModule(eventsNavButton);
        showPlaceholder("Events", "Upcoming events will appear here.");
    }

    @FXML
    private void handleRendezVous() {
        setActiveModule(rendezVousNavButton);
        showPlaceholder("Rendez-vous", "Rendez-vous tools will appear here.");
    }

    @FXML
    private void handleDashboard() {
        AppNavigator.showAdminDashboard(dashboardButton);
    }

    @FXML
    private void handleLogout() {
        AppNavigator.showLogin(logoutButton);
    }

    private void configureAccessForCurrentUser() {
        if (dashboardButton != null && !AuthSession.isAdminAreaAllowed()) {
            dashboardButton.setManaged(false);
            dashboardButton.setVisible(false);
        }
    }

    public void showHome() {
        setActiveModule(homeNavButton);
        loadContent("/views/front/home/front_home.fxml", controller -> {
        });
    }

    public void showBrowseCourses() {
        handleBrowseCourses();
    }

    public void showCourseShow(Course course) {
        setActiveModule(browseCoursesNavButton);
        loadContent("/views/front/courses/front_course_show.fxml", controller -> {
            if (controller instanceof FrontCourseShowController courseShowController) {
                courseShowController.setCourse(course);
            }
        });
    }

    public void showCourseConsume(Course course, Lesson lesson) {
        setActiveModule(browseCoursesNavButton);
        loadContent("/views/front/courses/front_course_consume.fxml", controller -> {
            if (controller instanceof FrontCourseConsumeController consumeController) {
                consumeController.setCourse(course);
                consumeController.setInitialLesson(lesson);
            }
        });
    }

    private void showPlaceholder(String title, String message) {
        applyExpandedLayout();
        VBox placeholder = new VBox(10);
        placeholder.getStyleClass().add("placeholder-panel");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("placeholder-title");
        Label bodyLabel = new Label(message);
        bodyLabel.getStyleClass().add("placeholder-subtitle");
        bodyLabel.setWrapText(true);
        placeholder.getChildren().addAll(titleLabel, bodyLabel);
        contentContainer.getChildren().setAll(placeholder);
    }

    private void loadContent(String resourcePath, ControllerConfigurer configurer) {
        try {
            applyLayoutForResource(resourcePath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Node content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof FrontShellAware shellAware) {
                shellAware.setShellController(this);
            }
            configurer.configure(controller);
            contentContainer.getChildren().setAll(content);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to load view");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void applyLayoutForResource(String resourcePath) {
        boolean compactShell = resourcePath.endsWith("/front_browse_courses.fxml")
                || resourcePath.endsWith("/front_course_consume.fxml");
        if (compactShell) {
            applyCompactLayout();
        } else {
            applyExpandedLayout();
        }
    }

    private void applyCompactLayout() {
        if (sidebarContainer != null) {
            sidebarContainer.setManaged(false);
            sidebarContainer.setVisible(false);
            sidebarContainer.setMinWidth(0);
            sidebarContainer.setPrefWidth(0);
            sidebarContainer.setMaxWidth(0);
        }
        if (contentContainer != null) {
            BorderPane.setMargin(contentContainer, new Insets(18, 20, 20, 20));
            contentContainer.setPadding(new Insets(14, 18, 24, 18));
        }
    }

    private void applyExpandedLayout() {
        if (sidebarContainer != null) {
            sidebarContainer.setManaged(true);
            sidebarContainer.setVisible(true);
            sidebarContainer.setMinWidth(286);
            sidebarContainer.setPrefWidth(286);
            sidebarContainer.setMaxWidth(286);
        }
        if (contentContainer != null) {
            BorderPane.setMargin(contentContainer, new Insets(20, 20, 20, 14));
            contentContainer.setPadding(new Insets(28, 32, 28, 32));
        }
    }

    private void setActiveModule(Button activeButton) {
        clearModuleSelection(homeNavButton);
        clearModuleSelection(browseCoursesNavButton);
        clearModuleSelection(myLearningNavButton);
        clearModuleSelection(communityNavButton);
        clearModuleSelection(eventsNavButton);
        clearModuleSelection(rendezVousNavButton);

        if (activeButton != null && !activeButton.getStyleClass().contains("sidebar-item-selected")) {
            activeButton.getStyleClass().remove("sidebar-item");
            activeButton.getStyleClass().add("sidebar-item-selected");
        }
    }

    private void clearModuleSelection(Button button) {
        button.getStyleClass().remove("sidebar-item-selected");
        if (!button.getStyleClass().contains("sidebar-item")) {
            button.getStyleClass().add("sidebar-item");
        }
    }

    @FunctionalInterface
    private interface ControllerConfigurer {
        void configure(Object controller);
    }
}
