package com.dreamcreators.swimgoaltracker.adapter;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.pojo.TrackerPojo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrackerListAdapter extends RecyclerView.Adapter<TrackerListAdapter.ViewHolder> {
    private List<TrackerPojo> items;
    private Context context;
    private long minFree, minFly, minBreast, minBack;

    public interface OnItemClickListener {
        void onItemClick(String date);
    }

    public interface ScrollSyncCallback {
        void onScroll(int scrollX);
    }

    private OnItemClickListener listener;
    private ScrollSyncCallback scrollCallback;

    public TrackerListAdapter(Context context, List<TrackerPojo> items, long minFree, long minFly, long minBreast, long minBack, OnItemClickListener listener, ScrollSyncCallback scrollCallback) {
        this.context = context;
        this.items = items;
        this.minFree = minFree;
        this.minFly = minFly;
        this.minBreast = minBreast;
        this.minBack = minBack;
        this.listener = listener;
        this.scrollCallback = scrollCallback;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDay, tvFree, tvFly, tvBreast, tvBack;
        TextView badgeFree, badgeFly, badgeBreast, badgeBack;
        View layFree, layFly, layBreast, layBack;

        public ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvFree = itemView.findViewById(R.id.tv_free);
            tvFly = itemView.findViewById(R.id.tv_fly);
            tvBreast = itemView.findViewById(R.id.tv_breast);
            tvBack = itemView.findViewById(R.id.tv_back);

            badgeFree = itemView.findViewById(R.id.badge_free);
            badgeFly = itemView.findViewById(R.id.badge_fly);
            badgeBreast = itemView.findViewById(R.id.badge_breast);
            badgeBack = itemView.findViewById(R.id.badge_back);

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

        String displayDate = parseDate(log.date);
        holder.tvDate.setText(displayDate);

        String dayOfWeek = getDayOfWeek(log.date);
        holder.tvDay.setText(dayOfWeek);
        holder.tvDay.setVisibility(View.VISIBLE);

        bindStroke(holder.tvFree, holder.badgeFree, holder.layFree, log.freeTime, log.freeMs, minFree);
        bindStroke(holder.tvFly, holder.badgeFly, holder.layFly, log.flyTime, log.flyMs, minFly);
        bindStroke(holder.tvBreast, holder.badgeBreast, holder.layBreast, log.breastTime, log.breastMs, minBreast);
        bindStroke(holder.tvBack, holder.badgeBack, holder.layBack, log.backTime, log.backMs, minBack);

        boolean allZero = log.freeMs == 0 && log.flyMs == 0 && log.breastMs == 0 && log.backMs == 0;
        holder.itemView.setAlpha(allZero ? 0.6f : 1f);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(log.date);
            }
        });

        HorizontalScrollView rowScroll = holder.itemView.findViewById(R.id.rowScroll);
        rowScroll.scrollTo(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            rowScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollCallback != null) {
                    scrollCallback.onScroll(scrollX);
                }
            });
        }
    }

    private void bindStroke(TextView tvTime, TextView badge, View container, String timeStr, long ms, long minMs) {
        boolean hasValue = ms > 0;
        boolean isPb = hasValue && ms == minMs;

        tvTime.setText(timeStr);
        tvTime.setTextColor(context.getColor(hasValue ? R.color.dark_on_surface : R.color.dark_on_surface_variant));

        badge.setVisibility(isPb ? View.VISIBLE : View.GONE);

        container.setBackgroundResource(isPb ? R.drawable.item_bg_best : R.drawable.item_bg_rounded);
    }

    private String parseDate(String dateStr) {
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inFmt.parse(dateStr);
            SimpleDateFormat outFmt = new SimpleDateFormat("MMM dd", Locale.US);
            return outFmt.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private String getDayOfWeek(String dateStr) {
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inFmt.parse(dateStr);
            SimpleDateFormat dayFmt = new SimpleDateFormat("EEEE", Locale.US);
            return dayFmt.format(date).toUpperCase(Locale.US);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
