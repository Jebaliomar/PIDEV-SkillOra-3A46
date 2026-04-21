package tn.esprit.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostTest {

    @Test
    @DisplayName("Post constructor should populate all fields")
    void constructorShouldPopulateAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 16, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 16, 11, 45);

        Post post = new Post(
                1,
                "discussion",
                "Testing in Java",
                "java",
                "This is a sample post",
                createdAt,
                updatedAt,
                42
        );

        assertEquals(1, post.getId());
        assertEquals("discussion", post.getType());
        assertEquals("Testing in Java", post.getTitle());
        assertEquals("java", post.getTopic());
        assertEquals("This is a sample post", post.getContent());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(updatedAt, post.getUpdatedAt());
        assertEquals(42, post.getUserId());
    }

    @Test
    @DisplayName("Post getters and setters should work correctly")
    void gettersAndSettersShouldWork() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 16, 9, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 16, 9, 15);

        Post post = new Post();
        post.setId(10);
        post.setType("announcement");
        post.setTitle("Platform update");
        post.setTopic("system");
        post.setContent("We deployed a new version.");
        post.setCreatedAt(createdAt);
        post.setUpdatedAt(updatedAt);
        post.setUserId(7);

        assertEquals(10, post.getId());
        assertEquals("announcement", post.getType());
        assertEquals("Platform update", post.getTitle());
        assertEquals("system", post.getTopic());
        assertEquals("We deployed a new version.", post.getContent());
        assertEquals(createdAt, post.getCreatedAt());
        assertEquals(updatedAt, post.getUpdatedAt());
        assertEquals(7, post.getUserId());
    }

    @Test
    @DisplayName("Post toString should include the main fields")
    void toStringShouldIncludeMainFields() {
        Post post = new Post();
        post.setId(3);
        post.setType("question");
        post.setTitle("Need help");
        post.setTopic("javafx");
        post.setContent("How do I bind fields?");
        post.setUserId(12);

        String text = post.toString();

        assertTrue(text.contains("id=3"));
        assertTrue(text.contains("type=question"));
        assertTrue(text.contains("title=Need help"));
        assertTrue(text.contains("topic=javafx"));
        assertTrue(text.contains("content=How do I bind fields?"));
        assertTrue(text.contains("userId=12"));
    }
}

class ReplyTest {

    @Test
    @DisplayName("Reply constructor should populate all fields")
    void constructorShouldPopulateAllFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 16, 12, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 16, 12, 10);

        Reply reply = new Reply(
                5,
                1,
                null,
                "Nice post",
                "Rayen",
                3,
                createdAt,
                updatedAt,
                42
        );

        assertEquals(5, reply.getId());
        assertEquals(1, reply.getPostId());
        assertNull(reply.getParentId());
        assertEquals("Nice post", reply.getContent());
        assertEquals("Rayen", reply.getAuthorName());
        assertEquals(3, reply.getUpvotes());
        assertEquals(createdAt, reply.getCreatedAt());
        assertEquals(updatedAt, reply.getUpdatedAt());
        assertEquals(42, reply.getUserId());
    }

    @Test
    @DisplayName("Reply getters and setters should work correctly")
    void gettersAndSettersShouldWork() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 16, 13, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 16, 13, 20);

        Reply reply = new Reply();
        reply.setId(11);
        reply.setPostId(2);
        reply.setParentId(4);
        reply.setContent("I agree with this point.");
        reply.setAuthorName("Admin");
        reply.setUpvotes(8);
        reply.setCreatedAt(createdAt);
        reply.setUpdatedAt(updatedAt);
        reply.setUserId(9);

        assertEquals(11, reply.getId());
        assertEquals(2, reply.getPostId());
        assertEquals(4, reply.getParentId());
        assertEquals("I agree with this point.", reply.getContent());
        assertEquals("Admin", reply.getAuthorName());
        assertEquals(8, reply.getUpvotes());
        assertEquals(createdAt, reply.getCreatedAt());
        assertEquals(updatedAt, reply.getUpdatedAt());
        assertEquals(9, reply.getUserId());
    }

    @Test
    @DisplayName("Reply toString should include the main fields")
    void toStringShouldIncludeMainFields() {
        Reply reply = new Reply();
        reply.setId(7);
        reply.setPostId(2);
        reply.setParentId(1);
        reply.setContent("Thanks for sharing");
        reply.setAuthorName("Alice");
        reply.setUpvotes(4);
        reply.setUserId(13);

        String text = reply.toString();

        assertTrue(text.contains("id=7"));
        assertTrue(text.contains("postId=2"));
        assertTrue(text.contains("parentId=1"));
        assertTrue(text.contains("content=Thanks for sharing"));
        assertTrue(text.contains("authorName=Alice"));
        assertTrue(text.contains("upvotes=4"));
        assertTrue(text.contains("userId=13"));
    }
}
