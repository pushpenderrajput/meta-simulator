package com.simulator.metawhatsapp.util;

import java.time.Instant;

/**
 * Meta represents all webhook timestamps as the current Unix epoch time,
 * in seconds, encoded as a JSON string (not a number) - e.g.
 * {@code "timestamp": "1690000000"}. This utility centralizes that format
 * so no call site invents its own representation.
 */
public final class TimestampUtil {

    private TimestampUtil() {
    }

    /**
     * Current time as a Unix epoch-seconds string, matching Meta's
     * webhook {@code timestamp} field format exactly.
     */
    public static String nowEpochSecondsString() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    /**
     * Given an instant, renders it as a Unix epoch-seconds string.
     */
    public static String epochSecondsString(Instant instant) {
        return String.valueOf(instant.getEpochSecond());
    }
}
