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
        float low = 0.5f, high = 1.0f;
        layImFly.setAlpha(imStrokeIndex == 0 ? high : low);
        layImBack.setAlpha(imStrokeIndex == 1 ? high : low);
        layImBreast.setAlpha(imStrokeIndex == 2 ? high : low);
        layImFree.setAlpha(imStrokeIndex == 3 ? high : low);
    }

    private void clearImHighlight() {
        float low = 0.5f;
        layImFly.setAlpha(low);
        layImBack.setAlpha(low);
        layImBreast.setAlpha(low);
        layImFree.setAlpha(low);
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
            // Validate: all 4 strokes must be completed
            if (imStrokeIndex < 4 && startIM >= 0) {
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
            values.put("created_at", createdAt);
            long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
            if (id > 0) {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                stopTimer(which);
                resetTimer(which);
                loadTodayEntries(which, date);
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
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
            if (which.equals("free") && showFree < 10000) {
                ThrowAlertDialog("free");
            } else if (which.equals("back") && showBack < 10000) {
                ThrowAlertDialog("back");
            } else if (which.equals("breast") && showBreast < 10000) {
                ThrowAlertDialog("breast");
            } else if (which.equals("fly") && showFly < 10000) {
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
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                    stopTimer(which);
                    resetTimer(which);
                    loadTodayEntries(which, date);
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
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            stopTimer(which);
            resetTimer(which);
            loadTodayEntries(which, date);
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
                "SELECT id, " + columnName + ", created_at, date FROM swim_sessions WHERE " + whereClause + " ORDER BY created_at DESC",
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
                entries.add(new SwimTimingEntry(id, timeMs, createdAt, entryDate));
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
            SwimTimingAdapter adapter = new SwimTimingAdapter(this, entries, bestTime == Long.MAX_VALUE ? 0 : bestTime, nowMs, entry -> {
                deleteEntry(entry.getId(), style, date);
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
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        loadTodayEntries(style, date);
                    } else {
                        Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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