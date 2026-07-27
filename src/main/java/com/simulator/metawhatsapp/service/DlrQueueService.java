package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.client.WebhookClient;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.RetriableDlr;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlrQueueService {

    private final WebhookClient webhookClient;

    // Queue holding retriable DLR items
    private final LinkedBlockingQueue<RetriableDlr> dlrQueue = new LinkedBlockingQueue<>(1_000_000);

    private static final int TARGET_DLRS_PER_SECOND = 50;
    private static final int MAX_REQUEUE_ATTEMPTS = 3; // Capped to prevent infinite 30-min loops

    public void enqueueDlr(MetaWebhookPayload payload) {
        enqueueDlr(new RetriableDlr(payload));
    }

    private void enqueueDlr(RetriableDlr item) {
        boolean added = dlrQueue.offer(item);
        if (!added) {
            log.error("❌ DLR Buffer Queue is FULL! Dropping DLR payload.");
        }
    }

    @PostConstruct
    public void startDlrQueueConsumer() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        long delayMillis = 1000L / TARGET_DLRS_PER_SECOND; // ~20ms delay

        executor.scheduleAtFixedRate(() -> {
            try {
                RetriableDlr item = dlrQueue.poll();
                if (item != null) {
                    webhookClient.sendWebhook(item.getPayload())
                            .onErrorResume(error -> {
                                int attempts = item.incrementAttempt();
                                if (attempts <= MAX_REQUEUE_ATTEMPTS) {
                                    log.warn("⚠️ Receiver refused connection. Re-queuing DLR (Attempt {}/{})",
                                            attempts, MAX_REQUEUE_ATTEMPTS);
                                    enqueueDlr(item);
                                } else {
                                    log.error("❌ Max re-queue attempts ({}) reached. Discarding DLR to prevent infinite loop.",
                                            MAX_REQUEUE_ATTEMPTS);
                                }
                                return Mono.empty(); // Safely consumes error to prevent onErrorDropped
                            })
                            .subscribe(); // Clean subscription with no unhandled errors
                }
            } catch (Exception e) {
                log.error("Error in DLR queue processor thread", e);
            }
        }, 0, delayMillis, TimeUnit.MILLISECONDS);

        log.info("🚀 Resilient DLR Queue Consumer started at {} DLR/sec limit.", TARGET_DLRS_PER_SECOND);
    }

    public int getQueueSize() {
        return dlrQueue.size();
    }
}