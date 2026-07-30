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
    private final LinkedBlockingQueue<RetriableDlr> dlrQueue = new LinkedBlockingQueue<>(1_000_000);

    // INCREASED SPEED: Send 500 DLRs/sec (2ms delay) instead of 50 DLRs/sec
    private static final int TARGET_DLRS_PER_SECOND = 500;
    private static final int MAX_REQUEUE_ATTEMPTS = 5;

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
        long delayMicros = 1_000_000L / TARGET_DLRS_PER_SECOND; // 2000 microseconds (2ms)

        executor.scheduleAtFixedRate(() -> {
            try {
                RetriableDlr item = dlrQueue.poll();
                if (item != null) {
                    webhookClient.sendWebhook(item.getPayload())
                            .onErrorResume(error -> {
                                int attempts = item.incrementAttempt();
                                if (attempts <= MAX_REQUEUE_ATTEMPTS) {
                                    log.warn("⚠️ Receiver error. Re-queuing DLR (Attempt {}/{})",
                                            attempts, MAX_REQUEUE_ATTEMPTS);
                                    enqueueDlr(item);
                                } else {
                                    log.error("❌ Max attempts reached. Discarding DLR.");
                                }
                                return Mono.empty();
                            })
                            .subscribe();
                }
            } catch (Exception e) {
                log.error("Error in DLR consumer thread", e);
            }
        }, 0, delayMicros, TimeUnit.MICROSECONDS);

        log.info("🚀 High-Speed DLR Consumer started at {} DLR/sec.", TARGET_DLRS_PER_SECOND);
    }

    public int getQueueSize() {
        return dlrQueue.size();
    }
}