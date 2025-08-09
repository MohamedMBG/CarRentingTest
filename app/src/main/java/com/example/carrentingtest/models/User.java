package com.example.carrentingtest.models;

public class User {
    private String id;
    private String email;
    private String name;
    private String phone;
    private String driverLicense;
    private String role;
    private String companyId;
    private String verificationStatus;
    private com.google.firebase.Timestamp verificationUpdatedAt;

    public User() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDriverLicense() { return driverLicense; }
    public void setDriverLicense(String driverLicense) { this.driverLicense = driverLicense; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public com.google.firebase.Timestamp getVerificationUpdatedAt() { return verificationUpdatedAt; }
    public void setVerificationUpdatedAt(com.google.firebase.Timestamp verificationUpdatedAt) { this.verificationUpdatedAt = verificationUpdatedAt; }
}