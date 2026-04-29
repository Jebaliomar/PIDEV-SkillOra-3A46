package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Post;
import tn.esprit.entities.Reply;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostServiceTest {

    static Connection connection;
    PostService postService;

    @BeforeAll
    static void initDatabase() throws Exception {

        Class.forName("org.h2.Driver");

        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        );

        Statement st = connection.createStatement();

        st.execute("""
            CREATE TABLE users(
                id INT PRIMARY KEY,
                username VARCHAR(50)
            )
        """);

        st.execute("""
            CREATE TABLE post(
                id INT AUTO_INCREMENT PRIMARY KEY,
                type VARCHAR(50),
                title VARCHAR(255),
                topic VARCHAR(100),
                content TEXT,
                image_url VARCHAR(512),
                created_at TIMESTAMP,
                updated_at TIMESTAMP,
                user_id INT
            )
        """);

        st.execute("INSERT INTO users VALUES (1,'testUser')");
    }

    @BeforeEach
    void setup() {
        postService = new PostService(connection);
    }

    @Test
    void shouldCreatePost() throws Exception {

        Post post = new Post();
        post.setType("discussion");
        post.setTitle("JUnit Test Post");
        post.setTopic("testing");
        post.setContent("Testing post creation");
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        post.setUserId(1);

        postService.add(post);

        assertNotNull(post.getId());
    }

    @Test
    void shouldFindPosts() throws Exception {

        List<Post> posts = postService.getAll();

        assertNotNull(posts);
    }
}



class ReplyServiceTest {

    static Connection connection;
    ReplyService replyService;

    @BeforeAll
    static void initDatabase() throws Exception {

        Class.forName("org.h2.Driver");

        connection = DriverManager.getConnection(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        );

        Statement st = connection.createStatement();

        st.execute("""
            CREATE TABLE reply(
                id INT AUTO_INCREMENT PRIMARY KEY,
                post_id INT,
                parent_id INT,
                content TEXT,
                author_name VARCHAR(100),
                upvotes INT,
                created_at TIMESTAMP,
                updated_at TIMESTAMP,
                user_id INT
            )
        """);

        st.execute("""
            INSERT INTO reply(
                post_id,parent_id,content,author_name,upvotes,created_at,updated_at,user_id
            )
            VALUES (
                1,NULL,'initial','tester',0,NOW(),NOW(),1
            )
        """);
    }

    @BeforeEach
    void setup() {
        replyService = new ReplyService(connection);
    }

    @Test
    void shouldAddReply() throws Exception {

        Reply reply = new Reply();
        reply.setPostId(1);
        reply.setParentId(null);
        reply.setContent("Unit test reply");
        reply.setAuthorName("tester");
        reply.setUpvotes(0);
        reply.setCreatedAt(LocalDateTime.now());
        reply.setUpdatedAt(LocalDateTime.now());
        reply.setUserId(1);

        replyService.add(reply);

        assertNotNull(reply.getId());
    }

    @Test
    void shouldRetrieveReplies() throws Exception {

        List<Reply> replies = replyService.getByPostId(1);

        assertNotNull(replies);
    }
}