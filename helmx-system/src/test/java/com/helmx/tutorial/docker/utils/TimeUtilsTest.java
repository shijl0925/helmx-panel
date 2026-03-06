package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilsTest {

    // ---- formatNanoseconds ----

    @Test
    void formatNanoseconds_milliseconds_returnsMsFormat() {
        // 500 ms = 500_000_000 ns
        assertEquals("500ms", TimeUtils.formatNanoseconds(500_000_000L));
    }

    @Test
    void formatNanoseconds_zero_returnsZeroMs() {
        assertEquals("0ms", TimeUtils.formatNanoseconds(0));
    }

    @Test
    void formatNanoseconds_seconds_returnsSecondsFormat() {
        // 5 s 250 ms = 5_250_000_000 ns
        assertEquals("5s250ms", TimeUtils.formatNanoseconds(5_250_000_000L));
    }

    @Test
    void formatNanoseconds_minutes_returnsMinutesFormat() {
        // 1 min 5 s = 65_000_000_000 ns
        assertEquals("1m5s", TimeUtils.formatNanoseconds(65_000_000_000L));
    }

    @Test
    void formatNanoseconds_hours_returnsHoursFormat() {
        // 1h 1m 5s = 3665 s = 3_665_000_000_000 ns
        assertEquals("1h1m5s", TimeUtils.formatNanoseconds(3_665_000_000_000L));
    }

    // ---- formatNanosecondsDetailed ----

    @Test
    void formatNanosecondsDetailed_zero_returnsZeroMs() {
        assertEquals("0ms", TimeUtils.formatNanosecondsDetailed(0));
    }

    @Test
    void formatNanosecondsDetailed_milliseconds_returnsMsOnly() {
        // 250 ms = 250_000_000 ns
        assertEquals("250ms", TimeUtils.formatNanosecondsDetailed(250_000_000L));
    }

    @Test
    void formatNanosecondsDetailed_seconds_returnsSecondsOnly() {
        // exactly 5 s = 5_000_000_000 ns (no millis)
        assertEquals("5s", TimeUtils.formatNanosecondsDetailed(5_000_000_000L));
    }

    @Test
    void formatNanosecondsDetailed_minutesAndSeconds_returnsComposite() {
        // 1m 30s = 90_000_000_000 ns
        assertEquals("1m30s", TimeUtils.formatNanosecondsDetailed(90_000_000_000L));
    }

    @Test
    void formatNanosecondsDetailed_days_returnsDaysAndHours() {
        // 1d 2h = 93600 s = 93_600_000_000_000 ns
        assertEquals("1d2h", TimeUtils.formatNanosecondsDetailed(93_600_000_000_000L));
    }
}
