package com.dreamcreators.swimgoaltracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TrackerListAdapter extends RecyclerView.Adapter<TrackerListAdapter.ViewHolder> {
    private List<TrackerPojo> items;
    private Context context;

    public TrackerListAdapter(Context contex, List<TrackerPojo> items) {
        this.context = context;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView swimmerImage;
        TextView logDate, nutritionStats, activityStats;


        public ViewHolder(View itemView) {
            super(itemView);

           // swimmerImage = itemView.findViewById(R.id.swimmerImage);
            logDate = itemView.findViewById(R.id.tv_log_date);
            nutritionStats = itemView.findViewById(R.id.tv_nutrition_info);
            activityStats = itemView.findViewById(R.id.tv_swim_info);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_swim_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      //  holder.textView.setText(items.get(position));
        TrackerPojo log = items.get(position);
        //holder.swimmerImage.setImageResource(R.drawable.swimmer); // Replace with dynamic image if needed
        holder.logDate.setText("📅 " + log.date);
        holder.nutritionStats.setText(log.nutrition);
        holder.activityStats.setText(log.swimActivity);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}