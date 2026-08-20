package com.simulator.metawhatsapp.client;

import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.dto.webhook.StatusDetail;
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
        String status = "unknown";
        String wamid = "UNKNOWN";

        try {
            StatusDetail statusDetail = payload.entry().get(0).changes().get(0).value().statuses().get(0);
            status = statusDetail.status();
            wamid = statusDetail.id();
        } catch (Exception ignored) {
            // Safe fallback if structure changes
        }

        final String finalStatus = status;
        final String finalWamid = wamid;

        return webhookWebClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(
                        Retry.backoff(2, Duration.ofMillis(200))
                                .maxBackoff(Duration.ofMillis(800))
                )
                .doOnSuccess(v -> log.debug("🚀 DLR delivered -> status={} wamid={} targetUrl={}",
                        finalStatus, finalWamid, targetUrl))
                .doOnError(e -> log.error("❌ Failed to deliver DLR -> status={} wamid={} targetUrl={} error={}",
                        finalStatus, finalWamid, targetUrl, e.getMessage()))
                .then();
    }
}