package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The driver dashboard's numbers from the server (public.trimax_driver_summary, sql/20261029_03): deliveries
 * today and this month, what is still to deliver, and pay from the admin's Payroll.
 */
public final class DriverSummary {

    /** One payroll record of this driver (not voided). */
    public static final class Pay {
        public String periodStart, periodEnd, status, paidAt;
        public BigDecimal netPay = BigDecimal.ZERO;

        public boolean paid() {
            return "PAID".equals(status);
        }

        /** "Oct 1 - Oct 15, 2026  ·  ₱6,300.00  ·  Paid Oct 16" */
        public String line() {
            return period(periodStart, periodEnd) + "  ·  " + peso(netPay) + "  ·  "
                    + (paid() ? "Paid" + (paidAt == null ? "" : " " + shortDate(paidAt, false)) : "Not yet paid");
        }
    }

    public int deliveredToday, deliveredMonth, toDeliver;
    public BigDecimal deliveredTodayAmount = BigDecimal.ZERO, deliveredMonthAmount = BigDecimal.ZERO;
    public BigDecimal payMonth = BigDecimal.ZERO;
    public final List<Pay> payRecords = new ArrayList<>();

    public static DriverSummary parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        DriverSummary s = new DriverSummary();
        s.deliveredToday = o.optInt("delivered_today", 0);
        s.deliveredMonth = o.optInt("delivered_month", 0);
        s.toDeliver = o.optInt("to_deliver", 0);
        s.deliveredTodayAmount = money(o, "delivered_today_amount");
        s.deliveredMonthAmount = money(o, "delivered_month_amount");
        s.payMonth = money(o, "pay_month");
        JSONArray pays = o.optJSONArray("pay_records");
        if (pays != null) {
            for (int i = 0; i < pays.length(); i++) {
                JSONObject p = pays.optJSONObject(i);
                if (p == null) continue;
                Pay pay = new Pay();
                pay.periodStart = text(p, "period_start");
                pay.periodEnd = text(p, "period_end");
                pay.status = text(p, "status");
                pay.paidAt = text(p, "paid_at");
                pay.netPay = money(p, "net_pay");
                s.payRecords.add(pay);
            }
        }
        return s;
    }

    /** "₱1,200.00" */
    public static String peso(BigDecimal v) {
        return String.format(Locale.US, "₱%,.2f", v == null ? BigDecimal.ZERO : v);
    }

    /** "₱1,200" for the small stat (no centavos when there are none). */
    public static String pesoShort(BigDecimal v) {
        BigDecimal x = v == null ? BigDecimal.ZERO : v;
        return x.stripTrailingZeros().scale() <= 0 ? String.format(Locale.US, "₱%,d", x.longValue()) : peso(x);
    }

    private static BigDecimal money(JSONObject o, String key) {
        if (!o.has(key) || o.isNull(key)) return BigDecimal.ZERO;
        try {
            return new BigDecimal(o.get(key).toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String text(JSONObject o, String key) {
        return o.has(key) && !o.isNull(key) ? o.optString(key, null) : null;
    }

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    /** "2026-10-16" -> "Oct 16" (or "Oct 16, 2026" with the year). The text itself when it is not a date. */
    static String shortDate(String isoDate, boolean withYear) {
        if (isoDate == null || !isoDate.matches("\\d{4}-\\d{2}-\\d{2}.*")) return isoDate == null ? "" : isoDate;
        int m = Integer.parseInt(isoDate.substring(5, 7));
        int d = Integer.parseInt(isoDate.substring(8, 10));
        if (m < 1 || m > 12) return isoDate;
        return MONTHS[m - 1] + " " + d + (withYear ? ", " + isoDate.substring(0, 4) : "");
    }

    /** "Oct 1 - Oct 15, 2026" */
    static String period(String start, String end) {
        if (start == null || end == null) return shortDate(end != null ? end : start, true);
        if (start.equals(end)) return shortDate(end, true);
        return shortDate(start, false) + " - " + shortDate(end, true);
    }
}
