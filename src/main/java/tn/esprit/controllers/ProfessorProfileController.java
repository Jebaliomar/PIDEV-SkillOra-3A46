package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.json.JSONArray;
import org.json.JSONObject;
import tn.esprit.entities.User;
import tn.esprit.tools.Avatar3D;
import tn.esprit.tools.LinkedInProfile;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ProfessorProfileController implements Initializable {

    @FXML private Label avatarInitials;
    @FXML private Circle avatarRing;
    @FXML private StackPane avatarHolder;
    @FXML private Label fullNameLabel;
    @FXML private Label headlineLabel;
    @FXML private Label roleBadge;
    @FXML private Label universityLabel;
    @FXML private Label countryLabel;
    @FXML private Label bioLabel;
    @FXML private Label emailLabel;
    @FXML private Label phoneLabel;
    @FXML private Label linkedInLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Button linkedInButton;
    @FXML private FlowPane expertiseTags;
    @FXML private FlowPane extrasTags;
    @FXML private VBox indexBox;
    @FXML private VBox experienceSection;
    @FXML private VBox experienceList;
    @FXML private VBox educationSection;
    @FXML private VBox educationList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = ProfessorLayoutController.getCurrentUser();
        if (u == null) return;

        String first = safe(u.getFirstName());
        String last = safe(u.getLastName());
        fullNameLabel.setText(("Prof. " + first + " " + last).trim());

        String initials = "";
        if (!first.isEmpty()) initials += first.charAt(0);
        if (!last.isEmpty()) initials += last.charAt(0);
        avatarInitials.setText(initials.toUpperCase());

        String avatarType = u.getAvatarType();
        if (avatarType != null && !avatarType.isEmpty()) {
            try {
                StackPane viewer = Avatar3D.buildViewer(avatarType, 200, 240);
                avatarHolder.getChildren().setAll(viewer);
                avatarInitials.setVisible(false);
                avatarInitials.setManaged(false);
                avatarRing.setVisible(false);
                avatarRing.setManaged(false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        headlineLabel.setText(nonEmpty(u.getFieldOfStudy(), "Faculty member at SkillORA"));
        universityLabel.setText(nonEmpty(u.getUniversity(), "—"));
        countryLabel.setText(nonEmpty(u.getCountry(), "—"));
        bioLabel.setText(nonEmpty(u.getBio(), "No bio yet. Import your profile from LinkedIn in Settings."));
        emailLabel.setText(safe(u.getEmail()));
        phoneLabel.setText(nonEmpty(u.getPhone(), "Not set"));

        String linkedIn = LinkedInProfile.getUrl(u.getId());
        if (linkedIn == null || linkedIn.isBlank()) {
            linkedInLabel.setText("Not linked");
            if (linkedInButton != null) linkedInButton.setDisable(true);
        } else {
            linkedInLabel.setText(shorten(linkedIn));
        }

        memberSinceLabel.setText(u.getCreatedAt() != null
                ? u.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                : "—");

        Platform.runLater(() -> populateFromLinkedIn(u, safe(u.getFieldOfStudy())));
    }

    private void populateFromLinkedIn(User u, String fallbackField) {
        String json = LinkedInProfile.loadData(u.getId());
        List<String> skills = new ArrayList<>();
        List<String> extras = new ArrayList<>();

        int experienceCount = 0;
        int educationCount = 0;
        int skillCount = 0;
        String topCertOrLang = "—";

        if (json != null) {
            try {
                JSONObject o = new JSONObject(json);
                experienceCount = renderExperiences(o.optJSONArray("experiences"));
                educationCount = renderEducation(o.optJSONArray("education"));
                JSONArray sk = o.optJSONArray("skills");
                if (sk != null) for (int i = 0; i < sk.length(); i++) {
                    String s = sk.optString(i, null);
                    if (s != null && !s.isBlank()) skills.add(s);
                }
                skillCount = skills.size();
                JSONArray certs = o.optJSONArray("certifications");
                if (certs != null) for (int i = 0; i < certs.length(); i++) {
                    JSONObject c = certs.optJSONObject(i);
                    if (c != null) {
                        String name = c.optString("name", null);
                        if (name != null && !name.isBlank()) extras.add(name);
                    }
                }
                JSONArray langs = o.optJSONArray("languages_and_proficiencies");
                if (langs == null) langs = o.optJSONArray("languages");
                if (langs != null) for (int i = 0; i < langs.length(); i++) {
                    Object item = langs.opt(i);
                    String name = null;
                    if (item instanceof String s) name = s;
                    else if (item instanceof JSONObject jo) name = jo.optString("name", null);
                    if (name != null && !name.isBlank()) extras.add(name);
                }
            } catch (Exception ignored) {
            }
        }

        // Index ledger — populated dynamically from imported data, with sensible defaults
        if (indexBox != null) {
            indexBox.getChildren().clear();
            addLedger("01", "Experience", experienceCount > 0 ? String.valueOf(experienceCount) : "—");
            addLedger("02", "Education", educationCount > 0 ? String.valueOf(educationCount) : "—");
            addLedger("03", "Skills", skillCount > 0 ? String.valueOf(skillCount) : "—");
            addLedger("04", "Languages", extras.isEmpty() ? "—" : String.valueOf(extras.size()));
        }

        // Expertise tags: skills if available, else fallback chips
        if (expertiseTags != null) {
            expertiseTags.getChildren().clear();
            List<String> tags = !skills.isEmpty()
                    ? skills.subList(0, Math.min(20, skills.size()))
                    : List.of(fallbackField.isEmpty() ? "Faculty" : fallbackField,
                              "Mentoring", "Research", "Pedagogy");
            for (String t : tags) expertiseTags.getChildren().add(makeTag(t));
        }

        // Languages & certifications
        if (extrasTags != null) {
            extrasTags.getChildren().clear();
            if (extras.isEmpty()) {
                Label l = new Label("None imported yet.");
                l.getStyleClass().add("pf-muted-small");
                extrasTags.getChildren().add(l);
            } else {
                for (String e : extras) extrasTags.getChildren().add(makeTag(e));
            }
        }
    }

    private int renderExperiences(JSONArray arr) {
        if (arr == null || arr.length() == 0 || experienceList == null) return 0;
        experienceList.getChildren().clear();
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e == null) continue;
            String title = e.optString("title", "");
            String company = e.optString("company", "");
            String range = formatRange(e.optJSONObject("starts_at"), e.optJSONObject("ends_at"));
            String desc = e.optString("description", null);
            experienceList.getChildren().add(buildEntry(title, company, range, desc));
            count++;
        }
        if (experienceSection != null && count > 0) {
            experienceSection.setVisible(true);
            experienceSection.setManaged(true);
        }
        return count;
    }

    private int renderEducation(JSONArray arr) {
        if (arr == null || arr.length() == 0 || educationList == null) return 0;
        educationList.getChildren().clear();
        int count = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e == null) continue;
            String school = e.optString("school", "");
            String degree = joinNonBlank(" · ", e.optString("degree_name", null), e.optString("field_of_study", null));
            String range = formatRange(e.optJSONObject("starts_at"), e.optJSONObject("ends_at"));
            educationList.getChildren().add(buildEntry(school, degree, range, null));
            count++;
        }
        if (educationSection != null && count > 0) {
            educationSection.setVisible(true);
            educationSection.setManaged(true);
        }
        return count;
    }

    private VBox buildEntry(String title, String subtitle, String range, String description) {
        VBox box = new VBox(4);
        Label t = new Label(nonEmpty(title, "—"));
        t.getStyleClass().add("pf-skill-name");
        Label s = new Label(nonEmpty(subtitle, ""));
        s.getStyleClass().add("pf-origin");
        Label r = new Label(nonEmpty(range, ""));
        r.getStyleClass().add("pf-muted-small");
        box.getChildren().addAll(t, s, r);
        if (description != null && !description.isBlank()) {
            Label d = new Label(description.length() > 400 ? description.substring(0, 397) + "…" : description);
            d.getStyleClass().add("pf-quote");
            d.setWrapText(true);
            box.getChildren().add(d);
        }
        return box;
    }

    private void addLedger(String num, String key, String val) {
        HBox row = new HBox(24);
        row.getStyleClass().add("pf-ledger-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label n = new Label(num); n.getStyleClass().add("pf-ledger-num");
        Label k = new Label(key); k.getStyleClass().add("pf-ledger-key");
        Region spacer = new Region(); HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label v = new Label(val); v.getStyleClass().add("pf-ledger-val");
        row.getChildren().addAll(n, k, spacer, v);
        Region hairline = new Region();
        hairline.getStyleClass().add("pf-hairline");
        hairline.setPrefHeight(1); hairline.setMaxHeight(1);
        indexBox.getChildren().addAll(row, hairline);
    }

    private Label makeTag(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("pf-tag");
        return l;
    }

    private String formatRange(JSONObject start, JSONObject end) {
        String s = formatDate(start);
        String e = end == null ? "Present" : formatDate(end);
        if (s == null && e == null) return "";
        if (s == null) return e;
        if (e == null) return s + " — Present";
        return s + " — " + e;
    }

    private String formatDate(JSONObject d) {
        if (d == null) return null;
        Integer year = d.has("year") && !d.isNull("year") ? d.optInt("year") : null;
        Integer month = d.has("month") && !d.isNull("month") ? d.optInt("month") : null;
        if (year == null) return null;
        if (month == null) return String.valueOf(year);
        String[] mn = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int mi = Math.max(1, Math.min(12, month)) - 1;
        return mn[mi] + " " + year;
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (sb.length() > 0) sb.append(sep);
            sb.append(p);
        }
        return sb.toString();
    }

    @FXML
    public void goToSettings() {
        ProfessorLayoutController.getInstance().showSettings();
    }

    @FXML
    public void openLinkedIn() {
        User u = ProfessorLayoutController.getCurrentUser();
        if (u == null) return;
        String url = LinkedInProfile.getUrl(u.getId());
        if (url == null || url.isBlank()) return;
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ignored) {
        }
    }

    private String safe(String s) { return s == null ? "" : s; }
    private String nonEmpty(String s, String fb) { return s == null || s.isBlank() ? fb : s; }
    private String shorten(String url) {
        return url.length() > 40 ? url.substring(0, 37) + "…" : url;
    }
}
