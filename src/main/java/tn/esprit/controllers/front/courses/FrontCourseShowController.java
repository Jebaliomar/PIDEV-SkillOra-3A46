package tn.esprit.controllers.front.courses;

import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.front.FrontShellAware;
import tn.esprit.controllers.front.FrontShellController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Lesson;
import tn.esprit.services.CourseSectionService;
import tn.esprit.services.LessonService;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FrontCourseShowController implements FrontShellAware {

    @FXML
    private Label courseTitleLabel;

    @FXML
    private Label categoryLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private ImageView thumbnailImageView;

    @FXML
    private Label thumbnailFallbackLabel;

    @FXML
    private Accordion sectionsAccordion;

    @FXML
    private ScrollPane rootScrollPane;

    private FrontShellController shellController;
    private CourseSectionService courseSectionService;
    private LessonService lessonService;
    private Course course;

    @Override
    public void setShellController(FrontShellController shellController) {
        this.shellController = shellController;
    }

    public void setCourse(Course course) {
        this.course = course;
        populateCourseHeader();
        loadCurriculum();
    }

    private void populateCourseHeader() {
        if (course == null) {
            return;
        }
        courseTitleLabel.setText(safeValue(course.getTitle(), "Untitled Course"));
        categoryLabel.setText(safeValue(course.getCategory(), "Uncategorized"));
        descriptionLabel.setText(safeValue(course.getDescription(), "No description is available for this course yet."));
        Image image = loadThumbnail(course.getThumbnail());
        thumbnailImageView.setImage(image);
        thumbnailImageView.setVisible(image != null);
        thumbnailImageView.setManaged(image != null);
        thumbnailFallbackLabel.setVisible(image == null);
        thumbnailFallbackLabel.setManaged(image == null);
    }

    private void loadCurriculum() {
        sectionsAccordion.getPanes().clear();
        if (course == null || course.getId() == null) {
            return;
        }

        try {
            List<CourseSection> sections = getCourseSectionService().getAll().stream()
                    .filter(section -> course.getId().equals(section.getCourseId()))
                    .sorted(Comparator.comparing(CourseSection::getPosition, Comparator.nullsLast(Integer::compareTo))
                            .thenComparing(CourseSection::getId, Comparator.nullsLast(Integer::compareTo)))
                    .toList();

            if (sections.isEmpty()) {
                sectionsAccordion.getPanes().add(buildEmptyPane("No sections yet", "This course does not contain any sections."));
                return;
            }

            Map<Integer, List<Lesson>> lessonsBySection = loadLessonsForSections(sections);
            for (int index = 0; index < sections.size(); index++) {
                CourseSection section = sections.get(index);
                TitledPane pane = buildSectionPane(section, lessonsBySection.getOrDefault(section.getId(), List.of()));
                sectionsAccordion.getPanes().add(pane);
                if (index == 0) {
                    sectionsAccordion.setExpandedPane(pane);
                }
            }
        } catch (SQLException | IllegalStateException e) {
            sectionsAccordion.getPanes().add(buildEmptyPane("Unable to load curriculum", e.getMessage()));
        }
    }

    private Map<Integer, List<Lesson>> loadLessonsForSections(List<CourseSection> sections) throws SQLException {
        Set<Integer> sectionIds = sections.stream()
                .map(CourseSection::getId)
                .collect(Collectors.toSet());

        return getLessonService().getAll().stream()
                .filter(lesson -> lesson.getSectionId() != null && sectionIds.contains(lesson.getSectionId()))
                .sorted(Comparator.comparing(Lesson::getPosition, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(Lesson::getId, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.groupingBy(Lesson::getSectionId, Collectors.mapping(Function.identity(), Collectors.toList())));
    }

    private TitledPane buildSectionPane(CourseSection section, List<Lesson> lessons) {
        VBox lessonList = new VBox(10);
        lessonList.getStyleClass().add("front-section-lessons");

        if (lessons.isEmpty()) {
            Label emptyLabel = new Label("No lessons in this section yet.");
            emptyLabel.getStyleClass().add("front-section-empty");
            lessonList.getChildren().add(emptyLabel);
        } else {
            lessons.forEach(lesson -> lessonList.getChildren().add(buildLessonRow(lesson)));
        }

        TitledPane pane = new TitledPane(section.getTitle(), lessonList);
        pane.getStyleClass().add("front-section-pane");
        pane.setAnimated(true);
        return pane;
    }

    private HBox buildLessonRow(Lesson lesson) {
        Label iconLabel = new Label(typeBadge(lesson.getType()));
        iconLabel.getStyleClass().add("front-lesson-icon");

        Label titleLabel = new Label(safeValue(lesson.getTitle(), "Untitled Lesson"));
        titleLabel.getStyleClass().add("front-lesson-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label actionLabel = new Label("Open");
        actionLabel.getStyleClass().add("front-lesson-action");

        HBox row = new HBox(12, iconLabel, titleLabel, spacer, actionLabel);
        row.getStyleClass().add("front-lesson-row");
        row.setOnMouseClicked(event -> openLesson(lesson));
        return row;
    }

    private TitledPane buildEmptyPane(String title, String body) {
        Label label = new Label(body == null || body.isBlank() ? "No additional details." : body);
        label.setWrapText(true);
        label.getStyleClass().add("front-section-empty");
        TitledPane pane = new TitledPane(title, new VBox(label));
        pane.setExpanded(true);
        pane.getStyleClass().add("front-section-pane");
        return pane;
    }

    private void openLesson(Lesson lesson) {
        if (shellController != null && course != null && lesson != null) {
            shellController.showCourseConsume(course, lesson);
        }
    }

    private String typeBadge(String type) {
        if (type == null) {
            return "TXT";
        }
        return switch (type.toUpperCase()) {
            case "VIDEO" -> "VID";
            case "PDF" -> "PDF";
            default -> "TXT";
        };
    }

    private Image loadThumbnail(String thumbnail) {
        if (thumbnail == null || thumbnail.isBlank()) {
            return null;
        }
        try {
            if (thumbnail.startsWith("http://") || thumbnail.startsWith("https://")) {
                return new Image(thumbnail, true);
            }
            File file = new File(thumbnail);
            String imageUrl = file.exists() ? file.toURI().toString() : new File(thumbnail.replace("/", File.separator)).toURI().toString();
            return new Image(imageUrl, true);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private CourseSectionService getCourseSectionService() {
        if (courseSectionService == null) {
            courseSectionService = new CourseSectionService();
        }
        return courseSectionService;
    }

    private LessonService getLessonService() {
        if (lessonService == null) {
            lessonService = new LessonService();
        }
        return lessonService;
    }
}
