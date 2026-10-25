package com.example.trimax;

public class Delivery {

    private int id;
    private String bookingId;
    private String driverId;
    private String customerId;
    private String customerName;
    private String customerPhone;
    private String pickupLocation;
    private double pickupLatitude;
    private double pickupLongitude;
    private String destination;
    private double destinationLatitude;
    private double destinationLongitude;
    private String specialInstructions;
    private String packageDetails;
    private double originalAmount;
    private double currentTotal;
    private double depositAmount;
    private String depositStatus; // PENDING, COLLECTED, RETURNED, FORFEITED, N/A
    private String status; // driver screens: ASSIGNED, STARTED, DELIVERED, FAILED, CANCELLED, COMPLETED (older rows may still say PICKED UP / ON THE WAY / ARRIVED AT DESTINATION); see DeliveryProgress
    private int sequenceNumber;
    private String scheduledTime;
    private String priority;
    private String createdAt;
    private String updatedAt;

    public Delivery() {}

    public Delivery(int id, String bookingId, String driverId, String customerId, String customerName, String customerPhone,
                    String pickupLocation, double pickupLatitude, double pickupLongitude,
                    String destination, double destinationLatitude, double destinationLongitude,
                    String specialInstructions, String packageDetails, double originalAmount, double currentTotal,
                    double depositAmount, String depositStatus, String status, int sequenceNumber,
                    String scheduledTime, String priority, String createdAt, String updatedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.driverId = driverId;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.pickupLocation = pickupLocation;
        this.pickupLatitude = pickupLatitude;
        this.pickupLongitude = pickupLongitude;
        this.destination = destination;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.specialInstructions = specialInstructions;
        this.packageDetails = packageDetails;
        this.originalAmount = originalAmount;
        this.currentTotal = currentTotal;
        this.depositAmount = depositAmount;
        this.depositStatus = depositStatus;
        this.status = status;
        this.sequenceNumber = sequenceNumber;
        this.scheduledTime = scheduledTime;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

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

    // Alias for legacy getDeliveryId()
    public String getDeliveryId() {
        return bookingId;
    }

    public void setDeliveryId(String deliveryId) {
        this.bookingId = deliveryId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    // Alias for legacy getDriverUsername()
    public String getDriverUsername() {
        return driverId;
    }

    public void setDriverUsername(String driverUsername) {
        this.driverId = driverUsername;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    // Alias for legacy getPickupAddress()
    public String getPickupAddress() {
        return pickupLocation;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupLocation = pickupAddress;
    }

    public double getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(double pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public double getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(double pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    // Alias for legacy getDestinationAddress()
    public String getDestinationAddress() {
        return destination;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destination = destinationAddress;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public void setDestinationLatitude(double destinationLatitude) {
        this.destinationLatitude = destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public void setDestinationLongitude(double destinationLongitude) {
        this.destinationLongitude = destinationLongitude;
    }

    // Compatibility getters for latitude/longitude (uses destination lat/lng by default)
    public double getLatitude() {
        return destinationLatitude != 0 ? destinationLatitude : pickupLatitude;
    }

    public void setLatitude(double latitude) {
        this.destinationLatitude = latitude;
    }

    public double getLongitude() {
        return destinationLongitude != 0 ? destinationLongitude : pickupLongitude;
    }

    public void setLongitude(double longitude) {
        this.destinationLongitude = longitude;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    // Alias for legacy getDeliveryNotes()
    public String getDeliveryNotes() {
        return specialInstructions;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.specialInstructions = deliveryNotes;
    }

    public String getPackageDetails() {
        return packageDetails;
    }

    public void setPackageDetails(String packageDetails) {
        this.packageDetails = packageDetails;
    }

    public double getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(double originalAmount) {
        this.originalAmount = originalAmount;
    }

    public double getCurrentTotal() {
        return currentTotal;
    }

    public void setCurrentTotal(double currentTotal) {
        this.currentTotal = currentTotal;
    }

    public double getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(double depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getDepositStatus() {
        return depositStatus;
    }

    public void setDepositStatus(String depositStatus) {
        this.depositStatus = depositStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
