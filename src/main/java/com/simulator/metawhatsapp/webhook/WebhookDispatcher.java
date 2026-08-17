package com.simulator.metawhatsapp.webhook;

import com.simulator.metawhatsapp.dto.webhook.ChangeValue;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.Metadata;
import com.simulator.metawhatsapp.dto.webhook.StatusDetail;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import com.simulator.metawhatsapp.service.DlrQueueService;
import com.simulator.metawhatsapp.service.StatsService;
import com.simulator.metawhatsapp.util.TimestampUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final ThreadPoolTaskScheduler webhookTaskScheduler;
    private final DlrQueueService dlrQueueService;
    private final SimulatorProperties properties;
    private final StatsService statsService;

    @Value("${simulator.webhook.callback-urls}")
    private String rawCallbackUrls;

    private List<String> targetUrls;

    @PostConstruct
    public void init() {
        // Parse and clean comma-separated URLs safely
        this.targetUrls = Arrays.stream(rawCallbackUrls.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .toList();

        log.info("🎯 WebhookDispatcher initialized with {} target URLs:", targetUrls.size());
        targetUrls.forEach(url -> log.info("   👉 Target: {}", url));
    }

    public void scheduleMessageLifecycle(String wamid, String recipientId) {
        log.debug("Scheduling lifecycle stages for wamid={} to recipientId={}", wamid, recipientId);

        if (properties.events().sentEnabled()) {
            Instant sentTime = Instant.now().plusSeconds(properties.delays().sentSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, "sent"), sentTime);
        }

        if (properties.events().deliveredEnabled()) {
            Instant deliveredTime = Instant.now().plusSeconds(properties.delays().deliveredSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, "delivered"), deliveredTime);
        }

        if (properties.events().readEnabled()) {
            Instant readTime = Instant.now().plusSeconds(properties.delays().readSeconds());
            webhookTaskScheduler.schedule(() -> dispatchStatus(wamid, recipientId, "read"), readTime);
        }
    }

    private void dispatchStatus(String wamid, String recipientId, String statusName) {
        log.info("🚀 DISPATCHING OUTBOUND DLR -> status={} wamid={}", statusName, wamid);

        statsService.incrementDlrStatus(statusName);

        Metadata metadata = new Metadata(
                properties.phoneNumber().displayPhoneNumber(),
                properties.phoneNumber().phoneNumberId()
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

        // Fan out DLR payload to ALL target callback URLs
        for (String url : targetUrls) {
            dlrQueueService.enqueueDlr(url, payload);
        }
    }
}