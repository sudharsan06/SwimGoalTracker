package com.dreamcreators.swimgoaltracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NutritionDbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "goaltracker.db";
    public static final int DB_VERSION = 2;

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

        db.execSQL("CREATE TABLE IF NOT EXISTS swim_sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT NOT NULL, " +
                "freestyle_ms INTEGER NOT NULL DEFAULT 0, " +
                "backstroke_ms INTEGER NOT NULL DEFAULT 0, " +
                "breaststroke_ms INTEGER NOT NULL DEFAULT 0, " +
                "butterfly_ms INTEGER NOT NULL DEFAULT 0" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS profile (" +
                "id INTEGER PRIMARY KEY CHECK(id=1), " +
                "image_uri TEXT, " +
                "name TEXT, " +
                "age INTEGER, " +
                "height REAL, " +
                "weight REAL, " +
                "start_date TEXT, " +
                "last_login TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS profile (" +
                    "id INTEGER PRIMARY KEY CHECK(id=1), " +
                    "image_uri TEXT, " +
                    "name TEXT, " +
                    "age INTEGER, " +
                    "height REAL, " +
                    "weight REAL, " +
                    "start_date TEXT, " +
                    "last_login TEXT" +
                    ")");
        }
    }
}


