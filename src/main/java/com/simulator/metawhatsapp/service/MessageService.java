package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.dto.request.SendMessageRequest;
import com.simulator.metawhatsapp.dto.response.ContactResponse;
import com.simulator.metawhatsapp.dto.response.MessageIdResponse;
import com.simulator.metawhatsapp.dto.response.SendMessageResponse;
import com.simulator.metawhatsapp.generator.WamidGenerator;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import com.simulator.metawhatsapp.util.PhoneNumberUtil;
import com.simulator.metawhatsapp.webhook.WebhookDispatcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final WamidGenerator wamidGenerator;
    private final WebhookDispatcher webhookDispatcher;
    private final SimulatorProperties properties;

    private final Map<String, String> cachedRoutes = new ConcurrentHashMap<>();
    private String defaultCallbackUrl;

    @PostConstruct
    public void init() {
        Map<String, String> configuredRoutes = properties.webhook().senderRoutes();
        if (configuredRoutes != null) {
            cachedRoutes.putAll(configuredRoutes);
        }
        this.defaultCallbackUrl = properties.webhook().defaultCallbackUrl();
    }

    public SendMessageResponse acceptMessage(String phoneNumberId, SendMessageRequest request) {
        String waId = PhoneNumberUtil.toWaId(request.to());
        String wamid = wamidGenerator.generate();
        String targetCallbackUrl = cachedRoutes.getOrDefault(phoneNumberId, defaultCallbackUrl);

        webhookDispatcher.scheduleMessageLifecycle(wamid, waId, phoneNumberId, targetCallbackUrl);

        ContactResponse contact = new ContactResponse(request.to(), waId);
        MessageIdResponse message = MessageIdResponse.withoutStatus(wamid);

        return SendMessageResponse.of(contact, message);
    }
}