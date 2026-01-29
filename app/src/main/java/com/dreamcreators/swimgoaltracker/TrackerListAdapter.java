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
    private long minFree, minFly, minBreast, minBack;

    public TrackerListAdapter(Context context, List<TrackerPojo> items, long minFree, long minFly, long minBreast, long minBack) {
        this.context = context;
        this.items = items;
        this.minFree = minFree;
        this.minFly = minFly;
        this.minBreast = minBreast;
        this.minBack = minBack;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvFree, tvFly, tvBreast, tvBack;
        View layFree, layFly, layBreast, layBack;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvFree = itemView.findViewById(R.id.tv_free);
            tvFly = itemView.findViewById(R.id.tv_fly);
            tvBreast = itemView.findViewById(R.id.tv_breast);
            tvBack = itemView.findViewById(R.id.tv_back);
            
            layFree = itemView.findViewById(R.id.lay_free_container);
            layFly = itemView.findViewById(R.id.lay_fly_container);
            layBreast = itemView.findViewById(R.id.lay_breast_container);
            layBack = itemView.findViewById(R.id.lay_back_container);
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
        holder.layFree.setBackgroundResource(log.freeMs > 0 && log.freeMs == minFree ? R.drawable.item_bg_best : R.drawable.item_bg_rounded);
        
        holder.tvFly.setText(log.flyTime);
        holder.layFly.setBackgroundResource(log.flyMs > 0 && log.flyMs == minFly ? R.drawable.item_bg_best : R.drawable.item_bg_rounded);
        
        holder.tvBreast.setText(log.breastTime);
        holder.layBreast.setBackgroundResource(log.breastMs > 0 && log.breastMs == minBreast ? R.drawable.item_bg_best : R.drawable.item_bg_rounded);
        
        holder.tvBack.setText(log.backTime);
        holder.layBack.setBackgroundResource(log.backMs > 0 && log.backMs == minBack ? R.drawable.item_bg_best : R.drawable.item_bg_rounded);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}