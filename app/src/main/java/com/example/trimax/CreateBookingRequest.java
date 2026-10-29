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
        /** Half cases (20261028_07): the quantity is in half cases, each priced at the half case price. */
        public final boolean half;
        /** Pesos off each piece (agent discount per item, 20261027_04); null or 0 = full price. */
        public java.math.BigDecimal discountPerUnit;

        public Line(int productId, int quantity) {
            this(productId, quantity, false);
        }

        public Line(int productId, int quantity, boolean half) {
            this.productId = productId;
            this.quantity = quantity;
            this.half = half;
        }

        /** A line from a line key (the product id, or minus it for half cases; see ItemDiscounts.key). */
        public static Line ofKey(int key, int quantity) {
            return new Line(ItemDiscounts.productOf(key), quantity, ItemDiscounts.isHalf(key));
        }

        /** The line key: the product id, or minus it for half cases. */
        public int key() {
            return ItemDiscounts.key(productId, half);
        }
    }

    /** How the agent took the order (20261029_02): went to the customer (a photo is required) or by text / call. */
    public static final String VIA_VISIT = "VISIT";
    public static final String VIA_TEXT_CALL = "TEXT_CALL";
    /** The server's rule for the photo's name in the agent's folder: visit_[A-Za-z0-9_-]{1,80}.jpg */
    static final java.util.regex.Pattern VISIT_PHOTO_NAME = java.util.regex.Pattern.compile("visit_[A-Za-z0-9_-]{1,80}\\.jpg");

    public final String requestId;
    /** VISIT or TEXT_CALL; null only for an older flow (then the booking is sent without it). */
    public String orderVia;
    /** VISIT: the photo taken at the customer's place (a JPEG on this phone). */
    public java.io.File visitPhoto;
    /** VISIT: the photo's name on the server; chosen once per photo, so a retry uploads to the same place. */
    public String visitPhotoName;
    /** VISIT: where and when the photo was taken (no location when the phone had none). */
    public Double visitLatitude, visitLongitude;
    public String visitTakenAtIso;
    public String customerName;
    public String customerContact;
    public String deliveryAddress;
    public double latitude;
    public double longitude;
    public String schedule;
    public String instructions;
    public final List<Line> lines = new ArrayList<>();

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
        String via = orderViaProblem();
        if (via != null) return via;
        for (Line l : lines) {
            if (l.productId <= 0) return "Refresh the product list and try again.";
            if (l.quantity < 1 || l.quantity > 1000) return "Each quantity must be between 1 and 1000.";
            if (l.discountPerUnit != null) {
                if (l.discountPerUnit.signum() < 0) return "A discount cannot be negative.";
                if (l.discountPerUnit.stripTrailingZeros().scale() > 2) return "Use at most 2 decimals for a discount.";
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
            // Only sent for half cases, so a whole-case booking is the same as before.
            if (l.half) i.put("unit", "HALF");
            // Only sent when there is a discount, so a booking without one also works on an older server.
            if (l.discountPerUnit != null && l.discountPerUnit.signum() > 0) {
                i.put("discount_per_unit", l.discountPerUnit.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
            }
            items.put(i);
        }
        a.put("p_items", items);
        a.put("p_requested_schedule", isBlank(schedule) ? JSONObject.NULL : schedule.trim());
        a.put("p_special_instructions", isBlank(instructions) ? JSONObject.NULL : instructions.trim());
        return a;
    }

    /** First problem with Visit / Text-Call, or null. Only checked when the choice is used (orderVia set by the screen). */
    String orderViaProblem() {
        if (orderVia == null) return null;
        if (!VIA_VISIT.equals(orderVia) && !VIA_TEXT_CALL.equals(orderVia)) {
            return "Choose how you took this order: Visit or Text / Call.";
        }
        if (VIA_VISIT.equals(orderVia)) {
            if (visitPhoto == null || !visitPhoto.isFile() || visitPhoto.length() == 0) {
                return "Take a photo of the visit before confirming the booking.";
            }
            if (visitPhotoName == null || !VISIT_PHOTO_NAME.matcher(visitPhotoName).matches()) {
                return "Take the visit photo again.";
            }
            if ((visitLatitude == null) != (visitLongitude == null)) return "Take the visit photo again.";
        }
        return null;
    }

    /** True when the booking is sent with the Visit / Text-Call choice (public.trimax_agent_create_booking_v2). */
    public boolean usesOrderVia() {
        return orderVia != null;
    }

    /**
     * Named arguments of public.trimax_agent_create_booking_v2: the same booking plus the choice and, for a
     * visit, the uploaded photo's path in this agent's folder ("&lt;auth uid&gt;/visit_....jpg").
     */
    public JSONObject toRpcArgsV2(String visitPhotoPath) throws JSONException {
        JSONObject a = toRpcArgs();
        a.put("p_order_via", orderVia);
        boolean visit = VIA_VISIT.equals(orderVia);
        boolean located = visit && visitLatitude != null && visitLongitude != null
                && Double.isFinite(visitLatitude) && Double.isFinite(visitLongitude)
                && !(visitLatitude == 0 && visitLongitude == 0);
        a.put("p_visit_photo_path", visit && visitPhotoPath != null ? visitPhotoPath : JSONObject.NULL);
        a.put("p_visit_latitude", located ? visitLatitude : JSONObject.NULL);
        a.put("p_visit_longitude", located ? visitLongitude : JSONObject.NULL);
        a.put("p_visit_taken_at", visit && visitTakenAtIso != null ? visitTakenAtIso : JSONObject.NULL);
        return a;
    }

    /** visit_1729850000000_ab12cd34.jpg: matches the server's rule visit_[A-Za-z0-9_-]{1,80}.jpg */
    static String visitPhotoName(long millis, String uuid) {
        String tail = uuid == null ? "x" : uuid.replaceAll("[^A-Za-z0-9]", "");
        if (tail.length() > 8) tail = tail.substring(0, 8);
        if (tail.isEmpty()) tail = "x";
        return String.format(java.util.Locale.US, "visit_%d_%s.jpg", millis, tail);
    }

    /** 2026-10-29T03:15:00Z: the photo time as the server reads it (UTC). */
    static String isoUtc(long millis) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US);
        f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        return f.format(new java.util.Date(millis));
    }

    /** A new server name for a newly taken visit photo. */
    static String newVisitPhotoName() {
        return visitPhotoName(System.currentTimeMillis(), UUID.randomUUID().toString());
    }

    public boolean hasDiscount() {
        for (Line l : lines) if (l.discountPerUnit != null && l.discountPerUnit.signum() > 0) return true;
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
