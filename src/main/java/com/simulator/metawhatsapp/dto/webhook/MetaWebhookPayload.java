package com.simulator.metawhatsapp.dto.webhook;

import java.util.List;

public record MetaWebhookPayload(
        String object,
        List<EntryWrapper> entry
) {
    public static MetaWebhookPayload buildContainer(String wabaId, ChangeValue value) {
        EntryWrapper entry = new EntryWrapper(wabaId, List.of(ChangeWrapper.of(value)));
        return new MetaWebhookPayload("whatsapp_business_account", List.of(entry));
    }
}