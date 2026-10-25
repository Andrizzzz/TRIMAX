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
}
