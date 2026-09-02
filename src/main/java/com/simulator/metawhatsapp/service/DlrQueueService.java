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

    private final BlockingQueue<RetriableDlr> dlrQueue = new LinkedBlockingQueue<>(5_000_000);
    private final BlockingQueue<RetriableDlr> retryDelayedQueue = new LinkedBlockingQueue<>(1_000_000);

    private static final int MAX_RETRY_ATTEMPTS = 15;
    private static final long[] BACKOFF_DELAYS_SECONDS = {3, 6, 10, 15, 25, 35, 45, 60};

    private ExecutorService dispatchWorkers;
    private ScheduledExecutorService delayedRetryScheduler;
    private volatile boolean running = true;

    public void enqueueDlr(String callbackUrl, MetaWebhookPayload payload) {
        enqueueDlr(new RetriableDlr(callbackUrl, payload));
    }

    private void enqueueDlr(RetriableDlr item) {
        if (dlrQueue.offer(item)) {
            statsService.incrementDlrEnqueued();
        } else {
            log.error("❌ Critical: DLR Queue full! Dropping item for {}", item.getCallbackUrl());
        }
    }

    @PostConstruct
    public void startDlrConsumers() {
        int workerCount = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
        this.dispatchWorkers = Executors.newFixedThreadPool(workerCount);
        this.delayedRetryScheduler = Executors.newSingleThreadScheduledExecutor();

        for (int i = 0; i < workerCount; i++) {
            dispatchWorkers.submit(this::processQueue);
        }

        // Background scheduler to re-feed delayed retries back into the active queue
        delayedRetryScheduler.scheduleWithFixedDelay(this::requeueDelayedItems, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void processQueue() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                RetriableDlr item = dlrQueue.poll(50, TimeUnit.MILLISECONDS);
                if (item != null) {
                    webhookClient.sendWebhook(item.getCallbackUrl(), item.getPayload())
                            .onErrorResume(error -> {
                                int attemptIndex = Math.min(item.getAttempts(), BACKOFF_DELAYS_SECONDS.length - 1);
                                long delaySeconds = BACKOFF_DELAYS_SECONDS[attemptIndex];
                                int attempts = item.incrementAttempt(delaySeconds);

                                if (attempts <= MAX_RETRY_ATTEMPTS) {
                                    log.warn("⚠️ Receiver timeout/error at {}. Scheduling retry (Attempt {}/{}) in {}s",
                                            item.getCallbackUrl(), attempts, MAX_RETRY_ATTEMPTS, delaySeconds);
                                    retryDelayedQueue.offer(item);
                                } else {
                                    statsService.incrementDlrDiscarded();
                                    log.error("❌ Max attempts ({}) exhausted for {}. Discarding DLR.",
                                            MAX_RETRY_ATTEMPTS, item.getCallbackUrl());
                                }
                                return Mono.empty();
                            })
                            .subscribe();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Worker processing exception", e);
            }
        }
    }

    private void requeueDelayedItems() {
        int size = retryDelayedQueue.size();
        for (int i = 0; i < size; i++) {
            RetriableDlr item = retryDelayedQueue.peek();
            if (item != null && item.isReady()) {
                retryDelayedQueue.poll();
                dlrQueue.offer(item);
            }
        }
    }

    public int getQueueSize() {
        return dlrQueue.size() + retryDelayedQueue.size();
    }
    public synchronized void clearAllQueues() {
        int clearedActive = dlrQueue.size();
        int clearedRetry = retryDelayedQueue.size();
        dlrQueue.clear();
        retryDelayedQueue.clear();
        log.warn("🧹 DlrQueueService cleared: {} active items, {} retry items purged.", clearedActive, clearedRetry);
    }

    @PreDestroy
    public void shutdown() {
        this.running = false;
        if (dispatchWorkers != null) dispatchWorkers.shutdownNow();
        if (delayedRetryScheduler != null) delayedRetryScheduler.shutdownNow();
    }
}