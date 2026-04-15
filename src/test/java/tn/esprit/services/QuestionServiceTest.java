package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Question;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QuestionServiceTest {

    static QuestionService service;
    static Integer idTest;

    // IMPORTANT :
    // mets ici un evaluation_id qui existe déjà dans ta table evaluation
    static final int EVALUATION_ID_EXISTANT = 1;

    @BeforeAll
    static void setup() {
        service = new QuestionService();
    }

    private Question createQuestion() {
        return new Question(
                null,
                "Question test",
                "Explication test",
                "QCM",
                5,
                EVALUATION_ID_EXISTANT
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