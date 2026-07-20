package com.simulator.metawhatsapp.dto.response;

/**
 * Mirrors Meta's {@code error.error_data} object, present on many
 * WhatsApp Cloud API error responses:
 * <pre>
 * "error_data": { "messaging_product": "whatsapp", "details": "..." }
 * </pre>
 */
public record ErrorData(
        String messaging_product,
        String details
) {
    public static ErrorData withDetails(String details) {
        return new ErrorData("whatsapp", details);
    }
}
