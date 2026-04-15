package tn.esprit.entities;

public class Vote {

    private Integer id;
    private String userIdentifier;
    private Short value;
    private Integer postId;
    private Integer replyId;

    public Vote() {
    }

    public Vote(Integer id, String userIdentifier, Short value, Integer postId, Integer replyId) {
        this.id = id;
        this.userIdentifier = userIdentifier;
        this.value = value;
        this.postId = postId;
        this.replyId = replyId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public Short getValue() {
        return value;
    }

    public void setValue(Short value) {
        this.value = value;
    }

    public Integer getPostId() {
        return postId;
    }

    public void setPostId(Integer postId) {
        this.postId = postId;
    }

    public Integer getReplyId() {
        return replyId;
    }

    public void setReplyId(Integer replyId) {
        this.replyId = replyId;
    }

    @Override
    public String toString() {
        return "Vote{" +
                "id=" + id + ", " + "userIdentifier=" + userIdentifier + ", " + "value=" + value + ", " + "postId=" + postId + ", " + "replyId=" + replyId +
                "}";
    }
}
