package com.example.carrentingtest.models;

import java.util.Date;

public class RentalRequest {
    private String requestId;
    private String carId;
    private String carModel;
    private String userId;
    private String userName;
    private String userDriverLicense; // Added driver license field
    private Date startDate;
    private Date endDate;
    private String additionalRequests;
    private String status; // "pending", "approved", "rejected"

    public RentalRequest() {
    }

    // Getters and Setters

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserDriverLicense() { // Getter for driver license
        return userDriverLicense;
    }

    public void setUserDriverLicense(String userDriverLicense) { // Setter for driver license
        this.userDriverLicense = userDriverLicense;
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

    public String getAdditionalRequests() {
        return additionalRequests;
    }

    public void setAdditionalRequests(String additionalRequests) {
        this.additionalRequests = additionalRequests;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

