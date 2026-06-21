package com.dreamcreators.swimgoaltracker.fragments;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
    private LinearLayout legendFree, legendFly, legendBreast, legendBack;

    // Cached data for re-rendering on filter toggle
    private List<TrackerPojo> cachedItems;
    private long[] cachedBestTimes;

    // Currently active filter: -1 = all styles, 0=Free, 1=Fly, 2=Breast, 3=Back
    private int activeFilter = -1;

    // Auto-restore handler
    private final Handler restoreHandler = new Handler(Looper.getMainLooper());
    private static final long RESTORE_DELAY_MS = 5000; // 5 seconds
    private final Runnable restoreRunnable = () -> {
        if (activeFilter != -1) {
            activeFilter = -1;
            updateLegendHighlights();
            renderChart();
        }
    };

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

        legendFree = view.findViewById(R.id.legendFree);
        legendFly = view.findViewById(R.id.legendFly);
        legendBreast = view.findViewById(R.id.legendBreast);
        legendBack = view.findViewById(R.id.legendBack);

        // Set click listeners on each legend item
        legendFree.setOnClickListener(v -> onLegendTapped(0));
        legendFly.setOnClickListener(v -> onLegendTapped(1));
        legendBreast.setOnClickListener(v -> onLegendTapped(2));
        legendBack.setOnClickListener(v -> onLegendTapped(3));
    }

    /**
     * Called when user taps a legend item.
     * If the same style is already active, reset to all styles.
     * Otherwise, filter to only show that style.
     */
    private void onLegendTapped(int styleIndex) {
        restoreHandler.removeCallbacks(restoreRunnable);

        if (activeFilter == styleIndex) {
            // Toggle off — restore all styles
            activeFilter = -1;
        } else {
            // Filter to only this style
            activeFilter = styleIndex;
            // Schedule auto-restore after inactivity
            restoreHandler.postDelayed(restoreRunnable, RESTORE_DELAY_MS);
        }

        updateLegendHighlights();
        renderChart();
    }

    /**
     * Updates the visual state of the legend items based on activeFilter.
     */
    private void updateLegendHighlights() {
        LinearLayout[] legends = {legendFree, legendFly, legendBreast, legendBack};
        int[] colors = {
            Color.parseColor("#0077B6"), // Free
            Color.parseColor("#FF6B6B"), // Fly
            Color.parseColor("#FFD166"), // Breast
            Color.parseColor("#06D6A0")  // Back
        };

        for (int i = 0; i < legends.length; i++) {
            if (legends[i] == null) continue;

            if (activeFilter == -1) {
                // All styles active — normal appearance
                legends[i].setAlpha(1.0f);
                legends[i].setBackground(null);
            } else if (activeFilter == i) {
                // This style is selected — highlight with border
                legends[i].setAlpha(1.0f);
                GradientDrawable border = new GradientDrawable();
                border.setCornerRadius(12f);
                border.setStroke(3, colors[i]);
                border.setColor(Color.TRANSPARENT);
                legends[i].setBackground(border);
            } else {
                // Other styles — dim them
                legends[i].setAlpha(0.35f);
                legends[i].setBackground(null);
            }
        }
    }

    public void updateChart(List<TrackerPojo> items, long[] bestTimes) {
        if (!isAdded() || getView() == null) return;

        // Cache data for re-rendering when filter changes
        this.cachedItems = items;
        this.cachedBestTimes = bestTimes;

        // Reset filter when new data arrives
        activeFilter = -1;
        restoreHandler.removeCallbacks(restoreRunnable);
        updateLegendHighlights();

        // Update personal best labels
        tvBestFree.setText(bestTimes[0] > 0 ? formatMs(bestTimes[0]) : "--:--");
        tvBestFly.setText(bestTimes[1] > 0 ? formatMs(bestTimes[1]) : "--:--");
        tvBestBreast.setText(bestTimes[2] > 0 ? formatMs(bestTimes[2]) : "--:--");
        tvBestBack.setText(bestTimes[3] > 0 ? formatMs(bestTimes[3]) : "--:--");

        renderChart();
    }

    /**
     * Renders the bar chart based on cachedItems and the current activeFilter.
     */
    private void renderChart() {
        if (!isAdded() || getView() == null) return;
        if (cachedItems == null || cachedItems.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("No swim data yet");
            barChart.setNoDataTextColor(requireContext().getColor(R.color.dark_on_surface_variant));
            barChart.invalidate();
            return;
        }

        int count = Math.min(cachedItems.size(), 7);
        List<TrackerPojo> chartItems = new ArrayList<>(cachedItems.subList(0, count));
        Collections.reverse(chartItems);

        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < chartItems.size(); i++) {
            String dateLabel = chartItems.get(i).date;
            if (dateLabel != null && dateLabel.length() >= 10) {
                dateLabel = dateLabel.substring(5).replace("-", "/");
            }
            labels.add(dateLabel);
        }

        if (activeFilter == -1) {
            // Show all 4 styles as grouped bars
            renderAllStyles(chartItems, labels);
        } else {
            // Show only the selected style as single bars
            renderSingleStyle(chartItems, labels, activeFilter);
        }
    }

    /**
     * Renders all four swim styles as grouped bars.
     */
    private void renderAllStyles(List<TrackerPojo> chartItems, ArrayList<String> labels) {
        ArrayList<BarEntry> freeEntries = new ArrayList<>();
        ArrayList<BarEntry> flyEntries = new ArrayList<>();
        ArrayList<BarEntry> breastEntries = new ArrayList<>();
        ArrayList<BarEntry> backEntries = new ArrayList<>();

        for (int i = 0; i < chartItems.size(); i++) {
            TrackerPojo p = chartItems.get(i);
            freeEntries.add(new BarEntry(i, p.freeMs / 1000f));
            flyEntries.add(new BarEntry(i, p.flyMs / 1000f));
            breastEntries.add(new BarEntry(i, p.breastMs / 1000f));
            backEntries.add(new BarEntry(i, p.backMs / 1000f));
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

        float groupSpace = 0.20f;
        float barSpace = 0.04f;
        float barWidth = 0.16f;

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

        configureCommonChartSettings();

        barChart.setFitBars(false);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.animateY(600);
        barChart.invalidate();
    }

    /**
     * Renders only a single swim style as individual bars.
     */
    private void renderSingleStyle(List<TrackerPojo> chartItems, ArrayList<String> labels, int styleIndex) {
        String[] styleNames = {"Free", "Fly", "Breast", "Back"};
        String[] styleColors = {"#0077B6", "#FF6B6B", "#FFD166", "#06D6A0"};

        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < chartItems.size(); i++) {
            TrackerPojo p = chartItems.get(i);
            float value;
            switch (styleIndex) {
                case 0: value = p.freeMs / 1000f; break;
                case 1: value = p.flyMs / 1000f; break;
                case 2: value = p.breastMs / 1000f; break;
                case 3: value = p.backMs / 1000f; break;
                default: value = 0; break;
            }
            entries.add(new BarEntry(i, value));
        }

        BarDataSet dataSet = new BarDataSet(entries, styleNames[styleIndex]);
        dataSet.setColor(Color.parseColor(styleColors[styleIndex]));
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(requireContext().getColor(R.color.dark_on_surface_variant));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);

        barChart.setData(data);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(false);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(chartItems.size() - 0.5f);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);

        configureCommonChartSettings();

        barChart.setFitBars(true);
        barChart.animateY(600);
        barChart.invalidate();
    }

    /**
     * Configures chart settings common to both all-styles and single-style views.
     */
    private void configureCommonChartSettings() {
        int textColor = requireContext().getColor(R.color.dark_on_surface_variant);
        int gridColor = requireContext().getColor(R.color.dark_outline_variant);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(textColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(gridColor);
        barChart.getAxisRight().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextColor(textColor);

        Legend legend = barChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(10f);
        legend.setTextColor(textColor);

        barChart.setDescription(null);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setExtraBottomOffset(8f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        restoreHandler.removeCallbacks(restoreRunnable);
    }

    private String formatMs(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}
