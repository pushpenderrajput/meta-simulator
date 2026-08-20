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
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher {

    private final DlrQueueService dlrQueueService;
    private final SimulatorProperties properties;
    private final StatsService statsService;

    // Lock-free transfer buffer between HTTP threads and lifecycle timers
    private final BlockingQueue<LifecycleTask> inboundBuffer = new LinkedBlockingQueue<>(2_000_000);
    private ScheduledExecutorService lifecycleScheduler;
    private ExecutorService bufferDrainer;
    private volatile boolean running = true;

    private record LifecycleTask(String wamid, String recipientId, String senderId, String callbackUrl) {}

    @PostConstruct
    public void init() {
        int cores = Runtime.getRuntime().availableProcessors();
        this.lifecycleScheduler = Executors.newScheduledThreadPool(Math.max(4, cores * 2));
        this.bufferDrainer = Executors.newSingleThreadExecutor();
        this.bufferDrainer.submit(this::drainInboundBuffer);
    }


    public void scheduleMessageLifecycle(String wamid, String recipientId, String senderId, String callbackUrl) {
        inboundBuffer.offer(new LifecycleTask(wamid, recipientId, senderId, callbackUrl));
    }

    private void drainInboundBuffer() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                LifecycleTask task = inboundBuffer.poll(20, TimeUnit.MILLISECONDS);
                if (task != null) {
                    if (properties.events().sentEnabled()) {
                        lifecycleScheduler.schedule(
                                () -> dispatchStatus(task.wamid(), task.recipientId(), task.senderId(), "sent", task.callbackUrl()),
                                properties.delays().sentSeconds(),
                                TimeUnit.SECONDS
                        );
                    }

                    if (properties.events().deliveredEnabled()) {
                        lifecycleScheduler.schedule(
                                () -> dispatchStatus(task.wamid(), task.recipientId(), task.senderId(), "delivered", task.callbackUrl()),
                                properties.delays().deliveredSeconds(),
                                TimeUnit.SECONDS
                        );
                    }

                    if (properties.events().readEnabled()) {
                        lifecycleScheduler.schedule(
                                () -> dispatchStatus(task.wamid(), task.recipientId(), task.senderId(), "read", task.callbackUrl()),
                                properties.delays().readSeconds(),
                                TimeUnit.SECONDS
                        );
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {
                // Prevent loop death
            }
        }
    }

    private void dispatchStatus(String wamid, String recipientId, String senderId, String statusName, String callbackUrl) {


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

        dlrQueueService.enqueueDlr(callbackUrl, payload);
    }

    @PreDestroy
    public void destroy() {
        this.running = false;
        if (bufferDrainer != null) bufferDrainer.shutdownNow();
        if (lifecycleScheduler != null) lifecycleScheduler.shutdownNow();
    }
}