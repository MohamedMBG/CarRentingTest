package com.example.carrentingtest.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Car implements Serializable {
    private String documentId;  // Add this field
    private String model;
    private String type;
    private double pricePerDay;
    private String imageUrl; // Legacy single image support
    private List<String> imageUrls;
    private boolean available;
    private String companyId;
    private int seats;
    private String transmissionType;
    private int rentalCount;

    // Empty constructor for Firestore
    public Car() {}

    // Add document ID getter/setter
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    // Getters and setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }

    public String getImageUrl() {
        List<String> urls = getImageUrls();
        return urls.isEmpty() ? null : urls.get(0);
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            List<String> urls = new ArrayList<>();
            urls.add(imageUrl.trim());
            this.imageUrls = urls;
        } else {
            this.imageUrls = new ArrayList<>();
        }
    }

    public List<String> getImageUrls() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        if (imageUrls.isEmpty() && imageUrl != null && !imageUrl.trim().isEmpty()) {
            imageUrls.add(imageUrl.trim());
        }

        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        if (imageUrls == null) {
            this.imageUrls = new ArrayList<>();
        } else {
            this.imageUrls = new ArrayList<>();
            for (String url : imageUrls) {
                if (url != null && !url.trim().isEmpty()) {
                    this.imageUrls.add(url.trim());
                }
            }
        }

        if (this.imageUrls.isEmpty()) {
            this.imageUrl = null;
        } else {
            this.imageUrl = this.imageUrls.get(0);
        }
    }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }


    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public String getTransmissionType() { return transmissionType; }
    public void setTransmissionType(String transmissionType) { this.transmissionType = transmissionType; }

    public int getRentalCount() { return rentalCount; }
    public void setRentalCount(int rentalCount) { this.rentalCount = rentalCount; }
}