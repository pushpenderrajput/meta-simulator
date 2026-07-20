package com.simulator.metawhatsapp.util;

public final class PhoneNumberUtil {

    private PhoneNumberUtil() {
    }

    public static String toWaId(String rawPhoneNumber) {
        if (rawPhoneNumber == null) {
            return null;
        }
        return rawPhoneNumber.replaceAll("[^0-9]", "");
    }

    public static boolean isPlausiblePhoneNumber(String rawPhoneNumber) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            return false;
        }
        if (!rawPhoneNumber.matches("^[0-9+\\-() ]+$")) {
            return false;
        }

        String waId = toWaId(rawPhoneNumber);

        // E.164 standard numbers (excluding country code 0 prefixes)
        // are between 7 and 15 digits long.
        return waId.length() >= 7 && waId.length() <= 15;
    }
}