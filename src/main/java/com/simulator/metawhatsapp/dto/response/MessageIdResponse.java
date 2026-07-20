package com.simulator.metawhatsapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors the {@code messages[]} entry Meta returns in a successful
 * send-message response:
 * <pre>
 * { "id": "wamid.HBgLMTY1MDUwNzY1MjAVAgARGBI5QTNDQTVCM0Q0Q0Q2RTY3RTcA", "message_status": "accepted" }
 * </pre>
 * {@code message_status} is only present on pacing-related responses
 * (e.g. {@code accepted}, {@code held_for_quality_assessment}) and is
 * omitted otherwise, matching Meta's actual behavior.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessageIdResponse(
        String id,
        String message_status
) {
    public static MessageIdResponse withoutStatus(String id) {
        return new MessageIdResponse(id, null);
    }

    public static MessageIdResponse withStatus(String id, String messageStatus) {
        return new MessageIdResponse(id, messageStatus);
    }
}
