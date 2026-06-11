package com.dreamcreators.swimgoaltracker.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NutritionDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "goaltracker.db";
    public static final int DB_VERSION = 7;

    public NutritionDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS daily_nutrition (" +
                "date TEXT PRIMARY KEY, " +
                "calories INTEGER NOT NULL DEFAULT 0, " +
                "protein INTEGER NOT NULL DEFAULT 0, " +
                "carbs INTEGER NOT NULL DEFAULT 0, " +
                "fats INTEGER NOT NULL DEFAULT 0" +
                ")");

        // No CHECK(id=1) — allows up to 2 profiles (id=1, id=2)
        db.execSQL("CREATE TABLE IF NOT EXISTS profile (" +
                "id INTEGER PRIMARY KEY, " +
                "image_uri TEXT, " +
                "name TEXT, " +
                "age INTEGER, " +
                "height REAL, " +
                "weight REAL, " +
                "start_date TEXT, " +
                "last_login TEXT, " +
                "pool_distance INTEGER NOT NULL DEFAULT 25" +
                ")");

        // profile_id column links sessions to their owner profile
        db.execSQL("CREATE TABLE IF NOT EXISTS swim_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "profile_id INTEGER NOT NULL DEFAULT 1, " +
                "date TEXT NOT NULL, " +
                "freestyle_ms INTEGER NOT NULL DEFAULT 0, " +
                "backstroke_ms INTEGER NOT NULL DEFAULT 0, " +
                "breaststroke_ms INTEGER NOT NULL DEFAULT 0, " +
                "butterfly_ms INTEGER NOT NULL DEFAULT 0, " +
                "im_ms INTEGER NOT NULL DEFAULT 0, " +
                "im_butterfly_ms INTEGER NOT NULL DEFAULT 0, " +
                "im_backstroke_ms INTEGER NOT NULL DEFAULT 0, " +
                "im_breaststroke_ms INTEGER NOT NULL DEFAULT 0, " +
                "im_freestyle_ms INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL DEFAULT 0, " +
                "pool_distance INTEGER NOT NULL DEFAULT 25" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS profile (" +
                    "id INTEGER PRIMARY KEY CHECK(id=1), " +
                    "image_uri TEXT, name TEXT, age INTEGER, " +
                    "height REAL, weight REAL, start_date TEXT, last_login TEXT)");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE swim_sessions ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            // Recreate profile table without CHECK(id=1) to support 2 profiles
            db.execSQL("CREATE TABLE IF NOT EXISTS profile_v2 (" +
                    "id INTEGER PRIMARY KEY, " +
                    "image_uri TEXT, name TEXT, age INTEGER, " +
                    "height REAL, weight REAL, start_date TEXT, last_login TEXT)");
            db.execSQL("INSERT OR IGNORE INTO profile_v2 SELECT id, image_uri, name, age, height, weight, start_date, last_login FROM profile");
            db.execSQL("DROP TABLE IF EXISTS profile");
            db.execSQL("ALTER TABLE profile_v2 RENAME TO profile");

            // Add profile_id to swim_sessions (existing sessions belong to profile 1)
            try {
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN profile_id INTEGER NOT NULL DEFAULT 1");
            } catch (Exception ignored) {
                // Column may already exist
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE profile ADD COLUMN pool_distance INTEGER NOT NULL DEFAULT 25");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN pool_distance INTEGER NOT NULL DEFAULT 25");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN im_ms INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN im_butterfly_ms INTEGER NOT NULL DEFAULT 0");
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN im_backstroke_ms INTEGER NOT NULL DEFAULT 0");
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN im_breaststroke_ms INTEGER NOT NULL DEFAULT 0");
                db.execSQL("ALTER TABLE swim_sessions ADD COLUMN im_freestyle_ms INTEGER NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
        }
    }
}
