package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.RentalRequest;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// Adapter specifically for displaying rental history to the client
public class ClientRentalRequestAdapter extends RecyclerView.Adapter<ClientRentalRequestAdapter.ViewHolder> {
    private List<RentalRequest> requests;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private Context context;

    public ClientRentalRequestAdapter(Context context, List<RentalRequest> requests) {
        this.context = context;
        this.requests = requests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the new client-specific layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_client_rental_request, parent, false);
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
        // References to the views in item_client_rental_request.xml
        private final TextView tvClientCarModel, tvClientDates, tvClientStatus, tvClientAdditionalRequests;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views from the new layout
            tvClientCarModel = itemView.findViewById(R.id.tvClientCarModel);
            tvClientDates = itemView.findViewById(R.id.tvClientDates);
            tvClientStatus = itemView.findViewById(R.id.tvClientStatus);
            tvClientAdditionalRequests = itemView.findViewById(R.id.tvClientAdditionalRequests);
        }

        public void bind(RentalRequest request) {
            tvClientCarModel.setText(request.getCarModel() != null ? request.getCarModel() : "Unknown Car");
            String startDateStr = request.getStartDate() != null ? dateFormat.format(request.getStartDate()) : "N/A";
            String endDateStr = request.getEndDate() != null ? dateFormat.format(request.getEndDate()) : "N/A";
            tvClientDates.setText(String.format("Dates: %s to %s", startDateStr, endDateStr));
            tvClientStatus.setText(String.format("Status: %s", request.getStatus() != null ? request.getStatus() : "Unknown"));

            // Handle additional requests visibility
            if (request.getAdditionalRequests() != null && !request.getAdditionalRequests().isEmpty()) {
                tvClientAdditionalRequests.setText("Special Requests: " + request.getAdditionalRequests());
                tvClientAdditionalRequests.setVisibility(View.VISIBLE);
            } else {
                tvClientAdditionalRequests.setVisibility(View.GONE);
            }

            // Set status color (example)
            int statusColorRes;
            String status = request.getStatus() != null ? request.getStatus().toLowerCase() : "unknown";
            switch (status) {
                case "approved":
                    statusColorRes = R.color.colorSuccess; // Define this color in colors.xml
                    break;
                case "rejected":
                    statusColorRes = R.color.colorError; // Define this color in colors.xml
                    break;
                case "pending":
                default:
                    statusColorRes = R.color.colorWarning; // Define this color in colors.xml
                    break;
            }
            // Ensure context is not null before getting color
            if (context != null) {
                tvClientStatus.setTextColor(ContextCompat.getColor(context, statusColorRes));
            } else {
                // Handle case where context is null, maybe set a default color or log an error
                // For example, setting text color to black:
                // tvClientStatus.setTextColor(android.graphics.Color.BLACK);
            }
        }
    }
}
