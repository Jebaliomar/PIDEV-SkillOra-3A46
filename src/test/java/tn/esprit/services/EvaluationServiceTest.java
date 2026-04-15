package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Evaluation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EvaluationServiceTest {

    static EvaluationService service;
    static Integer idTest;

    @BeforeAll
    static void setup() {
        service = new EvaluationService();
    }

    private Evaluation createEvaluation() {
        return new Evaluation(
                null,
                "Test Java",
                "Description test",
                "Exam",
                60,
                20,
                LocalDateTime.now(),
                "test.docx",
                "test.pdf"
        );
    }

    @Test
    @Order(1)
    void testAjouter() throws SQLException {
        Evaluation e = createEvaluation();
        service.ajouter(e);

        List<Evaluation> list = service.recuperer();

        assertFalse(list.isEmpty());

        Evaluation found = list.stream()
                .filter(ev -> ev.getTitle().equals("Test Java"))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        idTest = found.getId();
    }

    @Test
    @Order(2)
    void testModifier() throws SQLException {
        Evaluation e = service.chercherParId(idTest);

        e.setTitle("Modifie");
        service.modifier(e);

        Evaluation updated = service.chercherParId(idTest);

        assertEquals("Modifie", updated.getTitle());
    }

    @Test
    @Order(3)
    void testSupprimer() throws SQLException {
        Evaluation e = new Evaluation();
        e.setId(idTest);

        service.supprimer(e);

        Evaluation deleted = service.chercherParId(idTest);

        assertNull(deleted);
    }

    @Test
    @Order(4)
    void testValiderEvaluation() {
        Evaluation e = createEvaluation();
        assertTrue(service.validerEvaluation(e));
    }

    @Test
    @Order(5)
    void testAjouterInvalide() {
        Evaluation e = new Evaluation();

        assertThrows(IllegalArgumentException.class, () -> service.ajouter(e));
    }
}