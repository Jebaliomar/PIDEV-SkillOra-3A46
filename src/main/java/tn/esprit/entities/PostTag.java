package tn.esprit.entities;

public class PostTag {

    private Integer postId;
    private Integer tagId;

    public PostTag() {
    }

    public PostTag(Integer postId, Integer tagId) {
        this.postId = postId;
        this.tagId = tagId;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }

    @Override
    public String toString() {
        return "PostTag{" +
                "postId=" + postId + ", " + "tagId=" + tagId +
                "}";
    }
}
