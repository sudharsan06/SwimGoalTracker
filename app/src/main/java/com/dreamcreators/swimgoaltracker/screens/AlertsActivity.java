package com.dreamcreators.swimgoaltracker.screens;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Locale;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.dreamcreators.swimgoaltracker.utility.AlertManager;

public class AlertsActivity extends AppCompatActivity {

    private static final String PREFS = "swim_alerts";
    private SwitchMaterial switchTraining, switchRest, switchGoal;
    private TextView tvReminderTime;
    private View cardReminderTime;
    private int reminderHour = 7, reminderMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        setContentView(R.layout.activity_alerts);

        switchTraining = findViewById(R.id.switchTrainingReminder);
        switchRest = findViewById(R.id.switchRestAlert);
        switchGoal = findViewById(R.id.switchGoalAlert);
        tvReminderTime = findViewById(R.id.tvReminderTime);
        cardReminderTime = findViewById(R.id.cardReminderTime);

        // Load saved preferences
        int activeId = ProfileManager.getActiveProfileId(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean trainingOn = prefs.getBoolean("training_reminder_" + activeId, false);
        switchTraining.setChecked(trainingOn);
        switchRest.setChecked(prefs.getBoolean("rest_alert_" + activeId, false));
        switchGoal.setChecked(prefs.getBoolean("goal_alert_" + activeId, false));
        reminderHour = prefs.getInt("reminder_hour_" + activeId, 7);
        reminderMinute = prefs.getInt("reminder_minute_" + activeId, 0);
        updateReminderTimeLabel();
        cardReminderTime.setVisibility(trainingOn ? View.VISIBLE : View.GONE);

        switchTraining.setOnCheckedChangeListener((btn, isChecked) -> {
            cardReminderTime.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            prefs.edit().putBoolean("training_reminder_" + activeId, isChecked).apply();
            if (isChecked) {
                checkAndRequestNotificationPermission();
                AlertManager.scheduleTrainingReminder(this, reminderHour, reminderMinute);
            } else {
                AlertManager.cancelTrainingReminder(this);
            }
            Toast.makeText(this, isChecked ? "Training reminder enabled" : "Training reminder disabled", Toast.LENGTH_SHORT).show();
        });

        switchRest.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("rest_alert_" + activeId, isChecked).apply();
            if (isChecked) {
                checkAndRequestNotificationPermission();
                AlertManager.scheduleRestAlert(this);
            } else {
                AlertManager.cancelRestAlert(this);
            }
            Toast.makeText(this, isChecked ? "Rest day alert enabled" : "Rest day alert disabled", Toast.LENGTH_SHORT).show();
        });

        switchGoal.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("goal_alert_" + activeId, isChecked).apply();
            if (isChecked) {
                checkAndRequestNotificationPermission();
            }
            Toast.makeText(this, isChecked ? "Goal alert enabled" : "Goal alert disabled", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnPickReminderTime).setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                reminderHour = hourOfDay;
                reminderMinute = minute;
                updateReminderTimeLabel();
                prefs.edit()
                        .putInt("reminder_hour_" + activeId, hourOfDay)
                        .putInt("reminder_minute_" + activeId, minute)
                        .apply();
                if (switchTraining.isChecked()) {
                    AlertManager.scheduleTrainingReminder(this, hourOfDay, minute);
                }
            }, reminderHour, reminderMinute, false).show();
        });

        setupBottomNav();
    }

    private void updateReminderTimeLabel() {
        String amPm = reminderHour < 12 ? "AM" : "PM";
        int displayHour = reminderHour % 12;
        if (displayHour == 0) displayHour = 12;
        tvReminderTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, reminderMinute, amPm));
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
        bottomNav.setSelectedItemId(R.id.nav_alerts);
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
            } else if (id == R.id.nav_alerts) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
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
}
