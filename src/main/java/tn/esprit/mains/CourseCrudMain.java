package tn.esprit.mains;

import tn.esprit.entities.Certificate;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.Lesson;
import tn.esprit.entities.LessonCompletion;
import tn.esprit.services.CertificateService;
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.CourseService;
import tn.esprit.services.EnrollmentService;
import tn.esprit.services.LessonCompletionService;
import tn.esprit.services.LessonService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class CourseCrudMain {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void main(String[] args) {
        CourseService courseService = new CourseService();
        CourseSectionService courseSectionService = new CourseSectionService();
        EnrollmentService enrollmentService = new EnrollmentService();
        LessonService lessonService = new LessonService();
        LessonCompletionService lessonCompletionService = new LessonCompletionService();
        CertificateService certificateService = new CertificateService();
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleCourseMenu(scanner, courseService);
                    case "2" -> handleCourseSectionMenu(scanner, courseSectionService);
                    case "3" -> handleEnrollmentMenu(scanner, enrollmentService);
                    case "4" -> handleLessonMenu(scanner, lessonService);
                    case "5" -> handleLessonCompletionMenu(scanner, lessonCompletionService);
                    case "6" -> handleCertificateMenu(scanner, certificateService);
                    case "0" -> {
                        running = false;
                        System.out.println("Application fermee.");
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (SQLException e) {
                System.out.println("Erreur SQL: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur de saisie: " + e.getMessage());
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n===== Gestion E-Learning =====");
        System.out.println("1. CRUD Course");
        System.out.println("2. CRUD CourseSection");
        System.out.println("3. CRUD Enrollment");
        System.out.println("4. CRUD Lesson");
        System.out.println("5. CRUD LessonCompletion");
        System.out.println("6. CRUD Certificate");
        System.out.println("0. Quitter");
        System.out.print("Votre choix : ");
    }

    private static void handleCourseMenu(Scanner scanner, CourseService service) throws SQLException {
        boolean back = false;
        while (!back) {
            printCrudMenu("Courses", "course");
            switch (scanner.nextLine().trim()) {
                case "1" -> addCourse(scanner, service);
                case "2" -> showAllCourses(service);
                case "3" -> showCourseById(scanner, service);
                case "4" -> updateCourse(scanner, service);
                case "5" -> deleteCourse(scanner, service);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleCourseSectionMenu(Scanner scanner, CourseSectionService service) throws SQLException {
        boolean back = false;
        while (!back) {
            printCrudMenu("CourseSections", "course section");
            switch (scanner.nextLine().trim()) {
                case "1" -> addCourseSection(scanner, service);
                case "2" -> showAllCourseSections(service);
                case "3" -> showCourseSectionById(scanner, service);
                case "4" -> updateCourseSection(scanner, service);
                case "5" -> deleteCourseSection(scanner, service);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleEnrollmentMenu(Scanner scanner, EnrollmentService service) throws SQLException {
        boolean back = false;
        while (!back) {
            printCrudMenu("Enrollments", "enrollment");
            switch (scanner.nextLine().trim()) {
                case "1" -> addEnrollment(scanner, service);
                case "2" -> showAllEnrollments(service);
                case "3" -> showEnrollmentById(scanner, service);
                case "4" -> updateEnrollment(scanner, service);
                case "5" -> deleteEnrollment(scanner, service);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleLessonMenu(Scanner scanner, LessonService service) throws SQLException {
        boolean back = false;
        while (!back) {
            printCrudMenu("Lessons", "lesson");
            switch (scanner.nextLine().trim()) {
                case "1" -> addLesson(scanner, service);
                case "2" -> showAllLessons(service);
                case "3" -> showLessonById(scanner, service);
                case "4" -> updateLesson(scanner, service);
                case "5" -> deleteLesson(scanner, service);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleLessonCompletionMenu(Scanner scanner, LessonCompletionService service) throws SQLException {
        boolean back = false;
        while (!back) {
            printCrudMenu("LessonCompletions", "lesson completion");
            switch (scanner.nextLine().trim()) {
                case "1" -> addLessonCompletion(scanner, service);
                case "2" -> showAllLessonCompletions(service);
                case "3" -> showLessonCompletionById(scanner, service);
                case "4" -> updateLessonCompletion(scanner, service);
                case "5" -> deleteLessonCompletion(scanner, service);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void handleCertificateMenu(Scanner scanner, CertificateService service) throws SQLException {
        boolean back = false;
        while (!back) {
            printCrudMenu("Certificates", "certificate");
            switch (scanner.nextLine().trim()) {
                case "1" -> addCertificate(scanner, service);
                case "2" -> showAllCertificates(service);
                case "3" -> showCertificateById(scanner, service);
                case "4" -> updateCertificate(scanner, service);
                case "5" -> deleteCertificate(scanner, service);
                case "0" -> back = true;
                default -> System.out.println("Choix invalide.");
            }
        }
    }

    private static void printCrudMenu(String title, String singularLabel) {
        System.out.println("\n===== Gestion des " + title + " =====");
        System.out.println("1. Ajouter un " + singularLabel);
        System.out.println("2. Afficher tous les " + title);
        System.out.println("3. Rechercher un " + singularLabel + " par id");
        System.out.println("4. Modifier un " + singularLabel);
        System.out.println("5. Supprimer un " + singularLabel);
        System.out.println("0. Retour");
        System.out.print("Votre choix : ");
    }

    private static void addCourse(Scanner scanner, CourseService service) throws SQLException {
        Course course = readCourseData(scanner, new Course());
        service.add(course);
        System.out.println("Course ajoute avec succes. ID = " + course.getId());
    }

    private static void showAllCourses(CourseService service) throws SQLException {
        List<Course> items = service.getAll();
        printList(items, "Aucun course trouve.");
    }

    private static void showCourseById(Scanner scanner, CourseService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du course : ");
        printEntity(service.getById(id), "Course introuvable.");
    }

    private static void updateCourse(Scanner scanner, CourseService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du course a modifier : ");
        Course existing = service.getById(id);
        if (existing == null) {
            System.out.println("Course introuvable.");
            return;
        }

        Course updated = readCourseData(scanner, existing);
        updated.setId(id);
        System.out.println(service.update(updated) ? "Course modifie avec succes." : "Modification echouee.");
    }

    private static void deleteCourse(Scanner scanner, CourseService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du course a supprimer : ");
        System.out.println(service.delete(id) ? "Course supprime avec succes." : "Aucun course supprime.");
    }

    private static Course readCourseData(Scanner scanner, Course course) {
        System.out.print("Titre : ");
        course.setTitle(scanner.nextLine().trim());
        System.out.print("Categorie : ");
        course.setCategory(scanner.nextLine().trim());
        System.out.print("Description : ");
        course.setDescription(scanner.nextLine().trim());
        System.out.print("Thumbnail : ");
        course.setThumbnail(scanner.nextLine().trim());
        System.out.print("Status : ");
        course.setStatus(scanner.nextLine().trim());
        course.setCreatedAt(readNullableDateTime(scanner, "Created at (yyyy-MM-dd HH:mm, vide pour null) : "));
        course.setUpdatedAt(readNullableDateTime(scanner, "Updated at (yyyy-MM-dd HH:mm, vide pour null) : "));
        return course;
    }

    private static void addCourseSection(Scanner scanner, CourseSectionService service) throws SQLException {
        CourseSection courseSection = readCourseSectionData(scanner, new CourseSection());
        service.add(courseSection);
        System.out.println("CourseSection ajoute avec succes. ID = " + courseSection.getId());
    }

    private static void showAllCourseSections(CourseSectionService service) throws SQLException {
        List<CourseSection> items = service.getAll();
        printList(items, "Aucun course section trouve.");
    }

    private static void showCourseSectionById(Scanner scanner, CourseSectionService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du course section : ");
        printEntity(service.getById(id), "CourseSection introuvable.");
    }

    private static void updateCourseSection(Scanner scanner, CourseSectionService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du course section a modifier : ");
        CourseSection existing = service.getById(id);
        if (existing == null) {
            System.out.println("CourseSection introuvable.");
            return;
        }

        CourseSection updated = readCourseSectionData(scanner, existing);
        updated.setId(id);
        System.out.println(service.update(updated) ? "CourseSection modifie avec succes." : "Modification echouee.");
    }

    private static void deleteCourseSection(Scanner scanner, CourseSectionService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du course section a supprimer : ");
        System.out.println(service.delete(id) ? "CourseSection supprime avec succes." : "Aucun course section supprime.");
    }

    private static CourseSection readCourseSectionData(Scanner scanner, CourseSection courseSection) {
        System.out.print("Titre : ");
        courseSection.setTitle(scanner.nextLine().trim());
        courseSection.setPosition(readNullableInt(scanner, "Position (vide pour null) : "));
        courseSection.setCreatedAt(readNullableDateTime(scanner, "Created at (yyyy-MM-dd HH:mm, vide pour null) : "));
        courseSection.setUpdatedAt(readNullableDateTime(scanner, "Updated at (yyyy-MM-dd HH:mm, vide pour null) : "));
        courseSection.setCourseId(readNullableInt(scanner, "Course ID (vide pour null) : "));
        return courseSection;
    }

    private static void addEnrollment(Scanner scanner, EnrollmentService service) throws SQLException {
        Enrollment enrollment = readEnrollmentData(scanner, new Enrollment());
        service.add(enrollment);
        System.out.println("Enrollment ajoute avec succes. ID = " + enrollment.getId());
    }

    private static void showAllEnrollments(EnrollmentService service) throws SQLException {
        List<Enrollment> items = service.getAll();
        printList(items, "Aucun enrollment trouve.");
    }

    private static void showEnrollmentById(Scanner scanner, EnrollmentService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du enrollment : ");
        printEntity(service.getById(id), "Enrollment introuvable.");
    }

    private static void updateEnrollment(Scanner scanner, EnrollmentService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du enrollment a modifier : ");
        Enrollment existing = service.getById(id);
        if (existing == null) {
            System.out.println("Enrollment introuvable.");
            return;
        }

        Enrollment updated = readEnrollmentData(scanner, existing);
        updated.setId(id);
        System.out.println(service.update(updated) ? "Enrollment modifie avec succes." : "Modification echouee.");
    }

    private static void deleteEnrollment(Scanner scanner, EnrollmentService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du enrollment a supprimer : ");
        System.out.println(service.delete(id) ? "Enrollment supprime avec succes." : "Aucun enrollment supprime.");
    }

    private static Enrollment readEnrollmentData(Scanner scanner, Enrollment enrollment) {
        enrollment.setEnrolledAt(readNullableDateTime(scanner, "Enrolled at (yyyy-MM-dd HH:mm, vide pour null) : "));
        enrollment.setCompletedAt(readNullableDateTime(scanner, "Completed at (yyyy-MM-dd HH:mm, vide pour null) : "));
        enrollment.setProgressPercent(readNullableShort(scanner, "Progress percent (vide pour null) : "));
        System.out.print("Status : ");
        enrollment.setStatus(scanner.nextLine().trim());
        enrollment.setUserId(readNullableInt(scanner, "User ID (vide pour null) : "));
        enrollment.setCourseId(readNullableInt(scanner, "Course ID (vide pour null) : "));
        return enrollment;
    }

    private static void addLesson(Scanner scanner, LessonService service) throws SQLException {
        Lesson lesson = readLessonData(scanner, new Lesson());
        service.add(lesson);
        System.out.println("Lesson ajoute avec succes. ID = " + lesson.getId());
    }

    private static void showAllLessons(LessonService service) throws SQLException {
        List<Lesson> items = service.getAll();
        printList(items, "Aucun lesson trouve.");
    }

    private static void showLessonById(Scanner scanner, LessonService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du lesson : ");
        printEntity(service.getById(id), "Lesson introuvable.");
    }

    private static void updateLesson(Scanner scanner, LessonService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du lesson a modifier : ");
        Lesson existing = service.getById(id);
        if (existing == null) {
            System.out.println("Lesson introuvable.");
            return;
        }

        Lesson updated = readLessonData(scanner, existing);
        updated.setId(id);
        System.out.println(service.update(updated) ? "Lesson modifie avec succes." : "Modification echouee.");
    }

    private static void deleteLesson(Scanner scanner, LessonService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du lesson a supprimer : ");
        System.out.println(service.delete(id) ? "Lesson supprime avec succes." : "Aucun lesson supprime.");
    }

    private static Lesson readLessonData(Scanner scanner, Lesson lesson) {
        System.out.print("Titre : ");
        lesson.setTitle(scanner.nextLine().trim());
        System.out.print("Type : ");
        lesson.setType(scanner.nextLine().trim());
        System.out.print("Content : ");
        lesson.setContent(scanner.nextLine().trim());
        System.out.print("File path : ");
        lesson.setFilePath(scanner.nextLine().trim());
        lesson.setPosition(readNullableInt(scanner, "Position (vide pour null) : "));
        lesson.setCreatedAt(readNullableDateTime(scanner, "Created at (yyyy-MM-dd HH:mm, vide pour null) : "));
        lesson.setUpdatedAt(readNullableDateTime(scanner, "Updated at (yyyy-MM-dd HH:mm, vide pour null) : "));
        lesson.setSectionId(readNullableInt(scanner, "Section ID (vide pour null) : "));
        return lesson;
    }

    private static void addLessonCompletion(Scanner scanner, LessonCompletionService service) throws SQLException {
        LessonCompletion lessonCompletion = readLessonCompletionData(scanner, new LessonCompletion());
        service.add(lessonCompletion);
        System.out.println("LessonCompletion ajoute avec succes. ID = " + lessonCompletion.getId());
    }

    private static void showAllLessonCompletions(LessonCompletionService service) throws SQLException {
        List<LessonCompletion> items = service.getAll();
        printList(items, "Aucun lesson completion trouve.");
    }

    private static void showLessonCompletionById(Scanner scanner, LessonCompletionService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du lesson completion : ");
        printEntity(service.getById(id), "LessonCompletion introuvable.");
    }

    private static void updateLessonCompletion(Scanner scanner, LessonCompletionService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du lesson completion a modifier : ");
        LessonCompletion existing = service.getById(id);
        if (existing == null) {
            System.out.println("LessonCompletion introuvable.");
            return;
        }

        LessonCompletion updated = readLessonCompletionData(scanner, existing);
        updated.setId(id);
        System.out.println(service.update(updated) ? "LessonCompletion modifie avec succes." : "Modification echouee.");
    }

    private static void deleteLessonCompletion(Scanner scanner, LessonCompletionService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du lesson completion a supprimer : ");
        System.out.println(service.delete(id) ? "LessonCompletion supprime avec succes." : "Aucun lesson completion supprime.");
    }

    private static LessonCompletion readLessonCompletionData(Scanner scanner, LessonCompletion lessonCompletion) {
        lessonCompletion.setCompletedAt(readNullableDateTime(scanner, "Completed at (yyyy-MM-dd HH:mm, vide pour null) : "));
        lessonCompletion.setEnrollmentId(readNullableInt(scanner, "Enrollment ID (vide pour null) : "));
        lessonCompletion.setLessonId(readNullableInt(scanner, "Lesson ID (vide pour null) : "));
        return lessonCompletion;
    }

    private static void addCertificate(Scanner scanner, CertificateService service) throws SQLException {
        Certificate certificate = readCertificateData(scanner, new Certificate());
        service.add(certificate);
        System.out.println("Certificate ajoute avec succes. ID = " + certificate.getId());
    }

    private static void showAllCertificates(CertificateService service) throws SQLException {
        List<Certificate> items = service.getAll();
        printList(items, "Aucun certificate trouve.");
    }

    private static void showCertificateById(Scanner scanner, CertificateService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du certificate : ");
        printEntity(service.getById(id), "Certificate introuvable.");
    }

    private static void updateCertificate(Scanner scanner, CertificateService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du certificate a modifier : ");
        Certificate existing = service.getById(id);
        if (existing == null) {
            System.out.println("Certificate introuvable.");
            return;
        }

        Certificate updated = readCertificateData(scanner, existing);
        updated.setId(id);
        System.out.println(service.update(updated) ? "Certificate modifie avec succes." : "Modification echouee.");
    }

    private static void deleteCertificate(Scanner scanner, CertificateService service) throws SQLException {
        int id = readInt(scanner, "Entrer l'id du certificate a supprimer : ");
        System.out.println(service.delete(id) ? "Certificate supprime avec succes." : "Aucun certificate supprime.");
    }

    private static Certificate readCertificateData(Scanner scanner, Certificate certificate) {
        System.out.print("Certificate code : ");
        certificate.setCertificateCode(scanner.nextLine().trim());
        certificate.setIssuedAt(readNullableDateTime(scanner, "Issued at (yyyy-MM-dd HH:mm, vide pour null) : "));
        System.out.print("PDF path : ");
        certificate.setPdfPath(scanner.nextLine().trim());
        System.out.print("Student name : ");
        certificate.setStudentName(scanner.nextLine().trim());
        System.out.print("Course title : ");
        certificate.setCourseTitle(scanner.nextLine().trim());
        certificate.setEnrollmentId(readNullableInt(scanner, "Enrollment ID (vide pour null) : "));
        return certificate;
    }

    private static void printList(List<?> items, String emptyMessage) {
        if (items.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }
        for (Object item : items) {
            System.out.println(item);
        }
    }

    private static void printEntity(Object item, String notFoundMessage) {
        if (item == null) {
            System.out.println(notFoundMessage);
        } else {
            System.out.println(item);
        }
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

    private static Integer readNullableInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Valeur numerique invalide.");
            }
        }
    }

    private static Short readNullableShort(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                return Short.parseShort(input);
            } catch (NumberFormatException e) {
                System.out.println("Valeur numerique invalide.");
            }
        }
    }

    private static LocalDateTime readNullableDateTime(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(input, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException e) {
                System.out.println("Format invalide. Utilisez yyyy-MM-dd HH:mm");
            }
        }
    }
}
