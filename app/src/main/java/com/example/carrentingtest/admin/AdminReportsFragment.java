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
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.pricing.PricingService;
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
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminReportsFragment extends Fragment {

    private BarChart barChart;
    private TextView tvTotal;
    private TextView tvUniqueModels;
    private TextView tvTopCar;
    private TextView tvEmptyChart;
    private TextView tvUtilization;
    private TextView tvUtilizationDetail;
    private TextView tvRevenue;
    private TextView tvRevenuePerCar;
    private TextView tvRevenuePerCarDetail;
    private TextView tvPendingPayments;
    private TextView tvPendingPaymentsDetail;
    private TextView tvLateReturns;
    private TextView tvMaintenanceDowntime;
    private TextView tvMaintenanceDetail;
    private FirebaseFirestore db;
    private String companyId;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currencyFormat.setCurrency(java.util.Currency.getInstance(PricingService.DEFAULT_CURRENCY));
        currencyFormat.setMaximumFractionDigits(0);

        barChart = view.findViewById(R.id.barChart);
        tvTotal = view.findViewById(R.id.tvTotalRentals);
        tvUniqueModels = view.findViewById(R.id.tvUniqueModels);
        tvTopCar = view.findViewById(R.id.tvTopCar);
        tvEmptyChart = view.findViewById(R.id.tvEmptyChart);
        tvUtilization = view.findViewById(R.id.tvUtilization);
        tvUtilizationDetail = view.findViewById(R.id.tvUtilizationDetail);
        tvRevenue = view.findViewById(R.id.tvRevenue);
        tvRevenuePerCar = view.findViewById(R.id.tvRevenuePerCar);
        tvRevenuePerCarDetail = view.findViewById(R.id.tvRevenuePerCarDetail);
        tvPendingPayments = view.findViewById(R.id.tvPendingPayments);
        tvPendingPaymentsDetail = view.findViewById(R.id.tvPendingPaymentsDetail);
        tvLateReturns = view.findViewById(R.id.tvLateReturns);
        tvMaintenanceDowntime = view.findViewById(R.id.tvMaintenanceDowntime);
        tvMaintenanceDetail = view.findViewById(R.id.tvMaintenanceDetail);
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
                    loadCars();
                })
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadCars() {
        if (TextUtils.isEmpty(companyId)) {
            showEmptyState();
            return;
        }

        db.collection("cars")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(cars -> loadRentals(cars))
                .addOnFailureListener(e -> showEmptyState());
    }

    private void loadRentals(@NonNull QuerySnapshot carsSnapshot) {
        db.collection("rental_requests")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(rentalsSnapshot -> bindMetrics(carsSnapshot, rentalsSnapshot))
                .addOnFailureListener(e -> showEmptyState());
    }

    private void bindMetrics(@NonNull QuerySnapshot carsSnapshot, @NonNull QuerySnapshot rentalsSnapshot) {
        ReportMetrics metrics = calculateMetrics(carsSnapshot, rentalsSnapshot);

        tvTotal.setText(String.valueOf(metrics.recognizedRentalCount));
        tvUniqueModels.setText(String.valueOf(metrics.bookedModels.size()));
        tvRevenue.setText(formatMoney(metrics.totalRevenue));
        tvRevenuePerCar.setText(formatMoney(metrics.averageRevenuePerCar));
        tvRevenuePerCarDetail.setText(getString(
                R.string.admin_reports_revenue_per_car_detail,
                metrics.fleetSize));
        tvPendingPayments.setText(String.valueOf(metrics.pendingPaymentsCount));
        tvPendingPaymentsDetail.setText(getString(
                R.string.admin_reports_pending_payments_detail,
                formatMoney(metrics.pendingPaymentsValue)));
        tvLateReturns.setText(String.valueOf(metrics.lateReturnsCount));
        tvMaintenanceDowntime.setText(String.valueOf(metrics.maintenanceCount));
        tvMaintenanceDetail.setText(getString(
                R.string.admin_reports_maintenance_detail,
                metrics.maintenanceCount,
                formatMoney(metrics.maintenanceDailyRevenueOffline)));

        int utilizationPercent = metrics.rentableFleetSize == 0
                ? 0
                : Math.round((metrics.activeCarIds.size() * 100f) / metrics.rentableFleetSize);
        tvUtilization.setText(utilizationPercent + "%");
        tvUtilizationDetail.setText(getString(
                R.string.admin_reports_active_utilization,
                metrics.activeCarIds.size(),
                metrics.rentableFleetSize));

        bindTopCar(metrics);
        populateChart(metrics.revenueByCarLabel);
    }

    private ReportMetrics calculateMetrics(@NonNull QuerySnapshot carsSnapshot,
                                           @NonNull QuerySnapshot rentalsSnapshot) {
        ReportMetrics metrics = new ReportMetrics();
        Map<String, Car> carsById = new HashMap<>();
        long now = System.currentTimeMillis();

        for (QueryDocumentSnapshot doc : carsSnapshot) {
            Car car = doc.toObject(Car.class);
            if (car == null) {
                continue;
            }
            car.setDocumentId(doc.getId());
            carsById.put(doc.getId(), car);
            metrics.fleetSize++;
            if (car.isMaintenance()) {
                metrics.maintenanceCount++;
                metrics.maintenanceDailyRevenueOffline += PricingService.sanitizeAmount(car.getPricePerDay());
            }
        }
        metrics.rentableFleetSize = Math.max(0, metrics.fleetSize - metrics.maintenanceCount);

        for (QueryDocumentSnapshot doc : rentalsSnapshot) {
            RentalRequest request = doc.toObject(RentalRequest.class);
            if (request == null) {
                continue;
            }

            RentalRequestStatus status = RentalRequestStatus.from(request.getStatus());
            boolean recognized = status.isRevenueRecognized();
            boolean approved = status == RentalRequestStatus.APPROVED;
            Date startDate = request.getStartDate();
            Date endDate = request.getEndDate();

            if (approved && startDate != null && endDate != null
                    && startDate.getTime() <= now && endDate.getTime() >= now) {
                metrics.activeCarIds.add(request.getCarId());
            }
            if (approved && endDate != null && endDate.getTime() < now) {
                metrics.lateReturnsCount++;
            }
            if (!recognized) {
                continue;
            }

            metrics.recognizedRentalCount++;
            String label = resolveCarLabel(request, carsById);
            metrics.bookedModels.add(label);
            metrics.bookingCountByCarLabel.put(
                    label,
                    metrics.bookingCountByCarLabel.getOrDefault(label, 0) + 1);

            double revenue = PricingService.getStoredTotal(request);
            metrics.totalRevenue += revenue;
            metrics.revenueByCarLabel.put(
                    label,
                    metrics.revenueByCarLabel.getOrDefault(label, 0d) + revenue);

            if (!request.isPaymentProofProvided() && revenue > 0d) {
                metrics.pendingPaymentsCount++;
                metrics.pendingPaymentsValue += revenue;
            }
        }

        metrics.totalRevenue = PricingService.sanitizeAmount(metrics.totalRevenue);
        metrics.pendingPaymentsValue = PricingService.sanitizeAmount(metrics.pendingPaymentsValue);
        metrics.maintenanceDailyRevenueOffline = PricingService.sanitizeAmount(metrics.maintenanceDailyRevenueOffline);
        metrics.averageRevenuePerCar = metrics.fleetSize == 0
                ? 0d
                : PricingService.sanitizeAmount(metrics.totalRevenue / metrics.fleetSize);
        return metrics;
    }

    private String resolveCarLabel(@NonNull RentalRequest request, @NonNull Map<String, Car> carsById) {
        if (!TextUtils.isEmpty(request.getCarModel())) {
            return request.getCarModel();
        }
        Car car = carsById.get(request.getCarId());
        if (car != null && !TextUtils.isEmpty(car.getModel())) {
            return car.getModel();
        }
        return getString(R.string.active_rental_unknown_car);
    }

    private void bindTopCar(@NonNull ReportMetrics metrics) {
        if (metrics.bookingCountByCarLabel.isEmpty()) {
            tvTopCar.setVisibility(View.GONE);
            return;
        }
        Map.Entry<String, Integer> top = Collections.max(
                metrics.bookingCountByCarLabel.entrySet(),
                Comparator.comparingInt(Map.Entry::getValue));
        tvTopCar.setVisibility(View.VISIBLE);
        tvTopCar.setText(getString(R.string.admin_top_rented_car, top.getKey(), top.getValue()));
    }

    private void configureChart() {
        barChart.setNoDataText("");
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setExtraOffsets(6f, 6f, 6f, 12f);

        Legend legend = barChart.getLegend();
        legend.setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setAxisLineColor(ContextCompat.getColor(requireContext(), R.color.homeCardStroke));
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
        xAxis.setTextSize(11f);
        xAxis.setLabelRotationAngle(0f);
        xAxis.setAvoidFirstLastClipping(true);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(ContextCompat.getColor(requireContext(), R.color.homeCardStroke));
        leftAxis.setAxisLineColor(ContextCompat.getColor(requireContext(), R.color.homeCardStroke));
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColorSecondary));
        leftAxis.setTextSize(11f);
        leftAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return compactMoney(value);
            }
        });

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

    private void populateChart(Map<String, Double> revenueByCar) {
        if (revenueByCar == null || revenueByCar.isEmpty()) {
            showChartEmpty();
            return;
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(revenueByCar.entrySet());
        Collections.sort(sorted, (first, second) -> Double.compare(second.getValue(), first.getValue()));

        int maxItems = Math.min(6, sorted.size());
        List<BarEntry> entries = new ArrayList<>(maxItems);
        List<String> labels = new ArrayList<>(maxItems);
        for (int i = 0; i < maxItems; i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(shortenLabel(entry.getKey()));
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.admin_reports_chart_title));
        dataSet.setColors(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                ContextCompat.getColor(requireContext(), R.color.badgeRoundedBackground),
                ContextCompat.getColor(requireContext(), R.color.colorSecondary),
                ContextCompat.getColor(requireContext(), R.color.badgeLuxuryEnd),
                ContextCompat.getColor(requireContext(), R.color.homeHighlightGradientEnd)
        );
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.textColorPrimary));
        dataSet.setHighLightColor(ContextCompat.getColor(requireContext(), R.color.colorSecondary));
        dataSet.setDrawIcons(false);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return compactMoney(value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.56f);

        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.getXAxis().setLabelCount(labels.size(), true);
        barChart.setData(data);
        barChart.setFitBars(true);
        barChart.animateY(1000, Easing.EaseOutCubic);
        barChart.invalidate();

        tvEmptyChart.setVisibility(View.GONE);
        barChart.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        tvTotal.setText("0");
        tvUniqueModels.setText("0");
        tvUtilization.setText("0%");
        tvUtilizationDetail.setText(getString(R.string.admin_reports_active_utilization, 0, 0));
        tvRevenue.setText(formatMoney(0d));
        tvRevenuePerCar.setText(formatMoney(0d));
        tvRevenuePerCarDetail.setText(getString(R.string.admin_reports_revenue_per_car_detail, 0));
        tvPendingPayments.setText("0");
        tvPendingPaymentsDetail.setText(getString(R.string.admin_reports_pending_payments_detail, formatMoney(0d)));
        tvLateReturns.setText("0");
        tvMaintenanceDowntime.setText("0");
        tvMaintenanceDetail.setText(getString(R.string.admin_reports_maintenance_detail, 0, formatMoney(0d)));
        tvTopCar.setVisibility(View.GONE);
        showChartEmpty();
    }

    private void showChartEmpty() {
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
        return trimmed.substring(0, 11) + "...";
    }

    private String formatMoney(double value) {
        return currencyFormat.format(PricingService.sanitizeAmount(value));
    }

    private String compactMoney(float value) {
        if (value >= 1000f) {
            return Math.round(value / 1000f) + "k";
        }
        return String.valueOf(Math.round(value));
    }

    private static class ReportMetrics {
        int fleetSize;
        int rentableFleetSize;
        int maintenanceCount;
        int recognizedRentalCount;
        int pendingPaymentsCount;
        int lateReturnsCount;
        double totalRevenue;
        double averageRevenuePerCar;
        double pendingPaymentsValue;
        double maintenanceDailyRevenueOffline;
        final Set<String> activeCarIds = new HashSet<>();
        final Set<String> bookedModels = new HashSet<>();
        final Map<String, Integer> bookingCountByCarLabel = new HashMap<>();
        final Map<String, Double> revenueByCarLabel = new HashMap<>();
    }
}
