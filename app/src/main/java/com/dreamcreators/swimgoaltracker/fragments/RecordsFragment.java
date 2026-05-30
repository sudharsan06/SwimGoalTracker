package com.dreamcreators.swimgoaltracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dreamcreators.swimgoaltracker.utility.DailyAttemptsDialog;
import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.adapter.TrackerListAdapter;
import com.dreamcreators.swimgoaltracker.pojo.TrackerPojo;
import com.dreamcreators.swimgoaltracker.screens.TrackerActivity;

import java.util.List;

public class RecordsFragment extends Fragment {

    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.listTracker);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    public void updateRecords(List<TrackerPojo> items, long[] bestTimes) {
        if (!isAdded() || getView() == null || recyclerView == null) return;

        TrackerActivity activity = (TrackerActivity) getActivity();
        if (activity == null) return;

        TrackerListAdapter adapter = new TrackerListAdapter(
                requireContext(), items,
                bestTimes[0], bestTimes[1], bestTimes[2], bestTimes[3],
                date -> {
                    DailyAttemptsDialog.newInstance(date).show(activity.getSupportFragmentManager(), "daily_attempts");
                }
        );
        recyclerView.setAdapter(adapter);
    }
}
