package com.dreamcreators.swimgoaltracker;

import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DailyAttemptsDialog extends DialogFragment {

    private static final String ARG_DATE = "arg_date";

    public static DailyAttemptsDialog newInstance(String date) {
        DailyAttemptsDialog fragment = new DailyAttemptsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_DATE, date);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_daily_attempts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String date = getArguments() != null ? getArguments().getString(ARG_DATE) : "";

        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        if (tvTitle != null) {
            tvTitle.setText(date + " Attempts");
        }

        View rootLayout = view.findViewById(R.id.dialog_root);
        if (rootLayout != null) {
            rootLayout.setOnClickListener(v -> dismiss());
        }

        View cardView = view.findViewById(R.id.dialog_card);
        if (cardView != null) {
            cardView.setOnClickListener(v -> {}); // prevent propagation
        }

        View btnClose = view.findViewById(R.id.btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        RecyclerView rvAttempts = view.findViewById(R.id.rv_attempts);
        rvAttempts.setLayoutManager(new LinearLayoutManager(getContext()));

        NutritionDbHelper dbHelper = new NutritionDbHelper(getContext());
        List<TrackerPojo> attempts = new ArrayList<>();
        
        long minFree = Long.MAX_VALUE, minFly = Long.MAX_VALUE, minBreast = Long.MAX_VALUE, minBack = Long.MAX_VALUE;

        Cursor c = dbHelper.getReadableDatabase().rawQuery(
                "SELECT freestyle_ms, backstroke_ms, breaststroke_ms, butterfly_ms, created_at FROM swim_sessions WHERE date = ? ORDER BY id ASC",
                new String[]{date}
        );

        if (c.moveToFirst()) {
            int attemptCount = 1;
            do {
                long freeMs = c.isNull(0) ? 0 : c.getLong(0);
                long backMs = c.isNull(1) ? 0 : c.getLong(1);
                long breastMs = c.isNull(2) ? 0 : c.getLong(2);
                long flyMs = c.isNull(3) ? 0 : c.getLong(3);
                long createdAt = c.isNull(4) ? 0 : c.getLong(4);

                if (freeMs > 0 && freeMs < minFree) minFree = freeMs;
                if (flyMs > 0 && flyMs < minFly) minFly = flyMs;
                if (breastMs > 0 && breastMs < minBreast) minBreast = breastMs;
                if (backMs > 0 && backMs < minBack) minBack = backMs;

                TrackerPojo pojo = new TrackerPojo(
                        date,
                        formatMs(freeMs),
                        formatMs(flyMs),
                        formatMs(breastMs),
                        formatMs(backMs),
                        freeMs, flyMs, breastMs, backMs
                );

                if (createdAt > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    pojo.swimActivity = sdf.format(new Date(createdAt));
                } else {
                    pojo.swimActivity = "Attempt " + attemptCount;
                }

                attempts.add(pojo);
                attemptCount++;
            } while (c.moveToNext());
        }
        c.close();
        dbHelper.close();

        long[] finalMins = new long[]{
            minFree == Long.MAX_VALUE ? 0 : minFree,
            minFly == Long.MAX_VALUE ? 0 : minFly,
            minBreast == Long.MAX_VALUE ? 0 : minBreast,
            minBack == Long.MAX_VALUE ? 0 : minBack
        };

        DailyAttemptsAdapter adapter = new DailyAttemptsAdapter(getContext(), attempts, finalMins[0], finalMins[1], finalMins[2], finalMins[3]);
        rvAttempts.setAdapter(adapter);
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}
