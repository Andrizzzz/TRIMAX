package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;

/** Driver dashboard numbers (public.trimax_driver_summary) and the one greeting of both dashboards. */
public class DriverSummaryTest {

    private static final String JSON = "{\"today\": \"2026-10-30\", \"delivered_today\": 3, \"delivered_today_amount\": 1240.50,"
            + " \"delivered_month\": 21, \"delivered_month_amount\": 25180, \"to_deliver\": 2, \"pay_month\": 5200,"
            + " \"pay_records\": [{\"period_start\": \"2026-10-16\", \"period_end\": \"2026-10-31\", \"net_pay\": 5200,"
            + " \"status\": \"UNPAID\", \"paid_at\": null},"
            + " {\"period_start\": \"2026-10-01\", \"period_end\": \"2026-10-15\", \"net_pay\": 6300.00,"
            + " \"status\": \"PAID\", \"paid_at\": \"2026-10-16\"}]}";

    @Test
    public void readsTheServerSummary() throws Exception {
        DriverSummary s = DriverSummary.parse(JSON);
        assertEquals(3, s.deliveredToday);
        assertEquals(21, s.deliveredMonth);
        assertEquals(2, s.toDeliver);
        assertEquals(0, new BigDecimal("1240.50").compareTo(s.deliveredTodayAmount));
        assertEquals(0, new BigDecimal("5200").compareTo(s.payMonth));
        assertEquals(2, s.payRecords.size());
        assertFalse(s.payRecords.get(0).paid());
        assertTrue(s.payRecords.get(1).paid());
    }

    @Test
    public void payLinesAndAmounts() throws Exception {
        DriverSummary s = DriverSummary.parse(JSON);
        assertEquals("Oct 16 - Oct 31, 2026  ·  ₱5,200.00  ·  Not yet paid", s.payRecords.get(0).line());
        assertEquals("Oct 1 - Oct 15, 2026  ·  ₱6,300.00  ·  Paid Oct 16", s.payRecords.get(1).line());
        assertEquals("₱5,200", DriverSummary.pesoShort(new BigDecimal("5200.00")));
        assertEquals("₱1,240.50", DriverSummary.pesoShort(new BigDecimal("1240.50")));
        assertEquals("₱0", DriverSummary.pesoShort(null));
    }

    @Test
    public void anEmptyOrOlderAnswerIsZero() throws Exception {
        DriverSummary s = DriverSummary.parse("{}");
        assertEquals(0, s.deliveredToday);
        assertEquals(0, s.payMonth.signum());
        assertTrue(s.payRecords.isEmpty());
    }

    @Test
    public void oneGreetingForTheWholeDay() {
        assertEquals("Good morning", Greeting.forHour(0));
        assertEquals("Good morning", Greeting.forHour(11));
        assertEquals("Good afternoon", Greeting.forHour(12));
        assertEquals("Good afternoon", Greeting.forHour(16));
        assertEquals("Good evening", Greeting.forHour(17));
        assertEquals("Good evening", Greeting.forHour(23));
        assertEquals("Juan Santos 👋", Greeting.nameLine(" Juan Santos "));
        assertEquals("👋", Greeting.nameLine(null));
    }
}
