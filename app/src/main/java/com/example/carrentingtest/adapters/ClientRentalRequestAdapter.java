package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
        // Reuse the admin layout item, but we will hide the admin-specific parts
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
        private final TextView tvCarModel, tvDates, tvUser, tvStatus, tvAdditionalRequests, tvDriverLicense;
        private final LinearLayout layoutActions; // Admin actions layout

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvCarModel);
            tvDates = itemView.findViewById(R.id.tvDates);
            tvUser = itemView.findViewById(R.id.tvUser); // We might hide this for client view
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvAdditionalRequests = itemView.findViewById(R.id.tvAdditionalRequests);
            tvDriverLicense = itemView.findViewById(R.id.tvDriverLicense); // Hide this for client view
            layoutActions = itemView.findViewById(R.id.layoutActions); // Hide this for client view
        }

        public void bind(RentalRequest request) {
            tvCarModel.setText(request.getCarModel());
            String startDateStr = request.getStartDate() != null ? dateFormat.format(request.getStartDate()) : "N/A";
            String endDateStr = request.getEndDate() != null ? dateFormat.format(request.getEndDate()) : "N/A";
            tvDates.setText(String.format("Dates: %s to %s", startDateStr, endDateStr));
            tvStatus.setText(String.format("Status: %s", request.getStatus()));

            // Hide admin/unnecessary fields for client view
            tvUser.setVisibility(View.GONE);
            tvDriverLicense.setVisibility(View.GONE);
            layoutActions.setVisibility(View.GONE);

            // Handle additional requests visibility
            if (request.getAdditionalRequests() != null && !request.getAdditionalRequests().isEmpty()) {
                tvAdditionalRequests.setText("Special Requests: " + request.getAdditionalRequests());
                tvAdditionalRequests.setVisibility(View.VISIBLE);
            } else {
                tvAdditionalRequests.setVisibility(View.GONE);
            }

            // Set status color (example)
            int statusColorRes;
            switch (request.getStatus().toLowerCase()) {
                case "approved":
                    statusColorRes = R.color.colorSuccess; // Define this color
                    break;
                case "rejected":
                    statusColorRes = R.color.colorError; // Define this color
                    break;
                case "pending":
                default:
                    statusColorRes = R.color.colorWarning; // Define this color
                    break;
            }
            tvStatus.setTextColor(ContextCompat.getColor(context, statusColorRes));

        }
    }
}
