package com.dreamcreators.swimgoaltracker.screens;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.DatePickerDialog;
import java.util.Calendar;

import android.util.Log;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class SwimEntryActivity extends AppCompatActivity {
    Spinner swimStyleSpinner;
    EditText edtSwimTime;
    TextView tvSwimDate;
    Button btn_SaveTiming;

    String selectedStyle = "";
    private NutritionDbHelper dbHelper;
    private boolean fromSwimStopwatch = false;
    private String returnStroke = null;
    private Calendar selectedCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_swim_entry);

        View main = findViewById(R.id.main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manual swim entry");
        }

        swimStyleSpinner = findViewById(R.id.swimStyleSpinner);
        edtSwimTime = findViewById(R.id.edtSwimTime);
        tvSwimDate = findViewById(R.id.tvSwimDate);
        btn_SaveTiming = findViewById(R.id.btn_SaveTiming);
        dbHelper = new NutritionDbHelper(this);

        // Set initial date
        updateDateDisplay();

        findViewById(R.id.lay_datePicker).setOnClickListener(v -> showDatePickerDialog());

        String[] swimStyles = {"Freestyle", "Backstroke", "Breaststroke", "Butterfly", "IM"};

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item_dark, swimStyles) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(getResources().getColor(R.color.lightPrimary));
                    ((TextView) v).setTypeface(null, android.graphics.Typeface.BOLD);
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item_dark);
        swimStyleSpinner.setAdapter(adapter);

        // Check if we came from SwimStopwatchActivity
        fromSwimStopwatch = getIntent().getBooleanExtra("from_swim_stopwatch", false);
        returnStroke = getIntent().getStringExtra("selected_stroke");

        if (returnStroke != null) {
            for (int i = 0; i < swimStyles.length; i++) {
                if (swimStyles[i].equalsIgnoreCase(returnStroke)) {
                    swimStyleSpinner.setSelection(i);
                    selectedStyle = swimStyles[i].toLowerCase();
                    break;
                }
            }
        }

        swimStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStyle = swimStyles[position].toLowerCase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        btn_SaveTiming.setOnClickListener(v -> saveSession(selectedStyle, edtSwimTime.getText().toString()));

        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e("SwimEntryActivity", "Ad failed: " + loadAdError.getCode() + " " + loadAdError.getMessage());
                }
            });
            MobileAds.initialize(this, initializationStatus -> {
                adView.loadAd(new AdRequest.Builder().build());
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void saveSession(String selectedStyle, String inputSeconds) {
        // Hide keyboard
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        if (selectedStyle == null || selectedStyle.isEmpty()) {
            throwAlertMessage("Please select a swim style.");
            return;
        }

        if (inputSeconds == null || inputSeconds.isEmpty()) {
            throwAlertMessage("Please enter swim time in seconds.");
            return;
        }

        double seconds;
        try {
            seconds = Double.parseDouble(inputSeconds.trim());
        } catch (NumberFormatException e) {
            throwAlertMessage("Invalid input. Please enter a number (e.g. 65.5).");
            return;
        }

        if (seconds <= 0) {
            throwAlertMessage("Time must be greater than 0 seconds.");
            return;
        }

        long milliseconds = (long) (seconds * 1000);

        if (milliseconds <= 2000) {
            throwAlertAndStore(selectedStyle);
        } else {
            // Save to database
            saveSessionWithSelectedValue(selectedStyle);
        }
    }

    private void throwAlertAndStore(String which) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Warning")
                .setMessage("The recorded time for " + which + " is less than 2 seconds. Are you sure you want to save it?")
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
        String date = dateFormat.format(selectedCalendar.getTime());
        if (which != null && !which.isEmpty()) {
            double seconds;
            try {
                seconds = Double.parseDouble(edtSwimTime.getText().toString().trim());
            } catch (NumberFormatException e) {
                throwAlertMessage("Invalid input. Please enter a valid number.");
                return;
            }
            long milliseconds = (long) (seconds * 1000);
            
            // Always insert a new entry for each attempt
            // This allows multiple attempts per day, and MIN() query will find the best time
            ContentValues values = new ContentValues();
            values.put("date", date);
            values.put("profile_id", ProfileManager.getActiveProfileId(this));
            values.put("created_at", System.currentTimeMillis());
            
            // Only set the selected style, others will default to 0
            // This allows tracking multiple attempts and finding the best time using MIN()
            if (which.equalsIgnoreCase("freestyle") || which.equalsIgnoreCase("Freestyle")) {
                values.put("freestyle_ms", milliseconds);
            } else if (which.equalsIgnoreCase("backstroke") || which.equalsIgnoreCase("Backstroke")) {
                values.put("backstroke_ms", milliseconds);
            } else if (which.equalsIgnoreCase("breaststroke") || which.equalsIgnoreCase("Breaststroke")) {
                values.put("breaststroke_ms", milliseconds);
            } else if (which.equalsIgnoreCase("butterfly") || which.equalsIgnoreCase("Butterfly")) {
                values.put("butterfly_ms", milliseconds);
            } else if (which.equalsIgnoreCase("im") || which.equalsIgnoreCase("IM")) {
                values.put("im_ms", milliseconds);
            }
            
            long result = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
            
            if (result > 0) {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                edtSwimTime.setText(""); // Clear input for next entry
            } else {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        } else {
            throwAlertMessage("Please select a swim style.");
            return;
        }

    }

    private void throwAlertMessage(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showDatePickerDialog() {
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.CustomDatePickerDialog, (view, year1, month1, dayOfMonth) -> {
            selectedCalendar.set(Calendar.YEAR, year1);
            selectedCalendar.set(Calendar.MONTH, month1);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateDateDisplay();
        }, year, month, day);
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        tvSwimDate.setText(dateFormat.format(selectedCalendar.getTime()));
    }
}