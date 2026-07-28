package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.dto.response.StatsResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.LongAdder;

@Service
public class StatsService {

    private final DlrQueueService dlrQueueService;

    // Request Counters
    private final LongAdder incomingRequests = new LongAdder();
    private final LongAdder successRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    // DLR Status Lifecycle Counters
    private final LongAdder dlrSent = new LongAdder();
    private final LongAdder dlrDelivered = new LongAdder();
    private final LongAdder dlrRead = new LongAdder();
    private final LongAdder dlrFailed = new LongAdder();

    // Webhook Transport Counters
    private final LongAdder dlrEnqueued = new LongAdder();
    private final LongAdder dlrSuccessfullySent = new LongAdder();
    private final LongAdder dlrFailedToSend = new LongAdder();
    private final LongAdder dlrDiscarded = new LongAdder();

    // Explicit constructor with @Lazy to break the circular dependency loop
    public StatsService(@Lazy DlrQueueService dlrQueueService) {
        this.dlrQueueService = dlrQueueService;
    }

    // --- Metric Incrementors ---
    public void incrementIncomingRequests() { incomingRequests.increment(); }
    public void incrementSuccessRequests() { successRequests.increment(); }
    public void incrementFailedRequests() { failedRequests.increment(); }

    public void incrementDlrStatus(String statusName) {
        if (statusName == null) return;
        switch (statusName.toLowerCase()) {
            case "sent" -> dlrSent.increment();
            case "delivered" -> dlrDelivered.increment();
            case "read" -> dlrRead.increment();
            case "failed" -> dlrFailed.increment();
        }
    }

    public void incrementDlrEnqueued() { dlrEnqueued.increment(); }
    public void incrementDlrSuccessfullySent() { dlrSuccessfullySent.increment(); }
    public void incrementDlrFailedToSend() { dlrFailedToSend.increment(); }
    public void incrementDlrDiscarded() { dlrDiscarded.increment(); }

    // --- Get Current Stats ---
    public StatsResponse getStats() {
        return StatsResponse.builder()
                .requests(StatsResponse.RequestStats.builder()
                        .totalIncoming(incomingRequests.sum())
                        .totalSuccess(successRequests.sum())
                        .totalFailed(failedRequests.sum())
                        .build())
                .dlr(StatsResponse.DlrStats.builder()
                        .pendingInQueue(dlrQueueService.getQueueSize())
                        .totalEnqueued(dlrEnqueued.sum())
                        .sent(dlrSent.sum())
                        .delivered(dlrDelivered.sum())
                        .read(dlrRead.sum())
                        .failed(dlrFailed.sum())
                        .successfullySent(dlrSuccessfullySent.sum())
                        .failedToSend(dlrFailedToSend.sum())
                        .maxAttemptsDiscarded(dlrDiscarded.sum())
                        .build())
                .build();
    }

    // --- Reset/Refresh Stats ---
    public StatsResponse resetStats() {
        incomingRequests.reset();
        successRequests.reset();
        failedRequests.reset();

        dlrSent.reset();
        dlrDelivered.reset();
        dlrRead.reset();
        dlrFailed.reset();

        dlrEnqueued.reset();
        dlrSuccessfullySent.reset();
        dlrFailedToSend.reset();
        dlrDiscarded.reset();

        return getStats();
    }
}