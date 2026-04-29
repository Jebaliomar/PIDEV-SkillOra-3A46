package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import tn.esprit.controllers.forum.AdminPostManagementController;
import tn.esprit.controllers.forum.CreatePostController;
import tn.esprit.controllers.forum.PostDetailsController;
import tn.esprit.controllers.forum.PostOverviewController;
import tn.esprit.controllers.forum.ReportManagementController;
import tn.esprit.entities.User;
import tn.esprit.services.PostService;
import tn.esprit.services.ReactionService;
import tn.esprit.services.ReplyService;
import tn.esprit.services.ReportService;
import tn.esprit.services.ShareService;
import tn.esprit.services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ForumCrudLauncher extends Application {

    private static final double WINDOW_WIDTH = 980;
    private static final double WINDOW_HEIGHT = 720;
    private static final int STATIC_USER_ID = 2;
    private static final String STATIC_USERNAME = "Forum User";

    private final Map<Integer, String> usernameCache = new HashMap<>();

    private Stage primaryStage;
    private PostService postService;
    private ReactionService reactionService;
    private ReplyService replyService;
    private ReportService reportService;
    private UserService userService;
    private User currentUser;

    public static void main(String[] args) {
        launch(args);
    }

    private final ShareService shareService = new ShareService();

    public ShareService getShareService() {
        return shareService;
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            this.postService = new PostService();
            this.reactionService = new ReactionService();
            this.replyService = new ReplyService();
            this.reportService = new ReportService();
            this.userService = new UserService();
            this.currentUser = createStaticUser();
        } catch (RuntimeException exception) {
            showError("Database connection failed", exception.getMessage());
            throw exception;
        }

        showOverviewScene();
        primaryStage.show();
    }

    public void showOverviewScene() {
        loadScene("/tn/esprit/forum/post-overview.fxml", "Posts Overview", controller -> {
            PostOverviewController view = (PostOverviewController) controller;
            view.setApplication(this);
            view.loadPosts();
        });
    }

    public void showCreatePostScene() {
        loadScene("/tn/esprit/forum/create-post.fxml", "Create Post", controller -> {
            CreatePostController view = (CreatePostController) controller;
            view.setApplication(this);
        });
    }

    public void showEditPostScene(int postId) {
        loadScene("/tn/esprit/forum/create-post.fxml", "Edit Post", controller -> {
            CreatePostController view = (CreatePostController) controller;
            view.setApplication(this);
            view.loadPostForEdit(postId);
        });
    }

    public void showPostDetailsScene(int postId) {
        loadScene("/tn/esprit/forum/post-details.fxml", "Post Details", controller -> {
            PostDetailsController view = (PostDetailsController) controller;
            view.setApplication(this);
            view.loadPost(postId);
        });
    }

    public void showAdminPostManagementScene() {
        loadScene("/tn/esprit/forum/admin-post-management.fxml", "Admin Posts", controller -> {
            AdminPostManagementController view = (AdminPostManagementController) controller;
            view.setApplication(this);
            view.loadPosts();
        });
    }

    public void showReportManagementScene() {
        if (!canModeratePosts()) {
            showError("Access denied", "You do not have permission to open the moderation reports screen.");
            return;
        }

        loadScene("/tn/esprit/forum/report-management.fxml", "Reports", controller -> {
            ReportManagementController view = (ReportManagementController) controller;
            view.setApplication(this);
            view.loadReports();
        });
    }

    private void loadScene(String fxmlPath, String title, SceneInitializer initializer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            initializer.initialize(loader.getController());
            setScene(root, title);
        } catch (IOException exception) {
            exception.printStackTrace();
            showError("View loading failed", exception.getMessage());
        }
    }

    private void setScene(Parent root, String title) {
        primaryStage.setTitle("SkillOra Forum - " + title);
        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }

    public PostService getPostService() {
        return postService;
    }

    public ReplyService getReplyService() {
        return replyService;
    }

    public ReactionService getReactionService() {
        return reactionService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserDisplay() {
        return currentUser == null ? "No active user" : currentUser.getUsername() + " (ID: " + currentUser.getId() + ")";
    }

    public boolean canModeratePosts() {
        if (currentUser == null || currentUser.getId() == null) {
            return false;
        }

        try {
            return userService.hasRole(currentUser.getId(), "ADMIN", "ROLE_ADMIN");
        } catch (SQLException exception) {
            return STATIC_USER_ID == currentUser.getId();
        }
    }

    public String resolveUsername(Integer userId) {
        if (userId == null) {
            return "Unknown user";
        }

        if (usernameCache.containsKey(userId)) {
            return usernameCache.get(userId);
        }

        try {
            String username = userService.getUsernameById(userId);
            usernameCache.put(userId, username);
            return username;
        } catch (SQLException exception) {
            String fallback = "User #" + userId;
            usernameCache.put(userId, fallback);
            return fallback;
        }
    }

    private User createStaticUser() {
        User user = new User();
        user.setId(STATIC_USER_ID);
        user.setUsername(STATIC_USERNAME);
        usernameCache.put(user.getId(), user.getUsername());
        return user;
    }

    public void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean confirmAction(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    @FunctionalInterface
    private interface SceneInitializer {
        void initialize(Object controller);
    }
}
