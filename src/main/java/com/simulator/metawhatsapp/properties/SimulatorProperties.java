package com.simulator.metawhatsapp.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        ApiVersionProperties apiVersion,
        AuthProperties auth,
        WebhookProperties webhook,
        EventsProperties events,
        DelaysProperties delays,
        ProbabilityProperties probability,
        PhoneNumberProperties phoneNumber
) {

    public record ApiVersionProperties(
            List<String> supported,
            String latest
    ) {}

    public record AuthProperties(
            List<String> validTokens,
            String verifyToken
    ) {}

    public record WebhookProperties(
            String callbackUrl,
            String senderRoutePairs,
            Map<String, String> senderRoutes,
            String defaultCallbackUrl,
            String whatsappBusinessAccountId,
            int timeoutSeconds
    ) {}

    public record EventsProperties(
            boolean sentEnabled,
            boolean deliveredEnabled,
            boolean readEnabled
    ) {}

    public record DelaysProperties(
            int sentSeconds,
            int deliveredSeconds,
            int readSeconds
    ) {}

    public record ProbabilityProperties(
            int delivered,
            int failed,
            int expired
    ) {}

    public record PhoneNumberProperties(
            String displayPhoneNumber,
            String phoneNumberId
    ) {}
}