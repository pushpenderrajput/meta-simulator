package com.simulator.metawhatsapp.dto.webhook;

import lombok.Getter;

@Getter
public class RetriableDlr {
    private final MetaWebhookPayload payload;
    private int attemptCount = 0;

    public RetriableDlr(MetaWebhookPayload payload) {
        this.payload = payload;
    }

    public int incrementAttempt() {
        return ++this.attemptCount;
    }
}