package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.List;

public class SwitchSwimmerActivity extends AppCompatActivity {

    // Pass this extra = true when coming from LOGIN (so we can finish cleanly)
    public static final String EXTRA_FROM_LOGIN = "from_login";

    private List<ProfileManager.SwimmerProfile> profiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_switch_swimmer);

        refreshUI();

        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                }
            });
            adView.loadAd(new AdRequest.Builder().build());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from ProfileActivity (new swimmer just added)
        refreshUI();
    }

    private void refreshUI() {
        profiles = ProfileManager.getProfiles(this);
        int activeId = ProfileManager.getActiveProfileId(this);

        MaterialCardView card1 = findViewById(R.id.cardProfile1);
        MaterialCardView card2 = findViewById(R.id.cardProfile2);
        LinearLayout btnAdd = findViewById(R.id.btnAddProfile);
        TextView tvAddHint = findViewById(R.id.tvAddHint);
        TextView tvMaxHint = findViewById(R.id.tvMaxHint);

        // Hide both cards first
        card1.setVisibility(View.GONE);
        card2.setVisibility(View.GONE);

        for (ProfileManager.SwimmerProfile p : profiles) {
            if (p.id == 1) {
                card1.setVisibility(View.VISIBLE);
                bindCard(card1,
                        R.id.tvAvatar1, R.id.ivAvatar1, R.id.ivDelete1,
                        R.id.tvName1, R.id.tvSub1, R.id.tvAction1,
                        p, activeId);
            } else if (p.id == 2) {
                card2.setVisibility(View.VISIBLE);
                bindCard(card2,
                        R.id.tvAvatar2, R.id.ivAvatar2, R.id.ivDelete2,
                        R.id.tvName2, R.id.tvSub2, R.id.tvAction2,
                        p, activeId);
            }
        }

        // Add swimmer button
        boolean canAdd = profiles.size() < 2;
        if (canAdd) {
            btnAdd.setVisibility(View.VISIBLE);
            tvAddHint.setText("Add a swimmer");
            tvMaxHint.setText("");
            btnAdd.setOnClickListener(v -> openAddProfile());
        } else {
            // Show greyed-out hint
            btnAdd.setVisibility(View.VISIBLE);
            tvAddHint.setTextColor(0xFF9E9E9E);
            tvMaxHint.setText("Max 2 swimmers per device\nRemove a profile to add another");
            btnAdd.setOnClickListener(v ->
                    Toast.makeText(this, "Max 2 swimmers per device", Toast.LENGTH_SHORT).show());
        }

        // If no profiles at all → straight to setup
        if (profiles.isEmpty()) {
            findViewById(R.id.btnBackToHome).setVisibility(View.GONE);
            openAddProfile();
        } else {
            // Show Back to Home button at bottom
            View btnBack = findViewById(R.id.btnBackToHome);
            btnBack.setVisibility(View.VISIBLE);
            btnBack.setOnClickListener(v -> onBackPressed());
        }
    }

    private void bindCard(MaterialCardView card,
                          int tvAvatarId, int ivAvatarId, int ivDeleteId,
                          int nameId, int subId, int actionId,
                          ProfileManager.SwimmerProfile p, int activeId) {

        TextView tvAvatar = card.findViewById(tvAvatarId);
        ImageView ivAvatar = card.findViewById(ivAvatarId);
        ImageView ivDelete = card.findViewById(ivDeleteId);
        TextView tvName = card.findViewById(nameId);
        TextView tvSub = card.findViewById(subId);
        TextView tvAction = card.findViewById(actionId);

        tvAvatar.setText(p.initials());
        if (p.imageUri != null && !p.imageUri.isEmpty()) {
            boolean hasPermission = false;
            try {
                Uri uri = Uri.parse(p.imageUri);
                hasPermission = "file".equalsIgnoreCase(uri.getScheme()) ||
                            ("content".equalsIgnoreCase(uri.getScheme()) &&
                            getContentResolver().getPersistedUriPermissions().stream()
                                .anyMatch(perm -> perm.getUri().equals(uri)));
                if (hasPermission) {
                    ivAvatar.setImageURI(uri);
                    ivAvatar.setVisibility(View.VISIBLE);
                    tvAvatar.setVisibility(View.GONE);
                } else {
                    ivAvatar.setVisibility(View.GONE);
                    tvAvatar.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                ivAvatar.setVisibility(View.GONE);
                tvAvatar.setVisibility(View.VISIBLE);
            }
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvAvatar.setVisibility(View.VISIBLE);
        }
        tvName.setText(p.name);
        tvSub.setText(p.lastLogin != null ? "Last login: " + p.lastLogin : "Profile " + p.id);

        boolean isActive = (p.id == activeId);
        if (isActive) {
            tvAction.setText("Active");
            tvAction.setBackground(getDrawable(R.drawable.chip_bg_active));
            tvAction.setTextColor(0xFFFFFFFF);
            // Highlight card with light blue tint
            card.setCardBackgroundColor(0xFFE3F2FD);
            card.setOnClickListener(v -> goToHome());
            tvAction.setOnClickListener(v -> goToHome());
        } else {
            tvAction.setText("Switch →");
            tvAction.setBackground(null);
            tvAction.setTextColor(getColor(R.color.lightPrimary));
            card.setCardBackgroundColor(0xFFFFFFFF);
            card.setOnClickListener(v -> switchTo(p.id));
            tvAction.setOnClickListener(v -> switchTo(p.id));
        }

        ivDelete.setOnClickListener(v -> confirmDelete(p));
    }

    private void confirmDelete(ProfileManager.SwimmerProfile p) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to delete profile: " + p.name + "? All associated swims will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    NutritionDbHelper db = new NutritionDbHelper(this);
                    db.getWritableDatabase().execSQL("UPDATE profile SET name = NULL, image_uri = NULL WHERE id = " + p.id);
                    db.getWritableDatabase().execSQL("DELETE FROM swim_sessions WHERE profile_id = " + p.id);
                    
                    // If we deleted the active profile, reset the active profile to the remaining one (if any)
                    if (ProfileManager.getActiveProfileId(this) == p.id) {
                        List<ProfileManager.SwimmerProfile> remaining = ProfileManager.getProfiles(this);
                        if (!remaining.isEmpty()) {
                            ProfileManager.setActiveProfileId(this, remaining.get(0).id);
                        } else {
                            ProfileManager.setActiveProfileId(this, 1);
                        }
                    }
                    
                    Toast.makeText(this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    refreshUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchTo(int profileId) {
        ProfileManager.setActiveProfileId(this, profileId);
        goToHome();
    }

    private void openAddProfile() {
        int nextId = ProfileManager.getNextAvailableProfileId(this);
        if (nextId == -1) {
            Toast.makeText(this, "Max 2 swimmers per device", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ProfileActivity.class);
        intent.putExtra(ProfileActivity.EXTRA_PROFILE_ID, nextId);
        intent.putExtra(ProfileActivity.EXTRA_SETUP_MODE, true);
        startActivity(intent);
        // Don't finish — user may cancel and come back here
    }

    private void goToHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public void onBackPressed() {
        // If coming from login and there's at least 1 valid profile, allow continuing
        if (!profiles.isEmpty()) {
            // Ensure active profile is set
            if (ProfileManager.getActiveProfileId(this) == 0) {
                ProfileManager.setActiveProfileId(this, profiles.get(0).id);
            }
            goToHome();
        } else {
            super.onBackPressed();
        }
    }
}
