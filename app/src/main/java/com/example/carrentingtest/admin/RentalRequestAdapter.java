package com.example.carrentingtest.admin;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.pricing.PricingService;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class RentalRequestAdapter extends RecyclerView.Adapter<RentalRequestAdapter.ViewHolder> {
    private List<RentalRequest> requests;
    private BiConsumer<RentalRequest, Boolean> onDecision;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    public RentalRequestAdapter(List<RentalRequest> requests, BiConsumer<RentalRequest, Boolean> onDecision) {
        this.requests = requests;
        this.onDecision = onDecision;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rental_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(requests.get(position));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCarModel;
        private final TextView tvDates;
        private final TextView tvUser;
        private final TextView tvStatus;
        private final TextView tvAdditionalRequests;
        private final TextView tvDriverLicense;
        private final TextView tvTotal;
        private final TextView tvDuration;
        private final LinearLayout layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvCarModel);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdditionalRequests = itemView.findViewById(R.id.tvAdditionalRequests);
            tvDriverLicense = itemView.findViewById(R.id.tvDriverLicense);
            tvTotal = itemView.findViewById(R.id.tvTotalPrice);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }

        public void bind(RentalRequest request) {
            RentalRequestStatus status = RentalRequestStatus.from(request.getStatus());
            String startDateStr = request.getStartDate() != null ? dateFormat.format(request.getStartDate()) : "N/A";
            String endDateStr = request.getEndDate() != null ? dateFormat.format(request.getEndDate()) : "N/A";
            String customerName = request.getUserName() != null ? request.getUserName() : "N/A";
            String license = request.getUserDriverLicense();

            tvCarModel.setText(request.getCarModel());
            tvDates.setText(itemView.getContext().getString(R.string.request_dates_format, startDateStr, endDateStr));
            tvUser.setText(itemView.getContext().getString(R.string.request_customer_format, customerName));
            tvDriverLicense.setText(license != null && !license.isEmpty()
                    ? license
                    : itemView.getContext().getString(R.string.request_license_missing));
            tvTotal.setText(itemView.getContext().getString(
                    R.string.request_total_format,
                    currencyFormat.format(PricingService.getStoredTotal(request))));
            tvDuration.setText(itemView.getContext().getString(
                    R.string.request_duration_format,
                    PricingService.computeRentalDays(request.getStartDate(), request.getEndDate())));

            if (request.getAdditionalRequests() != null && !request.getAdditionalRequests().isEmpty()) {
                tvAdditionalRequests.setText(request.getAdditionalRequests());
                tvAdditionalRequests.setVisibility(View.VISIBLE);
            } else {
                tvAdditionalRequests.setVisibility(View.GONE);
            }

            bindStatus(status);

            if (status == RentalRequestStatus.PENDING) {
                layoutActions.setVisibility(View.VISIBLE);
                itemView.findViewById(R.id.btnApprove).setOnClickListener(v -> {
                    Log.d("ADAPTER", "Approve clicked for: " + request.getRequestId());
                    onDecision.accept(request, true);
                });
                itemView.findViewById(R.id.btnReject).setOnClickListener(v -> {
                    Log.d("ADAPTER", "Reject clicked for: " + request.getRequestId());
                    onDecision.accept(request, false);
                });
            } else {
                layoutActions.setVisibility(View.GONE);
            }
        }

        private void bindStatus(RentalRequestStatus status) {
            int backgroundRes = R.drawable.bg_status_pending;
            int textColor = ContextCompat.getColor(itemView.getContext(), R.color.colorWarning);
            int labelRes = R.string.request_status_pending;

            if (status == RentalRequestStatus.APPROVED || status == RentalRequestStatus.COMPLETED) {
                backgroundRes = R.drawable.bg_status_approved;
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.colorSuccess);
                labelRes = R.string.request_status_approved;
            } else if (status == RentalRequestStatus.REJECTED) {
                backgroundRes = R.drawable.bg_status_rejected;
                textColor = ContextCompat.getColor(itemView.getContext(), R.color.colorError);
                labelRes = R.string.request_status_rejected;
            }

            tvStatus.setBackgroundResource(backgroundRes);
            tvStatus.setTextColor(textColor);
            tvStatus.setText(labelRes);
        }
    }
}

