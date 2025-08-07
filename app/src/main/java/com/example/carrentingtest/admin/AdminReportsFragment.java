package com.example.carrentingtest.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.carrentingtest.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.components.XAxis;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminReportsFragment extends Fragment {

    private BarChart barChart;
    private TextView tvTotal;
    private FirebaseFirestore db;
    private String companyId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barChart = view.findViewById(R.id.barChart);
        tvTotal = view.findViewById(R.id.tvTotalRentals);
        db = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        companyId = doc.getString("companyId");
                        loadData();
                    });
        }
    }

    private void loadData() {
        if (companyId == null) return;
        db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, Integer> counts = new HashMap<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        String carModel = doc.getString("carModel");
                        if (carModel != null) {
                            counts.put(carModel, counts.getOrDefault(carModel, 0) + 1);
                        }
                    }
                    populateChart(counts);
                    tvTotal.setText(getString(R.string.total_rentals, snap.size()));
                });
    }

    private void populateChart(Map<String, Integer> counts) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            entries.add(new BarEntry(index, e.getValue()));
            labels.add(e.getKey());
            index++;
        }

        BarDataSet set = new BarDataSet(entries, getString(R.string.most_rented_cars));
        set.setColor(ContextCompat.getColor(requireContext(), R.color.palette_teal));
        List<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set);
        BarData data = new BarData(dataSets);
        data.setBarWidth(0.9f);
        data.setDrawValues(false);

        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.getAxisRight().setEnabled(false);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.invalidate();
    }
}