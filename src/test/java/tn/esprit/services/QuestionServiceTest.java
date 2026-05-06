package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Question;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QuestionServiceTest {

    static QuestionService service;
    static Integer idTest;
    static int evaluationIdTest;
    static final String EVALUATION_TITLE = "QuestionServiceTest-" + UUID.randomUUID();

    @BeforeAll
    static void setup() throws SQLException {
        service = new QuestionService();
        evaluationIdTest = createEvaluationFixture();
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (evaluationIdTest > 0) {
            Connection cn = MyConnection.getInstance().getConnection();

            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM question WHERE evaluation_id = ?")) {
                ps.setInt(1, evaluationIdTest);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = cn.prepareStatement("DELETE FROM evaluation WHERE id = ?")) {
                ps.setInt(1, evaluationIdTest);
                ps.executeUpdate();
            }
        }
    }

    private static int createEvaluationFixture() throws SQLException {
        String sql = "INSERT INTO evaluation " +
                "(title, description, type, duration, total_score, created_at, docx_path, pdf_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Connection cn = MyConnection.getInstance().getConnection();
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, EVALUATION_TITLE);
            ps.setString(2, "Fixture for question service tests");
            ps.setString(3, "Exam");
            ps.setInt(4, 60);
            ps.setInt(5, 20);
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(7, "question-service-test.docx");
            ps.setString(8, "question-service-test.pdf");
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Impossible de créer l'évaluation de test.");
    }

    private Question createQuestion() {
        return new Question(
                null,
                "Question test",
                "Explication test",
                "QCM",
                5,
                evaluationIdTest
        );
    }

    @Test
    @Order(1)
    void testAjouter() throws SQLException {
        Question q = createQuestion();
        service.ajouter(q);

        List<Question> list = service.recuperer();

        assertFalse(list.isEmpty());

        Question found = list.stream()
                .filter(qq -> qq.getContent().equals("Question test"))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        idTest = found.getId();
    }

    @Test
    @Order(2)
    void testModifier() throws SQLException {
        Question q = service.chercherParId(idTest);
        assertNotNull(q);

        q.setContent("Question modifiee");
        service.modifier(q);

        Question updated = service.chercherParId(idTest);
        assertNotNull(updated);
        assertEquals("Question modifiee", updated.getContent());
    }

    @Test
    @Order(3)
    void testSupprimer() throws SQLException {
        Question q = new Question();
        q.setId(idTest);

        service.supprimer(q);

        Question deleted = service.chercherParId(idTest);
        assertNull(deleted);
    }

    @Test
    @Order(4)
    void testValiderQuestion() {
        Question q = createQuestion();
        assertTrue(service.validerQuestion(q));
    }

    @Test
    @Order(5)
    void testAjouterInvalide() {
        Question q = new Question();

        assertThrows(IllegalArgumentException.class, () -> service.ajouter(q));
    }
}
