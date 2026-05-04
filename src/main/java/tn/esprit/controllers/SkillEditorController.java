package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;
import tn.esprit.tools.Loaders;
import tn.esprit.tools.SkillStore;
import tn.esprit.tools.ThemeManager;

import java.util.List;
import java.util.Optional;

public class SkillEditorController {

    public enum Action { SAVED, DELETED, CANCELLED }

    public static class Result {
        public final Action action;
        public final SkillStore.Category category;
        public final SkillStore.Skill skill;
        public Result(Action a, SkillStore.Category c, SkillStore.Skill s) {
            action = a; category = c; skill = s;
        }
    }

    @FXML private Label titleLabel;
    @FXML private ComboBox<SkillStore.Category> categoryCombo;
    @FXML private TextField nameField;
    @FXML private Slider progressSlider;
    @FXML private Label progressLabel;
    @FXML private TextArea descArea;
    @FXML private Label errorLabel;
    @FXML private Button deleteBtn;

    private Stage stage;
    private List<SkillStore.Category> categories;
    private SkillStore.Category editingCategory;
    private SkillStore.Skill editingSkill;
    private Result result = new Result(Action.CANCELLED, null, null);

    @FXML
    private void initialize() {
        progressSlider.valueProperty().addListener((o, a, v) ->
                progressLabel.setText((int) v.doubleValue() + "%"));
        categoryCombo.setConverter(new StringConverter<>() {
            @Override public String toString(SkillStore.Category c) { return c == null ? "" : c.name; }
            @Override public SkillStore.Category fromString(String s) { return null; }
        });
    }

    private void configure(List<SkillStore.Category> cats, SkillStore.Category cat, SkillStore.Skill skill) {
        this.categories = cats;
        this.editingCategory = cat;
        this.editingSkill = skill;
        categoryCombo.setItems(FXCollections.observableArrayList(cats));
        if (cat != null) categoryCombo.setValue(cat);
        else if (!cats.isEmpty()) categoryCombo.setValue(cats.get(0));

        if (skill != null) {
            titleLabel.setText("Edit skill");
            nameField.setText(skill.name);
            progressSlider.setValue(skill.progress);
            progressLabel.setText(skill.progress + "%");
            descArea.setText(skill.desc);
            deleteBtn.setVisible(true);
            deleteBtn.setManaged(true);
        }
    }

    @FXML public void handleCancel() {
        result = new Result(Action.CANCELLED, null, null);
        if (stage != null) stage.close();
    }

    @FXML public void handleDelete() {
        if (editingSkill != null && editingCategory != null) {
            result = new Result(Action.DELETED, editingCategory, editingSkill);
        }
        if (stage != null) stage.close();
    }

    @FXML public void handleSave() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        SkillStore.Category cat = categoryCombo.getValue();
        if (name.isEmpty()) { showError("Skill name is required."); return; }
        if (cat == null)    { showError("Pick a category."); return; }

        int progress = (int) Math.round(progressSlider.getValue());
        String desc = descArea.getText() == null ? "" : descArea.getText().trim();

        if (editingSkill != null) {
            editingSkill.name = name;
            editingSkill.progress = progress;
            editingSkill.desc = desc;
            // Move skill if category changed
            if (editingCategory != cat) {
                editingCategory.skills.remove(editingSkill);
                cat.skills.add(editingSkill);
            }
            result = new Result(Action.SAVED, cat, editingSkill);
        } else {
            SkillStore.Skill ns = new SkillStore.Skill(name, progress, desc);
            cat.skills.add(ns);
            result = new Result(Action.SAVED, cat, ns);
        }
        if (stage != null) stage.close();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    public static Result open(Window owner, List<SkillStore.Category> cats,
                              SkillStore.Category cat, SkillStore.Skill skill) {
        try {
            FXMLLoader loader = Loaders.loader(SkillEditorController.class, "/fxml/SkillEditorDialog.fxml");
            Parent root = loader.load();
            SkillEditorController ctrl = loader.getController();
            ctrl.configure(cats, cat, skill);

            Stage dialog = new Stage(StageStyle.UTILITY);
            dialog.initModality(Modality.APPLICATION_MODAL);
            if (owner != null) dialog.initOwner(owner);
            dialog.setTitle(skill == null ? "Add skill" : "Edit skill");
            dialog.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(SkillEditorController.class.getResource("/styles/style.css").toExternalForm());
            ThemeManager.applyTheme(scene);
            dialog.setScene(scene);
            ctrl.stage = dialog;
            dialog.showAndWait();
            return ctrl.result;
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(Action.CANCELLED, null, null);
        }
    }
}
