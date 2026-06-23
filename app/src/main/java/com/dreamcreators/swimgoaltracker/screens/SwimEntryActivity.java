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
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import java.util.Calendar;

import android.util.Log;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

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

    private TextInputLayout edtSwimTimeLay;
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
        setContentView(R.layout.activity_swim_entry);

        swimStyleSpinner = findViewById(R.id.swimStyleSpinner);
        edtSwimTime = findViewById(R.id.edtSwimTime);
        tvSwimDate = findViewById(R.id.tvSwimDate);
        btn_SaveTiming = findViewById(R.id.btn_SaveTiming);
        edtSwimTimeLay = findViewById(R.id.edtSwimTimeLay);
        dbHelper = new NutritionDbHelper(this);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Set initial date
        updateDateDisplay();

        findViewById(R.id.lay_datePicker).setOnClickListener(v -> showDatePickerDialog());

        String[] swimStyles = {"Freestyle", "Backstroke", "Breaststroke", "Butterfly", "IM"};

        int textColor = getColor(R.color.dark_on_surface);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item_dark, swimStyles) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(getColor(R.color.dark_primary_fixed_dim));
                    ((TextView) v).setTypeface(null, android.graphics.Typeface.BOLD);
                }
                return v;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setTextColor(getColor(R.color.dark_on_surface));
                    ((TextView) v).setTypeface(null, android.graphics.Typeface.NORMAL);
                }
                return v;
            }
        };
        adapter.setDropDownViewResource(R.layout.spinner_item_dark);
        swimStyleSpinner.setAdapter(adapter);

        // Check if we came from SwimStopwatchActivity
        fromSwimStopwatch = getIntent().getBooleanExtra("from_swim_stopwatch", false);
        returnStroke = getIntent().getStringExtra("selected_stroke");

        swimStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStyle = swimStyles[position].toLowerCase();
            edtSwimTime.setInputType(android.text.InputType.TYPE_NULL);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                edtSwimTime.setShowSoftInputOnFocus(false);
            }
            if (!edtSwimTime.getText().toString().contains(":")) {
                edtSwimTime.setText("");
            }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        if (returnStroke != null) {
            for (int i = 0; i < swimStyles.length; i++) {
                if (swimStyles[i].equalsIgnoreCase(returnStroke)) {
                    swimStyleSpinner.setSelection(i);
                    selectedStyle = swimStyles[i].toLowerCase();
                    break;
                }
            }
        } else {
            selectedStyle = swimStyles[0].toLowerCase();
        }

        edtSwimTime.setOnClickListener(v -> {
            showTimePickerDialog();
        });

        edtSwimTime.setFocusable(false);
        edtSwimTime.setClickable(true);

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

    private void saveSession(String selectedStyle, String inputTime) {
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

        if (inputTime == null || inputTime.isEmpty()) {
            if (selectedStyle.equalsIgnoreCase("im")) {
                throwAlertMessage("Please pick IM time using the time picker.");
            } else {
                throwAlertMessage("Please enter swim time in seconds.");
            }
            return;
        }

        long milliseconds;

        if (inputTime.contains(":")) {
            // Parse MM:SS:ss format for any style
            milliseconds = parseImTimeToMs(inputTime);
            if (milliseconds < 0) {
                throwAlertMessage("Invalid time format. Please use the time picker.");
                return;
            }
        } else {
            double seconds;
            try {
                seconds = Double.parseDouble(inputTime.trim());
            } catch (NumberFormatException e) {
                throwAlertMessage("Invalid input. Please enter a valid number.");
                return;
            }
            if (seconds <= 0) {
                throwAlertMessage("Time must be greater than 0 seconds.");
                return;
            }
            milliseconds = (long) (seconds * 1000);
        }

        if (milliseconds <= 0) {
            throwAlertMessage("Time must be greater than 0.");
            return;
        }

        if (milliseconds <= 2000) {
            throwAlertAndStore(selectedStyle);
        } else {
            // Save to database
            saveSessionWithSelectedValue(selectedStyle);
        }
    }

    /**
     * Parses IM time from "MM : SS : ss" format to milliseconds.
     * Returns -1 if parsing fails.
     */
    private long parseImTimeToMs(String imTime) {
        try {
            String[] parts = imTime.split(":");
            if (parts.length == 3) {
                int mins = Integer.parseInt(parts[0].trim());
                int secs = Integer.parseInt(parts[1].trim());
                int hundredths = Integer.parseInt(parts[2].trim());
                return (mins * 60L + secs) * 1000L + hundredths * 10L;
            }
        } catch (Exception e) {
            // fall through
        }
        return -1;
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
                    edtSwimTimeLay.setHint("Stroke (MM:SS)");
                    dialog.dismiss();
                })
                .show();
    }

    private void saveSessionWithSelectedValue(String which) {
        String date = dateFormat.format(selectedCalendar.getTime());
        if (which != null && !which.isEmpty()) {
            long milliseconds;
            String timeText = edtSwimTime.getText().toString().trim();
            if (timeText.contains(":")) {
                // Parse MM:SS:ss format (from time picker) for any style
                milliseconds = parseImTimeToMs(timeText);
                if (milliseconds < 0) {
                    throwAlertMessage("Invalid time format. Please use the time picker.");
                    return;
                }
            } else {
                double seconds;
                try {
                    seconds = Double.parseDouble(timeText);
                } catch (NumberFormatException e) {
                    throwAlertMessage("Invalid input. Please enter a valid number.");
                    return;
                }
                milliseconds = (long) (seconds * 1000);
            }
            
            // Always insert a new entry for each attempt
            // This allows multiple attempts per day, and MIN() query will find the best time
            ContentValues values = new ContentValues();
            values.put("date", date);
            values.put("profile_id", ProfileManager.getActiveProfileId(this));
            values.put("created_at", System.currentTimeMillis());
            values.put("total_distance", getPoolDistance());
            
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
                Toast.makeText(this, "Saved ✅", Toast.LENGTH_SHORT).show();
                edtSwimTime.setText(""); // Clear input for next entry
                if (fromSwimStopwatch) {
                    finish();
                } else {
                    int activeProfileId = ProfileManager.getActiveProfileId(this);
                    getSharedPreferences("swim_goals_check", MODE_PRIVATE)
                            .edit()
                            .putLong("last_checked_session_id_" + activeProfileId, result)
                            .apply();
                    checkGoalAchievement(which, milliseconds);
                }
            } else {
                Toast.makeText(this, "Save failed ❌", Toast.LENGTH_SHORT).show();
            }
        } else {
            throwAlertMessage("Please select a swim style.");
            return;
        }

    }

    private int getPoolDistance() {
        int activeProfileId = ProfileManager.getActiveProfileId(this);
        int poolDistance = 25;
        try {
            android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT pool_distance FROM profile WHERE id = ?",
                    new String[]{String.valueOf(activeProfileId)}
            );
            if (c.moveToFirst()) {
                poolDistance = c.getInt(0);
            }
            c.close();
        } catch (Exception e) {
            Log.e("SwimEntryActivity", "Error loading pool distance", e);
        }
        return poolDistance;
    }

    private void throwAlertMessage(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void checkGoalAchievement(String styleFullName, long timeMs) {
        String styleKey = "";
        if (styleFullName.equalsIgnoreCase("freestyle")) styleKey = "free";
        else if (styleFullName.equalsIgnoreCase("backstroke")) styleKey = "back";
        else if (styleFullName.equalsIgnoreCase("breaststroke")) styleKey = "breast";
        else if (styleFullName.equalsIgnoreCase("butterfly")) styleKey = "fly";
        else if (styleFullName.equalsIgnoreCase("im")) styleKey = "im";
        else return;

        int activeId = ProfileManager.getActiveProfileId(this);
        android.content.SharedPreferences alertPrefs = getSharedPreferences("swim_alerts", MODE_PRIVATE);
        boolean goalAlertEnabled = alertPrefs.getBoolean("goal_alert_" + activeId, false);
        
        if (!goalAlertEnabled) return;
        
        android.content.SharedPreferences goalPrefs = getSharedPreferences("swim_goals", MODE_PRIVATE);
        String goalKey = "goal_" + styleKey + "_" + activeId; 
        String goalStr = goalPrefs.getString(goalKey, "");
        long goalMs = parseGoalToMs(goalStr);
        
        if (goalMs > 0 && timeMs <= goalMs) {
            showGoalAchievedAlert(styleKey, timeMs, goalMs);
        }
    }

    private long parseGoalToMs(String goalStr) {
        if (goalStr == null || goalStr.trim().isEmpty()) return -1;
        try {
            String[] parts = goalStr.split(":");
            if (parts.length == 3) {
                long mins = Long.parseLong(parts[0].trim());
                long secs = Long.parseLong(parts[1].trim());
                long hundredths = Long.parseLong(parts[2].trim());
                return (mins * 60 + secs) * 1000 + hundredths * 10;
            } else if (parts.length == 2) {
                long mins = Long.parseLong(parts[0].trim());
                double secs = Double.parseDouble(parts[1].trim());
                return (long) ((mins * 60 + secs) * 1000);
            } else if (parts.length == 1) { 
                return (long) (Double.parseDouble(parts[0].trim()) * 1000);
            }
        } catch (Exception e) {}
        return -1;
    }

    private void showGoalAchievedAlert(String style, long timeMs, long goalMs) {
        String styleName = style.substring(0, 1).toUpperCase() + style.substring(1);
        if ("im".equals(style)) styleName = "IM";
        
        String msg = "Congratulations! You just beat your " + styleName + " target time.\n\n" +
                     "Your Time: " + formatMs(timeMs) + "\n" +
                     "Target Goal: " + formatMs(goalMs);

        // Play clap sound
        try {
            android.media.MediaPlayer mediaPlayer = android.media.MediaPlayer.create(this, R.raw.clap);
            if (mediaPlayer != null) {
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(mp -> mp.release());
            }
        } catch (Exception e) {
            // ignore if sound fails
        }

        // Show Konfetti
        nl.dionsegijn.konfetti.xml.KonfettiView konfettiView = findViewById(R.id.konfettiView);
        if (konfettiView != null) {
            nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig = new nl.dionsegijn.konfetti.core.emitter.Emitter(5L, java.util.concurrent.TimeUnit.SECONDS).perSecond(100);
            konfettiView.start(
                    new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                            .angle(nl.dionsegijn.konfetti.core.Angle.BOTTOM)
                            .spread(nl.dionsegijn.konfetti.core.Spread.ROUND)
                            .setSpeedBetween(0f, 15f)
                            .timeToLive(2000L)
                            .colors(java.util.Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                            .sizes(new nl.dionsegijn.konfetti.core.models.Size(12, 5f, 0.2f))
                            .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, -0.1))
                            .build()
            );
        }
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("🎉 Goal Achieved! 🎉")
                .setMessage(msg)
                .setPositiveButton("Awesome", null)
                .show();
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    private void showDatePickerDialog() {
        long today = com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds();
        com.google.android.material.datepicker.MaterialDatePicker.Builder<Long> builder =
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker();
        builder.setTitleText("Select Date");
        builder.setSelection(today);

        com.google.android.material.datepicker.MaterialDatePicker<Long> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTimeInMillis(selection);
            selectedCalendar.set(Calendar.YEAR, cal.get(Calendar.YEAR));
            selectedCalendar.set(Calendar.MONTH, cal.get(Calendar.MONTH));
            selectedCalendar.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH));
            updateDateDisplay();
        });
        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void updateDateDisplay() {
        tvSwimDate.setText(dateFormat.format(selectedCalendar.getTime()));
    }

    private void showTimePickerDialog() {
        // Parse existing value if present
        int currentMin = 0, currentSec = 0, currentHundredths = 0;
        String existing = edtSwimTime.getText().toString().trim();
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

        // Build horizontal layout with 3 NumberPickers and labels
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(android.view.Gravity.CENTER);
        int dp16 = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(dp16, dp16, dp16, dp16);

        // Instruction label
        TextView instruction = new TextView(this);
        instruction.setText("Pick your time took for this stroke");
        instruction.setTextSize(13);
        instruction.setTextColor(getColor(R.color.dark_on_surface_variant));
        instruction.setGravity(android.view.Gravity.CENTER);
        instruction.setPadding(0, 0, 0, dp16);
        container.addView(instruction);

        // Labels row below the pickers
        LinearLayout labelRow = new LinearLayout(this);
        labelRow.setOrientation(LinearLayout.HORIZONTAL);
        labelRow.setGravity(android.view.Gravity.CENTER);
        int dp8 = (int) (6 * getResources().getDisplayMetrics().density);
        labelRow.setPadding(0, dp8, 0, 8);

        int labelBgColor = getColor(R.color.dark_surface_dim);

        TextView lblMin = new TextView(this);
        lblMin.setText("MM");
        lblMin.setTextSize(14);
        lblMin.setBackgroundColor(labelBgColor);
        lblMin.setTextColor(getColor(R.color.dark_on_surface));
        lblMin.setTypeface(null, android.graphics.Typeface.BOLD);
        lblMin.setGravity(android.view.Gravity.CENTER);
        lblMin.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView lblSec = new TextView(this);
        lblSec.setText("SS");
        lblSec.setTextSize(14);
        lblSec.setBackgroundColor(labelBgColor);
        lblSec.setTextColor(getColor(R.color.dark_on_surface));
        lblSec.setTypeface(null, android.graphics.Typeface.BOLD);
        lblSec.setGravity(android.view.Gravity.CENTER);
        lblSec.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView lblMs = new TextView(this);
        lblMs.setText("ss");
        lblMs.setBackgroundColor(labelBgColor);
        lblMs.setTextSize(14);
        lblMs.setTextColor(getColor(R.color.dark_on_surface));
        lblMs.setTypeface(null, android.graphics.Typeface.BOLD);
        lblMs.setGravity(android.view.Gravity.CENTER);
        lblMs.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        labelRow.addView(lblMin);
        labelRow.addView(lblSec);
        labelRow.addView(lblMs);

        container.addView(labelRow);

        // Row of pickers
        LinearLayout pickerRow = new LinearLayout(this);
        pickerRow.setOrientation(LinearLayout.HORIZONTAL);
        pickerRow.setGravity(android.view.Gravity.CENTER);

        int accentColor = getColor(R.color.dark_primary_fixed_dim);

        int pickerTextColor = getColor(R.color.dark_on_surface);

        // Minutes picker
        NumberPicker npMin = new NumberPicker(this);
        npMin.setMinValue(0);
        npMin.setMaxValue(59);
        npMin.setValue(currentMin);
        npMin.setWrapSelectorWheel(true);
        npMin.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npMin.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));
        setNumberPickerTextColor(npMin, pickerTextColor);

        // Colon label 1
        TextView colon1 = new TextView(this);
        colon1.setText(" : ");
        colon1.setTextSize(22);
        colon1.setTextColor(accentColor);
        colon1.setTypeface(null, android.graphics.Typeface.BOLD);
        colon1.setGravity(android.view.Gravity.CENTER);

        // Seconds picker
        NumberPicker npSec = new NumberPicker(this);
        npSec.setMinValue(0);
        npSec.setMaxValue(59);
        npSec.setValue(currentSec);
        npSec.setWrapSelectorWheel(true);
        npSec.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npSec.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));
        setNumberPickerTextColor(npSec, pickerTextColor);

        // Colon label 2
        TextView colon2 = new TextView(this);
        colon2.setText(" : ");
        colon2.setTextSize(22);
        colon2.setTextColor(accentColor);
        colon2.setTypeface(null, android.graphics.Typeface.BOLD);
        colon2.setGravity(android.view.Gravity.CENTER);

        // Hundredths picker
        NumberPicker npHundredths = new NumberPicker(this);
        npHundredths.setMinValue(0);
        npHundredths.setMaxValue(99);
        npHundredths.setValue(currentHundredths);
        npHundredths.setWrapSelectorWheel(true);
        npHundredths.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        npHundredths.setFormatter(value -> String.format(java.util.Locale.getDefault(), "%02d", value));
        setNumberPickerTextColor(npHundredths, pickerTextColor);

        pickerRow.addView(npMin);
        pickerRow.addView(colon1);
        pickerRow.addView(npSec);
        pickerRow.addView(colon2);
        pickerRow.addView(npHundredths);

        container.addView(pickerRow);

        // Build dialog with style-specific title
        String dialogTitle;
        if (selectedStyle.equalsIgnoreCase("im")) {
            dialogTitle = "IM Recorded Time";
        } else {
            dialogTitle = selectedStyle.substring(0, 1).toUpperCase() + selectedStyle.substring(1) + " Recorded Time";
        }

        // Add note only for IM style
        if (selectedStyle.equalsIgnoreCase("im")) {
            TextView note = new TextView(this);
            note.setText("Note: Manual entry will not store the individual swim stroke time.");
            note.setTextSize(12);
            note.setTextColor(getColor(R.color.dark_on_surface_variant));
            note.setGravity(android.view.Gravity.CENTER);
            note.setPadding(0, dp16, 0, dp16);
            container.addView(note);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(dialogTitle)
                .setView(container)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String formatted = String.format(java.util.Locale.getDefault(),
                            "%02d : %02d : %02d",
                            npMin.getValue(), npSec.getValue(), npHundredths.getValue());
                    edtSwimTime.setText(formatted);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setNumberPickerTextColor(NumberPicker picker, int color) {
        try {
            java.lang.reflect.Field selectorField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            selectorField.setAccessible(true);
            ((android.graphics.Paint) selectorField.get(picker)).setColor(color);
        } catch (Exception ignored) {}

        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof android.widget.EditText) {
                ((android.widget.EditText) child).setTextColor(color);
                break;
            }
        }

        picker.invalidate();
    }
}