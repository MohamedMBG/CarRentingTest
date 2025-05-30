package com.example.carrentingtest.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.carrentingtest.R;
import com.example.carrentingtest.models.Car;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CarAdapter extends ArrayAdapter<Car> {

    public CarAdapter(Context context, List<Car> cars) {
        super(context, 0, cars);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Reuse view if exists
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_car, parent, false);
        }

        // Get current car
        Car car = getItem(position);

        // Setup views
        TextView modelText = convertView.findViewById(R.id.carModel);
        TextView typeText = convertView.findViewById(R.id.carType);
        TextView priceText = convertView.findViewById(R.id.carPrice);
        ImageView carImage = convertView.findViewById(R.id.carImage);

        // Set car data
        modelText.setText(car.getModel());
        typeText.setText(car.getType());
        priceText.setText(String.format("$%.2f/day", car.getPricePerDay()));

        // Load image
        if (car.getImageUrl() != null && !car.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(car.getImageUrl())
                    .placeholder(R.drawable.ic_app_logo)
                    .error(R.drawable.ic_app_logo)
                    .into(carImage);
        } else {
            carImage.setImageResource(R.drawable.ic_app_logo);
        }

        return convertView;
    }
}