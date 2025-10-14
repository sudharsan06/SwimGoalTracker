package com.dreamcreators.swimgoaltracker;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SwimStopwatchActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    private long startFree = -1, elapsedFree = 0;
    private long startBack = -1, elapsedBack = 0;
    private long startBreast = -1, elapsedBreast = 0;
    private long startFly = -1, elapsedFly = 0;

    private TextView tvDate;
    private TextView tvFree, tvBack, tvBreast, tvFly;
    private NutritionDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        dbHelper = new NutritionDbHelper(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("Date: " + today);

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
        handler.post(tick);
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
                handler.postDelayed(this, 100);
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
        long tenths = (ms % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }

    private void saveSession(String which) {

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
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
                // proceed to save
                ContentValues values = new ContentValues();
                values.put("date", date);
                if (which.equals("free")) values.put("freestyle_ms", showFree);
                else values.put("freestyle_ms", 0);
                if (which.equals("back")) values.put("backstroke_ms", showBack);
                else values.put("backstroke_ms", 0);
                if (which.equals("breast")) values.put("breaststroke_ms", showBreast);
                else values.put("breaststroke_ms", 0);
                if (which.equals("fly")) values.put("butterfly_ms", showFly);
                else values.put("butterfly_ms", 0);

                /* values.put("freestyle_ms", showFree);
                 values.put("backstroke_ms", showBack);
                 values.put("breaststroke_ms", showBreast);
                 values.put("butterfly_ms", showFly);*/
                long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
                if (id > 0) {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void ThrowAlertDialog(String which) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setTitle("Warning");
        builder.setMessage("The recorded time for " + which + " is less than 10 seconds. Are you sure you want to save it?");
        builder.setPositiveButton("Yes", (dialog, whichButton) -> {
            // User confirmed, proceed to save
            saveSessionWithSelectedValue(which);
        });
        builder.setNegativeButton("No", (dialog, whichButton) -> {
            // User cancelled, do nothing
            dialog.dismiss();
        });
        builder.show();
    }

    private void saveSessionWithSelectedValue(String which) {

        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        long now = System.currentTimeMillis();
        long showFree = elapsedFree + (startFree >= 0 ? now - startFree : 0);
        long showBack = elapsedBack + (startBack >= 0 ? now - startBack : 0);
        long showBreast = elapsedBreast + (startBreast >= 0 ? now - startBreast : 0);
        long showFly = elapsedFly + (startFly >= 0 ? now - startFly : 0);

        // proceed to save
        ContentValues values = new ContentValues();
        values.put("date", date);
        if (which.equals("free")) values.put("freestyle_ms", showFree);
        else values.put("freestyle_ms", 0);
        if (which.equals("back")) values.put("backstroke_ms", showBack);
        else values.put("backstroke_ms", 0);
        if (which.equals("breast")) values.put("breaststroke_ms", showBreast);
        else values.put("breaststroke_ms", 0);
        if (which.equals("fly")) values.put("butterfly_ms", showFly);
        else values.put("butterfly_ms", 0);

        long id = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
        if (id > 0) {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
        }
    }
}




