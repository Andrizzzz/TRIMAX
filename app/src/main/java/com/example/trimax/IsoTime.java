package com.example.trimax;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the timestamps the server sends, e.g. "2026-10-23T15:34:14.460485+08:00". java.time is not
 * available on every supported Android version (minSdk 24), and SimpleDateFormat would read the
 * 6-digit fraction as milliseconds, so this does it by hand.
 */
public final class IsoTime {

    private static final Pattern ISO = Pattern.compile(
            "(\\d{4})-(\\d{2})-(\\d{2})[T ](\\d{2}):(\\d{2}):(\\d{2})(?:\\.(\\d{1,9}))?(Z|[+-]\\d{2}(?::?\\d{2})?)?");

    private IsoTime() {
    }

    /** Milliseconds since the epoch, or -1 if the text is not a timestamp. No zone given = UTC. */
    public static long parseMillis(String text) {
        if (text == null) return -1;
        Matcher m = ISO.matcher(text.trim());
        if (!m.matches()) return -1;
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.clear();
        c.set(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)) - 1, Integer.parseInt(m.group(3)),
                Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
        long millis = c.getTimeInMillis();
        String fraction = m.group(7);
        if (fraction != null) {
            String ms = (fraction + "00").substring(0, 3);
            millis += Integer.parseInt(ms);
        }
        String zone = m.group(8);
        if (zone != null && !"Z".equals(zone)) {
            int sign = zone.charAt(0) == '-' ? -1 : 1;
            String digits = zone.substring(1).replace(":", "");
            int hours = Integer.parseInt(digits.substring(0, 2));
            int minutes = digits.length() >= 4 ? Integer.parseInt(digits.substring(2, 4)) : 0;
            millis -= sign * (hours * 3_600_000L + minutes * 60_000L);
        }
        return millis;
    }
}
