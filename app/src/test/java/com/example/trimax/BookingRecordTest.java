package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/** Fixtures are real responses of the TRIMAX database functions (captured from a test database). */
public class BookingRecordTest {

    static final String DRIVER_DETAIL = "{\"items\": [{\"quantity\": 5, \"subtotal\": 900.00, \"product_id\": 1, "
            + "\"unit_price\": 180.00, \"product_name\": \"Coke Case (24 Bottles)\"}], \"driver\": {\"name\": \"Dan Driver\", "
            + "\"username\": \"driver.dan\", \"vehicle_id\": \"TRK-001\", \"vehicle_type\": \"Truck\"}, \"events\": ["
            + "{\"at\": \"2026-10-23T15:34:14.460485+08:00\", \"to\": \"ON_THE_WAY\", \"from\": \"STARTED\", \"note\": null, "
            + "\"type\": \"delivery_status\", \"booking_status\": \"In Progress\"}, {\"at\": \"2026-10-23T15:34:14.446196+08:00\", "
            + "\"to\": null, \"from\": null, \"note\": \"1 product line(s) submitted from the mobile app\", \"type\": \"created\", "
            + "\"booking_status\": \"Pending\"}], \"status\": \"In Progress\", \"customer\": {\"name\": \"Kainan sa Kanto\", "
            + "\"address\": \"Kaypian Road, SJDM\", \"contact\": \"0945 333 2222\", \"client_id\": 6}, \"delivery\": {\"address\": "
            + "\"Kaypian Road, SJDM\", \"latitude\": 14.81, \"schedule\": \"2026-10-24 • 2:00 PM\", \"longitude\": 121.05, "
            + "\"instructions\": null}, \"booking_id\": 4, \"created_at\": \"2026-10-23T15:34:14.446196+08:00\", \"assigned_at\": "
            + "\"2026-10-23T15:34:14.451394+08:00\", \"created_via\": \"mobile\", \"total_amount\": 900.00, \"delivery_status\": "
            + "\"ON_THE_WAY\", \"last_changed_at\": \"2026-10-23T15:34:14.460485+08:00\", \"delivery_status_at\": "
            + "\"2026-10-23T15:34:14.460485+08:00\"}";

    static final String AGENT_NEW = "{\"items\": [{\"quantity\": 2, \"subtotal\": 360.00, \"product_id\": 1, \"unit_price\": 180.00, "
            + "\"product_name\": \"Coke Case (24 Bottles)\"}, {\"quantity\": 1, \"subtotal\": 180.00, \"product_id\": 2, \"unit_price\": 180.00, "
            + "\"product_name\": \"Sprite Case (24 Bottles)\"}], \"driver\": null, \"events\": null, \"status\": \"Pending\", "
            + "\"customer\": {\"name\": \"New Customer\", \"address\": \"Muzon\", \"contact\": \"0918 777 8888\", \"client_id\": 4}, "
            + "\"delivery\": {\"address\": \"Blk 1 Lot 2, Muzon, SJDM\", \"latitude\": 14.81, \"schedule\": \"ASAP\", \"longitude\": 121.05, "
            + "\"instructions\": \"Gate code 1234\"}, \"booking_id\": 2, \"created_at\": \"2026-10-23T15:16:01.1+08:00\", "
            + "\"assigned_at\": null, \"created_via\": \"mobile\", \"total_amount\": 540.00, \"delivery_status\": null, "
            + "\"last_changed_at\": \"2026-10-23T15:16:01.1+08:00\", \"delivery_status_at\": null, \"already_existed\": false, "
            + "\"customer_matched_existing\": false}";

    @Test
    public void parsesADriverDeliveryWithHistory() throws Exception {
        BookingRecord b = BookingRecord.parse(DRIVER_DETAIL);
        assertEquals(4, b.bookingId);
        assertEquals("In Progress", b.status);
        assertEquals("ON_THE_WAY", b.deliveryStatus);
        assertEquals(900.00, b.totalAmount, 0.001);
        assertEquals("Kainan sa Kanto", b.customerName);
        assertEquals("0945 333 2222", b.customerContact);
        assertEquals(Integer.valueOf(6), b.clientId);
        assertEquals(14.81, b.deliveryLatitude, 1e-9);
        assertEquals("driver.dan", b.driverUsername);
        assertEquals("TRK-001", b.vehicleId);
        assertEquals(1, b.items.size());
        assertEquals("Coke Case (24 Bottles) x5", b.itemsSummary());
        assertEquals(2, b.events.size());
        assertEquals("STARTED", b.events.get(0).from);
        assertNull("JSON null must not become the text \"null\"", b.events.get(0).note);
        assertNull(b.instructions);
    }

    @Test
    public void parsesAnUnassignedAgentBooking() throws Exception {
        BookingRecord b = BookingRecord.parse(AGENT_NEW);
        assertFalse(b.hasDriver());
        assertNull(b.deliveryStatus);
        assertTrue(b.events.isEmpty());
        assertEquals("Coke Case (24 Bottles) x2, Sprite Case (24 Bottles) x1", b.itemsSummary());
        assertEquals(3, b.itemCount());
        assertEquals("Blk 1 Lot 2, Muzon, SJDM", b.deliveryAddress);
        assertFalse(b.alreadyExisted);
    }

    @Test
    public void mapsOntoTheExistingScreensModel() throws Exception {
        Delivery forDriver = BookingRecord.parse(DRIVER_DETAIL).toDelivery(true);
        assertEquals("4", forDriver.getBookingId());
        assertEquals("ON THE WAY", forDriver.getStatus());
        assertEquals("Dan Driver", forDriver.getDriverId());
        assertEquals(BookingRecord.HUB_NAME, forDriver.getPickupLocation());
        assertEquals("Kaypian Road, SJDM", forDriver.getDestination());
        assertEquals(900.00, forDriver.getCurrentTotal(), 0.001);

        Delivery forAgent = BookingRecord.parse(AGENT_NEW).toDelivery(false);
        assertEquals("PENDING", forAgent.getStatus());
        assertNull("no fake driver for an unassigned booking", forAgent.getDriverId());
        assertEquals("ASAP", forAgent.getScheduledTime());
    }

    @Test
    public void parsesLists() throws Exception {
        List<BookingRecord> list = BookingRecord.parseList("[" + DRIVER_DETAIL + "," + AGENT_NEW + "]");
        assertEquals(2, list.size());
        assertTrue(BookingRecord.parseList("[]").isEmpty());
        assertTrue(BookingRecord.parseList("null").isEmpty());
    }

    @Test(expected = org.json.JSONException.class)
    public void rejectsARecordWithoutAnId() throws Exception {
        BookingRecord.parse("{\"status\": \"Pending\"}");
    }

    @Test
    public void parsesProductsAndAccount() throws Exception {
        List<Product> products = Product.parseList("[{\"price\": 180.00, \"stock\": 37, \"product_id\": 1, \"product_name\": "
                + "\"Coke Case (24 Bottles)\"}, {\"price\": 175.00, \"stock\": 0, \"product_id\": 3, \"product_name\": \"Royal Case\"}, "
                + "{\"price\": null, \"stock\": 1, \"product_id\": 9, \"product_name\": \"Broken\"}]");
        assertEquals("a product without a price is skipped", 2, products.size());
        assertEquals(37, products.get(0).stock);
        assertEquals(175.0, products.get(1).price, 0.001);

        MobileAccount a = MobileAccount.parse("{\"role\": \"DRIVER\", \"email\": \"dan@example.test\", \"username\": "
                + "\"driver.dan\", \"full_name\": \"Dan Driver\", \"contact_num\": null}");
        assertTrue(a.isDriver());
        assertEquals("Dan Driver", a.fullName);
        assertNull(a.contact);
        assertEquals("DRIVER", a.toUser().getRole());
    }

    @Test(expected = org.json.JSONException.class)
    public void rejectsAnUnknownRole() throws Exception {
        MobileAccount.parse("{\"role\": \"ADMIN\", \"username\": \"marc\"}");
    }
}
