package com.simulator.metawhatsapp.generator;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates WhatsApp Message IDs ("WAMIDs") in the same visual format Meta
 * uses: the literal prefix {@code wamid.} followed by a base64-encoded
 * opaque binary blob, e.g.
 * <pre>
 * wamid.HBgLMTY1MDUwNzY1MjAVAgARGBI5QTNDQTVCM0Q0Q0Q2RTY3RTcA
 * </pre>
 *
 * <p>Meta's internal binary encoding scheme for this blob is not publicly
 * documented, so this generator does not attempt to reverse engineer it.
 * Instead it produces a random, sufficiently long, base64url-safe payload
 * so that resulting IDs are indistinguishable in shape/length from real
 * WAMIDs to any consumer, while remaining unique per call.</p>
 */
@Component
public class WamidGenerator {

    private static final String PREFIX = "wamid.";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Produces a new, unique, Meta-shaped WAMID.
     */
    public String generate() {
        ByteBuffer buffer = ByteBuffer.allocate(32);
        UUID uuid = UUID.randomUUID();
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        buffer.putLong(System.currentTimeMillis());

        byte[] tail = new byte[8];
        RANDOM.nextBytes(tail);
        buffer.put(tail);

        String encoded = Base64.getEncoder().withoutPadding().encodeToString(buffer.array());
        return PREFIX + encoded;
    }
}
