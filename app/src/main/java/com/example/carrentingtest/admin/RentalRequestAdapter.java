package com.example.carrentingtest.admin;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class RentalRequestAdapter extends RecyclerView.Adapter<RentalRequestAdapter.ViewHolder> {
    private List<RentalRequest> requests;
    private BiConsumer<RentalRequest, Boolean> onDecision;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

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
        // Added tvDriverLicense
        private final TextView tvCarModel, tvDates, tvUser, tvStatus, tvAdditionalRequests, tvDriverLicense;
        private final LinearLayout layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvCarModel);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdditionalRequests = itemView.findViewById(R.id.tvAdditionalRequests);
            tvDriverLicense = itemView.findViewById(R.id.tvDriverLicense); // Initialize tvDriverLicense
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }

        public void bind(RentalRequest request) {
            tvCarModel.setText(request.getCarModel());
            String startDateStr = request.getStartDate() != null ? dateFormat.format(request.getStartDate()) : "N/A";
            String endDateStr = request.getEndDate() != null ? dateFormat.format(request.getEndDate()) : "N/A";
            tvDates.setText(String.format("Dates: %s to %s", startDateStr, endDateStr));
            tvUser.setText(String.format("User: %s", request.getUserName() != null ? request.getUserName() : "N/A"));
            tvStatus.setText(request.getStatus());

            // Set Driver License
            if (request.getUserDriverLicense() != null && !request.getUserDriverLicense().isEmpty()) {
                tvDriverLicense.setText(String.format("License: %s", request.getUserDriverLicense()));
                tvDriverLicense.setVisibility(View.VISIBLE);
            } else {
                tvDriverLicense.setText("License: Not Provided");
                tvDriverLicense.setVisibility(View.VISIBLE); // Or GONE if you prefer to hide it
            }

            // Handle additional requests
            if (request.getAdditionalRequests() != null && !request.getAdditionalRequests().isEmpty()) {
                tvAdditionalRequests.setText("Special Requests: " + request.getAdditionalRequests());
                tvAdditionalRequests.setVisibility(View.VISIBLE);
            } else {
                tvAdditionalRequests.setVisibility(View.GONE);
            }

            // Set status background (Optional: Add color logic if needed)
            // Example: if ("approved".equals(request.getStatus())) { ... }

            // Only show buttons for pending requests
            if ("pending".equalsIgnoreCase(request.getStatus())) { // Use equalsIgnoreCase for robustness
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
    }
}

