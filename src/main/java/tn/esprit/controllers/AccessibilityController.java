package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import tn.esprit.tools.AccessibilityManager;
import tn.esprit.tools.AccessibilityManager.ColorBlindMode;
import tn.esprit.tools.AccessibilityManager.ColorFilter;
import tn.esprit.tools.AccessibilityManager.Density;
import tn.esprit.tools.AccessibilityManager.FontFamily;
import tn.esprit.tools.AccessibilityManager.FontScale;
import tn.esprit.tools.AccessibilityManager.Preset;
import tn.esprit.tools.LanguageManager;
import tn.esprit.tools.LanguageManager.Language;
import tn.esprit.tools.ThemeManager;

import java.io.File;
import java.nio.file.Path;

import java.net.URL;
import java.util.ResourceBundle;

public class AccessibilityController implements Initializable {

    @FXML private ComboBox<FontScale> fontScaleCombo;
    @FXML private ComboBox<FontFamily> fontFamilyCombo;
    @FXML private ComboBox<Density> densityCombo;
    @FXML private ComboBox<ColorBlindMode> colorBlindCombo;
    @FXML private CheckBox highContrastCheck;
    @FXML private CheckBox focusRingCheck;
    @FXML private CheckBox reducedMotionCheck;
    @FXML private CheckBox soundCuesCheck;
    @FXML private CheckBox shortcutsCheck;
    @FXML private ColorPicker accentPicker;
    @FXML private ColorPicker backgroundPicker;
    @FXML private Slider zoomSlider;
    @FXML private Label zoomValueLabel;
    @FXML private ComboBox<ColorFilter> colorFilterCombo;
    @FXML private CheckBox autoDarkCheck;
    @FXML private ComboBox<Language> languageCombo;

    private boolean suppressBgListener = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fontScaleCombo.setItems(FXCollections.observableArrayList(FontScale.values()));
        fontScaleCombo.setConverter(labelConverter(v -> ((FontScale) v).label));
        fontFamilyCombo.setItems(FXCollections.observableArrayList(FontFamily.values()));
        fontFamilyCombo.setConverter(labelConverter(v -> ((FontFamily) v).label));
        densityCombo.setItems(FXCollections.observableArrayList(Density.values()));
        densityCombo.setConverter(labelConverter(v -> ((Density) v).label));
        colorBlindCombo.setItems(FXCollections.observableArrayList(ColorBlindMode.values()));
        colorBlindCombo.setConverter(labelConverter(v -> ((ColorBlindMode) v).label));
        colorFilterCombo.setItems(FXCollections.observableArrayList(ColorFilter.values()));
        colorFilterCombo.setConverter(labelConverter(v -> ((ColorFilter) v).label));
        languageCombo.setItems(FXCollections.observableArrayList(Language.values()));
        languageCombo.setConverter(labelConverter(v -> ((Language) v).label));
        languageCombo.setValue(LanguageManager.getCurrent());
        languageCombo.valueProperty().addListener((o, a, v) -> {
            if (v == null || v == LanguageManager.getCurrent()) return;
            LanguageManager.setLanguage(v);
            AdminPanelController inst = AdminPanelController.getInstance();
            if (inst != null) inst.reloadShell();
            else {
                Scene scene = languageCombo.getScene();
                LanguageManager.applyToScene(scene);
            }
        });

        // Load current values
        fontScaleCombo.setValue(AccessibilityManager.getFontScale());
        fontFamilyCombo.setValue(AccessibilityManager.getFontFamily());
        densityCombo.setValue(AccessibilityManager.getDensity());
        colorBlindCombo.setValue(AccessibilityManager.getColorBlind());
        highContrastCheck.setSelected(AccessibilityManager.isHighContrast());
        focusRingCheck.setSelected(AccessibilityManager.isFocusRing());
        reducedMotionCheck.setSelected(AccessibilityManager.isReducedMotion());
        soundCuesCheck.setSelected(AccessibilityManager.isSoundCues());
        shortcutsCheck.setSelected(AccessibilityManager.isShortcuts());
        accentPicker.setValue(parseColor(AccessibilityManager.getAccentColor(), Color.web("#7C3AED")));
        String bg = AccessibilityManager.getBackgroundColor();
        if (bg != null && !bg.isBlank()) backgroundPicker.setValue(parseColor(bg, Color.WHITE));
        zoomSlider.setValue(AccessibilityManager.getZoom());
        updateZoomLabel(AccessibilityManager.getZoom());
        colorFilterCombo.setValue(AccessibilityManager.getColorFilter());
        autoDarkCheck.setSelected(AccessibilityManager.isAutoDark());

        // Live listeners
        fontScaleCombo.valueProperty().addListener((o, a, v) -> { if (v != null) { AccessibilityManager.setFontScale(v); refresh(); } });
        fontFamilyCombo.valueProperty().addListener((o, a, v) -> { if (v != null) { AccessibilityManager.setFontFamily(v); refresh(); } });
        densityCombo.valueProperty().addListener((o, a, v) -> { if (v != null) { AccessibilityManager.setDensity(v); refresh(); } });
        colorBlindCombo.valueProperty().addListener((o, a, v) -> { if (v != null) { AccessibilityManager.setColorBlind(v); refresh(); } });
        highContrastCheck.selectedProperty().addListener((o, a, v) -> { AccessibilityManager.setHighContrast(v); refresh(); });
        focusRingCheck.selectedProperty().addListener((o, a, v) -> { AccessibilityManager.setFocusRing(v); refresh(); });
        reducedMotionCheck.selectedProperty().addListener((o, a, v) -> { AccessibilityManager.setReducedMotion(v); refresh(); });
        soundCuesCheck.selectedProperty().addListener((o, a, v) -> AccessibilityManager.setSoundCues(v));
        shortcutsCheck.selectedProperty().addListener((o, a, v) -> AccessibilityManager.setShortcuts(v));
        accentPicker.valueProperty().addListener((o, a, v) -> { if (v != null) { AccessibilityManager.setAccentColor(toHex(v)); refresh(); } });
        backgroundPicker.valueProperty().addListener((o, a, v) -> {
            if (suppressBgListener) return;
            if (v != null) { AccessibilityManager.setBackgroundColor(toHex(v)); refresh(); }
        });
        zoomSlider.valueProperty().addListener((o, a, v) -> {
            double z = Math.round(v.doubleValue() * 20.0) / 20.0;
            AccessibilityManager.setZoom(z);
            updateZoomLabel(z);
            refresh();
        });
        colorFilterCombo.valueProperty().addListener((o, a, v) -> { if (v != null) { AccessibilityManager.setColorFilter(v); refresh(); } });
        autoDarkCheck.selectedProperty().addListener((o, a, v) -> {
            AccessibilityManager.setAutoDark(v);
            Scene scene = autoDarkCheck.getScene();
            if (v) AccessibilityManager.installAutoDarkSchedule(scene);
            else AccessibilityManager.installAutoDarkSchedule(null);
        });
    }

    @FXML public void presetDay() { applyPreset(Preset.DAY); }
    @FXML public void presetNight() { applyPreset(Preset.NIGHT); }
    @FXML public void presetReading() { applyPreset(Preset.READING); }
    @FXML public void presetPresentation() { applyPreset(Preset.PRESENTATION); }

    private void applyPreset(Preset p) {
        AccessibilityManager.applyPreset(p);
        syncControlsFromManager();
        refresh();
    }

    @FXML public void handleExport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export accessibility settings");
        fc.setInitialFileName("skillora-accessibility.properties");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties", "*.properties"));
        File f = fc.showSaveDialog(autoDarkCheck.getScene().getWindow());
        if (f == null) return;
        try { AccessibilityManager.exportTo(f.toPath()); }
        catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void handleImport() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import accessibility settings");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Properties", "*.properties"));
        File f = fc.showOpenDialog(autoDarkCheck.getScene().getWindow());
        if (f == null) return;
        try {
            AccessibilityManager.importFrom(f.toPath());
            syncControlsFromManager();
            refresh();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void syncControlsFromManager() {
        fontScaleCombo.setValue(AccessibilityManager.getFontScale());
        fontFamilyCombo.setValue(AccessibilityManager.getFontFamily());
        densityCombo.setValue(AccessibilityManager.getDensity());
        colorBlindCombo.setValue(AccessibilityManager.getColorBlind());
        colorFilterCombo.setValue(AccessibilityManager.getColorFilter());
        highContrastCheck.setSelected(AccessibilityManager.isHighContrast());
        focusRingCheck.setSelected(AccessibilityManager.isFocusRing());
        reducedMotionCheck.setSelected(AccessibilityManager.isReducedMotion());
        soundCuesCheck.setSelected(AccessibilityManager.isSoundCues());
        shortcutsCheck.setSelected(AccessibilityManager.isShortcuts());
        autoDarkCheck.setSelected(AccessibilityManager.isAutoDark());
        accentPicker.setValue(parseColor(AccessibilityManager.getAccentColor(), Color.web("#7C3AED")));
        String bg = AccessibilityManager.getBackgroundColor();
        suppressBgListener = true;
        if (bg != null && !bg.isBlank()) backgroundPicker.setValue(parseColor(bg, Color.WHITE));
        else backgroundPicker.setValue(Color.WHITE);
        suppressBgListener = false;
        zoomSlider.setValue(AccessibilityManager.getZoom());
        updateZoomLabel(AccessibilityManager.getZoom());
    }

    @FXML public void handleClearBackground() {
        suppressBgListener = true;
        backgroundPicker.setValue(Color.WHITE);
        suppressBgListener = false;
        AccessibilityManager.setBackgroundColor("");
        refresh();
    }

    @FXML public void applySwatchPurple() { setAccent("#7C3AED"); }
    @FXML public void applySwatchBlue()   { setAccent("#0EA5E9"); }
    @FXML public void applySwatchGreen()  { setAccent("#22C55E"); }
    @FXML public void applySwatchOrange() { setAccent("#F59E0B"); }
    @FXML public void applySwatchRed()    { setAccent("#EF4444"); }
    @FXML public void applySwatchPink()   { setAccent("#EC4899"); }

    private void setAccent(String hex) {
        accentPicker.setValue(Color.web(hex));
    }

    @FXML
    public void handleReset() {
        AccessibilityManager.resetDefaults();
        fontScaleCombo.setValue(AccessibilityManager.getFontScale());
        fontFamilyCombo.setValue(AccessibilityManager.getFontFamily());
        densityCombo.setValue(AccessibilityManager.getDensity());
        colorBlindCombo.setValue(AccessibilityManager.getColorBlind());
        highContrastCheck.setSelected(AccessibilityManager.isHighContrast());
        focusRingCheck.setSelected(AccessibilityManager.isFocusRing());
        reducedMotionCheck.setSelected(AccessibilityManager.isReducedMotion());
        soundCuesCheck.setSelected(AccessibilityManager.isSoundCues());
        shortcutsCheck.setSelected(AccessibilityManager.isShortcuts());
        accentPicker.setValue(Color.web(AccessibilityManager.DEFAULT_ACCENT));
        backgroundPicker.setValue(Color.WHITE);
        zoomSlider.setValue(1.0);
        refresh();
    }

    private void refresh() {
        Scene scene = fontScaleCombo.getScene();
        if (scene != null) ThemeManager.applyTheme(scene);
    }

    private void updateZoomLabel(double z) {
        zoomValueLabel.setText(Math.round(z * 100) + "%");
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private Color parseColor(String hex, Color fallback) {
        try { return Color.web(hex); } catch (Exception e) { return fallback; }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private <T> StringConverter<T> labelConverter(java.util.function.Function<Object, String> fn) {
        return new StringConverter<T>() {
            @Override public String toString(T v) { return v == null ? "" : fn.apply(v); }
            @Override public T fromString(String s) { return null; }
        };
    }
}
