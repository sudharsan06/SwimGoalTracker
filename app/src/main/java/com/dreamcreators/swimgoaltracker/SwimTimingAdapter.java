package com.dreamcreators.swimgoaltracker;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class SwimTimingAdapter extends RecyclerView.Adapter<SwimTimingAdapter.ViewHolder> {
    private List<SwimTimingEntry> entries;
    private Context context;
    private long bestTime; // Best (lowest) time to highlight
    private long nowMs;    // Reference time to compute "X mins ago"
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(SwimTimingEntry entry);
    }

    public SwimTimingAdapter(Context context, List<SwimTimingEntry> entries, long bestTime, long nowMs, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.entries = entries;
        this.bestTime = bestTime;
        this.nowMs = nowMs;
        this.deleteClickListener = deleteClickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntryNumber;
        TextView tvTiming;
        TextView tvBestLabel;
        View btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvEntryNumber = itemView.findViewById(R.id.tvEntryNumber);
            tvTiming = itemView.findViewById(R.id.tvTiming);
            tvBestLabel = itemView.findViewById(R.id.tvBestLabel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_swim_timing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SwimTimingEntry entry = entries.get(position);

        // Entry number based on attempt count (latest entry has highest number)
        int attemptNumber = entries.size() - position;
        String timeAgo = formatTimeAgo(entry.getCreatedAtMs(), nowMs);
        holder.tvEntryNumber.setText("Entry #" + attemptNumber + " " + timeAgo);
        
        // Format and display timing
        String formattedTime = formatMs(entry.getTimeMs());
        holder.tvTiming.setText(formattedTime);
        
        // Highlight best time
        if (entry.getTimeMs() == bestTime && bestTime > 0) {
            holder.tvBestLabel.setVisibility(View.VISIBLE);
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_primary));
            holder.tvTiming.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvEntryNumber.setTextColor(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvBestLabel.setVisibility(View.GONE);
            Drawable bg = ContextCompat.getDrawable(context, R.drawable.edt_bg);
            if (bg != null) {
                holder.itemView.setBackground(bg);
            }
            holder.tvTiming.setTextColor(ContextCompat.getColor(context, R.color.teal_primary));
            holder.tvEntryNumber.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(entry);
            }
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long tenths = (ms % 1000) / 100;
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, tenths);
    }

    private String formatTimeAgo(long createdAtMs, long nowMs) {
        if (createdAtMs <= 0 || nowMs <= 0) return "";
        long diffMs = nowMs - createdAtMs;
        if (diffMs < 0) diffMs = 0;

        long totalSeconds = diffMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        // Latest / very recent
        if (totalSeconds < 5) {
            return "Just now";
        }

        // Show seconds for < 1 minute
        if (minutes == 0) {
            if (seconds == 1) {
                return "At 1 sec ago";
            } else {
                return "At " + seconds + " secs ago";
            }
        }

        // Show minutes for >= 1 minute
        if (minutes == 1) {
            return "At 1 min ago";
        } else {
            return "At " + minutes + " mins ago";
        }
    }
}
