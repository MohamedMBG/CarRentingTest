package com.example.carrentingtest.admin;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalRequest;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ActiveRentalAdapter extends RecyclerView.Adapter<ActiveRentalAdapter.ViewHolder> {

    public interface OnCallClientListener {
        void onCallClient(String phoneNumber);
    }

    private final List<RentalRequest> rentals;
    private final OnCallClientListener callListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    public ActiveRentalAdapter(List<RentalRequest> rentals, OnCallClientListener callListener) {
        this.rentals = rentals;
        this.callListener = callListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_rental, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalRequest request = rentals.get(position);

        holder.tvCarModel.setText(!TextUtils.isEmpty(request.getCarModel()) ? request.getCarModel() : "-");
        String clientName = !TextUtils.isEmpty(request.getUserName()) ? request.getUserName() : "-";
        holder.tvClientName.setText(holder.itemView.getContext().getString(R.string.name_label) + " " + clientName);

        String phone = request.getUserPhone();
        if (TextUtils.isEmpty(phone)) {
            holder.tvClientPhone.setText(holder.itemView.getContext().getString(R.string.phone_label) + " " + holder.itemView.getContext().getString(R.string.no_phone_available));
        } else {
            holder.tvClientPhone.setText(holder.itemView.getContext().getString(R.string.phone_label) + " " + phone);
        }

        String periodText = formatPeriod(request.getStartDate(), request.getEndDate(), holder);
        holder.tvRentalPeriod.setText(periodText);

        holder.tvElapsed.setText(holder.itemView.getContext().getString(R.string.elapsed_time_label) + ": " + formatDuration(request.getStartDate(), System.currentTimeMillis(), holder));
        holder.tvRemaining.setText(holder.itemView.getContext().getString(R.string.remaining_time_label) + ": " + formatRemaining(request.getEndDate(), System.currentTimeMillis(), holder));

        holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_label, request.getStatus() != null ? request.getStatus() : "-"));

        boolean canCall = !TextUtils.isEmpty(phone);
        holder.btnCallClient.setEnabled(canCall);
        holder.btnCallClient.setAlpha(canCall ? 1f : 0.6f);
        holder.btnCallClient.setOnClickListener(v -> {
            if (canCall && callListener != null) {
                callListener.onCallClient(phone);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rentals.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCarModel;
        final TextView tvClientName;
        final TextView tvClientPhone;
        final TextView tvRentalPeriod;
        final TextView tvElapsed;
        final TextView tvRemaining;
        final TextView tvStatus;
        final MaterialButton btnCallClient;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvActiveCarModel);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientPhone = itemView.findViewById(R.id.tvClientPhone);
            tvRentalPeriod = itemView.findViewById(R.id.tvRentalPeriod);
            tvElapsed = itemView.findViewById(R.id.tvElapsed);
            tvRemaining = itemView.findViewById(R.id.tvRemaining);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnCallClient = itemView.findViewById(R.id.btnCallClient);
        }
    }

    private String formatPeriod(Date startDate, Date endDate, ViewHolder holder) {
        String start = startDate != null ? dateFormat.format(startDate) : "-";
        String end = endDate != null ? dateFormat.format(endDate) : "-";
        return holder.itemView.getContext().getString(R.string.rental_period_format, start, end);
    }

    private String formatDuration(Date startDate, long now, ViewHolder holder) {
        if (startDate == null) {
            return "-";
        }
        long elapsedMillis = Math.max(0, now - startDate.getTime());
        return formatMillis(elapsedMillis, holder);
    }

    private String formatRemaining(Date endDate, long now, ViewHolder holder) {
        if (endDate == null) {
            return "-";
        }
        long remainingMillis = Math.max(0, endDate.getTime() - now);
        return formatMillis(remainingMillis, holder);
    }

    private String formatMillis(long millis, ViewHolder holder) {
        long totalHours = TimeUnit.MILLISECONDS.toHours(millis);
        if (totalHours <= 0) {
            return holder.itemView.getContext().getString(R.string.hours_suffix, 0);
        }
        long days = totalHours / 24;
        long hours = totalHours % 24;
        if (days > 0) {
            return holder.itemView.getContext().getString(R.string.days_hours_format, days, hours);
        }
        return holder.itemView.getContext().getString(R.string.hours_suffix, totalHours);
    }
}
