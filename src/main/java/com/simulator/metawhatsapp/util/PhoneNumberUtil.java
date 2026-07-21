package com.simulator.metawhatsapp.util;

import java.util.regex.Pattern;

public final class PhoneNumberUtil {

    private static final Pattern NON_DIGITS = Pattern.compile("[^0-9]");
    private static final Pattern VALID_CHARS = Pattern.compile("^[0-9+\\-() ]+$");

    private PhoneNumberUtil() {
    }

    public static String toWaId(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        // Uses pre-compiled Pattern matcher (significantly faster)
        return NON_DIGITS.matcher(rawPhoneNumber).replaceAll("");
    }

    public static boolean isPlausiblePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            return false;
        }
        if (!VALID_CHARS.matcher(rawPhoneNumber).matches()) {
            return false;
        }

        String waId = toWaId(rawPhoneNumber);
        return waId.length() >= 7 && waId.length() <= 15;
    }
}