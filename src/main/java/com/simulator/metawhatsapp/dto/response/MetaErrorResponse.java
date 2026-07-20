package com.simulator.metawhatsapp.dto.response;

/**
 * Exact shape of every Meta Graph API / WhatsApp Cloud API error response:
 * <pre>
 * { "error": { ... } }
 * </pre>
 */
public record MetaErrorResponse(
        ErrorDetail error
) {
    public static MetaErrorResponse of(ErrorDetail detail) {
        return new MetaErrorResponse(detail);
    }
}
