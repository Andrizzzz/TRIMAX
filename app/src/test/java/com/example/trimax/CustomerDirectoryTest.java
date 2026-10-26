package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/** Customer matching rules shared by the Clients screen and the New Booking customer picker. */
public class CustomerDirectoryTest {

    @Test
    public void samePhoneInDifferentFormatsIsTheSameCustomer() {
        assertEquals(CustomerDirectory.key("Ana", "0917-123-4567"), CustomerDirectory.key("Ana Cruz", "+63 917 123 4567"));
        assertEquals(CustomerDirectory.key("Ana", "09171234567"), CustomerDirectory.key("Ana", "(0917) 123 4567"));
    }

    @Test
    public void differentPhonesAreDifferentCustomers() {
        assertNotEquals(CustomerDirectory.key("Ana", "09171234567"), CustomerDirectory.key("Ana", "09181234567"));
    }

    @Test
    public void withoutAUsablePhoneTheNameDecides() {
        assertEquals(CustomerDirectory.key(" Ben Reyes ", ""), CustomerDirectory.key("ben reyes", null));
        assertEquals(CustomerDirectory.key("Ben Reyes", "123"), CustomerDirectory.key("ben reyes", "456"));
        assertNotEquals(CustomerDirectory.key("Ben", ""), CustomerDirectory.key("Ana", ""));
    }

    @Test
    public void digitsStripsEverythingElse() {
        assertEquals("639171234567", CustomerDirectory.digits("+63 (917) 123-4567"));
        assertEquals("", CustomerDirectory.digits(null));
    }

    private static ServerCustomer server(String name, String contact, String photo, Double lat, Double lng) {
        ServerCustomer c = new ServerCustomer();
        c.name = name;
        c.contact = contact;
        c.address = "Server address";
        c.photoPath = photo;
        c.latitude = lat;
        c.longitude = lng;
        return c;
    }

    @Test
    public void serverCustomersAreListedWithTheirPhotos() {
        java.util.List<DatabaseHelper.SavedCustomer> local = new java.util.ArrayList<>();
        local.add(new DatabaseHelper.SavedCustomer(1, "Ana", "0917 123 4567", "ana@x.ph", "", 0, 0, 2));
        java.util.List<ServerCustomer> onServer = java.util.Arrays.asList(
                server("Ana Cruz", "+63 917 123 4567", "u/customer_1_a.jpg", 14.8, 121.0),
                server("Ben", "0918 000 0000", null, null, null));

        java.util.List<DatabaseHelper.SavedCustomer> all = CustomerDirectory.list(local, null, onServer, "");
        assertEquals(2, all.size());
        DatabaseHelper.SavedCustomer ana = all.get(0);
        assertEquals("Ana", ana.getName());                     // the phone's own record is kept
        assertEquals("u/customer_1_a.jpg", ana.getPhotoPath());
        assertEquals(14.8, ana.getLatitude(), 1e-9);            // no pin on the phone: the server's
        assertEquals(true, ana.isOnServer());
        DatabaseHelper.SavedCustomer ben = all.get(1);           // saved on another phone, no booking yet
        assertEquals("Ben", ben.getName());
        assertEquals(null, ben.getPhotoPath());
        assertEquals(0, ben.getBookingCount());
        assertEquals(true, ben.isOnServer());

        assertEquals(1, CustomerDirectory.list(local, null, onServer, "0918").size());
        assertEquals(false, CustomerDirectory.list(local, null, null, "").get(0).isOnServer());
    }

    @Test
    public void onServerMatchesByPhone() {
        java.util.List<ServerCustomer> onServer = java.util.Collections.singletonList(
                server("Ana Cruz", "+63 917 123 4567", null, null, null));
        assertEquals(true, CustomerDirectory.onServer(onServer, "Ana", "0917-123-4567"));
        assertEquals(false, CustomerDirectory.onServer(onServer, "Ana", "0917-123-4568"));
        assertEquals(false, CustomerDirectory.onServer(null, "Ana", "0917-123-4567"));
    }
}
