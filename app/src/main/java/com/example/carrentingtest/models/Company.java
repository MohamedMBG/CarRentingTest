package com.example.carrentingtest.models;

import com.google.firebase.firestore.GeoPoint;

public class Company {
    private String id;
    private String name;
    private String phone;
    private String address;
    private GeoPoint location;

    public Company() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }
}