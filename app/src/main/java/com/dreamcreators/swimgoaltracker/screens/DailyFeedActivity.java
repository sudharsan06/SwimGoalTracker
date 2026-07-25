package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.pojo.DailyFeedComment;
import com.dreamcreators.swimgoaltracker.pojo.DailyFeedVideo;
import com.dreamcreators.swimgoaltracker.sync.DailyFeedManager;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyFeedActivity extends AppCompatActivity {

    private DailyFeedManager feedManager;
    private DailyFeedVideo currentVideo;

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar videoProgress;
    private TextView tvVideoError;

    private View cardVideo, cardLoading, cardNoVideo, layVideoInfo, layActions, layDivider;
    private View layCommentsHeader, layAddComment, videoContainer;
    private LinearLayout layCommentsList;
    private TextView tvVideoTitle, tvVideoDescription;
    private TextView tvLikeCount, tvDislikeCount, tvLikeIcon, tvDislikeIcon;
    private TextView tvCommentsCount, tvNoComments;
    private EditText etCommentInput;
    private MaterialButton btnPostComment;
    private LinearLayout btnLike, btnDislike, btnShare;
    private ImageButton btnFullscreen;
    private View topBanner, bottomNav, adView;
    private ScrollView scrollContent;
    private ProgressBar topProgressBar;

    private int currentUserLike = 0;
    private boolean isFullscreen = false;
    private int originalVideoHeight;

    private final Handler loadingTimeoutHandler = new Handler();
    private static final long NO_VIDEO_TIMEOUT_MS = 12000L;

    private InterstitialAd mInterstitialAd;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private static final String PREF_DAILY_FEED_AD_TIME = "last_daily_feed_ad_time";
    private static final long AD_COOLDOWN_MS = 5 * 60 * 1000;
    private static final String PREF_REWARDED_AD_TIME = "last_rewarded_ad_time";
    private static final long REWARDED_AD_COOLDOWN_MS = 30 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.dark_surface_low));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));
        setContentView(R.layout.activity_daily_feed);

        View topBanner = findViewById(R.id.top_banner);
        ViewCompat.setOnApplyWindowInsetsListener(topBanner, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        initViews();
        setupBottomNav();

        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putString("daily_feed_last_visit", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()))
                .apply();

        if (shouldShowRewardedAd()) {
            loadAndShowRewardedAd(this::loadContent);
        } else if (shouldShowAd()) {
            loadAndShowAd(this::loadContent);
        } else {
            loadContent();
        }
    }

    private boolean shouldShowAd() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long lastAdTime = prefs.getLong(PREF_DAILY_FEED_AD_TIME, 0);
        return System.currentTimeMillis() - lastAdTime >= AD_COOLDOWN_MS;
    }

    private void loadAndShowAd(Runnable onDone) {
        InterstitialAd.load(this, "ca-app-pub-6508060827158792/7650036098",
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd interstitialAd) {
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                saveAdTime();
                                onDone.run();
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                onDone.run();
                            }
                        });
                        interstitialAd.show(DailyFeedActivity.this);
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        onDone.run();
                    }
                });
    }

    private boolean shouldShowRewardedAd() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        long lastRewardedTime = prefs.getLong(PREF_REWARDED_AD_TIME, 0);
        return System.currentTimeMillis() - lastRewardedTime >= REWARDED_AD_COOLDOWN_MS;
    }

    private void loadAndShowRewardedAd(Runnable onDone) {
        RewardedInterstitialAd.load(this, "ca-app-pub-6508060827158792/1994162961",
                new AdRequest.Builder().build(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedInterstitialAd ad) {
                        rewardedInterstitialAd = ad;
                        rewardedInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                onDone.run();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                onDone.run();
                            }
                        });
                        rewardedInterstitialAd.show(DailyFeedActivity.this, rewardItem -> {
                            saveRewardedAdTime();
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        onDone.run();
                    }
                });
    }

    private void saveRewardedAdTime() {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putLong(PREF_REWARDED_AD_TIME, System.currentTimeMillis())
                .apply();
    }

    private void saveAdTime() {
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                .putLong(PREF_DAILY_FEED_AD_TIME, System.currentTimeMillis())
                .apply();
    }

    private void loadContent() {
        feedManager = new DailyFeedManager(this);
        showLoading(true);
        loadVideo();
    }

    private void showLoading(boolean loading) {
        if (topProgressBar != null) {
            topProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoadingVideo() {
        showLoading(true);
        cardLoading.setVisibility(View.VISIBLE);
        cardVideo.setVisibility(View.GONE);
        cardNoVideo.setVisibility(View.GONE);
        layVideoInfo.setVisibility(View.GONE);
        layActions.setVisibility(View.GONE);
        layDivider.setVisibility(View.GONE);
        layCommentsHeader.setVisibility(View.GONE);
        layAddComment.setVisibility(View.GONE);
        videoProgress.setVisibility(View.GONE);
        tvVideoError.setVisibility(View.GONE);
    }

    private void revealVideo() {
        cardLoading.setVisibility(View.GONE);
        showLoading(false);
        if (cardNoVideo.getVisibility() == View.VISIBLE) {
            return;
        }
        cardVideo.setVisibility(View.VISIBLE);
        layVideoInfo.setVisibility(View.VISIBLE);
        layActions.setVisibility(View.VISIBLE);
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        videoProgress = findViewById(R.id.videoProgress);
        tvVideoError = findViewById(R.id.tvVideoError);
        cardVideo = findViewById(R.id.cardVideo);
        cardLoading = findViewById(R.id.cardLoading);
        cardNoVideo = findViewById(R.id.cardNoVideo);
        videoContainer = findViewById(R.id.videoContainer);
        layVideoInfo = findViewById(R.id.layVideoInfo);
        layActions = findViewById(R.id.layActions);
        layDivider = findViewById(R.id.layDivider);
        layCommentsHeader = findViewById(R.id.layCommentsHeader);
        layAddComment = findViewById(R.id.layAddComment);
        layCommentsList = findViewById(R.id.layCommentsList);
        tvVideoTitle = findViewById(R.id.tvVideoTitle);
        tvVideoDescription = findViewById(R.id.tvVideoDescription);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvDislikeCount = findViewById(R.id.tvDislikeCount);
        tvLikeIcon = findViewById(R.id.tvLikeIcon);
        tvDislikeIcon = findViewById(R.id.tvDislikeIcon);
        tvCommentsCount = findViewById(R.id.tvCommentsCount);
        tvNoComments = findViewById(R.id.tvNoComments);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnPostComment = findViewById(R.id.btnPostComment);
        btnLike = findViewById(R.id.btnLike);
        btnDislike = findViewById(R.id.btnDislike);
        btnShare = findViewById(R.id.btnShare);
        btnFullscreen = findViewById(R.id.btnFullscreen);
        topBanner = findViewById(R.id.top_banner);
        bottomNav = findViewById(R.id.bottomNav);
        adView = findViewById(R.id.adView);
        scrollContent = findViewById(R.id.scrollContent);
        topProgressBar = findViewById(R.id.topProgressBar);

        originalVideoHeight = (int) (220 * getResources().getDisplayMetrics().density);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnLike.setOnClickListener(v -> toggleLike(1));
        btnDislike.setOnClickListener(v -> toggleLike(-1));
        btnShare.setOnClickListener(v -> shareVideo());

        btnPostComment.setOnClickListener(v -> postComment());
        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_daily_feed);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_tracker) {
                startActivity(new Intent(this, TrackerActivity.class));
                return true;
            } else if (id == R.id.nav_events) {
                startActivity(new Intent(this, EventsActivity.class));
                return true;
            } else if (id == R.id.nav_daily_feed) {
                return true;
            }
            return false;
        });
    }

    private void loadVideo() {
        showLoadingVideo();

        loadingTimeoutHandler.removeCallbacksAndMessages(null);
        loadingTimeoutHandler.postDelayed(() -> {
            runOnUiThread(() -> {
                if (cardLoading.getVisibility() == View.VISIBLE) {
                    showNoVideo();
                }
            });
        }, NO_VIDEO_TIMEOUT_MS);

        DailyFeedVideo cached = feedManager.getCachedVideo();
        if (cached != null) {
            showVideo(cached);
        }

        feedManager.fetchCurrentVideo(new DailyFeedManager.VideoCallback() {
            @Override
            public void onVideoLoaded(DailyFeedVideo video) {
                if (video != null) {
                    currentVideo = video;
                    showVideo(video);
                    loadComments(video.getVideoId());
                    listenLikes(video.getVideoId());
                }
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    private void showVideo(DailyFeedVideo video) {
        loadingTimeoutHandler.removeCallbacksAndMessages(null);
        showLoading(false);
        cardNoVideo.setVisibility(View.GONE);
        // cardVideo not shown yet — stays behind the skeleton until player is ready
        tvVideoTitle.setText(video.getTitle() != null ? video.getTitle() : "Daily Swim Video");
        tvVideoDescription.setText(video.getDescription() != null ? video.getDescription() : "");

        if (video.getVideoUrl() != null && !video.getVideoUrl().isEmpty()) {
            initializePlayer(video.getVideoUrl());
        } else {
            cardLoading.setVisibility(View.GONE);
            cardVideo.setVisibility(View.VISIBLE);
            videoProgress.setVisibility(View.GONE);
            tvVideoError.setVisibility(View.VISIBLE);
            tvVideoError.setText("Video URL not available");
        }
    }

    private void showNoVideo() {
        loadingTimeoutHandler.removeCallbacksAndMessages(null);
        showLoading(false);
        cardLoading.setVisibility(View.GONE);
        cardVideo.setVisibility(View.GONE);
        cardNoVideo.setVisibility(View.VISIBLE);
        layVideoInfo.setVisibility(View.GONE);
        layActions.setVisibility(View.GONE);
    }

    private void initializePlayer(String videoUrl) {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        videoProgress.setVisibility(View.VISIBLE);
                    } else {
                        videoProgress.setVisibility(View.GONE);
                    }
                    if (playbackState == Player.STATE_READY) {
                        tvVideoError.setVisibility(View.GONE);
                        videoProgress.setVisibility(View.GONE);
                        revealVideo();
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    videoProgress.setVisibility(View.GONE);
                    tvVideoError.setVisibility(View.VISIBLE);
                    tvVideoError.setText("Error playing video");
                    revealVideo();
                }
            });
        }

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private void loadComments(String videoId) {
        layCommentsHeader.setVisibility(View.VISIBLE);
        layAddComment.setVisibility(View.VISIBLE);

        List<DailyFeedComment> cachedComments = feedManager.getCachedComments(videoId);
        if (!cachedComments.isEmpty()) {
            displayComments(cachedComments);
        }

        feedManager.listenComments(videoId, new DailyFeedManager.CommentsCallback() {
            @Override
            public void onCommentsLoaded(List<DailyFeedComment> comments) {
                displayComments(comments);
            }

            @Override
            public void onError(String error) {
                if (cachedComments.isEmpty()) {
                    tvNoComments.setVisibility(View.VISIBLE);
                    tvNoComments.setText("Unable to load comments");
                }
            }
        });
    }

    private void displayComments(List<DailyFeedComment> comments) {
        layCommentsList.removeAllViews();
        tvCommentsCount.setText(String.valueOf(comments.size()));

        if (comments.isEmpty()) {
            tvNoComments.setVisibility(View.VISIBLE);
            tvNoComments.setText("No comments yet. Be the first to comment!");
            return;
        }
        tvNoComments.setVisibility(View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getUid();

        for (DailyFeedComment comment : comments) {
            View commentView = getLayoutInflater().inflate(R.layout.item_comment, null);
            TextView tvUser = commentView.findViewById(R.id.tvCommentUser);
            TextView tvText = commentView.findViewById(R.id.tvCommentText);
            TextView tvTime = commentView.findViewById(R.id.tvCommentTime);
            TextView btnEdit = commentView.findViewById(R.id.btnEditComment);
            TextView btnDelete = commentView.findViewById(R.id.btnDeleteComment);

            tvUser.setText(comment.getUserName() != null ? comment.getUserName() : "Swimmer");
            tvText.setText(comment.getText());

            long now = System.currentTimeMillis();
            long diff = now - comment.getCreatedAt();
            String timeAgo;
            if (diff < 60000) timeAgo = "Just now";
            else if (diff < 3600000) timeAgo = (diff / 60000) + "m ago";
            else if (diff < 86400000) timeAgo = (diff / 3600000) + "h ago";
            else timeAgo = new SimpleDateFormat("MMM dd", Locale.getDefault()).format(new Date(comment.getCreatedAt()));
            tvTime.setText(timeAgo);

            if (currentUserId != null && currentUserId.equals(comment.getUserId())) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                String commentId = comment.getCommentId();
                String videoId = comment.getVideoId();
                btnEdit.setOnClickListener(v -> showEditCommentDialog(videoId, commentId, comment.getText()));
                btnDelete.setOnClickListener(v -> showDeleteCommentDialog(videoId, commentId));
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }

            layCommentsList.addView(commentView);
        }
    }

    private void showEditCommentDialog(String videoId, String commentId, String currentText) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Edit Comment");

        EditText input = new EditText(this);
        input.setText(currentText);
        input.setSelection(currentText.length());
        input.setPadding(48, 24, 48, 24);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newText = input.getText().toString().trim();
            if (!newText.isEmpty()) {
                feedManager.editComment(videoId, commentId, newText, new DailyFeedManager.SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(DailyFeedActivity.this, "Comment updated", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(DailyFeedActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteCommentDialog(String videoId, String commentId) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    feedManager.deleteComment(videoId, commentId, new DailyFeedManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(() ->
                                    Toast.makeText(DailyFeedActivity.this, "Comment deleted", Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                    Toast.makeText(DailyFeedActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void postComment() {
        String text = etCommentInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentVideo == null) return;

        btnPostComment.setEnabled(false);
        feedManager.addComment(currentVideo.getVideoId(), text, new DailyFeedManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    etCommentInput.setText("");
                    btnPostComment.setEnabled(true);
                    Toast.makeText(DailyFeedActivity.this, "Comment posted", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    btnPostComment.setEnabled(true);
                    Toast.makeText(DailyFeedActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void toggleLike(int type) {
        if (currentVideo == null) return;
        if (FirebaseAuth.getInstance().getUid() == null) {
            Toast.makeText(this, "Please sign in to like", Toast.LENGTH_SHORT).show();
            return;
        }
        feedManager.toggleLike(currentVideo.getVideoId(), type);
    }

    private void listenLikes(String videoId) {
        feedManager.listenLikes(videoId, new DailyFeedManager.LikeCallback() {
            @Override
            public void onResult(boolean success, long likesCount, long dislikesCount, int userLike) {
                runOnUiThread(() -> {
                    currentUserLike = userLike;
                    tvLikeCount.setText(String.valueOf(likesCount));
                    tvDislikeCount.setText(String.valueOf(dislikesCount));
                    updateLikeUI();
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void updateLikeUI() {
        tvLikeIcon.setText(currentUserLike == 1 ? "👍" : "👍");
        tvLikeIcon.setAlpha(currentUserLike == 1 ? 1.0f : 0.5f);
        tvDislikeIcon.setText(currentUserLike == -1 ? "👎" : "👎");
        tvDislikeIcon.setAlpha(currentUserLike == -1 ? 1.0f : 0.5f);
    }

    private void toggleFullscreen() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) {
            topBanner.setVisibility(View.GONE);
            bottomNav.setVisibility(View.GONE);
            adView.setVisibility(View.GONE);
            layVideoInfo.setVisibility(View.GONE);
            layActions.setVisibility(View.GONE);
            layDivider.setVisibility(View.GONE);
            layCommentsHeader.setVisibility(View.GONE);
            layAddComment.setVisibility(View.GONE);
            layCommentsList.setVisibility(View.GONE);
            scrollContent.setBackgroundColor(getColor(R.color.black));
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);

            scrollContent.post(() -> {
                ViewGroup.LayoutParams cardParams = cardVideo.getLayoutParams();
                cardParams.height = scrollContent.getHeight();
                cardVideo.setLayoutParams(cardParams);
                ((ViewGroup.MarginLayoutParams) cardVideo.getLayoutParams()).setMargins(0, 0, 0, 0);
                cardVideo.setLayoutParams(cardVideo.getLayoutParams());

                ViewGroup.LayoutParams containerParams = videoContainer.getLayoutParams();
                containerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                videoContainer.setLayoutParams(containerParams);
            });
        } else {
            btnFullscreen.setImageResource(R.drawable.ic_fullscreen);
            topBanner.setVisibility(View.VISIBLE);
            bottomNav.setVisibility(View.VISIBLE);
            adView.setVisibility(View.VISIBLE);
            layVideoInfo.setVisibility(View.VISIBLE);
            layActions.setVisibility(View.VISIBLE);
            layDivider.setVisibility(View.VISIBLE);
            layCommentsHeader.setVisibility(View.VISIBLE);
            layAddComment.setVisibility(View.VISIBLE);
            layCommentsList.setVisibility(View.VISIBLE);
            scrollContent.setBackgroundResource(R.color.dark_background);

            ViewGroup.LayoutParams cardParams = cardVideo.getLayoutParams();
            cardParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            cardVideo.setLayoutParams(cardParams);
            ((ViewGroup.MarginLayoutParams) cardVideo.getLayoutParams()).setMargins(
                    dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12)
            );
            cardVideo.setLayoutParams(cardVideo.getLayoutParams());

            ViewGroup.LayoutParams containerParams = videoContainer.getLayoutParams();
            containerParams.height = originalVideoHeight;
            videoContainer.setLayoutParams(containerParams);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void shareVideo() {
        String shareText = "Check out today's swim video on SwimminGO!\n\n";
        if (currentVideo != null && currentVideo.getTitle() != null) {
            shareText += currentVideo.getTitle() + "\n";
        }
        shareText += "\nDownload SwimminGO - Your personal swim tracker\n" +
                "https://play.google.com/store/apps/details?id=" + getPackageName();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        if (currentVideo != null && currentVideo.getVideoUrl() != null) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                    currentVideo.getTitle() != null ? currentVideo.getTitle() : "SwimminGO Daily Video");
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_daily_feed);
        if (player != null) {
            player.setPlayWhenReady(true);
        } else if (currentVideo != null && currentVideo.getVideoUrl() != null) {
            initializePlayer(currentVideo.getVideoUrl());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        loadingTimeoutHandler.removeCallbacksAndMessages(null);
        if (currentVideo != null && feedManager != null) {
            feedManager.removeCommentsListener(currentVideo.getVideoId());
            feedManager.removeLikesListener(currentVideo.getVideoId());
        }
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
