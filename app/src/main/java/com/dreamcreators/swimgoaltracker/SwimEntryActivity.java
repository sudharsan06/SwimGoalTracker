package com.dreamcreators.swimgoaltracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SwimEntryActivity extends AppCompatActivity {
    Spinner swimStyleSpinner;
    EditText edtSwimTime;
    Button btn_SaveTiming;

    String selectedStyle = "";
    private NutritionDbHelper dbHelper;
    private boolean fromSwimStopwatch = false;
    private String returnStroke = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swim_entry);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Manual swim entry");
        }

        swimStyleSpinner = findViewById(R.id.swimStyleSpinner);
        edtSwimTime = findViewById(R.id.edtSwimTime);
        btn_SaveTiming = findViewById(R.id.btn_SaveTiming);
        dbHelper = new NutritionDbHelper(this);

        String[] swimStyles = {"Freestyle", "Backstroke", "Breaststroke", "Butterfly"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, swimStyles
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        swimStyleSpinner.setAdapter(adapter);

        // Check if we came from SwimStopwatchActivity
        fromSwimStopwatch = getIntent().getBooleanExtra("from_swim_stopwatch", false);
        returnStroke = getIntent().getStringExtra("selected_stroke");

        swimStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = swimStyles[position];
                selectedStyle = selected.toLowerCase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //selectedStyleText.setText("No style selected");
            }
        });
        btn_SaveTiming.setOnClickListener(v -> saveSession(selectedStyle, edtSwimTime.getText().toString()));
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

        double seconds = (double) 0.0;
       /* try {
             seconds = Long.parseLong();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input: must be a number", Toast.LENGTH_SHORT).show();
        }*/

        seconds = Double.parseDouble(inputSeconds.trim());
        long milliseconds = (long) (seconds * 1000);

        if (milliseconds <= 10000) {
            throwAlertAndStore(selectedStyle);
        } else {
            // Save to database
            saveSessionWithSelectedValue(selectedStyle);
        }
    }

    private void throwAlertAndStore(String which) {
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
        if (which != null && !which.isEmpty()) {
            Double seconds = Double.parseDouble(edtSwimTime.getText().toString().trim());
            long milliseconds = (long) (seconds * 1000);
            
            // Always insert a new entry for each attempt
            // This allows multiple attempts per day, and MIN() query will find the best time
            ContentValues values = new ContentValues();
            values.put("date", date);
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
            }
            
            long result = dbHelper.getWritableDatabase().insert("swim_sessions", null, values);
            
            if (result > 0) {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();

                // Navigate back to SwimStopwatchActivity if we came from there
                if (fromSwimStopwatch) {
                    Intent intent = new Intent(SwimEntryActivity.this, SwimStopwatchActivity.class);
                    if (returnStroke != null) {
                        intent.putExtra("stroke", returnStroke);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                } else {
                    // Otherwise go to MainActivity
                    Intent intent = new Intent(SwimEntryActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
                finish();

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
}