package com.dreamcreators.swimgoaltracker;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends ComponentActivity {

    private NutritionDbHelper dbHelper;
    private Uri imageUri;
    private String startDate = "";

    private final ActivityResultLauncher<String[]> pickImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    try { getContentResolver().takePersistableUriPermission(uri, takeFlags); } catch (Exception ignored) { }
                    imageUri = uri;
                    ImageView img = findViewById(R.id.imgProfile);
                    img.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbHelper = new NutritionDbHelper(this);

        ImageView img = findViewById(R.id.imgProfile);
        Button btnPick = findViewById(R.id.btnPickImage);
        EditText etName = findViewById(R.id.etName);
        EditText etAge = findViewById(R.id.etAge);
        EditText etHeight = findViewById(R.id.etHeight);
        EditText etWeight = findViewById(R.id.etWeight);
        Button btnPickStartDate = findViewById(R.id.btnPickStartDate);
        TextView tvStartDate = findViewById(R.id.tvStartDate);
        TextView tvLastLogin = findViewById(R.id.tvLastLogin);
        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnSkip = findViewById(R.id.btnSkip);
        Button btnLogout = findViewById(R.id.btnLogout);

        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT image_uri, name, age, height, weight, start_date, last_login FROM profile WHERE id=1", null);
        if (c.moveToFirst()) {
            String uriStr = c.getString(0);
            if (uriStr != null && !uriStr.isEmpty()) {
                imageUri = Uri.parse(uriStr);
                if (hasPersistedReadPermission(imageUri)) {
                    try { img.setImageURI(imageUri); } catch (Exception ignored) { }
                } else {
                    imageUri = null;
                }
            }
            etName.setText(c.getString(1));
            etAge.setText(String.valueOf(c.getInt(2)));
            etHeight.setText(String.valueOf(c.getFloat(3)));
            etWeight.setText(String.valueOf(c.getFloat(4)));
            startDate = c.getString(5) == null ? "" : c.getString(5);
            tvStartDate.setText(startDate);
            tvLastLogin.setText(c.getString(6) == null ? "" : ("Last login: " + c.getString(6)));
        }
        c.close();

        btnPick.setOnClickListener(v -> pickImage.launch(new String[]{"image/*"}));

        btnPickStartDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH);
            int d = cal.get(Calendar.DAY_OF_MONTH);
            android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(this, R.style.CustomDatePickerDialog,(view, year, month, dayOfMonth) -> {
                startDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                tvStartDate.setText(startDate);
            }, y, m, d);
            dialog.show();
        });

        btnSave.setOnClickListener(v -> {
            // Hide keyboard
            View currentFocus = getCurrentFocus();
            if (currentFocus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ContentValues values = new ContentValues();
            values.put("id", 1);
            values.put("image_uri", imageUri == null ? null : imageUri.toString());
            values.put("name", name);
            values.put("age", parseIntSafe(etAge.getText().toString()));
            values.put("height", parseFloatSafe(etHeight.getText().toString()));
            values.put("weight", parseFloatSafe(etWeight.getText().toString()));
            values.put("start_date", startDate);
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
            values.put("last_login", now);

            long updated = dbHelper.getWritableDatabase().update("profile", values, "id=1", null);
            if (updated == 0) {
                long inserted = dbHelper.getWritableDatabase().insert("profile", null, values);
                if (inserted <= 0) {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
            proceedToMain();
        });

        btnSkip.setOnClickListener(v -> proceedToMain());

        btnLogout.setOnClickListener(v -> {
            ContentValues values = new ContentValues();
            values.put("last_login", (String) null);
            dbHelper.getWritableDatabase().update("profile", values, "id=1", null);
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            finishAffinity();
            System.runFinalization();
            System.exit(0);
        });
    }

    private void proceedToMain() {
        // Hide keyboard
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private float parseFloatSafe(String s) {
        try { return Float.parseFloat(s.trim()); } catch (Exception e) { return 0f; }
    }

    private boolean hasPersistedReadPermission(Uri uri) {
        try {
            for (android.content.UriPermission p : getContentResolver().getPersistedUriPermissions()) {
                if (p.getUri().equals(uri) && p.isReadPermission()) {
                    return true;
                }
            }
        } catch (Exception ignored) { }
        return false;
    }
}


