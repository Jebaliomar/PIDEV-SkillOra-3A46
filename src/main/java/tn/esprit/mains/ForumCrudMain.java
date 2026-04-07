package tn.esprit.mains;

import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;
import tn.esprit.services.PostService;
import tn.esprit.services.ReplyService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class ForumCrudMain {

    public static void main(String[] args) {
        PostService postService = new PostService();
        ReplyService replyService = new ReplyService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handlePostMenu(scanner, postService);
                    case "2" -> handleReplyMenu(scanner, replyService);
                    case "0" -> {
                        running = false;
                        System.out.println("Application fermee.");
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur SQL: " + e.getMessage());
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n===== Forum CRUD =====");
        System.out.println("1. CRUD Post");
        System.out.println("2. CRUD Reply");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void handlePostMenu(Scanner scanner, PostService postService) throws SQLException {
        boolean back = false;

        while (!back) {
            System.out.println("\n===== Gestion des Posts =====");
            System.out.println("1. Ajouter un post");
            System.out.println("2. Afficher tous les posts");
            System.out.println("3. Rechercher un post par id");
            System.out.println("4. Afficher les posts d'un user");
            System.out.println("5. Modifier un post");
            System.out.println("6. Supprimer un post");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> addPost(scanner, postService);
                case "2" -> showAllPosts(postService);
                case "3" -> showPostById(scanner, postService);
                case "4" -> showPostsByUser(scanner, postService);
                case "5" -> updatePost(scanner, postService);
                case "6" -> deletePost(scanner, postService);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleReplyMenu(Scanner scanner, ReplyService replyService) throws SQLException {
        boolean back = false;

        while (!back) {
            System.out.println("\n===== Gestion des Replies =====");
            System.out.println("1. Ajouter un reply");
            System.out.println("2. Afficher tous les replies");
            System.out.println("3. Rechercher un reply par id");
            System.out.println("4. Afficher les replies d'un post");
            System.out.println("5. Modifier un reply");
            System.out.println("6. Supprimer un reply");
            System.out.println("0. Retour");
            System.out.print("Votre choix : ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> addReply(scanner, replyService);
                case "2" -> showAllReplies(replyService);
                case "3" -> showReplyById(scanner, replyService);
                case "4" -> showRepliesByPost(scanner, replyService);
                case "5" -> updateReply(scanner, replyService);
                case "6" -> deleteReply(scanner, replyService);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void addPost(Scanner scanner, PostService postService) throws SQLException {
        Post post = readPostData(scanner, new Post());
        LocalDateTime now = LocalDateTime.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        postService.add(post);
        System.out.println("Post ajoute avec succes. ID = " + post.getId());
    }

    private static void showAllPosts(PostService postService) throws SQLException {
        List<Post> posts = postService.getAll();
        if (posts.isEmpty()) {
            System.out.println("Aucun post trouve.");
            return;
        }

        for (Post post : posts) {
            System.out.println(post);
        }
    }

    private static void showPostById(Scanner scanner, PostService postService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du post : ");
        Post post = postService.getById(id);

        if (post == null) {
            System.out.println("Post introuvable.");
        } else {
            System.out.println(post);
        }
    }

    private static void showPostsByUser(Scanner scanner, PostService postService) throws SQLException {
        int userId = readInt(scanner, "Entrer le user_id : ");
        List<Post> posts = postService.getByUserId(userId);

        if (posts.isEmpty()) {
            System.out.println("Aucun post trouve pour ce user.");
            return;
        }

        for (Post post : posts) {
            System.out.println(post);
        }
    }

    private static void updatePost(Scanner scanner, PostService postService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du post a modifier : ");
        Post existingPost = postService.getById(id);

        if (existingPost == null) {
            System.out.println("Post introuvable.");
            return;
        }

        Post updatedPost = readPostData(scanner, existingPost);
        updatedPost.setId(id);
        if (updatedPost.getCreatedAt() == null) {
            updatedPost.setCreatedAt(LocalDateTime.now());
        }
        updatedPost.setUpdatedAt(LocalDateTime.now());

        if (postService.update(updatedPost)) {
            System.out.println("Post modifie avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deletePost(Scanner scanner, PostService postService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du post a supprimer : ");

        if (postService.delete(id)) {
            System.out.println("Post supprime avec succes.");
        } else {
            System.out.println("Aucun post supprime.");
        }
    }

    private static Post readPostData(Scanner scanner, Post post) {
        System.out.print("Type : ");
        post.setType(scanner.nextLine().trim());

        System.out.print("Titre : ");
        post.setTitle(scanner.nextLine().trim());

        System.out.print("Topic : ");
        post.setTopic(scanner.nextLine().trim());

        System.out.print("Content : ");
        post.setContent(scanner.nextLine().trim());

        System.out.print("User ID (laisser vide pour null) : ");
        String userInput = scanner.nextLine().trim();
        post.setUserId(userInput.isEmpty() ? null : Integer.parseInt(userInput));

        return post;
    }

    private static void addReply(Scanner scanner, ReplyService replyService) throws SQLException {
        Reply reply = readReplyData(scanner, new Reply());
        if (reply.getUpvotes() == null) {
            reply.setUpvotes(0);
        }
        LocalDateTime now = LocalDateTime.now();
        reply.setCreatedAt(now);
        reply.setUpdatedAt(now);
        replyService.add(reply);
        System.out.println("Reply ajoute avec succes. ID = " + reply.getId());
    }

    private static void showAllReplies(ReplyService replyService) throws SQLException {
        List<Reply> replies = replyService.getAll();
        if (replies.isEmpty()) {
            System.out.println("Aucun reply trouve.");
            return;
        }

        for (Reply reply : replies) {
            System.out.println(reply);
        }
    }

    private static void showReplyById(Scanner scanner, ReplyService replyService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du reply : ");
        Reply reply = replyService.getById(id);

        if (reply == null) {
            System.out.println("Reply introuvable.");
        } else {
            System.out.println(reply);
        }
    }

    private static void showRepliesByPost(Scanner scanner, ReplyService replyService) throws SQLException {
        int postId = readInt(scanner, "Entrer le post_id : ");
        List<Reply> replies = replyService.getByPostId(postId);

        if (replies.isEmpty()) {
            System.out.println("Aucun reply trouve pour ce post.");
            return;
        }

        for (Reply reply : replies) {
            System.out.println(reply);
        }
    }

    private static void updateReply(Scanner scanner, ReplyService replyService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du reply a modifier : ");
        Reply existingReply = replyService.getById(id);

        if (existingReply == null) {
            System.out.println("Reply introuvable.");
            return;
        }

        Reply updatedReply = readReplyData(scanner, existingReply);
        updatedReply.setId(id);
        if (updatedReply.getCreatedAt() == null) {
            updatedReply.setCreatedAt(LocalDateTime.now());
        }
        updatedReply.setUpdatedAt(LocalDateTime.now());

        if (replyService.update(updatedReply)) {
            System.out.println("Reply modifie avec succes.");
        } else {
            System.out.println("Modification echouee.");
        }
    }

    private static void deleteReply(Scanner scanner, ReplyService replyService) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du reply a supprimer : ");

        if (replyService.delete(id)) {
            System.out.println("Reply supprime avec succes.");
        } else {
            System.out.println("Aucun reply supprime.");
        }
    }

    private static Reply readReplyData(Scanner scanner, Reply reply) {
        reply.setPostId(readInt(scanner, "Post ID : "));

        System.out.print("Parent ID (laisser vide pour null) : ");
        String parentInput = scanner.nextLine().trim();
        reply.setParentId(parentInput.isEmpty() ? null : Integer.parseInt(parentInput));

        System.out.print("Content : ");
        reply.setContent(scanner.nextLine().trim());

        System.out.print("Author name : ");
        reply.setAuthorName(scanner.nextLine().trim());

        System.out.print("Upvotes (laisser vide pour 0/null) : ");
        String upvotesInput = scanner.nextLine().trim();
        reply.setUpvotes(upvotesInput.isEmpty() ? 0 : Integer.parseInt(upvotesInput));

        System.out.print("User ID (laisser vide pour null) : ");
        String userInput = scanner.nextLine().trim();
        reply.setUserId(userInput.isEmpty() ? null : Integer.parseInt(userInput));

        return reply;
    }

    private static int readInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Valeur numerique invalide.");
            }
        }
    }
}
