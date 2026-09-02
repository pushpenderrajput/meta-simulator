package com.simulator.metawhatsapp.controller;

import com.simulator.metawhatsapp.dto.response.StatsResponse;
import com.simulator.metawhatsapp.service.DlrQueueService;
import com.simulator.metawhatsapp.service.StatsService;
import com.simulator.metawhatsapp.webhook.WebhookDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final DlrQueueService dlrQueueService;
    private final WebhookDispatcher webhookDispatcher;

    @GetMapping
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(statsService.getStats());
    }

    @GetMapping("/refresh")
    public ResponseEntity<StatsResponse> refreshStats() {
        return ResponseEntity.ok(statsService.resetStats());
    }

    /**
     * Purges all queues and triggers explicit GC to return memory to the OS
     */
    @PostMapping("/purge")
    @GetMapping("/purge")
    public ResponseEntity<Map<String, Object>> purgeAllQueuesAndMemory() {
        webhookDispatcher.clearBuffer();
        dlrQueueService.clearAllQueues();
        statsService.resetStats();

        // Suggest immediate GC sweep
        System.gc();

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All in-memory queues purged and GC requested",
                "remainingQueueSize", dlrQueueService.getQueueSize()
        ));
    }
}