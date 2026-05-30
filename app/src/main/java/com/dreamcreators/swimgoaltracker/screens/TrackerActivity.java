package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.dreamcreators.swimgoaltracker.fragments.ChartFragment;
import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.fragments.RecordsFragment;
import com.dreamcreators.swimgoaltracker.adapter.TrackerPagerAdapter;
import com.dreamcreators.swimgoaltracker.pojo.TrackerPojo;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrackerActivity extends AppCompatActivity {

    private NutritionDbHelper dbHelper;
    private TextView tvTrackerTitle;
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TrackerPagerAdapter pagerAdapter;

    // Cached data to pass to fragments
    private List<TrackerPojo> currentItems = new ArrayList<>();
    private long[] currentBestTimes = new long[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.dark_night));
        setContentView(R.layout.activity_tracker);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Swim Tracker");
        }

        tvTrackerTitle = findViewById(R.id.tvTrackerTitle);
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        dbHelper = new NutritionDbHelper(this);

        // Setup ViewPager2 + Tabs
        pagerAdapter = new TrackerPagerAdapter(this);
        // Keep BOTH fragments alive off-screen so RecordsFragment is ready immediately
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("📊 Chart");
            } else {
                tab.setText("📋 Records");
            }
        }).attach();

        // Push data to whatever fragment is visible when the user swipes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                pushDataToFragments();
            }
        });

        // Load data - post so fragments have time to attach on first frame
        viewPager.post(() -> updateList(30));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_tracker);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_tracker) {
                return true;
            } else if (itemId == R.id.nav_goals) {
                startActivity(new Intent(this, GoalsActivity.class));
                return true;
            } else if (itemId == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e("TrackerActivity", "Ad failed: " + loadAdError.getCode() + " " + loadAdError.getMessage());
                }
            });
            MobileAds.initialize(this, initializationStatus -> {
                adView.loadAd(new AdRequest.Builder().build());
            });
        }
    }

    private void updateList(int limit) {
        String title = limit == -1 ? "All Records" : "Last " + limit + " Records";
        tvTrackerTitle.setText(title);
        
        currentItems.clear();
        currentBestTimes = loadRecords(limit, currentItems);
        
        // Push data to both fragments
        pushDataToFragments();
    }

    private void pushDataToFragments() {
        ChartFragment chartFragment = pagerAdapter.getChartFragment();
        if (chartFragment.isAdded()) {
            chartFragment.updateChart(currentItems, currentBestTimes);
        }

        RecordsFragment recordsFragment = pagerAdapter.getRecordsFragment();
        if (recordsFragment.isAdded()) {
            recordsFragment.updateRecords(currentItems, currentBestTimes);
        } else {
            // Fragment not attached yet — retry after it has been drawn
            viewPager.post(() -> {
                if (recordsFragment.isAdded()) {
                    recordsFragment.updateRecords(currentItems, currentBestTimes);
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tracker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.filter_30) {
            updateList(30);
            return true;
        } else if (id == R.id.filter_60) {
            updateList(60);
            return true;
        } else if (id == R.id.filter_1year) {
            updateList(365);
            return true;
        } else if (id == R.id.filter_all) {
            updateList(-1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private long[] loadRecords(int limit, List<TrackerPojo> result) {
        long minFree = Long.MAX_VALUE, minFly = Long.MAX_VALUE, minBreast = Long.MAX_VALUE, minBack = Long.MAX_VALUE;
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        
        String limitClause = limit == -1 ? "" : " LIMIT " + limit;
        String dateQuery = "SELECT DISTINCT date FROM swim_sessions WHERE profile_id = ? ORDER BY date DESC" + limitClause;
        
        Cursor dateCursor = dbHelper.getReadableDatabase().rawQuery(dateQuery, new String[]{String.valueOf(activeProfileId)});
        List<String> dates = new ArrayList<>();
        if (dateCursor.moveToFirst()) {
            do {
                dates.add(dateCursor.getString(0));
            } while (dateCursor.moveToNext());
        }
        dateCursor.close();

        for (String date : dates) {
            long freeMs = 0, backMs = 0, breastMs = 0, flyMs = 0;
            Cursor c2 = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT MIN(NULLIF(freestyle_ms, 0)), MIN(NULLIF(backstroke_ms, 0)), MIN(NULLIF(breaststroke_ms, 0)), MIN(NULLIF(butterfly_ms, 0)) FROM swim_sessions WHERE date = ? AND profile_id = ?",
                    new String[]{date, String.valueOf(activeProfileId)}
            );
            if (c2.moveToFirst()) {
                freeMs = c2.isNull(0) ? 0 : c2.getLong(0);
                backMs = c2.isNull(1) ? 0 : c2.getLong(1);
                breastMs = c2.isNull(2) ? 0 : c2.getLong(2);
                flyMs = c2.isNull(3) ? 0 : c2.getLong(3);
            }
            c2.close();

            if (freeMs > 0 && freeMs < minFree) minFree = freeMs;
            if (flyMs > 0 && flyMs < minFly) minFly = flyMs;
            if (breastMs > 0 && breastMs < minBreast) minBreast = breastMs;
            if (backMs > 0 && backMs < minBack) minBack = backMs;

            result.add(new TrackerPojo(
                    date,
                    formatMs(freeMs),
                    formatMs(flyMs),
                    formatMs(breastMs),
                    formatMs(backMs),
                    freeMs, flyMs, breastMs, backMs
            ));
        }

        return new long[]{
            minFree == Long.MAX_VALUE ? 0 : minFree,
            minFly == Long.MAX_VALUE ? 0 : minFly,
            minBreast == Long.MAX_VALUE ? 0 : minBreast,
            minBack == Long.MAX_VALUE ? 0 : minBack
        };
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}
