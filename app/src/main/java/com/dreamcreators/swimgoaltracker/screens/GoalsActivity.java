package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class GoalsActivity extends AppCompatActivity {

    private static final String PREFS = "swim_goals";
    private TextInputEditText etWeeklySessions, etGoalFree, etGoalFly, etGoalBreast, etGoalBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_goals);

        etWeeklySessions = findViewById(R.id.etWeeklySessions);
        etGoalFree = findViewById(R.id.etGoalFree);
        etGoalFly = findViewById(R.id.etGoalFly);
        etGoalBreast = findViewById(R.id.etGoalBreast);
        etGoalBack = findViewById(R.id.etGoalBack);
        MaterialButton btnSave = findViewById(R.id.btnSaveGoals);

        // Load saved goals
        int activeId = ProfileManager.getActiveProfileId(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        etWeeklySessions.setText(prefs.getString("weekly_sessions_" + activeId, ""));
        etGoalFree.setText(prefs.getString("goal_free_" + activeId, ""));
        etGoalFly.setText(prefs.getString("goal_fly_" + activeId, ""));
        etGoalBreast.setText(prefs.getString("goal_breast_" + activeId, ""));
        etGoalBack.setText(prefs.getString("goal_back_" + activeId, ""));

        btnSave.setOnClickListener(v -> {
            String weekly = etWeeklySessions.getText().toString().trim();
            String free = etGoalFree.getText().toString().trim();
            String fly = etGoalFly.getText().toString().trim();
            String breast = etGoalBreast.getText().toString().trim();
            String back = etGoalBack.getText().toString().trim();

            if (weekly.isEmpty() && free.isEmpty() && fly.isEmpty() && breast.isEmpty() && back.isEmpty()) {
                Toast.makeText(this, "Please fill in at least one goal", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString("weekly_sessions_" + activeId, weekly)
                    .putString("goal_free_" + activeId, free)
                    .putString("goal_fly_" + activeId, fly)
                    .putString("goal_breast_" + activeId, breast)
                    .putString("goal_back_" + activeId, back)
                    .apply();

            Toast.makeText(this, "Goals saved! 🎯", Toast.LENGTH_SHORT).show();
        });

        setupBottomNav();

        View rootView = findViewById(android.R.id.content);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);

            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            boolean isKeyboardVisible = keypadHeight > screenHeight * 0.15;

            if (isKeyboardVisible) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_goals);
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
                return true;
            } else if (id == R.id.nav_alerts) {
                startActivity(new Intent(this, AlertsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
