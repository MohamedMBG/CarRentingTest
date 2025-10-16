package com.example.carrentingtest.adapters;

import android.content.Context;
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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PastRentalAdapter extends RecyclerView.Adapter<PastRentalAdapter.ViewHolder> {

    public interface InvoiceListener {
        void onInvoice(@NonNull RentalRequest request);
    }

    private final List<RentalRequest> rentals;
    private final InvoiceListener invoiceListener;
    private final LayoutInflater inflater;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private final NumberFormat amountFormat = NumberFormat.getNumberInstance(Locale.getDefault());

    public PastRentalAdapter(@NonNull Context context,
                             @NonNull List<RentalRequest> rentals,
                             InvoiceListener invoiceListener) {
        this.rentals = rentals;
        this.invoiceListener = invoiceListener;
        this.inflater = LayoutInflater.from(context);
        amountFormat.setMinimumFractionDigits(2);
        amountFormat.setMaximumFractionDigits(2);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_past_rental, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RentalRequest request = rentals.get(position);
        Context context = holder.itemView.getContext();

        String carModel = !TextUtils.isEmpty(request.getCarModel())
                ? request.getCarModel()
                : context.getString(R.string.pos_unknown_car);
        holder.tvCarModel.setText(carModel);

        holder.tvClientName.setText(
                context.getString(R.string.pos_client_label, safeText(context, request.getUserName()))
        );

        String phone = request.getUserPhone();
        String phoneDisplay = TextUtils.isEmpty(phone)
                ? context.getString(R.string.pos_phone_label, context.getString(R.string.pos_value_placeholder))
                : context.getString(R.string.pos_phone_label, phone);
        holder.tvClientPhone.setText(phoneDisplay);

        holder.tvRentalPeriod.setText(
                context.getString(
                        R.string.pos_period_label,
                        formatDate(request.getStartDate(), context),
                        formatDate(request.getEndDate(), context))
        );

        int days = computeRentalDays(request.getStartDate(), request.getEndDate());
        holder.tvDuration.setText(
                context.getString(R.string.past_rental_duration_label, Math.max(1, days))
        );

        holder.tvCompletedOn.setText(
                context.getString(
                        R.string.past_rental_completed_on,
                        formatDate(request.getCompletedAt(), context))
        );

        double total = request.getTotalPrice();
        if (Double.isNaN(total) || Double.isInfinite(total) || total < 0) {
            total = 0d;
        }
        holder.tvTotal.setText(
                context.getString(R.string.past_rental_total_label, formatAmount(total))
        );

        String status = request.getStatus();
        if (TextUtils.isEmpty(status)) {
            status = context.getString(R.string.pos_unknown_status);
        }
        holder.tvStatus.setText(context.getString(R.string.status_label, status));

        holder.btnInvoice.setOnClickListener(v -> {
            if (invoiceListener != null) {
                invoiceListener.onInvoice(request);
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
        final TextView tvDuration;
        final TextView tvCompletedOn;
        final TextView tvTotal;
        final TextView tvStatus;
        final MaterialButton btnInvoice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvCarModel);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvClientPhone = itemView.findViewById(R.id.tvClientPhone);
            tvRentalPeriod = itemView.findViewById(R.id.tvRentalPeriod);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvCompletedOn = itemView.findViewById(R.id.tvCompletedOn);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnInvoice = itemView.findViewById(R.id.btnInvoice);
        }
    }

    private String safeText(Context context, String value) {
        if (TextUtils.isEmpty(value)) {
            return context.getString(R.string.pos_value_placeholder);
        }
        return value;
    }

    private String formatDate(Date date, Context context) {
        if (date == null) {
            return context.getString(R.string.pos_value_placeholder);
        }
        return dateFormat.format(date);
    }

    private String formatAmount(double amount) {
        return amountFormat.format(amount) + " dhs";
    }

    private int computeRentalDays(Date start, Date end) {
        if (start == null) {
            return 0;
        }
        long endMillis = end != null ? end.getTime() : start.getTime();
        long diff = Math.max(0, endMillis - start.getTime());
        long days = TimeUnit.MILLISECONDS.toDays(diff);
        if (diff % TimeUnit.DAYS.toMillis(1) != 0) {
            days++;
        }
        return (int) Math.max(1, days);
    }
}