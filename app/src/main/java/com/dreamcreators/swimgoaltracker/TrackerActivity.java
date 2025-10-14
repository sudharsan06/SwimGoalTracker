package com.dreamcreators.swimgoaltracker;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrackerActivity extends AppCompatActivity {

    private NutritionDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracker);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTitle("Date wise tracker");

        // Setup toolbar
        /*Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }*/

        RecyclerView recyclerView = findViewById(R.id.listTracker);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        dbHelper = new NutritionDbHelper(this);

        List<TrackerPojo> items = loadLast15Days();
        TrackerListAdapter adapter = new TrackerListAdapter(this, items);
        //list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items));
        recyclerView.setAdapter(adapter);

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /*private List<String> loadLast15Days() {
        List<TrackerPojo> result = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        Date today = new Date();
        cal.setTime(today);

        for (int i = 0; i < 15; i++) {
            String date = fmt.format(cal.getTime());
            int calories = 0, protein = 0, carbs = 0, fats = 0;
            Cursor c1 = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT calories, protein, carbs, fats FROM daily_nutrition WHERE date = ?",
                    new String[]{date}
            );
            if (c1.moveToFirst()) {
                calories = c1.getInt(0);
                protein = c1.getInt(1);
                carbs = c1.getInt(2);
                fats = c1.getInt(3);
            }
            c1.close();

            long freeMs = 0, backMs = 0, breastMs = 0, flyMs = 0;
            Cursor c2 = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT SUM(freestyle_ms), SUM(backstroke_ms), SUM(breaststroke_ms), SUM(butterfly_ms) FROM swim_sessions WHERE date = ?",
                    new String[]{date}
            );
            if (c2.moveToFirst()) {
                freeMs = c2.isNull(0) ? 0 : c2.getLong(0);
                backMs = c2.isNull(1) ? 0 : c2.getLong(1);
                breastMs = c2.isNull(2) ? 0 : c2.getLong(2);
                flyMs = c2.isNull(3) ? 0 : c2.getLong(3);
            }
            c2.close();

            String nutrition = "Cals:" + calories + ", P:" + protein + ", C:" + carbs + ", F:" + fats;

            String item =  "Free:" + formatMs(freeMs) + ", Back:" + formatMs(backMs) + ", Breast:" + formatMs(breastMs) + ", Fly:" + formatMs(flyMs);
            result.add(date,nutrition, item);

            //cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return result;
    }*/

    private List<TrackerPojo> loadLast15Days() {
        List<TrackerPojo> result = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 15; i++) {
            String date = fmt.format(cal.getTime());

            // Nutrition query
            int calories = 0, protein = 0, carbs = 0, fats = 0;
            Cursor c1 = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT SUM(calories), SUM(protein), SUM(carbs), SUM(fats) FROM daily_nutrition WHERE date = ?",
                    new String[]{date}
            );
            if (c1.moveToFirst()) {
                calories = c1.getInt(0);
                protein = c1.getInt(1);
                carbs = c1.getInt(2);
                fats = c1.getInt(3);
            }
            c1.close();

            // Swim session query
            long freeMs = 0, backMs = 0, breastMs = 0, flyMs = 0;
            Cursor c2 = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT MIN(freestyle_ms), MIN(backstroke_ms), MIN(breaststroke_ms), MIN(butterfly_ms) FROM swim_sessions WHERE date = ?",
                    new String[]{date}
            );
            if (c2.moveToFirst()) {
                freeMs = c2.isNull(0) ? 0 : c2.getLong(0);
                backMs = c2.isNull(1) ? 0 : c2.getLong(1);
                breastMs = c2.isNull(2) ? 0 : c2.getLong(2);
                flyMs = c2.isNull(3) ? 0 : c2.getLong(3);
            }
            c2.close();

            String nutrition = "Cals:" + calories + ", P:" + protein + ", C:" + carbs + ", F:" + fats;
            String activity = "Free:" + formatMs(freeMs) + ", Back:" + formatMs(backMs) +
                    ", Breast:" + formatMs(breastMs) + ", Fly:" + formatMs(flyMs);

            result.add(new TrackerPojo(date, nutrition, activity));

            cal.add(Calendar.DAY_OF_YEAR, -1); // Move to previous day
        }

        return result;
    }


    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (ms % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }
}


