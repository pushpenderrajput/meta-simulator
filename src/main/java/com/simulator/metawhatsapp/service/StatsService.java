package com.simulator.metawhatsapp.service;

import com.simulator.metawhatsapp.dto.response.StatsResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Service
public class StatsService {

    private final DlrQueueService dlrQueueService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("dd MMMM yyyy, hh:mm:ss.SSS a", Locale.ENGLISH)
            .withZone(ZoneId.of("Asia/Kolkata"));

    // Request Counters
    private final LongAdder incomingRequests = new LongAdder();
    private final LongAdder successRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();

    // Inbound Timestamps
    private final AtomicReference<Instant> firstMessageTime = new AtomicReference<>(null);
    private final AtomicReference<Instant> lastMessageTime = new AtomicReference<>(null);

    // Outbound Sent DLR Timestamps
    private final AtomicReference<Instant> firstDlrSentTime = new AtomicReference<>(null);
    private final AtomicReference<Instant> lastDlrSentTime = new AtomicReference<>(null);

    // DLR Lifecycle Counters
    private final LongAdder dlrSent = new LongAdder();
    private final LongAdder dlrDelivered = new LongAdder();
    private final LongAdder dlrRead = new LongAdder();
    private final LongAdder dlrFailed = new LongAdder();

    // Webhook Transport Counters
    private final LongAdder dlrEnqueued = new LongAdder();
    private final LongAdder dlrSuccessfullySent = new LongAdder();
    private final LongAdder dlrFailedToSend = new LongAdder();
    private final LongAdder dlrDiscarded = new LongAdder();

    public StatsService(@Lazy DlrQueueService dlrQueueService) {
        this.dlrQueueService = dlrQueueService;
    }

    // --- Metric Incrementors ---
    public void recordIncomingMessage() {
        Instant now = Instant.now();
        firstMessageTime.compareAndSet(null, now);
        lastMessageTime.set(now);
        incomingRequests.increment();
    }

    public void incrementSuccessRequests() { successRequests.increment(); }
    public void incrementFailedRequests() { failedRequests.increment(); }

    public void recordDlrDelivered(String statusName) {
        dlrSuccessfullySent.increment();
        if (statusName == null) return;
        switch (statusName.toLowerCase()) {
            case "sent" -> {
                Instant now = Instant.now();
                firstDlrSentTime.compareAndSet(null, now);
                lastDlrSentTime.set(now);
                dlrSent.increment();
            }
            case "delivered" -> dlrDelivered.increment();
            case "read" -> dlrRead.increment();
            case "failed" -> dlrFailed.increment();
        }
    }

    public void incrementDlrStatus(String statusName) {
        recordDlrDelivered(statusName);
    }

    public void incrementDlrEnqueued() { dlrEnqueued.increment(); }
    public void incrementDlrFailedToSend() { dlrFailedToSend.increment(); }
    public void incrementDlrDiscarded() { dlrDiscarded.increment(); }

    // --- Get Current Stats ---
    public StatsResponse getStats() {
        long totalInc = incomingRequests.sum();
        Instant first = firstMessageTime.get();
        Instant last = lastMessageTime.get();

        double inboundTps = 0.0;
        long inboundDurationSec = 0;

        if (first != null && last != null) {
            long durationMillis = Math.max(1, Duration.between(first, last).toMillis());
            inboundDurationSec = durationMillis / 1000;
            inboundTps = (double) totalInc / (durationMillis / 1000.0);
        }

        // Outbound Sent DLR TPS Calculation
        long totalSentDlrs = dlrSent.sum();
        Instant firstSent = firstDlrSentTime.get();
        Instant lastSent = lastDlrSentTime.get();

        double outboundSentTps = 0.0;
        long outboundDurationSec = 0;

        if (firstSent != null && lastSent != null) {
            long durationMillis = Math.max(1, Duration.between(firstSent, lastSent).toMillis());
            outboundDurationSec = durationMillis / 1000;
            outboundSentTps = (double) totalSentDlrs / (durationMillis / 1000.0);
        }

        return StatsResponse.builder()
                .requests(StatsResponse.RequestStats.builder()
                        .totalIncoming(totalInc)
                        .totalSuccess(successRequests.sum())
                        .totalFailed(failedRequests.sum())
                        .firstMessageTimestamp(first != null ? FORMATTER.format(first) : null)
                        .lastMessageTimestamp(last != null ? FORMATTER.format(last) : null)
                        .build())
                .performance(StatsResponse.PerformanceStats.builder()
                        .inboundTps(Math.round(inboundTps * 100.0) / 100.0)
                        .activeDurationSeconds(inboundDurationSec)
                        .outboundSentTps(Math.round(outboundSentTps * 100.0) / 100.0)
                        .outboundSentDurationSeconds(outboundDurationSec)
                        .firstDlrSentTimestamp(firstSent != null ? FORMATTER.format(firstSent) : null)
                        .lastDlrSentTimestamp(lastSent != null ? FORMATTER.format(lastSent) : null)
                        .build())
                .dlr(StatsResponse.DlrStats.builder()
                        .pendingInQueue(dlrQueueService.getQueueSize())
                        .totalEnqueued(dlrEnqueued.sum())
                        .sent(totalSentDlrs)
                        .delivered(dlrDelivered.sum())
                        .read(dlrRead.sum())
                        .failed(dlrFailed.sum())
                        .successfullySent(dlrSuccessfullySent.sum())
                        .failedToSend(dlrFailedToSend.sum())
                        .maxAttemptsDiscarded(dlrDiscarded.sum())
                        .build())
                .build();
    }

    // --- Reset Stats ---
    public StatsResponse resetStats() {
        incomingRequests.reset();
        successRequests.reset();
        failedRequests.reset();

        firstMessageTime.set(null);
        lastMessageTime.set(null);

        firstDlrSentTime.set(null);
        lastDlrSentTime.set(null);

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