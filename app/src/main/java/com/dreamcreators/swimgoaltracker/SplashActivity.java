package com.dreamcreators.swimgoaltracker;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;

public class SplashActivity extends ComponentActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            NutritionDbHelper db = new NutritionDbHelper(this);
            boolean needsProfile = true;
            Cursor c = db.getReadableDatabase().rawQuery("SELECT name FROM profile WHERE id=1", null);
            if (c.moveToFirst()) {
                String name = c.getString(0);
                needsProfile = (name == null || name.trim().isEmpty());
            }
            c.close();

            Intent next = new Intent(SplashActivity.this, needsProfile ? ProfileActivity.class : MainActivity.class);
            startActivity(next);
            finish();
        }, 4000);
    }
}


