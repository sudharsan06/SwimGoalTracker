package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.R;

public class SplashActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.midnight_blue));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                com.google.firebase.auth.FirebaseAuth mAuth = com.google.firebase.auth.FirebaseAuth.getInstance();
                if (mAuth.getCurrentUser() == null) {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
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
                                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                                        finish();
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
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }
        }, 1500);
    }
    
    private void navigateNext() {
        Intent next = new Intent(SplashActivity.this, SwitchSwimmerActivity.class);
        next.putExtra(SwitchSwimmerActivity.EXTRA_FROM_LOGIN, true);
        startActivity(next);
        finish();
    }
}


