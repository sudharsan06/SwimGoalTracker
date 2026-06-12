package com.dreamcreators.swimgoaltracker.screens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.dreamcreators.swimgoaltracker.R;

public class InstructionActivity extends AppCompatActivity {

    private int currentPage = 0;

    private TextView tvEmoji, tvStep, tvTitle, tvDesc;
    private View indicator1, indicator2, indicator3;
    private Button btnNext, btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(getColor(R.color.white));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        setContentView(R.layout.activity_instruction);

        tvEmoji = findViewById(R.id.tvIllustrationEmoji);
        tvStep = findViewById(R.id.tvStepNumber);
        tvTitle = findViewById(R.id.tvInstructionTitle);
        tvDesc = findViewById(R.id.tvInstructionDesc);

        indicator1 = findViewById(R.id.indicator1);
        indicator2 = findViewById(R.id.indicator2);
        indicator3 = findViewById(R.id.indicator3);

        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        btnNext.setOnClickListener(v -> {
            if (currentPage < 2) {
                currentPage++;
                updatePageUI();
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());

        updatePageUI();
    }

    private void updatePageUI() {
        switch (currentPage) {
            case 0:
                tvEmoji.setText("🏊");
                tvStep.setText("STEP 1 OF 3");
                tvTitle.setText("Track every lap, every day");
                tvDesc.setText("SwimminGO records your swim sessions automatically. Set personal goals and see how you improve over time.");
                
                indicator1.setBackgroundColor(0xFF1565C0);
                indicator2.setBackgroundColor(0xFFD1D9E6);
                indicator3.setBackgroundColor(0xFFD1D9E6);
                
                btnNext.setText("Next");
                btnSkip.setVisibility(View.VISIBLE);
                break;
            case 1:
                tvEmoji.setText("🎯");
                tvStep.setText("STEP 2 OF 3");
                tvTitle.setText("Set goals, stay motivated");
                tvDesc.setText("Create personalised swimming goals. Get notified with smart reminders to keep your training on track.");
                
                indicator1.setBackgroundColor(0xFF1565C0);
                indicator2.setBackgroundColor(0xFF1565C0);
                indicator3.setBackgroundColor(0xFFD1D9E6);
                
                btnNext.setText("Next");
                btnSkip.setVisibility(View.VISIBLE);
                break;
            case 2:
                tvEmoji.setText("🧑‍🧑‍🧒");
                tvStep.setText("STEP 3 OF 3");
                tvTitle.setText("Switch between swimmers easily");
                tvDesc.setText("One device supports up to 2 swimmer profiles. Each swimmer keeps their own records and progress history.");
                
                indicator1.setBackgroundColor(0xFF1565C0);
                indicator2.setBackgroundColor(0xFF1565C0);
                indicator3.setBackgroundColor(0xFF1565C0);
                
                btnNext.setText("Get Started");
                btnSkip.setVisibility(View.GONE);
                break;
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        prefs.edit().putBoolean("is_first_launch", false).apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 0) {
            currentPage--;
            updatePageUI();
        } else {
            super.onBackPressed();
        }
    }
}
