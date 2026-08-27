package com.simulator.metawhatsapp.dto.webhook;

import lombok.Getter;

@Getter
public class RetriableDlr {
    private final String callbackUrl;
    private final MetaWebhookPayload payload;
    private int attempts;
    private long executeAfterEpochMillis;

    public RetriableDlr(String callbackUrl, MetaWebhookPayload payload) {
        this.callbackUrl = callbackUrl;
        this.payload = payload;
        this.attempts = 0;
        this.executeAfterEpochMillis = System.currentTimeMillis();
    }

    public int incrementAttempt(long delaySeconds) {
        this.attempts++;
        this.executeAfterEpochMillis = System.currentTimeMillis() + (delaySeconds * 1000L);
        return this.attempts;
    }

    public boolean isReady() {
        return System.currentTimeMillis() >= executeAfterEpochMillis;
    }
}