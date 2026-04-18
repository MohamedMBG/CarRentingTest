package com.example.carrentingtest.models;

public class PricingBreakdown {
    private double unitPricePerDay;
    private int rentalDays;
    private long rentalHours;
    private double basePrice;
    private double extrasTotal;
    private double discountTotal;
    private double totalPrice;
    private String currency;

    public PricingBreakdown() {}

    public double getUnitPricePerDay() {
        return unitPricePerDay;
    }

    public void setUnitPricePerDay(double unitPricePerDay) {
        this.unitPricePerDay = unitPricePerDay;
    }

    public int getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(int rentalDays) {
        this.rentalDays = rentalDays;
    }

    public long getRentalHours() {
        return rentalHours;
    }

    public void setRentalHours(long rentalHours) {
        this.rentalHours = rentalHours;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    public double getExtrasTotal() {
        return extrasTotal;
    }

    public void setExtrasTotal(double extrasTotal) {
        this.extrasTotal = extrasTotal;
    }

    public double getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(double discountTotal) {
        this.discountTotal = discountTotal;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
