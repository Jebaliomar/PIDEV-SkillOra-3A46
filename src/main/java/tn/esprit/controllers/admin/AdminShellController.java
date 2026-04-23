package tn.esprit.controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import tn.esprit.controllers.admin.courses.CourseEditController;
import tn.esprit.controllers.admin.courses.CourseShowController;
import tn.esprit.controllers.admin.lessons.LessonEditController;
import tn.esprit.controllers.admin.lessons.LessonNewController;
import tn.esprit.controllers.admin.lessons.LessonShowController;
import tn.esprit.controllers.admin.sections.SectionEditController;
import tn.esprit.controllers.admin.sections.SectionIndexController;
import tn.esprit.controllers.admin.sections.SectionNewController;
import tn.esprit.controllers.admin.sections.SectionShowController;
import tn.esprit.entities.Course;
import tn.esprit.entities.CourseSection;
import tn.esprit.entities.Lesson;
import tn.esprit.tools.AppNavigator;

import java.io.IOException;

public class AdminShellController {

    @FXML
    private Button coursesModuleButton;

    @FXML
    private Button usersModuleButton;

    @FXML
    private Button eventsModuleButton;

    @FXML
    private Button evaluationsModuleButton;

    @FXML
    private Button forumModuleButton;

    @FXML
    private Button rendezVousModuleButton;

    @FXML
    private StackPane contentContainer;

    @FXML
    private Button frontHomeButton;

    @FXML
    private Button usersAdminButton;

    @FXML
    public void initialize() {
        System.out.println("AdminShellController.initialize() called");
        showCoursesIndex();
    }

    @FXML
    private void handleCoursesModule() {
        System.out.println("AdminShellController.handleCoursesModule() triggered");
        showCoursesIndex();
    }

    @FXML
    private void handleUsersModule() {
        AppNavigator.showUserAdmin(usersModuleButton);
    }

    @FXML
    private void handleEventsModule() {
        setActiveModule(eventsModuleButton);
        showPlaceholder("Events", "This module is not implemented yet in the desktop shell.");
    }

    @FXML
    private void handleEvaluationsModule() {
        setActiveModule(evaluationsModuleButton);
        showPlaceholder("Evaluations", "This module is not implemented yet in the desktop shell.");
    }

    @FXML
    private void handleForumModule() {
        setActiveModule(forumModuleButton);
        showPlaceholder("Forum", "This module is not implemented yet in the desktop shell.");
    }

    @FXML
    private void handleRendezVousModule() {
        setActiveModule(rendezVousModuleButton);
        showPlaceholder("Rendez-vous", "This module is not implemented yet in the desktop shell.");
    }

    @FXML
    private void handleFrontHome() {
        AppNavigator.showFrontHome(frontHomeButton);
    }

    @FXML
    private void handleUsersAdmin() {
        AppNavigator.showUserAdmin(usersAdminButton != null ? usersAdminButton : usersModuleButton);
    }

    public void showCoursesIndex() {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/courses/course_index.fxml", controller -> {
        });
    }

    public void showCourseNew() {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/courses/course_new.fxml", controller -> {
        });
    }

    public void showCourseShow(Course course) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/courses/course_show.fxml", controller -> {
            if (controller instanceof CourseShowController courseShowController) {
                courseShowController.setCourse(course);
            }
        });
    }

    public void showCourseEdit(Course course) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/courses/course_edit.fxml", controller -> {
            if (controller instanceof CourseEditController courseEditController) {
                courseEditController.setCourse(course);
            }
        });
    }

    public void showSectionIndex(Course course) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/sections/section_index.fxml", controller -> {
            if (controller instanceof SectionIndexController sectionIndexController) {
                sectionIndexController.setCourse(course);
            }
        });
    }

    public void showSectionNew(Course course) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/sections/section_new.fxml", controller -> {
            if (controller instanceof SectionNewController sectionNewController) {
                sectionNewController.setCourse(course);
            }
        });
    }

    public void showSectionEdit(Course course, CourseSection section) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/sections/section_edit.fxml", controller -> {
            if (controller instanceof SectionEditController sectionEditController) {
                sectionEditController.setCourse(course);
                sectionEditController.setSection(section);
            }
        });
    }

    public void showSectionShow(Course course, CourseSection section) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/sections/section_show.fxml", controller -> {
            if (controller instanceof SectionShowController sectionShowController) {
                sectionShowController.setCourse(course);
                sectionShowController.setSection(section);
            }
        });
    }

    public void showLessonNew(Course course, CourseSection section) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/lessons/lesson_new.fxml", controller -> {
            if (controller instanceof LessonNewController lessonNewController) {
                lessonNewController.setCourse(course);
                lessonNewController.setSection(section);
            }
        });
    }

    public void showLessonEdit(Course course, CourseSection section, Lesson lesson) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/lessons/lesson_edit.fxml", controller -> {
            if (controller instanceof LessonEditController lessonEditController) {
                lessonEditController.setCourse(course);
                lessonEditController.setSection(section);
                lessonEditController.setLesson(lesson);
            }
        });
    }

    public void showLessonShow(Course course, CourseSection section, Lesson lesson) {
        setActiveModule(coursesModuleButton);
        loadContent("/views/admin/lessons/lesson_show.fxml", controller -> {
            if (controller instanceof LessonShowController lessonShowController) {
                lessonShowController.setCourse(course);
                lessonShowController.setSection(section);
                lessonShowController.setLesson(lesson);
            }
        });
    }

    private void showPlaceholder(String title, String message) {
        VBox placeholder = new VBox(10);
        placeholder.getStyleClass().add("placeholder-panel");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("placeholder-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("placeholder-subtitle");
        messageLabel.setWrapText(true);
        placeholder.getChildren().addAll(titleLabel, messageLabel);
        contentContainer.getChildren().setAll(placeholder);
    }

    private void loadContent(String resourcePath, ControllerConfigurer controllerConfigurer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Node content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AdminShellAware shellAware) {
                shellAware.setShellController(this);
            }
            controllerConfigurer.configure(controller);
            contentContainer.getChildren().setAll(content);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to load view");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void setActiveModule(Button activeButton) {
        clearModuleSelection(coursesModuleButton);
        clearModuleSelection(usersModuleButton);
        clearModuleSelection(eventsModuleButton);
        clearModuleSelection(evaluationsModuleButton);
        clearModuleSelection(forumModuleButton);
        clearModuleSelection(rendezVousModuleButton);

        if (activeButton != null && !activeButton.getStyleClass().contains("sidebar-item-selected")) {
            activeButton.getStyleClass().remove("sidebar-item");
            activeButton.getStyleClass().add("sidebar-item-selected");
        }
    }

    private void clearModuleSelection(Button button) {
        button.getStyleClass().remove("sidebar-item-selected");
        if (!button.getStyleClass().contains("sidebar-item")) {
            button.getStyleClass().add("sidebar-item");
        }
    }

    @FunctionalInterface
    private interface ControllerConfigurer {
        void configure(Object controller);
    }
}
