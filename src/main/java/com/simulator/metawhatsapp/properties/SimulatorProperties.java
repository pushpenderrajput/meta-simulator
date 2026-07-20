package com.simulator.metawhatsapp.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly typed, immutable binding of the {@code simulator.*} configuration tree
 * in {@code application.yml}. Using a record hierarchy keeps configuration
 * immutable and makes every tunable knob of the simulator explicit and
 * self-documenting in one place.
 */
@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        ApiVersion apiVersion,
        Auth auth,
        Webhook webhook,
        Delays delays,
        Events events,
        Probability probability,
        PhoneNumber phoneNumber
) {

    /** Supported Graph API versions, e.g. v20.0 .. v23.0, and which one is "latest". */
    public record ApiVersion(List<String> supported, String latest) {
    }

    /** Bearer token allow-list and the webhook subscription verify token. */
    public record Auth(List<String> validTokens, String verifyToken) {
    }

    /** Outbound webhook (DLR) delivery target and retry behavior. */
    public record Webhook(
            String callbackUrl,
            String whatsappBusinessAccountId,
            int timeoutSeconds,
            Retry retry
    ) {
        public record Retry(int maxAttempts, List<Integer> backoffSeconds) {
        }
    }

    /** Delay, in seconds, before each DLR stage is emitted after "accepted". */
    public record Delays(int sentSeconds, int deliveredSeconds, int readSeconds) {
    }

    /** Toggles for which DLR stages are simulated at all. */
    public record Events(boolean sentEnabled, boolean deliveredEnabled, boolean readEnabled) {
    }

    /** Percentage distribution of terminal outcomes. Should sum to 100. */
    public record Probability(int delivered, int failed, int expired) {
    }

    /** Simulated sending phone number identity, echoed back in webhook metadata. */
    public record PhoneNumber(String displayPhoneNumber, String phoneNumberId) {
    }
}
