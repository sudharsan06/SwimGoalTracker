package com.dreamcreators.swimgoaltracker.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.dreamcreators.swimgoaltracker.fragments.ChartFragment;
import com.dreamcreators.swimgoaltracker.fragments.RecordsFragment;

public class TrackerPagerAdapter extends FragmentStateAdapter {

    private final ChartFragment chartFragment = new ChartFragment();
    private final RecordsFragment recordsFragment = new RecordsFragment();

    public TrackerPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return chartFragment;
        } else {
            return recordsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public ChartFragment getChartFragment() {
        return chartFragment;
    }

    public RecordsFragment getRecordsFragment() {
        return recordsFragment;
    }
}
