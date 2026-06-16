package com.dreamcreators.swimgoaltracker.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.pojo.SwimTimingEntry;

import java.util.List;
import java.util.Locale;

public class SwimTimingAdapter extends RecyclerView.Adapter<SwimTimingAdapter.ViewHolder> {
    private List<SwimTimingEntry> entries;
    private Context context;
    private long bestTime; // Best (lowest) time to highlight
    private long nowMs;    // Reference time to compute "X mins ago"
    private long goalTime; // Goal time to determine if goal is met
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(SwimTimingEntry entry);
    }

    public interface OnItemClickListener {
        void onItemClick(SwimTimingEntry entry);
    }

    private OnItemClickListener itemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public SwimTimingAdapter(Context context, List<SwimTimingEntry> entries, long bestTime, long nowMs, long goalTime, OnDeleteClickListener deleteClickListener) {
        this.context = context;
        this.entries = entries;
        this.bestTime = bestTime;
        this.nowMs = nowMs;
        this.goalTime = goalTime;
        this.deleteClickListener = deleteClickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEntryNumber;
        TextView tvTiming;
        TextView tvBestLabel;
        TextView tvGoalLabel;
        View btnDelete;
        View itemContainer;

        public ViewHolder(View itemView) {
            super(itemView);
            tvEntryNumber = itemView.findViewById(R.id.tvEntryNumber);
            tvTiming = itemView.findViewById(R.id.tvTiming);
            tvBestLabel = itemView.findViewById(R.id.tvBestLabel);
            tvGoalLabel = itemView.findViewById(R.id.tvGoalLabel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            itemContainer = itemView.findViewById(R.id.itemContainer);
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
        String todayDate = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        if (entry.getDate() != null && !entry.getDate().equals(todayDate)) {
            holder.tvEntryNumber.setText(entry.getDate() + " - " + timeAgo);
        } else {
            holder.tvEntryNumber.setText("Entry #" + attemptNumber + " - " + timeAgo);
        }
        
        // Format and display timing
        String formattedTime = formatMs(entry.getTimeMs());
        holder.tvTiming.setText(formattedTime);
        
        // Highlight best time
        if (entry.getTimeMs() == bestTime && bestTime > 0) {
            holder.tvBestLabel.setVisibility(View.VISIBLE);
            // Elegant highlighted state for BEST entry
            holder.itemContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.watercolor_gradient));
            holder.tvTiming.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvEntryNumber.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvBestLabel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#A2E535")));
            holder.tvBestLabel.setTextColor(android.graphics.Color.BLACK);
            ((android.widget.ImageView)holder.btnDelete).setColorFilter(ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvBestLabel.setVisibility(View.GONE);
            holder.tvTiming.setTextColor(ContextCompat.getColor(context, R.color.lightPrimary));
            holder.itemContainer.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_glass_card));
            holder.tvEntryNumber.setTextColor(ContextCompat.getColor(context, R.color.steel_blue));
            holder.tvBestLabel.setBackgroundTintList(null);
            ((android.widget.ImageView)holder.btnDelete).setColorFilter(ContextCompat.getColor(context, R.color.midnight_blue));
        }

        // Show Goal Met badge
        if (goalTime > 0 && entry.getTimeMs() <= goalTime) {
            holder.tvGoalLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvGoalLabel.setVisibility(View.GONE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(entry);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) {
                itemClickListener.onItemClick(entry);
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
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    private String formatTimeAgo(long createdAtMs, long nowMs) {
        if (createdAtMs <= 0 || nowMs <= 0) return "";
        long diffMs = nowMs - createdAtMs;
        if (diffMs < 0) diffMs = 0;

        long totalSeconds = diffMs / 1000;
        long totalMinutes = totalSeconds / 60;
        long totalHours = totalMinutes / 60;
        long days = totalHours / 24;
        long hours = totalHours % 24;
        long minutes = totalMinutes % 60;
        long seconds = totalSeconds % 60;

        // Latest / very recent
        if (totalSeconds < 5) {
            return "Just now";
        }

        // Show days and hours for >= 1 day
        if (days > 0) {
            String dayText = days == 1 ? "1 day" : days + " days";
            if (hours == 0) {
                return dayText + " ago";
            } else {
                String hourText = hours == 1 ? "1 hour" : hours + " hours";
                return dayText + " and " + hourText + " ago";
            }
        }

        // Show hours and minutes for >= 1 hour
        if (totalHours > 0) {
            String hourText = totalHours == 1 ? "1 hour" : totalHours + " hours";
            if (minutes == 0) {
                return hourText + " ago";
            } else {
                String minText = minutes == 1 ? "1 min" : minutes + " mins";
                return hourText + " " + minText + " ago";
            }
        }

        // Show seconds for < 1 minute
        if (totalMinutes == 0) {
            if (seconds == 1) {
                return "1 sec ago";
            } else {
                return seconds + " secs ago";
            }
        }

        // Show minutes for >= 1 minute and < 1 hour
        if (totalMinutes == 1) {
            return "1 min ago";
        } else {
            return totalMinutes + " mins ago";
        }
    }
}
