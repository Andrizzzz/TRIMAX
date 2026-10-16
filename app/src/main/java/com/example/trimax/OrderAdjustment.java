package com.example.trimax;

public class OrderAdjustment {

    private int id;
    private String bookingId;
    private String driverId;
    private String type; // ADDITIONAL_CHARGE, DAMAGE_CHARGE, DEPOSIT, DEDUCTION, REFUND
    private double amount;
    private String reason;
    private String description;
    private String createdAt;

    public OrderAdjustment() {}

    public OrderAdjustment(int id, String bookingId, String driverId, String type, double amount, String reason, String description, String createdAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.driverId = driverId;
        this.type = type;
        this.amount = amount;
        this.reason = reason;
        this.description = description;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
