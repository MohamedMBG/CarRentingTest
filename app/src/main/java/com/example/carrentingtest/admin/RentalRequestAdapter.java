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

import java.util.List;
import java.util.function.BiConsumer;

public class RentalRequestAdapter extends RecyclerView.Adapter<RentalRequestAdapter.ViewHolder> {
    private List<RentalRequest> requests;
    private BiConsumer<RentalRequest, Boolean> onDecision;

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
        private final TextView tvCarModel, tvDates, tvUser, tvStatus, tvAdditionalRequests;
        private final LinearLayout layoutActions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvCarModel);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvUser = itemView.findViewById(R.id.tvUser);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdditionalRequests = itemView.findViewById(R.id.tvAdditionalRequests);
            layoutActions = itemView.findViewById(R.id.layoutActions);
        }

        public void bind(RentalRequest request) {
            tvCarModel.setText(request.getCarModel());
            tvDates.setText(request.getStartDate() + " to " + request.getEndDate());
            tvUser.setText(request.getUserName());
            tvStatus.setText(request.getStatus());

            // Handle additional requests
            if (request.getAdditionalRequests() != null && !request.getAdditionalRequests().isEmpty()) {
                tvAdditionalRequests.setText("Special Requests: " + request.getAdditionalRequests());
                tvAdditionalRequests.setVisibility(View.VISIBLE);
            } else {
                tvAdditionalRequests.setVisibility(View.GONE);
            }

            // Only show buttons for pending requests
            if ("pending".equals(request.getStatus())) {
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