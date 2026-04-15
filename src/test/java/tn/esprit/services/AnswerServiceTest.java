package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.entities.Answer;
import tn.esprit.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AnswerServiceTest {

    static AnswerService service;
    static Integer idTest;

    static int questionIdExistant;
    static int studentIdExistant;

    @BeforeAll
    static void setup() throws SQLException {
        service = new AnswerService();

        Connection cn = MyConnection.getInstance().getConnection();

        // Récupérer un question_id existant
        String qSql = "SELECT id FROM question LIMIT 1";
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(qSql)) {

            if (rs.next()) {
                questionIdExistant = rs.getInt("id");
            } else {
                throw new RuntimeException("Aucune question trouvée dans la table question.");
            }
        }

        // Récupérer un student_id existant depuis users
        String uSql = "SELECT id FROM users LIMIT 1";
        try (Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(uSql)) {

            if (rs.next()) {
                studentIdExistant = rs.getInt("id");
            } else {
                throw new RuntimeException("Aucun user trouvé dans la table users.");
            }
        }
    }

    private Answer createAnswer() {
        return new Answer(
                null,
                "Reponse test",
                true,
                "TEACHER",
                questionIdExistant,
                studentIdExistant,
                LocalDateTime.now(),
                10,
                5,
                null,
                0,
                0,
                null,
                null
        );
    }

    @Test
    @Order(1)
    void testValiderAnswer() {
        Answer a = createAnswer();
        assertTrue(service.validerAnswer(a));
    }

    @Test
    @Order(2)
    void testAjouter() throws SQLException {
        Answer a = createAnswer();

        service.ajouter(a);

        List<Answer> list = service.recuperer();
        assertFalse(list.isEmpty());

        Answer found = list.stream()
                .filter(ans ->
                        "Reponse test".equals(ans.getContent()) &&
                                ans.getQuestionId().equals(questionIdExistant) &&
                                ans.getStudentId().equals(studentIdExistant))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        idTest = found.getId();
    }

    @Test
    @Order(3)
    void testModifier() throws SQLException {
        assertNotNull(idTest);

        Answer a = service.chercherParId(idTest);
        assertNotNull(a);

        a.setContent("Reponse modifiee");
        service.modifier(a);

        Answer updated = service.chercherParId(idTest);
        assertNotNull(updated);
        assertEquals("Reponse modifiee", updated.getContent());
    }

    @Test
    @Order(4)
    void testSupprimer() throws SQLException {
        assertNotNull(idTest);

        Answer a = new Answer();
        a.setId(idTest);

        service.supprimer(a);

        Answer deleted = service.chercherParId(idTest);
        assertNull(deleted);
    }

    @Test
    @Order(5)
    void testAjouterInvalide() {
        Answer a = new Answer();

        assertThrows(IllegalArgumentException.class, () -> service.ajouter(a));
    }
}