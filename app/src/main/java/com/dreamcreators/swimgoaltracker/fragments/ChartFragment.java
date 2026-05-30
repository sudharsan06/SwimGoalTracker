package com.dreamcreators.swimgoaltracker.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.dreamcreators.swimgoaltracker.R;
import com.dreamcreators.swimgoaltracker.pojo.TrackerPojo;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ChartFragment extends Fragment {

    private BarChart barChart;
    private TextView tvBestFree, tvBestFly, tvBestBreast, tvBestBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barChart = view.findViewById(R.id.barChartTimings);
        tvBestFree = view.findViewById(R.id.tvBestFree);
        tvBestFly = view.findViewById(R.id.tvBestFly);
        tvBestBreast = view.findViewById(R.id.tvBestBreast);
        tvBestBack = view.findViewById(R.id.tvBestBack);
    }

    public void updateChart(List<TrackerPojo> items, long[] bestTimes) {
        if (!isAdded() || getView() == null) return;

        // Update personal best labels
        tvBestFree.setText(bestTimes[0] > 0 ? formatMs(bestTimes[0]) : "--:--");
        tvBestFly.setText(bestTimes[1] > 0 ? formatMs(bestTimes[1]) : "--:--");
        tvBestBreast.setText(bestTimes[2] > 0 ? formatMs(bestTimes[2]) : "--:--");
        tvBestBack.setText(bestTimes[3] > 0 ? formatMs(bestTimes[3]) : "--:--");

        // Setup bar chart
        if (items == null || items.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("No swim data yet");
            barChart.invalidate();
            return;
        }

        int count = Math.min(items.size(), 7);
        List<TrackerPojo> chartItems = new ArrayList<>(items.subList(0, count));
        Collections.reverse(chartItems);

        ArrayList<BarEntry> freeEntries = new ArrayList<>();
        ArrayList<BarEntry> flyEntries = new ArrayList<>();
        ArrayList<BarEntry> breastEntries = new ArrayList<>();
        ArrayList<BarEntry> backEntries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < chartItems.size(); i++) {
            TrackerPojo p = chartItems.get(i);
            freeEntries.add(new BarEntry(i, p.freeMs / 1000f));
            flyEntries.add(new BarEntry(i, p.flyMs / 1000f));
            breastEntries.add(new BarEntry(i, p.breastMs / 1000f));
            backEntries.add(new BarEntry(i, p.backMs / 1000f));

            String dateLabel = p.date;
            if (dateLabel != null && dateLabel.length() >= 10) {
                dateLabel = dateLabel.substring(5).replace("-", "/");
            }
            labels.add(dateLabel);
        }

        BarDataSet freeSet = new BarDataSet(freeEntries, "Free");
        freeSet.setColor(Color.parseColor("#0077B6"));

        BarDataSet flySet = new BarDataSet(flyEntries, "Fly");
        flySet.setColor(Color.parseColor("#FF6B6B"));

        BarDataSet breastSet = new BarDataSet(breastEntries, "Breast");
        breastSet.setColor(Color.parseColor("#FFD166"));

        BarDataSet backSet = new BarDataSet(backEntries, "Back");
        backSet.setColor(Color.parseColor("#06D6A0"));

        freeSet.setDrawValues(false);
        flySet.setDrawValues(false);
        breastSet.setDrawValues(false);
        backSet.setDrawValues(false);

        float groupSpace = 0.12f;
        float barSpace = 0.02f;
        float barWidth = 0.2f;

        BarData data = new BarData(freeSet, flySet, breastSet, backSet);
        data.setBarWidth(barWidth);

        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(chartItems.size());
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(10f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        barChart.getAxisRight().setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);

        barChart.setDescription(null);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setFitBars(false);
        barChart.setExtraBottomOffset(8f);

        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.animateY(800);
        barChart.invalidate();
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}
