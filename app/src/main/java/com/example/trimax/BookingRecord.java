package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One booking as the TRIMAX server returns it (trimax_private.booking_json). This is the shared,
 * cloud-authoritative record; nothing here is ever written to the phone's local database.
 */
public final class BookingRecord {

    /** Where drivers collect the goods. Matches the hub used by the booking screens. */
    public static final String HUB_NAME = "TRIMAX Hub (14.782700, 121.047587)";
    public static final double HUB_LATITUDE = 14.7826996;
    public static final double HUB_LONGITUDE = 121.0475865;

    public static final class Item {
        public int productId;
        public String productName;
        public int quantity;
        public double unitPrice;
        public double subtotal;
    }

    public static final class Event {
        public String at, type, from, to, bookingStatus, note;
    }

    public int bookingId;
    public String createdAt;
    /** Administrator status: Pending, Confirmed, In Progress, Completed, Cancelled. */
    public String status;
    /** Driver progress (see DeliveryProgress), or null when no driver is assigned. */
    public String deliveryStatus;
    public String deliveryStatusAt;
    public String assignedAt;
    public String lastChangedAt;
    public double totalAmount;
    /** Before the agent's discount (equals totalAmount when there is none). */
    public double subtotalAmount;
    /** The agent's discount: amount in pesos (0 = none), how it was typed, who gave it. */
    public double discountAmount;
    public String discountType, discountBy, discountAt;
    public Double discountValue;
    public String createdVia;

    public Integer clientId;
    public String customerName, customerContact, customerAddress;

    public String deliveryAddress, schedule, instructions;
    public Double deliveryLatitude, deliveryLongitude;

    public String driverUsername, driverName, vehicleId, vehicleType;

    public List<Item> items = new ArrayList<>();
    /** Newest first. Empty unless the detail call was made. */
    public List<Event> events = new ArrayList<>();
    /** Working order for drivers (1 = current); 0 when not given. */
    public int sequence;
    /** Only on create: true when the server returned an earlier booking for the same request id. */
    public boolean alreadyExisted;
    public boolean customerMatchedExisting;

    public boolean hasDiscount() {
        return discountAmount > 0;
    }

    /** "10% (₱20.00)" or "₱20.00", for the booking screens. */
    public String discountText() {
        if (!hasDiscount()) return "";
        String amount = String.format(Locale.US, "\u20B1%,.2f", discountAmount);
        if ("PERCENT".equals(discountType) && discountValue != null) {
            String pct = discountValue % 1 == 0 ? String.valueOf(discountValue.longValue()) : String.valueOf(discountValue);
            return pct + "% (" + amount + ")";
        }
        return amount;
    }

    public String idText() {
        return String.valueOf(bookingId);
    }

    public boolean hasDriver() {
        return driverUsername != null;
    }

    /** "Coke Case (24 Bottles) x5, Sprite Case (24 Bottles) x1". */
    public String itemsSummary() {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Item i : items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(i.productName != null ? i.productName : "Product " + i.productId).append(" x").append(i.quantity);
        }
        return sb.toString();
    }

    public int itemCount() {
        int n = 0;
        for (Item i : items) n += i.quantity;
        return n;
    }

    /**
     * The existing screens were written around the local {@link Delivery} model; this fills one from the
     * server record so they can keep their layout. Statuses use the words those screens already show.
     *
     * @param forDriver true: status is the driver's progress; false: the agent's view of the booking.
     */
    public Delivery toDelivery(boolean forDriver) {
        Delivery d = new Delivery();
        d.setId(bookingId);
        d.setBookingId(idText());
        d.setDriverId(driverName != null ? driverName : driverUsername);
        d.setCustomerId(clientId != null ? String.valueOf(clientId) : null);
        d.setCustomerName(customerName);
        d.setCustomerPhone(customerContact);
        d.setPickupLocation(HUB_NAME);
        d.setPickupLatitude(HUB_LATITUDE);
        d.setPickupLongitude(HUB_LONGITUDE);
        d.setDestination(deliveryAddress);
        d.setDestinationLatitude(deliveryLatitude != null ? deliveryLatitude : 0);
        d.setDestinationLongitude(deliveryLongitude != null ? deliveryLongitude : 0);
        d.setSpecialInstructions(instructions);
        d.setPackageDetails(itemsSummary());
        d.setOriginalAmount(totalAmount);
        d.setCurrentTotal(totalAmount);
        d.setDepositStatus("N/A");
        d.setStatus(forDriver ? DeliveryProgress.driverDisplayStatus(status, deliveryStatus)
                              : DeliveryProgress.agentDisplayStatus(status));
        d.setSequenceNumber(sequence > 0 ? sequence : 1);
        d.setScheduledTime(schedule != null ? schedule : createdAt);
        d.setPriority("NORMAL");
        d.setCreatedAt(createdAt);
        d.setUpdatedAt(lastChangedAt);
        return d;
    }

    // =====================================================================================
    // Parsing
    // =====================================================================================

    public static BookingRecord parse(String json) throws JSONException {
        return from(new JSONObject(json));
    }

    public static List<BookingRecord> parseList(String json) throws JSONException {
        String trimmed = json == null ? "" : json.trim();
        if (trimmed.isEmpty() || "null".equals(trimmed)) return Collections.emptyList();
        JSONArray array = new JSONArray(trimmed);
        List<BookingRecord> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o != null) list.add(from(o));
        }
        return list;
    }

    public static BookingRecord from(JSONObject o) throws JSONException {
        BookingRecord b = new BookingRecord();
        Double id = Json.number(o, "booking_id");
        if (id == null) throw new JSONException("booking_id missing");
        b.bookingId = (int) Math.round(id);
        b.createdAt = Json.text(o, "created_at");
        b.status = Json.text(o, "status");
        b.deliveryStatus = Json.text(o, "delivery_status");
        b.deliveryStatusAt = Json.text(o, "delivery_status_at");
        b.assignedAt = Json.text(o, "assigned_at");
        b.lastChangedAt = Json.text(o, "last_changed_at");
        Double total = Json.number(o, "total_amount");
        b.totalAmount = total != null ? total : 0;
        Double sub = Json.number(o, "subtotal_amount");
        b.subtotalAmount = sub != null ? sub : b.totalAmount;
        JSONObject disc = Json.object(o, "discount");
        if (disc != null) {
            Double amt = Json.number(disc, "amount");
            b.discountAmount = amt != null ? amt : 0;
            b.discountType = Json.text(disc, "type");
            b.discountValue = Json.number(disc, "value");
            b.discountBy = Json.text(disc, "by");
            b.discountAt = Json.text(disc, "at");
        }
        b.createdVia = Json.text(o, "created_via");
        b.sequence = Json.integer(o, "sequence", 0);
        b.alreadyExisted = Boolean.TRUE.equals(Json.bool(o, "already_existed"));
        b.customerMatchedExisting = Boolean.TRUE.equals(Json.bool(o, "customer_matched_existing"));

        JSONObject c = Json.object(o, "customer");
        if (c != null) {
            Double cid = Json.number(c, "client_id");
            b.clientId = cid != null ? (int) Math.round(cid) : null;
            b.customerName = Json.text(c, "name");
            b.customerContact = Json.text(c, "contact");
            b.customerAddress = Json.text(c, "address");
        }
        JSONObject d = Json.object(o, "delivery");
        if (d != null) {
            b.deliveryAddress = Json.text(d, "address");
            b.deliveryLatitude = Json.number(d, "latitude");
            b.deliveryLongitude = Json.number(d, "longitude");
            b.schedule = Json.text(d, "schedule");
            b.instructions = Json.text(d, "instructions");
        }
        if (b.deliveryAddress == null) b.deliveryAddress = b.customerAddress;
        JSONObject dr = Json.object(o, "driver");
        if (dr != null) {
            b.driverUsername = Json.text(dr, "username");
            b.driverName = Json.text(dr, "name");
            b.vehicleId = Json.text(dr, "vehicle_id");
            b.vehicleType = Json.text(dr, "vehicle_type");
        }
        JSONArray items = o.has("items") && !o.isNull("items") ? o.optJSONArray("items") : null;
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject it = items.optJSONObject(i);
                if (it == null) continue;
                Item item = new Item();
                item.productId = Json.integer(it, "product_id", 0);
                item.productName = Json.text(it, "product_name");
                item.quantity = Json.integer(it, "quantity", 0);
                Double up = Json.number(it, "unit_price");
                Double st = Json.number(it, "subtotal");
                item.unitPrice = up != null ? up : 0;
                item.subtotal = st != null ? st : 0;
                b.items.add(item);
            }
        }
        JSONArray events = o.has("events") && !o.isNull("events") ? o.optJSONArray("events") : null;
        if (events != null) {
            for (int i = 0; i < events.length(); i++) {
                JSONObject e = events.optJSONObject(i);
                if (e == null) continue;
                Event ev = new Event();
                ev.at = Json.text(e, "at");
                ev.type = Json.text(e, "type");
                ev.from = Json.text(e, "from");
                ev.to = Json.text(e, "to");
                ev.bookingStatus = Json.text(e, "booking_status");
                ev.note = Json.text(e, "note");
                b.events.add(ev);
            }
        }
        return b;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "Booking #%d (%s/%s)", bookingId, status, deliveryStatus);
    }
}
