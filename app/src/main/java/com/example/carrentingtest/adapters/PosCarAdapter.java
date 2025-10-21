package com.example.carrentingtest.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.Car;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PosCarAdapter extends RecyclerView.Adapter<PosCarAdapter.CarViewHolder> {

    public interface PosActionListener {
        void onGenerateInvoice(@NonNull PosRentalDisplay rental);

        void onUploadPaymentProof(@NonNull PosRentalDisplay rental);

        void onRemovePaymentProof(@NonNull PosRentalDisplay rental);
    }

    private final List<PosCarSummary> data;
    private final PosActionListener listener;
    private final NumberFormat currencyFormat;
    private final SimpleDateFormat dateFormat;

    public PosCarAdapter(List<PosCarSummary> data,
                         PosActionListener listener,
                         NumberFormat currencyFormat,
                         SimpleDateFormat dateFormat) {
        this.data = data;
        this.listener = listener;
        this.currencyFormat = currencyFormat;
        this.dateFormat = dateFormat;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pos_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        holder.bind(data.get(position));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void updateData(List<PosCarSummary> newData) {
        if (newData == null) {
            data.clear();
            notifyDataSetChanged();
            return;
        }

        if (newData == data) {
            notifyDataSetChanged();
            return;
        }

        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    public void updatePaymentProofDetails(String requestId, boolean hasProof, String proofUrl) {
        for (int i = 0; i < data.size(); i++) {
            PosCarSummary summary = data.get(i);
            for (PosRentalDisplay rental : summary.getRentals()) {
                if (TextUtils.equals(requestId, rental.getRequestId())) {
                    rental.setPaymentProofProvided(hasProof);
                    rental.setPaymentProofUrl(proofUrl);
                    notifyItemChanged(i);
                    return;
                }
            }
        }
    }

    class CarViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCarModel;
        private final TextView tvRentalCount;
        private final TextView tvRevenue;
        private final RecyclerView nestedRecyclerView;
        private final PosRentalAdapter rentalAdapter;

        CarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCarModel = itemView.findViewById(R.id.tvCarModel);
            tvRentalCount = itemView.findViewById(R.id.tvRentalCount);
            tvRevenue = itemView.findViewById(R.id.tvRevenue);
            nestedRecyclerView = itemView.findViewById(R.id.recyclerRentals);
            nestedRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rentalAdapter = new PosRentalAdapter();
            nestedRecyclerView.setAdapter(rentalAdapter);
        }

        void bind(PosCarSummary summary) {
            String model = summary.getCar() != null ? summary.getCar().getModel() : null;
            if (TextUtils.isEmpty(model)) {
                model = itemView.getContext().getString(R.string.pos_unknown_car);
            }
            tvCarModel.setText(model);
            tvRentalCount.setText(itemView.getContext().getResources().getQuantityString(
                    R.plurals.pos_rental_count,
                    summary.getRentalCount(),
                    summary.getRentalCount()));
            tvRevenue.setText(itemView.getContext().getString(
                    R.string.pos_car_revenue,
                    currencyFormat.format(summary.getTotalRevenue())));
            rentalAdapter.submitList(summary.getRentals());
        }
    }

    private class PosRentalAdapter extends RecyclerView.Adapter<PosRentalAdapter.RentalViewHolder> {
        private final List<PosRentalDisplay> rentals = new ArrayList<>();

        void submitList(List<PosRentalDisplay> newRentals) {
            rentals.clear();
            rentals.addAll(newRentals);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RentalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pos_rental, parent, false);
            return new RentalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RentalViewHolder holder, int position) {
            holder.bind(rentals.get(position));
        }

        @Override
        public int getItemCount() {
            return rentals.size();
        }

        class RentalViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvClient;
            private final TextView tvPhone;
            private final TextView tvPeriod;
            private final TextView tvDuration;
            private final TextView tvStatus;
            private final TextView tvAmount;
            private final TextView tvPaymentProof;
            private final ImageView ivPaymentProof;
            private final MaterialButton btnInvoice;
            private final MaterialButton btnUploadProof;
            private final MaterialButton btnRemoveProof;

            RentalViewHolder(@NonNull View itemView) {
                super(itemView);
                tvClient = itemView.findViewById(R.id.tvClientName);
                tvPhone = itemView.findViewById(R.id.tvClientPhone);
                tvPeriod = itemView.findViewById(R.id.tvRentalPeriod);
                tvDuration = itemView.findViewById(R.id.tvRentalDuration);
                tvStatus = itemView.findViewById(R.id.tvRentalStatus);
                tvAmount = itemView.findViewById(R.id.tvRentalAmount);
                tvPaymentProof = itemView.findViewById(R.id.tvPaymentProofStatus);
                ivPaymentProof = itemView.findViewById(R.id.ivPaymentProof);
                btnInvoice = itemView.findViewById(R.id.btnInvoice);
                btnUploadProof = itemView.findViewById(R.id.btnUploadProof);
                btnRemoveProof = itemView.findViewById(R.id.btnRemoveProof);
            }

            void bind(PosRentalDisplay rental) {
                String clientName = TextUtils.isEmpty(rental.getUserName()) ?
                        itemView.getContext().getString(R.string.pos_value_placeholder) : rental.getUserName();
                tvClient.setText(itemView.getContext().getString(R.string.pos_client_label, clientName));

                if (TextUtils.isEmpty(rental.getUserPhone())) {
                    tvPhone.setVisibility(View.GONE);
                } else {
                    tvPhone.setVisibility(View.VISIBLE);
                    tvPhone.setText(itemView.getContext().getString(R.string.pos_phone_label, rental.getUserPhone()));
                }

                tvPeriod.setText(itemView.getContext().getString(
                        R.string.pos_period_label,
                        formatDate(rental.getStartDate()),
                        formatDate(rental.getEndDate())));

                tvDuration.setText(itemView.getContext().getResources().getQuantityString(
                        R.plurals.pos_days_count,
                        Math.max(rental.getRentalDays(), 1),
                        Math.max(rental.getRentalDays(), 1)));

                tvStatus.setText(itemView.getContext().getString(R.string.pos_status_label, formatStatus(rental.getStatus())));
                tvAmount.setText(itemView.getContext().getString(
                        R.string.pos_amount_label,
                        currencyFormat.format(rental.getTotalPrice())));

                boolean hasProof = rental.hasPaymentProof();
                boolean hasProofUrl = !TextUtils.isEmpty(rental.getPaymentProofUrl());
                tvPaymentProof.setText(hasProof ?
                        itemView.getContext().getString(R.string.pos_payment_proof_attached) :
                        itemView.getContext().getString(R.string.pos_payment_proof_missing));

                if (hasProof && hasProofUrl) {
                    ivPaymentProof.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(rental.getPaymentProofUrl())
                            .placeholder(R.drawable.car_placeholder)
                            .error(R.drawable.car_placeholder)
                            .into(ivPaymentProof);
                } else {
                    ivPaymentProof.setVisibility(View.GONE);
                }

                btnInvoice.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onGenerateInvoice(rental);
                    }
                });
                btnUploadProof.setText(hasProof ?
                        itemView.getContext().getString(R.string.pos_replace_payment_proof) :
                        itemView.getContext().getString(R.string.pos_upload_payment_proof));
                btnUploadProof.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUploadPaymentProof(rental);
                    }
                });

                btnRemoveProof.setVisibility(hasProof ? View.VISIBLE : View.GONE);
                btnRemoveProof.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onRemovePaymentProof(rental);
                    }
                });
            }

            private String formatDate(Date date) {
                return date == null ? itemView.getContext().getString(R.string.pos_value_placeholder) : dateFormat.format(date);
            }

            private String formatStatus(String status) {
                if (TextUtils.isEmpty(status)) {
                    return itemView.getContext().getString(R.string.pos_unknown_status);
                }
                String lower = status.toLowerCase(Locale.getDefault());
                return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            }
        }
    }

    public static class PosCarSummary {
        private final Car car;
        private final List<PosRentalDisplay> rentals;
        private final double totalRevenue;

        public PosCarSummary(Car car, List<PosRentalDisplay> rentals, double totalRevenue) {
            this.car = car;
            this.rentals = rentals;
            this.totalRevenue = totalRevenue;
        }

        public Car getCar() {
            return car;
        }

        public List<PosRentalDisplay> getRentals() {
            return rentals;
        }

        public double getTotalRevenue() {
            return totalRevenue;
        }

        public int getRentalCount() {
            return rentals.size();
        }
    }

    public static class PosRentalDisplay {
        private String requestId;
        private String carId;
        private String carModel;
        private String userName;
        private String userPhone;
        private Date startDate;
        private Date endDate;
        private String status;
        private int rentalDays;
        private double totalPrice;
        private boolean paymentProofProvided;
        private String paymentProofUrl;

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public String getCarId() {
            return carId;
        }

        public void setCarId(String carId) {
            this.carId = carId;
        }

        public String getCarModel() {
            return carModel;
        }

        public void setCarModel(String carModel) {
            this.carModel = carModel;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getUserPhone() {
            return userPhone;
        }

        public void setUserPhone(String userPhone) {
            this.userPhone = userPhone;
        }

        public Date getStartDate() {
            return startDate;
        }

        public void setStartDate(Date startDate) {
            this.startDate = startDate;
        }

        public Date getEndDate() {
            return endDate;
        }

        public void setEndDate(Date endDate) {
            this.endDate = endDate;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getRentalDays() {
            return rentalDays;
        }

        public void setRentalDays(int rentalDays) {
            this.rentalDays = rentalDays;
        }

        public double getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(double totalPrice) {
            this.totalPrice = totalPrice;
        }

        public boolean hasPaymentProof() {
            return paymentProofProvided;
        }

        public void setPaymentProofProvided(boolean paymentProofProvided) {
            this.paymentProofProvided = paymentProofProvided;
        }

        public String getPaymentProofUrl() {
            return paymentProofUrl;
        }

        public void setPaymentProofUrl(String paymentProofUrl) {
            this.paymentProofUrl = paymentProofUrl;
        }
    }
}