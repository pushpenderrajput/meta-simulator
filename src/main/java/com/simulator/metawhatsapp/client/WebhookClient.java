package com.simulator.metawhatsapp.client;

import com.simulator.metawhatsapp.dto.webhook.MetaWebhookPayload;
import com.simulator.metawhatsapp.properties.SimulatorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookClient {

    private final WebClient webhookWebClient;
    private final SimulatorProperties properties;

    /**
     * Dispatches a wire-compatible webhook payload asynchronously to the target CPaaS application.
     * Uses exponential backoff with jitter to handle high outbound DLR throughput reliably.
     *
     * @param payload The structured Meta webhook containing the message statuses.
     */
    public void sendWebhook(MetaWebhookPayload payload) {
        String targetUrl = properties.webhook().callbackUrl();
        int maxAttempts = properties.webhook().retry().maxAttempts();

        log.debug("Dispatching outbound DLR payload to target URL: {}", targetUrl);

        webhookWebClient.post()
                .uri(targetUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(
                        Retry.backoff(maxAttempts, Duration.ofMillis(500)) // Initial delay: 500ms
                                .maxBackoff(Duration.ofSeconds(3))          // Cap backoff at 3s
                                .jitter(0.5)                                // Add 50% randomness to un-cluster retries
                                .filter(this::isRetryableException)        // Only retry network errors / 5xx
                                .doBeforeRetry(retrySignal ->
                                        log.warn("Retrying DLR delivery to {}. Attempt #{}/{} | Cause: {}",
                                                targetUrl,
                                                retrySignal.totalRetries() + 1,
                                                maxAttempts,
                                                retrySignal.failure().getMessage()
                                        )
                                )
                )
                .doOnSuccess(response ->
                        log.info("Successfully delivered webhook DLR to target application. Status={}", response.getStatusCode())
                )
                .doOnError(error ->
                        log.error("Failed to deliver webhook DLR after {} retry attempts. Error={}", maxAttempts, error.getMessage())
                )
                .subscribe();
    }

    /**
     * Filter out non-retryable exceptions (like 4xx errors).
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            // Do NOT retry 4xx errors (e.g., 400 Bad Request, 404 Not Found, 401 Unauthorized)
            return ex.getStatusCode().is5xxServerError();
        }
        // Retry connection timeouts, refused connections, and network drops
        return true;
    }
}