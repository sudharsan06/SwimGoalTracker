package com.dreamcreators.swimgoaltracker.sync;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.pojo.DailyFeedVideo;
import com.dreamcreators.swimgoaltracker.pojo.DailyFeedComment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DailyFeedManager {

    private static final String FIREBASE_PATH = "daily_feed";
    private static final String FIREBASE_VIDEOS = "videos";
    private static final String FIREBASE_COMMENTS = "comments";
    private static final String FIREBASE_LIKES = "likes";

    private final Context context;
    private final NutritionDbHelper dbHelper;
    private final DatabaseReference feedRef;
    private final FirebaseAuth auth;
    private ValueEventListener currentVideoListener;
    private ValueEventListener commentsListener;
    private ValueEventListener likesListener;

    public DailyFeedManager(Context context) {
        this.context = context;
        this.dbHelper = new NutritionDbHelper(context);
        this.feedRef = FirebaseDatabase.getInstance().getReference(FIREBASE_PATH);
        this.auth = FirebaseAuth.getInstance();
    }

    public interface VideoCallback {
        void onVideoLoaded(DailyFeedVideo video);
        void onError(String error);
    }

    public interface CommentsCallback {
        void onCommentsLoaded(List<DailyFeedComment> comments);
        void onError(String error);
    }

    public interface LikeCallback {
        void onResult(boolean success, long likesCount, long dislikesCount, int currentUserLike);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    public DailyFeedVideo getCachedVideo() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM daily_feed_video ORDER BY created_at DESC LIMIT 1", null);
        DailyFeedVideo video = null;
        if (c.moveToFirst()) {
            video = cursorToVideo(c);
        }
        c.close();
        return video;
    }

    public List<DailyFeedComment> getCachedComments(String videoId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM daily_feed_comments WHERE video_id = ? ORDER BY created_at ASC",
                new String[]{videoId});
        List<DailyFeedComment> comments = new ArrayList<>();
        while (c.moveToNext()) {
            DailyFeedComment comment = new DailyFeedComment(
                    c.getString(c.getColumnIndex("comment_id")),
                    c.getString(c.getColumnIndex("video_id")),
                    c.getString(c.getColumnIndex("user_id")),
                    c.getString(c.getColumnIndex("user_name")),
                    c.getString(c.getColumnIndex("text")),
                    c.getLong(c.getColumnIndex("created_at"))
            );
            comments.add(comment);
        }
        c.close();
        return comments;
    }

    public int getCachedUserLike(String videoId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String uid = auth.getUid();
        if (uid == null) return 0;
        Cursor c = db.rawQuery("SELECT like_type FROM daily_feed_likes WHERE video_id = ? AND user_id = ?",
                new String[]{videoId, uid});
        int type = 0;
        if (c.moveToFirst()) {
            type = c.getInt(0);
        }
        c.close();
        return type;
    }

    public void fetchCurrentVideo(VideoCallback callback) {
        if (currentVideoListener != null) {
            feedRef.child(FIREBASE_VIDEOS).removeEventListener(currentVideoListener);
        }

        currentVideoListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                DailyFeedVideo latest = null;
                long maxCreatedAt = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    DailyFeedVideo v = snapshotToVideo(child);
                    if (v != null && v.getCreatedAt() > maxCreatedAt) {
                        maxCreatedAt = v.getCreatedAt();
                        latest = v;
                    }
                }
                if (latest != null) {
                    cacheVideo(latest);
                    callback.onVideoLoaded(latest);
                } else {
                    callback.onVideoLoaded(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        feedRef.child(FIREBASE_VIDEOS).addListenerForSingleValueEvent(currentVideoListener);
    }

    public void listenComments(String videoId, CommentsCallback callback) {
        if (commentsListener != null) {
            feedRef.child(FIREBASE_COMMENTS).child(videoId).removeEventListener(commentsListener);
        }

        commentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<DailyFeedComment> comments = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    DailyFeedComment comment = snapshotToComment(child, videoId);
                    if (comment != null) {
                        comments.add(comment);
                    }
                }
                cacheComments(videoId, comments);
                callback.onCommentsLoaded(comments);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        feedRef.child(FIREBASE_COMMENTS).child(videoId).addValueEventListener(commentsListener);
    }

    public void removeCommentsListener(String videoId) {
        if (commentsListener != null) {
            feedRef.child(FIREBASE_COMMENTS).child(videoId).removeEventListener(commentsListener);
            commentsListener = null;
        }
    }

    public void addComment(String videoId, String text, SimpleCallback callback) {
        String uid = auth.getUid();
        if (uid == null) {
            callback.onError("Not logged in");
            return;
        }

        DatabaseReference commentRef = feedRef.child(FIREBASE_COMMENTS).child(videoId).push();
        String commentId = commentRef.getKey();

        String userName = ProfileManager.getActiveProfileName(context);
        if (userName == null || userName.isEmpty()) userName = "Swimmer";

        ContentValues data = new ContentValues();
        data.put("user_id", uid);
        data.put("user_name", userName);
        data.put("text", text);
        data.put("created_at", System.currentTimeMillis());

        commentRef.setValue(toMap(data))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void editComment(String videoId, String commentId, String newText, SimpleCallback callback) {
        feedRef.child(FIREBASE_COMMENTS).child(videoId).child(commentId).child("text")
                .setValue(newText)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteComment(String videoId, String commentId, SimpleCallback callback) {
        feedRef.child(FIREBASE_COMMENTS).child(videoId).child(commentId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void toggleLike(String videoId, int newType) {
        String uid = auth.getUid();
        if (uid == null) return;

        int currentType = getCachedUserLike(videoId);

        DatabaseReference likeRef = feedRef.child(FIREBASE_VIDEOS).child(videoId).child("likes").child(uid);

        if (currentType == newType) {
            newType = 0;
        }

        if (newType == 0) {
            likeRef.removeValue();
        } else {
            likeRef.child("type").setValue(newType);
        }

        updateLocalLike(videoId, uid, newType);
    }

    public void listenLikes(String videoId, LikeCallback callback) {
        if (likesListener != null) {
            feedRef.child(FIREBASE_VIDEOS).child(videoId).child("likes").removeEventListener(likesListener);
        }

        likesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                long likes = 0, dislikes = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    long t = child.child("type").getValue(Long.class) != null ?
                            child.child("type").getValue(Long.class) : 0;
                    if (t == 1) likes++;
                    else if (t == -1) dislikes++;
                }
                int currentUserLike = getCachedUserLike(videoId);
                callback.onResult(true, likes, dislikes, currentUserLike);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        feedRef.child(FIREBASE_VIDEOS).child(videoId).child("likes").addValueEventListener(likesListener);
    }

    public void removeLikesListener(String videoId) {
        if (likesListener != null) {
            feedRef.child(FIREBASE_VIDEOS).child(videoId).child("likes").removeEventListener(likesListener);
            likesListener = null;
        }
    }

    private void cacheVideo(DailyFeedVideo video) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("daily_feed_video", null, null);
        ContentValues values = new ContentValues();
        values.put("video_id", video.getVideoId());
        values.put("video_url", video.getVideoUrl());
        values.put("thumbnail_url", video.getThumbnailUrl());
        values.put("title", video.getTitle());
        values.put("description", video.getDescription());
        values.put("publish_date", video.getPublishDate());
        values.put("created_at", video.getCreatedAt());
        db.insert("daily_feed_video", null, values);
    }

    private void cacheComments(String videoId, List<DailyFeedComment> comments) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("daily_feed_comments", "video_id = ?", new String[]{videoId});
        for (DailyFeedComment c : comments) {
            ContentValues values = new ContentValues();
            values.put("comment_id", c.getCommentId());
            values.put("video_id", c.getVideoId());
            values.put("user_id", c.getUserId());
            values.put("user_name", c.getUserName());
            values.put("text", c.getText());
            values.put("created_at", c.getCreatedAt());
            db.insert("daily_feed_comments", null, values);
        }
    }

    private void updateLocalLike(String videoId, String userId, int type) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("daily_feed_likes", "video_id = ? AND user_id = ?", new String[]{videoId, userId});
        if (type != 0) {
            ContentValues values = new ContentValues();
            values.put("video_id", videoId);
            values.put("user_id", userId);
            values.put("like_type", type);
            db.insert("daily_feed_likes", null, values);
        }
    }

    private DailyFeedVideo cursorToVideo(Cursor c) {
        DailyFeedVideo v = new DailyFeedVideo(
                c.getString(c.getColumnIndex("video_id")),
                c.getString(c.getColumnIndex("video_url")),
                c.getString(c.getColumnIndex("thumbnail_url")),
                c.getString(c.getColumnIndex("title")),
                c.getString(c.getColumnIndex("description")),
                c.getString(c.getColumnIndex("publish_date")),
                c.getLong(c.getColumnIndex("created_at"))
        );
        return v;
    }

    private DailyFeedVideo snapshotToVideo(DataSnapshot child) {
        try {
            String id = child.getKey();
            String url = child.child("video_url").getValue(String.class);
            if (url == null) return null;
            DailyFeedVideo v = new DailyFeedVideo();
            v.setVideoId(id);
            v.setVideoUrl(url);
            v.setThumbnailUrl(child.child("thumbnail_url").getValue(String.class));
            v.setTitle(child.child("title").getValue(String.class));
            v.setDescription(child.child("description").getValue(String.class));
            v.setPublishDate(child.child("publish_date").getValue(String.class));
            Long createdAt = child.child("created_at").getValue(Long.class);
            v.setCreatedAt(createdAt != null ? createdAt : 0);
            return v;
        } catch (Exception e) {
            return null;
        }
    }

    private DailyFeedComment snapshotToComment(DataSnapshot child, String videoId) {
        try {
            DailyFeedComment c = new DailyFeedComment();
            c.setCommentId(child.getKey());
            c.setVideoId(videoId);
            c.setUserId(child.child("user_id").getValue(String.class));
            c.setUserName(child.child("user_name").getValue(String.class));
            c.setText(child.child("text").getValue(String.class));
            Long createdAt = child.child("created_at").getValue(Long.class);
            c.setCreatedAt(createdAt != null ? createdAt : 0);
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private java.util.Map<String, Object> toMap(ContentValues values) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        for (String key : values.keySet()) {
            Object v = values.get(key);
            if (v instanceof Long) map.put(key, v);
            else if (v instanceof String) map.put(key, v);
            else if (v instanceof Integer) map.put(key, ((Integer) v).longValue());
            else map.put(key, String.valueOf(v));
        }
        return map;
    }
}
