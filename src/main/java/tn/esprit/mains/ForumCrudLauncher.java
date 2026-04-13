package tn.esprit.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import tn.esprit.controllers.forum.CreatePostController;
import tn.esprit.controllers.forum.PostDetailsController;
import tn.esprit.controllers.forum.PostOverviewController;
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

    private final Map<Integer, String> usernameCache = new HashMap<>();

    private Stage primaryStage;
    private PostService postService;
    private ReplyService replyService;
    private UserService userService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        try {
            this.postService = new PostService();
            this.replyService = new ReplyService();
            this.userService = new UserService();
        } catch (RuntimeException exception) {
            showError("Database connection failed", exception.getMessage());
            throw exception;
        }

        showOverviewScene();
        primaryStage.show();
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

    public void showPostDetailsScene(int postId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/tn/esprit/forum/post-details.fxml"));
            Parent root = loader.load();

            PostDetailsController controller = loader.getController();
            controller.setApplication(this);
            controller.loadPost(postId);

            setScene(root, "Post Details");
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
}
