package com.dreamcreators.swimgoaltracker.screens;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    private TextInputEditText etWeeklySessions, etGoalFree, etGoalFly, etGoalBreast, etGoalBack, etGoalIM;

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
        etGoalIM = findViewById(R.id.etGoalIM);
        MaterialButton btnSave = findViewById(R.id.btnSaveGoals);

        // Load saved goals
        int activeId = ProfileManager.getActiveProfileId(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        etWeeklySessions.setText(prefs.getString("weekly_sessions_" + activeId, ""));
        etGoalFree.setText(prefs.getString("goal_free_" + activeId, ""));
        etGoalFly.setText(prefs.getString("goal_fly_" + activeId, ""));
        etGoalBreast.setText(prefs.getString("goal_breast_" + activeId, ""));
        etGoalBack.setText(prefs.getString("goal_back_" + activeId, ""));
        etGoalIM.setText(prefs.getString("goal_im_" + activeId, ""));

        etGoalIM.setInputType(android.text.InputType.TYPE_NULL);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            etGoalIM.setShowSoftInputOnFocus(false);
        }

        etGoalIM.setOnClickListener(v -> showTimePickerDialog(etGoalIM, "IM Goal"));
        etGoalIM.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(etGoalIM.getWindowToken(), 0);
                showTimePickerDialog(etGoalIM, "IM Goal");
            }
        });

        btnSave.setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
            String weekly = etWeeklySessions.getText().toString().trim();
            String free = etGoalFree.getText().toString().trim();
            String fly = etGoalFly.getText().toString().trim();
            String breast = etGoalBreast.getText().toString().trim();
            String back = etGoalBack.getText().toString().trim();
            String im = etGoalIM.getText().toString().trim();

            if (weekly.isEmpty() && free.isEmpty() && fly.isEmpty() && breast.isEmpty() && back.isEmpty() && im.isEmpty()) {
                Toast.makeText(this, "Please fill in at least one goal", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString("weekly_sessions_" + activeId, weekly)
                    .putString("goal_free_" + activeId, free)
                    .putString("goal_fly_" + activeId, fly)
                    .putString("goal_breast_" + activeId, breast)
                    .putString("goal_back_" + activeId, back)
                    .putString("goal_im_" + activeId, im)
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
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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

    private void showTimePickerDialog(TextInputEditText targetEditText, String title) {
        int currentMin = 0, currentSec = 0, currentHundredths = 0;
        String existing = targetEditText.getText().toString().trim();
        if (!existing.isEmpty() && existing.contains(":")) {
            try {
                String[] parts = existing.split(":");
                if (parts.length == 3) {
                    currentMin = Integer.parseInt(parts[0].trim());
                    currentSec = Integer.parseInt(parts[1].trim());
                    currentHundredths = Integer.parseInt(parts[2].trim());
                }
            } catch (Exception ignored) {}
        }

        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(dp16, dp16, dp16, dp16);

        android.widget.TextView instruction = new android.widget.TextView(this);
        instruction.setText("Pick target time for this stroke");
        instruction.setTextSize(13);
        instruction.setTextColor(getColor(R.color.steel_blue));
        instruction.setGravity(android.view.Gravity.CENTER);
        instruction.setPadding(0, 0, 0, dp16);
        container.addView(instruction);

        android.widget.LinearLayout labelRow = new android.widget.LinearLayout(this);
        labelRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        labelRow.setGravity(android.view.Gravity.CENTER);
        int dp8 = (int) (6 * getResources().getDisplayMetrics().density);
        labelRow.setPadding(0, dp8, 0, 8);

        android.widget.TextView lblMin = new android.widget.TextView(this);
        lblMin.setText("MM");
        lblMin.setTextSize(14);
        lblMin.setBackground(androidx.core.content.ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_bg_best));
        lblMin.setTextColor(getColor(R.color.black));
        lblMin.setTypeface(null, android.graphics.Typeface.BOLD);
        lblMin.setGravity(android.view.Gravity.CENTER);
        lblMin.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.TextView lblSec = new android.widget.TextView(this);
        lblSec.setText("SS");
        lblSec.setTextSize(14);
        lblSec.setBackground(androidx.core.content.ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_bg_best));
        lblSec.setTextColor(getColor(R.color.black));
        lblSec.setTypeface(null, android.graphics.Typeface.BOLD);
        lblSec.setGravity(android.view.Gravity.CENTER);
        lblSec.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        android.widget.TextView lblMs = new android.widget.TextView(this);
        lblMs.setText("ss");
        lblMs.setBackground(androidx.core.content.ContextCompat.getDrawable(getApplicationContext(), R.drawable.item_bg_best));
        lblMs.setTextSize(14);
        lblMs.setTextColor(getColor(R.color.black));
        lblMs.setTypeface(null, android.graphics.Typeface.BOLD);
        lblMs.setGravity(android.view.Gravity.CENTER);
        lblMs.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        labelRow.addView(lblMin);
        labelRow.addView(lblSec);
        labelRow.addView(lblMs);
        container.addView(labelRow);

        android.widget.LinearLayout pickerRow = new android.widget.LinearLayout(this);
        pickerRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        pickerRow.setGravity(android.view.Gravity.CENTER);

        android.widget.NumberPicker npMin = new android.widget.NumberPicker(this);
        npMin.setMinValue(0);
        npMin.setMaxValue(59);
        npMin.setValue(currentMin);
        npMin.setWrapSelectorWheel(true);
        npMin.setDescendantFocusability(android.widget.NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npMin.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));

        android.widget.TextView colon1 = new android.widget.TextView(this);
        colon1.setText(" : ");
        colon1.setTextSize(22);
        colon1.setTextColor(getColor(R.color.lightPrimary));
        colon1.setTypeface(null, android.graphics.Typeface.BOLD);
        colon1.setGravity(android.view.Gravity.CENTER);

        android.widget.NumberPicker npSec = new android.widget.NumberPicker(this);
        npSec.setMinValue(0);
        npSec.setMaxValue(59);
        npSec.setValue(currentSec);
        npSec.setWrapSelectorWheel(true);
        npSec.setDescendantFocusability(android.widget.NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npSec.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));

        android.widget.TextView colon2 = new android.widget.TextView(this);
        colon2.setText(" : ");
        colon2.setTextSize(22);
        colon2.setTextColor(getColor(R.color.lightPrimary));
        colon2.setTypeface(null, android.graphics.Typeface.BOLD);
        colon2.setGravity(android.view.Gravity.CENTER);

        android.widget.NumberPicker npHundredths = new android.widget.NumberPicker(this);
        npHundredths.setMinValue(0);
        npHundredths.setMaxValue(99);
        npHundredths.setValue(currentHundredths);
        npHundredths.setWrapSelectorWheel(true);
        npHundredths.setDescendantFocusability(android.widget.NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npHundredths.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));

        pickerRow.addView(npMin);
        pickerRow.addView(colon1);
        pickerRow.addView(npSec);
        pickerRow.addView(colon2);
        pickerRow.addView(npHundredths);
        container.addView(pickerRow);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String formatted = String.format(java.util.Locale.getDefault(),
                            "%02d : %02d : %02d",
                            npMin.getValue(), npSec.getValue(), npHundredths.getValue());
                    targetEditText.setText(formatted);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
