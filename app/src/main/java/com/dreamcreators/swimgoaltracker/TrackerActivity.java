package com.dreamcreators.swimgoaltracker;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrackerActivity extends AppCompatActivity {

    private NutritionDbHelper dbHelper;
    private RecyclerView recyclerView;
    private TrackerListAdapter adapter;
    private TextView tvTrackerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        setContentView(R.layout.activity_tracker);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Swim Tracker");
        }

        recyclerView = findViewById(R.id.listTracker);
        tvTrackerTitle = findViewById(R.id.tvTrackerTitle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new NutritionDbHelper(this);

        updateList(30);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        android.view.View trackerHeader = findViewById(R.id.trackerHeader);

        bottomNav.setSelectedItemId(R.id.nav_tracker);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_tracker) {
                // Already on Tracker
                return true;
            }
            return false;
        });

        MobileAds.initialize(this, initializationStatus -> {
        });
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    private void updateList(int limit) {
        String title = limit == -1 ? "All Records" : "Last " + limit + " Records";
        tvTrackerTitle.setText(title);
        
        List<TrackerPojo> items = new ArrayList<>();
        long[] bestTimes = loadRecords(limit, items);
        adapter = new TrackerListAdapter(this, items, bestTimes[0], bestTimes[1], bestTimes[2], bestTimes[3]);
        recyclerView.setAdapter(adapter);
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
        
        // Query to get the last X distinct dates that have swim data, ordered by date descending
        String limitClause = limit == -1 ? "" : " LIMIT " + limit;
        String dateQuery = "SELECT DISTINCT date FROM swim_sessions ORDER BY date DESC" + limitClause;
        
        Cursor dateCursor = dbHelper.getReadableDatabase().rawQuery(dateQuery, null);
        List<String> dates = new ArrayList<>();
        if (dateCursor.moveToFirst()) {
            do {
                dates.add(dateCursor.getString(0));
            } while (dateCursor.moveToNext());
        }
        dateCursor.close();

        for (String date : dates) {
            // Swim session query
            long freeMs = 0, backMs = 0, breastMs = 0, flyMs = 0;
            Cursor c2 = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT MIN(NULLIF(freestyle_ms, 0)), MIN(NULLIF(backstroke_ms, 0)), MIN(NULLIF(breaststroke_ms, 0)), MIN(NULLIF(butterfly_ms, 0)) FROM swim_sessions WHERE date = ?",
                    new String[]{date}
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


