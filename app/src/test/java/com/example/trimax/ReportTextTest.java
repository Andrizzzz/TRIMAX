package com.example.trimax;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportTextTest {

    @Test
    public void completionRateIsNotZeroPercentWhenThereAreNoBookings() {
        assertEquals("Completion rate: n/a (no bookings in this period)", ReportText.completionRate(0, 0));
        assertEquals("Completion rate: 0% (0 of 3)", ReportText.completionRate(0, 3));
        assertEquals("Completion rate: 67% (2 of 3)", ReportText.completionRate(2, 3));
        assertEquals("Completion rate: 100% (4 of 4)", ReportText.completionRate(4, 4));
    }

    @Test
    public void csvCellsAreQuotedAndCannotRunFormulas() {
        assertEquals("\"\"", ReportText.csvCell(null));
        assertEquals("\"Ana Lopez\"", ReportText.csvCell("Ana Lopez"));
        assertEquals("\"Tondo, Manila\"", ReportText.csvCell("Tondo, Manila"));
        assertEquals("\"say \"\"hi\"\"\"", ReportText.csvCell("say \"hi\""));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", ReportText.csvCell("=HYPERLINK(\"x\")"));
        assertEquals("\"'+639171234567\"", ReportText.csvCell("+639171234567"));
        assertEquals("\"'@SUM(A1)\"", ReportText.csvCell("@SUM(A1)"));
    }

    @Test
    public void sharesOfAllBookingsAreComputed() {
        assertEquals("40% of all bookings", ReportText.shareOfTotal(2, 5));
        assertEquals("0% of all bookings", ReportText.shareOfTotal(0, 5));
        assertEquals("No bookings in this period", ReportText.shareOfTotal(0, 0));
    }
}
