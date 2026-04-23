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
import tn.esprit.entities.User;
import tn.esprit.services.PostService;
import tn.esprit.services.ReplyService;
import tn.esprit.services.UserService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ForumCrudLauncher extends Application {

    private static final double WINDOW_WIDTH = 980;
    private static final double WINDOW_HEIGHT = 720;
    private static final int STATIC_USER_ID = 1;
    private static final String STATIC_USERNAME = "Forum User";

    private final Map<Integer, String> usernameCache = new HashMap<>();

    private Stage primaryStage;
    private PostService postService;
    private ReplyService replyService;
    private UserService userService;
    private User currentUser;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        initializeServicesAndUser();
        showOverviewScene();
        primaryStage.show();
    }

    /**
     * Prepare services and current user for embedded usage (non-Application entrypoints).
     */
    public void initForEmbedded(Stage stage) {
        this.primaryStage = stage;
        initializeServicesAndUser();
    }

    private void initializeServicesAndUser() {
        try {
            this.postService = new PostService();
            this.replyService = new ReplyService();
            this.userService = new UserService();

            // Try to load a real user from the database (preferred). Fall back to a static user if DB is empty or an error occurs.
            try {
                User dbUser = null;
                try {
                    dbUser = userService.getById(STATIC_USER_ID);
                } catch (SQLException ignored) {
                    // ignore, we'll try other methods
                }

                if (dbUser == null) {
                    // if no user with STATIC_USER_ID, attempt to pick the first existing user
                    try {
                        java.util.List<User> users = userService.getAll();
                        if (users != null && !users.isEmpty()) {
                            dbUser = users.get(0);
                        }
                    } catch (SQLException ignored) {
                        // ignore and fallback
                    }
                }

                if (dbUser != null) {
                    this.currentUser = dbUser;
                    usernameCache.put(dbUser.getId(), dbUser.getUsername());
                } else {
                    this.currentUser = createStaticUser();
                }
            } catch (RuntimeException e) {
                // If anything unexpected happens, ensure we still have a usable currentUser
                this.currentUser = createStaticUser();
            }
        } catch (RuntimeException exception) {
            showError("Database connection failed", exception.getMessage());
            throw exception;
        }
    }

    public void showOverviewScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/forum/post-overview.fxml"));
            Parent root = loader.load();

            PostOverviewController controller = loader.getController();
            controller.setApplication(this);
            controller.loadPosts();

            setScene(root, "Posts Overview");
        } catch (IOException exception) {
            showError("View loading failed", exception.getMessage());
        }
    }

    public void showCreatePostScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/forum/create-post.fxml"));
            Parent root = loader.load();

            CreatePostController controller = loader.getController();
            controller.setApplication(this);

            setScene(root, "Create Post");
        } catch (IOException exception) {
            showError("View loading failed", exception.getMessage());
        }
    }

    public void showEditPostScene(int postId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/forum/create-post.fxml"));
            Parent root = loader.load();

            CreatePostController controller = loader.getController();
            controller.setApplication(this);
            if (!controller.loadPostForEdit(postId)) {
                return;
            }

            setScene(root, "Edit Post");
        } catch (IOException exception) {
            showError("View loading failed", exception.getMessage());
        }
    }

    public void showPostDetailsScene(int postId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/forum/post-details.fxml"));
            Parent root = loader.load();

            PostDetailsController controller = loader.getController();
            controller.setApplication(this);
            if (!controller.loadPost(postId)) {
                return;
            }

            setScene(root, "Post Details");
        } catch (IOException exception) {
            showError("View loading failed", exception.getMessage());
        }
    }

    public void showAdminPostManagementScene() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/forum/admin-post-management.fxml"));
            Parent root = loader.load();

            AdminPostManagementController controller = loader.getController();
            controller.setApplication(this);
            controller.loadPosts();

            setScene(root, "Admin Posts");
        } catch (IOException exception) {
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

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentUserDisplay() {
        return currentUser.getUsername() + " (ID: " + currentUser.getId() + ")";
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
            return "User #" + userId;
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
}
