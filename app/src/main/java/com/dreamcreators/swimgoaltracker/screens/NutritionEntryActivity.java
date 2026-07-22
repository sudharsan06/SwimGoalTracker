package com.dreamcreators.swimgoaltracker.screens;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.content.Intent;

public class NutritionEntryActivity extends AppCompatActivity {

    private NutritionDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.dark_surface_low));
        setContentView(R.layout.activity_nutrition_entry);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Daily Nutrition entry");
        // Setup toolbar
     /*   Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }*/

        TextView tvDate = findViewById(R.id.tvEntryDate);
        EditText etCalories = findViewById(R.id.etCalories);
        EditText etProtein = findViewById(R.id.etProtein);
        EditText etCarbs = findViewById(R.id.etCarbs);
        EditText etFats = findViewById(R.id.etFats);
        Button btnSave = findViewById(R.id.btnSaveNutrition);

        dbHelper = new NutritionDbHelper(this);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("Date: " + today);

        prefillIfExists(today, etCalories, etProtein, etCarbs, etFats);

        btnSave.setOnClickListener(v -> {
            // Hide keyboard
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
            int calories = parseIntSafe(etCalories.getText().toString());
            int protein = parseIntSafe(etProtein.getText().toString());
            int carbs = parseIntSafe(etCarbs.getText().toString());
            int fats = parseIntSafe(etFats.getText().toString());

            ContentValues values = new ContentValues();
            values.put("date", today);
            values.put("calories", calories);
            values.put("protein", protein);
            values.put("carbs", carbs);
            values.put("fats", fats);

            long updated = dbHelper.getWritableDatabase().update("daily_nutrition", values, "date = ?", new String[]{today});
            if (updated == 0) {
                long inserted = dbHelper.getWritableDatabase().insert("daily_nutrition", null, values);
                if (inserted <= 0) {
                    Toast.makeText(this, "Save failed ❌", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "Saved ✅", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void prefillIfExists(String date, EditText etCalories, EditText etProtein, EditText etCarbs, EditText etFats) {
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT calories, protein, carbs, fats FROM daily_nutrition WHERE date = ?",
                new String[]{date}
        );
        if (c.moveToFirst()) {
            etCalories.setText(String.valueOf(c.getInt(0)));
            etProtein.setText(String.valueOf(c.getInt(1)));
            etCarbs.setText(String.valueOf(c.getInt(2)));
            etFats.setText(String.valueOf(c.getInt(3)));
        }
        c.close();
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}


