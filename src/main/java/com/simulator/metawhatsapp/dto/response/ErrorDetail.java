package com.simulator.metawhatsapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors the {@code error} object Meta returns on every failed Graph API
 * call:
 * <pre>
 * {
 *   "message": "...",
 *   "type": "OAuthException",
 *   "code": 190,
 *   "error_data": { "messaging_product": "whatsapp", "details": "..." },
 *   "error_subcode": 460,
 *   "fbtrace_id": "EJplcsCHuLu"
 * }
 * </pre>
 * {@code error_data} and {@code error_subcode} are optional - not every
 * Meta error includes them, so they're omitted when null exactly like
 * real responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorDetail(
        String message,
        String type,
        int code,
        ErrorData error_data,
        Integer error_subcode,
        String fbtrace_id
) {
}
