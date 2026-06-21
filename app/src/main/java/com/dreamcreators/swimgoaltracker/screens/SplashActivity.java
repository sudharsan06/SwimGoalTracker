package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;

public class SplashActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (ThemeManager.isDarkMode(this)) {
            setTheme(R.style.AppTheme_NoActionBar_Dark);
        } else {
            setTheme(R.style.AppTheme_NoActionBar_Light);
        }
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.dark_surface_low));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
                if (mAuth.getCurrentUser() == null) {
                    launchLoginOrOnboarding();
                } else {
                    String deviceId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
                    com.google.firebase.database.FirebaseDatabase.getInstance().getReference("device_mapping")
                            .child(mAuth.getCurrentUser().getUid())
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(com.google.firebase.database.DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists() && !deviceId.equals(dataSnapshot.getValue(String.class))) {
                                        mAuth.signOut();
                                        android.widget.Toast.makeText(SplashActivity.this, "This account is registered on another device.", android.widget.Toast.LENGTH_LONG).show();
                                        launchLoginOrOnboarding();
                                    } else {
                                        navigateNext();
                                    }
                                }

                                @Override
                                public void onCancelled(com.google.firebase.database.DatabaseError databaseError) {
                                    navigateNext();
                                }
                            });
                }
            } catch (Exception e) {
                // In case google-services.json is missing or other initialization error
                launchLoginOrOnboarding();
            }
        }, 1500);
    }
    
    private void launchLoginOrOnboarding() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        if (isFirstLaunch) {
            startActivity(new Intent(SplashActivity.this, InstructionActivity.class));
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }

    private void navigateNext() {
        Intent next = new Intent(SplashActivity.this, SwitchSwimmerActivity.class);
        next.putExtra(SwitchSwimmerActivity.EXTRA_FROM_LOGIN, true);
        startActivity(next);
        finish();
    }
}


