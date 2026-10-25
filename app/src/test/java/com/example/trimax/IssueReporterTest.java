package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** What the driver's SOS / problem reports send to the server (public.trimax_report_issue). */
public class IssueReporterTest {

    @Test
    public void serverBookingNumbersAreRecognised() {
        assertEquals(Integer.valueOf(12), IssueReporter.bookingNumber("12"));
        assertEquals(Integer.valueOf(12), IssueReporter.bookingNumber(" #12 "));
        assertEquals(Integer.valueOf(345), IssueReporter.bookingNumber("Booking #345"));
    }

    @Test
    public void phoneOnlyCodesAndBlanksAreSentWithoutABooking() {
        assertNull(IssueReporter.bookingNumber("BK-2026-00125"));
        assertNull(IssueReporter.bookingNumber(""));
        assertNull(IssueReporter.bookingNumber(null));
        assertNull(IssueReporter.bookingNumber("0"));
        assertNull(IssueReporter.bookingNumber("1234567890"));   // more than the server's integer ids
    }

    @Test
    public void messageStartsWithTheProblemType() {
        assertEquals("Vehicle breakdown: flat tyre", IssueReporter.message("Vehicle breakdown", " flat tyre ", false));
        assertEquals("Vehicle breakdown", IssueReporter.message("Vehicle breakdown", "", false));
    }

    @Test
    public void typeIsNotRepeatedWhenTheTextAlreadyNamesIt() {
        assertEquals("Reported: Customer not home", IssueReporter.message("Customer not home", "Reported: Customer not home", false));
        assertEquals("Emergency SOS from the mobile app.", IssueReporter.message("SOS", "Emergency SOS from the mobile app.", true));
    }

    @Test
    public void messageAlwaysFitsTheServerLimits() {
        assertTrue(IssueReporter.message(null, null, true).length() >= 3);
        assertTrue(IssueReporter.message("", "ok", false).length() >= 3);
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1500; i++) longText.append('x');
        assertEquals(1000, IssueReporter.message("Other", longText.toString(), false).length());
    }
}
