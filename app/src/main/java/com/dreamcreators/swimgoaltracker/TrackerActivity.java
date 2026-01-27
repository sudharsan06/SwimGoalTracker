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
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.lightPrimaryDark));
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
        
        ViewCompat.setOnApplyWindowInsetsListener(tvTrackerTitle, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

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
        
        List<TrackerPojo> items = loadRecords(limit);
        adapter = new TrackerListAdapter(this, items);
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

    private List<TrackerPojo> loadRecords(int limit) {
        List<TrackerPojo> result = new ArrayList<>();
        
        // Query to get the last X distinct dates that have swim data
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

            result.add(new TrackerPojo(
                    date,
                    formatMs(freeMs),
                    formatMs(flyMs),
                    formatMs(breastMs),
                    formatMs(backMs)
            ));
        }

        return result;
    }


    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (ms % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }
}


