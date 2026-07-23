package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.client.WebhookClient;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlrQueueService {

    private final WebhookClient webhookClient;

    // High-capacity queue to buffer DLR payloads during load bursts
    private final LinkedBlockingQueue<MetaWebhookPayload> dlrQueue = new LinkedBlockingQueue<>(500000);

    // Dynamic rate throttler (e.g., send 300 DLRs per second max)
    private static final int MAX_DLRS_PER_SECOND = 300;

    /**
     * Enqueues a DLR payload instantly without blocking the inbound request thread.
     */
    public void enqueueDlr(MetaWebhookPayload payload) {
        boolean added = dlrQueue.offer(payload);
        if (!added) {
            log.error("DLR Buffer Queue is FULL! Payload dropped for WAMID: {}", payload);
        }
    }

    /**
     * Background daemon worker that steadily drains the queue.
     */
    @PostConstruct
    public void startDlrQueueConsumer() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        // Calculate interval in microseconds per DLR to maintain precise TPS
        long delayMicros = 1_000_000L / MAX_DLRS_PER_SECOND;

        executor.scheduleAtFixedRate(() -> {
            try {
                MetaWebhookPayload payload = dlrQueue.poll();
                if (payload != null) {
                    webhookClient.sendWebhook(payload);
                }
            } catch (Exception e) {
                log.error("Error processing queued DLR", e);
            }
        }, 0, delayMicros, TimeUnit.MICROSECONDS);

        log.info("DLR Queue Worker initialized. Throttled outbound DLR rate: {} DLRs/sec", MAX_DLRS_PER_SECOND);
    }

    /**
     * Helper metric to log remaining items in buffer.
     */
    public int getQueueSize() {
        return dlrQueue.size();
    }
}