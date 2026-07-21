package com.dreamcreators.swimgoaltracker.screens;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.adapter.EventsAdapter;
import com.dreamcreators.swimgoaltracker.pojo.SwimEvent;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class EventsActivity extends AppCompatActivity {

    private static final String EVENTS_URL = "https://swimmingo-default-rtdb.firebaseio.com/events.json";

    private RecyclerView rvEvents;
    private EventsAdapter adapter;
    private List<SwimEvent> eventList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvStatus;
    private SwipeRefreshLayout swipeRefresh;

    private static final long AUTO_REFRESH_INTERVAL_MS = 60_000;
    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = this::fetchLiveEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.dark_surface_low));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(!ThemeManager.isDarkMode(this));
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_events);

        View headerContent = findViewById(R.id.headerContent);
        ViewCompat.setOnApplyWindowInsetsListener(headerContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topPadding = systemBars.top + (int)(16 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), topPadding, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        rvEvents = findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(this));
        rvEvents.setHasFixedSize(true);

        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setColorSchemeResources(R.color.dark_primary_fixed_dim);
        swipeRefresh.setOnRefreshListener(() -> fetchLiveEvents(true));

        adapter = new EventsAdapter(eventList, event -> {
            Intent intent = new Intent(this, EventDetailActivity.class);
            intent.putExtra("title", event.getTitle());
            intent.putExtra("date", event.getDate());
            intent.putExtra("location", event.getLocation());
            intent.putExtra("time", event.getTime());
            intent.putExtra("strokes", event.getStrokes());
            intent.putExtra("description", event.getDescription());
            intent.putExtra("registrationUrl", event.getRegistrationUrl());
            startActivity(intent);
        });
        rvEvents.setAdapter(adapter);

        loadLocalEvents();
        fetchLiveEvents();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        bottomNav.setSelectedItemId(R.id.nav_events);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                return true;
            } else if (id == R.id.nav_tracker) {
                startActivity(new Intent(this, TrackerActivity.class));
                return true;
            } else if (id == R.id.nav_goals) {
                startActivity(new Intent(this, GoalsActivity.class)
                        .putExtra("highlight_weekly", true));
                return true;
            } else if (id == R.id.nav_events) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        AdView adView = findViewById(R.id.adView);
        if (adView != null) {
            adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.e("EventsActivity", "Ad failed: " + loadAdError.getCode() + " " + loadAdError.getMessage());
                }
            });
            MobileAds.initialize(this, initializationStatus -> {
                adView.loadAd(new AdRequest.Builder().build());
            });
        }
    }

    private void loadLocalEvents() {
        try {
            InputStream is = getAssets().open("events.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            JSONObject root = new JSONObject(sb.toString());
            JSONArray arr = root.getJSONArray("events");
            eventList.clear();
            for (int i = 0; i < arr.length(); i++) {
                eventList.add(SwimEvent.fromJson(arr.getJSONObject(i)));
            }
            adapter.notifyDataSetChanged();
        } catch (IOException | JSONException e) {
            Log.e("EventsActivity", "Failed to load local events", e);
        }
    }

    private void fetchLiveEvents() {
        fetchLiveEvents(false);
    }

    private void fetchLiveEvents(boolean fromSwipe) {
        if (!isNetworkAvailable()) {
            swipeRefresh.setRefreshing(false);
            if (eventList.isEmpty()) {
                showStatus("No internet connection. Showing saved events.");
            }
            return;
        }

        if (!fromSwipe && eventList.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tvStatus.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                String json = downloadJson(EVENTS_URL);
                List<SwimEvent> liveEvents = parseEvents(json);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    eventList.clear();
                    eventList.addAll(liveEvents);
                    adapter.notifyDataSetChanged();
                    if (liveEvents.isEmpty()) {
                        showStatus("No events available right now.");
                    } else {
                        tvStatus.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e("EventsActivity", "Failed to load live events", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (eventList.isEmpty()) {
                        showStatus("Couldn't reach the live feed. Showing saved events.");
                    } else {
                        tvStatus.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private String downloadJson(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private List<SwimEvent> parseEvents(String json) throws JSONException {
        List<SwimEvent> list = new ArrayList<>();
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.startsWith("[")) {
            addFromArray(new JSONArray(trimmed), list);
        } else {
            JSONObject root = new JSONObject(trimmed);
            if (root.has("events") && root.get("events") instanceof JSONArray) {
                addFromArray(root.getJSONArray("events"), list);
            } else {
                Iterator<String> keys = root.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = root.get(key);
                    if (value instanceof JSONObject) {
                        list.add(SwimEvent.fromJson((JSONObject) value));
                    }
                }
            }
        }
        list.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<SwimEvent> visible = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (SwimEvent e : list) {
            if (e.isPublished() || isScheduledNow(e, now)) {
                visible.add(e);
            }
        }
        return visible;
    }

    private void addFromArray(JSONArray arr, List<SwimEvent> list) throws JSONException {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj = arr.optJSONObject(i);
            if (obj != null) {
                list.add(SwimEvent.fromJson(obj));
            }
        }
    }

    private boolean isScheduledNow(SwimEvent e, long now) {
        if (e.getPublishAt() == null || e.getPublishAt().isEmpty()) {
            return false;
        }
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.parse(e.getPublishAt()).getTime() <= now;
        } catch (java.text.ParseException ex) {
            return false;
        }
    }

    private void showStatus(String message) {
        tvStatus.setText(message);
        tvStatus.setVisibility(View.VISIBLE);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchLiveEvents();
        autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL_MS);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_events);
    }

    @Override
    protected void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
