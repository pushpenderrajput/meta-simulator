package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.dto.request.SendMessageRequest;
import com.simulator.metawhatsapp.dto.response.ContactResponse;
import com.simulator.metawhatsapp.dto.response.MessageIdResponse;
import com.simulator.metawhatsapp.dto.response.SendMessageResponse;
import com.simulator.metawhatsapp.generator.WamidGenerator;
import com.simulator.metawhatsapp.util.PhoneNumberUtil;
import com.simulator.metawhatsapp.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final WamidGenerator wamidGenerator;
    private final WebhookDispatcher webhookDispatcher;
    private final DlrQueueService dlrQueueService; // Injected to support queued/throttled DLR dispatching

    public SendMessageResponse acceptMessage(String phoneNumberId, SendMessageRequest request) {
        String waId = PhoneNumberUtil.toWaId(request.to());
        String wamid = wamidGenerator.generate();

        log.debug("Accepted type={} message on phoneNumberId={} -> to={} waId={} wamid={}",
                request.type(), phoneNumberId, request.to(), waId, wamid);

        // Schedule async DLR lifecycle. DLR payloads generated here will now be enqueued
        // safely into DlrQueueService rather than directly hitting WebClient all at once.
        webhookDispatcher.scheduleMessageLifecycle(wamid, waId);

        ContactResponse contact = new ContactResponse(request.to(), waId);
        MessageIdResponse message = MessageIdResponse.withoutStatus(wamid);

        return SendMessageResponse.of(contact, message);
    }
}