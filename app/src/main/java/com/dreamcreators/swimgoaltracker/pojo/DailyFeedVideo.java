package com.dreamcreators.swimgoaltracker.pojo;

public class DailyFeedVideo {
    private String videoId;
    private String videoUrl;
    private String thumbnailUrl;
    private String title;
    private String description;
    private String publishDate;
    private long createdAt;
    private long likesCount;
    private long dislikesCount;
    private long commentsCount;

    public DailyFeedVideo() {}

    public DailyFeedVideo(String videoId, String videoUrl, String thumbnailUrl, String title,
                          String description, String publishDate, long createdAt) {
        this.videoId = videoId;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.title = title;
        this.description = description;
        this.publishDate = publishDate;
        this.createdAt = createdAt;
    }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPublishDate() { return publishDate; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getLikesCount() { return likesCount; }
    public void setLikesCount(long likesCount) { this.likesCount = likesCount; }
    public long getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(long dislikesCount) { this.dislikesCount = dislikesCount; }
    public long getCommentsCount() { return commentsCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }
}
