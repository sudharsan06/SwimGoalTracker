package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.util.Log;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static String userName = "";

    private TextView tvDate;
    private TextView tvCalories, tvProtein, tvCarbs, tvFats;
    private TextView tvUserName;
    private LinearLayout lay_freeStyle, lay_backStroke, lay_breastStroke, lay_butterFly, lay_im;
    private NutritionDbHelper dbHelper;
    private TextView tvBestFree, tvBestBack, tvBestBreast, tvBestFly, tvBestIM;
    private android.widget.ImageView ivProfileIcon;
    private TextView tvWeeklyDistance, tvWeeklyGoal, tvWeeklyPercent;
    private View viewProgressFill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.dark_background));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));
        setContentView(R.layout.activity_main);

        View headerContent = findViewById(R.id.headerContent);
        ViewCompat.setOnApplyWindowInsetsListener(headerContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topPadding = systemBars.top + (int)(20 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), topPadding, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });



        tvDate = findViewById(R.id.tvDate);
        tvCalories = findViewById(R.id.tvCalories);
        tvProtein = findViewById(R.id.tvProtein);
        tvCarbs = findViewById(R.id.tvCarbs);
        tvFats = findViewById(R.id.tvFats);
        tvBestFree = findViewById(R.id.tvBestFree);
        tvBestBack = findViewById(R.id.tvBestBack);
        tvBestBreast = findViewById(R.id.tvBestBreast);
        tvBestFly = findViewById(R.id.tvBestFly);
        tvBestIM = findViewById(R.id.tvBestIM);
        tvUserName = findViewById(R.id.tvUserName);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        tvWeeklyDistance = findViewById(R.id.tvWeeklyDistance);
        tvWeeklyGoal = findViewById(R.id.tvWeeklyGoal);
        tvWeeklyPercent = findViewById(R.id.tvWeeklyPercent);
        viewProgressFill = findViewById(R.id.viewProgressFill);



        View.OnClickListener openSwimStopwatch = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SwimStopwatchActivity.class);

                int viewId = view.getId();
                String stroke; // Declare a variable to hold the stroke value

                if (viewId == R.id.lay_freeStyle) {
                    stroke = "Freestyle";
                } else if (viewId == R.id.lay_backStroke) {
                    stroke = "Backstroke";
                } else if (viewId == R.id.lay_breastStroke) {
                    stroke = "Breaststroke";
                } else if (viewId == R.id.lay_butterFly) {
                    stroke = "Butterfly";
                } else if (viewId == R.id.lay_im) {
                    stroke = "IM";
                } else {
                    // Optional: handle an unexpected view click
                    return;
                }

                intent.putExtra("stroke", stroke);
                startActivity(intent);
            }
        };
        findViewById(R.id.lay_freeStyle).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_backStroke).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_breastStroke).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_butterFly).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_im).setOnClickListener(openSwimStopwatch);


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        // View All → Insights Records tab
        findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            Intent intent = new Intent(this, TrackerActivity.class);
            intent.putExtra("open_tab", 1);
            startActivity(intent);
        });

        // Profile icon → Switch Swimmer screen
        findViewById(R.id.btnOpenProfile).setOnClickListener(v ->
                startActivity(new Intent(this, SwitchSwimmerActivity.class)));

        // Weekly goal → Goals screen
        tvWeeklyGoal.setOnClickListener(v ->
                startActivity(new Intent(this, GoalsActivity.class)
                        .putExtra("highlight_weekly", true)));

        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e("MainActivity", "Ad failed: " + loadAdError.getCode() + " " + loadAdError.getMessage());
                }
            });
            MobileAds.initialize(this, initializationStatus -> {
                adView.loadAd(new AdRequest.Builder().build());
            });
        }

        dbHelper = new NutritionDbHelper(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("Today: " + today);
        tvUserName.setText(getGreetingMessage(userName));

        findViewById(android.R.id.content).post(() -> {
            loadUserData();
            loadWeeklyProgress();
            loadTodayNutrition(today);
        });


        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_tracker) {
                startActivity(new Intent(this, TrackerActivity.class));
                return true;
            } else if (id == R.id.nav_goals) {
                startActivity(new Intent(this, GoalsActivity.class)
                        .putExtra("highlight_weekly", true));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

   /* @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadUserData();
            tvUserName.setText(getGreetingMessage(userName));
        }
    }*/

    private void loadUserData() {
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT image_uri, name, age, height, weight, start_date, last_login FROM profile WHERE id=?", new String[]{String.valueOf(activeProfileId)});
        if (c.moveToFirst()) {
            String uriStr = c.getString(0);
            userName = c.getString(1);
            tvUserName.setText(getGreetingMessage(userName));
            
            if (uriStr != null && !uriStr.isEmpty()) {
                Uri uri = Uri.parse(uriStr);
                boolean canRead =
                        "file".equalsIgnoreCase(uri.getScheme()) ||
                        ("content".equalsIgnoreCase(uri.getScheme()) && hasPersistedReadPermission(uri));
                if (canRead) {
                    try {
                        ivProfileIcon.setImageURI(uri);
                        ivProfileIcon.setPadding(0, 0, 0, 0); // Remove padding if it's a real photo
                        ivProfileIcon.setImageTintList(null); // Remove white tint
                        ivProfileIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    } catch (Exception e) {
                        setDefaultProfileIcon();
                    }
                } else {
                    setDefaultProfileIcon();
                }
            } else {
                setDefaultProfileIcon();
            }
        }
        c.close();
        
        android.widget.ImageView ivSwitchBadge = findViewById(R.id.ivSwitchProfileBadge);
        if (ivSwitchBadge != null) {
            if (ProfileManager.getProfiles(this).size() > 1) {
                ivSwitchBadge.setVisibility(android.view.View.VISIBLE);
            } else {
                ivSwitchBadge.setVisibility(android.view.View.GONE);
            }
        }
    }

    private void loadWeeklyProgress() {
        int activeProfileId = ProfileManager.getActiveProfileId(this);

        // Calculate Monday of current week
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.Locale.getDefault());
        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY);
        String weekStart = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.getTime());

        // Calculate Sunday
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6);
        String weekEnd = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(cal.getTime());

        // Query total distance this week
        int totalMeters = 0;
        try {
            android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT COALESCE(SUM(total_distance), 0) FROM swim_sessions WHERE profile_id = ? AND date >= ? AND date <= ?",
                    new String[]{String.valueOf(activeProfileId), weekStart, weekEnd}
            );
            if (c.moveToFirst()) {
                totalMeters = c.getInt(0);
            }
            c.close();
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading weekly distance", e);
        }

        float totalKm = totalMeters / 1000f;

        // Load weekly km goal
        String goalStr = getSharedPreferences("swim_goals", MODE_PRIVATE)
                .getString("weekly_km_" + activeProfileId, "");
        float goalKm = 0;
        try {
            goalKm = Float.parseFloat(goalStr);
        } catch (NumberFormatException ignored) {}

        // Update UI
        tvWeeklyDistance.setText(String.format(java.util.Locale.getDefault(), "%.1f", totalKm));

        if (goalKm > 0) {
            tvWeeklyGoal.setText(String.format(java.util.Locale.getDefault(), "Goal: %.1f km", goalKm));
            int progress = (int) Math.min(100, (totalKm / goalKm) * 100);
            tvWeeklyPercent.setText(progress + "% complete");

            // Update progress bar weight
            android.widget.LinearLayout.LayoutParams params =
                    (android.widget.LinearLayout.LayoutParams) viewProgressFill.getLayoutParams();
            params.weight = progress;
            viewProgressFill.setLayoutParams(params);
        } else {
            tvWeeklyGoal.setText("Set a weekly goal");
            tvWeeklyPercent.setText("");
            android.widget.LinearLayout.LayoutParams params =
                    (android.widget.LinearLayout.LayoutParams) viewProgressFill.getLayoutParams();
            params.weight = 0;
            viewProgressFill.setLayoutParams(params);
        }
    }

    private void setDefaultProfileIcon() {
        ivProfileIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
        ivProfileIcon.setPadding(8, 8, 8, 8); // Restore icon padding
        ivProfileIcon.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        ivProfileIcon.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
    }

    private boolean hasPersistedReadPermission(Uri uri) {
        try {
            for (android.content.UriPermission p : getContentResolver().getPersistedUriPermissions()) {
                if (p.getUri().equals(uri) && p.isReadPermission()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

  /*  private View.OnClickListener openSwimStopwatch = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, SwimStopwatchActivity.class);
            switch (view.getId()) {
                case R.id.lay_freeStyle:
                    intent.putExtra("stroke", "Freestyle");
                    break;
                case R.id.lay_backStroke:
                    intent.putExtra("stroke", "Backstroke");
                    break;
                case R.id.lay_breastStroke:
                    intent.putExtra("stroke", "Breaststroke");
                    break;
                case R.id.lay_butterFly:
                    intent.putExtra("stroke", "Butterfly");
                    break;
            }
            startActivity(intent);
        }
    };*/



    private void loadTodayNutrition(String date) {
        if (tvCalories == null) return;
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT calories, protein, carbs, fats FROM daily_nutrition WHERE date = ?",
                new String[]{date}
        );
        if (cursor.moveToFirst()) {
            tvCalories.setText(cursor.getInt(0) + " kcal");
            tvProtein.setText(cursor.getInt(1) + " g");
            tvCarbs.setText(cursor.getInt(2) + " g");
            tvFats.setText(cursor.getInt(3) + " g");
        } else {
            tvCalories.setText("0 kcal");
            tvProtein.setText("0 g");
            tvCarbs.setText("0 g");
            tvFats.setText("0 g");
        }
        cursor.close();
    }

    private String getGreetingMessage(String userName) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Hello";
        }
        if (userName == null || userName.isEmpty()) {
            userName = "Swimmer";
        }

        return greeting + ", " + userName + "!";
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvUserName.setText(getGreetingMessage(userName));
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        findViewById(android.R.id.content).post(() -> {
            loadUserData();
            loadWeeklyProgress();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            loadTodayNutrition(today);
            loadBestSwimToday(today);
        });
    }

    private void loadBestSwimToday(String date) {
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        // Use NULLIF to convert 0 to NULL, so MIN() ignores zeros and finds the best (lowest) non-zero time
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT MIN(NULLIF(freestyle_ms, 0)), MIN(NULLIF(backstroke_ms, 0)), MIN(NULLIF(breaststroke_ms, 0)), MIN(NULLIF(butterfly_ms, 0)), MIN(NULLIF(im_ms, 0)) FROM swim_sessions WHERE date = ? AND profile_id = ?",
                new String[]{date, String.valueOf(activeProfileId)}
        );
        if (c.moveToFirst()) {
            tvBestFree.setText(formatMs(c.isNull(0) ? 0 : c.getLong(0)));
            tvBestBack.setText(formatMs(c.isNull(1) ? 0 : c.getLong(1)));
            tvBestBreast.setText(formatMs(c.isNull(2) ? 0 : c.getLong(2)));
            tvBestFly.setText(formatMs(c.isNull(3) ? 0 : c.getLong(3)));
            tvBestIM.setText(formatMs(c.isNull(4) ? 0 : c.getLong(4)));
        }
        c.close();
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}

