package com.simulator.metawhatsapp.generator;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class FbTraceIdGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MIN_LENGTH = 11;
    private static final int MAX_LENGTH = 22;

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = MIN_LENGTH + random.nextInt(MAX_LENGTH - MIN_LENGTH + 1);
        char[] buf = new char[length];

        // Always start with an alphabetic character
        buf[0] = ALPHABET.charAt(random.nextInt(52));

        for (int i = 1; i < length; i++) {
            if (random.nextInt(20) == 0) {
                buf[i] = '_';
            } else {
                buf[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
            }
        }
        return new String(buf);
    }
}