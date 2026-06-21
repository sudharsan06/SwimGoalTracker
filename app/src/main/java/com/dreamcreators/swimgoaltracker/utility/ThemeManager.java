package com.dreamcreators.swimgoaltracker.utility;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.Calendar;

public class ThemeManager {

    private static final String PREFS_NAME = "app_theme";
    private static final String KEY_THEME_MODE = "theme_mode";

    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_AUTO = "auto";

    public static String getThemeMode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_THEME_MODE, MODE_AUTO);
    }

    public static void setThemeMode(Context context, String mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_THEME_MODE, mode).apply();
        applyNightMode(mode);
    }

    public static void applyTheme(Activity activity) {
        String mode = getThemeMode(activity);
        applyNightMode(mode);
    }

    private static void applyNightMode(String mode) {
        int nightMode;
        if (MODE_LIGHT.equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if (MODE_DARK.equals(mode)) {
            nightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hour >= 6 && hour < 18) {
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else {
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
            }
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    public static boolean isDarkMode(Context context) {
        String mode = getThemeMode(context);
        if (MODE_LIGHT.equals(mode)) return false;
        if (MODE_DARK.equals(mode)) return true;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return hour < 6 || hour >= 18;
    }
}
