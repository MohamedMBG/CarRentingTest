package com.example.carrentingtest.models;

import java.io.Serializable;

public class Car implements Serializable {
    private String documentId;  // Add this field
    private String model;
    private String type;
    private double pricePerDay;
    private String imageUrl;
    private boolean available;
    private String companyId;

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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }


    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
}