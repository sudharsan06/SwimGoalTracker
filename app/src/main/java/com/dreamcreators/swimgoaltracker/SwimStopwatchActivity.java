package com.dreamcreators.swimgoaltracker;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SwimStopwatchActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long startFree = -1, elapsedFree = 0;
    private long startBack = -1, elapsedBack = 0;
    private long startBreast = -1, elapsedBreast = 0;
    private long startFly = -1, elapsedFly = 0;

    private TextView tvDate;
    private TextView tvFree, tvBack, tvBreast, tvFly;
    private TextView tvEntriesHeader;
    private NutritionDbHelper dbHelper;
    private RecyclerView recyclerViewEntries;
    private String selectedStroke = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(getColor(R.color.lightPrimaryDark));
        setContentView(R.layout.activity_swim_stopwatch);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Swim timer counter");
        // Setup toolbar
       /* Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }*/

        tvDate = findViewById(R.id.tvSwimDate);
        tvFree = findViewById(R.id.tvFree);
        tvBack = findViewById(R.id.tvBack);
        tvBreast = findViewById(R.id.tvBreast);
        tvFly = findViewById(R.id.tvFly);

        // Sections for each stroke (used to show/hide based on selected style from dashboard)
        LinearLayout sectionFree = findViewById(R.id.sectionFree);
        LinearLayout sectionBack = findViewById(R.id.sectionBack);
        LinearLayout sectionBreast = findViewById(R.id.sectionBreast);
        LinearLayout sectionFly = findViewById(R.id.sectionFly);

        Button btnFreeStart = findViewById(R.id.btnFreeStart);
        Button btnFreeStop = findViewById(R.id.btnFreeStop);
        Button btnFreeReset = findViewById(R.id.btnFreeReset);
        Button btnBackStart = findViewById(R.id.btnBackStart);
        Button btnBackStop = findViewById(R.id.btnBackStop);
        Button btnBackReset = findViewById(R.id.btnBackReset);
        Button btnBreastStart = findViewById(R.id.btnBreastStart);
        Button btnBreastStop = findViewById(R.id.btnBreastStop);
        Button btnBreastReset = findViewById(R.id.btnBreastReset);
        Button btnFlyStart = findViewById(R.id.btnFlyStart);
        Button btnFlyStop = findViewById(R.id.btnFlyStop);
        Button btnFlyReset = findViewById(R.id.btnFlyReset);
        Button btn_save_free = findViewById(R.id.btn_save_free);
        Button btn_save_back = findViewById(R.id.btn_save_back);
        Button btn_save_breast = findViewById(R.id.btn_save_breast);
        Button btn_save_fly = findViewById(R.id.btn_save_fly);

        // RecyclerView for entries list (shared for whichever style is visible)
        tvEntriesHeader = findViewById(R.id.tvEntriesHeader);
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries);
        recyclerViewEntries.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new NutritionDbHelper(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("Date: " + today);

        // Adjust UI based on which stroke was tapped on the dashboard
        // Dashboard passes extras: "Freestyle", "Backstroke", "Breaststroke", "Butterfly"
        // Also check if we're returning from SwimEntryActivity
        String stroke = getIntent().getStringExtra("stroke");
        if (stroke != null) {
            selectedStroke = stroke;
        }
        if (stroke != null) {
            // Default: hide all, then show only the selected section
            sectionFree.setVisibility(View.GONE);
            sectionBack.setVisibility(View.GONE);
            sectionBreast.setVisibility(View.GONE);
            sectionFly.setVisibility(View.GONE);

            if ("Freestyle".equalsIgnoreCase(stroke)) {
                sectionFree.setVisibility(View.VISIBLE);
                setTitle("Freestyle timer");
                loadTodayEntries("free", today);
            } else if ("Backstroke".equalsIgnoreCase(stroke)) {
                sectionBack.setVisibility(View.VISIBLE);
                setTitle("Backstroke timer");
                loadTodayEntries("back", today);
            } else if ("Breaststroke".equalsIgnoreCase(stroke)) {
                sectionBreast.setVisibility(View.VISIBLE);
                setTitle("Breaststroke timer");
                loadTodayEntries("breast", today);
            } else if ("Butterfly".equalsIgnoreCase(stroke)) {
                sectionFly.setVisibility(View.VISIBLE);
                setTitle("Butterfly timer");
                loadTodayEntries("fly", today);
            }
        } else {
            // If opened from menu, default to freestyle list
            loadTodayEntries("free", today);
        }

        btnFreeStart.setOnClickListener(v -> startTimer("free"));
        btnFreeStop.setOnClickListener(v -> stopTimer("free"));
        btnFreeReset.setOnClickListener(v -> resetTimer("free"));
        btn_save_free.setOnClickListener(v -> saveSession("free"));

        btnBackStart.setOnClickListener(v -> startTimer("back"));
        btnBackStop.setOnClickListener(v -> stopTimer("back"));
        btnBackReset.setOnClickListener(v -> resetTimer("back"));
        btn_save_back.setOnClickListener(v -> saveSession("back"));

        btnBreastStart.setOnClickListener(v -> startTimer("breast"));
        btnBreastStop.setOnClickListener(v -> stopTimer("breast"));
        btnBreastReset.setOnClickListener(v -> resetTimer("breast"));
        btn_save_breast.setOnClickListener(v -> saveSession("breast"));

        btnFlyStart.setOnClickListener(v -> startTimer("fly"));
        btnFlyStop.setOnClickListener(v -> stopTimer("fly"));
        btnFlyReset.setOnClickListener(v -> resetTimer("fly"));
        btn_save_fly.setOnClickListener(v -> saveSession("fly"));

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        
        ViewCompat.setOnApplyWindowInsetsListener(tvDate, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_tracker) {
                startActivity(new Intent(this, TrackerActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_swim_timer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Intent intent = new Intent(this, SwimEntryActivity.class);
            // Pass information about where we came from and which stroke is selected
            intent.putExtra("from_swim_stopwatch", true);
            if (selectedStroke != null) {
                intent.putExtra("selected_stroke", selectedStroke);
            }
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    private void startTimer(String which) {
        long now = System.currentTimeMillis();
        switch (which) {
            case "free":
                if (startFree < 0) startFree = now;
                break;
            case "back":
                if (startBack < 0) startBack = now;
                break;
            case "breast":
                if (startBreast < 0) startBreast = now;
                break;
            case "fly":
                if (startFly < 0) startFly = now;
                break;
        }
        if (startFree >= 0 || startBack >= 0 || startBreast >= 0 || startFly >= 0) {
            handler.removeCallbacks(tick);
            handler.post(tick);
        }
    }

    private void stopTimer(String which) {
        long now = System.currentTimeMillis();
        switch (which) {
            case "free":
                if (startFree >= 0) {
                    elapsedFree += now - startFree;
                    startFree = -1;
                }
                break;
            case "back":
                if (startBack >= 0) {
                    elapsedBack += now - startBack;
                    startBack = -1;
                }
                break;
            case "breast":
                if (startBreast >= 0) {
                    elapsedBreast += now - startBreast;
                    startBreast = -1;
                }
                break;
            case "fly":
                if (startFly >= 0) {
                    elapsedFly += now - startFly;
                    startFly = -1;
                }
                break;
        }
        updateLabels();
    }

    private void resetTimer(String which) {
        switch (which) {
            case "free":
                startFree = -1;
                elapsedFree = 0;
                break;
            case "back":
                startBack = -1;
                elapsedBack = 0;
                break;
            case "breast":
                startBreast = -1;
                elapsedBreast = 0;
                break;
            case "fly":
                startFly = -1;
                elapsedFly = 0;
                break;
        }
        updateLabels();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateLabels();
            if (startFree >= 0 || startBack >= 0 || startBreast >= 0 || startFly >= 0) {
                handler.postDelayed(this, 30);
            }
        }
    };

    private void updateLabels() {
        long now = System.currentTimeMillis();
        long showFree = elapsedFree + (startFree >= 0 ? now - startFree : 0);
        long showBack = elapsedBack + (startBack >= 0 ? now - startBack : 0);
        long showBreast = elapsedBreast + (startBreast >= 0 ? now - startBreast : 0);
        long showFly = elapsedFly + (startFly >= 0 ? now - startFly : 0);
        tvFree.setText(formatMs(showFree));
        tvBack.setText(formatMs(showBack));
        tvBreast.setText(formatMs(showBreast));
        tvFly.setText(formatMs(showFly));
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    private void saveSession(String which) {

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long createdAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        long showFree = elapsedFree + (startFree >= 0 ? now - startFree : 0);
        long showBack = elapsedBack + (startBack >= 0 ? now - startBack : 0);
        long showBreast = elapsedBreast + (startBreast >= 0 ? now - startBreast : 0);
        long showFly = elapsedFly + (startFly >= 0 ? now - startFly : 0);

        if (showFree == 0 && showBack == 0 && showBreast == 0 && showFly == 0) {
            Toast.makeText(this, "No time recorded to save", Toast.LENGTH_SHORT).show();
            return;
        } else {
            if (which.equals("free") && showFree < 10000) {
                ThrowAlertDialog("free");
            } else if (which.equals("back") && showBack < 10000) {
                ThrowAlertDialog("back");
            } else if (which.equals("breast") && showBreast < 10000) {
                ThrowAlertDialog("breast");
            } else if (which.equals("fly") && showFly < 10000) {
                ThrowAlertDialog("fly");
            } else {
                // proceed to save - always insert new entry for each attempt
                // This allows multiple attempts per day, and MIN() query will find the best time
                ContentValues values = new ContentValues();
                values.put("date", date);
                
                // Only set the selected style, others will default to 0
                // This allows tracking multiple attempts and finding the best time using MIN()
                if (which.equals("free")) {
                    values.put("freestyle_ms", showFree);
                } else if (which.equals("back")) {
                    values.put("backstroke_ms", showBack);
                } else if (which.equals("breast")) {
                    values.put("breaststroke_ms", showBreast);
                } else if (which.equals("fly")) {
                    values.put("butterfly_ms", showFly);
                }
                values.put("created_at", createdAt);
                
                long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
                if (id > 0) {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    // Stop and reset the timer after saving
                    stopTimer(which);
                    resetTimer(which);
                    // Refresh the entries list
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    loadTodayEntries(which, today);
                } else {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void ThrowAlertDialog(String which) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Warning")
                .setMessage("The recorded time for " + which + " is less than 10 seconds. Are you sure you want to save it?")
                .setPositiveButton("Yes", (dialog, whichButton) -> {
                    // User confirmed, proceed to save
                    saveSessionWithSelectedValue(which);
                })
                .setNegativeButton("No", (dialog, whichButton) -> {
                    // User cancelled, do nothing
                    dialog.dismiss();
                })
                .show();
    }

    private void saveSessionWithSelectedValue(String which) {

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long createdAt = System.currentTimeMillis();
        long now = System.currentTimeMillis();
        long showFree = elapsedFree + (startFree >= 0 ? now - startFree : 0);
        long showBack = elapsedBack + (startBack >= 0 ? now - startBack : 0);
        long showBreast = elapsedBreast + (startBreast >= 0 ? now - startBreast : 0);
        long showFly = elapsedFly + (startFly >= 0 ? now - startFly : 0);

        // Always insert new entry for each attempt
        // This allows multiple attempts per day, and MIN() query will find the best time
        ContentValues values = new ContentValues();
        values.put("date", date);
        
        // Only set the selected style, others will default to 0
        // This allows tracking multiple attempts and finding the best time using MIN()
        if (which.equals("free")) {
            values.put("freestyle_ms", showFree);
        } else if (which.equals("back")) {
            values.put("backstroke_ms", showBack);
        } else if (which.equals("breast")) {
            values.put("breaststroke_ms", showBreast);
        } else if (which.equals("fly")) {
            values.put("butterfly_ms", showFly);
        }
        values.put("created_at", createdAt);

        long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
        if (id > 0) {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            // Stop and reset the timer after saving
            stopTimer(which);
            resetTimer(which);
            // Refresh the entries list
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            loadTodayEntries(which, today);
        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadTodayEntries(String style, String date) {
        String columnName;

        switch (style) {
            case "free":
                columnName = "freestyle_ms";
                break;
            case "back":
                columnName = "backstroke_ms";
                break;
            case "breast":
                columnName = "breaststroke_ms";
                break;
            case "fly":
                columnName = "butterfly_ms";
                break;
            default:
                return;
        }

        // Query database for today's entries of this style (non-zero values only), newest first
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT id, " + columnName + ", created_at FROM swim_sessions WHERE date = ? AND " + columnName + " > 0 ORDER BY created_at DESC",
                new String[]{date}
        );

        List<SwimTimingEntry> entries = new ArrayList<>();
        long bestTime = Long.MAX_VALUE;

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            long timeMs = cursor.getLong(1);
            long createdAt = cursor.isNull(2) ? 0 : cursor.getLong(2);
            if (timeMs > 0) {
                entries.add(new SwimTimingEntry(id, timeMs, createdAt));
                if (timeMs < bestTime) {
                    bestTime = timeMs;
                }
            }
        }
        cursor.close();

        if (entries.isEmpty()) {
            // No entries: hide header and clear list
            tvEntriesHeader.setVisibility(View.GONE);
            recyclerViewEntries.setAdapter(null);
        } else {
            tvEntriesHeader.setVisibility(View.VISIBLE);
            long nowMs = System.currentTimeMillis();
            SwimTimingAdapter adapter = new SwimTimingAdapter(this, entries, bestTime == Long.MAX_VALUE ? 0 : bestTime, nowMs, entry -> {
                deleteEntry(entry.getId(), style, date);
            });
            recyclerViewEntries.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh entries when activity resumes (e.g., when returning from manual entry)
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Check if stroke was passed in intent (might be updated when returning from SwimEntryActivity)
        String strokeFromIntent = getIntent().getStringExtra("stroke");
        if (strokeFromIntent != null) {
            selectedStroke = strokeFromIntent;
        }
        
        if (selectedStroke != null) {
            if ("Freestyle".equalsIgnoreCase(selectedStroke)) {
                loadTodayEntries("free", today);
            } else if ("Backstroke".equalsIgnoreCase(selectedStroke)) {
                loadTodayEntries("back", today);
            } else if ("Breaststroke".equalsIgnoreCase(selectedStroke)) {
                loadTodayEntries("breast", today);
            } else if ("Butterfly".equalsIgnoreCase(selectedStroke)) {
                loadTodayEntries("fly", today);
            }
        } else {
            // Default to freestyle list if nothing specific selected
            loadTodayEntries("free", today);
        }
    }

    private void deleteEntry(long id, String style, String date) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this timing entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int deleted = dbHelper.getWritableDatabase().delete("swim_sessions", "id = ?", new String[]{String.valueOf(id)});
                    if (deleted > 0) {
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        loadTodayEntries(style, date);
                    } else {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}