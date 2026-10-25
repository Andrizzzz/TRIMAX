package com.example.trimax;

/**
 * Small text rules for the Reports screen and its exports (plain Java, unit tested).
 */
public final class ReportText {

    private ReportText() {
    }

    /**
     * "Completion rate: 75% (3 of 4)". Completed / all bookings created in the period; with no bookings the
     * rate is undefined, so it says "n/a" instead of a misleading 0%.
     */
    public static String completionRate(int completed, int total) {
        if (total <= 0) return "Completion rate: n/a (no bookings in this period)";
        return "Completion rate: " + Math.round(completed * 100f / total) + "% (" + completed + " of " + total + ")";
    }

    /**
     * One CSV cell, always quoted. A value that a spreadsheet would run as a formula (starting with = + - @,
     * tab or carriage return) gets a leading apostrophe, so a customer name cannot run anything when the
     * file is opened in Excel or Sheets.
     */
    public static String csvCell(String value) {
        String safe = value == null ? "" : value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) safe = "'" + safe;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
