package com.example.trimax;

import java.util.Locale;

/**
 * The shared status model, and how it maps onto the words the existing screens already use.
 *
 * Booking status (set by the administrator, stored in bookings.status):
 *   Pending -> Confirmed -> In Progress -> Completed, or Cancelled.
 * Driver progress (stored in bookings.delivery_status, changed only through the server):
 *   ASSIGNED -> STARTED -> DELIVERED (drop-off photo + recipient), or FAILED once started.
 * STARTED needs a Confirmed booking and moves it to In Progress; DELIVERED completes it. The server
 * enforces every rule; this class only decides what to show and which button to offer.
 * PICKED_UP, ON_THE_WAY and ARRIVED are only on rows saved by older app builds; such a delivery can
 * still be finished (DELIVERED) or failed, but nothing moves into those steps any more.
 */
public final class DeliveryProgress {

    // Booking status (administrator)
    public static final String PENDING = "Pending";
    public static final String CONFIRMED = "Confirmed";
    public static final String IN_PROGRESS = "In Progress";
    public static final String COMPLETED = "Completed";
    public static final String CANCELLED = "Cancelled";

    // Driver progress (server codes)
    public static final String ASSIGNED = "ASSIGNED";
    public static final String STARTED = "STARTED";
    public static final String PICKED_UP = "PICKED_UP";
    public static final String ON_THE_WAY = "ON_THE_WAY";
    public static final String ARRIVED = "ARRIVED";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";

    private DeliveryProgress() {
    }

    /**
     * Same rule as trimax_private.delivery_transition_allowed on the live server: a delivery must be
     * started before it can be delivered or failed (the server answers TM422 to ASSIGNED -> DELIVERED).
     */
    public static boolean canTransition(String from, String to) {
        if (from == null || to == null) return false;
        switch (from) {
            case ASSIGNED:   return STARTED.equals(to);
            case STARTED:
            case PICKED_UP:
            case ON_THE_WAY:
            case ARRIVED:    return DELIVERED.equals(to) || FAILED.equals(to);
            default:         return false;
        }
    }

    /** Driver has left with the goods and has not finished. */
    public static boolean isUnderWay(String deliveryStatus) {
        return STARTED.equals(deliveryStatus) || PICKED_UP.equals(deliveryStatus)
                || ON_THE_WAY.equals(deliveryStatus) || ARRIVED.equals(deliveryStatus);
    }

    /**
     * Status text for the driver screens, in the words they already used
     * (ASSIGNED, STARTED, PICKED UP, ON THE WAY, ARRIVED AT DESTINATION, DELIVERED, FAILED, CANCELLED).
     */
    public static String driverDisplayStatus(String bookingStatus, String deliveryStatus) {
        if (CANCELLED.equals(bookingStatus)) return "CANCELLED";
        String code = deliveryStatus == null ? ASSIGNED : deliveryStatus;
        if (COMPLETED.equals(bookingStatus) && !DELIVERED.equals(code)) return "COMPLETED";
        switch (code) {
            case PICKED_UP:  return "PICKED UP";
            case ON_THE_WAY: return "ON THE WAY";
            case ARRIVED:    return "ARRIVED AT DESTINATION";
            default:         return code;            // ASSIGNED, STARTED, DELIVERED, FAILED
        }
    }

    /** Server code for a driver display status, or null if it is not a driver step. */
    public static String serverCode(String displayStatus) {
        if (displayStatus == null) return null;
        switch (displayStatus.trim().toUpperCase(Locale.US)) {
            case "ASSIGNED":               return ASSIGNED;
            case "STARTED":                return STARTED;
            case "PICKED UP":
            case "PICKED_UP":              return PICKED_UP;
            case "ON THE WAY":
            case "ON_THE_WAY":             return ON_THE_WAY;
            case "ARRIVED AT DESTINATION":
            case "ARRIVED":                return ARRIVED;
            case "DELIVERED":              return DELIVERED;
            case "FAILED":                 return FAILED;
            default:                       return null;
        }
    }

    /** Status text for the agent screens: PENDING, CONFIRMED, IN PROGRESS, COMPLETED, CANCELLED. */
    public static String agentDisplayStatus(String bookingStatus) {
        if (bookingStatus == null) return "PENDING";
        return bookingStatus.trim().toUpperCase(Locale.US);
    }

    /** What the driver's main button should do for this delivery. */
    public static final class NextStep {
        public final String label;
        /** Server code to move to, or null when the button opens another screen or is disabled. */
        public final String target;
        /** The button opens the proof-of-delivery screen (ARRIVED -> DELIVERED). */
        public final boolean opensProof;
        public final boolean enabled;

        NextStep(String label, String target, boolean opensProof, boolean enabled) {
            this.label = label;
            this.target = target;
            this.opensProof = opensProof;
            this.enabled = enabled;
        }
    }

    /** The button labels are the ones the delivery details screen already used. */
    public static NextStep nextStep(String bookingStatus, String deliveryStatus) {
        if (CANCELLED.equals(bookingStatus)) return new NextStep("CANCELLED BY ADMIN", null, false, false);
        String code = deliveryStatus == null ? ASSIGNED : deliveryStatus;
        if (DELIVERED.equals(code) || COMPLETED.equals(bookingStatus)) {
            return new NextStep("✓ DELIVERY COMPLETED", null, false, false);
        }
        switch (code) {
            case ASSIGNED:
                if (PENDING.equals(bookingStatus)) {
                    return new NextStep("WAITING FOR ADMIN CONFIRMATION", null, false, false);
                }
                return new NextStep("START DELIVERY", STARTED, false, true);
            case STARTED:
            case PICKED_UP:
            case ON_THE_WAY:
            case ARRIVED:    return new NextStep("DROP-OFF PHOTO & PROOF OF DELIVERY", null, true, true);
            case FAILED:     return new NextStep("DELIVERY FAILED – ADMIN WILL REVIEW", null, false, false);
            default:         return new NextStep("DROP-OFF PHOTO & PROOF OF DELIVERY", null, true, true);
        }
    }
}
