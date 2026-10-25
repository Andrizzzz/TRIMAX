package com.example.trimax;

import java.util.Locale;

/**
 * The shared status model, and how it maps onto the words the existing screens already use.
 *
 * Booking status (set by the administrator, stored in bookings.status):
 *   Pending -> Confirmed -> In Progress -> Completed, or Cancelled.
 * Driver progress (stored in bookings.delivery_status, changed only through the server):
 *   ASSIGNED -> STARTED (Start Delivery) -> DELIVERED (Drop Off / Complete Delivery), or FAILED once started.
 * STARTED needs a Confirmed booking and moves it to In Progress; DELIVERED completes it. PICKED_UP,
 * ON_THE_WAY and ARRIVED are not steps any more: rows that older app builds left there can only be
 * completed or failed. Duty start/end never changes a delivery. The server enforces every rule
 * (sql/20261025_01_driver_started_delivered.sql in the desktop repository); this class only decides what
 * to show and which button to offer.
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
    /** Older app builds only. */
    public static final String PICKED_UP = "PICKED_UP";
    /** Older app builds only. */
    public static final String ON_THE_WAY = "ON_THE_WAY";
    /** Older app builds only. */
    public static final String ARRIVED = "ARRIVED";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";

    private DeliveryProgress() {
    }

    /** Same rule as trimax_private.delivery_transition_allowed on the server. */
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

    /**
     * Driver has left with the goods and has not finished: Drop Off / Complete Delivery (and "mark failed")
     * are available, and duty cannot end yet.
     */
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
        /** The button opens the proof-of-delivery screen, which sends STARTED -> DELIVERED. */
        public final boolean opensProof;
        public final boolean enabled;

        NextStep(String label, String target, boolean opensProof, boolean enabled) {
            this.label = label;
            this.target = target;
            this.opensProof = opensProof;
            this.enabled = enabled;
        }
    }

    public static final String START_DELIVERY = "START DELIVERY";
    public static final String DROP_OFF = "DROP OFF / COMPLETE DELIVERY";

    /** The driver's main button: Start Delivery on an assigned delivery, Drop Off on a started one. */
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
                return new NextStep(START_DELIVERY, STARTED, false, true);
            case STARTED:
            case PICKED_UP:      // older app builds
            case ON_THE_WAY:     // older app builds
            case ARRIVED:        // older app builds
                // DELIVERED is sent from the proof screen (drop-off photo + recipient's name).
                return new NextStep(DROP_OFF, null, true, true);
            case FAILED:     return new NextStep("DELIVERY FAILED – ADMIN WILL REVIEW", null, false, false);
            default:         return new NextStep("STATUS: " + code, null, false, false);
        }
    }

    /**
     * Text to show when the server refuses a driver step. Its messages are written for users; one case gets
     * more help: a refused step from Start Delivery or Drop Off means the server's delivery rules do not match
     * this app (the database update for the STARTED -> DELIVERED flow is missing), which only the office can fix.
     */
    public static String refusalMessage(String serverMessage) {
        String m = serverMessage == null ? "" : serverMessage.trim();
        if (m.startsWith("A delivery cannot go from ")) {
            return m + " The server's delivery rules do not match this app version. Tell the office "
                    + "(the TRIMAX database needs the latest delivery-status update).";
        }
        return m.isEmpty() ? "The server refused this change." : m;
    }
}
