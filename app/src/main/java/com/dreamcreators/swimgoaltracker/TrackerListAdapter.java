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

    public TrackerListAdapter(Context context, List<TrackerPojo> items) {
        this.context = context;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvFree, tvFly, tvBreast, tvBack;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvFree = itemView.findViewById(R.id.tv_free);
            tvFly = itemView.findViewById(R.id.tv_fly);
            tvBreast = itemView.findViewById(R.id.tv_breast);
            tvBack = itemView.findViewById(R.id.tv_back);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_tracker_table, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TrackerPojo log = items.get(position);
        holder.tvDate.setText(log.date);
        holder.tvFree.setText(log.freeTime);
        holder.tvFly.setText(log.flyTime);
        holder.tvBreast.setText(log.breastTime);
        holder.tvBack.setText(log.backTime);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}