package tn.esprit.tools;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists a user's skill graph as JSON in ~/.skillora/skills_<userId>.json.
 * Falls back to defaults the first time a user opens the screen.
 */
public final class SkillStore {

    public static class Skill {
        public String name;
        public int progress;
        public String desc;
        public Skill() {}
        public Skill(String name, int progress, String desc) {
            this.name = name; this.progress = progress; this.desc = desc;
        }
    }

    public static class Category {
        public String name;
        public String colorHex;
        public final List<Skill> skills = new ArrayList<>();
        public Category() {}
        public Category(String name, String colorHex) { this.name = name; this.colorHex = colorHex; }
    }

    private SkillStore() {}

    private static Path file(int userId) {
        return Paths.get(System.getProperty("user.home"), ".skillora", "skills_" + userId + ".json");
    }

    public static List<Category> load(int userId) {
        Path f = file(userId);
        if (!Files.exists(f)) {
            List<Category> defaults = defaults();
            try { save(userId, defaults); } catch (IOException ignored) {}
            return defaults;
        }
        try {
            String json = Files.readString(f);
            return parse(json);
        } catch (Exception e) {
            return defaults();
        }
    }

    public static void save(int userId, List<Category> categories) throws IOException {
        Path f = file(userId);
        Files.createDirectories(f.getParent());
        Files.writeString(f, serialize(categories));
    }

    private static String serialize(List<Category> categories) {
        JSONArray arr = new JSONArray();
        for (Category c : categories) {
            JSONObject co = new JSONObject();
            co.put("name", c.name);
            co.put("color", c.colorHex);
            JSONArray skills = new JSONArray();
            for (Skill s : c.skills) {
                JSONObject so = new JSONObject();
                so.put("name", s.name);
                so.put("progress", s.progress);
                so.put("desc", s.desc == null ? "" : s.desc);
                skills.put(so);
            }
            co.put("skills", skills);
            arr.put(co);
        }
        return new JSONObject().put("categories", arr).toString(2);
    }

    private static List<Category> parse(String json) {
        List<Category> out = new ArrayList<>();
        JSONObject root = new JSONObject(json);
        JSONArray cats = root.optJSONArray("categories");
        if (cats == null) return defaults();
        for (int i = 0; i < cats.length(); i++) {
            JSONObject co = cats.getJSONObject(i);
            Category cat = new Category(co.getString("name"), co.optString("color", "#7c3aed"));
            JSONArray skills = co.optJSONArray("skills");
            if (skills != null) {
                for (int j = 0; j < skills.length(); j++) {
                    JSONObject so = skills.getJSONObject(j);
                    cat.skills.add(new Skill(
                            so.getString("name"),
                            so.optInt("progress", 0),
                            so.optString("desc", "")));
                }
            }
            out.add(cat);
        }
        return out;
    }

    private static List<Category> defaults() {
        List<Category> list = new ArrayList<>();
        Category dev = new Category("Development", "#3b82f6");
        dev.skills.add(new Skill("Java", 85, "Solid grasp of OOP, generics, streams."));
        dev.skills.add(new Skill("JavaFX", 60, "FXML, controllers, custom CSS."));
        dev.skills.add(new Skill("Spring", 30, "Beginner. Working through tutorials."));
        dev.skills.add(new Skill("Web (JS)", 75, "HTML, CSS, vanilla JS, basic React."));
        list.add(dev);

        Category data = new Category("Data", "#22c55e");
        data.skills.add(new Skill("SQL", 70, "Joins, indexes, query optimization."));
        data.skills.add(new Skill("Python", 50, "Numpy, pandas basics."));
        data.skills.add(new Skill("ML basics", 15, "Just started."));
        list.add(data);

        Category design = new Category("Design", "#a855f7");
        design.skills.add(new Skill("UI / UX", 60, "Wireframing and user flows."));
        design.skills.add(new Skill("Figma", 40, "Component basics."));
        list.add(design);

        Category soft = new Category("Soft Skills", "#f59e0b");
        soft.skills.add(new Skill("Communication", 85, "Presentation and writing."));
        soft.skills.add(new Skill("Teamwork", 80, "Leading group projects."));
        soft.skills.add(new Skill("English", 75, "B2 / C1 level."));
        list.add(soft);

        return list;
    }
}
