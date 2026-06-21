package com.dreamcreators.swimgoaltracker.screens;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.AlertManager;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS = "swim_alerts";
    private static final String VIBE_PREFS = "app_settings";
    private static final String KEY_VIBRATION = "vibration_enabled";

    private SwitchMaterial switchTraining, switchRest, switchGoal, switchVibration;
    private TextView tvReminderTime;
    private View cardReminderTime, btnPickTime;
    private int reminderHour = 7, reminderMinute = 0;
    private int activeProfileId;

    private RadioGroup radioGroupTheme;
    private RadioButton radioLight, radioDark, radioAuto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));

        setContentView(R.layout.activity_settings);

        activeProfileId = ProfileManager.getActiveProfileId(this);

        initViews();
        loadThemePreference();
        loadAlertPreferences();
        loadVibrationPreference();
        loadAppVersion();
        setupBottomNav();
    }

    private void initViews() {
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        radioLight = findViewById(R.id.radioThemeLight);
        radioDark = findViewById(R.id.radioThemeDark);
        radioAuto = findViewById(R.id.radioThemeAuto);

        switchTraining = findViewById(R.id.switchTrainingReminder);
        switchRest = findViewById(R.id.switchRestAlert);
        switchGoal = findViewById(R.id.switchGoalAlert);
        switchVibration = findViewById(R.id.switchVibration);
        tvReminderTime = findViewById(R.id.tvReminderTime);
        cardReminderTime = findViewById(R.id.cardReminderTime);
        btnPickTime = findViewById(R.id.btnPickReminderTime);

        btnPickTime.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                reminderHour = hourOfDay;
                reminderMinute = minute;
                updateReminderTimeLabel();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putInt("reminder_hour_" + activeProfileId, hourOfDay)
                        .putInt("reminder_minute_" + activeProfileId, minute)
                        .apply();
                if (switchTraining.isChecked()) {
                    AlertManager.scheduleTrainingReminder(this, hourOfDay, minute);
                }
            }, reminderHour, reminderMinute, false).show();
        });

        findViewById(R.id.btnRateApp).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + getPackageName())));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
            }
        });

        findViewById(R.id.btnPrivacyPolicy).setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://dreamcreators.net/privacy-policy")));
        });

        findViewById(R.id.btnResetProgress).setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Reset All Progress")
                    .setMessage("This will permanently delete all swim sessions, nutrition data, and goals. Your profile will be kept. This action cannot be undone.")
                    .setPositiveButton("Reset", (dialog, which) -> resetAllProgress())
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void loadThemePreference() {
        radioGroupTheme.setOnCheckedChangeListener(null);
        String mode = ThemeManager.getThemeMode(this);
        if (ThemeManager.MODE_LIGHT.equals(mode)) {
            radioLight.setChecked(true);
        } else if (ThemeManager.MODE_DARK.equals(mode)) {
            radioDark.setChecked(true);
        } else {
            radioAuto.setChecked(true);
        }
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String newMode;
            if (checkedId == R.id.radioThemeLight) {
                newMode = ThemeManager.MODE_LIGHT;
            } else if (checkedId == R.id.radioThemeDark) {
                newMode = ThemeManager.MODE_DARK;
            } else {
                newMode = ThemeManager.MODE_AUTO;
            }
            ThemeManager.setThemeMode(SettingsActivity.this, newMode);
            Intent intent = new Intent(SettingsActivity.this, SettingsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
            overridePendingTransition(0, 0);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlertPreferences();
        loadVibrationPreference();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }

    private void loadAlertPreferences() {
        activeProfileId = ProfileManager.getActiveProfileId(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        switchTraining.setOnCheckedChangeListener(null);
        switchRest.setOnCheckedChangeListener(null);
        switchGoal.setOnCheckedChangeListener(null);

        boolean trainingOn = prefs.getBoolean("training_reminder_" + activeProfileId, false);
        switchTraining.setChecked(trainingOn);
        switchRest.setChecked(prefs.getBoolean("rest_alert_" + activeProfileId, false));
        switchGoal.setChecked(prefs.getBoolean("goal_alert_" + activeProfileId, false));
        reminderHour = prefs.getInt("reminder_hour_" + activeProfileId, 7);
        reminderMinute = prefs.getInt("reminder_minute_" + activeProfileId, 0);
        updateReminderTimeLabel();
        cardReminderTime.setVisibility(trainingOn ? View.VISIBLE : View.GONE);

        registerAlertListeners();
    }

    private void registerAlertListeners() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        switchTraining.setOnCheckedChangeListener((btn, isChecked) -> {
            cardReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("training_reminder_" + activeProfileId, isChecked).apply();
            if (isChecked) {
                checkAndRequestNotificationPermission();
                AlertManager.scheduleTrainingReminder(this, reminderHour, reminderMinute);
            } else {
                AlertManager.cancelTrainingReminder(this);
            }
            Toast.makeText(this, isChecked ? "Training reminder enabled" : "Training reminder disabled", Toast.LENGTH_SHORT).show();
        });

        switchRest.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("rest_alert_" + activeProfileId, isChecked).apply();
            if (isChecked) {
                checkAndRequestNotificationPermission();
                AlertManager.scheduleRestAlert(this);
            } else {
                AlertManager.cancelRestAlert(this);
            }
            Toast.makeText(this, isChecked ? "Rest day alert enabled" : "Rest day alert disabled", Toast.LENGTH_SHORT).show();
        });

        switchGoal.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("goal_alert_" + activeProfileId, isChecked).apply();
            if (isChecked) {
                checkAndRequestNotificationPermission();
            }
            Toast.makeText(this, isChecked ? "Goal alert enabled" : "Goal alert disabled", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadVibrationPreference() {
        SharedPreferences prefs = getSharedPreferences(VIBE_PREFS, MODE_PRIVATE);
        boolean enabled = prefs.getBoolean(KEY_VIBRATION, true);
        switchVibration.setChecked(enabled);
        switchVibration.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_VIBRATION, isChecked).apply();
        });
    }

    private void loadAppVersion() {
        TextView tvVersion = findViewById(R.id.tvAppVersion);
        try {
            PackageInfo pInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.PackageInfoFlags.of(0));
            } else {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            }
            tvVersion.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvVersion.setText("1.0");
        }
    }

    private void updateReminderTimeLabel() {
        String amPm = reminderHour < 12 ? "AM" : "PM";
        int displayHour = reminderHour % 12;
        if (displayHour == 0) displayHour = 12;
        tvReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, reminderMinute, amPm));
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1003
                );
            }
        }
    }

    private void resetAllProgress() {
        NutritionDbHelper dbHelper = new NutritionDbHelper(this);
        try {
            dbHelper.getWritableDatabase().execSQL("DELETE FROM swim_sessions");
            dbHelper.getWritableDatabase().execSQL("DELETE FROM daily_nutrition");
            // Clear goal preferences
            getSharedPreferences("swim_goals", MODE_PRIVATE).edit().clear().apply();
            Toast.makeText(this, "All progress has been reset", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error resetting data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
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
            } else if (id == R.id.nav_goals) {
                startActivity(new Intent(this, GoalsActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
