package com.dreamcreators.swimgoaltracker.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.pojo.SwimEvent;

import java.util.List;

public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

    private List<SwimEvent> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(SwimEvent event);
    }

    public EventsAdapter(List<SwimEvent> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        SwimEvent e = events.get(position);
        holder.tvDateBadge.setText(e.getDate());
        holder.tvTitle.setText(e.getTitle());
        holder.tvDescription.setText(e.getDescription());
        holder.tvLocation.setText(e.getLocation());
        holder.tvTime.setText(e.getTime());
        holder.tvStrokes.setText(e.getStrokes());

        String title = e.getTitle() != null ? e.getTitle().trim() : "";
        String letter;
        if (title.isEmpty()) {
            letter = "?";
        } else if (Character.isDigit(title.charAt(0))) {
            int space = title.indexOf(' ');
            letter = (space > 0 ? title.substring(0, space) : title).toUpperCase();
        } else {
            letter = title.substring(0, 1).toUpperCase();
        }
        holder.ivEventLetter.setText(letter);

        int bg = AvatarColor.forTitle(title);
        holder.ivEventLetter.setBackgroundColor(bg);
        // Ensure the letter is always readable: use white on dark colors, dark on light.
        holder.ivEventLetter.setTextColor(
                ColorUtils.calculateLuminance(bg) > 0.5 ? Color.BLACK : Color.WHITE);
        holder.vEventAccent.setBackgroundColor(bg);

        holder.itemView.setOnClickListener(v -> listener.onEventClick(e));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateBadge, tvTitle, tvDescription, tvLocation, tvTime, tvStrokes;
        TextView ivEventLetter;
        View vEventAccent;

        ViewHolder(View v) {
            super(v);
            tvDateBadge = v.findViewById(R.id.tvDateBadge);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvLocation = v.findViewById(R.id.tvLocation);
            tvTime = v.findViewById(R.id.tvTime);
            tvStrokes = v.findViewById(R.id.tvStrokes);
            ivEventLetter = v.findViewById(R.id.ivEventLetter);
            vEventAccent = v.findViewById(R.id.vEventAccent);
        }
    }

    private static class AvatarColor {
        private static final int[] COLORS = {
                0xFF00796B, 0xFF00897B, 0xFF0277BD, 0xFF1565C0, 0xFF4527A0,
                0xFF6A1B9A, 0xFFAD1457, 0xFFC2185B, 0xFFD81B60, 0xFFE64A19,
                0xFF5D4037, 0xFF3949AB, 0xFF00838F, 0xFF2E7D32, 0xFFF4511E,
                0xFF8E24AA, 0xFF3949AB, 0xFF1E88E5, 0xFF0D47A1, 0xFF4A148C
        };

        static int forTitle(String title) {
            int hash = 0;
            for (int i = 0; i < title.length(); i++) {
                hash = (hash * 31) + title.charAt(i);
            }
            return COLORS[Math.abs(hash) % COLORS.length];
        }
    }
}
