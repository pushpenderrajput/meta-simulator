package com.simulator.metawhatsapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private RequestStats requests;
    private DlrStats dlr;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestStats {
        private long totalIncoming;
        private long totalSuccess;
        private long totalFailed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DlrStats {
        private long pendingInQueue;
        private long totalEnqueued;

        // DLR Lifecycle Status Counters
        private long sent;
        private long delivered;
        private long read;
        private long failed;

        // Webhook Delivery Transport Counters
        private long successfullySent;
        private long failedToSend;
        private long maxAttemptsDiscarded;
    }
}