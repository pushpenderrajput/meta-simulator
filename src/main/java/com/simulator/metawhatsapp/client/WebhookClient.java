package com.simulator.metawhatsapp.client;

import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookClient {

    private final WebClient webhookWebClient;

    public Mono<Void> sendWebhook(String targetUrl, MetaWebhookPayload payload) {
        String wabaId = (payload.entry() != null && !payload.entry().isEmpty())
                ? payload.entry().get(0).id()
                : "UNKNOWN";

        String status = "unknown";
        try {
            status = payload.entry().get(0).changes().get(0).value().statuses().get(0).status();
        } catch (Exception ignored) {
            // Safe fallback if payload structure is altered
        }

        final String finalStatus = status;
        final String finalWabaId = wabaId;

        return webhookWebClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200)) // 2 fast WebClient retries before re-queueing
                                .maxBackoff(Duration.ofMillis(800))
                )
                .doOnSuccess(v -> log.debug("🚀 DLR delivered -> status={} waba={} targetUrl={}",
                        finalStatus, finalWabaId, targetUrl))
                .doOnError(e -> log.error("❌ Failed to deliver DLR -> status={} waba={} targetUrl={} error={}",
                        finalStatus, finalWabaId, targetUrl, e.getMessage()))
                .then();
    }
}