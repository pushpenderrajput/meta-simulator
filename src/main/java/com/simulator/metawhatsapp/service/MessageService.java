package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.dto.request.SendMessageRequest;
import com.simulator.metawhatsapp.dto.response.ContactResponse;
import com.simulator.metawhatsapp.dto.response.MessageIdResponse;
import com.simulator.metawhatsapp.dto.response.SendMessageResponse;
import com.simulator.metawhatsapp.generator.WamidGenerator;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import com.simulator.metawhatsapp.util.PhoneNumberUtil;
import com.simulator.metawhatsapp.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final WamidGenerator wamidGenerator;
    private final WebhookDispatcher webhookDispatcher;
    private final SimulatorProperties properties;

    public SendMessageResponse acceptMessage(String phoneNumberId, SendMessageRequest request) {
        String waId = PhoneNumberUtil.toWaId(request.to());
        String wamid = wamidGenerator.generate();

        // 1. Resolve the specific target URL for this sender ID
        String targetCallbackUrl = resolveCallbackUrl(phoneNumberId);

        log.info("📥 INBOUND REQUEST ACCEPTED -> senderId={} to={} wamid={} targetCallbackUrl={}",
                phoneNumberId, request.to(), wamid, targetCallbackUrl);

        // 2. Schedule DLR lifecycle targeted specifically to that callback URL
        webhookDispatcher.scheduleMessageLifecycle(wamid, waId, phoneNumberId, targetCallbackUrl);

        ContactResponse contact = new ContactResponse(request.to(), waId);
        MessageIdResponse message = MessageIdResponse.withoutStatus(wamid);

        return SendMessageResponse.of(contact, message);
    }

    private String resolveCallbackUrl(String phoneNumberId) {

        Map<String, String> routes = properties.webhook().senderRoutes();

        log.info("========== CALLBACK ROUTING ==========");
        log.info("Incoming Sender ID : {}", phoneNumberId);
        log.info("Configured Routes  : {}", routes);

        if (routes != null && routes.containsKey(phoneNumberId)) {

            String callbackUrl = routes.get(phoneNumberId);

            log.info("✅ ROUTE MATCHED");
            log.info("Sender ID         : {}", phoneNumberId);
            log.info("Callback URL      : {}", callbackUrl);
            log.info("======================================");

            return callbackUrl;
        }

        String defaultUrl = properties.webhook().defaultCallbackUrl();

        log.warn("⚠️ ROUTE NOT FOUND");
        log.warn("Sender ID         : {}", phoneNumberId);
        log.warn("Using Default URL : {}", defaultUrl);
        log.info("======================================");

        return defaultUrl;
    }
}