package com.simulator.metawhatsapp.dto.webhook;

import lombok.Getter;

@Getter
public class RetriableDlr {
    private final String callbackUrl;
    private final MetaWebhookPayload payload;
    private int attemptCount = 0;

    public RetriableDlr(String callbackUrl, MetaWebhookPayload payload) {
        this.callbackUrl = callbackUrl;
        this.payload = payload;
    }

    public int incrementAttempt() {
        return ++this.attemptCount;
    }
}