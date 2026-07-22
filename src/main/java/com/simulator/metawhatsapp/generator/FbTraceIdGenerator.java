package com.simulator.metawhatsapp.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates {@code fbtrace_id} values matching the shape Meta uses in error
 * responses, e.g. {@code "EJplcsCHuLu"} or {@code "AsnC2jHPNAav8JXp_7090vx"}:
 * a short alphanumeric token, occasionally containing an underscore, always
 * starting with a letter.
 */
@Component
public class FbTraceIdGenerator {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MIN_LENGTH = 11;
    private static final int MAX_LENGTH = 22;

    public String generate() {
        int length = MIN_LENGTH + RANDOM.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        StringBuilder sb = new StringBuilder(length);
        // Always start with a letter, as observed in real trace IDs.
        sb.append(ALPHABET.charAt(RANDOM.nextInt(52)));
        for (int i = 1; i < length; i++) {
            if (RANDOM.nextInt(20) == 0) {
                sb.append('_');
            } else {
                sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
        }
        return sb.toString();
    }
}
