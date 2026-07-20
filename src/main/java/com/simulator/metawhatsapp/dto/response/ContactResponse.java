package com.simulator.metawhatsapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors the {@code contacts[]} entry Meta returns in a successful
 * send-message response:
 * <pre>
 * { "input": "16505555555", "wa_id": "16505555555" }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContactResponse(
        String input,
        String wa_id
) {
}
