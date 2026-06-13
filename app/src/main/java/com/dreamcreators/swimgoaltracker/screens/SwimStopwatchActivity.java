package com.dreamcreators.swimgoaltracker.screens;

import static android.view.View.VISIBLE;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.adapter.SwimTimingAdapter;
import com.dreamcreators.swimgoaltracker.pojo.SwimTimingEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.core.view.WindowCompat;

public class SwimStopwatchActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private long startFree = -1, elapsedFree = 0;
    private long startBack = -1, elapsedBack = 0;
    private long startBreast = -1, elapsedBreast = 0;
    private long startFly = -1, elapsedFly = 0;

    // IM (Individual Medley) timing fields
    private long startIM = -1, elapsedIM = 0;
    private long imSplitFly = 0, imSplitBack = 0, imSplitBreast = 0, imSplitFree = 0;
    private int imStrokeIndex = 0; // 0=Fly, 1=Back, 2=Breast, 3=Free, 4=Done
    private TextView tvIM, tvImFlyVal, tvImBackVal, tvImBreastVal, tvImFreeVal;
    private TextView tvLabelImFly, tvLabelImBack, tvLabelImBreast, tvLabelImFree;
    private View layImFly, layImBack, layImBreast, layImFree;

    private TextView tvDate;
    private TextView tvPoolDistanceInfo;
    private TextView tvFree, tvBack, tvBreast, tvFly;
    private TextView tvEntriesHeader;
    private android.widget.Spinner spinnerImFilter;
    private int currentImFilter = 1; // Default: 1 = Last 30 Days
    private NutritionDbHelper dbHelper;
    private RecyclerView recyclerViewEntries;
    private String selectedStroke = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_swim_stopwatch);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvDate = findViewById(R.id.tvSwimDate);
        tvPoolDistanceInfo = findViewById(R.id.tvPoolDistanceInfo);
        tvFree = findViewById(R.id.tvFree);
        tvBack = findViewById(R.id.tvBack);
        tvBreast = findViewById(R.id.tvBreast);
        tvFly = findViewById(R.id.tvFly);

        // Sections for each stroke
        View sectionFree = findViewById(R.id.sectionFree);
        View sectionBack = findViewById(R.id.sectionBack);
        View sectionBreast = findViewById(R.id.sectionBreast);
        View sectionFly = findViewById(R.id.sectionFly);
        View sectionIM = findViewById(R.id.sectionIM);

        // IM views
        tvIM = findViewById(R.id.tvIM);
        tvImFlyVal = findViewById(R.id.tvImFlyVal);
        tvImBackVal = findViewById(R.id.tvImBackVal);
        tvImBreastVal = findViewById(R.id.tvImBreastVal);
        tvImFreeVal = findViewById(R.id.tvImFreeVal);
        tvLabelImFly = findViewById(R.id.tvLabelImFly);
        tvLabelImBack = findViewById(R.id.tvLabelImBack);
        tvLabelImBreast = findViewById(R.id.tvLabelImBreast);
        tvLabelImFree = findViewById(R.id.tvLabelImFree);
        layImFly = findViewById(R.id.layImFly);
        layImBack = findViewById(R.id.layImBack);
        layImBreast = findViewById(R.id.layImBreast);
        layImFree = findViewById(R.id.layImFree);
        Button btnIMStart = findViewById(R.id.btnIMStart);
        Button btnIMNext = findViewById(R.id.btnIMNext);
        Button btnIMReset = findViewById(R.id.btnIMReset);
        Button btn_save_im = findViewById(R.id.btn_save_im);

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

        // RecyclerView for entries list
        tvEntriesHeader = findViewById(R.id.tvEntriesHeader);
        spinnerImFilter = findViewById(R.id.spinnerImFilter);
        recyclerViewEntries = findViewById(R.id.recyclerViewEntries);
        recyclerViewEntries.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new NutritionDbHelper(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("DATE: " + today);

        // Adjust UI based on stroke intent
        String stroke = getIntent().getStringExtra("stroke");
        if (stroke != null) {
            selectedStroke = stroke;
            sectionFree.setVisibility(View.GONE);
            sectionBack.setVisibility(View.GONE);
            sectionBreast.setVisibility(View.GONE);
            sectionFly.setVisibility(View.GONE);
            sectionIM.setVisibility(View.GONE);

            if ("Freestyle".equalsIgnoreCase(stroke)) {
                sectionFree.setVisibility(VISIBLE);
                setTitle("Freestyle Timer");
                selectedStroke = "Freestyle";
                tvEntriesHeader.setText("TODAY'S SESSIONS");
                spinnerImFilter.setVisibility(View.GONE);
                loadTodayEntries("free", today);
            } else if ("Backstroke".equalsIgnoreCase(stroke)) {
                sectionBack.setVisibility(VISIBLE);
                setTitle("Backstroke Timer");
                selectedStroke = "Backstroke";
                loadTodayEntries("back", today);
                tvEntriesHeader.setText("TODAY'S SESSIONS");
                spinnerImFilter.setVisibility(View.GONE);
            } else if ("Breaststroke".equalsIgnoreCase(stroke)) {
                sectionBreast.setVisibility(VISIBLE);
                setTitle("Breaststroke Timer");
                loadTodayEntries("breast", today);
                selectedStroke = "Breaststroke";
                tvEntriesHeader.setText("TODAY'S SESSIONS");
                spinnerImFilter.setVisibility(View.GONE);
            } else if ("Butterfly".equalsIgnoreCase(stroke)) {
                sectionFly.setVisibility(VISIBLE);
                setTitle("Butterfly Timer");
                loadTodayEntries("fly", today);
                tvEntriesHeader.setText("TODAY'S SESSIONS");
                spinnerImFilter.setVisibility(View.GONE);
                selectedStroke = "Butterfly";
                tvDate.setText(today);
                loadTodayEntries("fly", today);
            } else if ("IM".equalsIgnoreCase(stroke)) {
                getSupportActionBar().setTitle("IM Timer");
                sectionIM.setVisibility(VISIBLE);
                tvEntriesHeader.setText("IM SESSIONS");
                spinnerImFilter.setVisibility(VISIBLE);
                selectedStroke = "IM";
                tvDate.setText(today);
                
                // Initialize Spinner
                String[] filters = {"Today", "Last 30 Days", "Last 60 Days", "Last 1 Year", "All Records"};
                android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filters);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerImFilter.setAdapter(spinnerAdapter);
                spinnerImFilter.setSelection(currentImFilter);
                
                spinnerImFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        currentImFilter = position;
                        loadTodayEntries("im", today);
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
                
                loadTodayEntries("im", today);
            }
        } else {
            setTitle("Swim Timer");
            loadTodayEntries("free", today);
        }

        // Initialize Ads
        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e("SwimStopwatchActivity", "Ad failed: " + loadAdError.getCode() + " " + loadAdError.getMessage());
                }
            });
            MobileAds.initialize(this, initializationStatus -> {
                adView.loadAd(new AdRequest.Builder().build());
            });
        }

        btnFreeStart.setOnClickListener(v -> { vibrate(); startTimer("free"); });
        btnFreeStop.setOnClickListener(v -> { vibrate(); stopTimer("free"); });
        btnFreeReset.setOnClickListener(v -> { vibrate(); resetTimer("free"); });
        btn_save_free.setOnClickListener(v -> { vibrate(); saveSession("free"); });

        btnBackStart.setOnClickListener(v -> { vibrate(); startTimer("back"); });
        btnBackStop.setOnClickListener(v -> { vibrate(); stopTimer("back"); });
        btnBackReset.setOnClickListener(v -> { vibrate(); resetTimer("back"); });
        btn_save_back.setOnClickListener(v -> { vibrate(); saveSession("back"); });

        btnBreastStart.setOnClickListener(v -> { vibrate(); startTimer("breast"); });
        btnBreastStop.setOnClickListener(v -> { vibrate(); stopTimer("breast"); });
        btnBreastReset.setOnClickListener(v -> { vibrate(); resetTimer("breast"); });
        btn_save_breast.setOnClickListener(v -> { vibrate(); saveSession("breast"); });

        btnFlyStart.setOnClickListener(v -> { vibrate(); startTimer("fly"); });
        btnFlyStop.setOnClickListener(v -> { vibrate(); stopTimer("fly"); });
        btnFlyReset.setOnClickListener(v -> { vibrate(); resetTimer("fly"); });
        btn_save_fly.setOnClickListener(v -> { vibrate(); saveSession("fly"); });

        btnIMStart.setOnClickListener(v -> { vibrate(); startIMTimer(); });
        btnIMNext.setOnClickListener(v -> { vibrate(); nextIMStroke(); });
        btnIMReset.setOnClickListener(v -> { vibrate(); resetIMTimer(); });
        btn_save_im.setOnClickListener(v -> { vibrate(); saveSession("im"); });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        
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
    }

    private void startIMTimer() {
        // Initialize IM timer with Fly stroke first
        startIM = System.currentTimeMillis();
        imSplitFly = imSplitBack = imSplitBreast = imSplitFree = 0;
        imStrokeIndex = 0;
        updateImSplitHighlight();
        handler.post(tick);
    }

    private void nextIMStroke() {
        if (startIM < 0) return;
        long now = System.currentTimeMillis();
        long split = now - startIM;
        switch (imStrokeIndex) {
            case 0: imSplitFly = split; break;
            case 1: imSplitBack = split; break;
            case 2: imSplitBreast = split; break;
            case 3: imSplitFree = split; break;
        }
        updateImSplitValues();
        imStrokeIndex++;
        if (imStrokeIndex < 4) {
            startIM = now;
            updateImSplitHighlight();
        } else {
            elapsedIM = imSplitFly + imSplitBack + imSplitBreast + imSplitFree;
            startIM = -1;
            tvIM.setText(formatMs(elapsedIM));
            clearImHighlight();
        }
    }

    private void resetIMTimer() {
        startIM = -1;
        elapsedIM = 0;
        imSplitFly = imSplitBack = imSplitBreast = imSplitFree = 0;
        imStrokeIndex = 0;
        tvIM.setText(formatMs(0));
        updateImSplitValues();
        clearImHighlight();
    }

    private void updateImSplitValues() {
        tvImFlyVal.setText(formatMs(imSplitFly));
        tvImBackVal.setText(formatMs(imSplitBack));
        tvImBreastVal.setText(formatMs(imSplitBreast));
        tvImFreeVal.setText(formatMs(imSplitFree));
    }

    private void updateImSplitHighlight() {
        int activeColor = getColor(R.color.black);
        int inactiveColor = getColor(R.color.steel_blue);

        tvLabelImFly.setTextColor(imStrokeIndex == 0 ? activeColor : inactiveColor);
        tvImFlyVal.setTextColor(imStrokeIndex == 0 ? activeColor : inactiveColor);
        tvLabelImFly.setTypeface(null, imStrokeIndex == 0 ? Typeface.BOLD : Typeface.NORMAL);
        tvImFlyVal.setTypeface(null, imStrokeIndex == 0 ? Typeface.BOLD : Typeface.NORMAL);

        tvLabelImBack.setTextColor(imStrokeIndex == 1 ? activeColor : inactiveColor);
        tvImBackVal.setTextColor(imStrokeIndex == 1 ? activeColor : inactiveColor);
        tvLabelImBack.setTypeface(null, imStrokeIndex == 1 ? Typeface.BOLD : Typeface.NORMAL);
        tvImBackVal.setTypeface(null, imStrokeIndex == 1 ? Typeface.BOLD : Typeface.NORMAL);

        tvLabelImBreast.setTextColor(imStrokeIndex == 2 ? activeColor : inactiveColor);
        tvImBreastVal.setTextColor(imStrokeIndex == 2 ? activeColor : inactiveColor);
        tvLabelImBreast.setTypeface(null, imStrokeIndex == 2 ? Typeface.BOLD : Typeface.NORMAL);
        tvImBreastVal.setTypeface(null, imStrokeIndex == 2 ? Typeface.BOLD : Typeface.NORMAL);

        tvLabelImFree.setTextColor(imStrokeIndex == 3 ? activeColor : inactiveColor);
        tvImFreeVal.setTextColor(imStrokeIndex == 3 ? activeColor : inactiveColor);
        tvLabelImFree.setTypeface(null, imStrokeIndex == 3 ? Typeface.BOLD : Typeface.NORMAL);
        tvImFreeVal.setTypeface(null, imStrokeIndex == 3 ? Typeface.BOLD : Typeface.NORMAL);

        layImFly.setAlpha(1.0f);
        layImBack.setAlpha(1.0f);
        layImBreast.setAlpha(1.0f);
        layImFree.setAlpha(1.0f);
    }

    private void clearImHighlight() {
        int inactiveColor = getColor(R.color.steel_blue);

        tvLabelImFly.setTextColor(inactiveColor);
        tvImFlyVal.setTextColor(inactiveColor);
        tvLabelImFly.setTypeface(null, Typeface.NORMAL);
        tvImFlyVal.setTypeface(null, Typeface.NORMAL);

        tvLabelImBack.setTextColor(inactiveColor);
        tvImBackVal.setTextColor(inactiveColor);
        tvLabelImBack.setTypeface(null, Typeface.NORMAL);
        tvImBackVal.setTypeface(null, Typeface.NORMAL);

        tvLabelImBreast.setTextColor(inactiveColor);
        tvImBreastVal.setTextColor(inactiveColor);
        tvLabelImBreast.setTypeface(null, Typeface.NORMAL);
        tvImBreastVal.setTypeface(null, Typeface.NORMAL);

        tvLabelImFree.setTextColor(inactiveColor);
        tvImFreeVal.setTextColor(inactiveColor);
        tvLabelImFree.setTypeface(null, Typeface.NORMAL);
        tvImFreeVal.setTypeface(null, Typeface.NORMAL);

        layImFly.setAlpha(1.0f);
        layImBack.setAlpha(1.0f);
        layImBreast.setAlpha(1.0f);
        layImFree.setAlpha(1.0f);
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
            case "im":
                if (startIM >= 0) {
                    elapsedIM += now - startIM;
                    startIM = -1;
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
            case "im":
                resetIMTimer();
                break;
        }
        updateLabels();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateLabels();
            if (startFree >= 0 || startBack >= 0 || startBreast >= 0 || startFly >= 0 || startIM >= 0) {
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
        long imTotal = 0L;
        if (imStrokeIndex > 0) {
            // sum of completed splits
            imTotal += imSplitFly + imSplitBack + imSplitBreast + imSplitFree;
        }
        if (startIM >= 0) {
            // add current running split
            long elapsedCurrent = now - startIM;
            long completed = 0L;
            switch (imStrokeIndex) {
                case 0: // Fly in progress
                    completed = 0L;
                    break;
                case 1: // Back in progress, Fly completed
                    completed = imSplitFly;
                    break;
                case 2: // Breast in progress, Fly+Back completed
                    completed = imSplitFly + imSplitBack;
                    break;
                case 3: // Free in progress, Fly+Back+Breast completed
                    completed = imSplitFly + imSplitBack + imSplitBreast;
                    break;
                case 4: // Done
                    completed = imSplitFly + imSplitBack + imSplitBreast + imSplitFree;
                    break;
            }
            imTotal = completed + elapsedCurrent;
        } else {
            // Not running, total is sum of splits
            imTotal = imSplitFly + imSplitBack + imSplitBreast + imSplitFree;
        }
        tvFree.setText(formatMs(showFree));
        tvBack.setText(formatMs(showBack));
        tvBreast.setText(formatMs(showBreast));
        tvFly.setText(formatMs(showFly));
        tvIM.setText(formatMs(imTotal));
        tvImFlyVal.setText(formatMs(imSplitFly));
        tvImBackVal.setText(formatMs(imSplitBack));
        tvImBreastVal.setText(formatMs(imSplitBreast));
        tvImFreeVal.setText(formatMs(imSplitFree));
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
        
        if (which.equals("im")) {
            // If the user taps Save while the final stroke (Freestyle) is running,
            // automatically finish the stroke and stop the timer for them.
            if (imStrokeIndex == 3 && startIM >= 0) {
                nextIMStroke();
            } else if (imStrokeIndex < 4 && (startIM >= 0 || imSplitFly > 0 || imSplitBack > 0 || imSplitBreast > 0)) {
                // If they are on strokes 1, 2, or 3, or stopped halfway
                Toast.makeText(this, "Complete all 4 strokes before saving", Toast.LENGTH_SHORT).show();
                return;
            }
            long totalMS = elapsedIM;
            if (totalMS <= 0) {
                // fallback: sum of splits
                totalMS = imSplitFly + imSplitBack + imSplitBreast + imSplitFree;
            }
            if (totalMS <= 0) {
                Toast.makeText(this, "No IM time recorded to save", Toast.LENGTH_SHORT).show();
                return;
            }
            ContentValues values = new ContentValues();
            values.put("date", date);
            values.put("profile_id", ProfileManager.getActiveProfileId(this));
            values.put("im_ms", totalMS);
            values.put("im_butterfly_ms", imSplitFly);
            values.put("im_backstroke_ms", imSplitBack);
            values.put("im_breaststroke_ms", imSplitBreast);
            values.put("im_freestyle_ms", imSplitFree);
            values.put("created_at", createdAt);
            long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
            if (id > 0) {
                Toast.makeText(this, "Saved ✅", Toast.LENGTH_SHORT).show();
                updateLastCheckedSessionId(id);
                checkGoalAchievement(which, totalMS);
                stopTimer(which);
                resetTimer(which);
                loadTodayEntries(which, date);
            } else {
                Toast.makeText(this, "Save failed ❌", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        long now = System.currentTimeMillis();
        long showFree = elapsedFree + (startFree >= 0 ? now - startFree : 0);
        long showBack = elapsedBack + (startBack >= 0 ? now - startBack : 0);
        long showBreast = elapsedBreast + (startBreast >= 0 ? now - startBreast : 0);
        long showFly = elapsedFly + (startFly >= 0 ? now - startFly : 0);

        if (showFree == 0 && showBack == 0 && showBreast == 0 && showFly == 0) {
            Toast.makeText(this, "No time recorded to save", Toast.LENGTH_SHORT).show();
            return;
        } else {
            if (which.equals("free") && showFree < 2000) {
                ThrowAlertDialog("free");
            } else if (which.equals("back") && showBack < 2000) {
                ThrowAlertDialog("back");
            } else if (which.equals("breast") && showBreast < 2000) {
                ThrowAlertDialog("breast");
            } else if (which.equals("fly") && showFly < 2000) {
                ThrowAlertDialog("fly");
            } else {
                ContentValues values = new ContentValues();
                values.put("date", date);
                values.put("profile_id", ProfileManager.getActiveProfileId(this));
                
                if (which.equals("free")) {
                    values.put("freestyle_ms", showFree);
                } else if (which.equals("back")) {
                    values.put("backstroke_ms", showBack);
                } else if (which.equals("breast")) {
                    values.put("breaststroke_ms", showBreast);
                } else if (which.equals("fly")) {
                    values.put("butterfly_ms", showFly);
                } else if (which.equals("im")) {
                    values.put("im_ms", elapsedIM);
                }
                values.put("created_at", createdAt);
                
                long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
                if (id > 0) {
                    Toast.makeText(this, "Saved ✅", Toast.LENGTH_SHORT).show();
                    updateLastCheckedSessionId(id);
                    
                    if (which.equals("free")) checkGoalAchievement("free", showFree);
                    else if (which.equals("back")) checkGoalAchievement("back", showBack);
                    else if (which.equals("breast")) checkGoalAchievement("breast", showBreast);
                    else if (which.equals("fly")) checkGoalAchievement("fly", showFly);
                    
                    stopTimer(which);
                    resetTimer(which);
                    loadTodayEntries(which, date);
                } else {
                    Toast.makeText(this, "Save failed ❌", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void checkGoalAchievement(String style, long timeMs) {
        int activeId = ProfileManager.getActiveProfileId(this);
        android.content.SharedPreferences alertPrefs = getSharedPreferences("swim_alerts", MODE_PRIVATE);
        boolean goalAlertEnabled = alertPrefs.getBoolean("goal_alert_" + activeId, false);
        
        if (!goalAlertEnabled) return;
        
        android.content.SharedPreferences goalPrefs = getSharedPreferences("swim_goals", MODE_PRIVATE);
        String goalKey = "goal_" + style + "_" + activeId; 
        String goalStr = goalPrefs.getString(goalKey, "");
        long goalMs = parseGoalToMs(goalStr);
        
        if (goalMs > 0 && timeMs <= goalMs) {
            showGoalAchievedAlert(style, timeMs, goalMs);
        }
    }

    private long parseGoalToMs(String goalStr) {
        if (goalStr == null || goalStr.trim().isEmpty()) return -1;
        try {
            String[] parts = goalStr.split(":");
            if (parts.length == 3) {
                long mins = Long.parseLong(parts[0].trim());
                long secs = Long.parseLong(parts[1].trim());
                long hundredths = Long.parseLong(parts[2].trim());
                return (mins * 60 + secs) * 1000 + hundredths * 10;
            } else if (parts.length == 2) {
                long mins = Long.parseLong(parts[0].trim());
                double secs = Double.parseDouble(parts[1].trim());
                return (long) ((mins * 60 + secs) * 1000);
            } else if (parts.length == 1) { 
                return (long) (Double.parseDouble(parts[0].trim()) * 1000);
            }
        } catch (Exception e) {}
        return -1;
    }

    private void showGoalAchievedAlert(String style, long timeMs, long goalMs) {
        String styleName = style.substring(0, 1).toUpperCase() + style.substring(1);
        if ("im".equals(style)) styleName = "IM";
        
        String msg = "Congratulations! You just beat your " + styleName + " target time.\n\n" +
                     "Your Time: " + formatMs(timeMs) + "\n" +
                     "Target Goal: " + formatMs(goalMs);

        // Play clap sound
        try {
            android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, R.raw.clap);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
            }
        } catch (Exception e) {
            // ignore if sound fails
        }

        // Show Konfetti
        nl.dionsegijn.konfetti.xml.KonfettiView konfettiView = findViewById(R.id.konfettiView);
        if (konfettiView != null) {
            nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig = new nl.dionsegijn.konfetti.core.emitter.Emitter(5L, java.util.concurrent.TimeUnit.SECONDS).perSecond(100);
            konfettiView.start(
                    new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                            .angle(nl.dionsegijn.konfetti.core.Angle.BOTTOM)
                            .spread(nl.dionsegijn.konfetti.core.Spread.ROUND)
                            .setSpeedBetween(0f, 15f)
                            .timeToLive(2000L)
                            .colors(java.util.Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                            .sizes(new nl.dionsegijn.konfetti.core.models.Size(12, 5f, 0.2f))
                            .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, -0.1))
                            .build()
            );
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Goal Achieved! 🎉")
                .setMessage(msg)
                .setPositiveButton("Awesome", null)
                .show();
    }

    private void ThrowAlertDialog(String which) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Warning")
                .setMessage("The recorded time for " + which + " is less than 2 seconds. Are you sure you want to save it?")
                .setPositiveButton("Yes", (dialog, whichButton) -> {
                    saveSessionWithSelectedValue(which);
                })
                .setNegativeButton("No", (dialog, whichButton) -> {
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

        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("profile_id", ProfileManager.getActiveProfileId(this));
        
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
            Toast.makeText(this, "Saved ✅", Toast.LENGTH_SHORT).show();
            updateLastCheckedSessionId(id);
            stopTimer(which);
            resetTimer(which);
            loadTodayEntries(which, date);
        } else {
            Toast.makeText(this, "Save failed ❌", Toast.LENGTH_SHORT).show();
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
            case "im":
                columnName = "im_ms";
                break;
            default:
                return;
        }

        String whereClause;
        String[] queryArgs;
        int activeProfileId = ProfileManager.getActiveProfileId(this);

        if ("im".equals(style)) {
            if (currentImFilter == 0) { // Today
                whereClause = "date = ? AND " + columnName + " > 0 AND profile_id = ?";
                queryArgs = new String[]{date, String.valueOf(activeProfileId)};
            } else if (currentImFilter == 4) { // All Records
                whereClause = columnName + " > 0 AND profile_id = ?";
                queryArgs = new String[]{String.valueOf(activeProfileId)};
            } else {
                int days = currentImFilter == 1 ? 30 : (currentImFilter == 2 ? 60 : 365);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DAY_OF_YEAR, -days);
                String pastDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
                whereClause = "date >= ? AND " + columnName + " > 0 AND profile_id = ?";
                queryArgs = new String[]{pastDate, String.valueOf(activeProfileId)};
            }
        } else {
            whereClause = "date = ? AND " + columnName + " > 0 AND profile_id = ?";
            queryArgs = new String[]{date, String.valueOf(activeProfileId)};
        }

        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT id, " + columnName + ", created_at, date, im_butterfly_ms, im_backstroke_ms, im_breaststroke_ms, im_freestyle_ms FROM swim_sessions WHERE " + whereClause + " ORDER BY created_at DESC",
                queryArgs
        );

        List<SwimTimingEntry> entries = new ArrayList<>();
        long bestTime = Long.MAX_VALUE;

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            long timeMs = cursor.getLong(1);
            long createdAt = cursor.isNull(2) ? 0 : cursor.getLong(2);
            String entryDate = cursor.isNull(3) ? date : cursor.getString(3);
            if (timeMs > 0) {
                SwimTimingEntry entry = new SwimTimingEntry(id, timeMs, createdAt, entryDate);
                entry.setSplits(cursor.isNull(4) ? 0 : cursor.getLong(4),
                                cursor.isNull(5) ? 0 : cursor.getLong(5),
                                cursor.isNull(6) ? 0 : cursor.getLong(6),
                                cursor.isNull(7) ? 0 : cursor.getLong(7));
                entries.add(entry);
                if (timeMs < bestTime) {
                    bestTime = timeMs;
                }
            }
        }
        cursor.close();

        if (entries.isEmpty()) {
            // No entries: clear list
            recyclerViewEntries.setAdapter(null);
        } else {
            long nowMs = System.currentTimeMillis();
            final long finalBestTime = (bestTime == Long.MAX_VALUE) ? 0 : bestTime;

            int activeId = ProfileManager.getActiveProfileId(this);
            android.content.SharedPreferences goalPrefs = getSharedPreferences("swim_goals", MODE_PRIVATE);
            String styleKey = style;
            if (styleKey.equals("fly")) styleKey = "fly";
            else if (styleKey.equals("breast")) styleKey = "breast";
            else if (styleKey.equals("back")) styleKey = "back";
            else if (styleKey.equals("free")) styleKey = "free";
            else if (styleKey.equals("im")) styleKey = "im";
            
            String goalKey = "goal_" + styleKey + "_" + activeId; 
            String goalStr = goalPrefs.getString(goalKey, "");
            long goalMs = parseGoalToMs(goalStr);

            SwimTimingAdapter adapter = new SwimTimingAdapter(this, entries, finalBestTime, nowMs, goalMs, entry -> {
                deleteEntry(entry.getId(), style, date);
            });
            adapter.setOnItemClickListener(entry -> {
                boolean isBest = (entry.getTimeMs() == finalBestTime && finalBestTime > 0);
                showPaceDialog(entry, style, isBest);
            });
            recyclerViewEntries.setAdapter(adapter);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePoolDistanceInfo();
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
            } else if ("IM".equalsIgnoreCase(selectedStroke)) {
                loadTodayEntries("im", today);
            } else {
                // Default to freestyle list if nothing specific selected
                loadTodayEntries("free", today);
            }
        }
        
        // Check if a new session was added (e.g. manually) and verify if it achieved a goal
        checkNewSessionGoalAchievement();
    }

    private void updateLastCheckedSessionId(long id) {
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        getSharedPreferences("swim_goals_check", MODE_PRIVATE)
                .edit()
                .putLong("last_checked_session_id_" + activeProfileId, id)
                .apply();
    }

    private void checkNewSessionGoalAchievement() {
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT id, freestyle_ms, backstroke_ms, breaststroke_ms, butterfly_ms, im_ms FROM swim_sessions WHERE profile_id = ? ORDER BY id DESC LIMIT 1",
                    new String[]{String.valueOf(activeProfileId)}
            );
            if (cursor.moveToFirst()) {
                long latestId = cursor.getLong(0);
                long freeMs = cursor.getLong(1);
                long backMs = cursor.getLong(2);
                long breastMs = cursor.getLong(3);
                long flyMs = cursor.getLong(4);
                long imMs = cursor.getLong(5);

                android.content.SharedPreferences prefs = getSharedPreferences("swim_goals_check", MODE_PRIVATE);
                long lastCheckedId = prefs.getLong("last_checked_session_id_" + activeProfileId, -1);

                // If this is the very first time running, initialize lastCheckedId to the current latest ID
                // to avoid popping up for old history sessions on startup.
                if (lastCheckedId == -1) {
                    prefs.edit().putLong("last_checked_session_id_" + activeProfileId, latestId).apply();
                    return;
                }

                if (latestId > lastCheckedId) {
                    // Mark as checked immediately
                    prefs.edit().putLong("last_checked_session_id_" + activeProfileId, latestId).apply();

                    // Find which style was saved in this session
                    String style = null;
                    long timeMs = 0;
                    if (freeMs > 0) {
                        style = "free";
                        timeMs = freeMs;
                    } else if (backMs > 0) {
                        style = "back";
                        timeMs = backMs;
                    } else if (breastMs > 0) {
                        style = "breast";
                        timeMs = breastMs;
                    } else if (flyMs > 0) {
                        style = "fly";
                        timeMs = flyMs;
                    } else if (imMs > 0) {
                        style = "im";
                        timeMs = imMs;
                    }

                    if (style != null) {
                        checkGoalAchievement(style, timeMs);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SwimStopwatchActivity", "Error checking new session goal achievement", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updatePoolDistanceInfo() {
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        int poolDistance = 18; // default
        try {
            Cursor c = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT pool_distance FROM profile WHERE id = ?",
                    new String[]{String.valueOf(activeProfileId)}
            );
            if (c.moveToFirst()) {
                poolDistance = c.getInt(0);
            }
            c.close();
        } catch (Exception e) {
            Log.e("SwimStopwatchActivity", "Error loading pool distance", e);
        }

        String distanceText;
        if (poolDistance == 0) {
            distanceText = "Open Water";
        } else {
            distanceText = poolDistance + "m";
        }
        
        String htmlText = "Current tracking uses a <b><font color='#102A43'>" + distanceText + "</font></b> pool. Change the pool distance in Profile screen.";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tvPoolDistanceInfo.setText(android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            tvPoolDistanceInfo.setText(android.text.Html.fromHtml(htmlText));
        }
    }

    private void deleteEntry(long id, String style, String date) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this timing entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    int deleted = dbHelper.getWritableDatabase().delete("swim_sessions", "id = ?", new String[]{String.valueOf(id)});
                    if (deleted > 0) {
                       // Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        loadTodayEntries(style, date);
                    } else {
                        Toast.makeText(this, "Delete failed ❌", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPaceDialog(SwimTimingEntry entry, String style, boolean isBestTime) {
        long timeMs = entry.getTimeMs();
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        int poolDistance = 25; // default
        try {
            Cursor c = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT pool_distance FROM profile WHERE id = ?",
                    new String[]{String.valueOf(activeProfileId)}
            );
            if (c.moveToFirst()) {
                poolDistance = c.getInt(0);
                if (poolDistance <= 0) poolDistance = 25;
            }
            c.close();
        } catch (Exception e) {
            Log.e("SwimStopwatchActivity", "Error loading pool distance", e);
        }

        float totalSeconds = timeMs / 1000f;
        float pacePerMeter = totalSeconds / poolDistance;
        CharSequence title;
        CharSequence message;

        if ("im".equalsIgnoreCase(style)) {
            int totalM = poolDistance * 4;
            boolean isManualEntry = (entry.getFlyMs() == 0 && entry.getBackMs() == 0
                    && entry.getBreastMs() == 0 && entry.getFreeMs() == 0);
            String msg = "For "+totalM+"m, you have swam this IM style in " + formatPace(totalSeconds) + ".\n\n";
            if (isManualEntry) {
                msg += "Note: Manual entry will not store the individual swim stroke time.";
            } else {
                msg += "\t\t\t * Butterfly = " + formatMs(entry.getFlyMs()) + "\n";
                msg += "\t\t\t * Backstroke = " + formatMs(entry.getBackMs()) + "\n";
                msg += "\t\t\t * Breaststroke = " + formatMs(entry.getBreastMs()) + "\n";
                msg += "\t\t\t * Freestyle = " + formatMs(entry.getFreeMs());
            }
            title = "IM Pace Calculator";
            message = msg;
        } else {
            String msg = "For "+poolDistance+"m, you have swam this "+selectedStroke+" style in " + formatPace(totalSeconds) + ".\nWhat if?\n\n";
            msg += "\t\t\t * 25m = " + formatPace(pacePerMeter * 25) + "\n";
            msg += "\t\t\t * 50m = " + formatPace(pacePerMeter * 50) + "\n";
            msg += "\t\t\t * 100m = " + formatPace(pacePerMeter * 100);
            title = selectedStroke + " Pace Calculator";
            message = msg;
        }

        if (isBestTime) {
            android.text.SpannableString titleSpannable = new android.text.SpannableString(title);
            titleSpannable.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE), 0, title.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            title = titleSpannable;

            android.text.SpannableString messageSpannable = new android.text.SpannableString(message);
            messageSpannable.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.WHITE), 0, message.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            message = messageSpannable;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null);

        if (isBestTime) {
            builder.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.watercolor_gradient));
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

        if (isBestTime) {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.WHITE);
        }
    }

    private String formatPace(float secondsFloat) {
        int mins = (int) (secondsFloat / 60);
        float secs = secondsFloat % 60;
        if (mins > 0) {
            return String.format(Locale.getDefault(), "%dmin %.2f seconds", mins, secs);
        } else {
            return String.format(Locale.getDefault(), "%.2f seconds", secs);
        }
    }

    private void vibrate() {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(50);
            }
        }
    }
}