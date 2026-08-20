package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.client.WebhookClient;
import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.RetriableDlr;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlrQueueService {

    private final WebhookClient webhookClient;
    private final StatsService statsService;

    // 10M item capacity to support massive continuous load
    private final BlockingQueue<RetriableDlr> dlrQueue = new LinkedBlockingQueue<>(10_000_000);

    private static final int MAX_REQUEUE_ATTEMPTS = 5;
    private ExecutorService dispatchWorkers;
    private volatile boolean running = true;

    public void enqueueDlr(String callbackUrl, MetaWebhookPayload payload) {
        enqueueDlr(new RetriableDlr(callbackUrl, payload));
    }

    private void enqueueDlr(RetriableDlr item) {
        // High-concurrency offer with immediate fallback tracking
        boolean added = dlrQueue.offer(item);
        if (added) {
            statsService.incrementDlrEnqueued();
        } else {
            log.error("❌ Critical: DLR Buffer Queue is completely saturated! Target: {}", item.getCallbackUrl());
        }
    }

    @PostConstruct
    public void startDlrConsumers() {
        int workerCount = Math.max(8, Runtime.getRuntime().availableProcessors() * 4);
        this.dispatchWorkers = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            dispatchWorkers.submit(this::processQueue);
        }
    }

    private void processQueue() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                RetriableDlr item = dlrQueue.poll(50, TimeUnit.MILLISECONDS);
                if (item != null) {
                    webhookClient.sendWebhook(item.getCallbackUrl(), item.getPayload())
                            .onErrorResume(error -> {
                                int attempts = item.incrementAttempt();
                                if (attempts <= MAX_REQUEUE_ATTEMPTS) {
                                    enqueueDlr(item);
                                } else {
                                    statsService.incrementDlrDiscarded();
                                    log.error("❌ Max retry attempts ({}) exhausted for {}", MAX_REQUEUE_ATTEMPTS, item.getCallbackUrl());
                                }
                                return Mono.empty();
                            })
                            .subscribe();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected worker exception", e);
            }
        }
    }

    public int getQueueSize() {
        return dlrQueue.size();
    }

    @PreDestroy
    public void shutdown() {
        this.running = false;
        if (dispatchWorkers != null) {
            dispatchWorkers.shutdownNow();
        }
    }
}