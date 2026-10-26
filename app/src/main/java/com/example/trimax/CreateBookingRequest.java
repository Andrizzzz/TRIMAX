package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What the agent submits. Prices are deliberately absent: the server prices each line from its product
 * list, so a phone cannot change what a booking costs. requestId must be generated once per booking
 * attempt and reused on every retry, so a retry after a time-out cannot create a second booking.
 */
public final class CreateBookingRequest {

    public static final class Line {
        public final int productId;
        public final int quantity;

        public Line(int productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }

    public final String requestId;
    public String customerName;
    public String customerContact;
    public String deliveryAddress;
    public double latitude;
    public double longitude;
    public String schedule;
    public String instructions;
    public final List<Line> lines = new ArrayList<>();
    /** Agent discount (optional): a peso amount or a percent. The server checks the minimum price. */
    public DiscountCalc.Type discountType = DiscountCalc.Type.AMOUNT;
    public java.math.BigDecimal discountValue;

    public CreateBookingRequest(String requestId) {
        this.requestId = requestId != null ? requestId : UUID.randomUUID().toString();
    }

    /** First problem a user should fix, or null when the request looks valid (the server checks again). */
    public String validate() {
        try {
            UUID.fromString(requestId);
        } catch (Exception e) {
            return "Internal error: invalid request id.";
        }
        if (isBlank(customerName) || customerName.trim().length() > 100) return "Enter the customer name.";
        int digits = customerContact == null ? 0 : customerContact.replaceAll("[^0-9]", "").length();
        if (digits < 7 || customerContact.trim().length() > 30) return "Enter a valid customer contact number.";
        if (isBlank(deliveryAddress) || deliveryAddress.trim().length() > 500) return "Choose a delivery address.";
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180
                || (latitude == 0 && longitude == 0)) {
            return "Choose the delivery location on the map.";
        }
        if (schedule != null && schedule.trim().length() > 100) return "The requested date/time is too long.";
        if (instructions != null && instructions.trim().length() > 1000) return "The instructions are too long (1000 characters at most).";
        if (lines.isEmpty() || lines.size() > 50) return "Please select at least 1 product before continuing.";
        for (Line l : lines) {
            if (l.productId <= 0) return "Refresh the product list and try again.";
            if (l.quantity < 1 || l.quantity > 1000) return "Each quantity must be between 1 and 1000.";
        }
        if (discountValue != null) {
            if (discountValue.signum() < 0) return "A discount cannot be negative.";
            if (discountType == DiscountCalc.Type.PERCENT && discountValue.compareTo(new java.math.BigDecimal(100)) > 0) {
                return "A percent discount must be from 0 to 100.";
            }
        }
        return null;
    }

    /** Named arguments of public.trimax_agent_create_booking. */
    public JSONObject toRpcArgs() throws JSONException {
        JSONObject a = new JSONObject();
        a.put("p_request_id", requestId);
        a.put("p_customer_name", customerName.trim());
        a.put("p_customer_contact", customerContact.trim());
        a.put("p_delivery_address", deliveryAddress.trim());
        a.put("p_delivery_latitude", latitude);
        a.put("p_delivery_longitude", longitude);
        JSONArray items = new JSONArray();
        for (Line l : lines) {
            JSONObject i = new JSONObject();
            i.put("product_id", l.productId);
            i.put("quantity", l.quantity);
            items.put(i);
        }
        a.put("p_items", items);
        a.put("p_requested_schedule", isBlank(schedule) ? JSONObject.NULL : schedule.trim());
        a.put("p_special_instructions", isBlank(instructions) ? JSONObject.NULL : instructions.trim());
        // Only sent when there is a discount, so a booking without one also works on an older server.
        if (hasDiscount()) {
            a.put("p_discount_type", discountType == DiscountCalc.Type.PERCENT ? "PERCENT" : "AMOUNT");
            a.put("p_discount_value", discountValue);
        }
        return a;
    }

    public boolean hasDiscount() {
        return discountValue != null && discountValue.signum() > 0;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
