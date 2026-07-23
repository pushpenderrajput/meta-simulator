package com.simulator.metawhatsapp.client;

import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookClient {

    private final WebClient webhookWebClient;
    private final SimulatorProperties properties;

    public void sendWebhook(MetaWebhookPayload payload) {
        String targetUrl = properties.webhook().callbackUrl();

        webhookWebClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(
                        Retry.backoff(5, Duration.ofSeconds(1)) // 5 Retries with backoff
                                .maxBackoff(Duration.ofSeconds(10))
                                .jitter(0.5)
                )
                .doOnSuccess(response ->
                        log.debug("DLR Delivered successfully.")
                )
                .doOnError(error ->
                        log.error("Failed DLR delivery to {} after retries. Error: {}", targetUrl, error.getMessage())
                )
                .subscribe();
    }
}