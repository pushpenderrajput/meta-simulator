package com.simulator.metawhatsapp.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatsResponse {

    private RequestStats requests;
    private DlrStats dlr;
    private PerformanceStats performance;

    @Getter
    @Builder
    public static class RequestStats {
        private long totalIncoming;
        private long totalSuccess;
        private long totalFailed;
        private String firstMessageTimestamp;
        private String lastMessageTimestamp;
    }

    @Getter
    @Builder
    public static class PerformanceStats {
        private double inboundTps;
        private long activeDurationSeconds;
        private double outboundSentTps;
        private long outboundSentDurationSeconds;
        private String firstDlrSentTimestamp;
        private String lastDlrSentTimestamp;
    }

    @Getter
    @Builder
    public static class DlrStats {
        private int pendingInQueue;
        private long totalEnqueued;
        private long sent;
        private long delivered;
        private long read;
        private long failed;
        private long successfullySent;
        private long failedToSend;
        private long maxAttemptsDiscarded;
    }
}