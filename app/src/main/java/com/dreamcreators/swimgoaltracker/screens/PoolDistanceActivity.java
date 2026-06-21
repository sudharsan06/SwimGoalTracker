package com.dreamcreators.swimgoaltracker.screens;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PoolDistanceActivity extends AppCompatActivity {

    private int currentDistance = 25; // default
    private int activeProfileId;
    private NutritionDbHelper dbHelper;

    private MaterialCardView card25m, card50m, cardOpen, cardCustom;
    private RadioButton rb25m, rb50m, rbOpen, rbCustom;
    private TextView tvLapsToday, tvCustomMeters;

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
        setContentView(R.layout.activity_pool_distance);

        dbHelper = new NutritionDbHelper(this);
        activeProfileId = getIntent().getIntExtra("profile_id", ProfileManager.getActiveProfileId(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> onBackPressed());

        card25m = findViewById(R.id.cardOption25m);
        card50m = findViewById(R.id.cardOption50m);
        cardOpen = findViewById(R.id.cardOptionOpenWater);
        cardCustom = findViewById(R.id.cardOptionCustom);

        rb25m = findViewById(R.id.ivCheck25m);
        rb50m = findViewById(R.id.ivCheck50m);
        rbOpen = findViewById(R.id.ivCheckOpenWater);
        rbCustom = findViewById(R.id.ivCheckCustom);

        tvLapsToday = findViewById(R.id.tvLapsToday);
        tvCustomMeters = findViewById(R.id.tvCustomMeters);

        card25m.setOnClickListener(v -> selectDistance(25));
        card50m.setOnClickListener(v -> selectDistance(50));
        cardOpen.setOnClickListener(v -> selectDistance(0)); // 0 for Open Water
        cardCustom.setOnClickListener(v -> promptCustomDistance());

        findViewById(R.id.btnConfirmSave).setOnClickListener(v -> saveAndExit());

        loadCurrentDistance();
    }

    private void loadCurrentDistance() {
        currentDistance = getIntent().getIntExtra("current_distance", 25);
        selectDistance(currentDistance);
    }

    private void promptCustomDistance() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("e.g. 15");

        new AlertDialog.Builder(this)
                .setTitle("Custom Pool Distance")
                .setMessage("Enter distance in metres:")
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    try {
                        int dist = Integer.parseInt(input.getText().toString().trim());
                        if (dist > 0) {
                            selectDistance(dist);
                        } else {
                            Toast.makeText(this, "Invalid distance ❌", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid distance ❌", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void selectDistance(int distance) {
        currentDistance = distance;

        // Reset all
        card25m.setCardBackgroundColor(0xFFFFFFFF);
        card50m.setCardBackgroundColor(0xFFFFFFFF);
        cardOpen.setCardBackgroundColor(0xFFFFFFFF);
        cardCustom.setCardBackgroundColor(0xFFFFFFFF);

        rb25m.setChecked(false);
        rb50m.setChecked(false);
        rbOpen.setChecked(false);
        rbCustom.setChecked(false);

        if (distance == 25) {
            card25m.setCardBackgroundColor(0xFFEAF3FD);
            rb25m.setChecked(true);
        } else if (distance == 50) {
            card50m.setCardBackgroundColor(0xFFEAF3FD);
            rb50m.setChecked(true);
        } else if (distance == 0) {
            cardOpen.setCardBackgroundColor(0xFFEAF3FD);
            rbOpen.setChecked(true);
        } else {
            cardCustom.setCardBackgroundColor(0xFFEAF3FD);
            rbCustom.setChecked(true);
            tvCustomMeters.setText(distance + "m");
        }

        updateLapsDisplay();
    }

    private void updateLapsDisplay() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM swim_sessions WHERE profile_id = ? AND date = ?",
                new String[]{String.valueOf(activeProfileId), today}
        );
        int laps = 0;
        if (c.moveToFirst()) {
            laps = c.getInt(0);
        }
        c.close();

        if (currentDistance == 0) {
            tvLapsToday.setText(laps + " sessions (Open Water)");
        } else {
            int totalMeters = laps * currentDistance;
            tvLapsToday.setText(laps + " laps = " + String.format(Locale.getDefault(), "%,d", totalMeters) + "m");
        }
    }

    private void saveAndExit() {
        android.content.Intent resultIntent = new android.content.Intent();
        resultIntent.putExtra("pool_distance", currentDistance);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
