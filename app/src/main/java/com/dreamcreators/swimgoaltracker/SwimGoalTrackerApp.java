package com.dreamcreators.swimgoaltracker;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.dreamcreators.swimgoaltracker.sync.SyncWorker;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;

import java.util.concurrent.TimeUnit;

public class SwimGoalTrackerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initNightMode();
        scheduleWeeklySync();
    }

    private void initNightMode() {
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

    private void scheduleWeeklySync() {
        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(this)
                .enqueueUniqueWork("weekly_sync", ExistingWorkPolicy.KEEP, syncWork);
    }
}