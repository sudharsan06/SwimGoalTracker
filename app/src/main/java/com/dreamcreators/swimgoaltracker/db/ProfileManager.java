package com.dreamcreators.swimgoaltracker.db;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/**
 * Central helper for managing multiple swimmer profiles (max 2).
 * The "active profile" is stored in SharedPreferences and used across the app.
 */
public class ProfileManager {

    private static final String PREFS = "profile_manager";
    private static final String KEY_ACTIVE_ID = "active_profile_id";

    // ─── Active profile id (1 or 2) ────────────────────────────────────────

    public static int getActiveProfileId(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_ACTIVE_ID, 1);
    }

    public static String getActiveProfileName(Context ctx) {
        int id = getActiveProfileId(ctx);
        NutritionDbHelper db = new NutritionDbHelper(ctx);
        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT name FROM profile WHERE id = ?", new String[]{String.valueOf(id)});
        String name = "";
        if (c.moveToFirst()) {
            name = c.getString(0);
        }
        c.close();
        return name != null ? name : "";
    }

    public static void setActiveProfileId(Context ctx, int id) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_ACTIVE_ID, id).apply();
    }

    // ─── Load all saved profiles ────────────────────────────────────────────

    public static List<SwimmerProfile> getProfiles(Context ctx) {
        List<SwimmerProfile> list = new ArrayList<>();
        NutritionDbHelper db = new NutritionDbHelper(ctx);
        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT id, name, last_login, image_uri FROM profile WHERE name IS NOT NULL AND name != '' ORDER BY id ASC", null);
        while (c.moveToNext()) {
            list.add(new SwimmerProfile(
                    c.getInt(0),
                    c.getString(1),
                    c.getString(2),
                    c.getString(3)
            ));
        }
        c.close();
        return list;
    }

    /** Count profiles that have a non-empty name */
    public static int getProfileCount(Context ctx) {
        NutritionDbHelper db = new NutritionDbHelper(ctx);
        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM profile WHERE name IS NOT NULL AND name != ''", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    /** First available slot id (1 or 2) that has no saved profile yet */
    public static int getNextAvailableProfileId(Context ctx) {
        NutritionDbHelper db = new NutritionDbHelper(ctx);
        for (int id = 1; id <= 2; id++) {
            Cursor c = db.getReadableDatabase().rawQuery(
                    "SELECT name FROM profile WHERE id = ?", new String[]{String.valueOf(id)});
            boolean hasProfile = c.moveToFirst() &&
                    c.getString(0) != null && !c.getString(0).trim().isEmpty();
            c.close();
            if (!hasProfile) return id;
        }
        return -1; // both slots taken
    }

    // ─── Simple data class ──────────────────────────────────────────────────

    public static class SwimmerProfile {
        public final int id;
        public final String name;
        public final String lastLogin;
        public final String imageUri;

        public SwimmerProfile(int id, String name, String lastLogin, String imageUri) {
            this.id = id;
            this.name = name != null ? name : "";
            this.lastLogin = lastLogin;
            this.imageUri = imageUri;
        }

        /** Two-letter initials for the avatar circle */
        public String initials() {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (!name.isEmpty()) {
                return name.substring(0, Math.min(2, name.length())).toUpperCase();
            }
            return "?";
        }
    }
}
