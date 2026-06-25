package com.dreamcreators.swimgoaltracker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.dreamcreators.swimgoaltracker.utility.ThemeManager;

public class SwimGoalTrackerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        String mode = ThemeManager.getThemeMode(this);
        if (ThemeManager.MODE_LIGHT.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (ThemeManager.MODE_DARK.equals(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            if (hour >= 6 && hour < 18) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        }
    }
}