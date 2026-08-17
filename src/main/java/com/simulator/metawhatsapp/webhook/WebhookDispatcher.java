package com.simulator.metawhatsapp.webhook;

import com.simulator.metawhatsapp.dto.webhook.ChangeValue;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.Metadata;
import com.simulator.metawhatsapp.dto.webhook.StatusDetail;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import com.simulator.metawhatsapp.service.DlrQueueService;
import com.simulator.metawhatsapp.service.StatsService;
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
    private final DlrQueueService dlrQueueService;
    private final SimulatorProperties properties;
    private final StatsService statsService;

    public void scheduleMessageLifecycle(String wamid, String recipientId, String senderId, String callbackUrl) {
        if (properties.events().sentEnabled()) {
            Instant sentTime = Instant.now().plusSeconds(properties.delays().sentSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, senderId, "sent", callbackUrl), sentTime);
        }

        if (properties.events().deliveredEnabled()) {
            Instant deliveredTime = Instant.now().plusSeconds(properties.delays().deliveredSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, senderId, "delivered", callbackUrl), deliveredTime);
        }

        if (properties.events().readEnabled()) {
            Instant readTime = Instant.now().plusSeconds(properties.delays().readSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, senderId, "read", callbackUrl), readTime);
        }
    }

    private void dispatchStatus(String wamid, String recipientId, String senderId, String statusName, String callbackUrl) {
        log.info("🚀 DISPATCHING OUTBOUND DLR -> status={} wamid={} senderId={} targetUrl={}",
                statusName, wamid, senderId, callbackUrl);

        statsService.incrementDlrStatus(statusName);

        Metadata metadata = new Metadata(
                properties.phoneNumber().displayPhoneNumber(),
                senderId
        );

        StatusDetail statusDetail = new StatusDetail(
                wamid,
                statusName,
                TimestampUtil.nowEpochSecondsString(),
                recipientId,
                null
        );

        ChangeValue changeValue = new ChangeValue(
                "whatsapp",
                metadata,
                List.of(statusDetail)
        );

        MetaWebhookPayload payload = MetaWebhookPayload.buildContainer(
                properties.webhook().whatsappBusinessAccountId(),
                changeValue
        );

        // Enqueue only to the specific caller's callback URL
        dlrQueueService.enqueueDlr(callbackUrl, payload);
    }
}