package com.simulator.metawhatsapp.generator;

import org.springframework.stereotype.Component;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class WamidGenerator {

    private static final String PREFIX = "wamid.";

    public String generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        byte[] buffer = new byte[32];

        // Fill random bytes without lock contention
        random.nextBytes(buffer);

        String encoded = Base64.getEncoder().withoutPadding().encodeToString(buffer);
        return PREFIX + encoded;
    }
}