package com.dreamcreators.swimgoaltracker.pojo;

public class DailyFeedComment {
    private String commentId;
    private String videoId;
    private String userId;
    private String userName;
    private String text;
    private long createdAt;

    public DailyFeedComment() {}

    public DailyFeedComment(String commentId, String videoId, String userId, String userName,
                            String text, long createdAt) {
        this.commentId = commentId;
        this.videoId = videoId;
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
