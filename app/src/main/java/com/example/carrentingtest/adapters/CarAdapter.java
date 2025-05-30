package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.carrentingtest.R;
import com.example.carrentingtest.models.Car;

import java.util.List;

public class CarAdapter extends ArrayAdapter<Car> {

    private final LayoutInflater inflater;

    public CarAdapter(Context context, List<Car> cars) {
        super(context, 0, cars);
        inflater = LayoutInflater.from(context);
    }

    static class ViewHolder {
        ImageView carImage;
        TextView carModel;
        TextView carType;
        TextView carPrice;
        TextView carAvailability;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Car car = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_car, parent, false);
            holder = new ViewHolder();

            holder.carImage = convertView.findViewById(R.id.carImage);
            holder.carModel = convertView.findViewById(R.id.carModel);
            holder.carType = convertView.findViewById(R.id.carType);
            holder.carPrice = convertView.findViewById(R.id.carPrice);
            holder.carAvailability = convertView.findViewById(R.id.carAvailability);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (car != null) {
            // Set texts
            holder.carModel.setText(car.getModel());
            holder.carType.setText(car.getType());

            // Format price
            holder.carPrice.setText(String.format("$%.2f / jour", car.getPricePerDay()));

            // Load image with Glide
            Glide.with(getContext())
                    .load(car.getImageUrl())
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .centerCrop()
                    .into(holder.carImage);

            // Handle availability badge
            if (car.isAvailable()) {
                holder.carAvailability.setText("Disponible");
                holder.carAvailability.setVisibility(View.VISIBLE);
                holder.carAvailability.setBackgroundTintList(
                        ContextCompat.getColorStateList(getContext(), R.color.primary_blue)); // #1F7A8C
            } else {
                holder.carAvailability.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}
