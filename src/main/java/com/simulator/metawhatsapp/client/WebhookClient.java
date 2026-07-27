package com.simulator.metawhatsapp.client;

import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
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
    private final SimulatorProperties properties;

    public Mono<Void> sendWebhook(MetaWebhookPayload payload) {
        String targetUrl = properties.webhook().callbackUrl();

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
                .doOnSuccess(response ->
                        log.debug("DLR delivered successfully to {}", targetUrl)
                )
                .then(); // Returns Mono<Void>
    }
}