package com.dreamcreators.swimgoaltracker.receivers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.db.NutritionDbHelper;
import com.dreamcreators.swimgoaltracker.db.ProfileManager;
import com.dreamcreators.swimgoaltracker.screens.MainActivity;
import com.dreamcreators.swimgoaltracker.utility.AlertManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "swim_alerts_channel";


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        createNotificationChannel(context);

        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || "android.intent.action.QUICKBOOT_POWERON".equals(action)) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("swim_alerts", Context.MODE_PRIVATE);
            int activeId = ProfileManager.getActiveProfileId(context);
            if (prefs.getBoolean("training_reminder_" + activeId, false)) {
                int hour = prefs.getInt("reminder_hour_" + activeId, 7);
                int minute = prefs.getInt("reminder_minute_" + activeId, 0);
                AlertManager.scheduleTrainingReminder(context, hour, minute);
            }
            if (prefs.getBoolean("rest_alert_" + activeId, false)) {
                AlertManager.scheduleRestAlert(context);
            }
            return;
        }

        if (AlertManager.ACTION_TRAINING_REMINDER.equals(action)) {
            showNotification(context, "Time to Swim! 🏊", "It's time for your daily training session.", 101);
            
            // Reschedule for next day (since setExact only runs once)
            android.content.SharedPreferences prefs = context.getSharedPreferences("swim_alerts", Context.MODE_PRIVATE);
            int activeId = ProfileManager.getActiveProfileId(context);
            if (prefs.getBoolean("training_reminder_" + activeId, false)) {
                int hour = prefs.getInt("reminder_hour_" + activeId, 7);
                int minute = prefs.getInt("reminder_minute_" + activeId, 0);
                AlertManager.scheduleTrainingReminder(context, hour, minute);
            }
        } else if (AlertManager.ACTION_REST_ALERT.equals(action)) {
            // Check if user swam in last 48 hours
            if (shouldTriggerRestAlert(context)) {
                showNotification(context, "Rest Day 😴", "You haven't swam in 2 days. Rest up and come back stronger!", 102);
            }
            // Reschedule for next day
            android.content.SharedPreferences prefs = context.getSharedPreferences("swim_alerts", Context.MODE_PRIVATE);
            int activeId = ProfileManager.getActiveProfileId(context);
            if (prefs.getBoolean("rest_alert_" + activeId, false)) {
                AlertManager.scheduleRestAlert(context);
            }
        }
    }

    private boolean shouldTriggerRestAlert(Context context) {
        int activeProfileId = ProfileManager.getActiveProfileId(context);
        NutritionDbHelper dbHelper = new NutritionDbHelper(context);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -2);
        String twoDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        boolean triggered = true; // assume true, if we find a session, we set to false
        
        Cursor cursor = null;
        try {
            cursor = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT COUNT(*) FROM swim_sessions WHERE date >= ? AND profile_id = ?",
                    new String[]{twoDaysAgo, String.valueOf(activeProfileId)}
            );
            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    triggered = false; // They have swum in the last 2 days
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return triggered;
    }

    private void showNotification(Context context, String title, String text, int notificationId) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.sgt_logo) // Using existing app icon
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify(notificationId, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Swim Goal Alerts";
            String description = "Notifications for training reminders and rest days";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
