package com.example.trimax;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IsoTimeTest {

    @Test
    public void parsesServerTimestamps() {
        // 2026-10-23T07:34:14.460Z
        long expected = 1792740854460L;
        assertEquals(expected, IsoTime.parseMillis("2026-10-23T15:34:14.460485+08:00"));
        assertEquals(expected, IsoTime.parseMillis("2026-10-23T07:34:14.460Z"));
        assertEquals(expected - 460, IsoTime.parseMillis("2026-10-23T07:34:14Z"));
        assertEquals(expected - 460, IsoTime.parseMillis("2026-10-23 07:34:14"));
        assertEquals(expected + 100, IsoTime.parseMillis("2026-10-23T15:34:14.56+0800"));
        assertEquals(expected - 460 + 9 * 3_600_000L, IsoTime.parseMillis("2026-10-23T07:34:14-09:00"));
    }

    @Test
    public void rejectsOtherText() {
        assertEquals(-1, IsoTime.parseMillis(null));
        assertEquals(-1, IsoTime.parseMillis(""));
        assertEquals(-1, IsoTime.parseMillis("1792740854460"));
        assertEquals(-1, IsoTime.parseMillis("ASAP"));
    }
}
