package com.example.trimax;

import java.util.Calendar;

/** One greeting for the dashboards, from the phone's clock (the label and the name line used to disagree). */
final class Greeting {

    private Greeting() {
    }

    /** "Good morning" before 12:00, "Good afternoon" until 17:00, then "Good evening". */
    static String forHour(int hourOfDay) {
        if (hourOfDay < 12) return "Good morning";
        if (hourOfDay < 17) return "Good afternoon";
        return "Good evening";
    }

    static String now() {
        return forHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }

    /** The name line under the greeting: "Juan Santos 👋" (or just the wave when the name is unknown). */
    static String nameLine(String name) {
        String n = name == null ? "" : name.trim();
        return n.isEmpty() ? "👋" : n + " 👋";
    }
}
