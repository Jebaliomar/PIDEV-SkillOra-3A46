package tn.esprit.entities;

public class Tag {

    private Integer id;
    private String name;
    private String slug;

    public Tag() {
    }

    public Tag(Integer id, String name, String slug) {
        this.id = id;
        this.name = name;
        this.slug = slug;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id + ", " + "name=" + name + ", " + "slug=" + slug +
                "}";
    }
}
