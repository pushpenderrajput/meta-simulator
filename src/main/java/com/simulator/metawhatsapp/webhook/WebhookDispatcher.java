package com.simulator.metawhatsapp.webhook;

import com.simulator.metawhatsapp.dto.webhook.ChangeValue;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.Metadata;
import com.simulator.metawhatsapp.dto.webhook.StatusDetail;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import com.simulator.metawhatsapp.service.DlrQueueService;
import com.simulator.metawhatsapp.util.TimestampUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final ThreadPoolTaskScheduler webhookTaskScheduler;
    private final DlrQueueService dlrQueueService; // Injected queue service for controlled DLR dispatch
    private final SimulatorProperties properties;

    /**
     * Entry point to trigger the asynchronous, staggered lifecycle of a simulated message.
     * Schedules each enabled DLR stage out-of-band based on configured delays.
     *
     * @param wamid       The generated WhatsApp Message ID.
     * @param recipientId The normalized destination phone number (wa_id).
     */
    public void scheduleMessageLifecycle(String wamid, String recipientId) {
        log.debug("Scheduling lifecycle stages for wamid={} to recipientId={}", wamid, recipientId);

        // 1. Schedule "sent" event
        if (properties.events().sentEnabled()) {
            Instant sentTime = Instant.now().plusSeconds(properties.delays().sentSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, "sent"), sentTime);
            log.trace("Scheduled 'sent' status execution at {} for wamid={}", sentTime, wamid);
        }

        // 2. Schedule "delivered" event
        if (properties.events().deliveredEnabled()) {
            Instant deliveredTime = Instant.now().plusSeconds(properties.delays().deliveredSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, "delivered"), deliveredTime);
            log.trace("Scheduled 'delivered' status execution at {} for wamid={}", deliveredTime, wamid);
        }

        // 3. Schedule "read" event
        if (properties.events().readEnabled()) {
            Instant readTime = Instant.now().plusSeconds(properties.delays().readSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, "read"), readTime);
            log.trace("Scheduled 'read' status execution at {} for wamid={}", readTime, wamid);
        }
    }

    /**
     * Assembles the wire-compatible Meta status JSON container payload and hands it over
     * to the DlrQueueService for rate-limited, zero-loss background delivery.
     */
    private void dispatchStatus(String wamid, String recipientId, String statusName) {
        log.debug("Triggering async DLR step: status={} for wamid={} to recipient={}", statusName, wamid, recipientId);

        // Map configuration phone identity values
        Metadata metadata = new Metadata(
                properties.phoneNumber().displayPhoneNumber(),
                properties.phoneNumber().phoneNumberId()
        );

        // Setup individual status array component
        StatusDetail statusDetail = new StatusDetail(
                wamid,
                statusName,
                TimestampUtil.nowEpochSecondsString(),
                recipientId,
                null // Phase 4: Error handling details will populate here
        );

        // Construct the Meta payload envelope
        ChangeValue changeValue = new ChangeValue(
                "whatsapp",
                metadata,
                List.of(statusDetail)
        );

        MetaWebhookPayload payload = MetaWebhookPayload.buildContainer(
                properties.webhook().whatsappBusinessAccountId(),
                changeValue
        );

        // Enqueue into in-memory queue buffer instead of firing HTTP directly
        dlrQueueService.enqueueDlr(payload);
    }
}