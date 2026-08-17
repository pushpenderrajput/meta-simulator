package com.simulator.metawhatsapp.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "simulator")
public record SimulatorProperties(
        WebhookProperties webhook,
        EventsProperties events,
        DelaysProperties delays,
        PhoneNumberProperties phoneNumber
) {
    public record WebhookProperties(
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

    public record PhoneNumberProperties(
            String displayPhoneNumber,
            String phoneNumberId
    ) {}
}