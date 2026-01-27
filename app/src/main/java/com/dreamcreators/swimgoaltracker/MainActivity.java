package com.dreamcreators.swimgoaltracker;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static String userName = "";

    private TextView tvDate;
    private TextView tvCalories;
    private TextView tvProtein;
    private TextView tvCarbs;
    private TextView tvFats, tvUserName;
    private LinearLayout lay_freeStyle, lay_backStroke, lay_breastStroke, lay_butterFly;
    private NutritionDbHelper dbHelper;
    private TextView tvBestFree, tvBestBack, tvBestBreast, tvBestFly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View topBanner = findViewById(R.id.top_banner);
       // View bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(topBanner, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });



        tvDate = findViewById(R.id.tvDate);
        tvCalories = findViewById(R.id.tvCalories);
        tvProtein = findViewById(R.id.tvProtein);
        tvCarbs = findViewById(R.id.tvCarbs);
        tvFats = findViewById(R.id.tvFats);
        tvBestFree = findViewById(R.id.tvBestFree);
        tvBestBack = findViewById(R.id.tvBestBack);
        tvBestBreast = findViewById(R.id.tvBestBreast);
        tvBestFly = findViewById(R.id.tvBestFly);
        tvUserName = findViewById(R.id.tvUserName);
        lay_freeStyle = findViewById(R.id.lay_freeStyle);
       /* lay_backStroke = findViewById(R.id.lay_backStroke);
        lay_breastStroke = findViewById(R.id.lay_breastStroke);
        lay_butterFly = findViewById(R.id.lay_butterFly);

        lay_freeStyle.setOnClickListener(openSwimStopwatch);
        lay_backStroke.setOnClickListener(openSwimStopwatch);
        lay_breastStroke.setOnClickListener(openSwimStopwatch);
        lay_butterFly.setOnClickListener(openSwimStopwatch);*/


        View.OnClickListener openSwimStopwatch = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SwimStopwatchActivity.class);

                int viewId = view.getId();
                String stroke; // Declare a variable to hold the stroke value

                if (viewId == R.id.lay_freeStyle) {
                    stroke = "Freestyle";
                } else if (viewId == R.id.lay_backStroke) {
                    stroke = "Backstroke";
                } else if (viewId == R.id.lay_breastStroke) {
                    stroke = "Breaststroke";
                } else if (viewId == R.id.lay_butterFly) {
                    stroke = "Butterfly";
                } else {
                    // Optional: handle an unexpected view click
                    return;
                }

                intent.putExtra("stroke", stroke);
                startActivity(intent);
            }
        };
        findViewById(R.id.lay_freeStyle).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_backStroke).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_breastStroke).setOnClickListener(openSwimStopwatch);
        findViewById(R.id.lay_butterFly).setOnClickListener(openSwimStopwatch);


        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        findViewById(R.id.btnOpenProfile).setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        MobileAds.initialize(this, initializationStatus -> {
        });
        AdView adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());

        dbHelper = new NutritionDbHelper(this);
        loadUserData();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText("Today: " + today);
        loadTodayNutrition(today);

        findViewById(R.id.layNutritionSummary).setVisibility(View.GONE);


        tvUserName.setText(getGreetingMessage(userName));


        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                // Already on Dashboard
                return true;
            } else if (item.getItemId() == R.id.nav_tracker) {
                startActivity(new Intent(this, TrackerActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        Cursor c = dbHelper.getReadableDatabase().rawQuery("SELECT image_uri, name, age, height, weight, start_date, last_login FROM profile WHERE id=1", null);
        if (c.moveToFirst()) {
            String uriStr = c.getString(0);
            userName = c.getString(1);
        }
        c.close();
    }

  /*  private View.OnClickListener openSwimStopwatch = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, SwimStopwatchActivity.class);
            switch (view.getId()) {
                case R.id.lay_freeStyle:
                    intent.putExtra("stroke", "Freestyle");
                    break;
                case R.id.lay_backStroke:
                    intent.putExtra("stroke", "Backstroke");
                    break;
                case R.id.lay_breastStroke:
                    intent.putExtra("stroke", "Breaststroke");
                    break;
                case R.id.lay_butterFly:
                    intent.putExtra("stroke", "Butterfly");
                    break;
            }
            startActivity(intent);
        }
    };*/

    private void loadTodayNutrition(String date) {
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT calories, protein, carbs, fats FROM daily_nutrition WHERE date = ?",
                new String[]{date}
        );
        if (cursor.moveToFirst()) {
            tvCalories.setText(cursor.getInt(0) + " kcal");
            tvProtein.setText(cursor.getInt(1) + " g");
            tvCarbs.setText(cursor.getInt(2) + " g");
            tvFats.setText(cursor.getInt(3) + " g");
        } else {
            tvCalories.setText("0 kcal");
            tvProtein.setText("0 g");
            tvCarbs.setText("0 g");
            tvFats.setText("0 g");
        }
        cursor.close();
    }

    private String getGreetingMessage(String userName) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;

        if (hour >= 5 && hour < 12) {
            greeting = "Good Morning";
        } else if (hour >= 12 && hour < 17) {
            greeting = "Good Afternoon";
        } else if (hour >= 17 && hour < 21) {
            greeting = "Good Evening";
        } else {
            greeting = "Hello";
        }
        if (userName == null || userName.isEmpty()) {
            userName = "Swimmer";
        }

        return greeting + ", " + userName + "!";
    }

    @Override
    protected void onResume() {
        super.onResume();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        loadTodayNutrition(today);
        loadBestSwimToday(today);
    }

    private void loadBestSwimToday(String date) {
        // Use NULLIF to convert 0 to NULL, so MIN() ignores zeros and finds the best (lowest) non-zero time
        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT MIN(NULLIF(freestyle_ms, 0)), MIN(NULLIF(backstroke_ms, 0)), MIN(NULLIF(breaststroke_ms, 0)), MIN(NULLIF(butterfly_ms, 0)) FROM swim_sessions WHERE date = ?",
                new String[]{date}
        );
        if (c.moveToFirst()) {
            tvBestFree.setText(formatMs(c.isNull(0) ? 0 : c.getLong(0)));
            tvBestBack.setText(formatMs(c.isNull(1) ? 0 : c.getLong(1)));
            tvBestBreast.setText(formatMs(c.isNull(2) ? 0 : c.getLong(2)));
            tvBestFly.setText(formatMs(c.isNull(3) ? 0 : c.getLong(3)));
        }
        c.close();
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (ms % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }
}

