package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BookingFilterTest {

    private static BookingRecord booking(int id, String status, String customer, String contact, String address) {
        BookingRecord b = new BookingRecord();
        b.bookingId = id;
        b.status = status;
        b.customerName = customer;
        b.customerContact = contact;
        b.deliveryAddress = address;
        return b;
    }

    private static final List<BookingRecord> ALL = Arrays.asList(
            booking(1, "Pending", "Aling Rosa", "0945 222 1111", "Citrus"),
            booking(2, "Confirmed", "Kainan", "+63 945 333 2222", "Kaypian"),
            booking(3, "In Progress", "Kainan", "09453332222", "Kaypian"),
            booking(4, "Completed", "Store Dos", "0917 333 4444", "Tungko"),
            booking(5, "Cancelled", "Aling Rosa", "0945 222 1111", "Citrus"));

    @Test
    public void chips() {
        assertEquals(5, BookingFilter.apply(ALL, "ALL", "").size());
        assertEquals(1, BookingFilter.apply(ALL, "PENDING", null).size());
        assertEquals("Confirmed includes In Progress", 2, BookingFilter.apply(ALL, "CONFIRMED", null).size());
        assertEquals(1, BookingFilter.apply(ALL, "COMPLETED", null).size());
        assertEquals(1, BookingFilter.apply(ALL, "cancelled", null).size());
        assertEquals(1, BookingFilter.count(ALL, "PENDING"));
        assertTrue(BookingFilter.apply(null, "ALL", null).isEmpty());
    }

    @Test
    public void search() {
        assertEquals(2, BookingFilter.apply(ALL, "ALL", "rosa").size());
        assertEquals(1, BookingFilter.apply(ALL, "ALL", "#4").size());
        assertEquals(2, BookingFilter.apply(ALL, "ALL", "kaypian").size());
        assertEquals(1, BookingFilter.apply(ALL, "PENDING", "rosa").size());
        assertEquals(0, BookingFilter.apply(ALL, "ALL", "nobody").size());
    }

    @Test
    public void customerMatchesByNumberInAnyFormat() {
        assertEquals(2, BookingFilter.forCustomer(ALL, "0945-333-2222", "Someone Else").size());
        assertEquals(2, BookingFilter.forCustomer(ALL, "+639452221111", null).size());
        List<BookingRecord> noNumber = new ArrayList<>(ALL);
        noNumber.add(booking(6, "Pending", "Walk-in Juan", null, "Muzon"));
        assertEquals(1, BookingFilter.forCustomer(noNumber, "", "walk-in juan").size());
        assertEquals("9453332222", BookingFilter.contactKey("+63 945 333 2222"));
        assertNull(BookingFilter.contactKey("12"));
    }
}
