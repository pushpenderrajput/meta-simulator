package com.simulator.metawhatsapp.dto.response;

import java.util.List;

/**
 * Exact shape of Meta's successful send-message response:
 * <pre>
 * {
 *   "messaging_product": "whatsapp",
 *   "contacts": [ { "input": "...", "wa_id": "..." } ],
 *   "messages": [ { "id": "wamid...." } ]
 * }
 * </pre>
 * This response only indicates that the API accepted the request - it does
 * not indicate actual delivery. Delivery status is communicated later via
 * webhook callbacks, exactly as with real Meta.
 */
public record SendMessageResponse(
        String messaging_product,
        List<ContactResponse> contacts,
        List<MessageIdResponse> messages
) {
    public static SendMessageResponse of(ContactResponse contact, MessageIdResponse message) {
        return new SendMessageResponse("whatsapp", List.of(contact), List.of(message));
    }
}
