package tn.esprit.services;

import tn.esprit.entities.Course;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.Lesson;
import tn.esprit.entities.LessonCompletion;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CourseProgressService {

    private final EnrollmentService enrollmentService;
    private final LessonService lessonService;
    private final LessonCompletionService lessonCompletionService;

    public CourseProgressService() {
        this(new EnrollmentService(), new LessonService(), new LessonCompletionService());
    }

    public CourseProgressService(EnrollmentService enrollmentService, LessonService lessonService, LessonCompletionService lessonCompletionService) {
        this.enrollmentService = enrollmentService;
        this.lessonService = lessonService;
        this.lessonCompletionService = lessonCompletionService;
    }

    public int getTotalLessons(Course course) throws SQLException {
        if (course == null || course.getId() == null) {
            return 0;
        }
        return lessonService.countByCourse(course.getId());
    }

    public int getCompletedLessons(Enrollment enrollment) throws SQLException {
        if (enrollment == null || enrollment.getId() == null) {
            return 0;
        }
        return lessonCompletionService.countByEnrollment(enrollment.getId());
    }

    public int calculateProgress(Enrollment enrollment, Course course) throws SQLException {
        int totalLessons = getTotalLessons(course);
        if (totalLessons <= 0) {
            return 0;
        }
        int completedLessons = Math.min(getCompletedLessons(enrollment), totalLessons);
        return Math.max(0, Math.min(100, (int) Math.floor((completedLessons * 100.0) / totalLessons)));
    }

    public Enrollment recalculateEnrollmentProgress(Enrollment enrollment, Course course) throws SQLException {
        if (enrollment == null) {
            return null;
        }

        int totalLessons = getTotalLessons(course);
        int progress = calculateProgress(enrollment, course);
        enrollment.setProgressPercent((short) progress);
        if (progress >= 100 && totalLessons > 0) {
            enrollment.setStatus("completed");
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
        } else {
            enrollment.setStatus("active");
            enrollment.setCompletedAt(null);
        }

        if (enrollment.getId() != null) {
            enrollmentService.update(enrollment);
        }
        return enrollment;
    }

    public LessonCompletion markLessonCompleted(Enrollment enrollment, Lesson lesson) throws SQLException {
        if (enrollment == null || enrollment.getId() == null || lesson == null || lesson.getId() == null) {
            return null;
        }
        return lessonCompletionService.markCompletedIfMissing(enrollment.getId(), lesson.getId());
    }

    public boolean isLessonCompleted(Enrollment enrollment, Lesson lesson) throws SQLException {
        if (enrollment == null || enrollment.getId() == null || lesson == null || lesson.getId() == null) {
            return false;
        }
        return lessonCompletionService.existsForEnrollmentAndLesson(enrollment.getId(), lesson.getId());
    }

    public Set<Integer> findCompletedLessonIds(Enrollment enrollment) throws SQLException {
        if (enrollment == null || enrollment.getId() == null) {
            return Set.of();
        }
        return new HashSet<>(lessonCompletionService.findCompletedLessonIdsForEnrollment(enrollment.getId()));
    }

    public Lesson getNextUncompletedLesson(Enrollment enrollment, Course course) throws SQLException {
        if (course == null || course.getId() == null) {
            return null;
        }

        List<Lesson> lessons = lessonService.findByCourse(course.getId());
        if (lessons.isEmpty()) {
            return null;
        }

        Set<Integer> completedLessonIds = findCompletedLessonIds(enrollment);
        return lessons.stream()
                .filter(lesson -> lesson.getId() != null && !completedLessonIds.contains(lesson.getId()))
                .findFirst()
                .orElse(lessons.get(0));
    }
}
