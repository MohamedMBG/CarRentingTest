package com.example.carrentingtest.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminReportsFragment extends Fragment {

    private BarChart barChart;
    private TextView tvTotal;
    private TextView tvTopCar;
    private TextView tvEmptyChart;
    private FirebaseFirestore db;
    private String companyId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barChart = view.findViewById(R.id.barChart);
        tvTotal = view.findViewById(R.id.tvTotalRentals);
        tvTopCar = view.findViewById(R.id.tvTopCar);
        tvEmptyChart = view.findViewById(R.id.tvEmptyChart);
        db = FirebaseFirestore.getInstance();

        configureChart();
        fetchCompanyAndLoadData();
    }

    private void fetchCompanyAndLoadData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            showEmptyState();
            return;
        }

        db.collection("users")
                .document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    companyId = doc.getString("companyId");
                    loadData();
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadData() {
        if (TextUtils.isEmpty(companyId)) {
            showEmptyState();
            return;
        }

        db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, Integer> counts = new HashMap<>();
                    int total = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        RentalRequestStatus status = RentalRequestStatus.from(doc.getString("status"));
                        if (!status.isRevenueRecognized()) {
                            continue;
                        }
                        total++;
                        String carModel = doc.getString("carModel");
                        if (!TextUtils.isEmpty(carModel)) {
                            counts.put(carModel, counts.getOrDefault(carModel, 0) + 1);
                        }
                    }

                    tvTotal.setText(getString(R.string.total_rentals, total));
                    populateChart(counts);
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void configureChart() {
        barChart.setNoDataText("");
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setExtraOffsets(8f, 6f, 8f, 10f);

        Legend legend = barChart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
        xAxis.setTextSize(11f);
        xAxis.setLabelRotationAngle(-12f);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.homeCardStroke));
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
        leftAxis.setGranularity(1f);
        leftAxis.setTextSize(11f);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void populateChart(Map<String, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            showEmptyState();
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        Collections.sort(sorted, Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed());

        int maxItems = Math.min(7, sorted.size());
        List<BarEntry> entries = new ArrayList<>(maxItems);
        List<String> labels = new ArrayList<>(maxItems);
        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, Integer> entry = sorted.get(i);
            entries.add(new BarEntry(i, entry.getValue()));
            labels.add(shortenLabel(entry.getKey()));
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.most_rented_cars));
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                ContextCompat.getColor(requireContext(), R.color.colorSecondary),
                ContextCompat.getColor(requireContext(), R.color.badgeRoundedBackground),
                ContextCompat.getColor(requireContext(), R.color.badgeLuxuryEnd),
                ContextCompat.getColor(requireContext(), R.color.homeHighlightGradientEnd)
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.textColorPrimary));
        dataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
        dataSet.setDrawIcons(false);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.62f);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size(), true);
        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.animateY(1100, Easing.EaseOutCubic);
        barChart.invalidate();

        tvTopCar.setVisibility(View.VISIBLE);
        tvTopCar.setText(getString(R.string.admin_top_rented_car, sorted.get(0).getKey(), sorted.get(0).getValue()));
        tvEmptyChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        tvTotal.setText(getString(R.string.total_rentals, 0));
        tvTopCar.setVisibility(View.GONE);
        tvEmptyChart.setVisibility(View.VISIBLE);
        barChart.clear();
        barChart.setVisibility(View.INVISIBLE);
    }

    private String shortenLabel(String label) {
        if (label == null) {
            return "";
        }
        String trimmed = label.trim();
        if (trimmed.length() <= 12) {
            return trimmed;
        }
        return trimmed.substring(0, 11) + "…";
    }
}
