package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.utility.ThemeManager;
import com.google.android.material.button.MaterialButton;

public class EventDetailActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_event_detail);

        View headerContent = findViewById(R.id.headerContent);
        ViewCompat.setOnApplyWindowInsetsListener(headerContent, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int topPadding = systemBars.top + (int)(14 * getResources().getDisplayMetrics().density);
            v.setPadding(v.getPaddingLeft(), topPadding, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String date = intent.getStringExtra("date");
        String location = intent.getStringExtra("location");
        String time = intent.getStringExtra("time");
        String strokes = intent.getStringExtra("strokes");
        String description = intent.getStringExtra("description");
        String registrationUrl = intent.getStringExtra("registrationUrl");

        TextView tvDate = findViewById(R.id.tvDetailDate);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvLocation = findViewById(R.id.tvDetailLocation);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        TextView tvStrokes = findViewById(R.id.tvDetailStrokes);
        TextView tvDescription = findViewById(R.id.tvDetailDescription);
        TextView tvLetter = findViewById(R.id.ivDetailLetter);
        View vAccent = findViewById(R.id.vDetailAccent);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);
        TextView btnBack = findViewById(R.id.btnBack);

        tvDate.setText(date);
        tvTitle.setText(title);
        tvLocation.setText(location);
        tvTime.setText(time);
        tvStrokes.setText(strokes);
        tvDescription.setText(description);

        String safeTitle = title != null ? title.trim() : "";
        String letter = safeTitle.isEmpty() ? "?" : safeTitle.substring(0, 1).toUpperCase();
        tvLetter.setText(letter);
        int bg = avatarColor(safeTitle);
        tvLetter.setBackgroundColor(bg);
        tvLetter.setTextColor(ColorUtils.calculateLuminance(bg) > 0.5 ? Color.BLACK : Color.WHITE);
        vAccent.setBackgroundColor(bg);

        if (registrationUrl != null && !registrationUrl.isEmpty()) {
            btnRegister.setVisibility(View.VISIBLE);
        } else {
            btnRegister.setVisibility(View.GONE);
        }

        String finalRegUrl = registrationUrl;
        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            if (finalRegUrl != null && !finalRegUrl.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(finalRegUrl)));
            }
        });
    }

    private static int avatarColor(String title) {
        final int[] COLORS = {
                0xFF00796B, 0xFF00897B, 0xFF0277BD, 0xFF1565C0, 0xFF4527A0,
                0xFF6A1B9A, 0xFFAD1457, 0xFFC2185B, 0xFFD81B60, 0xFFE64A19,
                0xFF5D4037, 0xFF3949AB, 0xFF00838F, 0xFF2E7D32, 0xFFF4511E,
                0xFF8E24AA, 0xFF3949AB, 0xFF1E88E5, 0xFF0D47A1, 0xFF4A148C
        };
        int hash = 0;
        for (int i = 0; i < title.length(); i++) {
            hash = (hash * 31) + title.charAt(i);
        }
        return COLORS[Math.abs(hash) % COLORS.length];
    }
}
