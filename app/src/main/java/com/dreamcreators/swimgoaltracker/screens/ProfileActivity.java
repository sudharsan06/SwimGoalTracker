package com.dreamcreators.swimgoaltracker.screens;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class ProfileActivity extends ComponentActivity {

    public static final String EXTRA_PROFILE_ID = "profile_id";
    public static final String EXTRA_SETUP_MODE = "setup_mode";

    private NutritionDbHelper dbHelper;
    private Uri imageUri;
    private String startDate = "";
    private int targetProfileId = 1;
    private boolean isSetupMode = false;

    private final ActivityResultLauncher<String[]> pickImage = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
                    } catch (SecurityException ignored) {
                    }
                    startCrop(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    final Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        // Grant persistable permission ONLY if it's a content URI. 
                        // Cached file URIs don't need it.
                        Uri previousImageUri = imageUri;
                        imageUri = resultUri;
                        ImageView img = findViewById(R.id.imgProfile);
                        img.setImageURI(imageUri);
                        persistProfileImageUri(imageUri);
                        deleteOwnedProfileImageIfReplaced(previousImageUri, imageUri);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    final Throwable cropError = UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown"), Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void startCrop(Uri uri) {
        String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
        File outDir = new File(getFilesDir(), "profile_images");
        //noinspection ResultOfMethodCallIgnored
        outDir.mkdirs();
        Uri destinationUri = Uri.fromFile(new File(outDir, destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(getResources().getColor(R.color.lightPrimary));
        options.setStatusBarColor(getResources().getColor(R.color.midnight_blue));
        options.setToolbarWidgetColor(getResources().getColor(R.color.white));
        options.setActiveControlsWidgetColor(getResources().getColor(R.color.lightAccent));
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(true);

        Intent intent = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options)
                .getIntent(this);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropImage.launch(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_profile);

        dbHelper = new NutritionDbHelper(this);

        // Determine which profile slot we're editing
        targetProfileId = getIntent().getIntExtra(EXTRA_PROFILE_ID,
                ProfileManager.getActiveProfileId(this));
        if (targetProfileId < 1 || targetProfileId > 2) targetProfileId = 1;
        isSetupMode = getIntent().getBooleanExtra(EXTRA_SETUP_MODE, false);

        ImageView img = findViewById(R.id.imgProfile);
        View btnPick = findViewById(R.id.btnPickImage);
        EditText etName = findViewById(R.id.etName);
        EditText etAge = findViewById(R.id.etAge);
        EditText etHeight = findViewById(R.id.etHeight);
        EditText etWeight = findViewById(R.id.etWeight);
        View btnPickStartDate = findViewById(R.id.btnPickStartDate);
        TextView tvStartDate = findViewById(R.id.tvStartDate);
        TextView tvLastLogin = findViewById(R.id.tvLastLogin);
        View btnSave = findViewById(R.id.btnSaveProfile);
        View btnSkip = findViewById(R.id.btnSkip);
        View btnLogout = findViewById(R.id.btnLogout);
        View dividerLogout = findViewById(R.id.btnLogoutDivider);
        
        View btnPoolDistance = findViewById(R.id.btnPoolDistance);
        
        btnPoolDistance.setOnClickListener(v -> {
            startActivity(new Intent(this, PoolDistanceActivity.class));
        });

        etAge.setOnClickListener(v -> {
            int current = parseIntSafe(etAge.getText().toString());
            showNumberPicker("Select Age", 1, 120, current == 0 ? 25 : current, "", etAge);
        });

        etHeight.setOnClickListener(v -> {
            int current = parseIntSafe(etHeight.getText().toString().replace(" cm", ""));
            showNumberPicker("Select Height", 50, 250, current == 0 ? 170 : current, "cm", etHeight);
        });

        etWeight.setOnClickListener(v -> {
            int current = parseIntSafe(etWeight.getText().toString().replace(" kg", ""));
            showNumberPicker("Select Weight", 10, 200, current == 0 ? 70 : current, "kg", etWeight);
        });

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT image_uri, name, age, height, weight, start_date, last_login, pool_distance FROM profile WHERE id=?",
                new String[]{String.valueOf(targetProfileId)});
        if (c.moveToFirst()) {
            String uriStr = c.getString(0);
            if (uriStr != null && !uriStr.isEmpty()) {
                imageUri = Uri.parse(uriStr);
                if (isReadableImageUri(imageUri)) {
                    try { img.setImageURI(imageUri); } catch (Exception ignored) { imageUri = null; }
                } else {
                    imageUri = null;
                }
            }
            etName.setText(c.getString(1));
            etAge.setText(String.valueOf(c.getInt(2)));

            // Pre-select height in spinner
            int savedHeight = Math.round(c.getFloat(3));
            if (savedHeight >= 50 && savedHeight <= 250) {
                etHeight.setText(savedHeight + " cm");
            }

            // Pre-select weight in spinner
            int savedWeight = Math.round(c.getFloat(4));
            if (savedWeight >= 10 && savedWeight <= 200) {
                etWeight.setText(savedWeight + " kg");
            }

            startDate = c.getString(5) == null ? "" : c.getString(5);
            tvStartDate.setText(startDate);
            tvLastLogin.setText(c.getString(6) == null ? "" : ("Last login: " + c.getString(6)));
            btnLogout.setVisibility(View.VISIBLE);
            dividerLogout.setVisibility(View.VISIBLE);
            
            // Pool distance is at index 7. Let onResume handle updating the TextView though, to keep it fresh
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

            String ageStr = etAge.getText().toString().trim();
            int age = parseIntSafe(ageStr);
            if (ageStr.isEmpty() || age <= 0 || age > 120) {
                Toast.makeText(this, "Please enter a valid age (1-120)", Toast.LENGTH_SHORT).show();
                return;
            }

            String heightStr = etHeight.getText().toString();
            if (heightStr.isEmpty()) {
                Toast.makeText(this, "Please select a valid height", Toast.LENGTH_SHORT).show();
                return;
            }
            int height = parseIntSafe(heightStr.replace(" cm", ""));

            String weightStr = etWeight.getText().toString();
            if (weightStr.isEmpty()) {
                Toast.makeText(this, "Please select a valid weight", Toast.LENGTH_SHORT).show();
                return;
            }
            int weight = parseIntSafe(weightStr.replace(" kg", ""));

            if (startDate == null || startDate.trim().isEmpty()) {
                Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ContentValues values = new ContentValues();
            values.put("id", targetProfileId);
            values.put("image_uri", imageUri == null ? null : imageUri.toString());
            values.put("name", name);
            values.put("age", age);
            values.put("height", (float) height);
            values.put("weight", (float) weight);
            values.put("start_date", startDate);
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime());
            values.put("last_login", now);

            long updated = dbHelper.getWritableDatabase().update("profile", values,
                    "id=" + targetProfileId, null);
            if (updated == 0) {
                long inserted = dbHelper.getWritableDatabase().insert("profile", null, values);
                if (inserted <= 0) {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
            ProfileManager.setActiveProfileId(this, targetProfileId);
            proceedToMain();
        });

        btnSkip.setOnClickListener(v -> proceedToMain());

        btnLogout.setOnClickListener(v -> {
            ContentValues values = new ContentValues();
            values.put("last_login", (String) null);
            dbHelper.getWritableDatabase().update("profile", values,
                    "id=" + targetProfileId, null);

            // Sign out of Firebase (handles Email/Password perfectly)
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            
            // Sign out of Google to force account picker prompt next time
            com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut();

            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
            
            // Redirect to Login Screen
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null) {
            Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT pool_distance FROM profile WHERE id=?", new String[]{String.valueOf(targetProfileId)});
            if (c.moveToFirst()) {
                int dist = c.getInt(0);
                TextView tvPoolDistance = findViewById(R.id.tvPoolDistance);
                if (tvPoolDistance != null) {
                    if (dist == 0) {
                        tvPoolDistance.setText("Open Water");
                    } else {
                        tvPoolDistance.setText(dist + " metres");
                    }
                }
            }
            c.close();
        }
    }
    private void proceedToMain() {
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        if (isSetupMode) {
            // Return to Switch Swimmer so user can choose which profile to continue as
            Intent i = new Intent(this, SwitchSwimmerActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private void persistProfileImageUri(Uri uri) {
        if (dbHelper == null || uri == null) return;
        ContentValues values = new ContentValues();
        values.put("id", targetProfileId);
        values.put("image_uri", uri.toString());
        long updated = dbHelper.getWritableDatabase().update("profile", values, "id=" + targetProfileId, null);
        if (updated == 0) {
            dbHelper.getWritableDatabase().insert("profile", null, values);
        }
    }

    private boolean isReadableImageUri(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) {
            return new File(uri.getPath() == null ? "" : uri.getPath()).exists();
        }
        if ("content".equalsIgnoreCase(scheme)) {
            return hasPersistedReadPermission(uri);
        }
        return false;
    }

    private void deleteOwnedProfileImageIfReplaced(Uri previous, Uri next) {
        if (previous == null || next == null) return;
        if (previous.equals(next)) return;
        if (!"file".equalsIgnoreCase(previous.getScheme())) return;

        try {
            String prevPath = previous.getPath();
            if (prevPath == null) return;
            File prevFile = new File(prevPath);

            File ownedDir = new File(getFilesDir(), "profile_images");
            String ownedDirPath = ownedDir.getCanonicalPath() + File.separator;
            String prevCanonical = prevFile.getCanonicalPath();
            if (prevCanonical.startsWith(ownedDirPath) && prevFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                prevFile.delete();
            }
        } catch (Exception ignored) {
        }
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

    private void showNumberPicker(String title, int minValue, int maxValue, int currentValue, String suffix, EditText targetEditText) {
        NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(minValue);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setValue(currentValue);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        // Style the picker with some padding
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        numberPicker.setPadding(0, padding, 0, padding);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(numberPicker)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String value = suffix.isEmpty()
                            ? String.valueOf(numberPicker.getValue())
                            : numberPicker.getValue() + " " + suffix;
                    targetEditText.setText(value);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}


