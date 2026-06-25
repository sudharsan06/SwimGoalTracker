package com.dreamcreators.swimgoaltracker.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    private static final String PREFS_SYNC = "swim_sync";
    private static final String KEY_LAST_SYNC = "last_sync_at";
    private static final String SYNC_WORK_NAME = "weekly_sync";
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No user logged in, skipping sync");
            scheduleNextSync();
            return Result.success();
        }

        String uid = user.getUid();
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_SYNC, Context.MODE_PRIVATE);
        long lastSyncAt = prefs.getLong(KEY_LAST_SYNC, 0);
        long now = System.currentTimeMillis();

        NutritionDbHelper db = new NutritionDbHelper(ctx);
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference("user_data").child(uid);

        try {
            List<ProfileManager.SwimmerProfile> profiles = ProfileManager.getProfiles(ctx);

            for (ProfileManager.SwimmerProfile p : profiles) {
                String pid = String.valueOf(p.id);

                // Profile info
                android.content.ContentValues profileRow = db.getProfileRow(p.id);
                if (profileRow != null && profileRow.size() > 0) {
                    Tasks.await(rootRef.child("profiles").child(pid).child("info").updateChildren(toMap(profileRow)), 15, TimeUnit.SECONDS);
                }

                // Swim sessions since last sync
                java.util.ArrayList<android.content.ContentValues> sessions = db.getSessionsSince(lastSyncAt, p.id);
                if (!sessions.isEmpty()) {
                    DatabaseReference sessionsRef = rootRef.child("profiles").child(pid).child("swim_sessions");
                    for (android.content.ContentValues session : sessions) {
                        Tasks.await(sessionsRef.push().setValue(toMap(session)), 15, TimeUnit.SECONDS);
                    }
                }

                // Goals
                SharedPreferences goalsPrefs = ctx.getSharedPreferences("swim_goals", Context.MODE_PRIVATE);
                Map<String, Object> goalData = new HashMap<>();
                goalData.put("weekly_sessions", goalsPrefs.getString("weekly_sessions_" + pid, ""));
                goalData.put("weekly_km", goalsPrefs.getString("weekly_km_" + pid, ""));
                goalData.put("goal_free", goalsPrefs.getString("goal_free_" + pid, ""));
                goalData.put("goal_back", goalsPrefs.getString("goal_back_" + pid, ""));
                goalData.put("goal_breast", goalsPrefs.getString("goal_breast_" + pid, ""));
                goalData.put("goal_fly", goalsPrefs.getString("goal_fly_" + pid, ""));
                goalData.put("goal_im", goalsPrefs.getString("goal_im_" + pid, ""));
                Tasks.await(rootRef.child("profiles").child(pid).child("goals").setValue(goalData), 10, TimeUnit.SECONDS);

                // Alerts
                SharedPreferences alertsPrefs = ctx.getSharedPreferences("swim_alerts", Context.MODE_PRIVATE);
                Map<String, Object> alertData = new HashMap<>();
                alertData.put("training_reminder", alertsPrefs.getBoolean("training_reminder_" + pid, false));
                alertData.put("rest_alert", alertsPrefs.getBoolean("rest_alert_" + pid, false));
                alertData.put("goal_alert", alertsPrefs.getBoolean("goal_alert_" + pid, false));
                alertData.put("reminder_hour", alertsPrefs.getInt("reminder_hour_" + pid, 8));
                alertData.put("reminder_minute", alertsPrefs.getInt("reminder_minute_" + pid, 0));
                Tasks.await(rootRef.child("profiles").child(pid).child("alerts").setValue(alertData), 10, TimeUnit.SECONDS);
            }

            // Nutrition (shared across profiles)
            java.util.ArrayList<android.content.ContentValues> nutrition = db.getAllNutrition();
            if (!nutrition.isEmpty()) {
                for (android.content.ContentValues row : nutrition) {
                    String date = row.getAsString("date");
                    Map<String, Object> data = new HashMap<>();
                    data.put("calories", row.getAsInteger("calories"));
                    data.put("protein", row.getAsInteger("protein"));
                    data.put("carbs", row.getAsInteger("carbs"));
                    data.put("fats", row.getAsInteger("fats"));
                    Tasks.await(rootRef.child("daily_nutrition").child(date).setValue(data), 10, TimeUnit.SECONDS);
                }
            }

            // Sync metadata
            Tasks.await(rootRef.child("last_sync").setValue(now), 10, TimeUnit.SECONDS);
            Tasks.await(rootRef.child("sync_version").setValue(1), 10, TimeUnit.SECONDS);

            prefs.edit().putLong(KEY_LAST_SYNC, now).apply();
            Log.i(TAG, "Sync completed for uid=" + uid);

        } catch (Exception e) {
            Log.e(TAG, "Sync failed", e);
            scheduleNextSync();
            return Result.retry();
        }

        scheduleNextSync();
        return Result.success();
    }

    private Map<String, Object> toMap(android.content.ContentValues values) {
        Map<String, Object> map = new HashMap<>();
        for (String key : values.keySet()) {
            Object val = values.get(key);
            if (val != null) map.put(key, val);
        }
        return map;
    }

    private void scheduleNextSync() {
        long delay = getDelayToNextSaturday2AM();
        OneTimeWorkRequest nextSync = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(SYNC_WORK_NAME)
                .build();
        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, nextSync);
    }

    private long getDelayToNextSaturday2AM() {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        next.set(Calendar.HOUR_OF_DAY, 2);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) {
            next.add(Calendar.WEEK_OF_YEAR, 1);
        }
        return next.getTimeInMillis() - now.getTimeInMillis();
    }

    public static void runNow(Context context) {
        OneTimeWorkRequest syncNow = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .addTag(SYNC_WORK_NAME)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniqueWork(SYNC_WORK_NAME, ExistingWorkPolicy.REPLACE, syncNow);
    }
}