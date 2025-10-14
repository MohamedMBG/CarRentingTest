package com.example.carrentingtest.admin;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalReport;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ClientReportsAdapter extends RecyclerView.Adapter<ClientReportsAdapter.ViewHolder> {

    public interface OnCallClientListener {
        void onCall(String phoneNumber);
    }

    private final List<RentalReport> reports;
    private final OnCallClientListener callClientListener;
    private final SimpleDateFormat createdDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat periodFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    public ClientReportsAdapter(List<RentalReport> reports, OnCallClientListener callClientListener) {
        this.reports = reports;
        this.callClientListener = callClientListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_client_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalReport report = reports.get(position);
        holder.tvCar.setText(!TextUtils.isEmpty(report.getCarModel()) ? report.getCarModel() : "-");
        String clientName = !TextUtils.isEmpty(report.getUserName()) ? report.getUserName() : "-";
        holder.tvClient.setText(holder.itemView.getContext().getString(R.string.name_label) + " " + clientName);

        String phone = report.getUserPhone();
        if (TextUtils.isEmpty(phone)) {
            holder.tvPhone.setText(holder.itemView.getContext().getString(R.string.phone_label) + " " + holder.itemView.getContext().getString(R.string.no_phone_available));
        } else {
            holder.tvPhone.setText(holder.itemView.getContext().getString(R.string.phone_label) + " " + phone);
        }

        holder.tvCreated.setText(holder.itemView.getContext().getString(R.string.report_created_on, formatTimestamp(report.getCreatedAt())));
        holder.tvPeriod.setText(formatPeriod(report.getStartDate(), report.getEndDate(), holder));
        holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_label, !TextUtils.isEmpty(report.getStatus()) ? report.getStatus() : "-"));
        holder.tvDescription.setText(!TextUtils.isEmpty(report.getDescription()) ? report.getDescription() : "-");

        boolean canCall = !TextUtils.isEmpty(phone);
        holder.btnCall.setEnabled(canCall);
        holder.btnCall.setAlpha(canCall ? 1f : 0.6f);
        holder.btnCall.setOnClickListener(v -> {
            if (canCall && callClientListener != null) {
                callClientListener.onCall(phone);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCar;
        final TextView tvClient;
        final TextView tvPhone;
        final TextView tvCreated;
        final TextView tvPeriod;
        final TextView tvStatus;
        final TextView tvDescription;
        final MaterialButton btnCall;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCar = itemView.findViewById(R.id.tvReportCar);
            tvClient = itemView.findViewById(R.id.tvReportClient);
            tvPhone = itemView.findViewById(R.id.tvReportPhone);
            tvCreated = itemView.findViewById(R.id.tvReportCreated);
            tvPeriod = itemView.findViewById(R.id.tvReportPeriod);
            tvStatus = itemView.findViewById(R.id.tvReportStatus);
            tvDescription = itemView.findViewById(R.id.tvReportDescription);
            btnCall = itemView.findViewById(R.id.btnCallFromReport);
        }
    }

    private String formatTimestamp(Timestamp timestamp) {
        Date date = timestamp != null ? timestamp.toDate() : null;
        return date != null ? createdDateFormat.format(date) : "-";
    }

    private String formatPeriod(Date start, Date end, ViewHolder holder) {
        String startText = start != null ? periodFormat.format(start) : "-";
        String endText = end != null ? periodFormat.format(end) : "-";
        return holder.itemView.getContext().getString(R.string.rental_period_format, startText, endText);
    }
}
